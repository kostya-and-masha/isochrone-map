package ru.hse.isochronemap.isochronebuilding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.hse.isochronemap.mapstructure.Coordinate;

import static org.junit.Assert.*;

public class HexagonalCoverBuilderTest {
    static final double DISTANCE_TO_BORDER = IsochroneBuilder.HEXAGON_RADIUS
                                             * Math.cos(Math.PI / 6);

    private static final Coordinate startCoordinate = new Coordinate(0, 0);
    private static final int pointsOnLine = 100;
    private static final int pointsOnCircle = 48;
    private final HexagonalCoverBuilder builder =
            new HexagonalCoverBuilder(IsochroneBuilder.HEXAGON_RADIUS, startCoordinate);

    @Test
    public void testDoesNotCoverZeroPoints() throws InterruptedException {
        List<Hexagon> hexagons = builder.getHexagonalCover(Collections.emptyList());
        assertEquals(0, hexagons.size());
    }

    @Test
    public void testCoversOnePoint() throws InterruptedException {
        List<Hexagon> hexagons =
                builder.getHexagonalCover(Collections.singletonList(startCoordinate));
        assertEquals(1, hexagons.size());
        assertTrue(hexagons.get(0).contains(startCoordinate));
    }

    @Test
    public void testCoversStraightLine() throws InterruptedException {
        int expectedNumberOfHexagons = 30;
        double lineEndLongitude = HexagonalCoverBuilder.longitudeFromKm(
                (expectedNumberOfHexagons - 1) * DISTANCE_TO_BORDER * 2, 0);

        List<Coordinate> lineCoordinates = new ArrayList<>();
        for (int i = 0; i < pointsOnLine; i++) {
            lineCoordinates.add(
                    new Coordinate(0, lineEndLongitude / (pointsOnLine - 1) * i));
        }

        List<Hexagon> hexagons = builder.getHexagonalCover(lineCoordinates);
        assertEquals(expectedNumberOfHexagons, hexagons.size());

        // checking that hexagons contain all points
        for (Coordinate point : lineCoordinates) {
            assertTrue(hexagons.stream().anyMatch(hexagon -> hexagon.contains(point)));
        }
    }

    /** See testCoversWithHolesExplanation.png in androidTest resources for explanation */
    @Test
    public void testCoversWithHoles() throws InterruptedException {
        int expectedNumberOfHexagons = 12;
        double outerCircleRadiusKm = DISTANCE_TO_BORDER * 4;
        double innerCircleRadiusKm = DISTANCE_TO_BORDER * 2;

        List<Coordinate> outerCircle = generatePointsOnCircle(outerCircleRadiusKm);
        List<Coordinate> innerCircle = generatePointsOnCircle(innerCircleRadiusKm);

        List<Hexagon> hexagons = builder.getHexagonalCover(outerCircle);
        assertEquals(expectedNumberOfHexagons, hexagons.size());

        for (Coordinate point : outerCircle) {
            assertTrue(hexagons.stream().anyMatch(hexagon -> hexagon.contains(point)));
        }
        for (Coordinate point : innerCircle) {
            assertFalse(hexagons.stream().anyMatch(hexagon -> hexagon.contains(point)));
        }
        assertFalse(hexagons.stream().anyMatch(hexagon -> hexagon.contains(startCoordinate)));
    }

    static List<Coordinate> generatePointsOnCircle(double radius) {
        List<Coordinate> circlePoints = new ArrayList<>();
        for (int i = 0; i < pointsOnCircle; i++) {
            double angle = 2 * Math.PI / pointsOnCircle * i;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            Coordinate currentPoint =
                    new Coordinate(HexagonalCoverBuilder.latitudeFromKm(y),
                                   HexagonalCoverBuilder.longitudeFromKm(x,0));
            circlePoints.add(currentPoint);
        }
        return circlePoints;
    }
}