package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.Edge;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.Node;
import com.example.isochronemap.mapstructure.TransportType;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IsochroneBuilderTest {

    @Test
    void getIsochronePolygonTriangle()
            throws UnsupportedParameterException, NotEnoughNodesException {
        Coordinate startNodeCoordinate = new Coordinate(59.931039, 30.360980);
        Coordinate hullNode1Coordinate = new Coordinate(59.932321, 30.361683);
        Coordinate hullNode2Coordinate = new Coordinate(59.931434, 30.358003);
        Coordinate hullNode3Coordinate = new Coordinate(59.929937, 30.361192);

        Node startNode = new Node(startNodeCoordinate, false);
        Node hullNode1 = new Node(hullNode1Coordinate, false);
        Node hullNode2 = new Node(hullNode2Coordinate, false);
        Node hullNode3 = new Node(hullNode3Coordinate, false);

        for (Node destination : Arrays.asList(hullNode1, hullNode2, hullNode3)) {
            Edge e = new Edge(startNodeCoordinate, destination);
            startNode.edges.add(e);
        }

        ArrayList<Node> nodes = new ArrayList<>(
                Arrays.asList(startNode, hullNode1, hullNode2, hullNode3));

        ArrayList<Node> startNodes = new ArrayList<>();
        startNodes.add(startNode);

        MapStructure map = new MapStructure(nodes, startNodes);
        List<Coordinate> Hull =
                IsochroneBuilder.getIsochronePolygon(map, 0.16, TransportType.FOOT);

        assertEquals(3, Hull.size());
        assertSame(hullNode1Coordinate, Hull.get(0));
        assertSame(hullNode2Coordinate, Hull.get(1));
        assertSame(hullNode3Coordinate, Hull.get(2));
    }
}