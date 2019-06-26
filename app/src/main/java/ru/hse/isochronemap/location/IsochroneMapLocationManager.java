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
import android.os.Looper;
import android.widget.Toast;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

/** Wraps LocationManager and provides methods to obtain current/last known location conveniently.*/
public class IsochroneMapLocationManager {
    public static final int APPROXIMATE_LOCATION_TIMEOUT = 4000;

    private static final String GPS_ALERT_FRAGMENT_TAG = "GPSAlertFragment";
    private final LocationManager locationManager;
    private final Criteria criteria = new Criteria();
    private final FragmentActivity fragmentActivity;

    /**
     * Creates IsochroneMapLocationManager and tries to obtain current location (this could
     * speed up next calls to approximate location method).
     *
     * @param fragmentActivity FragmentActivity is needed instead of Context in order to
     *                         create AlertFragments which are retained across screen rotations.
     */
    public IsochroneMapLocationManager(@NonNull FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
        locationManager =
                (LocationManager) fragmentActivity.getSystemService(Context.LOCATION_SERVICE);
        initializeCriteria();

        // speeds up getApproximateLocation in case the app is run right after phone restart
        if (hasPermissions()) {
            getPreciseLocationAsynchronously(null, Looper.getMainLooper());
        }
    }

    /** Checks for ACCESS_FINE_LOCATION permissions. */
    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(fragmentActivity,
                                                 Manifest.permission.ACCESS_FINE_LOCATION)
               == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Tries to immediately return last known location.
     * If there is no last known location for GPS ans NETWORK providers
     * then blocks for APPROXIMATE_LOCATION_TIMEOUT milliseconds,
     * tries to obtain precise location and returns it.
     * If precise location could not be obtained (waiting time elapses / app does not have
     * necessary permissions / geolocation is disabled) then returns null.
     *
     * @throws InterruptedException if caller thread was interrupted
     *                              while waiting for current location.
     */
    // Permissions are checked in a separate method therefore IntelliJ IDEA does not understand it.
    @SuppressLint("MissingPermission")
    public @Nullable Coordinate getApproximateLocation() throws InterruptedException {
        if (!hasPermissions()) {
            return null;
        }

        // try to receive last known gps location
        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (gpsLocation != null) {
            return new Coordinate(gpsLocation.getLatitude(), gpsLocation.getLongitude());
        }

        // try to receive last known network location
        Location networkLocation =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (networkLocation != null) {
            return new Coordinate(networkLocation.getLatitude(), networkLocation.getLongitude());
        }

        // try get precise gps location
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            BlockingQueue<Coordinate> queue = new ArrayBlockingQueue<>(1);
            getPreciseLocationAsynchronously(queue::add, Looper.getMainLooper());
            return queue.poll(APPROXIMATE_LOCATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // return null if nothing succeeded
        return null;
    }

    /**
     * Blocks, tries to obtain current location and then returns it. Must not be invoked from main
     * thread. Asks user to enable geolocation if both GPS and NETWORK providers are disabled.
     *
     * @return current location or null if location could not be obtained.
     * @throws InterruptedException if caller thread was interrupted while waiting for location.
     * @throws SecurityException if ACCESS_FINE_LOCATION permissions were not given.
     */
    public @Nullable Coordinate getPreciseLocationBlocking() throws InterruptedException,
                                                                    SecurityException {
        boolean gpsProviderEnabled =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkProviderEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsProviderEnabled && !networkProviderEnabled) {
            askUserToEnableGPS();
            return null;
        } else if (!networkProviderEnabled) {
            fragmentActivity.runOnUiThread(
                    () -> Toast.makeText(fragmentActivity,
                                         fragmentActivity.getString(R.string.a_gps_disabled_toast),
                                         Toast.LENGTH_LONG)
                               .show());
        }

        BlockingQueue<Coordinate> queue = new ArrayBlockingQueue<>(1);
        getPreciseLocationAsynchronously(queue::add, Looper.getMainLooper());
        return queue.take();
    }
    
    /**
     * Invokes callback asynchronously with current precise location.
     *
     * @param callback a callback that will be invoked with current location.
     * @param looper a Looper thread whose message queue will be used to implement the
     *               callback mechanism, or null to make callbacks on the calling thread .
     *
     * @throws SecurityException if ACCESS_FINE_LOCATION permissions were not given.
     */
    public void getPreciseLocationAsynchronously(@Nullable Consumer<Coordinate> callback,
                                                 @Nullable Looper looper) throws SecurityException {
        locationManager.requestSingleUpdate(criteria, new CachedLocationListener(callback), looper);
    }

    private void askUserToEnableGPS() {
        new EnableGPSDialogFragment().show(fragmentActivity.getSupportFragmentManager(),
                                           GPS_ALERT_FRAGMENT_TAG);
    }
    
    private void initializeCriteria() {
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
    }

    private class CachedLocationListener implements LocationListener {
        private Consumer<Coordinate> callback;

        private CachedLocationListener(@Nullable Consumer<Coordinate> callback) {
            this.callback = callback;
        }

        @Override
        public void onLocationChanged(Location location) {
            Coordinate coordinate = new Coordinate(location.getLatitude(), location.getLongitude());
            if (callback != null) {
                callback.accept(coordinate);
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
