package com.example.hijricalendarwidget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

        private TextView monthYearText;
        private ImageView btnPrev, btnNext;
        private ImageButton btnSettings;
        private android.widget.Button btnClockSet, btnCalendarSet, btnPrayerSet;
        private GridView calendarGrid;
        private static final String APP_VERSION = "v5.0 Build 50";
        private CalendarAdapter adapter;
        private int currentMonthOffset = 0;

        private TextView[] dayHeaders;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);

                monthYearText = findViewById(R.id.month_year_text);
                btnPrev = findViewById(R.id.btn_prev_month);
                btnNext = findViewById(R.id.btn_next_month);
                btnSettings = findViewById(R.id.btn_settings);
                calendarGrid = findViewById(R.id.calendar_grid);

                int[] dayHeaderIds = { R.id.day_name_1, R.id.day_name_2, R.id.day_name_3, R.id.day_name_4,
                                R.id.day_name_5, R.id.day_name_6, R.id.day_name_7 };
                dayHeaders = new TextView[7];
                for (int i = 0; i < 7; i++) {
                        dayHeaders[i] = findViewById(dayHeaderIds[i]);
                }

                adapter = new CalendarAdapter(this);
                calendarGrid.setAdapter(adapter);

                // Ensure periodic sync is active
                HijriDateUtils.schedulePeriodicSync(this);

                calendarGrid.setOnItemClickListener((parent, view, position, id) -> {
                        CalendarAdapter.CalendarDay day = (CalendarAdapter.CalendarDay) adapter.getItem(position);
                        if (day != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.set(day.year, day.month, day.day, 0, 0);
                                long startMillis = cal.getTimeInMillis();

                                android.net.Uri.Builder builder = android.provider.CalendarContract.CONTENT_URI
                                                .buildUpon();
                                builder.appendPath("time");
                                android.content.ContentUris.appendId(builder, startMillis);
                                Intent calendarIntent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
                                startActivity(calendarIntent);
                        }
                });

                btnPrev.setOnClickListener(v -> {
                        currentMonthOffset--;
                        updateCalendar();
                });

                btnNext.setOnClickListener(v -> {
                        currentMonthOffset++;
                        updateCalendar();
                });

                btnSettings.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                });

                btnClockSet = findViewById(R.id.btn_clock_settings_main);
                btnCalendarSet = findViewById(R.id.btn_calendar_settings_main);
                btnPrayerSet = findViewById(R.id.btn_prayer_settings_main);

                btnClockSet.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, ClockSettingsActivity.class);
                        MainActivity.this.startActivity(intent);
                });

                btnCalendarSet.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, CalendarSettingsActivity.class);
                        MainActivity.this.startActivity(intent);
                });

                btnPrayerSet.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, PrayerSettingsActivity.class);
                        MainActivity.this.startActivity(intent);
                });

                // Initial setup - Delay for performance
                findViewById(android.R.id.content).postDelayed(() -> {
                        updateCalendar();
                }, 300);
        }

        @Override
        protected void onResume() {
                super.onResume();
                // Return from settings may change perfs
                adapter.updatePreferences();
                updateCalendar(); // Updates headers and month format
        }

        private void updateCalendar() {
                adapter.setMonthOffset(currentMonthOffset);

                // Update Header Text
                long adjustedTime = HijriDateUtils.getAdjustedTime(this);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(adjustedTime);
                cal.add(Calendar.MONTH, currentMonthOffset);

                SharedPreferences prefs = getSharedPreferences("HijriWidgetPrefs", MODE_PRIVATE);

                // Language Handling
                String lang = prefs.getString("app_language", "ar");
                boolean isArabic = lang.equals("ar")
                                || (lang.equals("auto") && Locale.getDefault().getLanguage().equals("ar"));
                Locale locale = isArabic ? new Locale("ar") : Locale.ENGLISH;

                // Month Format Handling
                int formatIndex = prefs.getInt("month_format_index", 0);
                String pattern = "MMMM yyyy";
                switch (formatIndex) {
                        case 1:
                                pattern = "MM - yyyy";
                                break;
                        case 2:
                                pattern = "d MMMM yyyy";
                                break;
                        case 3:
                                pattern = "d/MM/yyyy";
                                break;
                        default:
                                pattern = "MMMM yyyy";
                                break;
                }

                SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
                int digitStyle = prefs.getInt("digit_style", 0);
                monthYearText.setText(HijriDateUtils.localizeDigits(sdf.format(cal.getTime()), digitStyle));

                // Set Version
                TextView tvVersion = findViewById(R.id.tv_version_main);
                if (tvVersion != null)
                        tvVersion.setText(APP_VERSION);

                // Update Day Headers
                int weekStart = prefs.getInt("week_start", 0); // 0 = Sunday, 1 = Monday
                String[] engNames;
                String[] arNames;

                if (weekStart == 1) { // Monday Start
                        engNames = new String[] { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
                        arNames = new String[] { "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد" };
                } else if (weekStart == 2) { // Saturday Start
                        engNames = new String[] { "SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI" };
                        arNames = new String[] { "سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة" };
                } else { // Sunday Start (Default)
                        engNames = new String[] { "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT" };
                        arNames = new String[] { "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت" };
                }

                // Day Language Override
                int dayLangIndex = prefs.getInt("month_day_language_index", 0);
                if (dayLangIndex == 1)
                        isArabic = true;
                else if (dayLangIndex == 2)
                        isArabic = false;

                for (int i = 0; i < 7; i++) {
                        dayHeaders[i].setText(isArabic ? arNames[i] : engNames[i]);
                }
        }
}