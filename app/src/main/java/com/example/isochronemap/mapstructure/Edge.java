package com.example.isochronemap.mapstructure;

/** Directed edge between map nodes **/
public class Edge {
    /** Length in kilometers. **/
    public final double length;
    /** Destination node. **/
    public final Node destination;

    public Edge(Coordinate sourceCoordinate, Node destination) {
        length = sourceCoordinate.distanceTo(destination.coordinate);
        this.destination = destination;
    }
}
