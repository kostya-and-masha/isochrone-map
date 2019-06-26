package ru.hse.isochronemap.isochronebuilding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.Edge;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.mapstructure.TransportType;

import static org.junit.Assert.*;
import static ru.hse.isochronemap.isochronebuilding.IsochroneBuilder.HEXAGON_RADIUS;

public class IsochroneBuilderTest {
    private static final double SMALL_EPSILON = 1e-7;
    private static final double BIG_EPSILON = 0.01;

    @Test(expected = NotEnoughNodesException.class)
    public void testThrowsExceptionOnSmallGraph()
            throws NotEnoughNodesException, InterruptedException {
        Node startNode = new Node(new Coordinate(0, 0), false);
        IsochroneBuilder.getIsochronePolygons(startNode, 10,
                                              TransportType.FOOT, IsochroneRequestType.CONVEX_HULL);
    }

    @Test
    public void basicConvexHullTestFoot() throws NotEnoughNodesException, InterruptedException {
        generateBasicConvexHullTest(TransportType.FOOT);
    }

    @Test
    public void basicConvexHullTestCar() throws NotEnoughNodesException, InterruptedException {
        generateBasicConvexHullTest(TransportType.CAR);
    }

    @Test
    public void basicConvexHullTestBike() throws NotEnoughNodesException, InterruptedException {
        generateBasicConvexHullTest(TransportType.BIKE);
    }

    @Test
    public void testHexagonalCoverWithSmallHole()
            throws NotEnoughNodesException, InterruptedException {
        List<Coordinate> coordinatesOnCircle = HexagonalCoverBuilderTest.generatePointsOnCircle(
                HexagonalCoverBuilderTest.DISTANCE_TO_BORDER * 2);
        List<Node> nodesOnCircle = new ArrayList<>();
        nodesOnCircle.add(new Node(coordinatesOnCircle.get(0), false));

        for (int i = 1; i < coordinatesOnCircle.size(); i++) {
            Coordinate previousCoordinate = coordinatesOnCircle.get(i - 1);
            Node previousNode = nodesOnCircle.get(i - 1);

            Coordinate currentCoordinate = coordinatesOnCircle.get(i);
            Node currentNode = new Node(currentCoordinate, false);

            previousNode.edges.add(
                    new Edge(previousCoordinate.distanceTo(currentCoordinate), currentNode));
            nodesOnCircle.add(currentNode);
        }

        List<IsochronePolygon> polygons = IsochroneBuilder.getIsochronePolygons(
                nodesOnCircle.get(0),
                Double.MAX_VALUE,
                TransportType.FOOT,
                IsochroneRequestType.HEXAGONAL_COVER);

        assertEquals(1, polygons.size());

        IsochronePolygon polygon = polygons.get(0);
        // check that small hole was deleted
        assertEquals(0, polygon.getInteriorRings().size());

        // check that polygon contains 7 hexagons
        double expectedPerimeter = HEXAGON_RADIUS * 18;
        double polygonPerimeter = calculatePolygonPerimeter(polygon);
        assertTrue(expectedPerimeter - BIG_EPSILON < polygonPerimeter);
        assertTrue(expectedPerimeter + BIG_EPSILON > polygonPerimeter);
    }

    private double calculatePolygonPerimeter(IsochronePolygon polygon) {
        double result = 0;
        List<Coordinate> coordinates = polygon.getExteriorRing();
        for (int i = 1; i < coordinates.size(); i++) {
            result += coordinates.get(i - 1).distanceTo(coordinates.get(i));
        }
        return result;
    }

    private void generateBasicConvexHullTest(TransportType transportType)
            throws NotEnoughNodesException, InterruptedException {
        Coordinate startNodeCoordinate = new Coordinate(59.931039, 30.360980);
        Coordinate hullNode1Coordinate = new Coordinate(59.932321, 30.361683);
        Coordinate hullNode2Coordinate = new Coordinate(59.931434, 30.358003);
        Coordinate hullNode3Coordinate = new Coordinate(59.929937, 30.361192);
        Coordinate farAwayNodeCoordinate = new Coordinate(59.925545, 30.357399);

        Node startNode = new Node(startNodeCoordinate, false);
        Node hullNode1 = new Node(hullNode1Coordinate, false);
        Node hullNode2 = new Node(hullNode2Coordinate, false);
        Node hullNode3 = new Node(hullNode3Coordinate, false);
        Node farAwayNode = new Node(farAwayNodeCoordinate, false);

        for (Node destination : Arrays.asList(hullNode1, hullNode2, hullNode3, farAwayNode)) {
            Edge e = new Edge(startNodeCoordinate.distanceTo(destination.coordinate), destination);
            startNode.edges.add(e);
        }

        // calculate time by which hull nodes are reachable but farAwayNode is not
        double time = startNode.coordinate.distanceTo(farAwayNodeCoordinate)
                      / transportType.getAverageSpeed() - SMALL_EPSILON;

        List<IsochronePolygon> polygons = IsochroneBuilder.getIsochronePolygons(
                startNode,
                time,
                transportType,
                IsochroneRequestType.CONVEX_HULL);

        assertEquals(1, polygons.size());

        IsochronePolygon hull = polygons.get(0);

        assertEquals(3, hull.getExteriorRing().size());
        assertEquals(0, hull.getInteriorRings().size());
        assertSame(hullNode1Coordinate, hull.getExteriorRing().get(0));
        assertSame(hullNode2Coordinate, hull.getExteriorRing().get(1));
        assertSame(hullNode3Coordinate, hull.getExteriorRing().get(2));
    }
}