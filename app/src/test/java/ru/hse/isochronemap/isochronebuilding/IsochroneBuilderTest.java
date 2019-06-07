package ru.hse.isochronemap.isochronebuilding;

import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.Edge;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.mapstructure.TransportType;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IsochroneBuilderTest {

    @Test
    void getIsochronePolygonTriangle()
            throws NotEnoughNodesException {
        Coordinate startNodeCoordinate = new Coordinate(59.931039, 30.360980);
        Coordinate hullNode1Coordinate = new Coordinate(59.932321, 30.361683);
        Coordinate hullNode2Coordinate = new Coordinate(59.931434, 30.358003);
        Coordinate hullNode3Coordinate = new Coordinate(59.929937, 30.361192);

        Node startNode = new Node(startNodeCoordinate, false);
        Node hullNode1 = new Node(hullNode1Coordinate, false);
        Node hullNode2 = new Node(hullNode2Coordinate, false);
        Node hullNode3 = new Node(hullNode3Coordinate, false);

        for (Node destination : Arrays.asList(hullNode1, hullNode2, hullNode3)) {
            Edge e = new Edge(startNodeCoordinate.distanceTo(destination.coordinate), destination);
            startNode.edges.add(e);
        }

        List<IsochronePolygon> polygons = IsochroneBuilder.getIsochronePolygons(
                startNode,
                0.16,
                TransportType.FOOT,
                IsochroneRequestType.CONVEX_HULL
        );

        assertEquals(1, polygons.size());

        IsochronePolygon hull = polygons.get(0);

        assertEquals(3, hull.getExteriorRing().size());
        assertEquals(0, hull.getInteriorRings().size());
        assertSame(hullNode1Coordinate, hull.getExteriorRing().get(0));
        assertSame(hullNode2Coordinate, hull.getExteriorRing().get(1));
        assertSame(hullNode3Coordinate, hull.getExteriorRing().get(2));
    }
}