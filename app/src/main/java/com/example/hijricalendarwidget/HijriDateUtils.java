package com.example.hijricalendarwidget;

import android.content.Context;
import android.os.Build;
import java.util.Calendar;
import java.util.Locale;

public class HijriDateUtils {

    private static final android.util.LruCache<String, int[]> hijriCache = new android.util.LruCache<>(100);

    public static final String[] HIJRI_MONTH_NAMES_AR = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    public static final String[] HIJRI_MONTH_NAMES_EN = {
            "Muharram", "Safar", "Rabi I", "Rabi II",
            "Jumada I", "Jumada II", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu'l-Qi'dah", "Dhu'l-Hijjah"
    };

    public static final String[] HIJRI_MONTH_NAMES_ES = {
            "Muharram", "Safar", "Rabi I", "Rabi II",
            "Jumada I", "Jumada II", "Rajab", "Sha'ban",
            "Ramadán", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    };

    public static final String[] HIJRI_MONTH_NAMES_FR = {
            "Mouharram", "Safar", "Rabi I", "Rabi II",
            "Joumada I", "Joumada II", "Rajab", "Cha'bane",
            "Ramadan", "Chawwal", "Dhou al-qi'da", "Dhou al-hijja"
    };

    public static final String[] HIJRI_MONTH_NAMES_JA = {
            "ムハッラム", "サファル", "ラビー I", "ラビー II",
            "ジュマーダ I", "ジュマーダ II", "ラジャブ", "シャアバーン",
            "ラマダーン", "シャウワール", "ズ・アル・キアダ", "ズ・アル・ヒッジャ"
    };

    public static final String[] HIJRI_MONTH_NAMES_ZH = {
            "穆哈兰姆月", "色法尔月", "赖比尔 I", "赖比尔 II",
            "朱马达 I", "朱马达 II", "赖哲卜月", "舍尔邦月",
            "赖买丹月", "闪瓦鲁月", "笃尔·卡尔德", "笃尔·希嘉"
    };

