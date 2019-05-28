package ru.hse.isochronemap.mapstructure;

/** Directed edge between map nodes **/
public class Edge {
    /** Length in kilometers. **/
    public final double length;
    /** Destination node. **/
    public final Node destination;
    /** High-speed major highway flag **/
    public final boolean majorHighway;

    public Edge(double length, Node destination) {
        this.length = length;
        this.destination = destination;
        majorHighway = false;
    }

    public Edge(double length, Node destination, boolean majorHighway) {
        this.length = length;
        this.destination = destination;
        this.majorHighway = majorHighway;
    }
}
