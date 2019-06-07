package ru.hse.isochronemap.isochronebuilding;

import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;

/** This class provides static method which covers given points by hexagons. */
class HexagonalCoverBuilder {
    private static final int KM_IN_ONE_LATITUDE_DEGREE = 111;
    private static final double EPSILON = 0.02;

    private final double radius;
    private final Coordinate startPoint;
    private final double hexagonsHorizontalDistance;
    private final double hexagonsVerticalDistance;

    /**
     * Creates HexagonalCoverBuilder
     *
     * @param radius     distance in KM from center to corners.
     * @param startPoint origin point.
     */
    HexagonalCoverBuilder(double radius, @NonNull Coordinate startPoint) {
        this.radius = radius;
        this.startPoint = startPoint;
        hexagonsHorizontalDistance = 2 * radius * Math.cos(Math.PI / 6);
        hexagonsVerticalDistance = 1.5 * radius;
    }

    /**
     * Covers given points by hexagons.
     *
     * @param coordinates points to cover.
     * @return list of resulting hexagons.
     */
    @NonNull List<Hexagon> getHexagonalCover(@NonNull List<Coordinate> coordinates) {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        List<List<Coordinate>> resultingHexagonCenters = new ArrayList<>(numberOfCores);

        for (int i = 0; i < numberOfCores; ++i) {
            resultingHexagonCenters.add(new ArrayList<>());
        }

        Thread[] threads = new Thread[numberOfCores];
        int chunkSize = (coordinates.size() + numberOfCores - 1) / numberOfCores;

        for (int i = 0; i < numberOfCores; ++i) {
            int currentThread = i;
            int leftIndex = Math.min(currentThread * chunkSize, coordinates.size());
            int rightIndex = Math.min((currentThread + 1) * chunkSize, coordinates.size());
            List<Coordinate> currentThreadCoordinates =
                    Collections.unmodifiableList(coordinates.subList(leftIndex, rightIndex));

            threads[i] = new Thread(() -> {
                for (Coordinate point : currentThreadCoordinates) {
                    List<Coordinate> potentialHexagonCenters = getClosestHexagonCenters(point);
                    for (Coordinate center : potentialHexagonCenters) {
                        Hexagon currentHexagon = getOneHexagonFromCenter(center);
                        if (currentHexagon.contains(point)) {
                            resultingHexagonCenters.get(currentThread).add(center);
                            break;
                        }
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < numberOfCores; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ignored) {
            }
        }

        List<Coordinate> mergedCenters = new ArrayList<>();
        for (int i = 0; i < numberOfCores; i++) {
            mergedCenters.addAll(resultingHexagonCenters.get(i));
        }
        return getHexagonsFromCenters(mergedCenters);
    }

    private @NonNull List<Hexagon> getHexagonsFromCenters(
            @NonNull List<Coordinate> hexagonCenters) {
        List<Hexagon> hexagons = new ArrayList<>();
        Comparator<Coordinate> comparator =
                (c1, c2) -> Math.round((float) Math.signum((c1.longitudeDeg + c1.latitudeDeg)
                                                           - (c2.longitudeDeg + c2.latitudeDeg)));
        Collections.sort(hexagonCenters, comparator);

        hexagons.add(getOneHexagonFromCenter(hexagonCenters.get(0)));
        for (int i = 1; i < hexagonCenters.size(); i++) {
            Coordinate currentCoordinate = hexagonCenters.get(i);
            Coordinate previousCoordinate = hexagonCenters.get(i - 1);
            if (!currentCoordinate.equalsWithPrecision(previousCoordinate)) {
                hexagons.add(getOneHexagonFromCenter(currentCoordinate));
            }
        }
        return hexagons;
    }

    private @NonNull Hexagon getOneHexagonFromCenter(@NonNull Coordinate center) {
        List<Coordinate> points = new ArrayList<>();
        for (double angle = Math.PI / 6; angle < 2 * Math.PI; angle += Math.PI / 3) {
            double latitude = center.latitudeDeg
                              + latitudeFromKm(radius * (1 + EPSILON) * Math.sin(angle));
            double longitude = center.longitudeDeg
                               + longitudeFromKm(radius * (1 + EPSILON) * Math.cos(angle),
                                                 center.latitudeDeg);
            points.add(new Coordinate(latitude, longitude));
        }
        return new Hexagon(points.toArray(new Coordinate[6]));
    }

    private @NonNull List<Coordinate> getClosestHexagonCenters(@NonNull Coordinate point) {
        double horizontalStepLength =
                longitudeFromKm(hexagonsHorizontalDistance, startPoint.latitudeDeg);
        double verticalStepLength = latitudeFromKm(hexagonsVerticalDistance);

        double verticalDistance = point.latitudeDeg - startPoint.latitudeDeg;
        long verticalStepNumberSmaller =
                Math.round(Math.floor(verticalDistance / verticalStepLength));

        double LowerPointsY =
                startPoint.latitudeDeg + verticalStepLength * verticalStepNumberSmaller;
        double UpperPointsY = LowerPointsY + verticalStepLength;

        List<Coordinate> result = new ArrayList<>();

        double[] evenStepsPointsXs =
                getTwoClosestValuesWithStep(startPoint.longitudeDeg, horizontalStepLength,
                                            point.longitudeDeg);
        double[] oddStepsPointsXs =
                getTwoClosestValuesWithStep(startPoint.longitudeDeg - horizontalStepLength / 2,
                                            horizontalStepLength, point.longitudeDeg);

        double evenStepsPointsY;
        double oddStepsPointsY;

        if ((verticalStepNumberSmaller % 2) == 0) {
            evenStepsPointsY = LowerPointsY;
            oddStepsPointsY = UpperPointsY;
        } else {
            evenStepsPointsY = UpperPointsY;
            oddStepsPointsY = LowerPointsY;
        }

        for (int i = 0; i < 2; i++) {
            result.add(new Coordinate(evenStepsPointsY, evenStepsPointsXs[i]));
        }
        for (int i = 0; i < 2; i++) {
            result.add(new Coordinate(oddStepsPointsY, oddStepsPointsXs[i]));
        }

        return result;
    }

    static private @NonNull double[] getTwoClosestValuesWithStep(double start, double step,
                                                                 double destination) {
        double leftValue = start + Math.floor((destination - start) / step) * step;
        return new double[]{leftValue, leftValue + step};
    }

    static private double latitudeFromKm(double km) {
        return km / KM_IN_ONE_LATITUDE_DEGREE;
    }

    static private double longitudeFromKm(double km, double latitude) {
        return km / (KM_IN_ONE_LATITUDE_DEGREE
                     * Math.cos(latitude * DistanceUtils.DEGREES_TO_RADIANS));
    }
}
