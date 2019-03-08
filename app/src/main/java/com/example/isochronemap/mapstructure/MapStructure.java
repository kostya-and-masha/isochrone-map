package com.example.isochronemap.mapstructure;

import java.util.List;

/** This class represents result of map structure request. **/
public class MapStructure {
    private List<Node> nodes;
    private List<Node> startNodes;

    public MapStructure(List<Node> nodes, List<Node> startNodes) {
        this.nodes = nodes;
        this.startNodes = startNodes;
    }

    /** Returns all nodes in specified area. **/
    public List<Node> getNodes() {
        return nodes;
    }

    /** Returns instantly reachable nodes. **/
    public List<Node> getStartNodes() {
        return startNodes;
    }
}
