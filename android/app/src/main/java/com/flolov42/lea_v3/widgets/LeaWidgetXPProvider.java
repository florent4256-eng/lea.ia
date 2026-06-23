package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.flolov42.lea_v3.R;

public class LeaWidgetXPProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_xp_level);

        LeaPlusDatabase.CharStats stats = LeaPlusDatabase.get(ctx).getCharStats();

        views.setTextViewText(R.id.widget_xp_title, "LÉA Level");
        views.setTextViewText(R.id.widget_xp_level, "Niv. " + stats.level);
        views.setTextViewText(R.id.widget_xp_value, stats.xp + " / " + stats.xpNext + " XP");
        views.setTextViewText(R.id.widget_xp_world, stats.world);
        if (stats.xpNext > 0) {
            views.setProgressBar(R.id.widget_xp_bar, stats.xpNext, stats.xp, false);
        }

        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launchIntent != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 1, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_xp_root, pi);
        }

        LeaFeaturesDatabase.get(ctx).touchWidget("widget_xp");
        mgr.updateAppWidget(widgetId, views);
    }
}
