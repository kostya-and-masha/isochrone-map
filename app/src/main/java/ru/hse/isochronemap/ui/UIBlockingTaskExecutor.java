package ru.hse.isochronemap.ui;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.geocoding.Geocoder;
import ru.hse.isochronemap.geocoding.Location;
import ru.hse.isochronemap.isochronebuilding.IsochroneBuilder;
import ru.hse.isochronemap.isochronebuilding.NotEnoughNodesException;
import ru.hse.isochronemap.location.IsochroneMapLocationManager;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.MapStructureManager;
import ru.hse.isochronemap.mapstructure.MapStructureRequest;
import ru.hse.isochronemap.mapstructure.Node;
import ru.hse.isochronemap.util.Consumer;
import ru.hse.isochronemap.util.IsochroneRequest;
import ru.hse.isochronemap.util.IsochroneResponse;

class UIBlockingTaskExecutor {
    static void executeIsochroneRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                        @NonNull IsochroneRequest request) {
        new GetIsochroneTask(auxiliaryFragment, request).execute();
    }

    static void executeGeocodingRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                        @NonNull String query,
                                        @Nullable Coordinate currentLocation,
                                        @NonNull Consumer<List<Location>> onSuccess,
                                        @NonNull Runnable onFailure) {
        new GeocodingTask(
                auxiliaryFragment,
                query,
                currentLocation,
                onSuccess,
                onFailure).execute();
    }

    static void executeLocationRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                       @NonNull IsochroneMapLocationManager locationManager,
                                       @NonNull Consumer<Coordinate> onSuccess,
                                       @NonNull Runnable onFailure) {
        new LocationTask(
                auxiliaryFragment,
                locationManager,
                onSuccess,
                onFailure).execute();
    }

    static void executeApproximateLocationRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                                  @NonNull IsochroneMapLocationManager locationManager,
                                                  @NonNull Consumer<Coordinate> callback) {
        new ApproximateLocationTask(
                auxiliaryFragment,
                locationManager,
                callback).execute();
    }

    private static class GetIsochroneTask extends UIBlockingTask {
        IsochroneRequest request;
        IsochroneResponse response;

        private GetIsochroneTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                                 @NonNull IsochroneRequest request) {
            super(auxiliaryFragment);
            this.request = request;
        }

        @Override
        protected Void doInBackground(Void ... v) {
            try {
                MapStructureRequest structureRequest = new MapStructureRequest(request);

                publishProgress("downloading map...");
                Node startNode = MapStructureManager.getMapStructure(structureRequest);

                publishProgress("building isochrone...");
                response = new IsochroneResponse(
                        IsochroneBuilder.getIsochronePolygons(
                                startNode,
                                request.travelTime,
                                request.transportType,
                                request.isochroneType),
                        request.travelTime,
                        request.transportType
                );
            } catch (IOException e) {
                response = new IsochroneResponse("failed to download map");
            } catch (NotEnoughNodesException e) {
                response = new IsochroneResponse("cannot build isochrone in this area");
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            /// ?????
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            auxiliaryFragment.transferActionToMainActivity(
                    activity -> activity.asyncMapRequestCallback(response));
        }
    }

    private static class GeocodingTask extends UIBlockingTask {
        private String query;
        private Coordinate currentLocation;
        private Consumer<List<Location>> onSuccess;
        private Runnable onFailure;

        private List<Location> result;
        private boolean isSuccessful;

        private GeocodingTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                              @NonNull String query,
                              @Nullable Coordinate currentLocation,
                              @NonNull Consumer<List<Location>> onSuccess,
                              @NonNull Runnable onFailure) {
            super(auxiliaryFragment);
            this.query = query;
            this.currentLocation = currentLocation;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                publishProgress("requesting geocoding results...");
                result = Geocoder.getLocations(query, currentLocation);
                isSuccessful = true;
            } catch (IOException ignored) {}
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (isSuccessful) {
                onSuccess.accept(result);
            } else {
                onFailure.run();
            }
        }
    }

    private static class LocationTask extends UIBlockingTask {
        private IsochroneMapLocationManager locationManager;
        private Coordinate location;
        private boolean isSuccessful;
        private Consumer<Coordinate> onSuccess;
        private Runnable onFailure;

        private LocationTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                             @NonNull IsochroneMapLocationManager locationManager,
                             @NonNull Consumer<Coordinate> onSuccess,
                             @NonNull Runnable onFailure) {
            super(auxiliaryFragment);
            this.locationManager = locationManager;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                publishProgress("requesting location...");
                location = locationManager.getPreciseLocationBlocking();
                if (location != null) {
                    isSuccessful = true;
                }
            } catch (InterruptedException ignored) {}
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (isSuccessful) {
                onSuccess.accept(location);
            } else {
                onFailure.run();
            }
        }
    }

    private static class ApproximateLocationTask extends UIBlockingTask {
        private IsochroneMapLocationManager locationManager;
        private Coordinate location;
        private Consumer<Coordinate> callback;

        private ApproximateLocationTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                             @NonNull IsochroneMapLocationManager locationManager,
                             @NonNull Consumer<Coordinate> callback) {
            super(auxiliaryFragment);
            this.locationManager = locationManager;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                publishProgress("requesting approximate location...");
                location = locationManager.getApproximateLocation();
            } catch (InterruptedException ignored) {}
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            callback.accept(location);
        }
    }
}
