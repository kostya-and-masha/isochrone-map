package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Edge;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.Node;
import com.example.isochronemap.mapstructure.TransportType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Dijkstra algorithm */
class ReachableNodesFinder {
    static @NotNull List<Node> getReachableNodes(
            @NotNull MapStructure map, double time, TransportType transportType)
            throws UnsupportedParameterException {
        //FIXME move constants to the enum itself
        double speed;
        switch (transportType) {
            case FOOT:
                speed = IsochroneBuilder.AVERAGE_FOOT_SPEED;
                break;
            case CAR:
                speed = IsochroneBuilder.AVERAGE_CAR_SPEED;
                break;
            case BIKE:
                speed = IsochroneBuilder.AVERAGE_BIKE_SPEED;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        HashMap<Node, Double> nodesReachTime = new HashMap<>();

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
            nodesIndex.put(node, currentIndex++);
            nodesQueue.add(new TimeAndNode(0.0, node));
        }

        while (!nodesQueue.isEmpty()) {
            Node currentNode = nodesQueue.first().node;
            double currentReachTime = nodesQueue.first().time;
            nodesQueue.remove(nodesQueue.first());

            for (Edge edge : currentNode.edges) {
                Node destination = edge.destination;
                double destinationTime = currentReachTime + edge.length / speed;

                if (currentNode.isCrossing) {
                    destinationTime += IsochroneBuilder.EXPECTED_CROSSROADS_WAITING;
                }

                if (!nodesReachTime.containsKey(destination)) {
                    nodesReachTime.put(destination, destinationTime);
                    nodesIndex.put(destination, currentIndex++);
                    nodesQueue.add(new TimeAndNode(destinationTime, destination));
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

    private static class TimeAndNode {
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
}
