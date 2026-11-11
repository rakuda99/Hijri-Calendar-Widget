package com.example.hijricalendarwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HijriCalendarWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        HijriDateUtils.schedulePeriodicSync(context);   // also calls scheduleMidnightAlarm internally
        WeatherUtils.scheduleWeatherSync(context);
        HijriDateUtils.syncTimeWithNetwork(context, true); // Initial sync
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Cancel the 12-hour periodic sync alarm
            Intent intent = new Intent(context, SyncReceiver.class);
            intent.setAction("com.example.hijricalendarwidget.ACTION_PERIODIC_SYNC");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);

            // Cancel the midnight update alarm
            Intent midnightIntent = new Intent(context, SyncReceiver.class);
            midnightIntent.setAction("com.example.hijricalendarwidget.ACTION_MIDNIGHT_UPDATE");
            PendingIntent midnightPending = PendingIntent.getBroadcast(context, 2001, midnightIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(midnightPending);
        }
        WeatherUtils.cancelWeatherSync(context);
    }

    private static final String ACTION_DOUBLE_CLICK_CHECK = "com.example.hijricalendarwidget.ACTION_DOUBLE_CLICK_CHECK";
    private static final String PREFS_NAME = "HijriWidgetPrefs";
    private static final String PREF_LAST_CLICK_TIME = "last_click_time";
    private static final long DOUBLE_CLICK_THRESHOLD = 500;
    private static long lastClickTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Guard: must call super first so ACTION_APPWIDGET_UPDATE reaches onUpdate()
        if (intent.getAction() == null) return;

        // Let the system handle APPWIDGET_UPDATE → onUpdate() before our custom logic
        super.onReceive(context, intent);

        try {
            String action = intent.getAction();

            // React to midnight / time-change events — refresh all widget instances immediately
            if (android.content.Intent.ACTION_DATE_CHANGED.equals(action)
                    || android.content.Intent.ACTION_TIME_CHANGED.equals(action)
                    || android.content.Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                forceUpdateAllWidgets(context);
                // Also reschedule midnight alarm in case timezone flipped
                HijriDateUtils.scheduleMidnightAlarm(context);
                return;
            }

            if (ACTION_DOUBLE_CLICK_CHECK.equals(action)) {
                long currentClickTime = System.currentTimeMillis();
                if (currentClickTime - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
                    Intent launchIntent = new Intent(context, SettingsActivity.class);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(launchIntent);
                    lastClickTime = 0; // reset
                } else {
                    lastClickTime = currentClickTime;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Force all active HijriCalendarWidget instances to redraw with the current date. */
    static void forceUpdateAllWidgets(Context context) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                    new android.content.ComponentName(context, HijriCalendarWidget.class));
            if (ids != null && ids.length > 0) {
                Intent updateIntent = new Intent(context, HijriCalendarWidget.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WeatherUtils.scheduleWeatherSync(context);
        for (int appWidgetId : appWidgetIds) {
            try {
                // Use the v2 layout to avoid cache issues
                updateAppWidget(context, appWidgetManager, appWidgetId, R.layout.widget_hijri_calendar_v2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int layoutId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String langStr = prefs.getString("language", "auto");
        int clockColor = prefs.getInt("clock_color", 0xFFFFFFFF);
        int hijriColor = prefs.getInt("hijri_color", 0xFFFFFFFF);
        int gregColor = prefs.getInt("greg_color", 0xFFFFFFFF);
        int dayColor = prefs.getInt("day_color", clockColor);

        int clockSizeIdx = prefs.getInt("clock_size_index", 1);
        int hijriSizeIdx = prefs.getInt("hijri_size_index", 1);
        int gregSizeIdx = prefs.getInt("greg_size_index", 1);
        int bgIdx = prefs.getInt("bg_color_index", 0);
        int monthFormatIdx = prefs.getInt("month_format_index", 0);

        boolean showH = prefs.getBoolean("show_h_symbol", true);
        boolean showM = prefs.getBoolean("show_m_symbol", true);
        boolean arabicAmPm = prefs.getBoolean("arabic_ampm", false);
        boolean use24h = prefs.getBoolean("use_24h", false);
        boolean padDates = prefs.getBoolean("pad_dates", false);
        boolean showClockVis = prefs.getBoolean("show_clock", true);
        boolean showHijriVis = prefs.getBoolean("show_hijri", true);
        boolean showGregVis = prefs.getBoolean("show_greg", true);
        boolean showDayArabic = prefs.getBoolean("show_day_arabic_v2", true);
        boolean showDayEnglish = prefs.getBoolean("show_day_english_v2", true);
        boolean dayTextLeft = prefs.getBoolean("day_position_left", false);
        int dayOrientation = prefs.getInt("day_orientation", 0); // 0 = Vertical, 1 = Horizontal
        boolean largeTimeLabels = prefs.getBoolean("large_time_labels", false);
        boolean showTemperature = prefs.getBoolean("show_temperature", false);
        String currentTemp = prefs.getString("current_temperature", "");

        // Language determination
        String language = langStr.equals("auto") ? Locale.getDefault().getLanguage() : langStr;
        if (langStr.equals("ar"))
            language = "ar";
        if (langStr.equals("en"))
            language = "en";

        float[] clockSizesList = { 35, 50, 65, 80, 100 };
        float[] hijriSizesList = { 16, 22, 28, 36, 46 };
        float[] gregSizesList = { 16, 22, 28, 36, 46 }; // Unified sizes

        float clockSize = clockSizesList[clockSizeIdx % clockSizesList.length];
        float hijriSize = hijriSizesList[hijriSizeIdx % hijriSizesList.length];
        float gregSize = gregSizesList[gregSizeIdx % gregSizesList.length];

        if (language.equals("ja") || language.equals("zh")) {
            hijriSize *= 0.85f; // Reduce size to fit long Hijri month names
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Dates logic
        // Dates logic
        Locale displayLocale = new Locale(language);
        long time = HijriDateUtils.getAdjustedTime(context);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        // Visibility Logic
        // Horizontal Mode: Show center if enabled (any of them)
        // Horizontal/Vertical refers to how the days are stacked next to the clock
        boolean isHorizontal = (dayOrientation == 1);

        // Hide all vertical day text views initially
        views.setViewVisibility(R.id.day_arabic_text, View.GONE);
        views.setViewVisibility(R.id.day_english_text, View.GONE);
        views.setViewVisibility(R.id.day_arabic_text_left, View.GONE);
        views.setViewVisibility(R.id.day_english_text_left, View.GONE);

        // Determine which to use based on alignment
        int arIdNorm = dayTextLeft ? R.id.day_arabic_text_left : R.id.day_arabic_text;
        int enIdNorm = dayTextLeft ? R.id.day_english_text_left : R.id.day_english_text;
        int arIdRot = dayTextLeft ? R.id.day_arabic_rotated_left : R.id.day_arabic_rotated;
        int enIdRot = dayTextLeft ? R.id.day_english_rotated_left : R.id.day_english_rotated;

        // Parent containers
        views.setViewVisibility(R.id.ll_day_left_normal, View.GONE);
        views.setViewVisibility(R.id.ll_day_left_rotated, View.GONE);
        views.setViewVisibility(R.id.ll_day_right_normal, View.GONE);
        views.setViewVisibility(R.id.ll_day_right_rotated, View.GONE);

        int llLeftNorm = R.id.ll_day_left_normal;
        int llLeftRot = R.id.ll_day_left_rotated;
        int llRightNorm = R.id.ll_day_right_normal;
        int llRightRot = R.id.ll_day_right_rotated;

        // Hide the separate bottom line day completely, we won't use it anymore
        views.setViewVisibility(R.id.day_horizontal_text, View.GONE);

        // Clock Visibility
        int clockVisibility = showClockVis ? View.VISIBLE : View.GONE;
        views.setViewVisibility(R.id.clock_text, clockVisibility);
        views.setViewVisibility(R.id.ampm_text, clockVisibility);

        // Date Visibility
        views.setViewVisibility(R.id.hijri_date_text, showHijriVis ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.gregorian_date_text, showGregVis ? View.VISIBLE : View.GONE);

        // Temperature Visibility and Styling
        if (showTemperature) {
            views.setViewVisibility(R.id.temperature_text, View.VISIBLE);
            String tempText = currentTemp.isEmpty() ? "--" : currentTemp;
            int digitStyle = prefs.getInt("digit_style", 0);
            views.setTextViewText(R.id.temperature_text, HijriDateUtils.localizeDigits(tempText, digitStyle));
            views.setTextColor(R.id.temperature_text, clockColor);
            float tempSize = clockSize * 0.35f;
            views.setTextViewTextSize(R.id.temperature_text, android.util.TypedValue.COMPLEX_UNIT_SP, tempSize);
        } else {
            views.setViewVisibility(R.id.temperature_text, View.GONE);
        }

        // Colors and Sizes
        views.setTextColor(R.id.clock_text, clockColor);
        views.setTextColor(R.id.ampm_text, clockColor);
        views.setTextColor(R.id.hijri_date_text, hijriColor);
        views.setTextColor(R.id.gregorian_date_text, gregColor);
        views.setTextColor(R.id.day_arabic_text, dayColor);
        views.setTextColor(R.id.day_english_text, dayColor);
        views.setTextColor(R.id.day_arabic_text_left, dayColor);
        views.setTextColor(R.id.day_english_text_left, dayColor);
        views.setTextColor(R.id.day_arabic_rotated, dayColor);
        views.setTextColor(R.id.day_english_rotated, dayColor);
        views.setTextColor(R.id.day_arabic_rotated_left, dayColor);
        views.setTextColor(R.id.day_english_rotated_left, dayColor);
        views.setTextColor(R.id.day_horizontal_text, dayColor);

        float baseLabelSize = largeTimeLabels ? (clockSize * 0.35f) : 14f;
        float ampmSize = largeTimeLabels ? (clockSize * 0.35f) : (float) (gregSize * 0.8f);

        views.setTextViewTextSize(R.id.clock_text, android.util.TypedValue.COMPLEX_UNIT_SP, clockSize);
        views.setTextViewTextSize(R.id.ampm_text, android.util.TypedValue.COMPLEX_UNIT_SP, ampmSize);
        views.setTextViewTextSize(R.id.hijri_date_text, android.util.TypedValue.COMPLEX_UNIT_SP, hijriSize);
        views.setTextViewTextSize(R.id.gregorian_date_text, android.util.TypedValue.COMPLEX_UNIT_SP, gregSize);

        int[] dayIds = {
            R.id.day_arabic_text, R.id.day_english_text,
            R.id.day_arabic_text_left, R.id.day_english_text_left,
            R.id.day_arabic_rotated, R.id.day_english_rotated,
            R.id.day_arabic_rotated_left, R.id.day_english_rotated_left,
            R.id.day_horizontal_text
        };
        for (int id : dayIds) {
            views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, baseLabelSize);
        }

        // Day Labels Logic
        SimpleDateFormat dayFormatEn = new SimpleDateFormat("EEE", Locale.ENGLISH);
        String[] arDays = {"", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
        String arDayStr = arDays[cal.get(Calendar.DAY_OF_WEEK)];

        if (isHorizontal) { // Normal stacked mode (Horizontal unrotated)
            int llNorm = dayTextLeft ? llLeftNorm : llRightNorm;
            views.setViewVisibility(llNorm, (showDayArabic || showDayEnglish) ? View.VISIBLE : View.GONE);
            views.setViewVisibility(arIdNorm, showDayArabic ? View.VISIBLE : View.GONE);
            views.setViewVisibility(enIdNorm, showDayEnglish ? View.VISIBLE : View.GONE);
            if (showDayArabic)
                views.setTextViewText(arIdNorm, arDayStr);
            if (showDayEnglish)
                views.setTextViewText(enIdNorm, dayFormatEn.format(cal.getTime()));
        } else { // Rotated mode (Vertical)
            // If they want BOTH, split them: English Left, Arabic Right
            if (showDayEnglish && showDayArabic) {
                views.setViewVisibility(llLeftRot, View.VISIBLE);
                views.setViewVisibility(llRightRot, View.VISIBLE);

                views.setViewVisibility(R.id.day_english_rotated_left, View.VISIBLE);
                views.setViewVisibility(R.id.day_arabic_rotated_left, View.GONE);
                views.setTextViewText(R.id.day_english_rotated_left, dayFormatEn.format(cal.getTime()));

                views.setViewVisibility(R.id.day_arabic_rotated, View.VISIBLE);
                views.setViewVisibility(R.id.day_english_rotated, View.GONE);
                views.setTextViewText(R.id.day_arabic_rotated, arDayStr);
            } else {
                // Otherwise respect the Left switch position
                int llRot = dayTextLeft ? llLeftRot : llRightRot;
                views.setViewVisibility(llRot, View.VISIBLE);
                views.setViewVisibility(arIdRot, showDayArabic ? View.VISIBLE : View.GONE);
                views.setViewVisibility(enIdRot, showDayEnglish ? View.VISIBLE : View.GONE);
                if (showDayArabic)
                    views.setTextViewText(arIdRot, arDayStr);
                if (showDayEnglish)
                    views.setTextViewText(enIdRot, dayFormatEn.format(cal.getTime()));
            }
        }

        // Clock Pattern
        if (use24h) {
            views.setCharSequence(R.id.clock_text, "setFormat12Hour", "kk:mm");
            views.setCharSequence(R.id.clock_text, "setFormat24Hour", "kk:mm");
        } else {
            views.setCharSequence(R.id.clock_text, "setFormat12Hour", "h:mm");
            views.setCharSequence(R.id.clock_text, "setFormat24Hour", "h:mm");

            // AM/PM Pattern
            String amPmFormat = "a";
            if (arabicAmPm) {
                amPmFormat = "aaaa";
            }
            views.setCharSequence(R.id.ampm_text, "setFormat12Hour", amPmFormat);
        }

        // Background
        int bgColor = 0x00000000;
        if (bgIdx == 1)
            bgColor = 0xFF000000;
        else if (bgIdx == 2)
            bgColor = 0xFFFFFFFF;
        else if (bgIdx == 3)
            bgColor = 0x88000000;
        else if (bgIdx == 4)
            bgColor = 0x44000000;
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor);

        // Gregorian
        int digitStyle = prefs.getInt("digit_style", 0);
        String gregPattern = "d MMMM yyyy";
        if (monthFormatIdx == 1 || monthFormatIdx == 3) {
            gregPattern = padDates ? "dd / MM / yyyy" : "d / M / yyyy";
        }
        if (language.equals("ja")) {
            gregPattern = "d日MMMM yyyy"; // e.g., 6日3月 2026
        }
        SimpleDateFormat df = new SimpleDateFormat(gregPattern, displayLocale);
        String gregStr = df.format(cal.getTime());
        String gSymbol, hSymbol;
        switch (language) {
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

        if (language.equals("ar") || arabicAmPm) {
            gregStr = "\u200F" + gregStr;
            if (showM)
                gregStr += gSymbol;
        } else {
            if (showM)
                gregStr += gSymbol;
        }
        views.setTextViewText(R.id.gregorian_date_text, HijriDateUtils.localizeDigits(gregStr, digitStyle));

        // Hijri
        int[] hijriArr = HijriDateUtils.getCurrentHijriDate(context, 0);
        String hijriStr = HijriDateUtils.formatHijriDate(hijriArr, language, monthFormatIdx, padDates);
        if (language.equals("ar") || arabicAmPm) {
            hijriStr = "\u200F" + hijriStr;
            if (showH)
                hijriStr += hSymbol;
        } else {
            if (showH)
                hijriStr += hSymbol;
        }
        views.setTextViewText(R.id.hijri_date_text, HijriDateUtils.localizeDigits(hijriStr, digitStyle));

        // Click action: Double Click to Sub-Settings / Hub
        Intent intent = new Intent(context, HijriCalendarWidget.class);
        intent.setAction(ACTION_DOUBLE_CLICK_CHECK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}