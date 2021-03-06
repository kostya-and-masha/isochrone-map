package ru.hse.isochronemap.geocoding;

import android.util.Log;

import ru.hse.isochronemap.mapstructure.BoundingBox;
import ru.hse.isochronemap.mapstructure.Coordinate;

import com.google.android.gms.common.util.IOUtils;
import com.jsoniter.JsonIterator;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.util.Constants;

/** This class translates address into geo coordinates. **/
public class Geocoder {
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org/search";
    private static final double SEARCH_RADIUS = 60;
    private static final String URI_ERROR_MESSAGE =
            "Unexpected error occurred while constructing geocoding request URL.";

    /**
     * Suggests locations matching with given query.
     * @param query location description (address/name/etc)
     * @param currentLocation if not {@code null}, close locations have higher priority
     * @return list of matching locations
     * @throws IOException if failed to connect to server
     */
    public static List<Location> getLocations(@NonNull String query,
                                              @Nullable Coordinate currentLocation)
            throws IOException {
        try {
            URIBuilder uriBuilder = new URIBuilder(NOMINATIM);
            uriBuilder.addParameter("q", query);
            uriBuilder.addParameter("format", "json");
            if (currentLocation != null) {
                uriBuilder.addParameter(
                        "viewbox",
                        getViewBox(currentLocation, SEARCH_RADIUS)
                );
            }
            URL url = uriBuilder.build().toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            byte[] data = IOUtils.toByteArray(connection.getInputStream());
            connection.disconnect();
            return parseNominatimOutput(data);
        } catch (URISyntaxException e) {
            // This exception should never be thrown here.
            // If it was thrown, then something is deeply wrong with URIBuilder.
            Log.e(Constants.APP_TAG, URI_ERROR_MESSAGE, e);
            throw new RuntimeException(e);
        }
    }

    private static @NonNull String getViewBox(@NonNull Coordinate center, double radius) {
        BoundingBox box = new BoundingBox(center, radius);
        return box.minimum.longitude + ","
               + box.minimum.latitude + ","
               + box.maximum.longitude + ","
               + box.maximum.latitude;
    }

    private static List<Location> parseNominatimOutput(@NonNull byte[] data) throws IOException {
        List<Location> result = new ArrayList<>();

        JsonIterator iterator = JsonIterator.parse(data);
        while (iterator.readArray()) {
            String name = null;
            double latitude = 0;
            double longitude = 0;
            for (String f = iterator.readObject(); f != null; f = iterator.readObject()) {
                switch (f) {
                    case "display_name":
                        name = iterator.readString();
                        break;
                    case "lat":
                        latitude = Double.parseDouble(iterator.readString());
                        break;
                    case "lon":
                        longitude = Double.parseDouble(iterator.readString());
                        break;
                    default:
                        iterator.skip();
                        break;
                }
            }
            result.add(new Location(
                            Objects.requireNonNull(name),
                            new Coordinate(latitude, longitude)));
        }
        return result;
    }
}
