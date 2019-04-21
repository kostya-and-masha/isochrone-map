package com.example.isochronemap.mapstructure;

import java.util.ArrayList;
import java.util.List;

/** Map node class. **/
public class Node {
    /** Geographical coordinate of node. **/
    public final Coordinate coordinate;
    /** Crosswalk/crossroads flag. **/
    public final boolean isCrossing;
    /** List of outgoing edges. **/
    public List<Edge> edges = new ArrayList<>();

    public Node(Coordinate coordinate, boolean isCrossing) {
        this.coordinate = coordinate;
        this.isCrossing = isCrossing;
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }
}
