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


import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaHealthAgent {

    private static final String ID    = LeaAgentActivationManager.HEALTH;
    private static final String PREFS = "lea_health_agent";

    // Thresholds
    private static final long   DRINK_INTERVAL_MS  = 2 * 3600 * 1000L; // 2h
    private static final long   SIT_THRESHOLD_MS   = 2 * 3600 * 1000L; // 2h sitting
    private static final int    MIN_SLEEP_HOURS     = 6;

    private final Context                   ctx;
    private final LeaAgentDatabase          db;
    private final LeaAgentNotificationManager notif;
    private final SharedPreferences         prefs;

    public LeaHealthAgent(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.notif = LeaAgentNotificationManager.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void execute() {
        try {
            checkHydration();
            checkSitting();
            analyzeSleepPattern();
            generateHealthSummary();
        } catch (Exception e) {
            db.addLog(ID, "⚠️ Erreur Santé: " + e.getMessage());
        }
    }

    private void checkHydration() {
        long lastDrink = prefs.getLong("last_drink_reminder", 0);
        long now       = System.currentTimeMillis();

        if (now - lastDrink > DRINK_INTERVAL_MS) {
            // Check if user is likely active (between 8h and 22h)
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

            if (hour >= 8 && hour <= 22) {
                String msg = "💧 Rappel: N'oublie pas de boire de l'eau! (Dernière rappel il y a " +
                    ((now - lastDrink) / 3600000) + "h)";
                db.addLog(ID, msg);
                notif.notify(ID, "💧 Hydratation", "N'oublie pas de boire de l'eau!");
                prefs.edit().putLong("last_drink_reminder", now).apply();
            }
        }
    }

    private void checkSitting() {
        long lastMove    = prefs.getLong("last_movement", System.currentTimeMillis());
        long lastSitNotif = prefs.getLong("last_sit_notif", 0);
        long now         = System.currentTimeMillis();

        // Notifier seulement si : assis depuis 2h+ ET dernière notif étirements il y a 2h+ minimum
        if (now - lastMove > SIT_THRESHOLD_MS && now - lastSitNotif > SIT_THRESHOLD_MS) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

            if (hour >= 9 && hour <= 20) {
                String msg = "🧘 Tu es assis depuis plus de 2h — quelques étirements?";
                db.addLog(ID, msg);
                notif.notify(ID, "🧘 Étirements", "Assis depuis 2h+. Pause stretching recommandée!");
                prefs.edit().putLong("last_sit_notif", now).apply();
            }
        }
    }

    public void recordMovement() {
        prefs.edit().putLong("last_movement", System.currentTimeMillis()).apply();
    }

    public void recordDrink() {
        long now = System.currentTimeMillis();
        prefs.edit().putLong("last_drink_reminder", now).apply();
        // Compteur journalier — incrémenté à chaque verre
        String today = getTodayKey();
        int count = prefs.getInt("drinks_" + today, 0) + 1;
        prefs.edit().putInt("drinks_" + today, count).apply();
        db.addLog(ID, "💧 Verre enregistré (" + count + " aujourd'hui)");
    }

    public int getDrinksToday() {
        return prefs.getInt("drinks_" + getTodayKey(), 0);
    }

    private void analyzeSleepPattern() {
        // Read sleep data from SharedPrefs (recorded by sleep detection)
        int sleepHours = prefs.getInt("last_sleep_hours", 0);

        if (sleepHours > 0 && sleepHours < MIN_SLEEP_HOURS) {
            String msg = "😴 Attention: seulement " + sleepHours + "h de sommeil détectées. Minimum recommandé: " + MIN_SLEEP_HOURS + "h";
            db.addLog(ID, msg);
            notif.notify(ID, "😴 Alerte Sommeil", msg);
        } else if (sleepHours >= 7) {
            db.addLog(ID, "✅ Sommeil: " + sleepHours + "h — excellent!");
        }
    }

    public void recordSleep(int hours) {
        prefs.edit().putInt("last_sleep_hours", hours).apply();
        // Historique par date pour la moyenne hebdomadaire
        prefs.edit().putInt("sleep_" + getTodayKey(), hours).apply();
        db.addLog(ID, "🌙 Sommeil enregistré: " + hours + "h");
    }

    private void generateHealthSummary() {
        int sleepH = prefs.getInt("last_sleep_hours", 0);
        int steps  = prefs.getInt("steps_" + getTodayKey(), -1);

        StringBuilder sb = new StringBuilder("📊 Bilan santé: ");
        if (sleepH > 0) sb.append(sleepH).append("h de sommeil");
        if (steps >= 0) sb.append(sleepH > 0 ? ", " : "").append(steps).append(" pas");
        if (sleepH == 0 && steps < 0) sb.append("Données insuffisantes");

        db.updateLastAction(ID, sb.toString());
    }

    public void recordSteps(int steps) {
        prefs.edit()
            .putInt("daily_steps", steps)
            .putInt("steps_" + getTodayKey(), steps)
            .apply();
        db.addLog(ID, "🚶 Pas enregistrés : " + steps);
    }

    public String getHealthSummary() {
        int sleepH = prefs.getInt("last_sleep_hours", 0);
        // Pas : n'affiche la valeur que si elle a été enregistrée aujourd'hui
        int steps  = prefs.getInt("steps_" + getTodayKey(), -1);
        int drinks = getDrinksToday();
        return "Sommeil: " + (sleepH > 0 ? sleepH + "h" : "N/A")
             + "  |  Pas: " + (steps >= 0 ? String.valueOf(steps) : "N/A")
             + "  |  Eau: " + drinks + " verre(s)";
    }

    /** Bilan des 7 derniers jours — moyenne sommeil, total pas, hydratation. */
    public String getWeeklyStats() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        int totalSleep = 0, sleepDays = 0;
        int totalDrinks = 0;
        int totalSteps = 0;
        int stepDays   = 0;

        StringBuilder sleepDetail = new StringBuilder();
        StringBuilder stepsDetail = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String key = sdf.format(cal.getTime());
            String label = i == 0 ? "Auj." : "J-" + i;

            int sleep = prefs.getInt("sleep_" + key, -1);
            if (sleep >= 0) {
                totalSleep += sleep;
                sleepDays++;
                sleepDetail.insert(0, "  " + label + " : " + sleep + "h\n");
            }

            int steps = prefs.getInt("steps_" + key, -1);
            if (steps >= 0) {
                totalSteps += steps;
                stepDays++;
                stepsDetail.insert(0, "  " + label + " : " + steps + " pas\n");
            }

            totalDrinks += prefs.getInt("drinks_" + key, 0);
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("😴 Sommeil (7 jours)\n");
        if (sleepDays > 0) {
            sb.append("  Moyenne : ").append(totalSleep / sleepDays).append("h/nuit\n");
            sb.append(sleepDetail);
        } else {
            sb.append("  Aucune donnée — enregistre tes nuits depuis l'app\n");
        }
        sb.append("\n💧 Hydratation : ").append(totalDrinks).append(" verres cette semaine\n");
        sb.append("   Aujourd'hui : ").append(getDrinksToday()).append("/8 verres\n");
        sb.append("\n🚶 Pas (7 jours)\n");
        if (stepDays > 0) {
            sb.append("  Total : ").append(totalSteps).append(" pas\n");
            sb.append(stepsDetail);
        } else {
            sb.append("  Aucune donnée — enregistre tes pas depuis l'app\n");
        }
        return sb.toString().trim();
    }

    private String getTodayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public List<LeaAgentDatabase.LogRow> getLogs() {
        return db.getLogs(ID, 20);
    }
}
