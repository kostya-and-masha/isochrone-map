package ru.hse.isochronemap.mapstructure;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.NotNull;
import org.locationtech.spatial4j.distance.DistanceUtils;

import androidx.annotation.NonNull;

/** Geographical coordinate. **/
public class Coordinate implements Parcelable {
    public static final Parcelable.Creator<Coordinate> CREATOR =
            new Parcelable.Creator<Coordinate>() {
        @Override
        public Coordinate createFromParcel(@NonNull Parcel in) {
            return new Coordinate(in);
        }

        @Override
        public Coordinate[] newArray(int size) {
            return new Coordinate[size];
        }
    };

    public final double latitudeDeg;
    public final double longitudeDeg;

    private final double latitudeRad;
    private final double longitudeRad;

    /**
     * Constructs geographical coordinate.
     * @param latitude latitude in degrees
     * @param longitude longitude in degrees
     */
    public Coordinate(double latitude, double longitude) {
        this.latitudeDeg = latitude;
        this.longitudeDeg = longitude;

        this.latitudeRad = latitudeDeg * DistanceUtils.DEGREES_TO_RADIANS;
        this.longitudeRad = longitudeDeg * DistanceUtils.DEGREES_TO_RADIANS;
    }

    /** Constructs geographical coordinate from {@link LatLng}. **/
    public Coordinate(@NonNull LatLng latLng) {
        this(latLng.latitude, latLng.longitude);
    }

    private Coordinate(@NonNull Parcel in) {
        this(in.readDouble(), in.readDouble());
    }

    /** Converts coordinate to {@link LatLng}. **/
    public @NonNull LatLng toLatLng() {
        return new LatLng(latitudeDeg, longitudeDeg);
    }

    /**
     * Calculates distance from other coordinate.
     * @return distance in kilometers
     */
    public double distanceTo(@NotNull Coordinate other) {
        return DistanceUtils.distHaversineRAD(
                latitudeRad, longitudeRad,
                other.latitudeRad, other.longitudeRad
        ) * DistanceUtils.EARTH_MEAN_RADIUS_KM;
    }

    /** Equals method with precision. Does not obey all equals laws */
    public boolean equalsWithPrecision(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coordinate that = (Coordinate) o;
        return Math.abs(that.latitudeDeg - latitudeDeg) < 1e-8
                && Math.abs(that.longitudeDeg - longitudeDeg) < 1e-8;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(latitudeDeg);
        out.writeDouble(longitudeDeg);
    }
}
