package com.flolov42.lea_v3.agents;

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


import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class LeaProductivityAgent {

    private static final String ID    = LeaAgentActivationManager.PRODUCTIVITY;
    private static final String PREFS = "lea_productivity";

    // Time-wasting apps
    private static final String[] DISTRACTION_APPS = {
        "com.zhiliaoapp.musically", "com.tiktok.android",
        "com.instagram.android", "com.facebook.katana",
        "com.twitter.android", "com.reddit.frontpage",
        "com.google.android.youtube"
    };

    // Distraction threshold: 1h
    private static final long DISTRACTION_THRESHOLD = 60 * 60 * 1000L;

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    private boolean focusMode = false;

    public LeaProductivityAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // Restaure l'état focus après un redémarrage du service
        this.focusMode = prefs.getBoolean("focus_active", false);
    }

    /** Vérifie si PACKAGE_USAGE_STATS est accordé (n'est pas une permission runtime normale). */
    public static boolean isUsageStatsGranted(Context ctx) {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager)
            ctx.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            ctx.getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    public void execute() {
        try {
            detectProcrastination();
            suggestTaskByEnergy();
            checkFocusModeExpiry();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Productivité: " + e.getMessage());
        }
    }

    private void detectProcrastination() {
        try {
            UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;

            long end   = System.currentTimeMillis();
            long start = end - 3 * 3600 * 1000L; // last 3h

            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(start, end);

            for (String pkg : DISTRACTION_APPS) {
                UsageStats s = stats.get(pkg);
                if (s != null && s.getTotalTimeInForeground() > DISTRACTION_THRESHOLD) {
                    long minutes = s.getTotalTimeInForeground() / 60000;
                    String appName = pkg.contains("tiktok") ? "TikTok" :
                                     pkg.contains("instagram") ? "Instagram" :
                                     pkg.contains("youtube") ? "YouTube" :
                                     pkg.contains("facebook") ? "Facebook" : pkg;
                    String msg = "⚡ Tu scrolles " + appName + " depuis " + minutes + "min. Focus?";
                    db.addLog(ID, msg);
                    notif.notify(ID, "⚡ Procrastination détectée", msg);
                }
            }
        } catch (Exception e) {
            db.addLog(ID, "📊 Stats usage non disponibles (permission requise)");
        }
    }

    private void suggestTaskByEnergy() {
        Calendar cal  = Calendar.getInstance();
        int hour      = cal.get(Calendar.HOUR_OF_DAY);
        String energy = getEnergyLevel(hour);
        String task   = getSuggestedTask(energy);

        if (task != null) {
            String msg = "💡 " + energy + " — Recommandé: " + task;
            db.addLog(ID, msg);
            db.updateLastAction(ID, msg);
        }
    }

    private String getEnergyLevel(int hour) {
        if (hour >= 9  && hour <= 12) return "Énergie maximale (9h-12h)";
        if (hour >= 14 && hour <= 17) return "Bonne énergie (14h-17h)";
        if (hour >= 20 || hour < 7)   return "Énergie faible (soirée/nuit)";
        if (hour >= 7  && hour < 9)   return "Énergie montante (7h-9h)";
        return "Énergie modérée (12h-14h)";
    }

    private String getSuggestedTask(String energy) {
        if (energy.contains("maximale")) return "Tâches complexes, analyse, code";
        if (energy.contains("Bonne"))    return "Réunions, emails, communication";
        if (energy.contains("faible"))   return "Tâches simples, lecture, organisation";
        if (energy.contains("montante")) return "Revue des priorités du jour, démarrage progressif";
        return "Pause déjeuner, revue des notes, planification de l'après-midi";
    }

    private void checkFocusModeExpiry() {
        if (!focusMode) return;
        long focusStart = prefs.getLong("focus_start", 0);
        long focusDur   = prefs.getLong("focus_duration", 90 * 60 * 1000L);

        if (System.currentTimeMillis() - focusStart > focusDur) {
            focusMode = false;
            prefs.edit().putBoolean("focus_active", false).apply();
            String msg = "🎉 Session FOCUS terminée! Excellent travail!";
            db.addLog(ID, msg);
            notif.notify(ID, "⚡ Focus terminé", msg);
        }
    }

    public void startFocusMode(int minutes) {
        focusMode = true;
        prefs.edit()
            .putBoolean("focus_active", true)
            .putLong("focus_start", System.currentTimeMillis())
            .putLong("focus_duration", minutes * 60 * 1000L)
            .apply();
        db.addLog(ID, "🎯 Mode FOCUS activé pour " + minutes + " minutes");
        notif.notify(ID, "🎯 Mode Focus", "Focus activé pour " + minutes + " minutes. Bonne concentration!");
    }

    public void celebrateTaskCompletion(String taskName) {
        int completed = getTasksCompletedToday() + 1;
        prefs.edit()
            .putInt("tasks_completed_today", completed)
            .putLong("tasks_day", getDayStart())
            .apply();
        String msg = "🎉 Tâche terminée: «" + taskName + "» (" + completed + " aujourd'hui)";
        db.addLog(ID, msg);
        notif.notify(ID, "🎉 Tâche complétée!", msg);
    }

    public boolean isFocusMode() { return focusMode; }

    public int getTasksCompletedToday() {
        long savedDay = prefs.getLong("tasks_day", 0);
        if (savedDay < getDayStart()) {
            // Nouveau jour — remet le compteur à zéro automatiquement
            prefs.edit().putInt("tasks_completed_today", 0).putLong("tasks_day", getDayStart()).apply();
            return 0;
        }
        return prefs.getInt("tasks_completed_today", 0);
    }

    private long getDayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
