package ru.hse.isochronemap.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

/** Wraps LocationManager and provides methods to obtain current/last known location conveniently.*/
public class IsochroneMapLocationManager {
    private final LocationManager locationManager;
    private final Criteria criteria = new Criteria();
    private final Context context;

    /**
     * Creates IsochroneMapLocationManager and tries to obtain approximate location (this could
     * speed up next calls to approximate location method).
     */
    public IsochroneMapLocationManager(@NonNull final Context context) throws InterruptedException {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initializeCriteria();

        // this method could run slowly right after a phone restart
        // (happens once and then the method runs normally) therefore it is better
        // to run it the first time in constructor rather than when the location is actually needed.
        getApproximateLocation();
    }

    /** Checks for ACCESS_FINE_LOCATION permissions. */
    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
               == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Tries to immediately return last known location.
     * If there is no last known location for GPS ans NETWORK providers
     * then blocks, tries to obtain precise location and returns it.
     * If precise location could not be obtained (app does not have
     * necessary permissions or geolocation is disabled) then immediately returns null.
     *
     * @throws InterruptedException if caller thread was interrupted
     *                              while waiting for current location.
     */
    // Permissions are checked in a separate method therefore IntelliJ IDEA does not understand it.
    @SuppressLint("MissingPermission")
    public Coordinate getApproximateLocation() throws InterruptedException {
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
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            BlockingQueue<Coordinate> queue = new ArrayBlockingQueue<>(1);
            getPreciseLocationAsynchronously(queue::add, Looper.getMainLooper());
            return queue.take();
        }

        // return null if nothing succeeded
        return null;
    }

    /**
     * Blocks, tries to obtain current location and then returns it. Must not be invoked from main
     * thread. Asks usert
     *
     * @return current GPS provider's location.
     * @throws InterruptedException if caller thread was interrupted while waiting for location.
     * @throws SecurityException if ACCESS_FINE_LOCATION permissions were not given.
     */
    public Coordinate getPreciseLocationBlocking() throws InterruptedException, SecurityException {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            askUserToEnableGPS();
            return null;
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
    // Permissions are checked in a separate method therefore IntelliJ IDEA does not understand it.
    @SuppressLint("MissingPermission")
    public void getPreciseLocationAsynchronously(@Nullable Consumer<Coordinate> callback,
                                                 @Nullable Looper looper) throws SecurityException{
        if (!hasPermissions()) {
            throw new SecurityException();
        }

        locationManager.requestSingleUpdate(criteria, new CachedLocationListener(callback), looper);
    }

    private void askUserToEnableGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Please enable GPS and try again.")
               .setCancelable(false)
               .setPositiveButton("Go to settings", (dialog, id) ->
                       context.startActivity(new Intent(android.provider.Settings
                                                                .ACTION_LOCATION_SOURCE_SETTINGS)))
               .setNegativeButton("NO!!!", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
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
