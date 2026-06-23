package com.flolov42.lea_v3.backup;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LeaBackupWorker extends Worker {

    public LeaBackupWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        final boolean[] success = {false};
        final Object lock = new Object();

        LeaBackupManager.get(getApplicationContext()).createBackup(new LeaBackupManager.BackupListener() {
            @Override public void onProgress(int percent, String status) {}
            @Override public void onComplete(String path, long sizeBytes) {
                success[0] = true;
                synchronized(lock) { lock.notifyAll(); }
            }
            @Override public void onError(String error) {
                synchronized(lock) { lock.notifyAll(); }
            }
        });

        synchronized(lock) {
            try { lock.wait(60_000); } catch (InterruptedException ignored) {}
        }
        return success[0] ? Result.success() : Result.retry();
    }
}
