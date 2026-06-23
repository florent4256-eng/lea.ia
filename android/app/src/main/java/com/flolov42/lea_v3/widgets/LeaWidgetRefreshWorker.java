package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LeaWidgetRefreshWorker extends Worker {

    public LeaWidgetRefreshWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);

            // Refresh all 4 widget types
            int[] habitIds = mgr.getAppWidgetIds(new ComponentName(ctx, LeaWidgetHabitProvider.class));
            for (int id : habitIds) LeaWidgetHabitProvider.updateWidget(ctx, mgr, id);

            int[] xpIds = mgr.getAppWidgetIds(new ComponentName(ctx, LeaWidgetXPProvider.class));
            for (int id : xpIds) LeaWidgetXPProvider.updateWidget(ctx, mgr, id);

            int[] questIds = mgr.getAppWidgetIds(new ComponentName(ctx, LeaWidgetQuestsProvider.class));
            for (int id : questIds) LeaWidgetQuestsProvider.updateWidget(ctx, mgr, id);

            int[] coinIds = mgr.getAppWidgetIds(new ComponentName(ctx, LeaWidgetCoinsProvider.class));
            for (int id : coinIds) LeaWidgetCoinsProvider.updateWidget(ctx, mgr, id);

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
