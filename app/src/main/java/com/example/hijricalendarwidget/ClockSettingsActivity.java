package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ClockSettingsActivity extends AppCompatActivity {

    private Spinner spinnerLanguage, spinnerDigitStyle;
    private Spinner spinnerClockColor, spinnerClockSize, spinnerHijriColor, spinnerHijriSize, spinnerHijriCorrection;
    private Spinner spinnerGregColor, spinnerGregSize, spinnerBgColor, spinnerMonthFormat, spinnerDayColor,
            spinnerDayLanguage;
    private androidx.appcompat.widget.SwitchCompat switchShowClockVis, switchShowHijriVis, switchShowGregVis;
    private androidx.appcompat.widget.SwitchCompat switchShowH, switchShowM, switchArabicAmPm, switchUse24h;
    private androidx.appcompat.widget.SwitchCompat switchDayPositionLeft, switchDayHorizontal, switchLargeTimeLabels, switchShowTemperature;

    private static final String PREFS_NAME = "HijriWidgetPrefs";
    private String[] languages = { "Auto (تلقائي)", "Arabic (العربية)", "English (الإنجليزية)" };
    private String[] fontSizes = { "Small (صغير)", "Medium (متوسط)", "Large (كبير)", "Extra Large (ضخم)",
            "Huge (عملاق)" };
    private String[] textColors = { "White (أبيض)", "Black (أسود)", "Gold (ذهبي)", "Green (أخضر)",
            "Light Blue (سماوي)", "Yellow (أصفر)", "Red (أحمر)", "Orange (برتقالي)", "Pink (وردي)", "Purple (بنفسجي)",
            "Dark Blue (أزرق غامق)", "Gray (رمادي)", "Cyan (قرمزي)", "Lime (ليموني)", "Teal (فيروزي)" };
    private final int[] colorValues = { 0xFFFFFFFF, 0xFF000000, 0xFFFFD700, 0xFF4CAF50, 0xFFB3E5FC, 0xFFFFFF00,
            0xFFFF0000, 0xFFFFA500, 0xFFFFC0CB, 0xFF800080, 0xFF0D47A1, 0xFF9E9E9E, 0xFF00FFFF, 0xFF00FF00,
            0xFF008080 };
    private String[] bgColors = { "Transparent (شفاف)", "Black (أسود)", "White (أبيض)", "Dark Glass (زجاجي داكن)",
            "Light Glass (زجاجي فاتح)", "Semi-Transparent Black" };
    private String[] monthFormats = { "Month Name (شهر باسم)", "Month Number (شهر برقم)",
            "Day & Month Name (يوم وشهر باسم)", "Day & Month Number (يوم وشهر برقم)" };
    private String[] dayDisplayOptions = { "Both / عربي وإنجليزي", "Arabic Only / عربي فقط",
            "English Only / إنجليزي فقط", "Hidden / مخفي" };
    private String[] digitStyles = { "Arabic (0-9)", "Hindi/Eastern (٠-٩)" };
    private String[] hijriCorrections = { "-2 Days", "-1 Day", "0 (Default)", "+1 Day", "+2 Days" };
    private final int[] hijriCorrectionValues = { -2, -1, 0, 1, 2 };
    private String[] temperatureIntervals = { "15 Minutes / 15 دقيقة", "30 Minutes / 30 دقيقة", "1 Hour / ساعة واحدة", "2 Hours / ساعتان", "6 Hours / 6 ساعات" };
    private final int[] temperatureIntervalValues = { 15, 30, 60, 120, 360 };
    private Spinner spinnerTemperatureInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_clock);

        initViews();
        applyLanguageToUI();
        setupSpinners();
        loadPreferences();

        switchShowTemperature.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkLocationPermission();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            savePreferences();
            finish(); // Close activity after save
        });

        findViewById(R.id.btn_sync_time).setOnClickListener(v -> {
            HijriDateUtils.syncTimeWithNetwork(this, true);
            Toast.makeText(this, "تمت المزامنة! ✅", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        spinnerLanguage = findViewById(R.id.spinner_language);
        spinnerDigitStyle = findViewById(R.id.spinner_digit_style);
        spinnerClockColor = findViewById(R.id.spinner_clock_color);
        spinnerClockSize = findViewById(R.id.spinner_clock_size);
        spinnerHijriColor = findViewById(R.id.spinner_hijri_color);
        spinnerHijriSize = findViewById(R.id.spinner_hijri_size);
        spinnerHijriCorrection = findViewById(R.id.spinner_hijri_correction);
        spinnerGregColor = findViewById(R.id.spinner_greg_color);
        spinnerGregSize = findViewById(R.id.spinner_greg_size);
        spinnerBgColor = findViewById(R.id.spinner_bg_color);
        spinnerMonthFormat = findViewById(R.id.spinner_month_format);
        spinnerDayColor = findViewById(R.id.spinner_day_color);
        spinnerDayLanguage = findViewById(R.id.spinner_day_language);

        switchShowClockVis = findViewById(R.id.switch_show_clock_visibility);
        switchShowHijriVis = findViewById(R.id.switch_show_hijri_visibility);
        switchShowGregVis = findViewById(R.id.switch_show_greg_visibility);
        switchShowH = findViewById(R.id.switch_show_h);
        switchShowM = findViewById(R.id.switch_show_m);
        switchArabicAmPm = findViewById(R.id.switch_arabic_ampm);
        switchUse24h = findViewById(R.id.switch_use_24h);
        switchDayPositionLeft = findViewById(R.id.switch_day_position_left);
        switchDayHorizontal = findViewById(R.id.switch_day_horizontal);
        switchLargeTimeLabels = findViewById(R.id.switch_large_time_labels);
        switchShowTemperature = findViewById(R.id.switch_show_temperature);
        spinnerTemperatureInterval = findViewById(R.id.spinner_temperature_interval);
    }

    private void setupSpinners() {
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, languages);
        langAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerLanguage.setAdapter(langAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, fontSizes);
        sizeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerClockSize.setAdapter(sizeAdapter);
        spinnerHijriSize.setAdapter(sizeAdapter);
        spinnerGregSize.setAdapter(sizeAdapter);

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, textColors);
        colorAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerClockColor.setAdapter(colorAdapter);
        spinnerHijriColor.setAdapter(colorAdapter);
        spinnerGregColor.setAdapter(colorAdapter);
        spinnerDayColor.setAdapter(colorAdapter);

        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, bgColors);
        bgAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerBgColor.setAdapter(bgAdapter);

        ArrayAdapter<String> mFormatAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, monthFormats);
        mFormatAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerMonthFormat.setAdapter(mFormatAdapter);

        ArrayAdapter<String> dayLanguageAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                dayDisplayOptions);
        dayLanguageAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerDayLanguage.setAdapter(dayLanguageAdapter);

        ArrayAdapter<String> digitAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, digitStyles);
        digitAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerDigitStyle.setAdapter(digitAdapter);

        ArrayAdapter<String> correctionAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                hijriCorrections);
        correctionAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerHijriCorrection.setAdapter(correctionAdapter);

        ArrayAdapter<String> tempIntervalAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, temperatureIntervals);
        tempIntervalAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerTemperatureInterval.setAdapter(tempIntervalAdapter);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String lang = prefs.getString("language", "auto");
        if (lang.equals("auto"))
            spinnerLanguage.setSelection(0);
        else if (lang.equals("ar"))
            spinnerLanguage.setSelection(1);
        else
            spinnerLanguage.setSelection(2);

        spinnerDigitStyle.setSelection(prefs.getInt("digit_style", 0));
        spinnerClockColor.setSelection(getColorIndex(prefs.getInt("clock_color", 0xFFFFFFFF)));
        spinnerHijriColor.setSelection(getColorIndex(prefs.getInt("hijri_color", 0xFFFFFFFF)));
        spinnerGregColor.setSelection(getColorIndex(prefs.getInt("greg_color", 0xFFFFFFFF)));
        spinnerClockSize.setSelection(prefs.getInt("clock_size_index", 1));
        spinnerHijriSize.setSelection(prefs.getInt("hijri_size_index", 1));
        spinnerGregSize.setSelection(prefs.getInt("greg_size_index", 1));
        spinnerBgColor.setSelection(prefs.getInt("bg_color_index", 0));
        spinnerMonthFormat.setSelection(prefs.getInt("month_format_index", 0));

        int corrValue = prefs.getInt("hijri_correction", 0);
        for (int i = 0; i < hijriCorrectionValues.length; i++) {
            if (hijriCorrectionValues[i] == corrValue) {
                spinnerHijriCorrection.setSelection(i);
                break;
            }
        }

        switchShowClockVis.setChecked(prefs.getBoolean("show_clock", true));
        switchShowHijriVis.setChecked(prefs.getBoolean("show_hijri", true));
        switchShowGregVis.setChecked(prefs.getBoolean("show_greg", true));
        switchShowH.setChecked(prefs.getBoolean("show_h_symbol", true));
        switchShowM.setChecked(prefs.getBoolean("show_m_symbol", true));
        switchArabicAmPm.setChecked(prefs.getBoolean("arabic_ampm", true));
        switchUse24h.setChecked(prefs.getBoolean("use_24h", false));
        switchLargeTimeLabels.setChecked(prefs.getBoolean("large_time_labels", false));
        switchShowTemperature.setChecked(prefs.getBoolean("show_temperature", false));

        int tempIntervalValue = prefs.getInt("temperature_interval", 30);
        for (int i = 0; i < temperatureIntervalValues.length; i++) {
            if (temperatureIntervalValues[i] == tempIntervalValue) {
                spinnerTemperatureInterval.setSelection(i);
                break;
            }
        }

        // Migrate old prefs -> spinner selection
        int dayMode = 0; // Both Default
        if (prefs.contains("day_display_mode")) {
            dayMode = prefs.getInt("day_display_mode", 0);
        } else {
            boolean sa = prefs.getBoolean("show_day_arabic_v2", true);
            boolean se = prefs.getBoolean("show_day_english_v2", true);
            if (sa && !se)
                dayMode = 1; // Arabic only
            else if (!sa && se)
                dayMode = 2; // English only
            else if (!sa && !se)
                dayMode = 3; // Hidden
        }
        spinnerDayLanguage.setSelection(dayMode);

        switchDayPositionLeft.setChecked(prefs.getBoolean("day_position_left", false));
        switchDayHorizontal.setChecked(prefs.getInt("day_orientation", 0) == 1);
        spinnerDayColor.setSelection(getColorIndex(prefs.getInt("day_color", 0xFFFFFFFF)));
    }

    private void applyLanguageToUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString("app_language", "ar");

        String titleStr = "Clock Widget Settings";
        String saveStr = "SAVE SETTINGS";
        String syncStr = "Network Sync";

        switch (lang) {
            case "ar":
                titleStr = "إعدادات الساعة والويدجت";
                saveStr = "حفظ الإعدادات";
                syncStr = "مزامنة الشبكة";
                languages = new String[] { "تلقائي", "العربية", "الإنجليزية" };
                fontSizes = new String[] { "صغير", "متوسط", "كبير", "ضخم", "عملاق" };
                textColors = new String[] { "أبيض", "أسود", "ذهبي", "أخضر", "سماوي", "أصفر", "أحمر", "برتقالي", "وردي",
                        "بنفسجي", "أزرق غامق", "رمادي", "قرمزي", "ليموني", "فيروزي" };
                bgColors = new String[] { "شفاف", "أسود", "أبيض", "زجاجي داكن", "زجاجي فاتح", "أسود نصف شفاف" };
                monthFormats = new String[] { "اسم الشهر", "رقم الشهر", "اليوم واسم الشهر", "اليوم ورقم الشهر" };
                digitStyles = new String[] { "أرقام عربية (0-9)", "أرقام هندية (٠-٩)" };
                hijriCorrections = new String[] { "-2 يوم", "-1 يوم", "0 (افتراضي)", "+1 يوم", "+2 يوم" };
                temperatureIntervals = new String[] { "15 دقيقة", "30 دقيقة", "ساعة واحدة", "ساعتان", "6 ساعات" };
                break;
            default:
                break;
        }

        // Update basic UI labels
        android.widget.TextView titleAr = findViewById(R.id.title_ar);
        android.widget.TextView titleEn = findViewById(R.id.title_en);
        if (lang.equals("ar")) {
            titleAr.setText(titleStr);
            titleAr.setVisibility(android.view.View.VISIBLE);
            titleEn.setVisibility(android.view.View.GONE);
        } else {
            titleEn.setText(titleStr);
            titleEn.setVisibility(android.view.View.VISIBLE);
            titleAr.setVisibility(android.view.View.GONE);
        }

        ((android.widget.Button) findViewById(R.id.btn_save)).setText(saveStr);
        ((android.widget.Button) findViewById(R.id.btn_sync_time)).setText(syncStr);

        // Re-setup spinners with localized arrays
        setupSpinners();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int langPos = spinnerLanguage.getSelectedItemPosition();
        if (langPos == 0)
            editor.putString("language", "auto");
        else if (langPos == 1)
            editor.putString("language", "ar");
        else
            editor.putString("language", "en");

        editor.putInt("digit_style", spinnerDigitStyle.getSelectedItemPosition());
        editor.putInt("clock_color", colorValues[spinnerClockColor.getSelectedItemPosition()]);
        editor.putInt("hijri_color", colorValues[spinnerHijriColor.getSelectedItemPosition()]);
        editor.putInt("greg_color", colorValues[spinnerGregColor.getSelectedItemPosition()]);
        editor.putInt("clock_size_index", spinnerClockSize.getSelectedItemPosition());
        editor.putInt("hijri_size_index", spinnerHijriSize.getSelectedItemPosition());
        editor.putInt("greg_size_index", spinnerGregSize.getSelectedItemPosition());
        editor.putInt("bg_color_index", spinnerBgColor.getSelectedItemPosition());
        editor.putInt("month_format_index", spinnerMonthFormat.getSelectedItemPosition());
        editor.putInt("hijri_correction", hijriCorrectionValues[spinnerHijriCorrection.getSelectedItemPosition()]);

        editor.putBoolean("show_clock", switchShowClockVis.isChecked());
        editor.putBoolean("show_hijri", switchShowHijriVis.isChecked());
        editor.putBoolean("show_greg", switchShowGregVis.isChecked());
        editor.putBoolean("show_h_symbol", switchShowH.isChecked());
        editor.putBoolean("show_m_symbol", switchShowM.isChecked());
        editor.putBoolean("arabic_ampm", switchArabicAmPm.isChecked());
        editor.putBoolean("use_24h", switchUse24h.isChecked());
        editor.putBoolean("large_time_labels", switchLargeTimeLabels.isChecked());
        editor.putBoolean("show_temperature", switchShowTemperature.isChecked());
        editor.putInt("temperature_interval", temperatureIntervalValues[spinnerTemperatureInterval.getSelectedItemPosition()]);

        int dayMode = spinnerDayLanguage.getSelectedItemPosition();
        editor.putInt("day_display_mode", dayMode);

        // Legacy compat
        editor.putBoolean("show_day_arabic_v2", dayMode == 0 || dayMode == 1);
        editor.putBoolean("show_day_english_v2", dayMode == 0 || dayMode == 2);

        editor.putBoolean("day_position_left", switchDayPositionLeft.isChecked());
        editor.putInt("day_orientation", switchDayHorizontal.isChecked() ? 1 : 0);
        editor.putInt("day_color", colorValues[spinnerDayColor.getSelectedItemPosition()]);

        editor.commit();

        // Reschedule/trigger weather sync based on new preferences
        WeatherUtils.scheduleWeatherSync(this);

        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());

            // --- Update Clock Widgets ---
            int[] clockIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(getApplication(), HijriCalendarWidget.class));
            if (clockIds != null && clockIds.length > 0) {
                Intent intentClock = new Intent(this, HijriCalendarWidget.class);
                intentClock.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentClock.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, clockIds);
                sendBroadcast(intentClock);
            }

            // --- Update Month Widgets ---
            int[] monthIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(getApplication(), HijriMonthWidget.class));
            if (monthIds != null && monthIds.length > 0) {
                Intent intentMonth = new Intent(this, HijriMonthWidget.class);
                intentMonth.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentMonth.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, monthIds);
                sendBroadcast(intentMonth);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "تم الحفظ! ✅ Settings Saved.", Toast.LENGTH_SHORT).show();
    }

    private int getColorIndex(int color) {
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i] == color)
                return i;
        }
        return 0;
    }

    private void checkLocationPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION },
                    200);
        }
    }
}
