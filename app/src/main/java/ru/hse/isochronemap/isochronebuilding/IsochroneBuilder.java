package ru.hse.isochronemap.isochronebuilding;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.Edge;
import ru.hse.isochronemap.mapstructure.MapStructureManager;
import ru.hse.isochronemap.mapstructure.MapStructureRequest;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.mapstructure.TransportType;

/** This class contains methods for building isochrones. */
public class IsochroneBuilder {
    public static final double UNCONDITIONAL_ACCESS_DISTANCE = 0.1;

    static final double EXPECTED_CROSSROADS_WAITING = 0.005;

    private static final double HEXAGON_RADIUS = 0.05; // 50 meters
    private static final double IGNORED_HOLES_AREA_MULTIPLIER = 1.2;

    /**
     * Builds polygon which represents reachable area. This method calls {@link MapStructureManager}
     * to download the map segment which is needed to calculate reachable area.
     *
     * @param startCoordinate start point.
     * @param time            time in hours.
     * @param transportType   type of transport.
     * @param requestType     Convex Hull or Hexagonal cover.
     * @return Resulting polygons.
     * @throws IOException             if error during OSM map downloading occurred.
     * @throws NotEnoughNodesException if no reachable OSM nodes were found nearby.
     */
    public static @NonNull List<IsochronePolygon> getIsochronePolygons(
            @NonNull Coordinate startCoordinate,
            double time,
            @NonNull TransportType transportType,
            @NonNull IsochroneRequestType requestType)
            throws IOException, NotEnoughNodesException {

        double maxDistance = time * transportType.getAverageSpeed();
        MapStructureRequest request =
                new MapStructureRequest(startCoordinate, UNCONDITIONAL_ACCESS_DISTANCE, maxDistance,
                                        transportType);
        Node startNode = MapStructureManager.getMapStructure(request);
        return getIsochronePolygons(startNode, time, transportType, requestType);
    }

    /**
     * Builds polygon which represents reachable area. This method uses pre-downloaded MapStructure.
     *
     * @param startNode     start node of pre-downloaded MapStructure.
     * @param time          time in hours.
     * @param transportType type of transport.
     * @param requestType   Convex Hull or Hexagonal cover.
     * @return Resulting polygons.
     * @throws NotEnoughNodesException if no reachable OSM nodes were found nearby.
     */
    public static List<IsochronePolygon> getIsochronePolygons(
            @NonNull Node startNode,
            double time,
            @NonNull TransportType transportType,
            @NonNull IsochroneRequestType requestType)
            throws NotEnoughNodesException{

        List<Node> reachableNodes =
                ReachableNodesFinder.getReachableNodes(startNode, time, transportType);
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
                densifyPointsOnEdges(reachableNodes, reachablePoints);
                HexagonalCoverBuilder builder =
                        new HexagonalCoverBuilder(HEXAGON_RADIUS, startNode.coordinate);
                List<Hexagon> hexagons = builder.getHexagonalCover(reachablePoints);
                return turnHexagonsToPolygons(hexagons,
                                              hexagons.get(0).toJTSPolygon().getArea()
                                              * IGNORED_HOLES_AREA_MULTIPLIER);
            default:
                throw new RuntimeException();
        }
    }

    private static void densifyPointsOnEdges(@NonNull List<Node> reachableNodes,
                                             @NonNull List<Coordinate> reachablePoints) {
        Set<Node> reachableNodesSet = new HashSet<>(reachableNodes);
        for (Node node : reachableNodes) {
            for (Edge edge : node.edges) {
                if (!reachableNodesSet.contains(edge.destination)) {
                    continue;
                }
                int numberOfSegments = (int) (edge.length / (HEXAGON_RADIUS * 0.5)) + 1;
                double latitudeStep =
                        (edge.destination.coordinate.latitude
                         - node.coordinate.latitude)
                        / numberOfSegments;
                double longitudeStep =
                        (edge.destination.coordinate.longitude
                         - node.coordinate.longitude)
                        / numberOfSegments;
                for (int i = 1; i < numberOfSegments; i++) {
                    double latitude = node.coordinate.latitude + latitudeStep * i;
                    double longitude = node.coordinate.longitude + longitudeStep * i;
                    reachablePoints.add(new Coordinate(latitude, longitude));
                }
            }
        }
    }

    private static @NonNull List<Coordinate> getCoordinates(@NonNull Collection<Node> nodes) {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        for (Node currentNode : nodes) {
            coordinates.add(currentNode.coordinate);
        }
        return coordinates;
    }

    private static @NonNull List<IsochronePolygon> turnHexagonsToPolygons(
            @NonNull Collection<Hexagon> hexagons, double ignoredHolesArea) {
        List<Polygon> polygons = new ArrayList<>();
        for (Hexagon hexagon : hexagons) {
            polygons.add(hexagon.toJTSPolygon());
        }

        List<Geometry> unitedGeometries = getGeometriesConcurrently(polygons);
        unitedGeometries = new ArrayList<>(unitedGeometries); // for remove operation
        unitedGeometries.removeAll(Collections.singletonList(null));
        Geometry geometry = UnaryUnionOp.union(unitedGeometries);

        polygons.clear();
        PolygonExtracter.getPolygons(geometry, polygons);
        List<IsochronePolygon> isochronePolygonList = new ArrayList<>();
        for (Polygon JTSPolygon : polygons) {
            isochronePolygonList.add(new IsochronePolygon(JTSPolygon, ignoredHolesArea));
        }

        return isochronePolygonList;
    }

    private static @NonNull List<Geometry> getGeometriesConcurrently(
            @NonNull List<Polygon> polygons) {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[numberOfCores];
        Geometry[] geometries = new Geometry[numberOfCores];
        int chunkSize = (polygons.size() + numberOfCores - 1) / numberOfCores;

        for (int i = 0; i < numberOfCores; ++i) {
            int currentThread = i;
            int leftIndex = Math.min(currentThread * chunkSize, polygons.size());
            int rightIndex = Math.min((currentThread + 1) * chunkSize, polygons.size());
            List<Polygon> currentThreadSublist =
                    Collections.unmodifiableList(polygons.subList(leftIndex, rightIndex));

            threads[i] = new Thread(
                    () -> geometries[currentThread] = UnaryUnionOp.union(currentThreadSublist));
            threads[i].start();
        }

        for (int i = 0; i < numberOfCores; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }

        return Arrays.asList(geometries);
    }
}
