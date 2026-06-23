package com.flolov42.lea_v3.core;

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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.flolov42.lea_v3.widgets.LeaWidgetRefreshWorker;
import com.flolov42.lea_v3.backup.LeaBackupWorker;
import com.flolov42.lea_v3.offline.LeaSyncWorker;
import com.flolov42.lea_v3.analytics.LeaAnalyticsWorker;
import com.flolov42.lea_v3.utilities.LeaNetworkDetectionWorker;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class LeaWorkScheduler {

    public static void schedule(Context ctx) {
        WorkManager wm = WorkManager.getInstance(ctx);

        // Vérification quotidienne des habitudes — 24h, premier run à minuit
        wm.enqueueUniquePeriodicWork(
            "lea_habits",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaHabitCheckWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayUntilMidnight(), TimeUnit.MILLISECONDS)
                .build()
        );

        // Rafraîchissement des quêtes quotidiennes — 24h, premier run à 6h du matin
        wm.enqueueUniquePeriodicWork(
            "lea_quests",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaQuestRefreshWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayUntil6am(), TimeUnit.MILLISECONDS)
                .build()
        );

        // Digest notifications groupées — toutes les heures
        wm.enqueueUniquePeriodicWork(
            "lea_notifs",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaNotificationWorker.class, 1, TimeUnit.HOURS)
                .build()
        );

        // Contrôle parental — vérification horaire (temps écran + couvre-feu)
        wm.enqueueUniquePeriodicWork(
            "lea_parental_control",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaParentalControlWorker.class, 1, TimeUnit.HOURS)
                .build()
        );

        // Leçon de langue quotidienne — livraison à 8h
        wm.enqueueUniquePeriodicWork(
            "lea_language_lesson",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaLanguageLessonWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayUntil8am(), TimeUnit.MILLISECONDS)
                .build()
        );

        // Rafraîchissement widgets — toutes les heures
        wm.enqueueUniquePeriodicWork(
            "lea_widgets",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaWidgetRefreshWorker.class, 1, TimeUnit.HOURS)
                .build()
        );

        // Backup automatique quotidien — 2h du matin
        wm.enqueueUniquePeriodicWork(
            "lea_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaBackupWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayUntil2am(), TimeUnit.MILLISECONDS)
                .build()
        );

        // Synchronisation offline — toutes les 30 minutes quand online
        wm.enqueueUniquePeriodicWork(
            "lea_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaSyncWorker.class, 30, TimeUnit.MINUTES)
                .build()
        );

        // Agrégation analytics quotidienne — minuit
        wm.enqueueUniquePeriodicWork(
            "lea_analytics",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaAnalyticsWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayUntilMidnight(), TimeUnit.MILLISECONDS)
                .build()
        );

        // Détection réseau (LOCAL vs CLOUDFLARE) — toutes les 30 minutes
        wm.enqueueUniquePeriodicWork(
            "lea_network_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(LeaNetworkDetectionWorker.class, 30, TimeUnit.MINUTES)
                .build()
        );
    }

    private static long delayUntilMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = (Calendar) now.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }

    private static long delayUntil6am() {
        Calendar now = Calendar.getInstance();
        Calendar sixAm = (Calendar) now.clone();
        sixAm.set(Calendar.HOUR_OF_DAY, 6);
        sixAm.set(Calendar.MINUTE, 0);
        sixAm.set(Calendar.SECOND, 0);
        sixAm.set(Calendar.MILLISECOND, 0);
        if (sixAm.before(now)) sixAm.add(Calendar.DAY_OF_MONTH, 1);
        return sixAm.getTimeInMillis() - now.getTimeInMillis();
    }

    private static long delayUntil8am() {
        Calendar now   = Calendar.getInstance();
        Calendar eightAm = (Calendar) now.clone();
        eightAm.set(Calendar.HOUR_OF_DAY, 8);
        eightAm.set(Calendar.MINUTE, 0);
        eightAm.set(Calendar.SECOND, 0);
        eightAm.set(Calendar.MILLISECOND, 0);
        if (eightAm.before(now)) eightAm.add(Calendar.DAY_OF_MONTH, 1);
        return eightAm.getTimeInMillis() - now.getTimeInMillis();
    }

    private static long delayUntil2am() {
        Calendar now = Calendar.getInstance();
        Calendar twoAm = (Calendar) now.clone();
        twoAm.set(Calendar.HOUR_OF_DAY, 2);
        twoAm.set(Calendar.MINUTE, 0);
        twoAm.set(Calendar.SECOND, 0);
        twoAm.set(Calendar.MILLISECOND, 0);
        if (twoAm.before(now)) twoAm.add(Calendar.DAY_OF_MONTH, 1);
        return twoAm.getTimeInMillis() - now.getTimeInMillis();
    }
}
