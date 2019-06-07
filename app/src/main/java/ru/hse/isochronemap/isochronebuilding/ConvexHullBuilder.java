package ru.hse.isochronemap.isochronebuilding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;

/** This class provides static method which builds convex hull of given points */
class ConvexHullBuilder {
    /** Andrew's monotone chain algorithm */
    static @NonNull List<Coordinate> getPointsConvexHull(@NonNull List<Coordinate> coordinates) {
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
    private static @NonNull List<Coordinate> buildConvexHullGraham(
            @NonNull List<Coordinate> sortedCoordinates) {
        ArrayList<Coordinate> resultArray = new ArrayList<>();
        resultArray.add(sortedCoordinates.get(0));
        resultArray.add(sortedCoordinates.get(1));

        for (int i = 2; i < sortedCoordinates.size(); i++) {
            while (resultArray.size() >= 2 && isRightTurn(resultArray.get(resultArray.size() - 2),
                                                          resultArray.get(resultArray.size() - 1),
                                                          sortedCoordinates.get(i))) {
                resultArray.remove(resultArray.size() - 1);
            }
            resultArray.add(sortedCoordinates.get(i));
        }

        return resultArray;
    }

    static boolean isRightTurn(Coordinate p1, Coordinate p2, Coordinate p3) {
        // x1y2 - y1x2
        return !((p2.longitudeDeg - p1.longitudeDeg) * (p3.latitudeDeg - p1.latitudeDeg)
                 - (p2.latitudeDeg - p1.latitudeDeg) * (p3.longitudeDeg - p1.longitudeDeg) > 0);
    }
}
