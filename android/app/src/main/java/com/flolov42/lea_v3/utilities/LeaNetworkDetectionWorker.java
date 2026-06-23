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
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Periodic worker (30 minutes) that re-probes both server endpoints.
 * On type change, broadcasts ACTION_NETWORK_CHANGED so all listeners update.
 */
public class LeaNetworkDetectionWorker extends Worker {

    public static final String ACTION_NETWORK_CHANGED = "com.flolov42.lea_v3.NETWORK_CHANGED";
    public static final String EXTRA_TYPE             = "connection_type";
    public static final String EXTRA_LATENCY          = "latency_ms";

    public LeaNetworkDetectionWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        String prevType = LeaNetworkDetector.getCachedType(ctx);
        String newType  = LeaNetworkDetector.probeSync(ctx);

        if (!newType.equals(prevType)) {
            LeaNetworkLogger.get(ctx).switchEvent(prevType, newType, "scheduled_probe");
            // Broadcast so LeaBixbyService can reconnect if needed
            Intent broadcast = new Intent(ACTION_NETWORK_CHANGED);
            broadcast.putExtra(EXTRA_TYPE, newType);
            broadcast.putExtra(EXTRA_LATENCY, LeaNetworkDetector.getCachedLatency(ctx));
            ctx.sendBroadcast(broadcast);
        }
        return Result.success();
    }
}
