package ru.hse.isochronemap.mapstructure;

import androidx.annotation.NonNull;

/** Directed edge between map nodes **/
public class Edge {
    /** Length in kilometers. **/
    public final double length;
    /** Destination node. **/
    public final Node destination;

    /**
     * Constructs new edge.
     * @param length edge length in kilometers
     * @param destination edge destination node
     */
    public Edge(double length, @NonNull Node destination) {
        this.length = length;
        this.destination = destination;
    }
}
