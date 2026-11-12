package com.example.hijricalendarwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncReceiver extends BroadcastReceiver {

    /** Broadcast this action to force an immediate location + prayer refresh. */
    public static final String ACTION_FORCE_LOCATION_REFRESH =
            "com.example.hijricalendarwidget.ACTION_FORCE_LOCATION_REFRESH";

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

        } else if (ACTION_FORCE_LOCATION_REFRESH.equals(action)) {
            // ── User pressed "Refresh Location" button ─────────────────────────
            // 1. Wipe all stale caches (last_lat, last_lon, prayer JSON, network sync ts)
            // 2. Force a full location + prayer + weather refresh immediately
            Log.i("SyncReceiver", "Force location refresh requested by user.");
            HijriDateUtils.invalidateLocationAndPrayerCache(context);
            HijriDateUtils.syncTimeWithNetwork(context, true /* force = bypass 12h guard */);
        }
    }
}
