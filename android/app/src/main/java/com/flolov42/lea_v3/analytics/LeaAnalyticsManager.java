package com.flolov42.lea_v3.analytics;

import com.flolov42.lea_v3.database.*;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaAnalyticsManager {

    private static LeaAnalyticsManager instance;
    private final Context ctx;
    private long featureStartTs = 0;
    private String currentFeature = "";

    public static synchronized LeaAnalyticsManager get(Context ctx) {
        if (instance == null) instance = new LeaAnalyticsManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaAnalyticsManager(Context ctx) { this.ctx = ctx; }

    public void trackOpen(String featureName) {
        featureStartTs = System.currentTimeMillis();
        currentFeature = featureName;
        LeaFeaturesDatabase.get(ctx).logEvent("open", 0, featureName);
    }

    public void trackClose(String featureName) {
        long duration = featureStartTs > 0 ? (System.currentTimeMillis() - featureStartTs) / 1000 : 0;
        featureStartTs = 0;
        LeaFeaturesDatabase.get(ctx).logEvent("close", duration, featureName);
        aggregateToday();
    }

    public void trackAction(String featureName, String action) {
        LeaFeaturesDatabase.get(ctx).logEvent("action_" + action, 0, featureName);
    }

    private void aggregateToday() {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(new Date());
        int featuresUsed = db.getTodayEventCount(null);
        LeaPlusDatabase plus = LeaPlusDatabase.get(ctx);
        LeaPlusDatabase.CharStats stats = plus.getCharStats();
        List<LeaPlusDatabase.HabitRow> habits = plus.getActiveHabits();
        long dayStart = (System.currentTimeMillis() / 86400_000L) * 86400_000L;
        int habitsCompleted = 0;
        for (LeaPlusDatabase.HabitRow h : habits) if (h.lastCheck >= dayStart) habitsCompleted++;
        db.upsertDailyStat(today, featuresUsed, 0, stats.xp, habitsCompleted);
    }

    public String getMostUsedFeature() {
        return LeaFeaturesDatabase.get(ctx).getMostUsedFeature();
    }

    public List<LeaFeaturesDatabase.DailyStat> getWeekStats() {
        return LeaFeaturesDatabase.get(ctx).getWeekStats();
    }

    public int getTodayEventCount() {
        return LeaFeaturesDatabase.get(ctx).getTodayEventCount(null);
    }
}
