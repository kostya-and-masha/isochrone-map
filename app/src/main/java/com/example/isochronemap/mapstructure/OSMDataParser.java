package com.example.isochronemap.mapstructure;

import android.annotation.SuppressLint;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OSMDataParser {
    private static final Gson gson = new Gson();

    static MapStructure parse(@NotNull MapStructureRequest request,
                              @NotNull InputStream osmData) {
        Coordinate startCoordinate = request.getStartCoordinate();
        double unconditionalAccessDistance = request.getUnconditionalAccessDistance();
        double maximumDistance = request.getMaximumDistance();
        List<Node> nodes = new ArrayList<>();
        List<Node> startNodes = new ArrayList<>();

        @SuppressLint("UseSparseArrays")
        Map<Long, Node> nodeMap = new HashMap<>();

        EntityCollection entities = gson.fromJson(
                new InputStreamReader(osmData),
                EntityCollection.class
        );

        // TODO split into smaller functions
        for (Entity e : entities.elements) {
            if (e.type.equals("node")) {
                Coordinate coordinate = new Coordinate(e.lat, e.lon);
                double distanceFromStart = startCoordinate.distanceTo(coordinate);
                if (distanceFromStart > maximumDistance) {
                    continue;
                }

                boolean isCrossing = e.highway.equals("crossing");
                Node node = new Node(coordinate, isCrossing);
                nodes.add(node);
                nodeMap.put(e.id, node);
                if (distanceFromStart < unconditionalAccessDistance) {
                    startNodes.add(node);
                }
            } else if (e.type.equals("way")) {
                boolean forward = false;
                boolean backward = false;
                if (request.getTransportType() == TransportType.FOOT) {
                    forward = true;
                    backward = true;
                } else {
                    if (!e.oneway.matches("-1|reverse")) {
                        forward = true;
                    }
                    if (!e.oneway.matches("yes|true|1")) {
                        backward = true;
                    }
                }
                ArrayList<Long> nodeIds = e.nodes;
                Node from;
                Node to = nodeMap.get(nodeIds.get(0));
                for (int i = 1; i < nodeIds.size(); i++) {
                    from = to;
                    to = nodeMap.get(nodeIds.get(i));
                    if (from == null || to == null) {
                        continue;
                    }
                    if (forward) {
                        from.edges.add(new Edge(from.coordinate, to));
                    }
                    if (backward) {
                        to.edges.add(new Edge(to.coordinate, from));
                    }
                }
            }
        }

        return new MapStructure(nodes, startNodes);
    }

    static class Entity {
        long id;
        String type;
        double lat;
        double lon;
        String highway = "";
        String oneway = "";
        ArrayList<Long> nodes;
    }

    static class EntityCollection {
        ArrayList<Entity> elements;
    }
}
