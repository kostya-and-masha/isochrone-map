package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class HexagonalCoverBuilder {
    static final double HEXAGON_RADIUS = 0.05; // 50 meters

    static Set<Hexagon> getHexagonalCover(List<Coordinate> coordinates) {
        //FIXME magic constants
        double leftmostX = 1000;
        double lowermostY = 1000;

        for (Coordinate point : coordinates) {
            leftmostX = Math.min(leftmostX, point.longitudeDeg);
            lowermostY = Math.min(lowermostY, point.latitudeDeg);
        }

        Coordinate leftCornerHexagonCenter = new Coordinate(lowermostY, leftmostX);
        Set<Hexagon> hexagons = new HashSet<>();

        for (Coordinate point: coordinates) {
            List<Coordinate> hexagonCenters =
                    getClosestHexagonCenters(leftCornerHexagonCenter, point);
            for (Coordinate center : hexagonCenters) {
                Hexagon currentHexagon = getHexagonFromCenter(center);
                if (currentHexagon.contains(point)) {
                    hexagons.add(currentHexagon);
                    break;
                }
            }
        }
        return hexagons;
    }

    static private Hexagon getHexagonFromCenter(Coordinate center) {
        List<Coordinate> points = new ArrayList<>();
        for (double angle = Math.PI/6; angle < 2*Math.PI; angle += Math.PI/3) {
            points.add(new Coordinate(
                    center.latitudeDeg + latitudeFromKm(HEXAGON_RADIUS * 1.0001 * Math.sin(angle)),
                    center.longitudeDeg + longitudeFromkm(
                            HEXAGON_RADIUS * 1.0001 * Math.cos(angle), center.latitudeDeg))
            );
        }
        try {
            return new Hexagon(points.toArray(new Coordinate[0]));
        } catch (UnsupportedParameterException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    //FIXME this method is disgusting
    static private List<Coordinate> getClosestHexagonCenters(Coordinate cornerHexagonCenter,
                                                             Coordinate point) {
        double horizontalStepLength = longitudeFromkm(
                2 * HEXAGON_RADIUS * Math.cos(Math.PI/6), cornerHexagonCenter.latitudeDeg);
        double verticalStepLength = latitudeFromKm(1.5 * HEXAGON_RADIUS);

        double verticalDistance = point.latitudeDeg - cornerHexagonCenter.latitudeDeg;
        long verticalStepNumberSmaller = Math.round(Math.floor(verticalDistance/verticalStepLength));

        double LowerPointsY = cornerHexagonCenter.latitudeDeg
                + verticalStepLength * verticalStepNumberSmaller;
        double UpperPointsY = LowerPointsY + verticalStepLength;

        List<Coordinate> result = new ArrayList<>();

        double[] evenStepsPointsXs = getTwoClosestValuesWithStep(
                cornerHexagonCenter.longitudeDeg, horizontalStepLength, point.longitudeDeg);
        double[] oddStepsPointsXs = getTwoClosestValuesWithStep(
                cornerHexagonCenter.longitudeDeg - horizontalStepLength / 2,
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

    static private double longitudeFromkm(double km, double latitude) {
        return km / (111 * Math.cos(latitude * DistanceUtils.DEGREES_TO_RADIANS));
    }
}
