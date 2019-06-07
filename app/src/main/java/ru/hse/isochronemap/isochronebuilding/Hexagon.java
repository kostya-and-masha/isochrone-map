package ru.hse.isochronemap.isochronebuilding;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;

/** This class represents hexagons on the plane. */
class Hexagon {
    private static final String WRONG_NUMBER_OF_POINTS_MESSAGE = "Hexagon must contain 6 points";
    private final Coordinate[] coordinates;

    /** Creates hexagon with 6 given points arranged in a clockwise traversal. */
    Hexagon(@NonNull Coordinate[] coordinates) {
        if (coordinates.length != 6) {
            throw new IllegalArgumentException(WRONG_NUMBER_OF_POINTS_MESSAGE);
        }
        this.coordinates = Arrays.copyOf(coordinates, 6);
    }

    /** Returns hexagons' border points. */
    @NonNull Coordinate[] getCoordinates() {
        return coordinates;
    }

    /** Checks whether hexagon contains given point. */
    boolean contains(@NonNull Coordinate point) {
        for (int i = 0; i < 6; i++) {
            if (ConvexHullBuilder.isRightTurn(coordinates[i], coordinates[(i + 1) % 6], point)) {
                return false;
            }
        }
        return true;
    }

    /** Converts hexagon to JTS Polygon */
    @NonNull Polygon toJTSPolygon() {
        GeometryFactory geometryFactory = new GeometryFactory();
        org.locationtech.jts.geom.Coordinate coordinatesJTS[] =
                new org.locationtech.jts.geom.Coordinate[7];
        for (int i = 0; i < 7; i++) {
            coordinatesJTS[i] =
                    new org.locationtech.jts.geom.Coordinate(coordinates[i % 6].longitude,
                                                             coordinates[i % 6].latitude);
        }
        return geometryFactory.createPolygon(coordinatesJTS);
    }
}
