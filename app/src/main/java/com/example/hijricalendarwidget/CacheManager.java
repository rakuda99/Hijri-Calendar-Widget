package com.example.hijricalendarwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

/**
 * Central prayer cache manager. Cache holds date, city, coordinates, prayer JSON, and sync
 * timestamps. A cache entry is valid only when the date, city, and coordinates (within 3 km)
 * all match the current values.
 */
public class CacheManager {

    private static final String PREFS_NAME = "HijriWidgetPrefs";

    /** Prayer cache preference keys (public for cross-class access). */
    public static final String KEY_CACHE_DATE = "prayer_cache_date";
    public static final String KEY_CACHE_CITY = "prayer_cache_city";
    /** Legacy city key still read by the settings screens. */
    public static final String KEY_PRAYER_CITY = "prayer_city";
    public static final String KEY_CACHE_LAT = "prayer_cache_lat";
    public static final String KEY_CACHE_LON = "prayer_cache_lon";
    /** Legacy coordinate keys. Written alongside the cache keys so both views stay in sync. */
    public static final String KEY_LAST_LAT = "last_lat";
    public static final String KEY_LAST_LON = "last_lon";
    public static final String KEY_PRAYER_JSON = "prayer_times_json";
    private static final String KEY_LAST_PRAYER_UPDATE_MS = "prayer_last_prayer_update_ms";
    private static final String KEY_LAST_LOCATION_UPDATE_MS = "prayer_last_location_update_ms";
    private static final String KEY_LAST_WIDGET_UPDATE_MS = "prayer_last_widget_update_ms";

    /** Maximum distance (km) between cached and current coordinates before cache is invalidated. */
    public static final float MAX_DISTANCE_KM = 5.0f;

    /** Returns today's date as a cache key (yyyy-MM-dd, local time zone). */
    public static String todayCacheKey() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(cal.getTimeInMillis());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns true when the cached prayer data is valid for the given date, city, and coordinates.
     * Valid when: date matches today AND city matches AND haversine distance <= 3 km.
     */
    public static boolean isPrayerCacheValid(Context context, double lat, double lon, String city) {
        SharedPreferences p = prefs(context);
        String cachedDate = p.getString(KEY_CACHE_DATE, null);
        String cachedCity = p.getString(KEY_CACHE_CITY, null);
        float cachedLat = p.getFloat(KEY_CACHE_LAT, 0);
        float cachedLon = p.getFloat(KEY_CACHE_LON, 0);

        android.util.Log.d("PrayerDebug", "isPrayerCacheValid: cachedDate=" + cachedDate + " today=" + todayCacheKey()
                + " cachedCity=" + cachedCity + " city=" + city
                + " cachedLat=" + cachedLat + " cachedLon=" + cachedLon + " lat=" + lat + " lon=" + lon);

        if (cachedDate == null || !cachedDate.equals(todayCacheKey())) {
            android.util.Log.d("PrayerDebug", "Cache invalid: date mismatch");
            return false;
        }
        if (cachedCity == null || !cachedCity.equals(city)) {
            android.util.Log.d("PrayerDebug", "Cache invalid: city mismatch");
            return false;
        }
        if (cachedLat == 0 && cachedLon == 0) {
            android.util.Log.d("PrayerDebug", "Cache invalid: zero coordinates");
            return false;
        }
        float distanceKm = haversineDistanceKm(cachedLat, cachedLon, lat, lon);
        android.util.Log.d("PrayerDebug", "Distance: " + String.format("%.2f", distanceKm) + " km (threshold: " + MAX_DISTANCE_KM + " km)");
        boolean valid = distanceKm <= MAX_DISTANCE_KM;
        android.util.Log.d("PrayerDebug", "Cache valid=" + valid);
        return valid;
    }

    /** Clears the entire prayer cache. Last known coordinates are kept as a fallback. */
    public static void invalidatePrayerCache(Context context) {
        prefs(context).edit()
                .remove(KEY_CACHE_DATE)
                .remove(KEY_CACHE_CITY)
                .remove(KEY_CACHE_LAT)
                .remove(KEY_CACHE_LON)
                .remove(KEY_PRAYER_JSON)
                .remove(KEY_LAST_PRAYER_UPDATE_MS)
                .remove(KEY_LAST_LOCATION_UPDATE_MS)
                .apply();
    }

    /**
     * Persists the last known coordinates in both the cache keys and the legacy keys so every
     * reader sees one consistent position. Leaves date, city, and prayer JSON untouched.
     */
    public static void saveLastKnownLocation(Context context, double lat, double lon) {
        prefs(context).edit()
                .putFloat(KEY_CACHE_LAT, (float) lat)
                .putFloat(KEY_CACHE_LON, (float) lon)
                .putFloat(KEY_LAST_LAT, (float) lat)
                .putFloat(KEY_LAST_LON, (float) lon)
                .apply();
    }

