package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.utilities.*;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.flolov42.lea_v3.R;
import java.util.List;

public class LeaWidgetHabitProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_habit_progress);

        LeaPlusDatabase db = LeaPlusDatabase.get(ctx);
        List<LeaPlusDatabase.HabitRow> habits = db.getActiveHabits();
        int total = habits.size();
        int streak = 0;
        if (!habits.isEmpty()) streak = habits.get(0).streak;
        int completed = 0;
        long today = (System.currentTimeMillis() / 86400_000L) * 86400_000L;
        for (LeaPlusDatabase.HabitRow h : habits) {
            if (h.lastCheck >= today) completed++;
        }

        views.setTextViewText(R.id.widget_habit_title, "Habitudes du Jour");
        views.setTextViewText(R.id.widget_habit_progress, completed + "/" + total);
        views.setTextViewText(R.id.widget_habit_streak, "🔥 " + streak + " jours");
        if (total > 0) {
            views.setProgressBar(R.id.widget_habit_bar, total, completed, false);
        }

        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launchIntent != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_habit_root, pi);
        }

        LeaFeaturesDatabase.get(ctx).touchWidget("widget_habits");
        mgr.updateAppWidget(widgetId, views);
    }
}
