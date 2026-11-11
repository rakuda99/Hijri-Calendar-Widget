package com.example.hijricalendarwidget;

import android.content.Context;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PrayerUtils {

    /** Adhan highlight window: starts 5 min before the prayer, ends 20 min after it. */
    private static final long ADHAN_PRE_WINDOW_MS = 5L * 60L * 1000L;
    private static final long ADHAN_POST_WINDOW_MS = 20L * 60L * 1000L;

    /** 0 = Western digits (0-9), 1 = Arabic-Indic digits, same as "digit_style". */
    public static final int DEFAULT_DIGIT_STYLE = 0;

    public interface PrayerCallback {
        void onSuccess(JSONObject data);

        void onError(Exception e);
    }

    public static void fetchPrayerTimes(Context context, double lat, double lon, PrayerCallback callback) {
        new Thread(() -> {
            try {
                // http://api.aladhan.com/v1/timings?latitude=24.7136&longitude=46.6753&method=4
                android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
                int method = prefs.getInt("prayer_calc_method", 0);
                // Map local PrayTime indices to Aladhan API method IDs if possible, or just use the local one
                // For now, let's stick to the URL format but we could inject method here
                int apiMethod = (method == 0) ? 4 : (method == 1 ? 3 : (method == 2 ? 2 : (method == 3 ? 5 : (method == 4 ? 1 : (method == 5 ? 7 : (method == 6 ? 0 : 4))))));
                
                String urlStr = String.format(Locale.US,
                        "https://api.aladhan.com/v1/timings?latitude=%f&longitude=%f&method=%d", lat, lon, apiMethod);
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line);
                    reader.close();

                    JSONObject response = new JSONObject(sb.toString());
                    JSONObject data = response.getJSONObject("data");
                    JSONObject timings = data.getJSONObject("timings");

                    JSONObject processed = processTimings(context, timings, false);
                    callback.onSuccess(processed);
                } else {
                    // Fallback to offline calculation if API fails
                    JSONObject offlineTimings = calculateOfflineTimings(context, lat, lon);
                    JSONObject processed = processTimings(context, offlineTimings, false);
                    callback.onSuccess(processed);
                }
                conn.disconnect();
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public static void fetchPrayerTimes(Context context, PrayerCallback callback) {
        // Overload to use last known location or fallback
        android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                Context.MODE_PRIVATE);
        float lat = prefs.getFloat("last_lat", 0);
        float lon = prefs.getFloat("last_lon", 0);

        if (lat != 0 && lon != 0) {
            fetchPrayerTimes(context, lat, lon, callback);
        } else {
            // Fallback to offline with default/Riyadh coordinates if none saved
            try {
                JSONObject offline = calculateOfflineTimings(context, 24.7136, 46.6753);
                JSONObject processed = processTimings(context, offline, false);
                callback.onSuccess(processed);
            } catch (Exception e) {
                callback.onError(e);
            }
        }
    }

    public static JSONObject calculateOfflineTimings(Context context, double lat, double lon) throws Exception {
        android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
        int methodIdx = prefs.getInt("prayer_calc_method", 0);
        int asrIdx = prefs.getInt("prayer_asr_juristic", 0);

        PrayTime prayers = new PrayTime();
        prayers.setTimeFormat(prayers.Time24);
        
        // Map UI index to PrayTime constants
        int[] methods = {
            prayers.Makkah, 
            prayers.MWL, 
            prayers.ISNA, 
            prayers.Egypt, 
            prayers.Karachi, 
            prayers.Tehran, 
            prayers.Jafari
        };
        
        prayers.setCalcMethod(methods[methodIdx]);
        prayers.setAsrJuristic(asrIdx == 1 ? prayers.Hanafi : prayers.Shafii);
        prayers.setAdjustHighLats(prayers.MidNight);

        Calendar cal = Calendar.getInstance();
        double timezone = TimeZone.getDefault().getOffset(cal.getTimeInMillis()) / (1000.0 * 3600.0);

        java.util.ArrayList<String> prayerTimes = prayers.getPrayerTimes(cal, lat, lon, timezone);
        
        // PrayTime returns: Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha
        JSONObject timings = new JSONObject();
        timings.put("Fajr", prayerTimes.get(0));
        timings.put("Sunrise", prayerTimes.get(1));
        timings.put("Dhuhr", prayerTimes.get(2));
        timings.put("Asr", prayerTimes.get(3));
        timings.put("Maghrib", prayerTimes.get(5));
        timings.put("Isha", prayerTimes.get(6));
        
        return timings;
    }

    public static JSONObject getFallbackTimings() throws Exception {
        JSONObject timings = new JSONObject();
        timings.put("Fajr", "04:53");
        timings.put("Sunrise", "06:12");
        timings.put("Dhuhr", "12:04");
        timings.put("Asr", "15:29");
        timings.put("Maghrib", "18:04");
        timings.put("Isha", "19:34");
        return timings;
    }

    public static JSONObject processTimings(JSONObject timings) throws Exception {
        return processTimings(timings, false, DEFAULT_DIGIT_STYLE);
    }

    public static JSONObject processTimings(JSONObject timings, boolean showShuruq) throws Exception {
        return processTimings(timings, showShuruq, DEFAULT_DIGIT_STYLE);
    }

    public static JSONObject processTimings(Context context, JSONObject timings, boolean showShuruq)
            throws Exception {
        return processTimings(timings, showShuruq, getDigitStyle(context));
    }

    public static JSONObject processTimings(JSONObject timings, boolean showShuruq, int digitStyle)
            throws Exception {
        // Single precise "now" reference so every diff is exact and consistent
        long nowMillis = System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        String[] keys = { "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha" };

        // Translations
        String[] arNames = { "الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء" };
        String[] enNames = { "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha" };
        String[] esNames = { "Al-Fajr", "Shuruq", "Ad-Duhr", "Al-Asr", "Al-Maghrib", "Al-Isha" };
        String[] frNames = { "Fajr", "Shuruq", "Dohr", "Asr", "Maghrib", "Icha" };
        String[] jaNames = { "晨礼", "日出", "昼礼", "晡礼", "昏礼", "宵礼" };
        String[] zhNames = { "晨礼", "日出", "晌礼", "晡礼", "昏礼", "宵礼" };

        if (now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            arNames[2] = "الجمعة";
            enNames[2] = "Friday";
            esNames[2] = "Viernes";
            frNames[2] = "Vendredi";
            jaNames[2] = "主麻";
            zhNames[2] = "主麻礼";
        }

        JSONObject timesObj = new JSONObject();
        Calendar[] prayerCals = new Calendar[keys.length];
        long[] diffs = new long[keys.length];
        boolean[] valid = new boolean[keys.length];

        for (int i = 0; i < keys.length; i++) {
            String timeStr = timings.optString(keys[i], "");
            int[] hm = parseTimeOfDay(timeStr);
            valid[i] = (hm != null);

            Calendar pCal = (Calendar) now.clone();
            pCal.set(Calendar.SECOND, 0);
            pCal.set(Calendar.MILLISECOND, 0);

            String cleanTime = valid[i] ? String.format(Locale.US, "%02d:%02d", hm[0], hm[1]) : null;
            if (valid[i]) {
                pCal.set(Calendar.HOUR_OF_DAY, hm[0]);
                pCal.set(Calendar.MINUTE, hm[1]);
            } else {
                pCal.set(Calendar.HOUR_OF_DAY, 0);
                pCal.set(Calendar.MINUTE, 0);
            }

            prayerCals[i] = pCal;
            // Exact difference: prayer time - current time
            diffs[i] = valid[i] ? (pCal.getTimeInMillis() - nowMillis) : Long.MIN_VALUE;

            JSONObject pObj = new JSONObject();
            pObj.put("name_ar", arNames[i]);
            pObj.put("name_en", enNames[i]);
            pObj.put("name_es", esNames[i]);
            pObj.put("name_fr", frNames[i]);
            pObj.put("name_ja", jaNames[i]);
            pObj.put("name_zh", zhNames[i]);

            pObj.put("time_ar", formatTime(cleanTime, "ar", digitStyle));
            pObj.put("time_en", formatTime(cleanTime, "en", digitStyle));
            pObj.put("time_es", formatTime(cleanTime, "es", digitStyle));
            pObj.put("time_fr", formatTime(cleanTime, "fr", digitStyle));
            pObj.put("time_ja", formatTime(cleanTime, "ja", digitStyle));
            pObj.put("time_zh", formatTime(cleanTime, "zh", digitStyle));
            pObj.put("raw_time", cleanTime != null ? cleanTime : "");

            pObj.put("time", formatTime(cleanTime, "ar", digitStyle));
            pObj.put("name", arNames[i]);
            timesObj.put(keys[i], pObj);
        }

        // Build a strictly chronological index of the valid prayers (by time of day)
        int[] chrono = new int[keys.length];
        int chronoCount = 0;
        for (int i = 0; i < keys.length; i++) {
            if (!valid[i])
                continue;
            int j = chronoCount - 1;
            while (j >= 0 && minutesOfDay(prayerCals[chrono[j]]) > minutesOfDay(prayerCals[i])) {
                chrono[j + 1] = chrono[j];
                j--;
            }
            chrono[j + 1] = i;
            chronoCount++;
        }

        // 1. Adhan window: from 5 min before the prayer until 20 min after it (Sunrise excluded)
        String adhanPrayerKey = null;
        int adhanPrayerIdx = -1;
        long adhanDiff = 0;

        for (int c = 0; c < chronoCount; c++) {
            int i = chrono[c];
            if (keys[i].equals("Sunrise"))
                continue;

            long diff = diffs[i]; // prayer time - now
            if (diff <= ADHAN_PRE_WINDOW_MS && diff >= -ADHAN_POST_WINDOW_MS) {
                adhanPrayerKey = keys[i];
                adhanPrayerIdx = i;
                adhanDiff = diff;
                break;
            }
        }

        // Within the adhan window of tomorrow's Fajr
        if (adhanPrayerKey == null && valid[0]) {
            Calendar tomorrowFajr = (Calendar) prayerCals[0].clone();
            tomorrowFajr.add(Calendar.DAY_OF_YEAR, 1);
            long diffTmrFajr = tomorrowFajr.getTimeInMillis() - nowMillis;
            if (diffTmrFajr <= ADHAN_PRE_WINDOW_MS && diffTmrFajr >= -ADHAN_POST_WINDOW_MS) {
                adhanPrayerKey = "Fajr";
                adhanPrayerIdx = 0;
                adhanDiff = diffTmrFajr;
            }
        }

        // 2. Next prayer = the FIRST prayer (in chronological order) still ahead of the current time
        String nextPrayerKey = null;
        long minDiff = Long.MAX_VALUE;

        for (int c = 0; c < chronoCount; c++) {
            int i = chrono[c];
            if (!showShuruq && keys[i].equals("Sunrise"))
                continue;

            long diff = diffs[i];
            if (diff > 0) {
                // chrono is sorted ascending, so the first positive diff is the nearest prayer
                minDiff = diff;
                nextPrayerKey = keys[i];
                break;
            }
        }

        // All of today's prayers have passed -> next is tomorrow's Fajr
        if (nextPrayerKey == null && valid[0]) {
            nextPrayerKey = "Fajr";
            Calendar tomorrowFajr = (Calendar) prayerCals[0].clone();
            tomorrowFajr.add(Calendar.DAY_OF_YEAR, 1);
            minDiff = tomorrowFajr.getTimeInMillis() - nowMillis;
        }

        boolean isAdhanWindow = (adhanPrayerKey != null);
        String activePrayerKey = isAdhanWindow ? adhanPrayerKey : nextPrayerKey;

        JSONObject result = new JSONObject();
        result.put("times", timesObj);
        result.put("next", nextPrayerKey);
        result.put("active_prayer", activePrayerKey);
        result.put("is_adhan_window", isAdhanWindow);

        if (isAdhanWindow) {
            if (adhanDiff > 0) {
                long m = ceilMinutes(adhanDiff);
                result.put("duration_ar", "بقي على الأذان: " + arabicMinutesPhrase(m, digitStyle));
                result.put("duration_en", "Adhan in: " + m + " min");
                result.put("duration_es", "Athan en: " + m + " min");
                result.put("duration_fr", "Adhan dans: " + m + " min");
                result.put("duration_ja", "礼拝まで: " + m + "分");
                result.put("duration_zh", "唤拜倒计时: " + m + "分钟");

                result.put("duration_only_ar", localize(String.valueOf(m), digitStyle) + " د");
                result.put("duration_only_en", m + "m");
                result.put("duration_only_es", m + "m");
                result.put("duration_only_fr", m + "m");
                result.put("duration_only_ja", m + "分");
                result.put("duration_only_zh", m + "分");
            } else {
                String pNameAr = arNames[adhanPrayerIdx];
                String pNameEn = enNames[adhanPrayerIdx];
                result.put("duration_ar", "حان الآن وقت صلاة " + pNameAr);
                result.put("duration_en", "Prayer Time: " + pNameEn);
                result.put("duration_es", "Hora de oración: " + esNames[adhanPrayerIdx]);
                result.put("duration_fr", "Heure de prière: " + frNames[adhanPrayerIdx]);
                result.put("duration_ja", "礼拝の時間です");
                result.put("duration_zh", "礼拜时间到了");

                result.put("duration_only_ar", "وقت الصلاة");
                result.put("duration_only_en", "Prayer Time");
                result.put("duration_only_es", "Oración");
                result.put("duration_only_fr", "Prière");
                result.put("duration_only_ja", "礼拝");
                result.put("duration_only_zh", "礼拜");
            }
        } else {
            result.put("duration_ar", formatDuration(minDiff, "ar", digitStyle));
            result.put("duration_en", formatDuration(minDiff, "en", digitStyle));
            result.put("duration_es", formatDuration(minDiff, "es", digitStyle));
            result.put("duration_fr", formatDuration(minDiff, "fr", digitStyle));
            result.put("duration_ja", formatDuration(minDiff, "ja", digitStyle));
            result.put("duration_zh", formatDuration(minDiff, "zh", digitStyle));

            result.put("duration_only_ar", formatDurationOnly(minDiff, "ar", digitStyle));
            result.put("duration_only_en", formatDurationOnly(minDiff, "en", digitStyle));
            result.put("duration_only_es", formatDurationOnly(minDiff, "es", digitStyle));
            result.put("duration_only_fr", formatDurationOnly(minDiff, "fr", digitStyle));
            result.put("duration_only_ja", formatDurationOnly(minDiff, "ja", digitStyle));
            result.put("duration_only_zh", formatDurationOnly(minDiff, "zh", digitStyle));
        }

        return result;
    }

    public static JSONObject getDynamicTimings(JSONObject storedData) {
        return getDynamicTimings(storedData, false, DEFAULT_DIGIT_STYLE);
    }

    public static JSONObject getDynamicTimings(JSONObject storedData, boolean showShuruq) {
        return getDynamicTimings(storedData, showShuruq, DEFAULT_DIGIT_STYLE);
    }

    public static JSONObject getDynamicTimings(Context context, JSONObject storedData, boolean showShuruq) {
        return getDynamicTimings(storedData, showShuruq, getDigitStyle(context));
    }

    public static JSONObject getDynamicTimings(JSONObject storedData, boolean showShuruq, int digitStyle) {
        try {
            JSONObject timings = new JSONObject();
            JSONObject times = (storedData.has("times")) ? storedData.getJSONObject("times") : storedData;
            String[] keys = { "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha" };

            for (String key : keys) {
                String raw = "";
                if (times.has(key)) {
                    JSONObject p = times.optJSONObject(key);
                    if (p != null) {
                        if (p.has("raw_time"))
                            raw = p.optString("raw_time", "");
                        else if (p.has("raw"))
                            raw = p.optString("raw", "");
                        else if (p.has("time_24"))
                            raw = p.optString("time_24", "");
                    }
                }
                // Keep an unparsable value as empty so it is skipped instead of
                // being treated as midnight (which would pick the wrong prayer)
                timings.put(key, raw);
            }
            return processTimings(timings, showShuruq, digitStyle);
        } catch (Exception e) {
            return storedData;
        }
    }

    /**
     * Parses a raw prayer time ("18:04", "18:04 (AST)", "6:04 pm") into {hour, minute}.
     * Returns null when the value is missing or invalid.
     */
    private static int[] parseTimeOfDay(String timeStr) {
        if (timeStr == null)
            return null;
        String s = timeStr.trim();
        if (s.isEmpty())
            return null;

        s = s.split("\\(")[0].trim(); // strip "(AST)" / "(EDT)"
        boolean pm = s.toLowerCase(Locale.US).contains("pm");
        boolean am = s.toLowerCase(Locale.US).contains("am");

        s = s.replaceAll("[^0-9:]", "");
        String[] parts = s.split(":");
        if (parts.length < 2)
            return null;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (am && h == 12)
                h = 0;
            if (pm && h < 12)
                h += 12;
            if (h < 0 || h > 23 || m < 0 || m > 59)
                return null;
            return new int[] { h, m };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int minutesOfDay(Calendar cal) {
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    /** Remaining whole minutes, rounded up, so a countdown never shows 0 too early. */
    private static long ceilMinutes(long diffMs) {
        if (diffMs <= 0)
            return 0;
        return (diffMs + 60000L - 1) / 60000L;
    }

    public static String formatTime(String time24, String lang) {
        return formatTime(time24, lang, DEFAULT_DIGIT_STYLE);
    }

    public static String formatTime(String time24, String lang, int digitStyle) {
        int[] hm = parseTimeOfDay(time24);
        if (hm == null)
            return "--:--";

        int h = hm[0] % 12;
        if (h == 0)
            h = 12;
        String res = h + ":" + String.format(Locale.US, "%02d", hm[1]);
        return localize(res, digitStyle);
    }

    public static String formatDuration(long diffMs, String lang) {
        return formatDuration(diffMs, lang, DEFAULT_DIGIT_STYLE);
    }

    public static String formatDuration(long diffMs, String lang, int digitStyle) {
        if (diffMs == Long.MAX_VALUE)
            return "--";
        long totalMin = ceilMinutes(diffMs);
        long h = totalMin / 60;
        long m = totalMin % 60;

        if (lang.equals("ar")) {
            String res;
            if (h == 0)
                res = arabicMinutesPhrase(m, digitStyle);
            else if (m == 0)
                res = arabicHoursPhrase(h, digitStyle);
            else
                res = arabicHoursPhrase(h, digitStyle) + " و" + arabicMinutesPhrase(m, digitStyle);
            return "بقي: " + res;
        } else if (lang.equals("es")) {
            String res = (h == 0) ? (m + " min") : (h + " h y " + m + " min");
            return "Quedan: " + localize(res, digitStyle);
        } else if (lang.equals("fr")) {
            String res = (h == 0) ? (m + " min") : (h + " h et " + m + " min");
            return "Il reste: " + localize(res, digitStyle);
        } else if (lang.equals("ja")) {
            String res = (h == 0) ? (m + "分") : (h + "時間" + m + "分");
            return "残り: " + res;
        } else if (lang.equals("zh")) {
            String res = (h == 0) ? (m + " 分钟") : (h + " 小时 " + m + " 分钟");
            return "剩余: " + localize(res, digitStyle);
        } else {
            String res = (h == 0) ? (m + " min") : (h + " hr " + m + " min");
            return "Remaining: " + localize(res, digitStyle);
        }
    }

    public static String formatDurationOnly(long diffMs, String lang) {
        return formatDurationOnly(diffMs, lang, DEFAULT_DIGIT_STYLE);
    }

    public static String formatDurationOnly(long diffMs, String lang, int digitStyle) {
        if (diffMs == Long.MAX_VALUE)
            return "--";
        long totalMin = ceilMinutes(diffMs);
        long h = totalMin / 60;
        long m = totalMin % 60;

        if (lang.equals("ar")) {
            return (h == 0) ? (localize(String.valueOf(m), digitStyle) + " د")
                    : (localize(String.valueOf(h), digitStyle) + " س و"
                            + localize(String.valueOf(m), digitStyle) + " د");
        } else if (lang.equals("ja")) {
            return (h == 0) ? (m + "分") : (h + "時" + m + "分");
        } else if (lang.equals("zh")) {
            return (h == 0) ? (m + "分") : (h + "时" + m + "分");
        } else {
            return (h == 0) ? (m + "m") : (h + "h " + m + "m");
        }
    }

    /** Arabic minutes with correct grammar: 1 / 2 / 3-10 (دقائق) / 11+ (دقيقة). */
    private static String arabicMinutesPhrase(long m, int digitStyle) {
        if (m == 1)
            return "دقيقة واحدة";
        if (m == 2)
            return "دقيقتان";
        String word = (m >= 3 && m <= 10) ? " دقائق" : " دقيقة";
        return localize(String.valueOf(m), digitStyle) + word;
    }

    /** Arabic hours with correct grammar: 1 / 2 / 3-10 (ساعات) / 11+ (ساعة). */
    private static String arabicHoursPhrase(long h, int digitStyle) {
        if (h == 1)
            return "ساعة";
        if (h == 2)
            return "ساعتان";
        String word = (h >= 3 && h <= 10) ? " ساعات" : " ساعة";
        return localize(String.valueOf(h), digitStyle) + word;
    }

    /** Single digit-style entry point so every number on the widget looks the same. */
    public static String localize(String input, int digitStyle) {
        return HijriDateUtils.localizeDigits(input, digitStyle);
    }

    public static int getDigitStyle(Context context) {
        if (context == null)
            return DEFAULT_DIGIT_STYLE;
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                    Context.MODE_PRIVATE);
            return prefs.getInt("digit_style", DEFAULT_DIGIT_STYLE);
        } catch (Exception e) {
            return DEFAULT_DIGIT_STYLE;
        }
    }

    public static String localizeDigits(String input) {
        return localize(input, DEFAULT_DIGIT_STYLE);
    }
}
