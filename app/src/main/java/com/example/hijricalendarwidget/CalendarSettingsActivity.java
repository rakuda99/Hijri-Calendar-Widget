package com.example.hijricalendarwidget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CalendarSettingsActivity extends AppCompatActivity {

    private Spinner spinnerMonthOrientation, spinnerMonthDayLanguage, spinnerWeekStart;
    private Spinner spinnerMonthGregColor, spinnerMonthHijriColor, spinnerMonthTodayColor, spinnerMonthEventColor,
            spinnerMonthDaySize, spinnerMonthWeekHeaderColor, spinnerMonthWeekHeaderSize, spinnerMonthBgColor;
    private androidx.appcompat.widget.SwitchCompat switchMonthDayBold, switchMonthHollowCircle, switchMonthShowArrows,
            switchMonthOpenApp, switchMonthShowGreg, switchMonthShowHijri;

    private static final String PREFS_NAME = "HijriWidgetPrefs";
    private String[] orientations = { "Horizontal (أفقي)", "Vertical (عمودي)" };
    private String[] calendarLangs = {
            "Auto (تلقائي)",
            "Arabic (العربية)",
            "English (الإنجليزية)"
    };
    private String[] weekStarts = { "Sunday (الأحد)", "Monday (الإثنين)", "Saturday (السبت)" };
    private String[] fontSizes = { "Small (صغير)", "Medium (متوسط)", "Large (كبير)", "Extra Large (ضخم)",
            "Huge (عملاق)" };
    private String[] textColors = { "White (أبيض)", "Black (أسود)", "Gold (ذهبي)", "Green (أخضر)",
            "Light Blue (سماوي)", "Yellow (أصفر)", "Red (أحمر)", "Orange (برتقالي)", "Pink (وردي)", "Purple (بنفسجي)",
            "Dark Blue (أزرق غامق)", "Gray (رمادي)", "Cyan (قرمزي)", "Lime (ليموني)", "Teal (فيروزي)" };
    private final int[] colorValues = { 0xFFFFFFFF, 0xFF000000, 0xFFFFD700, 0xFF4CAF50, 0xFFB3E5FC, 0xFFFFFF00,
            0xFFFF0000, 0xFFFFA500, 0xFFFFC0CB, 0xFF800080, 0xFF0D47A1, 0xFF9E9E9E, 0xFF00FFFF, 0xFF00FF00,
            0xFF008080 };
    private String[] bgColors = { "Transparent (شفاف)", "Semi-Black (أسود نصف شفاف)", "Solid Black (أسود)",
            "Semi-White (أبيض نصف شفاف)", "Solid White (أبيض)", "Dark Blue (أزرق غامق)", "Dark Gray (رمادي غامق)",
            "Dark Green (أخضر غامق)" };
    private final int[] bgColorValues = { 0x00000000, 0x88000000, 0xFF000000,
            0x88FFFFFF, 0xFFFFFFFF, 0xFF000077, 0xFF1A1A1A, 0xFF004400 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_calendar);

        initViews();
        applyLanguageToUI();
        setupSpinners();
        loadPreferences();

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            savePreferences();
            finish();
        });

        findViewById(R.id.btn_perm_calendar).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_CALENDAR }, 100);
            } else {
                Toast.makeText(this, "Permission already granted! ✅", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        spinnerMonthOrientation = findViewById(R.id.spinner_month_orientation);
        spinnerMonthDayLanguage = findViewById(R.id.spinner_month_day_language);
        spinnerWeekStart = findViewById(R.id.spinner_week_start);
        spinnerMonthGregColor = findViewById(R.id.spinner_month_greg_color);
        spinnerMonthHijriColor = findViewById(R.id.spinner_month_hijri_color);
        spinnerMonthTodayColor = findViewById(R.id.spinner_month_today_color);
        spinnerMonthEventColor = findViewById(R.id.spinner_month_event_color);
        spinnerMonthDaySize = findViewById(R.id.spinner_month_day_size);
        spinnerMonthWeekHeaderColor = findViewById(R.id.spinner_month_week_header_color);
        spinnerMonthWeekHeaderSize = findViewById(R.id.spinner_month_week_header_size);
        spinnerMonthBgColor = findViewById(R.id.spinner_month_bg_color);

        switchMonthDayBold = findViewById(R.id.switch_month_day_bold);
        switchMonthHollowCircle = findViewById(R.id.switch_month_hollow_circle);
        switchMonthShowArrows = findViewById(R.id.switch_month_show_arrows);
        switchMonthOpenApp = findViewById(R.id.switch_month_open_app);
        switchMonthShowGreg = findViewById(R.id.switch_month_show_greg);
        switchMonthShowHijri = findViewById(R.id.switch_month_show_hijri);
    }

    private void setupSpinners() {
        ArrayAdapter<String> orientationAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, orientations);
        orientationAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthOrientation.setAdapter(orientationAdapter);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, calendarLangs);
        langAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthDayLanguage.setAdapter(langAdapter);
        ArrayAdapter<String> weekStartAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, weekStarts);
        weekStartAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerWeekStart.setAdapter(weekStartAdapter);
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, fontSizes);
        sizeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthDaySize.setAdapter(sizeAdapter);
        spinnerMonthWeekHeaderSize.setAdapter(sizeAdapter);

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                textColors);
        colorAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthGregColor.setAdapter(colorAdapter);
        spinnerMonthHijriColor.setAdapter(colorAdapter);
        spinnerMonthTodayColor.setAdapter(colorAdapter);
        spinnerMonthEventColor.setAdapter(colorAdapter);
        spinnerMonthWeekHeaderColor.setAdapter(colorAdapter);

        ArrayAdapter<String> bgColorAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, bgColors);
        bgColorAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthBgColor.setAdapter(bgColorAdapter);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        spinnerMonthOrientation.setSelection(prefs.getInt("month_orientation", 0));
        spinnerMonthDayLanguage.setSelection(prefs.getInt("month_day_language_index", 0));
        spinnerWeekStart.setSelection(prefs.getInt("week_start", 0));
        spinnerMonthDaySize.setSelection(prefs.getInt("month_day_size_index", 1));
        spinnerMonthWeekHeaderSize.setSelection(prefs.getInt("month_week_header_size_index", 2)); // default Large/2

        spinnerMonthGregColor.setSelection(getColorIndex(prefs.getInt("month_greg_color", 0xFFFFFFFF)));
        spinnerMonthHijriColor.setSelection(getColorIndex(prefs.getInt("month_hijri_color", 0xFFFFFFFF)));
        spinnerMonthTodayColor.setSelection(getColorIndex(prefs.getInt("month_today_color", 0xFF4CAF50)));
        spinnerMonthEventColor.setSelection(getColorIndex(prefs.getInt("month_event_color", 0xFFFFD700)));
        spinnerMonthWeekHeaderColor.setSelection(getColorIndex(prefs.getInt("month_week_header_color", 0xFFFFD700))); // Gold
        spinnerMonthBgColor.setSelection(getBgColorIndex(prefs.getInt("month_bg_color", 0x88000000)));

        switchMonthDayBold.setChecked(prefs.getBoolean("month_day_bold", false));
        switchMonthHollowCircle.setChecked(prefs.getBoolean("month_hollow_circle", false));
        switchMonthShowArrows.setChecked(prefs.getBoolean("month_show_arrows", true));
        switchMonthOpenApp.setChecked(prefs.getBoolean("month_open_app", true));
        switchMonthShowGreg.setChecked(prefs.getBoolean("month_show_greg", true));
        switchMonthShowHijri.setChecked(prefs.getBoolean("month_show_hijri", true));
    }

    private void applyLanguageToUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString("app_language", "ar");

        // UI Labels Translation
        String titleStr = "Calendar Settings";
        String saveStr = "SAVE SETTINGS";
        String boldStr = "Bold Text";
        String hollowStr = "Hollow Circle";
        String arrowsStr = "Show Arrows";
        String openStr = "Open App on Click";
        String gregStr = "Show Greg";
        String hijriStr = "Show Hijri";
        String permStr = "Grant Permission";

        String lblOrientation = "Orientation";
        String lblDayLang = "Day Language";
        String lblWeekStart = "Week Start";
        String lblGregColor = "Greg Color";
        String lblHijriColor = "Hijri Color";
        String lblTodayCircle = "Today Circle";
        String lblEventDot = "Event Dot";
        String lblDaySize = "Day Size (Numbers)";
        String lblWeekHeaderColor = "Weekdays Color";
        String lblWeekHeaderSize = "Weekdays Size";
        String lblBgColor = "Background Color";

        switch (lang) {
            case "ar":
                titleStr = "إعدادات التقويم الشهري";
                saveStr = "حفظ الإعدادات";
                boldStr = "خط غامق";
                hollowStr = "دائرة مفرغة";
                arrowsStr = "عرض الأسهم";
                openStr = "فتح التطبيق عند اللمس";
                gregStr = "إظهار الميلادي";
                hijriStr = "إظهار الهجري";
                permStr = "منح الصلاحية";
                lblOrientation = "اتجاه التقويم";
                lblDayLang = "لغة الأيام";
                lblWeekStart = "بداية الأسبوع";
                lblGregColor = "لون الميلادي";
                lblHijriColor = "لون الهجري";
                lblTodayCircle = "دائرة اليوم";
                lblEventDot = "نقطة الموعد";
                lblDaySize = "حجم أرقام الأيام";
                lblWeekHeaderColor = "لون أسماء الأيام";
                lblWeekHeaderSize = "حجم أسماء الأيام";
                lblBgColor = "لون الخلفية";
                orientations = new String[] { "أفقي", "عمودي" };
                weekStarts = new String[] { "الأحد", "الإثنين", "السبت" };
                textColors = new String[] { "أبيض", "أسود", "ذهبي", "أخضر", "سماوي", "أصفر", "أحمر", "برتقالي", "وردي",
                        "بنفسجي", "أزرق غامق", "رمادي", "قرمزي", "ليموني", "فيروزي" };
                bgColors = new String[] { "شفاف", "أسود نصف شفاف", "أسود", "أبيض نصف شفاف", "أبيض", "أزرق غامق",
                        "رمادي غامق", "أخضر غامق" };
                fontSizes = new String[] { "صغير", "متوسط", "كبير", "ضخم", "عملاق" };
                break;
            default: // English
                orientations = new String[] { "Horizontal", "Vertical" };
                weekStarts = new String[] { "Sunday", "Monday", "Saturday" };
                textColors = new String[] { "White", "Black", "Gold", "Green", "Light Blue", "Yellow", "Red", "Orange",
                        "Pink", "Purple", "Dark Blue", "Gray", "Cyan", "Lime", "Teal" };
                bgColors = new String[] { "Transparent", "Semi-Black", "Solid Black", "Semi-White", "Solid White",
                        "Dark Blue", "Dark Gray", "Dark Green" };
                fontSizes = new String[] { "Small", "Medium", "Large", "Extra Large", "Huge" };
                break;
        }

        ((android.widget.TextView) findViewById(R.id.lbl_title)).setText(titleStr);
        ((android.widget.TextView) findViewById(R.id.btn_save)).setText(saveStr);
        ((android.widget.TextView) findViewById(R.id.btn_perm_calendar)).setText(permStr);
        switchMonthDayBold.setText(boldStr);
        switchMonthHollowCircle.setText(hollowStr);
        switchMonthShowArrows.setText(arrowsStr);
        switchMonthOpenApp.setText(openStr);
        switchMonthShowGreg.setText(gregStr);
        switchMonthShowHijri.setText(hijriStr);

        ((android.widget.TextView) findViewById(R.id.lbl_orientation)).setText(lblOrientation);
        ((android.widget.TextView) findViewById(R.id.lbl_day_lang)).setText(lblDayLang);
        ((android.widget.TextView) findViewById(R.id.lbl_week_start)).setText(lblWeekStart);
        ((android.widget.TextView) findViewById(R.id.lbl_greg_color)).setText(lblGregColor);
        ((android.widget.TextView) findViewById(R.id.lbl_hijri_color)).setText(lblHijriColor);
        ((android.widget.TextView) findViewById(R.id.lbl_today_circle)).setText(lblTodayCircle);
        ((android.widget.TextView) findViewById(R.id.lbl_event_dot)).setText(lblEventDot);
        ((android.widget.TextView) findViewById(R.id.lbl_day_size)).setText(lblDaySize);
        ((android.widget.TextView) findViewById(R.id.lbl_week_header_color)).setText(lblWeekHeaderColor);
        ((android.widget.TextView) findViewById(R.id.lbl_week_header_size)).setText(lblWeekHeaderSize);
        ((android.widget.TextView) findViewById(R.id.lbl_bg_color)).setText(lblBgColor);

        // Update adapters to reflect new localized arrays
        setupSpinners();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("month_orientation", spinnerMonthOrientation.getSelectedItemPosition());
        editor.putInt("month_day_language_index", spinnerMonthDayLanguage.getSelectedItemPosition());
        editor.putInt("week_start", spinnerWeekStart.getSelectedItemPosition());
        editor.putInt("month_day_size_index", spinnerMonthDaySize.getSelectedItemPosition());
        editor.putInt("month_week_header_size_index", spinnerMonthWeekHeaderSize.getSelectedItemPosition());

        editor.putInt("month_greg_color", colorValues[spinnerMonthGregColor.getSelectedItemPosition()]);
        editor.putInt("month_hijri_color", colorValues[spinnerMonthHijriColor.getSelectedItemPosition()]);
        editor.putInt("month_today_color", colorValues[spinnerMonthTodayColor.getSelectedItemPosition()]);
        editor.putInt("month_event_color", colorValues[spinnerMonthEventColor.getSelectedItemPosition()]);
        editor.putInt("month_week_header_color", colorValues[spinnerMonthWeekHeaderColor.getSelectedItemPosition()]);
        editor.putInt("month_bg_color", bgColorValues[spinnerMonthBgColor.getSelectedItemPosition()]);

        editor.putBoolean("month_day_bold", switchMonthDayBold.isChecked());
        editor.putBoolean("month_hollow_circle", switchMonthHollowCircle.isChecked());
        editor.putBoolean("month_show_arrows", switchMonthShowArrows.isChecked());
        editor.putBoolean("month_open_app", switchMonthOpenApp.isChecked());
        editor.putBoolean("month_show_greg", switchMonthShowGreg.isChecked());
        editor.putBoolean("month_show_hijri", switchMonthShowHijri.isChecked());

        editor.commit();

        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());

            // --- Update Month Widgets ---
            int[] monthIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(getApplication(), HijriMonthWidget.class));
            if (monthIds != null && monthIds.length > 0) {
                Intent intentMonth = new Intent(this, HijriMonthWidget.class);
                intentMonth.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentMonth.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, monthIds);
                sendBroadcast(intentMonth);
                // Also force it directly inside the app process
                for (int id : monthIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.calendar_grid);
                    appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.calendar_grid_vertical);
                }
            }

            // --- Update Clock Widgets ---
            int[] clockIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(getApplication(), HijriCalendarWidget.class));
            if (clockIds != null && clockIds.length > 0) {
                Intent intentClock = new Intent(this, HijriCalendarWidget.class);
                intentClock.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentClock.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, clockIds);
                sendBroadcast(intentClock);
                // Clock might not have calendar_grid, but won't hurt to let the broadcast
                // handle it
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "تم الحفظ! ✅ Calendar Updated.", Toast.LENGTH_SHORT).show();
    }

    private int getColorIndex(int color) {
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i] == color)
                return i;
        }
        return 0;
    }

    private int getBgColorIndex(int color) {
        for (int i = 0; i < bgColorValues.length; i++) {
            if (bgColorValues[i] == color)
                return i;
        }
        return 1; // Default to Semi-Black
    }
}
