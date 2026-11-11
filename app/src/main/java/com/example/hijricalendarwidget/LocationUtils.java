package com.example.hijricalendarwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class LocationUtils {

    public interface LocationCallback {
        void onLocationFound(double lat, double lon);

        void onError(String msg);
    }

    @SuppressLint("MissingPermission")
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            callback.onError("Location Manager not available");
            return;
        }

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            callback.onError("GPS and Network are disabled");
            return;
        }

        // Retrieve best last known location of any age as a fallback
        Location lastKnownGps = null;
        Location lastKnownNetwork = null;
        try {
            if (isGpsEnabled) {
                lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (isNetworkEnabled) {
                lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            Log.e("LocationUtils", "Error getting last known location", e);
        }

        Location fallbackLocation = null;
        if (lastKnownGps != null && lastKnownNetwork != null) {
            fallbackLocation = (lastKnownGps.getTime() > lastKnownNetwork.getTime()) ? lastKnownGps : lastKnownNetwork;
        } else {
            fallbackLocation = (lastKnownGps != null) ? lastKnownGps : lastKnownNetwork;
        }

        // If we have a fallback location and it is very fresh (< 15 mins), use it immediately to save battery
        if (fallbackLocation != null && (System.currentTimeMillis() - fallbackLocation.getTime() < 15 * 60 * 1000)) {
            callback.onLocationFound(fallbackLocation.getLatitude(), fallbackLocation.getLongitude());
            return;
        }

        // Otherwise, request a fresh location.
        // We prefer NETWORK_PROVIDER because it is fast and works indoors.
        String provider = isNetworkEnabled ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;

        final Location finalFallback = fallbackLocation;
        final boolean[] hasFinished = { false };
        final Handler handler = new Handler(Looper.getMainLooper());

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (hasFinished) {
                    if (hasFinished[0]) return;
                    hasFinished[0] = true;
                }
                handler.removeCallbacksAndMessages(null);
                callback.onLocationFound(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Timeout handler: if the location manager takes more than 8 seconds, cancel and use the last known location
        Runnable timeoutRunnable = () -> {
            synchronized (hasFinished) {
                if (hasFinished[0]) return;
                hasFinished[0] = true;
            }
            try {
                locationManager.removeUpdates(listener);
            } catch (Exception e) {
                Log.e("LocationUtils", "Error removing updates on timeout", e);
            }
            if (finalFallback != null) {
                Log.d("LocationUtils", "Location request timed out. Using fallback last known location.");
                callback.onLocationFound(finalFallback.getLatitude(), finalFallback.getLongitude());
            } else {
                callback.onError("Location request timed out and no fallback location is available");
            }
        };

        // Schedule the timeout
        handler.postDelayed(timeoutRunnable, 8000);

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        } catch (Exception e) {
            synchronized (hasFinished) {
                if (hasFinished[0]) return;
                hasFinished[0] = true;
            }
            handler.removeCallbacksAndMessages(null);
            if (finalFallback != null) {
                callback.onLocationFound(finalFallback.getLatitude(), finalFallback.getLongitude());
            } else {
                callback.onError("Error requesting single update: " + e.getMessage());
            }
        }
    }
}
