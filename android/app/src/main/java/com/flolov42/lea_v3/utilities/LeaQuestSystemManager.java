package com.flolov42.lea_v3.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import com.flolov42.lea_v3.database.LeaPlusDatabase;
import com.flolov42.lea_v3.plus.gamification.LeaAdventureXPManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class LeaQuestSystemManager {

    private static final String PREFS        = "lea_quest_system";
    private static final String KEY_LAST_GEN = "last_generation_date";
    private static final String CHANNEL_ID   = "lea_quests";

    private static final String[][] HEALTH_TEMPLATES = {
        {"Marche matinale",    "Faire 10 minutes de marche avant 9h",       "easy",   "SANTE", "50", "20", "1"},
        {"Hydratation",        "Boire 8 verres d'eau dans la journée",       "easy",   "SANTE", "40", "15", "8"},
        {"Pause active",       "Faire 5 séries de 10 squats",                "medium", "SANTE", "80", "30", "50"},
        {"Respiration 4-7-8",  "Pratiquer la technique de respiration 3x",  "easy",   "SANTE", "60", "20", "3"},
        {"Étirements du matin","5 minutes d'étirements au réveil",           "easy",   "SANTE", "50", "15", "1"},
    };
    private static final String[][] SOCIAL_TEMPLATES = {
        {"Appel bienveillant", "Appeler un proche que tu n'as pas contacté", "easy",   "SOCIAL", "70", "25", "1"},
        {"Message positif",    "Envoyer un compliment sincère à quelqu'un",  "easy",   "SOCIAL", "50", "20", "1"},
        {"Écoute active",      "Écouter quelqu'un sans l'interrompre 10min", "medium", "SOCIAL", "90", "35", "1"},
        {"Nouvelle rencontre",  "Engager la conversation avec quelqu'un",    "hard",   "SOCIAL", "120", "50", "1"},
        {"Gratitude partagée", "Remercier quelqu'un de spécifique aujourd'hui","easy", "SOCIAL", "60", "20", "1"},
    };
    private static final String[][] LEARNING_TEMPLATES = {
        {"Vocabulaire du jour", "Apprendre 5 nouveaux mots en langue étrangère","easy",  "APPRENTISSAGE", "60", "25", "5"},
        {"Lecture 20 min",      "Lire 20 minutes sans distraction",            "easy",   "APPRENTISSAGE", "70", "25", "20"},
        {"Podcast éducatif",    "Écouter 15 minutes d'un podcast informatif",  "easy",   "APPRENTISSAGE", "60", "20", "15"},
        {"Résumé d'article",    "Lire et résumer un article en 3 phrases",     "medium", "APPRENTISSAGE", "80", "30", "1"},
        {"Cours en ligne",      "Compléter 1 leçon d'une formation en ligne",  "medium", "APPRENTISSAGE", "100", "40", "1"},
    };
    private static final String[][] PRODUCTIVITY_TEMPLATES = {
        {"Pomodoro matinal",    "Compléter 2 sessions Pomodoro (25min each)",  "medium", "PRODUCTIVITE", "90", "35", "2"},
        {"Inbox zéro",         "Vider ta boîte mail principale",               "medium", "PRODUCTIVITE", "80", "30", "1"},
        {"Planification",      "Écrire ta to-do list de demain ce soir",       "easy",   "PRODUCTIVITE", "50", "20", "1"},
        {"Tâche difficile",    "Compléter ta tâche la plus redoutée en premier","hard",  "PRODUCTIVITE", "150", "60", "1"},
        {"Ordre du bureau",    "Ranger et nettoyer ton espace de travail",     "easy",   "PRODUCTIVITE", "60", "25", "1"},
    };
    private static final String[][] MINDFULNESS_TEMPLATES = {
        {"Méditation 5min",    "5 minutes de méditation guidée ou silencieuse","easy",   "PLEINE_CONSCIENCE", "70", "25", "5"},
        {"Journal intime",     "Écrire 3 choses dont tu es reconnaissant·e",  "easy",   "PLEINE_CONSCIENCE", "60", "20", "3"},
        {"Déconnexion digitale","1 heure sans écran dans la soirée",          "medium", "PLEINE_CONSCIENCE", "90", "35", "60"},
        {"Repas conscient",    "Manger un repas sans téléphone ni TV",        "easy",   "PLEINE_CONSCIENCE", "70", "25", "1"},
        {"Body scan",          "Pratiquer un scan corporel de 10 minutes",    "easy",   "PLEINE_CONSCIENCE", "80", "30", "1"},
    };

    private static final String[] WORLDS = {
        "Procrastination Valley", "Anxiety Mountains", "Chaos Forest",
        "Fear Swamp", "Distraction Desert", "Envy Tundra",
        "Perfectionism Peaks", "Clarity Summit", "Wisdom Isles", "Legend's Gate"
    };

    private final Context ctx;
    private final LeaPlusDatabase db;
    private final SharedPreferences prefs;

    private static LeaQuestSystemManager instance;
    public static synchronized LeaQuestSystemManager get(Context ctx) {
        if (instance == null) instance = new LeaQuestSystemManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaQuestSystemManager(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaPlusDatabase.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        createNotifChannel();
    }

    // ── Génération journalière ────────────────────────────────────────────────
    public void generateDailyQuests() {
        String today = todayKey();
        if (today.equals(prefs.getString(KEY_LAST_GEN, ""))) return;

        Random rnd = new Random();
        String[][][][] allCats = {
            {HEALTH_TEMPLATES}, {SOCIAL_TEMPLATES}, {LEARNING_TEMPLATES},
            {PRODUCTIVITY_TEMPLATES}, {MINDFULNESS_TEMPLATES}
        };

        for (String[][][] catBlock : allCats) {
            String[][] templates = catBlock[0];
            List<String[]> shuffled = new ArrayList<>();
            for (String[] t : templates) shuffled.add(t);
            Collections.shuffle(shuffled, rnd);

            // 2 quêtes par catégorie
            for (int i = 0; i < Math.min(2, shuffled.size()); i++) {
                String[] t  = shuffled.get(i);
                String title = t[0], desc = t[1], diff = t[2], cat = t[3];
                int xp = Integer.parseInt(t[4]), coins = Integer.parseInt(t[5]), target = Integer.parseInt(t[6]);
                // ID unique avec date pour idempotence
                String id = "daily_" + today + "_" + cat + "_" + i;
                db.insertQuest(id, title, desc, cat, diff, xp, coins, target);
            }
        }
        prefs.edit().putString(KEY_LAST_GEN, today).apply();
    }

    // ── Compléter une quête ───────────────────────────────────────────────────
    public void completeQuest(String questId) {
        List<LeaPlusDatabase.QuestRow> quests = db.getQuests("available");
        for (LeaPlusDatabase.QuestRow q : quests) {
            if (q.id.equals(questId)) {
                db.updateQuestProgress(questId, q.target, "completed");
                db.addCoins(q.coins, "Quête: " + q.title);
                LeaAdventureXPManager.get(ctx).awardXP(q.xp, "Quête: " + q.title);
                sendQuestNotif(q.title, q.xp, q.coins);
                return;
            }
        }
    }

    // ── Stats aujourd'hui ─────────────────────────────────────────────────────
    public int[] getDailyStats() {
        List<LeaPlusDatabase.QuestRow> all = db.getQuests(null);
        int total = 0, done = 0;
        String today = todayKey();
        for (LeaPlusDatabase.QuestRow q : all) {
            if (q.id.startsWith("daily_" + today)) {
                total++;
                if ("completed".equals(q.status)) done++;
            }
        }
        return new int[]{done, total};
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void sendQuestNotif(String questTitle, int xp, int coins) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Quête accomplie!")
            .setContentText(questTitle + " • +" + xp + " XP • +" + coins + " coins")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        nm.notify((int)(System.currentTimeMillis() % 90000) + 9000, n.build());
    }
    private void createNotifChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Quêtes Léa", NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
