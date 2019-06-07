package ru.hse.isochronemap.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

/** Provides precise location and stores previous results. */
public class CachedLocationProvider {
    private static final int MIN_UPDATE_TIME_MILLIS = 1000 * 60 * 15; // 15 minutes
    private static final int MIN_UPDATE_DISTANCE_KM = 5;

    private final LocationManager locationManager;
    private final Criteria criteria = new Criteria();
    private final Context context;
    private boolean subscribedToUpdates;
    private Coordinate lastLocation;

    /**
     * Creates CachedLocationProvider from current context and initial cached location.
     * Subscribes cache to regular updates if has permissions.
     */
    // Permissions are checked in a separate method therefore IntelliJ does not understand it.
    @SuppressLint("MissingPermission")
    public CachedLocationProvider(@NonNull final Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        if (hasPermissions()) {
            subscribeToUpdates();
        }

    }

    /** Checks for location permissions. */
    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
               == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Immediately invokes callback with cached location.
     * If there is no cached location then tries to obtain precise location and asynchronously
     * invokes callback with it. If precise location could not be obtained then immediately invokes
     * callback with null.
     *
     * @throws SecurityException if cached location is null but precise location
     *                           could not be obtained due to lack of permissions.
     */
    public void getApproximateLocation(@NonNull Consumer<Coordinate> callback) {
        if (hasPermissions()) {
            subscribeToUpdates();
        }

        if (lastLocation != null) {
            callback.accept(lastLocation);
        } else if (hasPermissions()) {
            getPreciseLocation(callback);
        } else {
            callback.accept(null);
        }
    }

    /**
     * Subscribes location cache to regular updates.
     *
     * @throws SecurityException if no permissions were given.
     */
    // Permissions are checked in a separate method therefore IntelliJ does not understand it.
    @SuppressLint("MissingPermission")
    private void subscribeToUpdates() {
        if (!hasPermissions()) {
            throw new SecurityException();
        }

        if (!subscribedToUpdates) {
            locationManager.requestLocationUpdates(MIN_UPDATE_TIME_MILLIS,
                                                   MIN_UPDATE_DISTANCE_KM,
                                                   criteria,
                                                   new CachedLocationListener(null),
                                                   null);
            subscribedToUpdates = true;
        }
    }

    /**
     * Calls callback with current precise location.
     *
     * @throws SecurityException if no permissions were given.
     */
    // Permissions are checked in a separate method therefore IntelliJ does not understand it.
    @SuppressLint("MissingPermission")
    public void getPreciseLocation(@NonNull Consumer<Coordinate> callback) {
        if (!hasPermissions()) {
            throw new SecurityException();
        }

        subscribeToUpdates();
        locationManager.requestSingleUpdate(criteria, new CachedLocationListener(callback), null);
    }

    private class CachedLocationListener implements LocationListener {
        private Consumer<Coordinate> callback;

        private CachedLocationListener(@Nullable Consumer<Coordinate> callback) {
            this.callback = callback;
        }

        @Override
        public void onLocationChanged(Location location) {
            lastLocation = new Coordinate(location.getLatitude(), location.getLongitude());
            if (callback != null) {
                callback.accept(lastLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}
