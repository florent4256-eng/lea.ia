package com.flolov42.lea_v3.utilities;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.flolov42.lea_v3.plus.premium.LeaParentalControlManager;

public class LeaParentalControlWorker extends Worker {

    public LeaParentalControlWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            LeaParentalControlManager pcm = LeaParentalControlManager.get(getApplicationContext());
            if (pcm.isChildSessionActive()) {
                pcm.checkAndNotifyScreenTime();
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