    /**
     * Persists only the resolved city name. Coordinates, date, and prayer JSON are left untouched
     * so a coarse or stale fix cannot corrupt a good cache.
     */
    public static void saveCity(Context context, String city) {
        if (city == null || city.isEmpty()) return;
        prefs(context).edit()
                .putString(KEY_CACHE_CITY, city)
                .putString(KEY_PRAYER_CITY, city)
                .apply();
        android.util.Log.d("CityDebug", "saveCity: " + city);
    }

    /**
     * Persists a resolved location without recomputing prayer times. When the city differs from
     * the cached one, the prayer JSON is dropped because it no longer matches the location, so
     * the next widget render recalculates it.
     */
    public static void saveResolvedLocation(Context context, String city, double lat, double lon) {
        SharedPreferences p = prefs(context);
        String cachedCity = p.getString(KEY_CACHE_CITY, null);
        boolean cityChanged = city != null && !city.isEmpty() && !city.equals(cachedCity);

        SharedPreferences.Editor editor = p.edit()
                .putFloat(KEY_CACHE_LAT, (float) lat)
                .putFloat(KEY_CACHE_LON, (float) lon)
                .putFloat(KEY_LAST_LAT, (float) lat)
                .putFloat(KEY_LAST_LON, (float) lon);
        if (city != null && !city.isEmpty()) {
            editor.putString(KEY_CACHE_CITY, city).putString(KEY_PRAYER_CITY, city);
        }
        if (cityChanged) {
            editor.remove(KEY_PRAYER_JSON);
        }
        editor.apply();

        android.util.Log.d("CityDebug", "saveResolvedLocation: city=" + city + " lat=" + lat + " lon=" + lon
                + " cityChanged=" + cityChanged);
    }

    /**
     * Persists the prayer cache with date, city, coordinates, JSON, and timestamps.
     * Updates last_prayer_update_ms and last_location_update_ms.
     */
    public static void savePrayerCache(Context context, String city, double lat,
                                       double lon, String json) {
        long now = System.currentTimeMillis();
        prefs(context).edit()
                .putString(KEY_CACHE_DATE, todayCacheKey())
                .putString(KEY_CACHE_CITY, city)
                .putFloat(KEY_CACHE_LAT, (float) lat)
                .putFloat(KEY_CACHE_LON, (float) lon)
                .putString(KEY_PRAYER_JSON, json)
                .putString(KEY_PRAYER_CITY, city)
                .putFloat(KEY_LAST_LAT, (float) lat)
                .putFloat(KEY_LAST_LON, (float) lon)
                .putLong(KEY_LAST_PRAYER_UPDATE_MS, now)
                .putLong(KEY_LAST_LOCATION_UPDATE_MS, now)
                .apply();
        android.util.Log.d("PrayerDebug", "savePrayerCache: city=" + city + " lat=" + lat + " lon=" + lon + " json=" + (json != null ? json.substring(0, Math.min(json.length(), 60)) : "null"));
    }

    /** Returns the cached prayer times JSON, or null when absent. */
    public static String getCachedPrayerJson(Context context) {
        return prefs(context).getString(KEY_PRAYER_JSON, null);
    }

    public static String getCacheDate(Context context) {
        return prefs(context).getString(KEY_CACHE_DATE, null);
    }

    public static String getCacheCity(Context context) {
        String city = prefs(context).getString(KEY_CACHE_CITY, null);
        android.util.Log.d("CityDebug", "getCacheCity: returning=" + city);
        return city;
    }

    public static double getCacheLat(Context context) {
        return prefs(context).getFloat(KEY_CACHE_LAT, 0);
    }

    public static double getCacheLon(Context context) {
        return prefs(context).getFloat(KEY_CACHE_LON, 0);
    }

    public static long getLastPrayerUpdateMs(Context context) {
        return prefs(context).getLong(KEY_LAST_PRAYER_UPDATE_MS, 0);
    }

    public static long getLastLocationUpdateMs(Context context) {
        return prefs(context).getLong(KEY_LAST_LOCATION_UPDATE_MS, 0);
    }

    public static long getLastWidgetUpdateMs(Context context) {
        return prefs(context).getLong(KEY_LAST_WIDGET_UPDATE_MS, 0);
    }

    /** Records that all widgets were just refreshed. */
    public static void touchWidgetUpdate(Context context) {
        prefs(context).edit().putLong(KEY_LAST_WIDGET_UPDATE_MS, System.currentTimeMillis()).apply();
    }

    /**
     * Haversine great-circle distance between two points, in kilometers.
     */
    public static float haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadiusKm * c);
    }

    /** Returns true when a Location's accuracy is within the acceptable threshold (meters). */
    public static boolean isAccuracyAcceptable(Location location, float maxAccuracyMeters) {
        if (location == null) {
            return false;
        }
        float acc = location.getAccuracy();
        return acc > 0 && acc <= maxAccuracyMeters;
    }
}
