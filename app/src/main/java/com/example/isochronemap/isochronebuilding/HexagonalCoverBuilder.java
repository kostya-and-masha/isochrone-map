package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;
import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class HexagonalCoverBuilder {
    static final double HEXAGON_RADIUS = 0.05; // 50 meters
    private static final double HEXAGONS_HORIZONTAL_DISTANCE =
            2 * HEXAGON_RADIUS * Math.cos(Math.PI/6);
    private static final double HEXAGONS_VERTICAL_DISTANCE = 1.5 * HEXAGON_RADIUS;

    static List<Hexagon> getHexagonalCover(List<Coordinate> coordinates, Coordinate startPoint) {
        List<Coordinate> resultingHexagonCenters = new ArrayList<>();

        for (Coordinate point: coordinates) {
            List<Coordinate> potentialHexagonCenters =
                    getClosestHexagonCenters(point, startPoint);
            for (Coordinate center : potentialHexagonCenters) {
                Hexagon currentHexagon = getOneHexagonFromCenter(center);
                if (currentHexagon.contains(point)) {
                    resultingHexagonCenters.add(center);
                    break;
                }
            }
        }

        return getHexagonsFromCenters(resultingHexagonCenters);
    }

    private static @NotNull List<Hexagon> getHexagonsFromCenters(
            @NotNull List<Coordinate> resultingHexagonCenters) {
        List<Hexagon> hexagons = new ArrayList<>();
        Collections.sort(resultingHexagonCenters,
                (c1, c2) -> Math.round((float) Math.signum((c1.longitudeDeg + c1.latitudeDeg) -
                        (c2.longitudeDeg + c2.latitudeDeg))));

        hexagons.add(getOneHexagonFromCenter(resultingHexagonCenters.get(0)));
        for (int i = 1; i < resultingHexagonCenters.size(); i++) {
            Coordinate currentCoordinate = resultingHexagonCenters.get(i);
            Coordinate previousCoordinate = resultingHexagonCenters.get(i-1);
            if (!currentCoordinate.equalsWithPrecision(previousCoordinate)) {
                hexagons.add(getOneHexagonFromCenter(currentCoordinate));
            }
        }
        return hexagons;
    }

    static private Hexagon getOneHexagonFromCenter(Coordinate center) {
        List<Coordinate> points = new ArrayList<>();
        for (double angle = Math.PI/6; angle < 2*Math.PI; angle += Math.PI/3) {
            points.add(new Coordinate(
                    center.latitudeDeg + latitudeFromKm(
                            HEXAGON_RADIUS * 1.02 * Math.sin(angle)),
                    center.longitudeDeg + longitudeFromKm(
                            HEXAGON_RADIUS * 1.02 * Math.cos(angle), center.latitudeDeg))
            );
        }
        try {
            return new Hexagon(points.toArray(new Coordinate[0]));
        } catch (UnsupportedParameterException e) {
            //FIXME
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    //FIXME this method is disgusting
    static private List<Coordinate> getClosestHexagonCenters(Coordinate point,
                                                             Coordinate startPoint) {
        double horizontalStepLength = longitudeFromKm(HEXAGONS_HORIZONTAL_DISTANCE,
                startPoint.latitudeDeg);
        double verticalStepLength = latitudeFromKm(HEXAGONS_VERTICAL_DISTANCE);

        double verticalDistance = point.latitudeDeg - startPoint.latitudeDeg;
        long verticalStepNumberSmaller =
                Math.round(Math.floor(verticalDistance/verticalStepLength));

        double LowerPointsY = startPoint.latitudeDeg
                + verticalStepLength * verticalStepNumberSmaller;
        double UpperPointsY = LowerPointsY + verticalStepLength;

        List<Coordinate> result = new ArrayList<>();

        double[] evenStepsPointsXs = getTwoClosestValuesWithStep(
                startPoint.longitudeDeg, horizontalStepLength, point.longitudeDeg);
        double[] oddStepsPointsXs = getTwoClosestValuesWithStep(
                startPoint.longitudeDeg - horizontalStepLength / 2,
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

    static private double[] getTwoClosestValuesWithStep(
            double start, double step, double destination) {
        double leftValue = start + Math.floor((destination - start)/step) * step;
        return new double[] {leftValue, leftValue + step};
    }

    static private double latitudeFromKm(double km) {
        return km / 111;
    }

    static private double longitudeFromKm(double km, double latitude) {
        return km / (111 * Math.cos(latitude * DistanceUtils.DEGREES_TO_RADIANS));
    }
}
