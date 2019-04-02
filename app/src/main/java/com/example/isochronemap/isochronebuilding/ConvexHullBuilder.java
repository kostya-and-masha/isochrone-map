package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ConvexHullBuilder {
    /** Andrew's monotone chain algorithm */
    static @NotNull List<Coordinate> getPointsConvexHull(
            @NotNull List<Coordinate> coordinates) {
        Collections.sort(coordinates, (o1, o2) -> {
            if (o1.longitudeDeg == o2.longitudeDeg) {
                return Double.compare(o1.latitudeDeg, o2.latitudeDeg);
            }
            return Double.compare(o1.longitudeDeg, o2.longitudeDeg);
        });

        List<Coordinate> bottomHull = buildConvexHullGraham(coordinates);
        Collections.reverse(coordinates);
        List<Coordinate> topHull = buildConvexHullGraham(coordinates);
        for (int i = 1; i < bottomHull.size() - 1; ++i) {
            topHull.add(bottomHull.get(i));
        }

        return topHull;
    }

    /** Graham's algorithm for one half of the plane */
    private static @NotNull List<Coordinate> buildConvexHullGraham(
            @NotNull List<Coordinate> sortedCoordinates) {
        ArrayList<Coordinate> resultArray = new ArrayList<>();
        resultArray.add(sortedCoordinates.get(0));
        resultArray.add(sortedCoordinates.get(1));

        for (int i = 2; i < sortedCoordinates.size(); i++) {
            while (resultArray.size() >= 2 && !isLeftTurn(
                    resultArray.get(resultArray.size() - 2),
                    resultArray.get(resultArray.size() - 1),
                    sortedCoordinates.get(i))) {
                resultArray.remove(resultArray.size() - 1);
            }
            resultArray.add(sortedCoordinates.get(i));
        }

        return resultArray;
    }

    private static boolean isLeftTurn(Coordinate p1, Coordinate p2, Coordinate p3) {
        // x1y2 - y1x2
        return (p2.longitudeDeg - p1.longitudeDeg)*(p3.latitudeDeg - p1.latitudeDeg) -
                (p2.latitudeDeg - p1.latitudeDeg)*(p3.longitudeDeg - p1.longitudeDeg) > 0;
    }
}
