package com.flolov42.lea_v3.routines;

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
import android.os.Build;

public class LeaRoutineNotificationManager {

    private static final String CHANNEL_ID   = "LEA_ROUTINE_CHANNEL";
    private static final String CHANNEL_NAME = "Routines Léa";
    private static final int    NOTIF_ID     = 7001;

    private final Context             ctx;
    private final NotificationManager nm;

    public LeaRoutineNotificationManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.nm  = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Indique la routine actuellement active");
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    public void showActiveRoutine(String routineName, String icon) {
        if (nm == null) return;
        nm.notify(NOTIF_ID, buildNotif(
            icon + "  Routine active : " + routineName,
            "Léa applique la routine « " + routineName + " »"));
    }

    public void showMultipleActive(int count) {
        if (nm == null) return;
        nm.notify(NOTIF_ID, buildNotif(
            "⭐  " + count + " routines actives",
            "Appuie pour gérer tes routines Léa"));
    }

    public void cancel() {
        if (nm != null) nm.cancel(NOTIF_ID);
    }

    private Notification buildNotif(String title, String text) {
        Intent tap = new Intent(ctx, LeaRoutineActivity.class);
        tap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, tap, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(ctx, CHANNEL_ID);
        } else {
            b = new Notification.Builder(ctx);
        }
        return b.setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pi)
                .build();
    }
}
