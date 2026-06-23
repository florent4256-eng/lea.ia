package com.flolov42.lea_v3.widgets;

import com.flolov42.lea_v3.database.*;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.flolov42.lea_v3.R;
import java.util.List;

public class LeaWidgetQuestsProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_quests);

        LeaPlusDatabase db = LeaPlusDatabase.get(ctx);
        List<LeaPlusDatabase.QuestRow> available = db.getQuests("available");
        List<LeaPlusDatabase.QuestRow> completed = db.getQuests("completed");

        int totalToday = available.size() + completed.size();
        int doneToday = completed.size();

        views.setTextViewText(R.id.widget_quests_title, "Quêtes du Jour");
        views.setTextViewText(R.id.widget_quests_count, doneToday + "/" + totalToday + " complétées");
        String nextQuest = available.isEmpty() ? "Toutes complétées! 🎉" : "⚔️ " + available.get(0).title;
        views.setTextViewText(R.id.widget_quests_next, nextQuest);
        if (totalToday > 0) {
            views.setProgressBar(R.id.widget_quests_bar, totalToday, doneToday, false);
        }

        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launchIntent != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 2, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_quests_root, pi);
        }

        LeaFeaturesDatabase.get(ctx).touchWidget("widget_quests");
        mgr.updateAppWidget(widgetId, views);
    }
}
