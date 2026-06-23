package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.flolov42.lea_v3.R;

public class LeaWidgetCoinsProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_coins);

        LeaPlusDatabase db = LeaPlusDatabase.get(ctx);
        int balance = db.getCoinBalance();
        int unlockedAch = LeaFeaturesDatabase.get(ctx).getUnlockedCount();

        views.setTextViewText(R.id.widget_coins_title, "LÉA Coins");
        views.setTextViewText(R.id.widget_coins_balance, "🪙 " + balance);
        views.setTextViewText(R.id.widget_coins_ach, "🏆 " + unlockedAch + " achievements");

        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launchIntent != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 3, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_coins_root, pi);
        }

        LeaFeaturesDatabase.get(ctx).touchWidget("widget_coins");
        mgr.updateAppWidget(widgetId, views);
    }
}
