package com.example.isochronemap.mapstructure;

/** Directed edge between map nodes **/
public class Edge {
    /** Length in kilometers. **/
    public final double length;
    /** Destination node. **/
    public final Node destination;
    /** Crosswalk flag. **/
    public final boolean isCrossing;

    public Edge(Coordinate sourceCoordinate, Node destination, boolean isCrossing) {
        length = sourceCoordinate.distanceTo(destination.coordinate);
        this.destination = destination;
        this.isCrossing = isCrossing;
    }
}
