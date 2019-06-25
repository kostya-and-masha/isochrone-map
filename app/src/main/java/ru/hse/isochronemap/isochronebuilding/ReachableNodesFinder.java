package ru.hse.isochronemap.isochronebuilding;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Edge;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.mapstructure.TransportType;

/**
 * This class provides static method which finds
 * reachable (in a specified time) nodes in a graph.
 */
class ReachableNodesFinder {
    /**
     * Dijkstra algorithm
     *
     * @param startNode     graph's start point.
     * @param time          upper bound time.
     * @param transportType type of transport.
     * @return list of reachable nodes.
     */
    static @NonNull List<Node> getReachableNodes(@NonNull Node startNode, double time,
                                                 @NonNull TransportType transportType) {
        double speed = transportType.getAverageSpeed();

        HashMap<Node, Double> nodesReachTime = new HashMap<>();

        // used for nodes order in TreeSet
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

        nodesReachTime.put(startNode, 0.0);
        nodesIndex.put(startNode, currentIndex++);
        nodesQueue.add(new TimeAndNode(0.0, startNode));

        while (!nodesQueue.isEmpty()) {
            Node currentNode = nodesQueue.first().node;
            double currentReachTime = nodesQueue.first().time;
            nodesQueue.remove(nodesQueue.first());

            if (currentReachTime > time) {
                continue;
            }

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

        private TimeAndNode(double time, @NonNull Node node) {
            this.time = time;
            this.node = node;
        }

        @Override
        public int hashCode() {
            return Objects.hash(time, node);
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
            return Double.compare(that.time, time) == 0 && node == that.node;
        }
    }
}
