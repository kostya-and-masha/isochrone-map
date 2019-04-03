package com.example.isochronemap.mapstructure;

import com.google.android.gms.common.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** This class downloads map structure information and transforms it into graph. **/
public class MapStructureManager {
    private static final String OVERPASS_API_INTERPRETER =
            "https://overpass.kumi.systems/api/interpreter";

    /**
     * Returns map structure.
     * @param request map request parameters
     * @return map structure
     */
    // TODO normal exception handling
    public static MapStructure getMapStructure(MapStructureRequest request) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        downloadMap(request, out);
        return OSMDataParser.parse(request, out.toByteArray());
    }

    /**
     * Saves result of Overpass request.
     * @param request request parameters
     */
    private static void downloadMap(MapStructureRequest request, OutputStream out)
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
