package com.example.hijricalendarwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // Reschedule all alarms after boot / reinstall
            HijriDateUtils.schedulePeriodicSync(context);
            HijriDateUtils.scheduleMidnightAlarm(context);
            WeatherUtils.scheduleWeatherSync(context);
            // Immediately refresh all widget instances so they show today's date
            HijriDateUtils.refreshAllWidgetsNow(context);

        } else if ("com.example.hijricalendarwidget.ACTION_MIDNIGHT_UPDATE".equals(action)) {
            // Fired at 00:00:05 every night — update widgets and reschedule for next midnight
            HijriDateUtils.refreshAllWidgetsNow(context);
            // Reschedule for the next midnight (alarm is one-shot so we must re-arm it)
            HijriDateUtils.scheduleMidnightAlarm(context);

        } else if ("com.example.hijricalendarwidget.ACTION_PERIODIC_SYNC".equals(action)) {
            HijriDateUtils.syncTimeWithNetwork(context);

        } else if ("com.example.hijricalendarwidget.ACTION_WEATHER_SYNC".equals(action)) {
            WeatherUtils.triggerWeatherFetch(context);
        }
    }
}
