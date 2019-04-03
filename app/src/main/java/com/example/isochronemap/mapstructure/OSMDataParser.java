package com.example.isochronemap.mapstructure;

import android.annotation.SuppressLint;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.DecodingMode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OSMDataParser {
    static {
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
    }

    static MapStructure parse(@NotNull MapStructureRequest request,
                              @NotNull byte[] osmData) throws IOException {
        Coordinate startCoordinate = request.getStartCoordinate();
        double unconditionalAccessDistance = request.getUnconditionalAccessDistance();
        double maximumDistance = request.getMaximumDistance();
        List<Node> nodes = new ArrayList<>();
        List<Node> startNodes = new ArrayList<>();

        @SuppressLint("UseSparseArrays")
        Map<Long, Node> nodeMap = new HashMap<>();

        JsonIterator iterator = JsonIterator.parse(osmData);
        for (String e = iterator.readObject(); e != null; e = iterator.readObject()) {
            switch (e) {
                case "elements":
                    while (iterator.readArray()) {
                        long id = 0;
                        String type = "";
                        double latitude = 0;
                        double longitude = 0;
                        String highway = "";
                        String oneway = "";
                        long[] nodeIds = new long[0];

                        for (String f = iterator.readObject();
                             f != null;
                             f = iterator.readObject()) {
                            switch (f) {
                                case "id":
                                    id = iterator.readLong();
                                    continue;
                                case "type":
                                    type = iterator.readString();
                                    continue;
                                case "lat":
                                    latitude = iterator.readDouble();
                                    continue;
                                case "lon":
                                    longitude = iterator.readDouble();
                                    continue;
                                case "nodes":
                                    nodeIds = iterator.read(long[].class);
                                    continue;
                                case "tags":
                                    for (String t = iterator.readObject();
                                         t != null;
                                         t = iterator.readObject()) {
                                        switch (t) {
                                            case "highway":
                                                highway = iterator.readString();
                                                continue;
                                            case "oneway":
                                                oneway = iterator.readString();
                                                continue;
                                            default:
                                                iterator.skip();
                                        }
                                    }
                                    continue;
                                default:
                                    iterator.skip();
                            }
                        }

                        if (type.equals("node")) {
                            Coordinate coordinate = new Coordinate(latitude, longitude);
                            double distanceFromStart = startCoordinate.distanceTo(coordinate);
                            if (distanceFromStart > maximumDistance) {
                                continue;
                            }

                            boolean isCrossing = highway.equals("crossing");
                            Node node = new Node(coordinate, isCrossing);
                            nodes.add(node);
                            nodeMap.put(id, node);
                            if (distanceFromStart < unconditionalAccessDistance) {
                                startNodes.add(node);
                            }
                        } else if (type.equals("way")) {
                            boolean forward = false;
                            boolean backward = false;
                            if (request.getTransportType() == TransportType.FOOT) {
                                forward = true;
                                backward = true;
                            } else {
                                if (!oneway.matches("-1|reverse")) {
                                    forward = true;
                                }
                                if (!oneway.matches("yes|true|1")) {
                                    backward = true;
                                }
                            }
                            Node from;
                            Node to = nodeMap.get(nodeIds[0]);
                            for (int i = 1; i < nodeIds.length; i++) {
                                from = to;
                                to = nodeMap.get(nodeIds[i]);
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
                    continue;
                default:
                    iterator.skip();
            }
        }

        return new MapStructure(nodes, startNodes);
    }
}
