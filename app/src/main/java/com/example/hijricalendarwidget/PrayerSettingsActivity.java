package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import org.json.JSONObject;

public class PrayerSettingsActivity extends AppCompatActivity {

    private Spinner spinnerLayoutMode, spinnerBgType, spinnerHighlightColor, spinnerTextColor, spinnerPrayerTextSize, spinnerPrayerTimeColor, spinnerWidgetLang, spinnerCalcMethod, spinnerAsrJuristic;
    private SwitchCompat switchShowClock, switchShowShuruq, switchShowRemaining, switchShowTimes, switchShowNextOnly,
            switchShowRemainingOnly;
    private static final String PREFS_NAME = "HijriWidgetPrefs";

    private final String[] layoutModes = { "Horizontal (أفقي)", "Vertical (عمودي)" };
    private final String[] bgTypes = { "Transparent (شفاف)", "Solid Dark (غامق)", "Solid Light (فاتح)",
            "Gradient (متدرج)", "Glass (زجاجي)" };
    private final String[] highlightColors = {
            "Golden Solid (ذهبي ممتلئ)",
            "Golden Border (إطار ذهبي)",
            "Teal Border (إطار تركوازي)",
            "White Border (إطار أبيض)",
            "Transparent (بدون)"
    };
    private final String[] textColors = { "White (أبيض)", "Black (أسود)", "Gray (رمادي)", "Gold (ذهبي)", "Green (أخضر)", "Yellow (أصفر)", "Cyan (قرمزي)" };
    private final String[] timeColorValues = { "#FFFFFF", "#000000", "#888888", "#FFD700", "#4CAF50", "#FFFF00", "#00FFFF" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_prayer);

