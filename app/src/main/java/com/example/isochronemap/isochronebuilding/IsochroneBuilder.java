package com.example.isochronemap.isochronebuilding;

import com.example.isochronemap.mapstructure.Coordinate;

import com.example.isochronemap.mapstructure.Edge;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.MapStructureManager;
import com.example.isochronemap.mapstructure.MapStructureRequest;
import com.example.isochronemap.mapstructure.Node;
import com.example.isochronemap.mapstructure.TransportType;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
    public static @NotNull List<IsochronePolygon> getIsochronePolygons(
            @NotNull Coordinate startCoordinate, double time,
            @NotNull TransportType transportType, @NotNull IsochroneRequestType requestType)
            throws IOException, UnsupportedParameterException, NotEnoughNodesException {
        double maxDistance = 0.0;
        //TODO move constants to enum
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
        return getIsochronePolygons(mapStructure, time, transportType, requestType);
    }

    /**
     * Builds polygon which represents reachable area.
     * This method uses pre-downloaded MapStructure.
     */
    public static @NotNull List<IsochronePolygon> getIsochronePolygons(
            @NotNull MapStructure map, double time,
            @NotNull TransportType transportType, @NotNull IsochroneRequestType requestType)
            throws NotEnoughNodesException, UnsupportedParameterException {
        List<Node> reachableNodes = ReachableNodesFinder.getReachableNodes(map, time, transportType);
        if (reachableNodes.size() < 2) {
            throw new NotEnoughNodesException();
        }
        List<Coordinate> reachablePoints = getCoordinates(reachableNodes);
        switch (requestType) {
            case CONVEX_HULL:
                IsochronePolygon polygon = new IsochronePolygon();
                polygon.setExteriorRing(ConvexHullBuilder.getPointsConvexHull(reachablePoints));
                return Collections.singletonList(polygon);
            case HEXAGONAL_COVER:
                //TODO IMPROVE PERFORMANCE

                Set<Node> reachableNodesSet = new HashSet<>(reachableNodes);
                for (Node node : reachableNodes) {
                    for (Edge edge : node.edges) {
                        if (!reachableNodesSet.contains(edge.destination)) {
                            continue;
                        }
                        int numberOfSegments = (int) (edge.length /
                                (HexagonalCoverBuilder.HEXAGON_RADIUS * 0.5)) + 1;
                        double latitudeStep = (edge.destination.coordinate.latitudeDeg
                                - node.coordinate.latitudeDeg) / numberOfSegments;
                        double longitutdeStep = (edge.destination.coordinate.longitudeDeg
                                - node.coordinate.longitudeDeg) / numberOfSegments;
                        for (int i = 1; i < numberOfSegments; i++) {
                            reachablePoints.add(new Coordinate(
                                    node.coordinate.latitudeDeg + latitudeStep * i,
                                    node.coordinate.longitudeDeg + longitutdeStep * i
                            ));
                        }
                    }
                }

                Set<Hexagon> hexagons = HexagonalCoverBuilder.getHexagonalCover(reachablePoints);
                return turnHexagonsToPolygons(hexagons);
                //FIXME
            default:
                throw new RuntimeException();
        }
    }

    private static @NotNull List<Coordinate> getCoordinates(@NotNull Collection<Node> nodes) {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        for (Node currentNode : nodes) {
            coordinates.add(currentNode.coordinate);
        }
        return coordinates;
    }

    private static @NotNull List<IsochronePolygon> turnHexagonsToPolygons(
            Collection<Hexagon> hexagons) {
        List<Polygon> polygons = new ArrayList<>();
        for (Hexagon hexagon : hexagons) {
            polygons.add(hexagon.toJTSPolygon());
        }

        Geometry geometry = UnaryUnionOp.union(polygons);
        polygons.clear();
        PolygonExtracter.getPolygons(geometry, polygons);
        List<IsochronePolygon> isochronePolygonList = new ArrayList<>();
        for (Polygon JTSPolygon : polygons) {
            isochronePolygonList.add(new IsochronePolygon(JTSPolygon));
        }
        return isochronePolygonList;
    }
}
