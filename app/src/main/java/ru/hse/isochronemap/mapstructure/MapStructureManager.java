package ru.hse.isochronemap.mapstructure;

import com.google.android.gms.common.util.IOUtils;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import gnu.trove.list.TLongList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongCharHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/** This class downloads map structure information and transforms it into graph. **/
public class MapStructureManager {
    private static final String OVERPASS_API_INTERPRETER =
            "https://overpass.kumi.systems/api/interpreter";
    private static final int verticalCountWays;
    private static final int horizontalCountWays;
    private static final int verticalCountMapData;
    private static final int horizontalCountMapData;

    static {
        final TIntObjectMap<int[]> splitParameters = new TIntObjectHashMap<>();
        splitParameters.put(1, new int[] {1, 1, 1, 1});
        splitParameters.put(2, new int[] {1, 1, 2, 1});
        splitParameters.put(4, new int[] {1, 1, 3, 1});
        splitParameters.put(6, new int[] {2, 1, 2, 2});
        splitParameters.put(8, new int[] {2, 1, 3, 2});
        splitParameters.put(10, new int[] {2, 1, 4, 2});

        final int parallelism = Runtime.getRuntime().availableProcessors();
        int[] values;
        if (splitParameters.containsKey(parallelism)) {
            values = splitParameters.get(parallelism);
        } else {
            values = splitParameters.get(10);
        }
        verticalCountWays = values[0];
        horizontalCountWays = values[1];
        verticalCountMapData = values[2];
        horizontalCountMapData = values[3];
    }

    /**
     * Returns map structure.
     * @param request map request parameters
     * @return start node
     * @throws IOException if failed to connect to server.
     */
    public static @NonNull Node getMapStructure(MapStructureRequest request) throws IOException {
        BoundingBox box = new BoundingBox(
                request.getStartCoordinate(),
                request.getMaximumDistance());
        List<BoundingBox> boxesForWays = splitBoundingBox(
                box, verticalCountWays, horizontalCountWays);
        List<BoundingBox> boxesForMapData = splitBoundingBox(
                box, verticalCountMapData, horizontalCountMapData);

        List<GetMapDataTask> mapDataTasks = new ArrayList<>();
        List<Thread> mapDataThreads = new ArrayList<>();

        for (BoundingBox smallBox : boxesForMapData) {
            GetMapDataTask task = new GetMapDataTask(request.getTransportType(), smallBox);
            Thread thread = new Thread(task);
            thread.start();

            mapDataTasks.add(task);
            mapDataThreads.add(thread);
        }

        List<GetWaysTask> waysTasks = new ArrayList<>();
        List<Thread> waysThreads = new ArrayList<>();

        for (BoundingBox smallBox : boxesForWays) {
            GetWaysTask task = new GetWaysTask(request.getTransportType(), smallBox);
            Thread thread = new Thread(task);
            thread.start();

            waysTasks.add(task);
            waysThreads.add(thread);
        }

        try {
            TLongObjectMap<Node> nodesMap = new TLongObjectHashMap<>();
            TLongCharMap waysMap = new TLongCharHashMap();
            for (int i = 0; i < mapDataThreads.size(); i++) {
                mapDataThreads.get(i).join();
                GetMapDataTask task = mapDataTasks.get(i);
                if (!task.isSuccessful) {
                    throw new IOException();
                }
                nodesMap.putAll(task.mapData.nodesMap);
                waysMap.putAll(task.mapData.waysMap);
            }

            TLongObjectMap<TLongList> ways = new TLongObjectHashMap<>();
            for (int i = 0; i < waysThreads.size(); i++) {
                waysThreads.get(i).join();
                GetWaysTask task = waysTasks.get(i);
                if (!task.isSuccessful) {
                    throw new IOException();
                }
                ways.putAll(task.ways);
            }
            OSMDataParser.connectNodes(new MapData(nodesMap, waysMap), ways);

            double unconditionalAccessDistance = request.getUnconditionalAccessDistance();
            Coordinate startCoordinate = request.getStartCoordinate();
            Node startNode = new Node(startCoordinate, false);
            for (Object o : nodesMap.values()) {
                Node node = (Node) o;
                double distance = startCoordinate.distanceTo(node.coordinate);
                if (distance < unconditionalAccessDistance) {
                    startNode.addEdge(new Edge(distance, node));
                }
            }
            return startNode;
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

    private static byte[] downloadMap(BoundingBox box,
                                    TransportType transportType,
                                    OverpassRequestType requestType)
            throws IOException {
        try {
            URIBuilder uriBuilder = new URIBuilder(OVERPASS_API_INTERPRETER);
            uriBuilder.addParameter(
                    "data",
                    OverpassRequestBuilder.buildRequest(box, transportType, requestType));
            URL url = uriBuilder.build().toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            byte[] result = IOUtils.toByteArray(connection.getInputStream());
            connection.disconnect();
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<BoundingBox> splitBoundingBox(BoundingBox box,
                                                      int verticalCount,
                                                      int horizontalCount) {
        double minimumLatitude = box.minimum.latitude;
        double maximumLatitude = box.maximum.latitude;
        double minimumLongitude = box.minimum.longitude;
        double maximumLongitude = box.maximum.longitude;

        double verticalStep = (maximumLatitude - minimumLatitude) / verticalCount;
        double horizontalStep = (maximumLongitude - minimumLongitude) / horizontalCount;

        List<BoundingBox> result = new ArrayList<>();

        for (int i = 0; i < verticalCount; i++) {
            double currentMinimumLatitude = minimumLatitude + i * verticalStep;
            double currentMaximumLatitude = currentMinimumLatitude + verticalStep;
            for (int j = 0; j < horizontalCount; j++) {
                double currentMinimumLongitude = minimumLongitude + j * horizontalStep;
                double currentMaximumLongitude = currentMinimumLongitude + horizontalStep;
                result.add(new BoundingBox(
                        new Coordinate(currentMinimumLatitude, currentMinimumLongitude),
                        new Coordinate(currentMaximumLatitude, currentMaximumLongitude)
                ));
            }
        }
        return result;
    }

    private static class GetMapDataTask implements Runnable {
        private final TransportType transportType;
        private final BoundingBox box;
        private MapData mapData;
        private boolean isSuccessful = true;

        private GetMapDataTask(TransportType transportType, BoundingBox box) {
            this.transportType = transportType;
            this.box = box;
        }

        @Override
        public void run() {
            byte[] out;
            try {
                out = downloadMap(box, transportType, OverpassRequestType.TAGS_CSV);
                mapData = OSMDataParser.parseCSV(out, transportType);
            } catch (IOException e) {
                isSuccessful = false;
            }
        }
    }

    private static class GetWaysTask implements Runnable {
        private final TransportType transportType;
        private final BoundingBox box;
        private TLongObjectMap<TLongList> ways;
        private boolean isSuccessful = true;

        private GetWaysTask(TransportType transportType, BoundingBox box) {
            this.transportType = transportType;
            this.box = box;
        }

        @Override
        public void run() {
            try {
                byte[] out = downloadMap(box, transportType, OverpassRequestType.WAYS_JSON);
                ways = OSMDataParser.parseJSON(out);
            } catch (IOException e) {
                isSuccessful = false;
            }
        }
    }
}
