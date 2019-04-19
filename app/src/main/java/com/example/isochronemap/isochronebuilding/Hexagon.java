package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

//TODO add javadocs
class Hexagon {
    private final Coordinate[] coordinates = new Coordinate[6];

    Hexagon(@NotNull Coordinate[] coordinates) throws UnsupportedParameterException {
        if (coordinates.length != 6) {
            throw new UnsupportedParameterException("Hexagon contains 6 points");
        }
        System.arraycopy(coordinates, 0, this.coordinates, 0, 6);
    }

    @NotNull Coordinate[] getCoordinates() {
        return coordinates;
    }

    boolean contains(@NotNull Coordinate point) {
        for (int i = 0; i < 6; i++) {
            if (!ConvexHullBuilder.isLeftTurn(coordinates[i], coordinates[(i + 1)%6], point)) {
                return false;
            }
        }
        return true;
    }

    @NotNull Polygon toJTSPolygon() {
        GeometryFactory geometryFactory = new GeometryFactory();
        org.locationtech.jts.geom.Coordinate coordinatesJTS[] =
                new org.locationtech.jts.geom.Coordinate[7];
        for (int i = 0; i < 7; i++) {
            coordinatesJTS[i] = new org.locationtech.jts.geom.Coordinate(
                    coordinates[i%6].longitudeDeg, coordinates[i%6].latitudeDeg);
        }
        return geometryFactory.createPolygon(coordinatesJTS);
    }
}
