package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

//TODO add javadocs
public class Hexagon {
    private final Coordinate[] coordinates = new Coordinate[6];

    public Hexagon(@NotNull Coordinate[] coordinates) throws UnsupportedParameterException {
        if (coordinates.length != 6) {
            throw new UnsupportedParameterException("Hexagon contains 6 points");
        }
        System.arraycopy(coordinates, 0, this.coordinates, 0, 6);
    }

    public @NotNull Coordinate[] getCoordinates() {
        return coordinates;
    }

    public boolean contains(@NotNull Coordinate point) {
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
}
