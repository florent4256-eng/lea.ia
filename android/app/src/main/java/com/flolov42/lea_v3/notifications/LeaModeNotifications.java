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

public class LeaModeNotifications {

    private static final String CHANNEL_ID   = "lea_modes";
    private static final String CHANNEL_NAME = "LÉA Modes Innovants";
    private static final int    BASE_ID      = 9000;

    private static LeaModeNotifications instance;
    public static synchronized LeaModeNotifications get(Context ctx) {
        if (instance == null) instance = new LeaModeNotifications(ctx.getApplicationContext());
        return instance;
    }

    private final Context             ctx;
    private final NotificationManager nm;
    private       int                 nextId = BASE_ID;

    private LeaModeNotifications(Context ctx) {
        this.ctx = ctx;
        this.nm  = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Notifications des modes innovants LÉA");
        ch.enableLights(true);
        ch.setLightColor(Color.CYAN);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    public void notify(String modeId, String title, String message) {
        Intent intent = new Intent(ctx, LeaAgentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(0xFF00E5FF);

        nm.notify(getIdFor(modeId), builder.build());
    }

    public void cancel(String modeId) {
        nm.cancel(getIdFor(modeId));
    }

    private int getIdFor(String modeId) {
        switch (modeId) {
            case LeaModeDatabase.DUPLICATE:     return BASE_ID;
            case LeaModeDatabase.MENTAL_HEALTH: return BASE_ID + 1;
            case LeaModeDatabase.INTERVIEW:     return BASE_ID + 2;
            case LeaModeDatabase.VOICE_BIO:     return BASE_ID + 3;
            case LeaModeDatabase.FUTURE:        return BASE_ID + 4;
            case LeaModeDatabase.DREAM:         return BASE_ID + 5;
            case LeaModeDatabase.ALTER_EGO:     return BASE_ID + 6;
            case LeaModeDatabase.NEGOTIATION:   return BASE_ID + 7;
            case LeaModeDatabase.RELATIONS:     return BASE_ID + 8;
            case LeaModeDatabase.CREATIVE:      return BASE_ID + 9;
            default:                            return nextId++;
        }
    }
}
