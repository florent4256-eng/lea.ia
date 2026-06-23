package com.flolov42.lea_v3.plus.lifestyle;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LeaLifeOS extends LeaBasePlusFeature {

    private static final String PREFS = "lea_lifeos";

    private static final String[][] NUTRITION_TIPS = {
        {"Petit-déjeuner",  "Protéines + fibres = énergie stable jusqu'à midi"},
        {"Déjeuner",        "Légumes de saison + glucides complexes pour l'après-midi"},
        {"Dîner",           "Repas léger 2h avant de dormir pour un meilleur sommeil"},
        {"Goûter",          "Fruits + poignée de noix — satiété sans pic glycémique"},
    };

    private static final String[][] MORNING_ROUTINES = {
        {"Méditation", "5 min de respiration consciente"},
        {"Hydratation", "Grand verre d'eau au réveil"},
        {"Mouvement",   "10 squats / 10 pompes / 10 étirements"},
        {"Intentions",  "Écris tes 3 priorités du jour"},
    };

    private static final String[][] EVENING_ROUTINES = {
        {"Débrief",      "Revois tes 3 intentions — atteintes ?"},
        {"Gratitude",    "Note 3 choses positives de la journée"},
        {"Préparation",  "Prépare tes affaires pour demain"},
        {"Digital detox","Pose le téléphone 30 min avant de dormir"},
    };

    public LeaLifeOS(Context ctx) { super(ctx, LeaPlusDatabase.LIFE_OS); }

    @Override
    public void execute() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        checkMorningRoutine(hour);
        checkEveningRoutine(hour);
        checkNutritionTip(hour);
        checkScheduleAlerts(hour);
    }

    private void checkMorningRoutine(int hour) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (hour == 7 && !today.equals(prefs.getString("morning_done", ""))) {
            notify("☀️ Routine Matinale LÉA", buildRoutineText(MORNING_ROUTINES));
            log("☀️ Routine matinale déclenchée");
            prefs.edit().putString("morning_done", today).apply();
        }
    }

    private void checkEveningRoutine(int hour) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = String.valueOf(System.currentTimeMillis() / 86400_000L);
        if (hour == 21 && !today.equals(prefs.getString("evening_done", ""))) {
            notify("🌙 Routine du Soir LÉA", buildRoutineText(EVENING_ROUTINES));
            log("🌙 Routine du soir déclenchée");
            prefs.edit().putString("evening_done", today).apply();
        }
    }

    private void checkNutritionTip(int hour) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String hourKey = "nutri_" + hour;
        if ((hour == 7 || hour == 12 || hour == 16 || hour == 19)
                && !String.valueOf(System.currentTimeMillis() / 86400_000L).equals(prefs.getString(hourKey, ""))) {
            int idx = (hour == 7) ? 0 : (hour == 12) ? 1 : (hour == 19) ? 2 : 3;
            String[] tip = NUTRITION_TIPS[idx];
            notify("🥗 " + tip[0], tip[1]);
            log("🥗 Conseil nutrition: " + tip[0]);
            prefs.edit().putString(hourKey, String.valueOf(System.currentTimeMillis() / 86400_000L)).apply();
        }
    }

    private void checkScheduleAlerts(int hour) {
        List<LeaPlusDatabase.ScheduleItem> items = db.getScheduleForHour(hour);
        for (LeaPlusDatabase.ScheduleItem item : items) {
            notify("📅 " + item.title, item.description + " — Maintenant !");
            db.markScheduleNotified(item.id);
            log("📅 Alerte agenda: " + item.title);
        }
    }

    public void addScheduleItem(String title, String description, int hour, int minute) {
        db.insertScheduleItem(title, description, hour, minute);
        log("📅 Agenda: \"" + title + "\" ajouté à " + hour + "h" + (minute<10?"0":"") + minute);
    }

    public void logFeedback(String activity, int satisfaction) {
        db.insertLifeFeedback(activity, satisfaction);
        log("📊 Feedback: \"" + activity + "\" — " + satisfaction + "/10");
        LeaPlusManager.get(ctx).onTaskCompleted("LifeOS feedback: " + activity, 1);
    }

    public String getDailyPlan() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        List<LeaPlusDatabase.ScheduleItem> items = db.getTodaySchedule();
        StringBuilder sb = new StringBuilder("📅 TON PLAN LÉA OS\n\n");

        sb.append("☀️ ROUTINE MATIN:\n");
        for (String[] r : MORNING_ROUTINES) sb.append("  • ").append(r[0]).append(": ").append(r[1]).append("\n");

        sb.append("\n📆 AGENDA:\n");
        if (items.isEmpty()) sb.append("  Aucun événement — ajoute tes tâches !\n");
        else for (LeaPlusDatabase.ScheduleItem i : items)
            sb.append("  ").append(i.hour).append("h — ").append(i.title).append("\n");

        sb.append("\n🌙 ROUTINE SOIR:\n");
        for (String[] r : EVENING_ROUTINES) sb.append("  • ").append(r[0]).append(": ").append(r[1]).append("\n");

        sb.append("\n💡 Conseil: ").append(getNutritionForHour(hour));
        return sb.toString();
    }

    private String getNutritionForHour(int hour) {
        if (hour < 10) return NUTRITION_TIPS[0][1];
        if (hour < 15) return NUTRITION_TIPS[1][1];
        if (hour < 18) return NUTRITION_TIPS[3][1];
        return NUTRITION_TIPS[2][1];
    }

    private String buildRoutineText(String[][] routine) {
        StringBuilder sb = new StringBuilder();
        for (String[] r : routine) sb.append("• ").append(r[0]).append(": ").append(r[1]).append("\n");
        return sb.toString().trim();
    }

    public List<LeaPlusDatabase.ScheduleItem> getTodaySchedule() { return db.getTodaySchedule(); }
}
