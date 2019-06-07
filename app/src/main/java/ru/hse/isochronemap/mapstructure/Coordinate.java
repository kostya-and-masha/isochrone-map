package ru.hse.isochronemap.mapstructure;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.locationtech.spatial4j.distance.DistanceUtils;

import androidx.annotation.NonNull;

/** This class represents geographical coordinate. **/
public class Coordinate implements Parcelable {
    /** Creates Coordinate instance from {@link Parcel} **/
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

    /** Latitude in degrees. **/
    public final double latitude;
    /** Longitude in degrees. **/
    public final double longitude;

    private final double latitudeRadians;
    private final double longitudeRadians;

    /**
     * Constructs geographical coordinate.
     * @param latitude latitude in degrees
     * @param longitude longitude in degrees
     */
    public Coordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        latitudeRadians = latitude * DistanceUtils.DEGREES_TO_RADIANS;
        longitudeRadians = longitude * DistanceUtils.DEGREES_TO_RADIANS;
    }

    /**
     * Constructs geographical coordinate from {@link LatLng}.
     * @param latLng {@link @LatLng} coordinate representation
     **/
    public Coordinate(@NonNull LatLng latLng) {
        this(latLng.latitude, latLng.longitude);
    }

    /** Constructs geographical coordinate from {@link Parcel}. **/
    private Coordinate(@NonNull Parcel in) {
        this(in.readDouble(), in.readDouble());
    }

    /** Converts coordinate to {@link LatLng}. **/
    public @NonNull LatLng toLatLng() {
        return new LatLng(latitude, longitude);
    }

    /**
     * Calculates distance to other coordinate.
     * @return distance in kilometers
     */
    public double distanceTo(@NonNull Coordinate other) {
        return DistanceUtils.distHaversineRAD(
                latitudeRadians,
                longitudeRadians,
                other.latitudeRadians,
                other.longitudeRadians) * DistanceUtils.EARTH_MEAN_RADIUS_KM;
    }

    /** Equals method with precision. Does not obey all equals laws. **/
    public boolean equalsWithPrecision(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coordinate that = (Coordinate) o;
        final double THRESHOLD = 1e-8;
        return Math.abs(that.latitude - latitude) < THRESHOLD
                && Math.abs(that.longitude - longitude) < THRESHOLD;
    }

    /**
     * Describes the kinds of special objects contained in this {@link Parcelable} instance
     * (no special objects).
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Saves Coordinate into {@link Parcel}.
     * @param out parcel to write in
     * @param flags ignored
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeDouble(latitude);
        out.writeDouble(longitude);
    }
}
