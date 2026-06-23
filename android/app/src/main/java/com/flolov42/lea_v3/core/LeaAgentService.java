package com.flolov42.lea_v3.core;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.plus.gamification.*;
import com.flolov42.lea_v3.plus.lifestyle.*;
import com.flolov42.lea_v3.plus.learning.*;
import com.flolov42.lea_v3.plus.premium.*;
import com.flolov42.lea_v3.plus.connect.*;
import com.flolov42.lea_v3.bixby.*;
import com.flolov42.lea_v3.routines.*;
import com.flolov42.lea_v3.telephony.*;
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;

public class LeaAgentService extends Service {

    public static LeaAgentService instance;

    private Handler                   handler;
    private LeaAgentActivationManager mgr;
    private LeaAgentNotificationManager notifMgr;

    // Agent instances
    private LeaEmailAgent        emailAgent;
    private LeaNotificationAgent notifAgent;
    private LeaCalendarAgent     calendarAgent;
    private LeaFinanceAgent      financeAgent;
    private LeaHealthAgent       healthAgent;
    private LeaProductivityAgent productivityAgent;
    private LeaSocialAgent       socialAgent;
    private LeaSmartHomeAgent    smartHomeAgent;
    private LeaLearningAgent     learningAgent;
    private LeaSecurityAgent     securityAgent;
    private LeaCodeAgent         codeAgent;

    // Modes / Plus managers
    private LeaModeManager modeManager;
    private LeaPlusManager plusManager;

    // Runnables actifs par agent (pour annulation)
    private final Map<String, Runnable> agentRunnables = new HashMap<>();
    private Runnable modeRunnable;
    private Runnable plusRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        instance  = this;
        handler   = new Handler(Looper.getMainLooper());
        mgr       = LeaAgentActivationManager.get(this);
        notifMgr  = LeaAgentNotificationManager.get(this);

