package com.flolov42.lea_v3.plus.gamification;

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
import java.util.UUID;

public class LeaQuestSystem extends LeaBasePlusFeature {

    private static final String PREFS = "lea_quest_system";

    private static final String[][] DAILY_QUEST_POOL = {
        {"Méditer 10 minutes",         "Health",      "Easy",   "10",  "5",  "1"},
        {"Boire 8 verres d'eau",        "Health",      "Easy",   "8",   "5",  "1"},
        {"Faire 30 min de sport",       "Health",      "Medium", "25",  "15", "1"},
        {"Appeler un ami/famille",      "Social",      "Easy",   "10",  "5",  "1"},
        {"Écrire dans un journal",      "Productivity","Easy",   "8",   "5",  "1"},
        {"Apprendre 10 mots nouveaux",  "Learning",    "Medium", "20",  "10", "1"},
        {"Lire 20 pages",               "Learning",    "Medium", "18",  "10", "1"},
        {"Compléter 3 tâches du jour",  "Productivity","Medium", "25",  "15", "3"},
        {"Aucun écran après 22h",       "Health",      "Hard",   "35",  "20", "1"},
        {"Faire une sieste de 20min",   "Health",      "Easy",   "10",  "5",  "1"},
    };

    private static final String[][] WEEKLY_QUEST_POOL = {
        {"Méditer 3 fois cette semaine",     "Health",      "Medium", "60",  "30", "3"},
        {"Appeler 3 amis différents",        "Social",      "Hard",   "80",  "40", "3"},
        {"Apprendre 50 nouveaux mots",       "Learning",    "Hard",   "100", "50", "50"},
        {"7 jours sans sucre",               "Health",      "Hard",   "120", "60", "7"},
        {"Terminer un projet personnel",     "Productivity","Hard",   "100", "50", "1"},
        {"Lire un livre entier",             "Learning",    "Hard",   "150", "75", "1"},
    };

    public LeaQuestSystem(Context ctx) { super(ctx, LeaPlusDatabase.QUESTS); }

    @Override
    public void execute() {
        generateDailyQuestsIfNeeded();
        checkExpiredQuests();
        checkDailyLogin();
    }

    private void generateDailyQuestsIfNeeded() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (today.equals(prefs.getString("last_quest_gen", ""))) return;

        // Generate 3 daily quests
        int[] indices = pickRandom(DAILY_QUEST_POOL.length, 3);
        for (int idx : indices) {
            String[] q = DAILY_QUEST_POOL[idx];
            String id = "daily_" + today + "_" + idx;
            db.insertQuest(id, q[0], "Quête quotidienne: " + q[0], q[1], q[2],
                Integer.parseInt(q[3]), Integer.parseInt(q[4]), Integer.parseInt(q[5]));
        }

        // Generate 1 weekly quest on Mondays
        if (new java.util.Calendar.Builder().setInstant(new Date()).build().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
            String weekKey = new SimpleDateFormat("yyyyWW", Locale.getDefault()).format(new Date());
            if (!weekKey.equals(prefs.getString("last_weekly_quest", ""))) {
                int widx = (int)(System.currentTimeMillis() % WEEKLY_QUEST_POOL.length);
                String[] wq = WEEKLY_QUEST_POOL[widx];
                String wid = "weekly_" + weekKey;
                db.insertQuest(wid, wq[0], "Quête hebdomadaire: " + wq[0], wq[1], wq[2],
                    Integer.parseInt(wq[3]), Integer.parseInt(wq[4]), Integer.parseInt(wq[5]));
                prefs.edit().putString("last_weekly_quest", weekKey).apply();
            }
        }

        prefs.edit().putString("last_quest_gen", today).apply();
        log("🎯 " + indices.length + " quête(s) générée(s) pour aujourd'hui");
        notify("🎯 Nouvelles quêtes disponibles !",
            "Tu as " + indices.length + " nouvelles quêtes aujourd'hui. Prêt à les accomplir ?");
    }

    public boolean completeQuest(String questId) {
        List<LeaPlusDatabase.QuestRow> quests = db.getQuests(null);
        for (LeaPlusDatabase.QuestRow q : quests) {
            if (q.id.equals(questId) && !"completed".equals(q.status)) {
                db.updateQuestProgress(q.id, q.target, "completed");
                // Award XP + coins via manager
                LeaPlusManager.get(ctx).onTaskCompleted(q.title, difficultyToLevel(q.difficulty));
                notif.notifyQuestComplete(q.title, q.xp, q.coins);
                log("✅ Quête complétée: \"" + q.title + "\" | +" + q.xp + " XP, +" + q.coins + " coins");
                return true;
            }
        }
        return false;
    }

    public void updateQuestProgress(String questId, int progressDelta) {
        List<LeaPlusDatabase.QuestRow> quests = db.getQuests("available");
        for (LeaPlusDatabase.QuestRow q : quests) {
            if (q.id.equals(questId)) {
                int newProg = q.progress + progressDelta;
                if (newProg >= q.target) { completeQuest(questId); }
                else { db.updateQuestProgress(questId, newProg, "in_progress"); }
                return;
            }
        }
    }

    private void checkExpiredQuests() {
        // Daily quests expire after 24h — mark as failed
        long yesterday = System.currentTimeMillis() - 86400_000L;
        String yest = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date(yesterday));
        List<LeaPlusDatabase.QuestRow> quests = db.getQuests("available");
        int expired = 0;
        for (LeaPlusDatabase.QuestRow q : quests) {
            if (q.id.startsWith("daily_" + yest)) {
                db.updateQuestProgress(q.id, q.progress, "expired");
                expired++;
            }
        }
        if (expired > 0) log("⌛ " + expired + " quête(s) expirée(s)");
    }

    private void checkDailyLogin() {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        if (!today.equals(prefs.getString("last_login_bonus", ""))) {
            new LeaCoinSystem(ctx).addCoins(5, "Connexion quotidienne");
            prefs.edit().putString("last_login_bonus", today).apply();
            log("🎁 +5 coins bonus connexion quotidienne");
        }
    }

    public List<LeaPlusDatabase.QuestRow> getActiveQuests() { return db.getQuests("available"); }
    public List<LeaPlusDatabase.QuestRow> getInProgressQuests() { return db.getQuests("in_progress"); }
    public List<LeaPlusDatabase.QuestRow> getCompletedQuests() { return db.getQuests("completed"); }

    public String getQuestSummary() {
        int active    = getActiveQuests().size();
        int completed = getCompletedQuests().size();
        return "🎯 " + active + " quête(s) disponible(s) | ✅ " + completed + " complétée(s)";
    }

    private int difficultyToLevel(String diff) {
        if ("Hard".equalsIgnoreCase(diff))   return 4;
        if ("Medium".equalsIgnoreCase(diff)) return 2;
        return 1;
    }

    private int[] pickRandom(int max, int count) {
        count = Math.min(count, max); // évite boucle infinie si count > max
        java.util.Set<Integer> picked = new java.util.HashSet<>();
        java.util.Random rnd = new java.util.Random();
        while (picked.size() < count) picked.add(rnd.nextInt(max));
        int[] arr = new int[picked.size()]; int i=0;
        for (int v : picked) arr[i++] = v;
        return arr;
    }
}
