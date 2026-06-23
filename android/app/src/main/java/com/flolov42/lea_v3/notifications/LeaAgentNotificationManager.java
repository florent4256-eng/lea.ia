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
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class LeaAgentNotificationManager {

    public static final String CHANNEL_AGENTS  = "lea_agents";
    public static final String CHANNEL_SERVICE = "lea_agent_service";

    private static final int NOTIF_SERVICE = 8000;
    private static int       notifCounter  = 8001;

    private final Context             ctx;
    private final NotificationManager nm;

    private static LeaAgentNotificationManager instance;

    public static synchronized LeaAgentNotificationManager get(Context ctx) {
        if (instance == null) instance = new LeaAgentNotificationManager(ctx.getApplicationContext());
        return instance;
    }

    private LeaAgentNotificationManager(Context ctx) {
        this.ctx = ctx;
        this.nm  = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel agents = new NotificationChannel(
                CHANNEL_AGENTS, "Léa Agents", NotificationManager.IMPORTANCE_DEFAULT);
            agents.setDescription("Notifications des agents intelligents");

            NotificationChannel service = new NotificationChannel(
                CHANNEL_SERVICE, "Léa Agent Service", NotificationManager.IMPORTANCE_LOW);
            service.setDescription("Service de fond des agents");

            nm.createNotificationChannel(agents);
            nm.createNotificationChannel(service);
        }
    }

    /** Foreground service persistent notification */
    public Notification buildServiceNotification() {
        Intent tap = new Intent(ctx, LeaAgentActivity.class);
        tap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, tap,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(ctx, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🤖 Léa Agents Actifs")
            .setContentText("Agents intelligents en cours d'exécution")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    public int getServiceNotifId() { return NOTIF_SERVICE; }

    /** Send a notification from an agent */
    public void notify(String agentId, String title, String message) {
        Intent tap = new Intent(ctx, LeaAgentActivity.class);
        tap.putExtra("agent_id", agentId);
        tap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, notifCounter, tap,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(ctx, CHANNEL_AGENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();

        nm.notify(notifCounter++, notif);
    }
}
