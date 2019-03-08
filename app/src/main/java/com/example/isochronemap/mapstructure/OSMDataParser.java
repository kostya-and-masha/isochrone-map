package com.example.isochronemap.mapstructure;

import android.annotation.SuppressLint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class OSMDataParser {
    private static final JsonParser parser = new JsonParser();

    // TODO distance calculation optimization
    // TODO json -> csv
    static MapStructure parse(@NotNull MapStructureRequest request,
                      @NotNull InputStream osmJsonData) {
        Coordinate startCoordinate = request.getStartCoordinate();
        double unconditionalAccessDistance = request.getUnconditionalAccessDistance();
        List<Node> nodes = new ArrayList<>();
        List<Node> startNodes = new ArrayList<>();

        JsonObject data = parser.parse(new InputStreamReader(osmJsonData)).getAsJsonObject();
        JsonArray elements = data.get("elements").getAsJsonArray();

        @SuppressLint("UseSparseArrays")
        Map<Long, Node> nodeMap = new HashMap<>();

        // TODO split into smaller functions
        for (JsonElement e : elements) {
            JsonObject o = e.getAsJsonObject();
            String type = o.get("type").getAsString();
            if (type.equals("node")) {
                boolean isCrossing = o.has("tags") &&
                        o.get("tags").getAsJsonObject().has("crossing");
                Node node = new Node(
                        new Coordinate(
                                o.get("lat").getAsDouble(),
                                o.get("lon").getAsDouble()
                        ),
                        isCrossing
                );
                nodes.add(node);
                nodeMap.put(o.get("id").getAsLong(), node);

                double distanceFromStart = startCoordinate.distanceTo(node.coordinate);
                if (distanceFromStart < unconditionalAccessDistance) {
                    startNodes.add(node);
                }
            } else if (type.equals("way")) {
                boolean forward = false;
                boolean backward = false;
                if (o.has("tags") &&
                        o.get("tags").getAsJsonObject().has("oneway")) {
                    String oneway = o.get("tags").getAsJsonObject().get("oneway").getAsString();
                    if (!oneway.matches("-1|reverse")) {
                        forward = true;
                    }
                    if (!oneway.matches("yes|true|1")) {
                        backward = true;
                    }
                } else {
                    forward = true;
                    backward = true;
                }
                JsonArray nodeIds = o.get("nodes").getAsJsonArray();
                boolean isCrossing = o.has("tags") &&
                        o.get("tags").getAsJsonObject().has("crossing");
                Node from;
                Node to = Objects.requireNonNull(nodeMap.get(nodeIds.get(0).getAsLong()));
                for (int i = 1; i < nodeIds.size(); i++) {
                    from = to;
                    to = Objects.requireNonNull(nodeMap.get(nodeIds.get(i).getAsLong()));

                    if (forward) {
                        from.edges.add(new Edge(from.coordinate, to, isCrossing));
                    }
                    if (backward) {
                        to.edges.add(new Edge(to.coordinate, from, isCrossing));
                    }
                }
            }
        }
        return new MapStructure(nodes, startNodes);
    }
}
