package com.example.btcwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 */
public class BitcoinComparisonWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                               int appWidgetId) {

        // Create RemoteViews object to update the widget UI
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Fetch real data and update widget
        DataFetcher dataFetcher = new DataFetcher(context, appWidgetId, views, new DataFetcher.DataFetchCallback() {
            @Override
            public void onDataFetched(RemoteViews updatedViews) {
                appWidgetManager.updateAppWidget(appWidgetId, updatedViews);
            }
        });
        dataFetcher.fetchAllData();

        // Set up refresh button intent
        Intent intent = new Intent(context, BitcoinComparisonWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
