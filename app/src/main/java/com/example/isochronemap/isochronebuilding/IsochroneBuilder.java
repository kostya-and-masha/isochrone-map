package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.Edge;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.Node;
import com.example.isochronemap.mapstructure.TransportType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import androidx.annotation.NonNull;

/** This class contains methods for building isochrones. */
public class IsochroneBuilder {
    private static final double AVERAGE_FOOT_SPEED = 5; // in km/h

    // TODO calculate more realistic value
    private static final double EXPECTED_CROSSWALK_FOOT_WAITING = 0.005;

    /** Builds polygon which represents reachable area **/
    public static @NonNull List<Coordinate> getIsochronePolygon(
            @NonNull MapStructure map, double time, @NonNull TransportType transportType) {
        List<Node> reachableNodes = getReachableNodes(map, time, transportType);
        List<Coordinate> reachablePoints = getCoordinates(reachableNodes);
        return getNodesConvexHull(reachablePoints);
    }

    /** Dijkstra algorithm */
    private static @NonNull List<Node> getReachableNodes(@NonNull MapStructure map, double time,
                                                         TransportType transportType){
        if (transportType != TransportType.FOOT) {
            throw new UnsupportedOperationException("the support of this transport " +
                    "type is not implemented yet :(");
        }

        double speed;
        switch (transportType) {
            case FOOT:
                speed = AVERAGE_FOOT_SPEED;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        HashMap<Node, Double> nodesReachTime = new HashMap<>();

        class TimeAndNode {
            private double time;
            private Node node;

            private TimeAndNode(double time, Node node) {
                this.time = time;
                this.node = node;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                TimeAndNode that = (TimeAndNode) o;
                return Double.compare(that.time, time) == 0 &&
                       node == that.node;
            }

            @Override
            public int hashCode() {
                return Objects.hash(time, node);
            }
        }

        // used for nodes order in TreeSet TODO fix it
        int currentIndex = 0;
        HashMap<Node, Integer> nodesIndex = new HashMap<>();

        // PriorityQueue has worse remove complexity
        TreeSet<TimeAndNode> nodesQueue = new TreeSet<>((o1, o2) -> {
            int comparisonResult = Double.compare(o1.time, o2.time);
            if (comparisonResult == 0) {
                Integer o1NodeIndex = nodesIndex.get(o1.node);
                Integer o2NodeIndex = nodesIndex.get(o2.node);
                assert o1NodeIndex != null && o2NodeIndex != null;
                return Integer.compare(o1NodeIndex, o2NodeIndex);
            }
            return comparisonResult;
        });

        for (Node node : map.getStartNodes()) {
            nodesReachTime.put(node, 0.0);
            nodesQueue.add(new TimeAndNode(0.0, node));
            nodesIndex.put(node, currentIndex++);
        }

        while (!nodesQueue.isEmpty()) {
            Node currentNode = nodesQueue.first().node;
            double currentReachTime = nodesQueue.first().time;
            nodesQueue.remove(nodesQueue.first());

            for (Edge edge : currentNode.edges) {
                Node destination = edge.destination;
                double destinationTime = currentReachTime + edge.length / speed;

                // TODO another transport type support
                if (edge.isCrossing) {
                    destinationTime += EXPECTED_CROSSWALK_FOOT_WAITING;
                }

                if (!nodesReachTime.containsKey(destination)) {
                    nodesReachTime.put(destination, destinationTime);
                    nodesQueue.add(new TimeAndNode(destinationTime, destination));
                    nodesIndex.put(destination, currentIndex++);
                } else if (nodesReachTime.get(destination) > destinationTime) {
                    nodesQueue.remove(
                            new TimeAndNode(nodesReachTime.get(destination), destination));
                    nodesReachTime.put(destination, destinationTime);
                    nodesQueue.add(new TimeAndNode(destinationTime, destination));
                }
            }
        }

        ArrayList<Node> reachableNodes = new ArrayList<>();
        for (Map.Entry<Node, Double> entry : nodesReachTime.entrySet()) {
            if (entry.getValue() <= time) {
                reachableNodes.add(entry.getKey());
            }
        }
        return reachableNodes;
    }

    private static @NonNull List<Coordinate> getCoordinates(@NonNull List<Node> nodes) {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        for (Node node : nodes) {
            coordinates.add(node.coordinate);
        }
        return coordinates;
    }
    private static @NonNull List<Coordinate> getNodesConvexHull(
            @NonNull List<Coordinate> coordinates) {
        
    }
}
