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
import java.util.Calendar;
import java.util.List;

public class LeaHabitTracker extends LeaBasePlusFeature {

    public LeaHabitTracker(Context ctx) { super(ctx, LeaPlusDatabase.HABITS); }

    @Override
    public void execute() {
        checkReminders();
        updateStats();
    }

    private void checkReminders() {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();
        for (LeaPlusDatabase.HabitRow h : habits) {
            if (h.reminderHour == currentHour) {
                long dayAgo = System.currentTimeMillis() - 86400_000L;
                if (h.lastCheck < dayAgo) {
                    notif.notifyHabitReminder(h.name, h.streak);
                    log("⏰ Rappel envoyé: " + h.name + " (série: " + h.streak + " jours)");
                }
            }
        }
    }

    private void updateStats() {
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();
        int totalStreak = 0;
        for (LeaPlusDatabase.HabitRow h : habits) totalStreak += h.streak;
        if (!habits.isEmpty()) log("🔗 " + habits.size() + " habitude(s) | Série totale: " + totalStreak + " jours");
    }

    public long addHabit(String name, String frequency, int reminderHour) {
        long id = db.insertHabit(name, frequency, reminderHour);
        log("➕ Nouvelle habitude: \"" + name + "\" (" + frequency + ", rappel " + reminderHour + "h)");
        return id;
    }

    public void checkIn(String habitId) {
        db.checkInHabit(habitId);
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();
        for (LeaPlusDatabase.HabitRow h : habits) {
            if (h.id.equals(habitId)) {
                log("✅ Check-in: \"" + h.name + "\" — Série: " + h.streak + " jours");
                // Milestone badges
                if (h.streak == 7)   { log("🏅 Badge: 7 jours !"); LeaPlusManager.get(ctx).onTaskCompleted("Série 7 jours: " + h.name, 3); }
                if (h.streak == 30)  { log("🥇 Badge: 30 jours !"); LeaPlusManager.get(ctx).onTaskCompleted("Série 30 jours: " + h.name, 5); }
                if (h.streak == 100) { log("👑 Badge: 100 jours !"); LeaPlusManager.get(ctx).onTaskCompleted("Série 100 jours: " + h.name, 10); }
                break;
            }
        }
    }

    public String getStats() {
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();
        if (habits.isEmpty()) return "🔗 Aucune habitude — ajoute ta première !";
        StringBuilder sb = new StringBuilder("🔗 STATS HABITUDES\n\n");
        for (LeaPlusDatabase.HabitRow h : habits) {
            sb.append(buildChain(h.streak)).append(" ").append(h.name).append("\n");
            sb.append("  Série: ").append(h.streak).append(" | Record: ").append(h.bestStreak).append("\n\n");
        }
        return sb.toString();
    }

    private String buildChain(int streak) {
        StringBuilder chain = new StringBuilder();
        int show = Math.min(streak, 10);
        for (int i=0; i<show; i++) chain.append("🟩");
        if (streak > 10) chain.append("(+").append(streak-10).append(")");
        return chain.length()==0 ? "⬜" : chain.toString();
    }

    public List<LeaPlusDatabase.HabitRow> getHabits() { return db.getActiveHabits(); }
}
