package com.flolov42.lea_v3.plus.gamification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import com.flolov42.lea_v3.database.LeaPlusDatabase;

public class LeaAdventureXPManager {

    private static final String CHANNEL_ID = "lea_adventure";

    private static final String[] WORLDS = {
        "Procrastination Valley", "Anxiety Mountains", "Chaos Forest",
        "Fear Swamp", "Distraction Desert", "Envy Tundra",
        "Perfectionism Peaks", "Clarity Summit", "Wisdom Isles", "Legend's Gate"
    };
    private static final String[] BOSS_NAMES = {
        "Baron Procrastinator", "Countess Anxiety", "Lord Chaos",
        "Duchess Fear", "Count Distraction", "Lady Envy",
        "The Perfectionist", "Shadow Doubt", "Eternal Wanderer", "The Final Shadow"
    };

    private final Context ctx;
    private final LeaPlusDatabase db;

    private static LeaAdventureXPManager instance;
    public static synchronized LeaAdventureXPManager get(Context ctx) {
        if (instance == null) instance = new LeaAdventureXPManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaAdventureXPManager(Context ctx) {
        this.ctx = ctx;
        this.db  = LeaPlusDatabase.get(ctx);
        createNotifChannel();
    }

    // ── Point d'entrée central ────────────────────────────────────────────────
    public void awardXP(int amount, String reason) {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        int newXp   = s.xp + amount;
        int newLevel = s.level;
        int newXpNext = s.xpNext;
        String newWorld = s.world;
        int newBoss = s.bossDefeated;
        boolean leveledUp = false;

        // Level-up en boucle (peut passer plusieurs niveaux d'un coup)
        while (newXp >= newXpNext) {
            newXp -= newXpNext;
            newLevel++;
            newXpNext = xpNeededForLevel(newLevel);
            newWorld  = worldForLevel(newLevel);
            leveledUp = true;
        }

        // Mise à jour
        db.updateChar(newLevel, newXp, newXpNext, s.hp, newWorld, newBoss, s.totalTasks + 1);

        if (leveledUp) {
            sendLevelUpNotif(newLevel, newWorld);
            // Boss toutes les 3 montées de niveau
            if (newLevel % 3 == 0) {
                triggerBossBattle(newLevel, s.bossDefeated, s.totalTasks);
            }
        }
    }

    public void defeatBoss() {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        int newBoss = s.bossDefeated + 1;
        db.addCoins(100, "Boss vaincu: " + getBossName(s.bossDefeated));
        db.updateChar(s.level, s.xp, s.xpNext, Math.min(100, s.hp + 20), s.world, newBoss, s.totalTasks);
        sendBossVictoryNotif(getBossName(s.bossDefeated));
    }

    // ── Boss ──────────────────────────────────────────────────────────────────
    private void triggerBossBattle(int level, int bossDefeated, int totalTasks) {
        String bossName = getBossName(bossDefeated);
        // Résolution automatique : plus on a accompli de tâches, plus on est fort
        // Chance de victoire = 50% + 1% par 5 tâches accomplies (max 90%)
        int winChance = Math.min(90, 50 + (totalTasks / 5));
        boolean won = (new java.util.Random().nextInt(100) + 1) <= winChance;
        if (won) {
            defeatBoss();
        } else {
            // Boss tient — HP réduit
            LeaPlusDatabase.CharStats s = db.getCharStats();
            int dmg = 10 + (level * 2);
            int newHp = Math.max(1, s.hp - dmg);
            db.updateChar(s.level, s.xp, s.xpNext, newHp, s.world, s.bossDefeated, s.totalTasks);
            sendBossWarningNotif(bossName, newHp);
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(won ? "⚔️ Boss vaincu ! " + bossName : "⚔️ Boss résiste ! " + bossName)
            .setContentText(won ? "+100 coins ! Bien joué !" : "HP réduit — accomplis des tâches pour te renforcer !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        nm.notify(7000 + bossDefeated, n.build());
    }

    private void sendBossWarningNotif(String bossName, int remainingHp) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("💔 " + bossName + " t'a blessé !")
            .setContentText("HP restants: " + remainingHp + ". Accomplis des tâches pour récupérer !")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        nm.notify(7500, n.build());
    }

    // Méthode publique pour combattre manuellement depuis l'UI
    public boolean fightBoss() {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        int winChance = Math.min(90, 50 + (s.totalTasks / 5));
        boolean won = (new java.util.Random().nextInt(100) + 1) <= winChance;
        if (won) defeatBoss();
        else {
            int newHp = Math.max(1, s.hp - 15);
            db.updateChar(s.level, s.xp, s.xpNext, newHp, s.world, s.bossDefeated, s.totalTasks);
        }
        return won;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────
    public int getCurrentLevel()   { return db.getCharStats().level; }
    public int getCurrentXP()      { return db.getCharStats().xp; }
    public int getXPToNextLevel()  { return db.getCharStats().xpNext; }
    public String getCurrentWorld(){ return db.getCharStats().world; }
    public float getProgressFraction() {
        LeaPlusDatabase.CharStats s = db.getCharStats();
        return s.xpNext > 0 ? (float) s.xp / s.xpNext : 0f;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int xpNeededForLevel(int level) {
        return level * 100;  // L1→L2=100, L2→L3=200, ...
    }
    private String worldForLevel(int level) {
        int idx = Math.min(level - 1, WORLDS.length - 1);
        return WORLDS[idx];
    }
    public static String getBossName(int bossIndex) {
        return BOSS_NAMES[bossIndex % BOSS_NAMES.length];
    }
    private void sendLevelUpNotif(int newLevel, String world) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("Niveau " + newLevel + " atteint!")
            .setContentText("Bienvenue dans: " + world)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        nm.notify(6000 + newLevel, n.build());
    }
    private void sendBossVictoryNotif(String bossName) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Boss vaincu!")
            .setContentText(bossName + " a été défait! +100 coins gagnés.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        nm.notify(6500, n.build());
    }
    private void createNotifChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Aventure Léa", NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
}
