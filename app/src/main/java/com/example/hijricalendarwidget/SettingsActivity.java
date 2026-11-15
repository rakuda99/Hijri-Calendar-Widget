package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_settings_hub);

                // Settings Navigation
                findViewById(R.id.btn_goto_clock_new).setOnClickListener(v -> {
                        SettingsActivity.this
                                        .startActivity(new Intent(SettingsActivity.this, ClockSettingsActivity.class));
                });
                findViewById(R.id.btn_goto_calendar_new).setOnClickListener(v -> {
                        SettingsActivity.this.startActivity(
                                        new Intent(SettingsActivity.this, CalendarSettingsActivity.class));
                });
                findViewById(R.id.btn_goto_prayer_new).setOnClickListener(v -> {
                        SettingsActivity.this
                                        .startActivity(new Intent(SettingsActivity.this, PrayerSettingsActivity.class));
                });
                findViewById(R.id.btn_goto_backup_new).setOnClickListener(v -> {
                        SettingsActivity.this
                                        .startActivity(new Intent(SettingsActivity.this, BackupSettingsActivity.class));
                });
                findViewById(R.id.btn_goto_help_new).setOnClickListener(v -> {
                        SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, HelpActivity.class));
                });

                // Pinning Widgets
                findViewById(R.id.btn_pin_clock_fancy).setOnClickListener(v -> pinWidget(HijriCalendarWidget.class));
                findViewById(R.id.btn_pin_calendar_fancy).setOnClickListener(v -> pinWidget(HijriMonthWidget.class));
                findViewById(R.id.btn_pin_prayer_fancy).setOnClickListener(v -> pinWidget(PrayerWidgetProvider.class));

                // Language Selection Logic
                Button btnAr = findViewById(R.id.btn_lang_ar);
                Button btnEn = findViewById(R.id.btn_lang_en);

                btnAr.setOnClickListener(v -> updateLanguage("ar"));
                btnEn.setOnClickListener(v -> updateLanguage("en"));

        // Initialize state with a slight delay to ensure UI is ready
        findViewById(android.R.id.content).postDelayed(() -> {
            try {
                String savedLang = getSharedPreferences("HijriWidgetPrefs", MODE_PRIVATE)
                                .getString("app_language", "ar");
                applyLanguageUI(savedLang);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 400);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePinIcons();
    }

    private void updatePinIcons() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        
        // Clock Widget
        int[] clockIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, HijriCalendarWidget.class));
        android.widget.ImageView imgClock = findViewById(R.id.img_btn_clock);
        if (clockIds != null && clockIds.length > 0) {
            imgClock.setImageResource(R.drawable.ic_check);
        } else {
            imgClock.setImageResource(android.R.drawable.ic_menu_add);
        }

        // Calendar Widget
        int[] calIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, HijriMonthWidget.class));
        android.widget.ImageView imgCal = findViewById(R.id.img_btn_calendar);
        if (calIds != null && calIds.length > 0) {
            imgCal.setImageResource(R.drawable.ic_check);
        } else {
            imgCal.setImageResource(android.R.drawable.ic_menu_add);
        }

        // Prayer Widget
        int[] prayerIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, PrayerWidgetProvider.class));
        android.widget.ImageView imgPrayer = findViewById(R.id.img_btn_prayer);
        if (prayerIds != null && prayerIds.length > 0) {
            imgPrayer.setImageResource(R.drawable.ic_check);
        } else {
            imgPrayer.setImageResource(android.R.drawable.ic_menu_add);
        }
    }


        private void updateLanguage(String lang) {
                applyLanguageUI(lang);
                triggerAllWidgetsUpdate(lang);
        }

        private void applyLanguageUI(String lang) {
                Button ar = findViewById(R.id.btn_lang_ar);
                Button en = findViewById(R.id.btn_lang_en);

                // Visual update for buttons
                int activeColor = android.graphics.Color.parseColor("#8ACCC9");
                int inactiveColor = android.graphics.Color.parseColor("#2E2E2E");

                ar.setBackgroundTintList(android.content.res.ColorStateList
                                .valueOf(lang.equals("ar") ? activeColor : inactiveColor));
                en.setBackgroundTintList(android.content.res.ColorStateList
                                .valueOf(lang.equals("en") ? activeColor : inactiveColor));

                ar.setTextColor(lang.equals("ar") ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                en.setTextColor(lang.equals("en") ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);

                // Global labels
                String addLbl, clockLbl, calLbl, prayerLbl, setLbl, clockSet, calSet, prayerSet, backupSet, helpLbl;

                switch (lang) {
                        case "en":
                                addLbl = "ADD TO HOME SCREEN";
                                clockLbl = "Clock";
                                calLbl = "Calendar";
                                prayerLbl = "Prayers";
                                setLbl = "SETTINGS";
                                clockSet = "Clock Settings";
                                calSet = "Calendar Settings";
                                prayerSet = "Prayer Settings";
                                backupSet = "Backup & Tools";
                                helpLbl = "Help";
                                break;
                        default: // ar
                                addLbl = "إضافة للشاشة";
                                clockLbl = "ساعة";
                                calLbl = "تقويم";
                                prayerLbl = "صلاة";
                                setLbl = "الإعدادات";
                                clockSet = "إعدادات الساعة";
                                calSet = "إعدادات التقويم";
                                prayerSet = "إعدادات الصلاة";
                                backupSet = "النسخ والاحتياط";
                                helpLbl = "المساعدة";
                                break;
                }

                // Update UI Views (Using existing IDs but changing text)
                TextView tvAddEn = findViewById(R.id.section_add_en);
                TextView tvAddAr = findViewById(R.id.section_add_ar);
                if (tvAddEn != null) {
                        tvAddEn.setText(addLbl);
                        tvAddEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                        if (!lang.equals("ar"))
                                tvAddEn.setGravity(android.view.Gravity.LEFT);
                }
                if (tvAddAr != null) {
                        tvAddAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);
                }

                // Clock
                View vClockEn = findViewById(R.id.txt_add_clock_en);
                if (vClockEn instanceof TextView)
                        ((TextView) vClockEn).setText(clockLbl);
                if (vClockEn != null)
                        vClockEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vClockAr = findViewById(R.id.txt_add_clock_ar);
                if (vClockAr != null)
                        vClockAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                // Calendar
                View vCalEn = findViewById(R.id.txt_add_calendar_en);
                if (vCalEn instanceof TextView)
                        ((TextView) vCalEn).setText(calLbl);
                if (vCalEn != null)
                        vCalEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vCalAr = findViewById(R.id.txt_add_calendar_ar);
                if (vCalAr != null)
                        vCalAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                // Prayer
                View vPrayEn = findViewById(R.id.txt_add_prayer_en);
                if (vPrayEn instanceof TextView)
                        ((TextView) vPrayEn).setText(prayerLbl);
                if (vPrayEn != null)
                        vPrayEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vPrayAr = findViewById(R.id.txt_add_prayer_ar);
                if (vPrayAr != null)
                        vPrayAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                // Settings Section
                TextView tvSetEn = findViewById(R.id.section_settings_en);
                TextView tvSetAr = findViewById(R.id.section_settings_ar);
                if (tvSetEn != null) {
                        tvSetEn.setText(setLbl);
                        tvSetEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                }
                if (tvSetAr != null) {
                        tvSetAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);
                }

                // Settings Items
                View vSetClockEn = findViewById(R.id.txt_settings_clock_en);
                if (vSetClockEn instanceof TextView)
                        ((TextView) vSetClockEn).setText(clockSet);
                if (vSetClockEn != null)
                        vSetClockEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vSetClockAr = findViewById(R.id.txt_settings_clock_ar);
                if (vSetClockAr != null)
                        vSetClockAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                View vSetCalEn = findViewById(R.id.txt_settings_calendar_en);
                if (vSetCalEn instanceof TextView)
                        ((TextView) vSetCalEn).setText(calSet);
                if (vSetCalEn != null)
                        vSetCalEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vSetCalAr = findViewById(R.id.txt_settings_calendar_ar);
                if (vSetCalAr != null)
                        vSetCalAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                View vSetPrayEn = findViewById(R.id.txt_settings_prayer_en);
                if (vSetPrayEn instanceof TextView)
                        ((TextView) vSetPrayEn).setText(prayerSet);
                if (vSetPrayEn != null)
                        vSetPrayEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vSetPrayAr = findViewById(R.id.txt_settings_prayer_ar);
                if (vSetPrayAr != null)
                        vSetPrayAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                View vSetBackEn = findViewById(R.id.txt_settings_backup_en);
                if (vSetBackEn instanceof TextView)
                        ((TextView) vSetBackEn).setText(backupSet);
                if (vSetBackEn != null)
                        vSetBackEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vSetBackAr = findViewById(R.id.txt_settings_backup_ar);
                if (vSetBackAr != null)
                        vSetBackAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                // Help
                View vHelpEn = findViewById(R.id.txt_help_en);
                if (vHelpEn instanceof TextView)
                        ((TextView) vHelpEn).setText(helpLbl);
                if (vHelpEn != null)
                        vHelpEn.setVisibility(lang.equals("ar") ? View.GONE : View.VISIBLE);
                View vHelpAr = findViewById(R.id.txt_help_ar);
                if (vHelpAr != null)
                        vHelpAr.setVisibility(lang.equals("ar") ? View.VISIBLE : View.GONE);

                // Save to SharedPreferences centrally
                SharedPreferences.Editor editor = getSharedPreferences("HijriWidgetPrefs", MODE_PRIVATE).edit();
                editor.putString("app_language", lang);
                editor.putString("prayer_widget_lang", lang); // Sync prayer widget
                editor.putString("language", lang); // Sync main calendar logic
                editor.apply();
        }

        private void triggerAllWidgetsUpdate(String lang) {
                PrayerUtils.fetchPrayerTimes(this, new PrayerUtils.PrayerCallback() {
                        @Override
                        public void onSuccess(org.json.JSONObject data) {
                                CacheManager.savePrayerCache(SettingsActivity.this,
                                        CacheManager.getCacheCity(SettingsActivity.this),
                                        CacheManager.getCacheLat(SettingsActivity.this),
                                        CacheManager.getCacheLon(SettingsActivity.this),
                                        data.toString());

                                Intent prayerIntent = new Intent(SettingsActivity.this, PrayerWidgetProvider.class);
                                prayerIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                int[] prayerIds = AppWidgetManager.getInstance(SettingsActivity.this)
                                                .getAppWidgetIds(new ComponentName(SettingsActivity.this,
                                                                PrayerWidgetProvider.class));
                                prayerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds);
                                sendBroadcast(prayerIntent);
                        }

                        @Override
                        public void onError(Exception e) {
                                // Fallback refresh
                                Intent prayerIntent = new Intent(SettingsActivity.this, PrayerWidgetProvider.class);
                                prayerIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                int[] prayerIds = AppWidgetManager.getInstance(SettingsActivity.this)
                                                .getAppWidgetIds(new ComponentName(SettingsActivity.this,
                                                                PrayerWidgetProvider.class));
                                prayerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds);
                                sendBroadcast(prayerIntent);
                        }
                });

                // Trigger update for Main Clock/Date Widget
                Intent calendarIntent = new Intent(this, HijriCalendarWidget.class);
                calendarIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] calIds = AppWidgetManager.getInstance(this)
                                .getAppWidgetIds(new ComponentName(this, HijriCalendarWidget.class));
                calendarIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, calIds);
                sendBroadcast(calendarIntent);

                // Trigger update for Monthly Calendar Widget
                Intent monthIntent = new Intent(this, HijriMonthWidget.class);
                monthIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] monthIds = AppWidgetManager.getInstance(this)
                                .getAppWidgetIds(new ComponentName(this, HijriMonthWidget.class));
                monthIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, monthIds);
                sendBroadcast(monthIntent);

                Toast.makeText(this, "Language set to: " + lang, Toast.LENGTH_SHORT).show();
        }

        private void pinWidget(Class<?> providerClass) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.appwidget.AppWidgetManager appWidgetManager = getSystemService(
                                        android.appwidget.AppWidgetManager.class);
                        android.content.ComponentName myProvider = new android.content.ComponentName(this,
                                        providerClass);
                        if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null);
                        } else {
                                android.widget.Toast.makeText(this, "Pinning not supported on this launcher",
                                                android.widget.Toast.LENGTH_SHORT).show();
                        }
                } else {
                        android.widget.Toast.makeText(this, "Android 8.0+ required for this feature",
                                        android.widget.Toast.LENGTH_SHORT).show();
                }
        }
}