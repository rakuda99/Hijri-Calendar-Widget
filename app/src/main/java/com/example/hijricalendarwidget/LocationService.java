package com.example.hijricalendarwidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.Locale;

/**
 * Location service with travel validation. Recalculates prayer times when the device moves
 * more than 5 km from the cached coordinates, the resolved city changes, or the cached day is
 * stale. Location accuracy must be better than 100 meters.
 */
public class LocationService {

    private static final String TAG = "LocationRefresh";

    /** How long to wait for a fresh one-shot fix before falling back to a last known fix. */
    private static final long FRESH_FIX_TIMEOUT_MS = 8000L;

    /** Maximum distance (km) before cache is invalidated. Delegates to CacheManager.MAX_DISTANCE_KM. */
    public static final float MAX_DISTANCE_KM = CacheManager.MAX_DISTANCE_KM;

    /** Minimum acceptable location accuracy (meters). */
    public static final float MAX_ACCURACY_METERS = 100.0f;

    public interface LocationResultCallback {
        void onLocationUpdated(double lat, double lon, String city, boolean cacheInvalidated,
                               String previousCity, float distanceKm);

        void onError(String msg);
    }

    /**
     * Checks the current location against the cached coordinates. When the device has moved
     * more than 3 km with acceptable accuracy, the prayer cache is invalidated, prayer times
     * are recalculated, and all widgets are refreshed.
     */
    public static void checkLocationAndUpdate(final Context context,
                                              final LocationResultCallback callback) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
                && androidx.core.content.ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted — cannot refresh");
            if (callback != null) callback.onError("Location permission not granted");
            return;
        }

        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            if (callback != null) callback.onError("Location Manager not available");
            return;
        }

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGpsEnabled && !isNetworkEnabled) {
            if (callback != null) callback.onError("GPS and Network are disabled");
            return;
        }

        String provider = isNetworkEnabled ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;

        final Location lastKnownGps = isGpsEnabled ? safeLastKnown(locationManager, LocationManager.GPS_PROVIDER) : null;
        final Location lastKnownNetwork = isNetworkEnabled
                ? safeLastKnown(locationManager, LocationManager.NETWORK_PROVIDER) : null;
        final Location lastKnown = newestLocation(lastKnownGps, lastKnownNetwork);

        Log.i(TAG, "checkLocationAndUpdate — provider=" + provider
                + " gps=" + isGpsEnabled + " network=" + isNetworkEnabled
                + " lastKnown=" + describeLocation(lastKnown));

        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] hasFinished = { false };

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (!finish(hasFinished, handler)) return;
                try {
                    locationManager.removeUpdates(this);
                } catch (Exception ignored) { }
                handleLocationResult(context, location, callback);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onProviderDisabled(String provider) { }
        };

        Runnable timeoutRunnable = () -> {
            if (!finish(hasFinished, handler)) return;
            if (lastKnown != null) {
                // Widget taps run without a foreground process, so a fresh fix may never
                // arrive. A last known fix is good enough to refresh the city.
                Log.w(TAG, "Fresh fix timed out after " + FRESH_FIX_TIMEOUT_MS + "ms — using last known location");
                handleLocationResult(context, lastKnown, callback);
                return;
            }
            Log.w(TAG, "Fresh fix timed out and no last known location is available");
            postError(callback, "Could not get a location fix. Check that location is enabled and try again.");
        };

        handler.postDelayed(timeoutRunnable, FRESH_FIX_TIMEOUT_MS);

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        } catch (Exception e) {
            if (finish(hasFinished, handler)) {
                Log.w(TAG, "requestSingleUpdate failed: " + e.getMessage());
                if (lastKnown != null) {
                    handleLocationResult(context, lastKnown, callback);
                } else {
                    postError(callback, "Could not request a location fix: " + e.getMessage());
                }
            }
        }
    }

    /** Guards the single-shot listener/timeout race. Returns true for the first caller. */
    private static synchronized boolean finish(boolean[] hasFinished, Handler handler) {
        if (hasFinished[0]) return false;
        hasFinished[0] = true;
        handler.removeCallbacksAndMessages(null);
        return true;
    }

    @SuppressLint("MissingPermission")
    private static Location safeLastKnown(LocationManager locationManager, String provider) {
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (Exception e) {
            return null;
        }
    }

    private static Location newestLocation(Location a, Location b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getTime() >= b.getTime() ? a : b;
    }

    private static String describeLocation(Location location) {
        if (location == null) return "null";
        return location.getLatitude() + "," + location.getLongitude()
                + " accuracy=" + location.getAccuracy() + "m age="
                + (System.currentTimeMillis() - location.getTime()) + "ms";
    }

    /** Entry point for the city/temperature row tap. Always requests a fresh fix. */
    public static void refreshLocationFromUserTap(Context context) {
        Log.i(TAG, "User tapped city/temperature row — requesting fresh GPS fix");
        checkLocationAndUpdate(context, null);
    }

    private static void handleLocationResult(Context context, Location location,
                                             LocationResultCallback callback) {
        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final float accuracy = location.getAccuracy();

        new Thread(() -> {
            SharedPreferences prefs = context.getSharedPreferences("HijriWidgetPrefs",
                    Context.MODE_PRIVATE);
            double cachedLat = CacheManager.getCacheLat(context);
            double cachedLon = CacheManager.getCacheLon(context);
            String cachedCity = CacheManager.getCacheCity(context);
            String previousJson = CacheManager.getCachedPrayerJson(context);

            String resolvedCity = resolveCityName(context, lat, lon);
            String city = (resolvedCity != null && !resolvedCity.isEmpty()) ? resolvedCity : cachedCity;

            float distanceKm = 0f;
            if (cachedLat != 0f || cachedLon != 0f) {
                distanceKm = CacheManager.haversineDistanceKm(cachedLat, cachedLon, lat, lon);
            }

            boolean cityChanged = resolvedCity != null && !resolvedCity.isEmpty()
                    && !resolvedCity.equals(cachedCity);
            boolean movedSignificantly = distanceKm > MAX_DISTANCE_KM;
            boolean noCache = (cachedLat == 0f && cachedLon == 0f);
            boolean dateStale = !CacheManager.todayCacheKey().equals(CacheManager.getCacheDate(context));
            boolean noJson = (previousJson == null || previousJson.isEmpty() || previousJson.equals("{}"));
            boolean shouldRecalc = cityChanged || movedSignificantly || noCache || dateStale || noJson;

            // The accuracy gate must come first: an unacceptably coarse fix is not allowed to
            // overwrite a good cache. A stale or coarse fix must never silently replace the city.
            boolean accuracyOk = CacheManager.isAccuracyAcceptable(location, MAX_ACCURACY_METERS);
            Log.i(TAG, "Accuracy " + accuracy + "m — acceptable=" + accuracyOk);
            if (!accuracyOk) {
                Log.w(TAG, "Location accuracy " + accuracy + "m exceeds "
                        + MAX_ACCURACY_METERS + "m — keeping existing cache");
                if (city != null && !city.isEmpty() && !city.equals(cachedCity)) {
                    // City resolution is independent of the fix quality; persist it alone so the
                    // widget can show the right name without touching prayer times.
                    CacheManager.saveCity(context, city);
                    Log.i(TAG, "Resolved city saved without recalculation: " + city);
                    final String resolvedForCallback = city;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        HijriDateUtils.refreshAllWidgetsNow(context);
                        if (callback != null) {
                            callback.onLocationUpdated(cachedLat, cachedLon, resolvedForCallback, false,
                                    cachedCity, 0f);
                        }
                    });
                    return;
                }
                postError(callback, "Location accuracy too low (" + (int) accuracy + "m).");
                return;
            }

            Log.i(TAG, "Previous city: " + cachedCity + " | New city: " + city);
            Log.i(TAG, "Previous coordinates: " + cachedLat + "," + cachedLon
                    + " | New coordinates: " + lat + "," + lon);
            Log.i(TAG, "Distance moved: " + String.format(Locale.US, "%.2f", distanceKm)
                    + " km (threshold " + MAX_DISTANCE_KM + " km)");
            Log.i(TAG, "City changed: " + cityChanged + " | Moved: " + movedSignificantly
                    + " | No cache: " + noCache + " | Date stale: " + dateStale
                    + " | No JSON: " + noJson + " | Should recalc: " + shouldRecalc);

            if (!shouldRecalc) {
                if (previousJson != null && !previousJson.isEmpty()) {
                    CacheManager.savePrayerCache(context, city, lat, lon, previousJson);
                    Log.i(TAG, "No prayer recalculation (same city, distance <= "
                            + MAX_DISTANCE_KM + " km); cached coordinates updated");
                } else {
                    CacheManager.saveResolvedLocation(context, city, lat, lon);
                }
                final String callbackCity = city;
                final String callbackPreviousCity = cachedCity;
                final float callbackDistanceKm = distanceKm;
                new Handler(Looper.getMainLooper()).post(() -> {
                    HijriDateUtils.refreshAllWidgetsNow(context);
                    if (callback != null) {
                        callback.onLocationUpdated(lat, lon, callbackCity, false,
                                callbackPreviousCity, callbackDistanceKm);
                    }
                });
                return;
            }

            String reason = cityChanged ? "city changed"
                    : movedSignificantly ? "distance > " + MAX_DISTANCE_KM + " km"
                    : noCache ? "no cached coordinates"
                    : noJson ? "no cached prayer JSON"
                    : "cache date stale";
            Log.i(TAG, "Cache invalidated — reason: " + reason);
            CacheManager.invalidatePrayerCache(context);

            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                final int year = cal.get(java.util.Calendar.YEAR);
                final int month = cal.get(java.util.Calendar.MONTH) + 1;
                final int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

                int methodIdx = prefs.getInt("prayer_calc_method", 0);
                int asrIdx = prefs.getInt("prayer_asr_juristic", 0);
                final PrayerService.CalcMethod method = PrayerService.INSTANCE.fromLegacyIndex(methodIdx);
                final com.batoulapps.adhan2.Madhab madhab =
                        (asrIdx == 1)
                                ? com.batoulapps.adhan2.Madhab.HANAFI
                                : com.batoulapps.adhan2.Madhab.SHAFI;

                final String finalCity = (city != null && !city.isEmpty()) ? city : "Riyadh";
                final float finalDistanceKm = distanceKm;
                final String previousCity = cachedCity;

                Log.i(TAG, "Adhan recalculation: lat=" + lat + " lon=" + lon
                        + " method=" + method + " year=" + year + " month=" + month + " day=" + day);
                PrayerService.PrayerTimesResult result = PrayerService.INSTANCE.calculate(
                        lat, lon, year, month, day, method, madhab);
                String json = buildPrayerJson(result);

                CacheManager.savePrayerCache(context, finalCity, lat, lon, json);
                CacheManager.touchWidgetUpdate(context);

                Log.i(TAG, "Previous prayer JSON: " + previousJson);
                Log.i(TAG, "New prayer JSON: " + json);
                Log.i(TAG, "Cache saved — city: " + finalCity + " lat: " + lat + " lon: " + lon);

                new Handler(Looper.getMainLooper()).post(() -> {
                    HijriDateUtils.refreshAllWidgetsNow(context);
                    Log.i(TAG, "All widgets refresh broadcast sent");
                    if (callback != null) {
                        callback.onLocationUpdated(lat, lon, finalCity, true,
                                previousCity, finalDistanceKm);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Prayer recalculation failed", e);
                postError(callback, "Prayer calculation failed: " + e.getMessage());
            }
        }, "LocationRefresh").start();
    }

    private static void postError(LocationResultCallback callback, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onError(msg);
        });
    }

    private static String resolveCityName(Context context, double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && addresses.size() > 0) {
                Address addr = addresses.get(0);
                String locality = addr.getLocality();
                if (locality != null && !locality.isEmpty()) {
                    return locality;
                }
                String adminArea = addr.getAdminArea();
                if (adminArea != null && !adminArea.isEmpty()) {
                    return adminArea;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String buildPrayerJson(PrayerService.PrayerTimesResult result) {
        // PrayerUtils.getDynamicTimings() expects capitalized keys: Fajr, Sunrise, Dhuhr,
        // Asr, Maghrib, Isha (HH:mm strings).
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("Fajr", formatTime(result.getFajrMs()));
            json.put("Sunrise", formatTime(result.getSunriseMs()));
            json.put("Dhuhr", formatTime(result.getDhuhrMs()));
            json.put("Asr", formatTime(result.getAsrMs()));
            json.put("Maghrib", formatTime(result.getMaghribMs()));
            json.put("Isha", formatTime(result.getIshaMs()));
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static String formatTime(long utcEpochMs) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(utcEpochMs);
    }

    // Prevent instantiation
    private LocationService() { }
}
