package ru.hse.isochronemap.mapstructure;

import com.jsoniter.JsonIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import de.siegmar.fastcsv.reader.CsvParser;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongCharHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

class OSMDataParser {
    static @NonNull MapData parseCSV(@NonNull byte[] osmData, @NonNull TransportType transportType)
            throws IOException {
        boolean isBiDirectional = transportType == TransportType.FOOT;

        TLongObjectMap<Node> nodesMap = new TLongObjectHashMap<>();
        TLongCharMap waysMap = new TLongCharHashMap();

        CsvReader csvReader = new CsvReader();
        csvReader.setContainsHeader(false);
        csvReader.setFieldSeparator('\t');

        CsvParser csvParser =
                csvReader.parse(new InputStreamReader(new ByteArrayInputStream(osmData)));
        CsvRow row;
        while ((row = csvParser.nextRow()) != null) {
            // 0 - type, 1 - id, 2 - lat, 3 - lon, 4 - highway, 5 - oneway
            String type = row.getField(0);
            long id = Long.parseLong(row.getField(1));
            String highway = row.getField(4);

            if (type.equals("node")) {
                double latitude = Double.parseDouble(row.getField(2));
                double longitude = Double.parseDouble(row.getField(3));

                Coordinate coordinate = new Coordinate(latitude, longitude);
                boolean isCrossing =
                        highway.equals("crossing") || highway.equals("traffic_signals");
                Node node = new Node(coordinate, isCrossing);
                nodesMap.put(id, node);
            } else if (type.equals("way")) {
                String oneway = row.getField(5);
                boolean forward = isBiDirectional;
                boolean backward = isBiDirectional;
                if (!isBiDirectional) {
                    if (!oneway.equals("-1") && !oneway.equals("reverse")) {
                        forward = true;
                    }
                    if (!oneway.equals("yes") &&
                            !oneway.equals("true") &&
                            !oneway.equals("1")) {
                        backward = true;
                    }
                }
                waysMap.put(id,
                        forward ? backward ? 'a' : 'f' : 'b');
            }
        }
        return new MapData(nodesMap, waysMap);
    }

    static TLongObjectMap<TLongList> parseJSON(@NonNull byte[] osmData) throws IOException {
        TLongObjectMap<TLongList> ways = new TLongObjectHashMap<>();

        JsonIterator iterator = JsonIterator.parse(osmData);
        for (String e = iterator.readObject(); e != null; e = iterator.readObject()) {
            if (!e.equals("elements")) {
                iterator.skip();
                continue;
            }
            while (iterator.readArray()) {
                long id = 0;
                TLongList nodes = new TLongArrayList();
                for (String f = iterator.readObject(); f != null; f = iterator.readObject()) {
                    switch (f) {
                        case "id":
                            id = iterator.readLong();
                            continue;
                        case "nodes":
                            while (iterator.readArray()) {
                                nodes.add(iterator.readLong());
                            }
                            continue;
                        default:
                            iterator.skip();
                    }
                }
                ways.put(id, nodes);
            }
        }
        return ways;
    }

    static void connectNodes(@NonNull MapData map, @NonNull TLongObjectMap<TLongList> ways) {
        TLongObjectMap<Node> nodesMap = map.nodesMap;
        TLongCharMap waysMap = map.waysMap;

        for (long key : ways.keys()) {
            char direction = waysMap.get(key);
            boolean forward = direction != 'b';
            boolean backward = direction != 'f';

            TLongList nodes = ways.get(key);
            TLongIterator it = nodes.iterator();

            Node from;
            Node to = nodesMap.get(it.next());
            while (it.hasNext()) {
                from = to;
                to = nodesMap.get(it.next());
                if (from == null || to == null) {
                    continue;
                }
                double distance = from.coordinate.distanceTo(to.coordinate);
                if (forward) {
                    from.addEdge(new Edge(distance, to));
                }
                if (backward) {
                    to.addEdge(new Edge(distance, from));
                }
            }
        }
    }
}
