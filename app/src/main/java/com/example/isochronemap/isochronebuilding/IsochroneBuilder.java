package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import com.example.isochronemap.mapstructure.Edge;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.MapStructureManager;
import com.example.isochronemap.mapstructure.MapStructureRequest;
import com.example.isochronemap.mapstructure.Node;
import com.example.isochronemap.mapstructure.TransportType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



/** This class contains methods for building isochrones. */
public class IsochroneBuilder {
    static final double AVERAGE_FOOT_SPEED = 5; // in km/h
    static final double AVERAGE_CAR_SPEED = 35;
    static final double AVERAGE_BIKE_SPEED = 18;

    // TODO calculate more realistic value
    static final double EXPECTED_CROSSROADS_WAITING = 0.005;

    /**
     * Builds polygon which represents reachable area. This method calls {@link MapStructureManager}
     * to download the map segment which is needed to calculate reachable area.
     */
    public static @NotNull List<Coordinate> getIsochronePolygon(
            @NotNull Coordinate startCoordinate, double time, @NotNull TransportType transportType)
            throws IOException, UnsupportedParameterException, NotEnoughNodesException {
        double maxDistance = 0;
        switch (transportType) {
            case FOOT:
                maxDistance = time * AVERAGE_FOOT_SPEED + 0.2;
                break;
            case CAR:
                maxDistance = time * AVERAGE_CAR_SPEED + 0.2;
                break;
            case BIKE:
                maxDistance = time * AVERAGE_BIKE_SPEED + 0.2;
                break;
        }
        MapStructureRequest request = new MapStructureRequest(
                startCoordinate, 0.1, maxDistance, transportType);
        MapStructure mapStructure = MapStructureManager.getMapStructure(request);

        Node startNode = new Node(startCoordinate, false);
        for (Node destinationNode: mapStructure.getStartNodes()) {
            startNode.edges.add(new Edge(startCoordinate, destinationNode));
        }
        mapStructure = new MapStructure(mapStructure.getNodes(),
                Collections.singletonList(startNode));
        return getIsochronePolygon(mapStructure, time, transportType);
    }

    /**
     * Builds polygon which represents reachable area.
     * This method uses pre-downloaded MapStructure.
     */
    public static @NotNull List<Coordinate> getIsochronePolygon(
            @NotNull MapStructure map, double time, @NotNull TransportType transportType)
            throws NotEnoughNodesException, UnsupportedParameterException {
        List<Node> reachableNodes = ReachableNodesFinder.getReachableNodes(map, time, transportType);
        if (reachableNodes.size() < 2) {
            throw new NotEnoughNodesException();
        }
        List<Coordinate> reachablePoints = getCoordinates(reachableNodes);
        return ConvexHullBuilder.getPointsConvexHull(reachablePoints);
    }

    private static @NotNull List<Coordinate> getCoordinates(@NotNull List<Node> nodes) {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        for (Node currentNode : nodes) {
            coordinates.add(currentNode.coordinate);
        }
        return coordinates;
    }
}