        initViews();
        applyLanguage();
        setupSpinners();
        loadPreferences();

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            savePreferences();
            finish();
        });

        findViewById(R.id.btn_update_now).setOnClickListener(v -> {
            updateWidgetNow();
        });

        findViewById(R.id.btn_sync_gps).setOnClickListener(v -> {
            checkPermissionAndSyncLocation();
        });
    }

    private void initViews() {
        spinnerLayoutMode = findViewById(R.id.spinner_layout_mode);
        spinnerBgType = findViewById(R.id.spinner_bg_type);
        spinnerHighlightColor = findViewById(R.id.spinner_highlight_color);
        spinnerTextColor = findViewById(R.id.spinner_text_color);
        spinnerPrayerTextSize = findViewById(R.id.spinner_prayer_text_size);
        spinnerPrayerTimeColor = findViewById(R.id.spinner_prayer_time_color);

        switchShowClock = findViewById(R.id.switch_show_clock);
        switchShowShuruq = findViewById(R.id.switch_show_shuruq);
        switchShowRemaining = findViewById(R.id.switch_show_remaining);
        switchShowTimes = findViewById(R.id.switch_show_times);
        switchShowNextOnly = findViewById(R.id.switch_show_next_only);
        switchShowRemainingOnly = findViewById(R.id.switch_show_remaining_only);
        spinnerWidgetLang = findViewById(R.id.spinner_widget_lang);
        spinnerCalcMethod = findViewById(R.id.spinner_calc_method);
        spinnerAsrJuristic = findViewById(R.id.spinner_asr_juristic);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, layoutModes);
        adapter1.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerLayoutMode.setAdapter(adapter1);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, bgTypes);
        adapter2.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerBgType.setAdapter(adapter2);

        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, highlightColors);
        adapter3.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerHighlightColor.setAdapter(adapter3);

        ArrayAdapter<String> adapter4 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, textColors);
        adapter4.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerTextColor.setAdapter(adapter4);
        spinnerPrayerTimeColor.setAdapter(adapter4);

        ArrayAdapter<String> adapterSize = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                new String[] { "Small (صغير)", "Medium (متوسط)", "Large (كبير)", "X-Large (كبير جداً)", "Huge (عملاق)" });
        adapterSize.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerPrayerTextSize.setAdapter(adapterSize);

        ArrayAdapter<String> adapter5 = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                new String[] {
                        "Arabic (العربية)",
                        "English (الإنجليزية)"
                });
        adapter5.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerWidgetLang.setAdapter(adapter5);

        ArrayAdapter<String> adapter6 = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                new String[] {
                        "Umm Al-Qura (ام القرى)",
                        "Muslim World League (الرابطة)",
                        "ISNA (أمريكا الشمالية)",
                        "Egypt (الهيئة المصرية)",
                        "Karachi (جامعة كراتشي)",
                        "Tehran (جامعة طهران)",
                        "Jafari (الشيعة الإثنى عشرية)"
                });
        adapter6.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerCalcMethod.setAdapter(adapter6);

        ArrayAdapter<String> adapter7 = new ArrayAdapter<>(this, R.layout.custom_spinner_item,
                new String[] { "Shafii, Maliki, Hanbali (شافعي)", "Hanafi (حنفي)" });
        adapter7.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerAsrJuristic.setAdapter(adapter7);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String layout = prefs.getString("prayer_layout_mode", "horizontal");
        spinnerLayoutMode.setSelection(layout.equals("vertical") ? 1 : 0);

        String bg = prefs.getString("prayer_bg_type", "transparent");
        if (bg.equals("solid_dark"))
            spinnerBgType.setSelection(1);
        else if (bg.equals("solid_light"))
            spinnerBgType.setSelection(2);
        else if (bg.equals("gradient"))
            spinnerBgType.setSelection(3);
        else if (bg.equals("glass"))
            spinnerBgType.setSelection(4);
        else
            spinnerBgType.setSelection(0);

        String highlight = prefs.getString("prayer_highlight_color", "golden_solid");
        if (highlight.equals("golden_stroke"))
            spinnerHighlightColor.setSelection(1);
        else if (highlight.equals("teal_stroke"))
            spinnerHighlightColor.setSelection(2);
        else if (highlight.equals("white_stroke"))
            spinnerHighlightColor.setSelection(3);
        else if (highlight.equals("transparent"))
            spinnerHighlightColor.setSelection(4);
        else
            spinnerHighlightColor.setSelection(0);

        String color = prefs.getString("prayer_text_color", "#FFFFFF");
        int foundColorIdx = 0;
        for (int i=0; i<timeColorValues.length; i++) { if (timeColorValues[i].equals(color)) foundColorIdx = i; }
        spinnerTextColor.setSelection(foundColorIdx);

        String tColor = prefs.getString("prayer_time_color", "faded"); // 'faded' is default
        int foundTColorIdx = 2; // Default to Gray usually, but 'faded' means use opacity
        if (tColor.equals("faded")) foundTColorIdx = 2;
        else {
            for (int i=0; i<timeColorValues.length; i++) { if (timeColorValues[i].equals(tColor)) foundTColorIdx = i; }
        }
        spinnerPrayerTimeColor.setSelection(foundTColorIdx);

        spinnerPrayerTextSize.setSelection(prefs.getInt("prayer_text_size_index", 2));

        switchShowClock.setChecked(prefs.getBoolean("prayer_show_clock", false));
        switchShowShuruq.setChecked(prefs.getBoolean("prayer_show_shuruq", false));
        switchShowRemaining.setChecked(prefs.getBoolean("prayer_show_remaining", true));
        switchShowTimes.setChecked(prefs.getBoolean("prayer_show_times", true));
        switchShowNextOnly.setChecked(prefs.getBoolean("prayer_show_next_only", false));
        switchShowRemainingOnly.setChecked(prefs.getBoolean("prayer_show_remaining_only", false));
        String wLang = prefs.getString("prayer_widget_lang", "ar");
        int langPos = wLang.equals("en") ? 1 : 0;
        spinnerWidgetLang.setSelection(langPos);

        spinnerCalcMethod.setSelection(prefs.getInt("prayer_calc_method", 0));
        spinnerAsrJuristic.setSelection(prefs.getInt("prayer_asr_juristic", 0));

        // Display sync timestamps
        TextView txtPrayerSync = findViewById(R.id.txt_last_prayer_sync);
        TextView txtLocationSync = findViewById(R.id.txt_last_location_sync);
        long prayerMs = CacheManager.getLastPrayerUpdateMs(this);
        long locationMs = CacheManager.getLastLocationUpdateMs(this);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale.getDefault());
        if (prayerMs > 0) {
            txtPrayerSync.setText("Last Prayer Sync: " + sdf.format(prayerMs));
        } else {
            txtPrayerSync.setText("Last Prayer Sync: --");
        }
        if (locationMs > 0) {
            long now = System.currentTimeMillis();
            long diffMin = (now - locationMs) / (1000 * 60);
            if (diffMin < 1) {
                txtLocationSync.setText("Updated: just now");
            } else if (diffMin < 60) {
                txtLocationSync.setText("Updated: " + diffMin + " min ago");
            } else if (diffMin < 1440) {
                txtLocationSync.setText("Updated: " + (diffMin / 60) + " hours ago");
            } else {
                txtLocationSync.setText("Updated: " + sdf.format(locationMs));
            }
        } else {
            txtLocationSync.setText("Updated: --");
        }
    }

    private void applyLanguage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString("app_language", "ar");

        if (lang.equals("ar")) {
            ((android.widget.TextView) findViewById(R.id.btn_save)).setText("حفظ الإعدادات");
            switchShowClock.setText("عرض الساعة الحالية");
            switchShowShuruq.setText("عرض الشروق");
            switchShowRemaining.setText("إظهار الوقت المتبقي");
            switchShowTimes.setText("عرض الأوقات");
            switchShowNextOnly.setText("الصلاة القادمة فقط");
            switchShowRemainingOnly.setText("عرض العد التنازلي فقط");
            ((android.widget.TextView) findViewById(R.id.lbl_widget_lang)).setText("لغة الويدجت");
            ((android.widget.TextView) findViewById(R.id.lbl_calc_method)).setText("طريقة الحساب");
            ((android.widget.TextView) findViewById(R.id.lbl_asr_juristic)).setText("مذهب العصر");
            if (findViewById(R.id.lbl_prayer_text_size) != null) ((android.widget.TextView) findViewById(R.id.lbl_prayer_text_size)).setText("حجم نص الأوقات");
            if (findViewById(R.id.lbl_prayer_time_color) != null) ((android.widget.TextView) findViewById(R.id.lbl_prayer_time_color)).setText("لون أوقات الصلوات");
        } else if (lang.equals("en")) {
            ((android.widget.TextView) findViewById(R.id.btn_save)).setText("SAVE SETTINGS");
            switchShowClock.setText("Show Current Clock");
            switchShowShuruq.setText("Show Sunrise (Shuruq)");
            switchShowRemaining.setText("Show Remaining Time");
            switchShowTimes.setText("Show Prayer Times");
            switchShowNextOnly.setText("Next Prayer Only");
            switchShowRemainingOnly.setText("Remaining Time Only");
            ((android.widget.TextView) findViewById(R.id.lbl_widget_lang)).setText("Widget Language");
            ((android.widget.TextView) findViewById(R.id.lbl_calc_method)).setText("Calculation Method");
            ((android.widget.TextView) findViewById(R.id.lbl_asr_juristic)).setText("Asr Madhab");
            if (findViewById(R.id.lbl_prayer_text_size) != null) ((android.widget.TextView) findViewById(R.id.lbl_prayer_text_size)).setText("Prayer Text Size");
            if (findViewById(R.id.lbl_prayer_time_color) != null) ((android.widget.TextView) findViewById(R.id.lbl_prayer_time_color)).setText("Prayer Times Color");
        }
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("prayer_layout_mode",
                spinnerLayoutMode.getSelectedItemPosition() == 1 ? "vertical" : "horizontal");

        int bgPos = spinnerBgType.getSelectedItemPosition();
        if (bgPos == 1)
            editor.putString("prayer_bg_type", "solid_dark");
        else if (bgPos == 2)
            editor.putString("prayer_bg_type", "solid_light");
        else if (bgPos == 3)
            editor.putString("prayer_bg_type", "gradient");
        else if (bgPos == 4)
            editor.putString("prayer_bg_type", "glass");
        else
            editor.putString("prayer_bg_type", "transparent");

        int highPos = spinnerHighlightColor.getSelectedItemPosition();
        if (highPos == 1)
            editor.putString("prayer_highlight_color", "golden_stroke");
        else if (highPos == 2)
            editor.putString("prayer_highlight_color", "teal_stroke");
        else if (highPos == 3)
            editor.putString("prayer_highlight_color", "white_stroke");
        else if (highPos == 4)
            editor.putString("prayer_highlight_color", "transparent");
        else
            editor.putString("prayer_highlight_color", "golden_solid");

        int colorPos = spinnerTextColor.getSelectedItemPosition();
        editor.putString("prayer_text_color", timeColorValues[colorPos]);

        int timeColorPos = spinnerPrayerTimeColor.getSelectedItemPosition();
        // If gray is selected for time color, by default it means faded. Otherwise precise color.
        if (timeColorPos == 2) {
            editor.putString("prayer_time_color", "faded");
        } else {
            editor.putString("prayer_time_color", timeColorValues[timeColorPos]);
        }

        editor.putInt("prayer_text_size_index", spinnerPrayerTextSize.getSelectedItemPosition());

        editor.putBoolean("prayer_show_clock", switchShowClock.isChecked());
        editor.putBoolean("prayer_show_shuruq", switchShowShuruq.isChecked());
        editor.putBoolean("prayer_show_remaining", switchShowRemaining.isChecked());
        editor.putBoolean("prayer_show_times", switchShowTimes.isChecked());
        editor.putBoolean("prayer_show_next_only", switchShowNextOnly.isChecked());
        editor.putBoolean("prayer_show_remaining_only", switchShowRemainingOnly.isChecked());
        int langPos = spinnerWidgetLang.getSelectedItemPosition();
        String wLang = (langPos == 1) ? "en" : "ar";
        editor.putString("prayer_widget_lang", wLang);

        editor.putInt("prayer_calc_method", spinnerCalcMethod.getSelectedItemPosition());
        editor.putInt("prayer_asr_juristic", spinnerAsrJuristic.getSelectedItemPosition());

        editor.commit();
        triggerLocalWidgetUpdate(); // Update UI immediately
        updateWidgetNow(); // Refresh data in background
        Toast.makeText(this, "تم الحفظ! ✅ Settings Saved", Toast.LENGTH_SHORT).show();
    }

    private void triggerLocalWidgetUpdate() {
        Intent intent = new Intent(this, PrayerWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), PrayerWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void checkPermissionAndSyncLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) { // Permission
                                                                                                                             // hack:
            // using visibility
            // for status
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION }, 100);
            }
        } else {
            syncLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            syncLocation();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncLocation() {
        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();
        LocationService.checkLocationAndUpdate(this, new LocationService.LocationResultCallback() {
            @Override
            public void onLocationUpdated(double lat, double lon, String city, boolean cacheInvalidated,
                                          String previousCity, float distanceKm) {
                runOnUiThread(() -> {
                    if (cacheInvalidated) {
                        String cityChangeMsg = "Location updated: ";
                        if (previousCity != null && !previousCity.isEmpty() && !previousCity.equals(city)) {
                            cityChangeMsg += previousCity + " → " + city;
                        } else {
                            cityChangeMsg += city;
                        }
                        cityChangeMsg += " — prayer times recalculated (" + String.format("%.1f", distanceKm) + " km) ✅";
                        Toast.makeText(PrayerSettingsActivity.this,
                                cityChangeMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PrayerSettingsActivity.this,
                                "Location unchanged: " + city, Toast.LENGTH_SHORT).show();
                    }
                    loadPreferences(); // refresh timestamps display
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> Toast
                        .makeText(PrayerSettingsActivity.this, "Location Error: " + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateWidgetNow() {
        PrayerUtils.fetchPrayerTimes(this, new PrayerUtils.PrayerCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    String json = data.toString();
                    String city = CacheManager.getCacheCity(PrayerSettingsActivity.this);
                    float lat = (float) CacheManager.getCacheLat(PrayerSettingsActivity.this);
                    float lon = (float) CacheManager.getCacheLon(PrayerSettingsActivity.this);
                    CacheManager.savePrayerCache(PrayerSettingsActivity.this, city, lat, lon, json);

                    Intent intent = new Intent(PrayerSettingsActivity.this, PrayerWidgetProvider.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    int[] ids = AppWidgetManager.getInstance(getApplication())
                            .getAppWidgetIds(new ComponentName(getApplication(), PrayerWidgetProvider.class));
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    sendBroadcast(intent);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(
                        () -> Toast.makeText(PrayerSettingsActivity.this, "فشل التحديث! ❌", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
