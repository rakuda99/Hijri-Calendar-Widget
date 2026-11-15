package com.example.hijricalendarwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class LocationUtils {

    private static final String TAG = "LocationUtils";
    private static final String PREFS_NAME = "HijriWidgetPrefs";

    /** Minimum distance (km) that triggers a full prayer-time + cache invalidation. */
    public static final float SIGNIFICANT_MOVE_KM = CacheManager.MAX_DISTANCE_KM;

    /** Max age for a cached location before we force a fresh GPS fix (regardless of distance). */
    private static final long MAX_LOCATION_AGE_MS = 15L * 60L * 1000L; // 15 minutes

    public interface LocationCallback {
        void onLocationFound(double lat, double lon);
        void onError(String msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Distance helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the great-circle distance in kilometres between two coordinates.
     * Uses {@link Location#distanceBetween} which is accurate for all distances.
     */
    public static float distanceKm(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache invalidation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clears all location-dependent caches so the next network sync will
     * re-fetch prayer times and weather for the new position.
     */
    public static void invalidateLocationAndPrayerCache(Context context) {
        CacheManager.invalidatePrayerCache(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putLong("last_network_sync", 0L)
            .remove("prayer_times_date")
            .remove("current_temperature")
            .apply();

        Log.i(TAG, "Location and prayer caches invalidated.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core location fetch with movement-distance validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtains the device's current position with movement-distance awareness:
     *
     * <ol>
     *   <li>Reads the best available {@code LastKnownLocation}.</li>
     *   <li>If it is fresh (&lt;15 min) <em>and</em> the device has NOT moved
     *       more than {@link #SIGNIFICANT_MOVE_KM} from the last saved position,
     *       returns it immediately (battery-friendly fast path).</li>
     *   <li>Otherwise, requests a fresh one-shot fix from the OS and waits up
     *       to 8 seconds before falling back.</li>
     *   <li>After a fresh fix is obtained, automatically checks for significant
     *       movement and invalidates caches + refreshes all widgets if needed.</li>
     * </ol>
     */
    @SuppressLint("MissingPermission")
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            callback.onError("Location Manager not available");
            return;
        }

        boolean isGpsEnabled     = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            callback.onError("GPS and Network are disabled");
            return;
        }

        // --- Step 1: collect LastKnownLocation from both providers ----------------
        Location lastKnownGps = null;
        Location lastKnownNetwork = null;
        try {
            if (isGpsEnabled)     lastKnownGps     = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (isNetworkEnabled) lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location", e);
        }

        // Pick the most recent of the two
        Location lastKnown;
        if (lastKnownGps != null && lastKnownNetwork != null) {
            lastKnown = (lastKnownGps.getTime() > lastKnownNetwork.getTime())
                        ? lastKnownGps : lastKnownNetwork;
        } else {
            lastKnown = (lastKnownGps != null) ? lastKnownGps : lastKnownNetwork;
        }

        // --- Step 2: fast path only if fresh AND not a significant move -----------
        if (lastKnown != null
                && (System.currentTimeMillis() - lastKnown.getTime() < MAX_LOCATION_AGE_MS)) {

            boolean movedSignificantly = hasMovedSignificantly(context,
                    lastKnown.getLatitude(), lastKnown.getLongitude());

            if (!movedSignificantly) {
                // Location is fresh and device hasn't moved far — use it directly
                Log.d(TAG, "Using fresh LastKnownLocation (no significant movement).");
                callback.onLocationFound(lastKnown.getLatitude(), lastKnown.getLongitude());
                return;
            }

            // Device moved > 5 km — fall through to get a fresh fix and
            // let the post-fix handler decide whether to invalidate caches.
            Log.i(TAG, "LastKnownLocation is fresh but device moved significantly — requesting new fix.");
        }

        // --- Step 3: request a fresh one-shot fix ---------------------------------
        String provider = isNetworkEnabled
                ? LocationManager.NETWORK_PROVIDER
                : LocationManager.GPS_PROVIDER;

        final Location finalLastKnown = lastKnown;
        final boolean[] hasFinished = {false};
        final Handler handler = new Handler(Looper.getMainLooper());

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (hasFinished) {
                    if (hasFinished[0]) return;
                    hasFinished[0] = true;
                }
                handler.removeCallbacksAndMessages(null);
                try { locationManager.removeUpdates(this); } catch (Exception ignored) {}

                // Check if we moved significantly from the SAVED position
                handleFreshLocation(context, location, callback);
            }

            @Override public void onStatusChanged(String p, int s, Bundle e) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };

        // Timeout: fall back to LastKnownLocation (or error) after 8 seconds
        Runnable timeoutRunnable = () -> {
            synchronized (hasFinished) {
                if (hasFinished[0]) return;
                hasFinished[0] = true;
            }
            try { locationManager.removeUpdates(listener); } catch (Exception ignored) {}

            if (finalLastKnown != null) {
                Log.d(TAG, "Location fix timed out. Falling back to last known location.");
                // Even the stale fallback should be checked for movement
                handleFreshLocation(context, finalLastKnown, callback);
            } else {
                callback.onError("Location request timed out and no fallback location is available");
            }
        };

        handler.postDelayed(timeoutRunnable, 8000);

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        } catch (Exception e) {
            synchronized (hasFinished) {
                if (hasFinished[0]) return;
                hasFinished[0] = true;
            }
            handler.removeCallbacksAndMessages(null);
            if (finalLastKnown != null) {
                handleFreshLocation(context, finalLastKnown, callback);
            } else {
                callback.onError("Error requesting single update: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Movement detection + cache invalidation on fresh fix
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given coordinates are more than
     * {@link CacheManager#MAX_DISTANCE_KM} km from the last saved position.
     * Returns {@code false} when no saved position exists (treat as "not moved").
     */
    public static boolean hasMovedSignificantly(Context context, double newLat, double newLon) {
        float savedLat = (float) CacheManager.getCacheLat(context);
        float savedLon = (float) CacheManager.getCacheLon(context);

        if (savedLat == 0f && savedLon == 0f) {
            // No saved location yet — treat as first run, not a move
            return false;
        }

        float km = distanceKm(newLat, newLon, savedLat, savedLon);
        Log.d(TAG, String.format("Distance from saved position: %.2f km (threshold: %.1f km)",
                km, CacheManager.MAX_DISTANCE_KM));
        return km > CacheManager.MAX_DISTANCE_KM;
    }

    /**
     * Called whenever we have a "fresh" location (new fix or stale fallback).
     * <p>
     * Logic:
     * <ul>
     *   <li>If the device moved &gt;5 km from the last saved coords:
     *       invalidate all caches, trigger an immediate full refresh, then
     *       deliver the new coordinates to the callback.</li>
     *   <li>Otherwise just deliver the coordinates.</li>
     * </ul>
     */
    private static void handleFreshLocation(Context context, Location location, LocationCallback callback) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        if (hasMovedSignificantly(context, lat, lon)) {
            Log.i(TAG, String.format(
                    "Significant movement detected! Invalidating caches and refreshing. New pos: %.4f, %.4f",
                    lat, lon));

            // 1. Wipe all stale caches
            invalidateLocationAndPrayerCache(context);

            // 2. Save the new position immediately so the next check doesn't fire again
            CacheManager.saveLastKnownLocation(context, lat, lon);

            // 3. Trigger a full widget refresh (prayer times + date)
            //    This runs on whatever thread called us; use a background thread
            //    so we don't block the main thread on network I/O.
            new Thread(() -> {
                try {
                    HijriDateUtils.syncTimeWithNetwork(context, true /* force */);
                } catch (Exception e) {
                    Log.e(TAG, "Error during post-move sync", e);
                }
            }).start();
        }

        // Deliver coordinates to the original caller (prayer/weather fetch continues normally)
        callback.onLocationFound(lat, lon);
    }
}