        emailAgent        = new LeaEmailAgent(this);
        notifAgent        = new LeaNotificationAgent(this);
        calendarAgent     = new LeaCalendarAgent(this);
        financeAgent      = new LeaFinanceAgent(this);
        healthAgent       = new LeaHealthAgent(this);
        productivityAgent = new LeaProductivityAgent(this);
        socialAgent       = new LeaSocialAgent(this);
        smartHomeAgent    = new LeaSmartHomeAgent(this);
        learningAgent     = new LeaLearningAgent(this);
        securityAgent     = new LeaSecurityAgent(this);
        codeAgent         = new LeaCodeAgent(this);
        modeManager       = LeaModeManager.get(this);
        plusManager       = LeaPlusManager.get(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Vérifie si au moins un agent est activé avant de passer en foreground
        boolean anyEnabled = false;
        String[] allIds = {
            LeaAgentActivationManager.EMAIL, LeaAgentActivationManager.NOTIFICATION,
            LeaAgentActivationManager.CALENDAR, LeaAgentActivationManager.FINANCE,
            LeaAgentActivationManager.HEALTH, LeaAgentActivationManager.PRODUCTIVITY,
            LeaAgentActivationManager.SOCIAL, LeaAgentActivationManager.LEARNING,
            LeaAgentActivationManager.SECURITY, LeaAgentActivationManager.CODE
        };
        for (String id : allIds) { if (mgr.isEnabled(id)) { anyEnabled = true; break; } }

        if (!anyEnabled) {
            // Android 8+ exige startForeground() avant stopSelf() sous peine de crash ANR
            startForeground(notifMgr.getServiceNotifId(), notifMgr.buildServiceNotification());
            stopForeground(true); // supprime la notification immédiatement
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(notifMgr.getServiceNotifId(), notifMgr.buildServiceNotification());
        startEnabledAgents();
        startModesLoop();
        startPlusFeaturesLoop();
        return START_STICKY;
    }

    private void startEnabledAgents() {
        // SMART_HOME est exclu : il ne tourne que quand l'utilisateur ouvre Léa Home
        String[] ids = {
            LeaAgentActivationManager.EMAIL,
            LeaAgentActivationManager.NOTIFICATION,
            LeaAgentActivationManager.CALENDAR,
            LeaAgentActivationManager.FINANCE,
            LeaAgentActivationManager.HEALTH,
            LeaAgentActivationManager.PRODUCTIVITY,
            LeaAgentActivationManager.SOCIAL,
            LeaAgentActivationManager.LEARNING,
            LeaAgentActivationManager.SECURITY,
            LeaAgentActivationManager.CODE,
        };
        for (String id : ids) {
            if (mgr.isEnabled(id)) startAgent(id);
        }
    }

    public void startAgent(String agentId) {
        stopAgent(agentId);
        long interval = getInterval(agentId);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Si l'agent a été désactivé, on arrête définitivement
                if (!mgr.isEnabled(agentId)) {
                    agentRunnables.remove(agentId);
                    return;
                }
                executeAgent(agentId);
                handler.postDelayed(this, interval);
            }
        };
        agentRunnables.put(agentId, r);
        handler.post(r);
    }

    public void stopAgent(String agentId) {
        Runnable r = agentRunnables.remove(agentId);
        if (r != null) handler.removeCallbacks(r);
    }

    /**
     * Appelé depuis l'UI quand l'utilisateur bascule un agent ON/OFF.
     * Démarre ou arrête immédiatement le runnable périodique.
     */
    public void onAgentToggled(String agentId, boolean enabled) {
        if (enabled) {
            startAgent(agentId);
        } else {
            stopAgent(agentId);
        }
    }

    private long getInterval(String agentId) {
        switch (agentId) {
            case LeaAgentActivationManager.EMAIL:        return 15 * 60 * 1000L;   // 15 min
            case LeaAgentActivationManager.NOTIFICATION: return 2  * 3600 * 1000L; // 2h (rapport groupé)
            case LeaAgentActivationManager.CALENDAR:     return 10 * 60 * 1000L;   // 10 min
            case LeaAgentActivationManager.FINANCE:      return 30 * 60 * 1000L;   // 30 min
            case LeaAgentActivationManager.HEALTH:       return 60 * 60 * 1000L;   // 1h
            case LeaAgentActivationManager.PRODUCTIVITY: return 20 * 60 * 1000L;   // 20 min
            case LeaAgentActivationManager.SOCIAL:       return 4  * 3600 * 1000L; // 4h
            case LeaAgentActivationManager.LEARNING:     return 2  * 3600 * 1000L; // 2h
            case LeaAgentActivationManager.SECURITY:     return 10 * 60 * 1000L;   // 10 min
            case LeaAgentActivationManager.CODE:         return 60 * 60 * 1000L;   // 1h
            default:                                     return 30 * 60 * 1000L;
        }
    }

    private void executeAgent(String agentId) {
        new Thread(() -> {
            try {
                switch (agentId) {
                    case LeaAgentActivationManager.EMAIL:        emailAgent.execute();        break;
                    case LeaAgentActivationManager.NOTIFICATION: notifAgent.execute();        break;
                    case LeaAgentActivationManager.CALENDAR:     calendarAgent.execute();     break;
                    case LeaAgentActivationManager.FINANCE:      financeAgent.execute();      break;
                    case LeaAgentActivationManager.HEALTH:       healthAgent.execute();       break;
                    case LeaAgentActivationManager.PRODUCTIVITY: productivityAgent.execute(); break;
                    case LeaAgentActivationManager.SOCIAL:       socialAgent.execute();       break;
                    case LeaAgentActivationManager.LEARNING:     learningAgent.execute();     break;
                    case LeaAgentActivationManager.SECURITY:     securityAgent.execute();     break;
                    case LeaAgentActivationManager.CODE:         codeAgent.runIdle();         break;
                }
            } catch (Exception e) {
                LeaAgentDatabase.get(this).addLog(agentId, "⚠️ Erreur: " + e.getMessage());
            }
        }).start();
    }

    // ── Plus features loop ────────────────────────────────────────────────────

    private void startPlusFeaturesLoop() {
        if (plusRunnable != null) handler.removeCallbacks(plusRunnable);
        plusRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try { plusManager.executeAll(); } catch (Exception ignored) {}
                }).start();
                handler.postDelayed(this, 60 * 60 * 1000L);
            }
        };
        handler.postDelayed(plusRunnable, 5 * 60 * 1000L);
    }

    // ── Modes loop ────────────────────────────────────────────────────────────

    private void startModesLoop() {
        if (modeRunnable != null) handler.removeCallbacks(modeRunnable);
        modeRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try { modeManager.executeAll(); } catch (Exception ignored) {}
                }).start();
                handler.postDelayed(this, 30 * 60 * 1000L);
            }
        };
        // Premier run 5 min après démarrage (laisser le temps à l'app de s'initialiser)
        handler.postDelayed(modeRunnable, 5 * 60 * 1000L);
    }

    public void triggerMode(String modeId) {
        new Thread(() -> {
            try { modeManager.executeMode(modeId); } catch (Exception ignored) {}
        }).start();
    }

    // Expose agent instances for direct calls from UI
    public LeaCodeAgent          getCodeAgent()     { return codeAgent; }
    public LeaFinanceAgent       getFinanceAgent()  { return financeAgent; }
    public LeaHealthAgent        getHealthAgent()   { return healthAgent; }
    public LeaCalendarAgent      getCalendarAgent() { return calendarAgent; }
    public LeaModeManager        getModeManager()   { return modeManager; }
    public LeaNotificationAgent  getNotifAgent()    { return notifAgent; }
    public LeaSmartHomeAgent     getSmartHomeAgent(){ return smartHomeAgent; }
    public LeaEmailAgent         getEmailAgent()    { return emailAgent; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        for (Runnable r : agentRunnables.values()) handler.removeCallbacks(r);
        agentRunnables.clear();
        if (modeRunnable != null) handler.removeCallbacks(modeRunnable);
        if (plusRunnable  != null) handler.removeCallbacks(plusRunnable);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
