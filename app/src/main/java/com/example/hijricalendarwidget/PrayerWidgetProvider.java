package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;

public class PrayerWidgetProvider extends AppWidgetProvider {

    private static final String PREFS_NAME = "HijriWidgetPrefs";
    public static final String ACTION_UPDATE_PRAYER_COUNTDOWN = "com.example.hijricalendarwidget.ACTION_UPDATE_PRAYER_COUNTDOWN";
    public static final String ACTION_CLICK = "com.example.hijricalendarwidget.ACTION_PRAYER_CLICK";
    private static long lastClickTime = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, android.content.Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        // React to date/time/boot changes — validate cache and refresh prayer widget
        if (android.content.Intent.ACTION_DATE_CHANGED.equals(action)
                || android.content.Intent.ACTION_TIME_CHANGED.equals(action)
                || android.content.Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || android.content.Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            double lat = CacheManager.getCacheLat(context);
            double lon = CacheManager.getCacheLon(context);
            String city = CacheManager.getCacheCity(context);
            if (!CacheManager.isPrayerCacheValid(context, lat, lon, city)) {
                PrayerUtils.refreshPrayerTimesIfNewDay(context);
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager
                    .getAppWidgetIds(new android.content.ComponentName(context, PrayerWidgetProvider.class));
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    updateAppWidget(context, appWidgetManager, id);
                }
            }
            CacheManager.touchWidgetUpdate(context);
            return;
        }

        if (ACTION_CLICK.equals(action)) {
            long currentClickTime = System.currentTimeMillis();
            if (currentClickTime - lastClickTime < 500) {
                android.content.Intent launchIntent = new android.content.Intent(context, SettingsActivity.class);
                launchIntent.setFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
                lastClickTime = 0;
            } else {
                lastClickTime = currentClickTime;
            }
            return;
        }

        if (ACTION_UPDATE_PRAYER_COUNTDOWN.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager
                    .getAppWidgetIds(new android.content.ComponentName(context, PrayerWidgetProvider.class));
            for (int id : ids) {
                updateAppWidget(context, appWidgetManager, id);
            }
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Validate prayer cache: date + city + coordinates (within 5 km).
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        double lat = CacheManager.getCacheLat(context);
        double lon = CacheManager.getCacheLon(context);
        String city = CacheManager.getCacheCity(context);

        android.util.Log.d("PrayerDebug", "updateAppWidget: lat=" + lat + " lon=" + lon + " city=" + city);

        if (!CacheManager.isPrayerCacheValid(context, lat, lon, city)) {
            android.util.Log.d("PrayerDebug", "Cache invalid — synchronous recalculation before render");
            // Cache stale (new day, moved city, or traveled > 5 km): recalculate SYNCHRONOUSLY
            // before rendering so the widget never shows --:--.
            try {
                double calcLat = (lat != 0 && lon != 0) ? lat : PrayerUtils.DEFAULT_LAT;
                double calcLon = (lat != 0 && lon != 0) ? lon : PrayerUtils.DEFAULT_LON;
                String calcCity = (city != null && !city.isEmpty()) ? city : PrayerUtils.DEFAULT_CITY;
                String newJson = PrayerUtils.buildTimingsJson(
                        PrayerUtils.calculateToday(context, calcLat, calcLon)).toString();
                CacheManager.savePrayerCache(context, calcCity, calcLat, calcLon, newJson);
                CacheManager.touchWidgetUpdate(context);
                android.util.Log.d("PrayerDebug", "Synchronous calculation complete — city=" + calcCity);
            } catch (Exception e) {
                android.util.Log.d("PrayerDebug", "Synchronous calculation failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            android.util.Log.d("PrayerDebug", "Cache valid — using cached prayer data");
        }

        String json = CacheManager.getCachedPrayerJson(context);
        android.util.Log.d("PrayerDebug", "Prayer JSON for render: " + (json != null ? json.substring(0, Math.min(json.length(), 80)) : "null"));

        // Emergency fallback: if cache is still empty, calculate immediately with default coordinates
        if (json == null || json.isEmpty()) {
            android.util.Log.d("PrayerDebug", "Emergency fallback: calculating prayer times immediately");
            try {
                String newJson = PrayerUtils.buildTimingsJson(PrayerUtils.calculateToday(
                        context, PrayerUtils.DEFAULT_LAT, PrayerUtils.DEFAULT_LON)).toString();
                CacheManager.savePrayerCache(context, PrayerUtils.DEFAULT_CITY,
                        PrayerUtils.DEFAULT_LAT, PrayerUtils.DEFAULT_LON, newJson);
                CacheManager.touchWidgetUpdate(context);
                json = newJson;
                android.util.Log.d("PrayerDebug", "Emergency fallback complete — json present");
            } catch (Exception e) {
                android.util.Log.d("PrayerDebug", "Emergency fallback failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        String layoutMode = prefs.getString("prayer_layout_mode", "horizontal");
        String bgType = prefs.getString("prayer_bg_type", "transparent");
        String highlightColor = prefs.getString("prayer_highlight_color", "golden");
        String textColorHex = prefs.getString("prayer_text_color", "#FFFFFF");
        boolean showRemaining = prefs.getBoolean("prayer_show_remaining", true);
        boolean showTimes = prefs.getBoolean("prayer_show_times", true);
        boolean showNextOnly = prefs.getBoolean("prayer_show_next_only", false);
        boolean showRemainingOnly = prefs.getBoolean("prayer_show_remaining_only", false);
        boolean showClock = prefs.getBoolean("prayer_show_clock", false);
        if (!prefs.contains("prayer_shuruq_default_set")) {
            prefs.edit().putBoolean("prayer_show_shuruq", false).putBoolean("prayer_shuruq_default_set", true).apply();
        }
        boolean showShuruq = prefs.getBoolean("prayer_show_shuruq", false);

        int layoutRes = layoutMode.equals("vertical") ? R.layout.widget_prayer_vertical
                : R.layout.widget_prayer_horizontal;
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutRes);

        // Background
        int bgRes = R.drawable.prayer_bg_transparent;
        if (bgType.equals("solid_dark"))
            bgRes = R.drawable.prayer_bg_solid_dark;
        else if (bgType.equals("solid_light"))
            bgRes = R.drawable.prayer_bg_solid_light;
        else if (bgType.equals("gradient"))
            bgRes = R.drawable.prayer_bg_gradient;
        else if (bgType.equals("glass"))
            bgRes = R.drawable.prayer_bg_glass;
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes);

        // Click to open settings Hub
        android.content.Intent configIntent = new android.content.Intent(context, PrayerWidgetProvider.class);
        configIntent.setAction(ACTION_CLICK);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        android.app.PendingIntent configPendingIntent = android.app.PendingIntent.getBroadcast(context, appWidgetId,
                configIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent);

        // Text Color Handling
        boolean isLightBg = bgType.equals("solid_light");
        String resolvedTextColor = (textColorHex.equals("#FFFFFF") && isLightBg) ? "#000000" : textColorHex;
        String fadedTextColorStr = prefs.getString("prayer_faded_text_color", "#888888");
        String widgetLang = prefs.getString("prayer_widget_lang", "ar");
        int baseTextColor;
        try {
            baseTextColor = Color.parseColor(resolvedTextColor);
        } catch (Exception e) {
            baseTextColor = 0xFFFFFFFF;
        }

        String tColorVal = prefs.getString("prayer_time_color", "faded");
        int finalTimeColor;
        if (tColorVal.equals("faded")) {
            finalTimeColor = Color.argb(180, Color.red(baseTextColor), Color.green(baseTextColor),
                    Color.blue(baseTextColor));
        } else {
            try {
                finalTimeColor = Color.parseColor(tColorVal);
            } catch (Exception e) {
                finalTimeColor = Color.argb(180, Color.red(baseTextColor), Color.green(baseTextColor),
                        Color.blue(baseTextColor));
            }
        }

        // Initial reset
        int[] allNameIds = { R.id.tv_fajr_name, R.id.tv_sunrise_name, R.id.tv_dhuhr_name, R.id.tv_asr_name,
                R.id.tv_maghrib_name,
                R.id.tv_isha_name, R.id.tv_single_name };
        int[] allTimeIds = { R.id.tv_fajr_time, R.id.tv_sunrise_time, R.id.tv_dhuhr_time, R.id.tv_asr_time,
                R.id.tv_maghrib_time,
                R.id.tv_isha_time, R.id.tv_single_time };

        int textSizeIdx = prefs.getInt("prayer_text_size_index", 2);
        float nSize;
        float tSize;
        if (layoutMode.equals("horizontal")) {
            // Horizontal layout (6 columns):
            // Names: Small (11sp), Medium (12.5sp), Large (13.5sp), X-Large (15sp), Huge (17sp)
            float[] hNameSizes = { 11f, 12.5f, 13.5f, 15f, 17f };
            // Prayer times: enlarged prominently so the digits are bold and easy to read
            // Small (13sp), Medium (14.5sp), Large (16sp), X-Large (18sp), Huge (20.5sp)
            float[] hTimeSizes = { 13f, 14.5f, 16f, 18f, 20.5f };
            nSize = hNameSizes[textSizeIdx % hNameSizes.length];
            tSize = hTimeSizes[textSizeIdx % hTimeSizes.length];
        } else {
            float[] vNameSizes = { 12f, 14f, 16f, 18f, 22f };
            float[] vTimeSizes = { 13f, 15f, 17f, 20f, 24f };
            nSize = vNameSizes[textSizeIdx % vNameSizes.length];
            tSize = vTimeSizes[textSizeIdx % vTimeSizes.length];
        }

        for (int id : allNameIds) {
            if (id != 0) {
                views.setTextColor(id, baseTextColor);
                views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, nSize);
            }
        }
        for (int id : allTimeIds) {
            if (id != 0) {
                views.setTextColor(id, finalTimeColor);
                views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, tSize);
            }
        }
        views.setTextColor(R.id.tv_remaining, baseTextColor);
        views.setTextViewTextSize(R.id.tv_remaining, android.util.TypedValue.COMPLEX_UNIT_SP, Math.max(14.5f, nSize + 2));
        int digitStyle = PrayerUtils.getDigitStyle(context);
        views.setTextViewText(R.id.tv_clock, buildClockText(context, digitStyle));
        views.setTextColor(R.id.tv_clock, baseTextColor);
        views.setViewVisibility(R.id.tv_clock, showClock ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.ll_sunrise, showShuruq ? View.VISIBLE : View.GONE);

        // Visibility Logic
        if (showRemainingOnly) {
            views.setViewVisibility(R.id.ll_all_prayers_grid, View.GONE);
            views.setViewVisibility(R.id.ll_all_prayers_list, View.GONE);
            views.setViewVisibility(R.id.ll_next_prayer_only, View.GONE);
            views.setViewVisibility(R.id.tv_remaining, View.VISIBLE);
        } else {
            // Next Prayer Prominent Box
            views.setViewVisibility(R.id.ll_next_prayer_only, showNextOnly ? View.VISIBLE : View.GONE);

            // Full List
            if (showTimes) {
                if (layoutMode.equals("vertical")) {
                    views.setViewVisibility(R.id.ll_all_prayers_list, View.VISIBLE);
                    views.setViewVisibility(R.id.ll_all_prayers_grid, View.GONE);
                } else {
                    views.setViewVisibility(R.id.ll_all_prayers_grid, View.VISIBLE);
                    views.setViewVisibility(R.id.ll_all_prayers_list, View.GONE);
                }
            } else {
                views.setViewVisibility(R.id.ll_all_prayers_list, View.GONE);
                views.setViewVisibility(R.id.ll_all_prayers_grid, View.GONE);
            }

            views.setViewVisibility(R.id.tv_remaining, showRemaining ? View.VISIBLE : View.GONE);
        }

        if (json != null) {
            try {
                JSONObject root = new JSONObject(json);
                // Dynamically recalculate based on CURRENT system time and showShuruq setting!
                root = PrayerUtils.getDynamicTimings(context, root, showShuruq);

                JSONObject data = root.optJSONObject("times");
                if (data == null)
                    data = new JSONObject();
                String next = root.optString("next", "Fajr");
                String activePrayer = root.optString("active_prayer", next);
                boolean isAdhanWindow = root.optBoolean("is_adhan_window", false);
                String remainingStr = root.optString("duration_" + widgetLang, "--");

                if (showRemainingOnly) {
                    if (isAdhanWindow) {
                        views.setTextColor(R.id.tv_remaining, Color.parseColor("#FF5252"));
                    } else {
                        views.setTextColor(R.id.tv_remaining, Color.parseColor("#FBCD34"));
                    }
                    views.setTextViewText(R.id.tv_remaining, remainingStr);
                } else if (showNextOnly) {
                    JSONObject p = data.optJSONObject(activePrayer);
                    if (p == null)
                        p = data.optJSONObject("Fajr");
                    String pName = (p != null) ? p.optString("name_" + widgetLang, p.optString("name_ar", "--")) : "--";
                    String pTime = (p != null) ? p.optString("time_" + widgetLang, p.optString("time_ar", "--:--")) : "--:--";
                    if (pTime == null || pTime.isEmpty())
                        pTime = "--:--";

                    String nextPrefix;
                    if (isAdhanWindow) {
                        nextPrefix = widgetLang.equals("ar") ? "حان الآن: " : "Now: ";
                    } else {
                        switch (widgetLang) {
                            case "en":
                                nextPrefix = "Next: ";
                                break;
                            case "es":
                                nextPrefix = "Próxima: ";
                                break;
                            case "fr":
                                nextPrefix = "Prochaine: ";
                                break;
                            case "ja":
                                nextPrefix = "次: ";
                                break;
                            case "zh":
                                nextPrefix = "下一次: ";
                                break;
                            default:
                                nextPrefix = "القادمة: ";
                        }
                    }

                    String nameText = nextPrefix + pName;
                    String timeText = pTime;
                    String durationStr = root.optString("duration_only_" + widgetLang, "");

                    if (showRemaining && !durationStr.isEmpty()) {
                        timeText += " (" + durationStr + ")";
                    }

                    views.setTextViewText(R.id.tv_single_name, nameText);
                    views.setTextViewText(R.id.tv_single_time, timeText);
                    views.setViewVisibility(R.id.tv_single_time, showTimes ? View.VISIBLE : View.GONE);

                    if (isAdhanWindow) {
                        views.setInt(R.id.ll_next_prayer_only, "setBackgroundResource", R.drawable.prayer_highlight_red_solid);
                        views.setTextColor(R.id.tv_single_name, Color.WHITE);
                        views.setTextColor(R.id.tv_single_time, Color.WHITE);
                    } else {
                        views.setInt(R.id.ll_next_prayer_only, "setBackgroundResource", R.drawable.prayer_bg_glass);
                        views.setTextColor(R.id.tv_single_name, Color.WHITE);
                        views.setTextColor(R.id.tv_single_time, Color.parseColor("#FBCD34"));
                    }

                    // Hide bottom countdown if integrated
                    views.setViewVisibility(R.id.tv_remaining, View.GONE);
                } else {
                    String[] keys = { "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha" };
                    int[] vIds = { R.id.ll_fajr, R.id.ll_sunrise, R.id.ll_dhuhr, R.id.ll_asr, R.id.ll_maghrib,
                            R.id.ll_isha };
                    int[] nIds = { R.id.tv_fajr_name, R.id.tv_sunrise_name, R.id.tv_dhuhr_name, R.id.tv_asr_name,
                            R.id.tv_maghrib_name,
                            R.id.tv_isha_name };
                    int[] tIds = { R.id.tv_fajr_time, R.id.tv_sunrise_time, R.id.tv_dhuhr_time, R.id.tv_asr_time,
                            R.id.tv_maghrib_time,
                            R.id.tv_isha_time };

                    for (int i = 0; i < keys.length; i++) {
                        JSONObject p = data.optJSONObject(keys[i]);
                        String nameVal = "--";
                        String timeVal = "--:--";
                        if (p != null) {
                            nameVal = p.optString("name_" + widgetLang, p.optString("name_ar", "--"));
                            timeVal = p.optString("time_" + widgetLang, p.optString("time_ar", "--:--"));
                            if (timeVal == null || timeVal.isEmpty())
                                timeVal = "--:--";
                        }

                        views.setTextViewText(nIds[i], nameVal);
                        views.setViewVisibility(tIds[i], showTimes ? View.VISIBLE : View.GONE);

                        if (keys[i].equals(activePrayer)) {
                            int hRes;
                            if (isAdhanWindow) {
                                // Turn yellow to RED during Adhan window (-5 min to +20 min)
                                if (highlightColor.equals("golden_stroke") || highlightColor.equals("teal_stroke")
                                        || highlightColor.equals("white_stroke")) {
                                    hRes = R.drawable.prayer_highlight_red_stroke;
                                } else {
                                    hRes = R.drawable.prayer_highlight_red_solid;
                                }
                            } else {
                                if (highlightColor.equals("golden_stroke"))
                                    hRes = R.drawable.prayer_highlight_golden_stroke;
                                else if (highlightColor.equals("teal_stroke"))
                                    hRes = R.drawable.prayer_highlight_teal_stroke;
                                else if (highlightColor.equals("white_stroke"))
                                    hRes = R.drawable.prayer_highlight_white_stroke;
                                else if (highlightColor.equals("transparent"))
                                    hRes = 0;
                                else
                                    hRes = R.drawable.prayer_highlight_golden_solid;
                            }

                            views.setInt(vIds[i], "setBackgroundResource", hRes);

                            if (showRemaining && layoutMode.equals("vertical")) {
                                String dur = root.optString("duration_only_" + widgetLang, "");
                                if (!dur.isEmpty()) {
                                    timeVal += " (" + dur + ")";
                                }
                            }

                            if (isAdhanWindow) {
                                if (hRes == R.drawable.prayer_highlight_red_solid) {
                                    views.setTextColor(nIds[i], Color.WHITE);
                                    views.setTextColor(tIds[i], Color.WHITE);
                                } else {
                                    views.setTextColor(nIds[i], Color.parseColor("#E53935"));
                                    views.setTextColor(tIds[i], Color.parseColor("#E53935"));
                                }
                            } else {
                                if (hRes == R.drawable.prayer_highlight_golden_solid) {
                                    views.setTextColor(nIds[i], Color.BLACK);
                                    views.setTextColor(tIds[i], Color.BLACK);
                                } else {
                                    views.setTextColor(nIds[i], Color.parseColor("#FBCD34"));
                                    views.setTextColor(tIds[i], Color.parseColor("#FBCD34"));
                                }
                            }
                        } else {
                            views.setInt(vIds[i], "setBackgroundResource", 0);
                            views.setTextColor(nIds[i], baseTextColor);
                            views.setTextColor(tIds[i], finalTimeColor);
                        }
                        views.setTextViewText(tIds[i], timeVal);
                    }
                    if (showRemaining) {
                        if (isAdhanWindow) {
                            views.setTextColor(R.id.tv_remaining, Color.parseColor("#FF5252"));
                        } else {
                            views.setTextColor(R.id.tv_remaining, Color.parseColor("#FBCD34"));
                        }
                        views.setTextViewText(R.id.tv_remaining, remainingStr);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Schedule next update for the next minute to keep countdown accurate
        scheduleNextUpdate(context);
    }

    /**
     * Current time as text, using the user's 12/24-hour setting but always rendering
     * the digits with the single digit style chosen in settings (no mixed styles).
     */
    private static String buildClockText(Context context, int digitStyle) {
        try {
            java.text.DateFormat fmt = android.text.format.DateFormat.getTimeFormat(context);
            String text = fmt.format(new java.util.Date(System.currentTimeMillis()));
            return HijriDateUtils.localizeDigits(text, digitStyle);
        } catch (Exception e) {
            return "--:--";
        }
    }

    private static void scheduleNextUpdate(Context context) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        android.content.Intent intent = new android.content.Intent(context, PrayerWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_PRAYER_COUNTDOWN);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        // One inexact repeating alarm drives every widget instance: updateAppWidget() is called per
        // instance, so a single shared alarm avoids stacking an exact wakeup per widget. The
        // countdown is minute-granular, so inexact delivery is acceptable and far kinder to battery.
        long interval = 60_000L;
        long triggerAt = (System.currentTimeMillis() / interval + 1) * interval;

        try {
            alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC, triggerAt, interval, pendingIntent);
        } catch (Exception e) {
            android.util.Log.w("PrayerWidget", "Could not schedule countdown update", e);
        }
    }
}
