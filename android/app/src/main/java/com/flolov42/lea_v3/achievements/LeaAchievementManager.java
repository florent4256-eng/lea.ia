package com.flolov42.lea_v3.achievements;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class LeaAchievementManager {

    private static final String CHANNEL_ID = "lea_achievements";
    private static LeaAchievementManager instance;

    private final Context ctx;

    public static synchronized LeaAchievementManager get(Context ctx) {
        if (instance == null) instance = new LeaAchievementManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaAchievementManager(Context ctx) {
        this.ctx = ctx;
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                "Achievements Léa", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Célèbre tes achievements débloqués");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public void checkAll() {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);
        List<LeaFeaturesDatabase.Achievement> all = db.getAllAchievements();

        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        int coins = plus.getCoinBalance();
        List<LeaPlusDatabase.HabitRow> habits = plus.getActiveHabits();
        int maxStreak = 0;
        for (LeaPlusDatabase.HabitRow h : habits) if (h.streak > maxStreak) maxStreak = h.streak;
        int installedSkills = plus.getInstalledSkills().size();

        for (LeaFeaturesDatabase.Achievement a : all) {
            if (a.unlocked == 1) continue;
            int progress = 0;
            switch (a.conditionType) {
                case "HABIT_CREATE": progress = habits.size(); break;
                case "QUEST_COMPLETE":
                    List<LeaPlusDatabase.QuestRow> done = plus.getQuests("completed");
                    progress = done.size(); break;
                case "SKILL_BUY": progress = installedSkills; break;
                case "STREAK_7":  case "STREAK_30": case "STREAK_100": progress = maxStreak; break;
                case "XP_50":  case "XP_500": case "XP_5000": progress = stats.xp; break;
                case "COINS_10": progress = coins; break;
                case "LEVEL_5": progress = stats.level; break;
                case "SKILLS_10": progress = installedSkills; break;
                case "BIOMETRIC_ON":
                    LeaFeaturesDatabase.BiometricCfg bio = db.getBiometricConfig();
                    progress = bio.enabled; break;
                case "DARK_MODE":
                    LeaFeaturesDatabase.ThemeCfg theme = db.getThemeConfig();
                    progress = (theme.currentTheme != null && !theme.currentTheme.equals("galaxie")) ? 1 : 0; break;
                default: continue;
            }
            db.updateAchievementProgress(a.id, progress);
            if (progress >= a.target) {
                boolean justUnlocked = db.unlockAchievement(a.id);
                if (justUnlocked) sendUnlockNotification(a.name, a.description, a.icon);
            }
        }
    }

    public void trigger(String conditionType) {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        List<LeaFeaturesDatabase.Achievement> all = db.getAllAchievements();
        for (LeaFeaturesDatabase.Achievement a : all) {
            if (a.unlocked == 1 || !a.conditionType.equals(conditionType)) continue;
            int newProgress = a.progress + 1;
            db.updateAchievementProgress(a.id, newProgress);
            if (newProgress >= a.target) {
                boolean justUnlocked = db.unlockAchievement(a.id);
                if (justUnlocked) sendUnlockNotification(a.name, a.description, a.icon);
            }
        }
    }

    private void sendUnlockNotification(String name, String desc, String icon) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(icon + " Achievement débloqué!")
            .setContentText(name + " — " + desc)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH);
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    public String getSummary() {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        int unlocked = db.getUnlockedCount();
        int total = db.getAllAchievements().size();
        return unlocked + "/" + total + " débloqués";
    }
}
