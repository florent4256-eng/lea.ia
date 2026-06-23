package com.flolov42.lea_v3.analytics;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.text.SimpleDateFormat;
import java.util.*;

public class LeaAnalyticsWorker extends Worker {

    public LeaAnalyticsWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(new Date());
            LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
            LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);

            int eventsToday = db.getTodayEventCount(null);
            LeaPlusDatabase.CharStats stats = plus.getCharStats();
            List<LeaPlusDatabase.HabitRow> habits = plus.getActiveHabits();
            long dayStart = (System.currentTimeMillis() / 86400_000L) * 86400_000L;
            int habitsCompleted = 0;
            for (LeaPlusDatabase.HabitRow h : habits) if (h.lastCheck >= dayStart) habitsCompleted++;

            db.upsertDailyStat(today, eventsToday, 0, stats.xp, habitsCompleted);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
