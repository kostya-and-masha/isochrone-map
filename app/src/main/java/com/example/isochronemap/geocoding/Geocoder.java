package com.example.isochronemap.geocoding;

import android.os.AsyncTask;

import com.example.isochronemap.mapstructure.BoundingBox;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.util.Consumer;
import com.google.android.gms.common.util.IOUtils;
import com.jsoniter.JsonIterator;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** This class translates address into geo coordinates. **/
public class Geocoder {
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org/search";
    private static final double SEARCH_RADIUS = 60;

    /**
     * Suggests locations matching with given query.
     * @param query location description (address/name/etc)
     * @param currentLocation if not {@code null}, close locations have higher priority
     * @return list of matching locations
     */
    // fixme exceptions
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
            throw new RuntimeException(e);
        }
    }

    /**
     * Suggests locations matching with given query.
     * @param query location description (address/name/etc)
     * @param currentLocation if not {@code null}, close locations have higher priority
     * @param onSuccess on success callback
     * @param onFailure on failure callback
     */
    public static void getLocations(@NonNull String query,
                                    @Nullable Coordinate currentLocation,
                                    @NonNull Consumer<List<Location>> onSuccess,
                                    @NonNull Consumer<Exception> onFailure) {
        new GeocodingTask(query, currentLocation, onSuccess, onFailure).execute();
    }

    private static @NonNull String getViewBox(@NonNull Coordinate center, double radius) {
        BoundingBox box = new BoundingBox(center, radius);
        return box.minimum.longitudeDeg + "," +
               box.minimum.latitudeDeg  + "," +
               box.maximum.longitudeDeg + "," +
               box.maximum.latitudeDeg;
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
            result.add(new Location(name, new Coordinate(latitude, longitude)));
        }
        return result;
    }

    private static class GeocodingTask extends AsyncTask<Void, Void, Void> {
        private String query;
        private Coordinate currentLocation;
        private Consumer<List<Location>> onSuccess;
        private Consumer<Exception> onFailure;

        private List<Location> result;
        private Exception exception;

        private GeocodingTask(@NonNull String query,
                              @Nullable Coordinate currentLocation,
                              @NonNull Consumer<List<Location>> onSuccess,
                              @NonNull Consumer<Exception> onFailure) {
            this.query = query;
            this.currentLocation = currentLocation;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                result = Geocoder.getLocations(query, currentLocation);
            } catch (IOException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (exception != null) {
                onFailure.accept(exception);
            } else {
                onSuccess.accept(result);
            }
        }
    }
}