    /**
     * Localizes digits based on user preference.
     * 
     * @param input String with Western digits (0-9)
     * @param style 0 = Western (0-9), 1 = Eastern/Hindi (٠-٩)
     */
    public static String localizeDigits(String input, int style) {
        if (input == null)
            return null;
        char[] hindiDigits = { '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩' };
        char[] persianDigits = { '۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹' };
        char[] westernDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(style == 1 ? hindiDigits[c - '0'] : c);
            } else if (c >= '٠' && c <= '٩') {
                sb.append(style == 0 ? westernDigits[c - '٠'] : c);
            } else if (c >= '۰' && c <= '۹') {
                sb.append(style == 0 ? westernDigits[c - '۰'] : (style == 1 ? hindiDigits[c - '۰'] : c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void syncTimeWithNetwork(android.content.Context context) {
        syncTimeWithNetwork(context, false);
    }

    public static void syncTimeWithNetwork(android.content.Context context, boolean force) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                android.content.Context.MODE_PRIVATE);
        long lastSync = prefs.getLong("last_network_sync", 0);
        long currentTime = System.currentTimeMillis();

        // ── PRIORITY RULE ──────────────────────────────────────────────────────────
        // The 12-hour guard applies ONLY to weather/prayer API network calls.
        // Location accuracy ALWAYS has higher priority than cache lifetime.
        //
        // When the device moves > 5 km, LocationUtils.invalidateLocationAndPrayerCache()
        // resets last_network_sync to 0, so the guard is automatically bypassed on the
        // next call without needing an explicit force=true here.
        // ──────────────────────────────────────────────────────────────────────────
        boolean skipNetwork = !force && (currentTime - lastSync < 43200000L); // 12 h

        // Always refresh widget date immediately from the local clock — no network required.
        refreshAllWidgetsNow(context);

        if (skipNetwork) {
            schedulePeriodicSync(context);
            return;
        }

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://www.google.com");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.connect();
                String dateStr = connection.getHeaderField("Date");
                connection.disconnect();

                if (dateStr != null) {
                    boolean hasLocation =
                        androidx.core.content.ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED
                        || androidx.core.content.ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED;

                    if (hasLocation) {
                        // Location fetch performs its own movement check and cache invalidation
                        trySyncLocationAndPrayers(context, prefs);
                    } else {
                        // No GPS permission — refresh prayer times from last saved location
                        PrayerUtils.fetchPrayerTimes(context, new PrayerUtils.PrayerCallback() {
                            @Override
                            public void onSuccess(org.json.JSONObject data) {
                                String json = data.toString();
                                float lat = (float) CacheManager.getCacheLat(context);
                                float lon = (float) CacheManager.getCacheLon(context);
                                String city = CacheManager.getCacheCity(context);
                                CacheManager.savePrayerCache(context, city, lat, lon, json);
                                notifyPrayerWidget(context);
                            }

                            @Override
                            public void onError(Exception e) { }
                        });
                        WeatherUtils.triggerWeatherFetch(context);
                    }
                    prefs.edit().putLong("last_network_sync", System.currentTimeMillis()).apply();
                }

                // Widget date update: always done regardless of network success —
                // moved above this thread so it fires instantly from the local clock.
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Always ensure periodic sync + midnight alarm are scheduled
                schedulePeriodicSync(context);
            }
        }).start();
    }

    public static void schedulePeriodicSync(android.content.Context context) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context
                .getSystemService(android.content.Context.ALARM_SERVICE);
        android.content.Intent intent = new android.content.Intent(context, SyncReceiver.class);
        intent.setAction("com.example.hijricalendarwidget.ACTION_PERIODIC_SYNC");

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 1001, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        // Schedule every 12 hours (Inexact is fine and battery friendly)
        long interval = 43200000L; // 12 hours
        long triggerAt = System.currentTimeMillis() + interval;

        alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerAt,
                interval,
                pendingIntent);

        // Also schedule the midnight alarm to guarantee date refresh at day change
        scheduleMidnightAlarm(context);
    }

    /**
     * Schedules an exact wakeup alarm 5 seconds after the next local midnight.
     * This is the most reliable mechanism to keep a date widget current.
     * Called from schedulePeriodicSync(), onEnabled(), and after TIMEZONE_CHANGED.
     */
    public static void scheduleMidnightAlarm(android.content.Context context) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context
                .getSystemService(android.content.Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        android.content.Intent intent = new android.content.Intent(context, SyncReceiver.class);
        intent.setAction("com.example.hijricalendarwidget.ACTION_MIDNIGHT_UPDATE");

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 2001, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        // Calculate next midnight + 5 seconds to allow system to stabilise
        java.util.Calendar midnight = java.util.Calendar.getInstance();
        midnight.add(java.util.Calendar.DAY_OF_MONTH, 1);
        midnight.set(java.util.Calendar.HOUR_OF_DAY, 0);
        midnight.set(java.util.Calendar.MINUTE, 0);
        midnight.set(java.util.Calendar.SECOND, 5);
        midnight.set(java.util.Calendar.MILLISECOND, 0);
        long triggerAtMidnight = midnight.getTimeInMillis();

        // setExactAndAllowWhileIdle ensures the alarm fires even in Doze mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP, triggerAtMidnight, pendingIntent);
        } else {
            alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP, triggerAtMidnight, pendingIntent);
        }
    }

    /**
     * Immediately refreshes all widget instances using the current local time.
     * Does NOT require network access.
     */
    public static void refreshAllWidgetsNow(android.content.Context context) {
        try {
            android.appwidget.AppWidgetManager mgr = android.appwidget.AppWidgetManager.getInstance(context);

            int[] ids = mgr.getAppWidgetIds(
                    new android.content.ComponentName(context, HijriCalendarWidget.class));
            if (ids != null && ids.length > 0) {
                android.content.Intent updateIntent = new android.content.Intent(context,
                        HijriCalendarWidget.class);
                updateIntent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(updateIntent);
            }

            int[] monthIds = mgr.getAppWidgetIds(
                    new android.content.ComponentName(context, HijriMonthWidget.class));
            if (monthIds != null && monthIds.length > 0) {
                android.content.Intent mIntent = new android.content.Intent(context, HijriMonthWidget.class);
                mIntent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                mIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, monthIds);
                context.sendBroadcast(mIntent);
            }

            int[] prayerIds = mgr.getAppWidgetIds(
                    new android.content.ComponentName(context, PrayerWidgetProvider.class));
            if (prayerIds != null && prayerIds.length > 0) {
                android.content.Intent pIntent = new android.content.Intent(context,
                        PrayerWidgetProvider.class);
                pIntent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                pIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds);
                context.sendBroadcast(pIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience wrapper — delegates to {@link LocationUtils#invalidateLocationAndPrayerCache}.
     * Resets last_lat, last_lon, last_network_sync, prayer_times_json, and weather cache
     * so the next {@link #syncTimeWithNetwork} call bypasses the 12-hour guard automatically.
     */
    public static void invalidateLocationAndPrayerCache(android.content.Context context) {
        LocationUtils.invalidateLocationAndPrayerCache(context);
    }

    public static long getAdjustedTime(android.content.Context context) {
        return System.currentTimeMillis();
    }

    public static int[] getCurrentHijriDate(android.content.Context context, int userOffsetDays) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                android.content.Context.MODE_PRIVATE);
        int hijriAdjustment = prefs.getInt("hijri_correction", 0);
        int combinedOffset = userOffsetDays + hijriAdjustment;

        long time = getAdjustedTime(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return HijriApi26Helper.getHijriDate(time, combinedOffset);
            } catch (Throwable e) {
                return getCalculatedHijriDate(time, combinedOffset);
            }
        }
        return getCalculatedHijriDate(time, combinedOffset);
    }

    public static int[] getHijriDateFromGregorian(Context context, int year, int month, int day) {
        String key = year + "-" + month + "-" + day;
        int[] cached = hijriCache.get(key);
        if (cached != null)
            return cached;

        android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                android.content.Context.MODE_PRIVATE);
        int hijriAdjustment = prefs.getInt("hijri_correction", 0);
        int[] result = getHijriDateWithAdjustment(year, month, day, hijriAdjustment);
        hijriCache.put(key, result);
        return result;
    }

    public static int[] getHijriDateWithAdjustment(int year, int month, int day, int hijriAdjustment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Calendar c = Calendar.getInstance();
                c.set(year, month - 1, day);
                return HijriApi26Helper.getHijriDate(c.getTimeInMillis(), hijriAdjustment);
            } catch (Throwable e) {
                return applyOffset(gregorianToHijri(year, month, day), hijriAdjustment);
            }
        }
        return applyOffset(gregorianToHijri(year, month, day), hijriAdjustment);
    }

    private static int[] applyOffset(int[] hDate, int offset) {
        if (offset == 0)
            return hDate;
        // Simple day offset for Hijri (doesn't handle month wrap perfectly but enough
        // for +-2 days)
        hDate[2] += offset;
        if (hDate[2] > 30) {
            hDate[2] -= 30;
            hDate[1]++;
        }
        if (hDate[2] < 1) {
            hDate[2] += 29;
            hDate[1]--;
        }
        if (hDate[1] > 12) {
            hDate[1] = 1;
            hDate[0]++;
        }
        if (hDate[1] < 1) {
            hDate[1] = 12;
            hDate[0]--;
        }
        return hDate;
    }

    private static int[] getCalculatedHijriDate(long time, int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.DAY_OF_MONTH, offsetDays);
        return gregorianToHijri(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static int[] gregorianToHijri(int year, int month, int day) {
        if (month < 3) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        double jd = Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;

        jd = Math.floor(jd) + 0.5;
        double z = jd - 1948440.0;
        double cyc = Math.floor(z / 10631.0);
        z = z - 10631.0 * cyc;
        double j = Math.floor((z - 0.133) / 354.367);
        double r = z - Math.floor(j * 354.367 + 0.85);
        int yearH = (int) (30.0 * cyc + j + 1);
        int monthH = (int) (Math.floor((r + 28.5) / 29.5) + 1);
        int dayH = (int) (r - Math.floor((monthH - 1) * 29.5 + 0.5) + 30);

        if (monthH > 12) {
            monthH -= 12;
            yearH++;
        }
        return new int[] { yearH, monthH, dayH };
    }

    public static String formatHijriDate(int[] hijriDate, String language, int monthFormatIdx, boolean padDates) {
        if (hijriDate == null || hijriDate.length < 3)
            return "";
        int year = hijriDate[0], month = hijriDate[1], day = hijriDate[2];
        if (month < 1 || month > 12)
            return "";

        String dayStr = padDates ? String.format(Locale.US, "%02d", day) : String.valueOf(day);
        String monthStr = padDates ? String.format(Locale.US, "%02d", month) : String.valueOf(month);

        if (monthFormatIdx == 1 || monthFormatIdx == 3)
            return dayStr + " / " + monthStr + " / " + year;

        String lang = (language == null || language.equals("auto")) ? Locale.getDefault().getLanguage() : language;

        String[] monthNames;
        switch (lang) {
            case "ar":
                monthNames = HIJRI_MONTH_NAMES_AR;
                break;
            case "es":
                monthNames = HIJRI_MONTH_NAMES_ES;
                break;
            case "fr":
                monthNames = HIJRI_MONTH_NAMES_FR;
                break;
            case "ja":
                monthNames = HIJRI_MONTH_NAMES_JA;
                break;
            case "zh":
                monthNames = HIJRI_MONTH_NAMES_ZH;
                break;
            default:
                monthNames = HIJRI_MONTH_NAMES_EN;
                break;
        }

        if (lang.equals("ar"))
            return dayStr + " " + monthNames[month - 1] + " " + year;
        if (lang.equals("ja"))
            return dayStr + "日 " + monthNames[month - 1] + " " + year;
        return monthNames[month - 1] + " " + dayStr + ", " + year;
    }

    private static void trySyncLocationAndPrayers(android.content.Context context,
            android.content.SharedPreferences prefs) {
        LocationUtils.getCurrentLocation(context, new LocationUtils.LocationCallback() {
            @Override
            public void onLocationFound(double lat, double lon) {
                CacheManager.saveLastKnownLocation(context, lat, lon);
                WeatherUtils.fetchWeather(context, lat, lon);
                PrayerUtils.fetchPrayerTimes(context, lat, lon, new PrayerUtils.PrayerCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject data) {
                        String json = data.toString();
                        String city = CacheManager.getCacheCity(context);
                        PrayerUtils.savePrayerCache(context, city, lat, lon, json);
                        notifyPrayerWidget(context);
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
            }

            @Override
            public void onError(String msg) {
                // Fallback refresh using last known data
                notifyPrayerWidget(context);
            }
        });
    }

    private static void notifyPrayerWidget(android.content.Context context) {
        android.appwidget.AppWidgetManager appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context);
        int[] prayerIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, PrayerWidgetProvider.class));
        if (prayerIds.length > 0) {
            android.content.Intent pIntent = new android.content.Intent(context, PrayerWidgetProvider.class);
            pIntent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            pIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds);
            context.sendBroadcast(pIntent);
        }
    }

    // Helper to avoid class verification issues on old devices
    private static class HijriApi26Helper {
        static int[] getHijriDate(long time, int offsetDays) {
            java.time.chrono.HijrahDate hd = java.time.chrono.HijrahDate.from(
                    java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(time),
                            java.time.ZoneId.systemDefault()))
                    .plus(offsetDays, java.time.temporal.ChronoUnit.DAYS);
            return new int[] {
                    hd.get(java.time.temporal.ChronoField.YEAR),
                    hd.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                    hd.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            };
        }
    }
}