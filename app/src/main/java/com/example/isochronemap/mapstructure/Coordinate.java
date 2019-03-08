package com.example.isochronemap.mapstructure;

import org.jetbrains.annotations.NotNull;
import org.locationtech.spatial4j.distance.DistanceUtils;

/** Geographical coordinate. **/
public class Coordinate {
    final double latitudeDeg;
    final double longitudeDeg;

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
}
