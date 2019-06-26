package ru.hse.isochronemap.isochronebuilding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.Edge;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.mapstructure.TransportType;

import static org.junit.Assert.*;

public class ReachableNodesFinderTest {
    private static final Coordinate dummyCoordinate = new Coordinate(0, 0);
    private static final int depthInTreeTest = 5;
    private static final int numberOfInnerNodesInTree = (1 << (depthInTreeTest - 1)) - 1;
    private static final int numberOfNodesInCycleTest = 16;

    @Test
    public void testTreeGraphFoot() {
        generateTreeGraphTest(TransportType.FOOT);
    }

    @Test
    public void testTreeGraphCar() {
        generateTreeGraphTest(TransportType.CAR);
    }

    @Test
    public void testTreeGraphBike() {
        generateTreeGraphTest(TransportType.BIKE);
    }

    @Test
    public void testCycleGraphFoot() {
        generateCycleGraphTest(TransportType.FOOT);
    }

    @Test
    public void testCycleGraphCar() {
        generateCycleGraphTest(TransportType.CAR);
    }

    @Test
    public void testCycleGraphBike() {
        generateCycleGraphTest(TransportType.BIKE);
    }

    @Test
    public void testWaitsOnCrossroadsFoot() {
        generateCrossroadsTest(TransportType.FOOT);
    }

    @Test
    public void testWaitsOnCrossroadsCar() {
        generateCrossroadsTest(TransportType.CAR);
    }

    @Test
    public void testWaitsOnCrossroadsBike() {
        generateCrossroadsTest(TransportType.BIKE);
    }

    /** Generates binary tree graph tests with unreachable leaves */
    private static void generateTreeGraphTest(TransportType transportType) {
        Node root = generateTree(transportType.getAverageSpeed(), depthInTreeTest);
        List<Node> reachableNodes = ReachableNodesFinder.getReachableNodes(
                root,  depthInTreeTest - 1.5, transportType);

        assertEquals(numberOfInnerNodesInTree, reachableNodes.size());
        assertOnlyLeavesAreUnreachable(reachableNodes, root);
    }

    private static Node generateTree(double distanceToSons, int depth) {
        Node currentNode = new Node(dummyCoordinate, false);
        if (depth <= 1) return currentNode;

        for (int i = 0; i < 2; i++) {
            Node son = generateTree(distanceToSons, depth - 1);
            currentNode.edges.add(new Edge(distanceToSons, son));
        }
        return currentNode;
    }

    private static void assertOnlyLeavesAreUnreachable(List<Node> nodes, Node root) {
        if (root.edges.size() == 0) {
            assertFalse(nodes.contains(root));
            return;
        }

        assertTrue(nodes.contains(root));
        for (Edge edgeToSon : root.edges) {
            assertOnlyLeavesAreUnreachable(nodes, edgeToSon.destination);
        }
    }

    private static void generateCycleGraphTest(TransportType transportType) {
        List<Node> graphNodes = new ArrayList<>();
        for (int i = 0; i < numberOfNodesInCycleTest; i++) {
            graphNodes.add(new Node(dummyCoordinate, false));
        }
        for (int i = 0; i < numberOfNodesInCycleTest; i++) {
            int previousNodeNumber = (i - 1 + numberOfNodesInCycleTest) % numberOfNodesInCycleTest;
            makeBidirectionalEdge(graphNodes.get(i), graphNodes.get(previousNodeNumber),
                                  transportType.getAverageSpeed());
        }
        List<Node> reachableNodes = ReachableNodesFinder.getReachableNodes(
                graphNodes.get(0), numberOfNodesInCycleTest / 2.0 - 0.5, transportType);

        assertEquals(numberOfNodesInCycleTest - 1, reachableNodes.size());
        assertFalse(reachableNodes.contains(graphNodes.get(numberOfNodesInCycleTest / 2)));

        graphNodes.remove(numberOfNodesInCycleTest / 2);
        assertTrue(reachableNodes.containsAll(graphNodes));
    }

    private static void makeBidirectionalEdge(Node first, Node second, double distance) {
        first.edges.add(new Edge(distance, second));
        second.edges.add(new Edge(distance, first));
    }

    private static void generateCrossroadsTest(TransportType transportType) {
        Node firstNode = new Node(dummyCoordinate, true);
        Node secondNode = new Node(dummyCoordinate, true);
        Node thirdNode = new Node(dummyCoordinate, true);

        makeBidirectionalEdge(firstNode, secondNode, 0);
        makeBidirectionalEdge(secondNode, thirdNode, 0);

        List<Node> reachableNodes = ReachableNodesFinder.getReachableNodes(
                firstNode, IsochroneBuilder.EXPECTED_CROSSROADS_WAITING * 1.5, transportType);
        assertTrue(reachableNodes.contains(firstNode));
        assertTrue(reachableNodes.contains(secondNode));
        assertFalse(reachableNodes.contains(thirdNode));
    }
}