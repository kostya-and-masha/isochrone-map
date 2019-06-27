package ru.hse.isochronemap.mapstructure;

import com.google.android.gms.common.util.IOUtils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.NonNull;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongObjectMap;

import static org.junit.Assert.assertArrayEquals;

public class OSMDataParserTest {
    private static final String TEST_CSV_FILE = "test_osm.csv";
    private static final String TEST_JSON_FILE = "test_osm.json";
    private static final String TEST_NODES_FILE = "nodes";
    private static final String TEST_WAYS_FILE = "ways";
    private static final String TEST_WAY_NODES_FILE = "way_nodes";
    private static final String TEST_NODE_DEGREES_FILE = "node_degrees";
    private static final TransportType TEST_TRANSPORT = TransportType.CAR;

    @Test
    public void testParseCSV() throws IOException {
        byte[] csvData = fileToByteArray(TEST_CSV_FILE);
        MapData result = OSMDataParser.parseCSV(csvData, TEST_TRANSPORT);
        assertArrayEquals(fileToByteArray(TEST_NODES_FILE), nodesMapToByteArray(result.nodesMap));
        assertArrayEquals(fileToByteArray(TEST_WAYS_FILE), waysMapToByteArray(result.waysMap));
    }

    @Test
    public void testParseJSON() throws IOException {
        byte[] jsonData = fileToByteArray(TEST_JSON_FILE);
        TLongObjectMap<TLongList> result = OSMDataParser.parseJSON(jsonData);
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