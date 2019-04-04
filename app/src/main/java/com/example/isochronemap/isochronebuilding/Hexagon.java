package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Hexagon hexagon = (Hexagon) o;
        return Arrays.equals(coordinates, hexagon.coordinates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates);
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
