package com.example.hijricalendarwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

public class WeatherUtils {
    private static final String PREFS_NAME = "HijriWidgetPrefs";

    public static void updateWidget(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] clockIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, HijriCalendarWidget.class));
            if (clockIds.length > 0) {
                HijriCalendarWidget widgetProvider = new HijriCalendarWidget();
                for (int id : clockIds) {
                    widgetProvider.updateAppWidget(context, appWidgetManager, id, R.layout.widget_hijri_calendar_v2);
                }
            }
        } catch (Exception e) {
            Log.e("WeatherUtils", "Error updating widget directly", e);
        }
    }

    public static void fetchWeather(Context context, double latitude, double longitude) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean showTemp = prefs.getBoolean("show_temperature", false);
        if (!showTemp) {
            return;
        }
        new Thread(() -> {
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current_weather=true";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    if (jsonObject.has("current_weather")) {
                        JSONObject current = jsonObject.getJSONObject("current_weather");
                        double temp = current.getDouble("temperature");
                        
                        // Round temperature to integer
                        long roundedTemp = Math.round(temp);
                        String tempString = roundedTemp + "°";

                        // Save to SharedPreferences
                        SharedPreferences localPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        localPrefs.edit().putString("current_temperature", tempString).apply();
                        
                        Log.d("WeatherUtils", "Fetched temp: " + tempString);
                        
                        // Update widget immediately
                        updateWidget(context);
                    } else {
                        revertTempIfPending(context);
                    }
                } else {
                    Log.e("WeatherUtils", "Weather API response code: " + responseCode);
                    revertTempIfPending(context);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("WeatherUtils", "Error fetching weather", e);
                revertTempIfPending(context);
            }
        }).start();
    }

    public static void scheduleWeatherSync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean showTemp = prefs.getBoolean("show_temperature", false);
        int intervalMins = prefs.getInt("temperature_interval", 30);

        cancelWeatherSync(context);

        if (!showTemp) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncReceiver.class);
        intent.setAction("com.example.hijricalendarwidget.ACTION_WEATHER_SYNC");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1002, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long intervalMillis = (long) intervalMins * 60 * 1000;
        long triggerAt = System.currentTimeMillis() + intervalMillis;

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intervalMillis,
                    pendingIntent);
        }

        // Fetch immediately when scheduling (e.g. settings saved)
        triggerWeatherFetch(context);
    }

    public static void cancelWeatherSync(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SyncReceiver.class);
        intent.setAction("com.example.hijricalendarwidget.ACTION_WEATHER_SYNC");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1002, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void triggerWeatherFetch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean showTemp = prefs.getBoolean("show_temperature", false);
        if (!showTemp) {
            return;
        }

        // Set temp to "..." to indicate we are fetching only if empty, dashes or already dots
        String currentTemp = prefs.getString("current_temperature", "");
        if (currentTemp.isEmpty() || "--".equals(currentTemp) || "...".equals(currentTemp)) {
            prefs.edit().putString("current_temperature", "...").apply();
            updateWidget(context);
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationUtils.getCurrentLocation(context, new LocationUtils.LocationCallback() {
                @Override
                public void onLocationFound(double lat, double lon) {
                    // Update last known lat/lon in prefs
                    prefs.edit().putFloat("last_lat", (float) lat).putFloat("last_lon", (float) lon).apply();
                    fetchWeather(context, lat, lon);
                }

                @Override
                public void onError(String msg) {
                    Log.e("WeatherUtils", "Location error: " + msg);
                    // Fallback to last known lat/lon if GPS fails
                    float lastLat = prefs.getFloat("last_lat", 0.0f);
                    float lastLon = prefs.getFloat("last_lon", 0.0f);
                    if (lastLat != 0.0f || lastLon != 0.0f) {
                        fetchWeather(context, lastLat, lastLon);
                    } else {
                        // Fallback to Riyadh if no last known location
                        fetchWeather(context, 24.7136, 46.6753);
                    }
                }
            });
        } else {
            Log.w("WeatherUtils", "Location permission not granted for weather update. Using fallback.");
            // If no permission, use last known location or Riyadh as fallback
            float lastLat = prefs.getFloat("last_lat", 0.0f);
            float lastLon = prefs.getFloat("last_lon", 0.0f);
            if (lastLat != 0.0f || lastLon != 0.0f) {
                fetchWeather(context, lastLat, lastLon);
            } else {
                fetchWeather(context, 24.7136, 46.6753);
            }
        }
    }

    private static void revertTempIfPending(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String currentTemp = prefs.getString("current_temperature", "");
            if ("...".equals(currentTemp)) {
                prefs.edit().putString("current_temperature", "").apply();
                updateWidget(context);
            }
        } catch (Exception e) {
            Log.e("WeatherUtils", "Error reverting temp", e);
        }
    }
}
