package com.flolov42.lea_v3.offline;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LeaSyncWorker extends Worker {

    public LeaSyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            LeaOfflineManager mgr = LeaOfflineManager.get(getApplicationContext());
            if (mgr.isOnline()) {
                mgr.syncPending();
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
