package com.example.isochronemap.mapstructure;

import org.jetbrains.annotations.NotNull;
import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.Objects;

/** Geographical coordinate. **/
public class Coordinate {
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

    /**
     * Calculates distance from other coordinate.
     * @return distance in kilometers
     */
    double distanceTo(@NotNull Coordinate other) {
        return DistanceUtils.distHaversineRAD(
                latitudeRad, longitudeRad,
                other.latitudeRad, other.longitudeRad
        ) * DistanceUtils.EARTH_MEAN_RADIUS_KM;
    }

    @Override
    public boolean equals(Object o) {
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
    public int hashCode() {
        return Objects.hash(latitudeDeg, longitudeDeg);
    }
}
