package ru.hse.isochronemap.mapstructure;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/** This class represents map node. **/
public class Node {
    /** Geographical coordinate of node. **/
    public final Coordinate coordinate;
    /** Crosswalk/crossroads flag. **/
    public final boolean isCrossing;
    /** List of outgoing edges. **/
    public final List<Edge> edges = new ArrayList<>();

    /**
     * Constructs new map node.
     * @param coordinate node location
     * @param isCrossing crosswalk/crossroads flag
     */
    public Node(@NonNull Coordinate coordinate, boolean isCrossing) {
        this.coordinate = coordinate;
        this.isCrossing = isCrossing;
    }

    /** Adds outgoing edge. **/
    void addEdge(@NonNull Edge edge) {
        edges.add(edge);
    }
}
