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

import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

import androidx.core.content.ContextCompat;

//FIXME add javadoc
//FIXME make methods non static
public class OneTimeLocationProvider {
    @SuppressLint("MissingPermission") // :)
    public static void getLocation(final Context context, Consumer<Coordinate> callback) {
        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        if (!hasPermissions(context)) {
            throw new SecurityException();
        }

        locationManager.requestSingleUpdate(criteria, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                callback.accept(new Coordinate(location.getLatitude(), location.getLongitude()));
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
        }, null);
    }

    public static boolean hasPermissions(final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
