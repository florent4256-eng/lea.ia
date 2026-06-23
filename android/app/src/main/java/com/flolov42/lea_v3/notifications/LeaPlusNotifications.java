package com.flolov42.lea_v3.notifications;

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


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class LeaPlusNotifications {

    private static final String CHANNEL_GENERAL  = "lea_plus_general";
    private static final String CHANNEL_QUEST     = "lea_plus_quest";
    private static final String CHANNEL_GAMIF     = "lea_plus_gamif";
    private static final int    BASE_ID           = 10000;

    private static LeaPlusNotifications instance;
    public static synchronized LeaPlusNotifications get(Context ctx) {
        if (instance == null) instance = new LeaPlusNotifications(ctx.getApplicationContext());
        return instance;
    }

    private final Context             ctx;
    private final NotificationManager nm;

    private LeaPlusNotifications(Context ctx) {
        this.ctx = ctx;
        this.nm  = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        createChannel(CHANNEL_GENERAL, "LÉA Plus — Général",   NotificationManager.IMPORTANCE_DEFAULT, Color.CYAN);
        createChannel(CHANNEL_QUEST,   "LÉA Plus — Quêtes",    NotificationManager.IMPORTANCE_DEFAULT, 0xFFFFD700);
        createChannel(CHANNEL_GAMIF,   "LÉA Plus — Gamification", NotificationManager.IMPORTANCE_HIGH, 0xFF7B2CBF);
    }

    private void createChannel(String id, String name, int importance, int lightColor) {
        NotificationChannel ch = new NotificationChannel(id, name, importance);
        ch.enableLights(true); ch.setLightColor(lightColor); ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    public void notify(String featureId, String title, String message) {
        Intent intent = new Intent(ctx, LeaPlusActivity.class);
        intent.putExtra("feature_id", featureId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, getIdFor(featureId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channel = featureId.equals(LeaPlusDatabase.QUESTS) || featureId.equals(LeaPlusDatabase.ADVENTURE)
            ? CHANNEL_GAMIF : CHANNEL_GENERAL;

        Notification n = new Notification.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(0xFF00D9FF)
            .build();

        nm.notify(getIdFor(featureId), n);
    }

    public void notifyQuestComplete(String questTitle, int xp, int coins) {
        notify(LeaPlusDatabase.QUESTS,
            "🏆 Quête accomplie !",
            "\"" + questTitle + "\" terminée ! +"+xp+" XP, +"+coins+" coins 🎉");
    }

    public void notifyLevelUp(int newLevel, String world) {
        notify(LeaPlusDatabase.ADVENTURE,
            "⚔️ LEVEL UP ! Niveau " + newLevel,
            "Tu es maintenant dans : " + world + ". Continue l'aventure !");
    }

    public void notifyHabitReminder(String habitName, int streak) {
        notify(LeaPlusDatabase.HABITS,
            "🔗 N'oublie pas : " + habitName,
            "Série actuelle : " + streak + " jours. Ne casse pas la chaîne !");
    }

    public void notifyCoinMilestone(int balance) {
        notify(LeaPlusDatabase.COINS,
            "💰 " + balance + " Léa Coins !",
            "Tu accumules bien ! Visite la boutique pour dépenser tes coins.");
    }

    public void cancel(String featureId) { nm.cancel(getIdFor(featureId)); }

    private int getIdFor(String featureId) {
        switch (featureId) {
            case LeaPlusDatabase.QUESTS:      return BASE_ID;
            case LeaPlusDatabase.ADVENTURE:   return BASE_ID + 1;
            case LeaPlusDatabase.COINS:       return BASE_ID + 2;
            case LeaPlusDatabase.HABITS:      return BASE_ID + 3;
            case LeaPlusDatabase.REPORT:      return BASE_ID + 4;
            case LeaPlusDatabase.COMPANION:   return BASE_ID + 5;
            case LeaPlusDatabase.LIFE_OS:     return BASE_ID + 6;
            case LeaPlusDatabase.STUDENT:     return BASE_ID + 7;
            case LeaPlusDatabase.LANGUAGE:    return BASE_ID + 8;
            case LeaPlusDatabase.SMART_NOTIF: return BASE_ID + 9;
            case LeaPlusDatabase.CLOUD_SYNC:  return BASE_ID + 10;
            case LeaPlusDatabase.MARKETPLACE: return BASE_ID + 11;
            case LeaPlusDatabase.FAMILY:      return BASE_ID + 12;
            case LeaPlusDatabase.OMNICHANNEL: return BASE_ID + 13;
            case LeaPlusDatabase.STREAMING:   return BASE_ID + 14;
            default:                          return BASE_ID + 99;
        }
    }
}
