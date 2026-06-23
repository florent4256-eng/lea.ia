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

public class LeaAnnualReport extends LeaBasePlusFeature {

    public LeaAnnualReport(Context ctx) { super(ctx, LeaPlusDatabase.REPORT); }

    @Override
    public void execute() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        // Collect cross-feature stats on each cycle
        collectCurrentStats(year);
        log("📊 Stats de l'année " + year + " mises à jour");
    }

    private void collectCurrentStats(int year) {
        // Habits stats
        List<LeaPlusDatabase.HabitRow> habits = LeaPlusDatabase.get(ctx).getActiveHabits();
        int totalStreak = 0, bestStreak = 0;
        for (LeaPlusDatabase.HabitRow h : habits) {
            totalStreak += h.streak;
            if (h.streak > bestStreak) bestStreak = h.streak;
        }
        db.setStat(year, "habits_count",       String.valueOf(habits.size()));
        db.setStat(year, "total_streak",        String.valueOf(totalStreak));
        db.setStat(year, "best_streak",         String.valueOf(bestStreak));

        // Quests stats
        List<LeaPlusDatabase.QuestRow> completed = LeaPlusDatabase.get(ctx).getQuests("completed");
        db.setStat(year, "quests_completed",    String.valueOf(completed.size()));

        // Coins stats
        int coins = LeaPlusDatabase.get(ctx).getCoinBalance();
        db.setStat(year, "coins_total",         String.valueOf(coins));

        // Character stats
        LeaPlusDatabase.CharStats charStats = LeaPlusDatabase.get(ctx).getCharStats();
        db.setStat(year, "character_level",     String.valueOf(charStats.level));
        db.setStat(year, "total_xp",            String.valueOf(charStats.xp));
        db.setStat(year, "tasks_completed",     String.valueOf(charStats.totalTasks));

        // Language stats
        db.setStat(year, "words_learned",       String.valueOf(LeaPlusDatabase.get(ctx).getVocabCount(null)));
    }

    public String generateReport() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        collectCurrentStats(year);
        return buildReportText(year);
    }

    private String buildReportText(int year) {
        String habitCount    = safe(db.getStat(year, "habits_count"), "0");
        String bestStreak    = safe(db.getStat(year, "best_streak"), "0");
        String questsDone    = safe(db.getStat(year, "quests_completed"), "0");
        String coins         = safe(db.getStat(year, "coins_total"), "0");
        String charLevel     = safe(db.getStat(year, "character_level"), "1");
        String totalXp       = safe(db.getStat(year, "total_xp"), "0");
        String tasksDone     = safe(db.getStat(year, "tasks_completed"), "0");
        String wordsLearned  = safe(db.getStat(year, "words_learned"), "0");

        return "✨ TON RAPPORT LÉA " + year + " ✨\n\n" +
               "🔗 Habitudes actives: " + habitCount + "\n" +
               "🏆 Meilleure série: " + bestStreak + " jours\n" +
               "🎯 Quêtes complétées: " + questsDone + "\n" +
               "💰 Léa Coins gagnés: " + coins + "\n" +
               "⚔️ Niveau personnage: " + charLevel + "\n" +
               "⭐ XP total: " + totalXp + "\n" +
               "✅ Tâches accomplies: " + tasksDone + "\n" +
               "🌐 Mots appris: " + wordsLearned + "\n\n" +
               "📈 " + computeGlobalScore(bestStreak, questsDone, tasksDone) + "\n\n" +
               "Partage ton rapport avec tes amis ! 🎉";
    }

    private String computeGlobalScore(String streak, String quests, String tasks) {
        try {
            int s = Integer.parseInt(streak);
            int q = Integer.parseInt(quests);
            int t = Integer.parseInt(tasks);
            int score = (s * 2) + (q * 3) + t;
            if (score > 500)  return "Score: " + score + " — LÉGENDE ! 👑";
            if (score > 200)  return "Score: " + score + " — Excellent ! 🌟";
            if (score > 50)   return "Score: " + score + " — Bien joué ! 👍";
            return "Score: " + score + " — Continue ! 💪";
        } catch (Exception e) { return "Score en cours de calcul…"; }
    }

    public String getShareText() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String report = generateReport();
        return report + "\n\n#LéaAI #MonAnnée" + year + " #IA";
    }

    private String safe(String val, String def) { return val != null ? val : def; }
}
