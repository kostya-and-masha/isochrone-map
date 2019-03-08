package com.example.isochronemap.mapstructure;

import com.google.android.gms.common.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** This class downloads map structure information and transforms it into graph. **/
public class MapStructureManager {
    private static final String OVERPASS_API_INTERPRETER =
            "https://overpass.kumi.systems/api/interpreter";

    /**
     * Fills {@code request.nodes} and {@code request.startNodes} representing map structure.
     * @param request map request parameters
     */
    // TODO 180 meridian handling
    // TODO poles handling
    // TODO normal exception handling
    public static void getMapStructure(MapStructureRequest request) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        downloadMap(request, out);
        getMapStructure(request, new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Fills {@code request.nodes} and {@code request.startNodes} representing map structure.
     * @param request request parameters
     * @param in stream with result of Overpass request
     */
    public static void getMapStructure(MapStructureRequest request, InputStream in) {
        OSMDataParser.parse(request, in);
    }

    /**
     * Saves result of Overpass request.
     * @param request request parameters
     */
    public static void downloadMap(MapStructureRequest request, OutputStream out)
            throws IOException {
        URL url = new URL(OVERPASS_API_INTERPRETER);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);

        DataOutputStream connectionRequestParameters =
                new DataOutputStream(connection.getOutputStream());
        connectionRequestParameters.writeBytes("data=" +
                OverpassRequestBuilder.buildRequest(
                        new BoundingBox(
                                request.getStartCoordinate(),
                                request.getMaximumDistance()
                        ),
                        request.getTransportType()
                )
        );
        connectionRequestParameters.close();

        // TODO normal exception handling
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException();
        }
        IOUtils.copyStream(connection.getInputStream(), out);
        connection.disconnect();
    }
}
