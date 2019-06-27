package ru.hse.isochronemap.mapstructure;

import com.google.android.gms.common.util.IOUtils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import androidx.annotation.NonNull;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongObjectMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OSMDataParserTest {
    private static final String SMALL_TEST_CSV_FILE = "small_test_osm.csv";
    private static final String SMALL_TEST_JSON_FILE = "small_test_osm.json";
    private static final String SMALL_TEST_GRAPH_FILE = "small_test_graph";
    private static final String TEST_CSV_FILE = "test_osm.csv";
    private static final String TEST_JSON_FILE = "test_osm.json";
    private static final String TEST_NODES_FILE = "nodes";
    private static final String TEST_WAYS_FILE = "ways";
    private static final String TEST_WAY_NODES_FILE = "way_nodes";
    private static final String TEST_NODE_DEGREES_FILE = "node_degrees";
    private static final TransportType TEST_TRANSPORT = TransportType.CAR;

    @Test
    public void testSimpleMap() throws IOException {
        MapData map = OSMDataParser.parseCSV(fileToByteArray(SMALL_TEST_CSV_FILE), TEST_TRANSPORT);
        TLongObjectMap<TLongList> ways =
                OSMDataParser.parseJSON(fileToByteArray(SMALL_TEST_JSON_FILE));
        OSMDataParser.connectNodes(map, ways);

        TLongObjectMap<Node> nodesMap = map.nodesMap;

        assertEquals(6, nodesMap.size());
        for (long id : nodesMap.keys()) {
            Node node = nodesMap.get(id);

            assertTrue(1 <= id && id <= 6);
            assertEquals(id, (long) node.coordinate.latitude);
            assertEquals(id, (long) node.coordinate.longitude);
            assertEquals(id == 4, node.isCrossing);
        }

        final Map<Long, long[]> graph = readGraph(new Scanner(
                getClass().getResourceAsStream(SMALL_TEST_GRAPH_FILE)));
        checkGraph(graph, map.nodesMap);
    }

    @Test
    public void testParseCSV() throws IOException {
        MapData result = OSMDataParser.parseCSV(fileToByteArray(TEST_CSV_FILE), TEST_TRANSPORT);
        assertArrayEquals(fileToByteArray(TEST_NODES_FILE), nodesMapToByteArray(result.nodesMap));
        assertArrayEquals(fileToByteArray(TEST_WAYS_FILE), waysMapToByteArray(result.waysMap));
    }

    @Test
    public void testParseJSON() throws IOException {
        TLongObjectMap<TLongList> result = OSMDataParser.parseJSON(fileToByteArray(TEST_JSON_FILE));
        assertArrayEquals(fileToByteArray(TEST_WAY_NODES_FILE), wayNodesToByteArray(result));
    }

    @Test
    public void testConnectNodes() throws IOException {
        MapData map = OSMDataParser.parseCSV(fileToByteArray(TEST_CSV_FILE), TEST_TRANSPORT);
        TLongObjectMap<TLongList> ways = OSMDataParser.parseJSON(fileToByteArray(TEST_JSON_FILE));
        OSMDataParser.connectNodes(map, ways);
        assertArrayEquals(fileToByteArray(TEST_NODE_DEGREES_FILE),
                          nodeDegreesToByteArray(map.nodesMap));
    }

    private Map<Long, long[]> readGraph(Scanner scanner) {
        Map<Long, long[]> graph = new HashMap<>();
        int numberOfVertices = scanner.nextInt();
        for (int i = 0; i < numberOfVertices; i++) {
            long id = scanner.nextLong();
            int numberOfNeighbours = scanner.nextInt();
            long[] neighbours = new long[numberOfNeighbours];
            for (int j = 0; j < numberOfNeighbours; j++) {
                neighbours[j] = scanner.nextLong();
            }
            graph.put(id, neighbours);
        }
        return graph;
    }

    private void checkGraph(Map<Long, long[]> graph, TLongObjectMap<Node> nodesMap) {
        for (long id : nodesMap.keys()) {
            Node node = nodesMap.get(id);
            long[] neighboursExpected = Objects.requireNonNull(graph.get(id));
            List<Edge> edges = node.edges;
            assertEquals(neighboursExpected.length, edges.size());

            for (long neighbourId : neighboursExpected) {
                Node neighbour = nodesMap.get(neighbourId);
                boolean isPresented = false;
                for (Edge edge : edges) {
                    if (neighbour == edge.destination) {
                        isPresented = true;
                        break;
                    }
                }
                assertTrue(isPresented);
            }
        }
    }

    private byte[] fileToByteArray(@NonNull String fileName) throws IOException {
        InputStream in = getClass().getResourceAsStream(fileName);
        return IOUtils.toByteArray(Objects.requireNonNull(in));
    }

    private byte[] nodesMapToByteArray(TLongObjectMap<Node> nodesMap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        long[] keys = nodesMap.keys();
        Arrays.sort(keys);
        for (long id : keys) {
            Node node = nodesMap.get(id);
            printStream.print(id);
            printStream.print(' ');
            printStream.print(node.coordinate.latitude);
            printStream.print(' ');
            printStream.print(node.coordinate.longitude);
            printStream.print(' ');
            printStream.print(node.isCrossing);
            printStream.print('\n');
        }

        return out.toByteArray();
    }

    private byte[] waysMapToByteArray(TLongCharMap waysMap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        long[] keys = waysMap.keys();
        Arrays.sort(keys);
        for (long id : keys) {
            char c = waysMap.get(id);
            printStream.print(id);
            printStream.print(' ');
            printStream.print(c);
            printStream.print('\n');
        }

        return out.toByteArray();
    }

    private byte[] wayNodesToByteArray(TLongObjectMap<TLongList> wayNodes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        long[] keys = wayNodes.keys();
        Arrays.sort(keys);
        for (long id : keys) {
            TLongList nodeList = wayNodes.get(id);
            printStream.print(id);
            printStream.print(':');
            for (long nodeId : nodeList.toArray()) {
                printStream.print(' ');
                printStream.print(nodeId);
            }
            printStream.print('\n');
        }

        return out.toByteArray();
    }

    private byte[] nodeDegreesToByteArray(TLongObjectMap<Node> nodesMap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        long[] keys = nodesMap.keys();
        Arrays.sort(keys);
        for (long id : keys) {
            int degree = nodesMap.get(id).edges.size();
            printStream.print(id);
            printStream.print(' ');
            printStream.print(degree);
            printStream.print('\n');
        }

        return out.toByteArray();
    }
}