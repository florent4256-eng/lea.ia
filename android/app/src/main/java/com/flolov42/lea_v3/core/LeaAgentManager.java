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


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

public class LeaAgentManager {

    // ── Real-time state observer ──────────────────────────────────────────────

    public interface StateListener {
        void onAgentStateChanged(String agentId, boolean enabled);
    }

    private final List<StateListener> listeners = new ArrayList<>();

    public void registerListener(StateListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void unregisterListener(StateListener l) {
        listeners.remove(l);
    }

    private void notifyListeners(String agentId, boolean enabled) {
        for (StateListener l : listeners) l.onAgentStateChanged(agentId, enabled);
    }

    // ── Core fields ───────────────────────────────────────────────────────────

    private final Context                   ctx;
    private final LeaAgentActivationManager activation;
    private final LeaAgentDatabase          db;

    private static LeaAgentManager instance;

    public static synchronized LeaAgentManager get(Context ctx) {
        if (instance == null) instance = new LeaAgentManager(ctx.getApplicationContext());
        return instance;
    }

    private LeaAgentManager(Context ctx) {
        this.ctx        = ctx;
        this.activation = LeaAgentActivationManager.get(ctx);
        this.db         = LeaAgentDatabase.get(ctx);
    }

    public void enableAgent(String agentId) {
        activation.setEnabled(agentId, true);
        notifyListeners(agentId, true);
        ensureServiceRunning();
        // Démarre immédiatement le Runnable périodique si le service tourne déjà
        if (LeaAgentService.instance != null) {
            LeaAgentService.instance.startAgent(agentId);
        }
    }

    public void disableAgent(String agentId) {
        activation.setEnabled(agentId, false);
        notifyListeners(agentId, false);
        // Arrête immédiatement le Runnable — ne pas attendre le prochain cycle
        if (LeaAgentService.instance != null) {
            LeaAgentService.instance.stopAgent(agentId);
        }
    }

    public boolean isEnabled(String agentId) {
        return activation.isEnabled(agentId);
    }

    public List<LeaAgentDatabase.LogRow> getLogs(String agentId) {
        return db.getLogs(agentId, 25);
    }

    public void ensureServiceRunning() {
        Intent svc = new Intent(ctx, LeaAgentService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(svc);
        } else {
            ctx.startService(svc);
        }
    }

    public void stopService() {
        ctx.stopService(new Intent(ctx, LeaAgentService.class));
    }

    // ── Agent metadata ────────────────────────────────────────────────────────

    public static class AgentInfo {
        public final String id, name, icon, description;
        public final int    color;
        public AgentInfo(String id, String name, String icon, String description, int color) {
            this.id          = id;
            this.name        = name;
            this.icon        = icon;
            this.description = description;
            this.color       = color;
        }
    }

    public static final AgentInfo[] ALL_AGENTS = {
        new AgentInfo(LeaAgentActivationManager.EMAIL,
            "Email Intelligent", "📧",
            "Lit et répond automatiquement aux emails non lus. Génère des réponses intelligentes basées sur le contexte.",
            0xFF2196F3),
        new AgentInfo(LeaAgentActivationManager.NOTIFICATION,
            "Notification Smart", "🔔",
            "Groupe et résume toutes les notifications. Supprime les spams et crée un résumé clair.",
            0xFF9C27B0),
        new AgentInfo(LeaAgentActivationManager.CALENDAR,
            "Calendrier Intelligent", "📅",
            "Détecte les conflits, rappelle les RDV 30min avant, alerte sur les réunions tardives.",
            0xFF4CAF50),
        new AgentInfo(LeaAgentActivationManager.FINANCE,
            "Finance / Budget", "💰",
            "Parse les SMS bancaires, catégorise les dépenses, détecte les abonnements inutiles.",
            0xFFFF9800),
        new AgentInfo(LeaAgentActivationManager.HEALTH,
            "Santé / Bien-être", "💪",
            "Rappels hydratation, étirements après 2h assis, analyse du sommeil et activité physique.",
            0xFFF44336),
        new AgentInfo(LeaAgentActivationManager.PRODUCTIVITY,
            "Productivité", "⚡",
            "Détecte la procrastination, propose les tâches selon l'énergie, mode Focus 90min.",
            0xFFFFEB3B),
        new AgentInfo(LeaAgentActivationManager.SOCIAL,
            "Social", "👥",
            "Rappelle d'appeler les amis oubliés, anniversaires, et événements sociaux.",
            0xFF00BCD4),
        new AgentInfo(LeaAgentActivationManager.SMART_HOME,
            "Smart Home", "🏠",
            "Détecte présence via WiFi, active scènes SmartThings, optimise l'énergie automatiquement.",
            0xFF607D8B),
        new AgentInfo(LeaAgentActivationManager.LEARNING,
            "Apprentissage", "📚",
            "Micro-sessions d'apprentissage de 5min, suivi des progrès, contenu personnalisé.",
            0xFF8BC34A),
        new AgentInfo(LeaAgentActivationManager.SECURITY,
            "Anti-Spam / Sécurité", "🔐",
            "Détecte SMS phishing, scanne les apps suspectes, bloque spam automatiquement.",
            0xFFEF5350),
        new AgentInfo(LeaAgentActivationManager.CODE,
            "Code Agent", "💻",
            "Génère du code Android via IA, éditeur VSCode-like, compilation Gradle, installation APK.",
            0xFF673AB7),
    };
}
