package com.flolov42.lea_v3.utilities;

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
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Exécution horaire — envoie le digest groupé des notifications en batch
 * et exécute le pipeline LeaSmartNotifications.
 */
public class LeaNotificationWorker extends Worker {

    public LeaNotificationWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();
            // Ne rien faire si la feature Smart Notifications est désactivée
            if (!LeaPlusManager.get(ctx).isEnabled(LeaPlusDatabase.SMART_NOTIF)) {
                return Result.success();
            }
            LeaSmartNotifications smartNotifs = new LeaSmartNotifications(ctx);
            smartNotifs.execute();
            return Result.success();
        } catch (Exception e) {
            LeaPlusDatabase.get(getApplicationContext())
                .log(LeaPlusDatabase.SMART_NOTIF, "⚠️ Worker erreur: " + e.getMessage());
            return Result.failure();
        }
    }
}
