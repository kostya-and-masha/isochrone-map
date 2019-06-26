package ru.hse.isochronemap.isochronebuilding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import ru.hse.isochronemap.mapstructure.Coordinate;

import static org.junit.Assert.*;

public class ConvexHullBuilderTest {
    private static final int pointsOnCircle = 24;
    private static final String BIG_TEST_RESOURCE_NAME = "convex_hull_big_test";

    @Test
    public void testConvexHullTwoPoints() {
        Coordinate leftPoint = new Coordinate(0, 0);
        Coordinate rightPoint = new Coordinate(0, 10);

        List<Coordinate> convexHull = ConvexHullBuilder.getPointsConvexHull(
                Arrays.asList(leftPoint, rightPoint));

        assertEquals(2, convexHull.size());
        assertSame(rightPoint, convexHull.get(0));
        assertSame(leftPoint, convexHull.get(1));
    }

    @Test
    public void testConvexHullTriangle() {
        Coordinate leftPoint = new Coordinate(0, 0);
        Coordinate rightPoint = new Coordinate(0, 10);
        Coordinate upPoint = new Coordinate(10, 5);

        List<Coordinate> allPoints = new ArrayList<>(
                Arrays.asList(leftPoint, rightPoint, upPoint));

        // add points which are inside the triangle and should not be in the convex hull
        allPoints.add(new Coordinate(5, 5));
        allPoints.add(new Coordinate(4, 4));
        allPoints.add(new Coordinate(1, 6));

        List<Coordinate> convexHull = ConvexHullBuilder.getPointsConvexHull(allPoints);

        assertEquals(3, convexHull.size());
        assertSame(rightPoint, convexHull.get(0));
        assertSame(upPoint, convexHull.get(1));
        assertSame(leftPoint, convexHull.get(2));
    }

    @Test
    public void testConvexHullStraightLine() {
        Coordinate leftPoint = new Coordinate(0, 0);
        Coordinate rightPoint = new Coordinate(0, 10);

        List<Coordinate> allPoints = new ArrayList<>(Arrays.asList(leftPoint, rightPoint));

        // generate points on the line
        for (int i = 1; i < 10; i++) {
            allPoints.add(new Coordinate(0, i));
        }

        List<Coordinate> convexHull = ConvexHullBuilder.getPointsConvexHull(allPoints);

        assertTrue(convexHull.contains(leftPoint));
        assertTrue(convexHull.contains(rightPoint));
    }

    @Test
    public void testOrderOfPoints() {
        List<Coordinate> sortedPoints = new ArrayList<>();

        // generate points on a circle
        for (int i = 0; i < pointsOnCircle; i++) {
            double angle = 2 * Math.PI / pointsOnCircle * i;
            Coordinate point = new Coordinate(Math.sin(angle), Math.cos(angle));
            sortedPoints.add(point);
        }

        List<Coordinate> shuffledPoints = new ArrayList<>(sortedPoints);
        // shuffle points with predefined seed
        Collections.shuffle(shuffledPoints, new Random(0));

        List<Coordinate> convexHull = ConvexHullBuilder.getPointsConvexHull(shuffledPoints);

        assertEquals(convexHull, sortedPoints);
    }

    @Test
    public void testBigTestCase() {
        Scanner in = new Scanner(getClass().getResourceAsStream(BIG_TEST_RESOURCE_NAME));
        int pointsNumber = in.nextInt();

        List<Coordinate> allPoints = new ArrayList<>(pointsNumber);
        List<Coordinate> convexHullPointsExpected = new ArrayList<>();
        for (int i = 0; i < pointsNumber; i++) {
            Coordinate currentPoint = new Coordinate(in.nextDouble(), in.nextDouble());
            allPoints.add(currentPoint);
            if (in.next().equals("y")) {
                convexHullPointsExpected.add(currentPoint);
            }
        }

        List<Coordinate> convexHullPointsResult = ConvexHullBuilder.getPointsConvexHull(allPoints);
        assertEquals(convexHullPointsExpected.size(), convexHullPointsResult.size());
        assertTrue(convexHullPointsResult.containsAll(convexHullPointsExpected));
    }
}