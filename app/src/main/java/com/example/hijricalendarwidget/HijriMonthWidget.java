package com.example.hijricalendarwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HijriMonthWidget extends AppWidgetProvider {

    public static final String ACTION_NEXT_MONTH = "com.example.hijricalendarwidget.ACTION_NEXT_MONTH";
    public static final String ACTION_PREV_MONTH = "com.example.hijricalendarwidget.ACTION_PREV_MONTH";
    public static final String ACTION_REFRESH = "com.example.hijricalendarwidget.ACTION_REFRESH";
    public static final String ACTION_CLICK = "com.example.hijricalendarwidget.ACTION_CLICK";
    private static long lastClickTime = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (action == null)
                return;

            super.onReceive(context, intent);

            // Refresh all month widget instances on date/time/boot changes
            if (android.content.Intent.ACTION_DATE_CHANGED.equals(action)
                    || android.content.Intent.ACTION_TIME_CHANGED.equals(action)
                    || android.content.Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    || android.content.Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                int[] ids = mgr.getAppWidgetIds(
                        new android.content.ComponentName(context, HijriMonthWidget.class));
                if (ids != null && ids.length > 0) {
                    onUpdate(context, mgr, ids);
                }
                return;
            }

            if (ACTION_CLICK.equals(action)) {
                long currentClickTime = System.currentTimeMillis();
                if (currentClickTime - lastClickTime < 500) {
                    Intent launchIntent = new Intent(context, SettingsActivity.class);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(launchIntent);
                    lastClickTime = 0;
                } else {
                    lastClickTime = currentClickTime;
                }
                return;
            }

            if (ACTION_NEXT_MONTH.equals(action) || ACTION_PREV_MONTH.equals(action) || ACTION_REFRESH.equals(action)) {
                int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    if (!ACTION_REFRESH.equals(action)) {
                        SharedPreferences prefs = context.getSharedPreferences("MonthWidgetPrefs",
                                Context.MODE_PRIVATE);
                        int current = prefs.getInt("month_offset_" + appWidgetId, 0);
                        prefs.edit().putInt("month_offset_" + appWidgetId,
                                current + (ACTION_NEXT_MONTH.equals(action) ? 1 : -1)).apply();
                    }

                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            // Defense-in-depth: validate date before rendering month widget.
            SharedPreferences mainPrefs = context.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
            String cachedDate = mainPrefs.getString(CacheManager.KEY_CACHE_DATE, null);
            if (cachedDate == null || !cachedDate.equals(CacheManager.todayCacheKey())) {
                PrayerUtils.refreshPrayerTimesIfNewDay(context);
                HijriDateUtils.refreshAllWidgetsNow(context);
                CacheManager.touchWidgetUpdate(context);
            }
            int orientation = mainPrefs.getInt("month_orientation", 0);
            int layoutId = (orientation == 1) ? R.layout.widget_month_calendar_vertical
                    : R.layout.widget_month_calendar;

            RemoteViews rv = new RemoteViews(context.getPackageName(), layoutId);

            int bgColor = mainPrefs.getInt("month_bg_color", 0x88000000); // Default Semi-Black
            int rootId = (orientation == 1) ? R.id.widget_month_vertical_root : R.id.widget_month_root;
            rv.setInt(rootId, "setBackgroundColor", bgColor);

            // Unique Data URI is CRITICAL to force update on service/factory
            Intent serviceIntent = new Intent(context, MonthWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // Add timestamp to data to guarantee a new RemoteViewsFactory is requested
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            int gridId = (orientation == 1) ? R.id.calendar_grid_vertical : R.id.calendar_grid;
            rv.setRemoteAdapter(gridId, serviceIntent);

            // Header Title [V9.1] with proper Localization
            SharedPreferences widgetPrefs = context.getSharedPreferences("MonthWidgetPrefs", Context.MODE_PRIVATE);
            int offset = widgetPrefs.getInt("month_offset_" + appWidgetId, 0);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, offset);

            String globalLang = mainPrefs.getString("app_language", "ar");
            int langIdx = mainPrefs.getInt("month_day_language_index", 0); // 0=Auto
            String widgetLang = globalLang;
            if (langIdx > 0) {
                String[] langCodes = { "ar", "ar", "en", "es", "fr", "ja", "zh" };
                if (langIdx < langCodes.length)
                    widgetLang = langCodes[langIdx];
            }

            Locale locale = new Locale(widgetLang);
            int digitStyle = mainPrefs.getInt("digit_style", 0);
            int gYear = cal.get(Calendar.YEAR);
            int gMonth = cal.get(Calendar.MONTH) + 1;
            int[] hijri = HijriDateUtils.getHijriDateFromGregorian(context, gYear, gMonth, 15);
            int hYear = hijri[0];
            int hMonth = hijri[1];

            String gSymbol, hSymbol;
            switch (widgetLang) {
                case "es":
                    gSymbol = " d.C.";
                    hSymbol = " H";
                    break;
                case "fr":
                    gSymbol = " ap. J.-C.";
                    hSymbol = " H";
                    break;
                case "ja":
                    gSymbol = " G";
                    hSymbol = " H";
                    break;
                case "zh":
                    gSymbol = " 公元";
                    hSymbol = " 回历";
                    break;
                case "en":
                    gSymbol = " G";
                    hSymbol = " H";
                    break;
                default:
                    gSymbol = " م";
                    hSymbol = " هـ";
                    break;
            }

            int gregColor = mainPrefs.getInt("month_greg_color", 0xFFFFFFFF);
            int hijriColor = mainPrefs.getInt("month_hijri_color", 0xFFFFFFFF);

            String gPart = String.format(Locale.US, "%04d/%02d%s", gYear, gMonth, gSymbol);
            String hPart = String.format(Locale.US, "%04d/%02d%s", hYear, hMonth, hSymbol);

            gPart = HijriDateUtils.localizeDigits(gPart, digitStyle);
            hPart = HijriDateUtils.localizeDigits(hPart, digitStyle);

            String colorG = String.format("#%06X", (0xFFFFFF & gregColor));
            String colorH = String.format("#%06X", (0xFFFFFF & hijriColor));

            String html = "<font color='" + colorG + "'>" + gPart + "</font> <small> - </small> <font color='" + colorH
                    + "'>" + hPart + "</font>";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                rv.setTextViewText(R.id.month_year_text,
                        android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                rv.setTextViewText(R.id.month_year_text, android.text.Html.fromHtml(html));
            }

            // Tap Title to Refresh
            rv.setOnClickPendingIntent(R.id.month_year_text,
                    getPendingSelfIntent(context, appWidgetId, ACTION_REFRESH));

            // Day Headers (Localized)
            int weekStart = mainPrefs.getInt("week_start", 0);
            int[] ids = { R.id.day_name_1, R.id.day_name_2, R.id.day_name_3, R.id.day_name_4, R.id.day_name_5,
                    R.id.day_name_6, R.id.day_name_7 };
            String[] headers = new String[7];
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", locale);
            Calendar headerCal = Calendar.getInstance(locale);

            // Start from Sunday (1)
            headerCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            // Adjust to weekStart
            if (weekStart == 1)
                headerCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            else if (weekStart == 2)
                headerCal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

            for (int i = 0; i < 7; i++) {
                headers[i] = sdf.format(headerCal.getTime());
                headerCal.add(Calendar.DAY_OF_WEEK, 1);
            }

            int weekHeaderColor = mainPrefs.getInt("month_week_header_color", 0xFFFFD700);
            int headerSizeIdx = mainPrefs.getInt("month_week_header_size_index", 2);
            float[] headerSizes = { 10f, 11f, 13f, 15f, 18f };
            float hSize = headerSizes[headerSizeIdx % headerSizes.length];
            boolean isBold = mainPrefs.getBoolean("month_day_bold", false);

            for (int i = 0; i < 7; i++) {
                if (ids[i] != 0) {
                    if (isBold) {
                        rv.setTextViewText(ids[i], android.text.Html.fromHtml("<b>" + headers[i] + "</b>"));
                    } else {
                        rv.setTextViewText(ids[i], headers[i]);
                    }
                    rv.setTextColor(ids[i], weekHeaderColor);
                    rv.setTextViewTextSize(ids[i], android.util.TypedValue.COMPLEX_UNIT_SP, hSize);
                }
            }

            // Buttons
            rv.setOnClickPendingIntent(R.id.btn_next_month,
                    getPendingSelfIntent(context, appWidgetId, ACTION_NEXT_MONTH));
            rv.setOnClickPendingIntent(R.id.btn_prev_month,
                    getPendingSelfIntent(context, appWidgetId, ACTION_PREV_MONTH));

            Intent clickIntent = new Intent(context, HijriMonthWidget.class);
            clickIntent.setAction(ACTION_CLICK);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent clickPending = PendingIntent.getBroadcast(context, appWidgetId, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(rootId, clickPending);

            appWidgetManager.updateAppWidget(appWidgetId, rv);

            // Critical for orientation changes: force the list to clear its cache and
            // reload from the new layout
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, gridId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PendingIntent getPendingSelfIntent(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, HijriMonthWidget.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId + action.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
