package ru.hse.isochronemap.ui;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.R;
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

/** This class contains all blocking UI asynchronous tasks. **/
class UIBlockingTaskExecutor {
    /**
     * Executes isochrone request.
     * @param auxiliaryFragment used to deliver callbacks to living {@link MainActivity}.
     * @param request request parameters.
     */
    static void executeIsochroneRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                        @NonNull IsochroneRequest request) {
        new GetIsochroneTask(auxiliaryFragment, request).execute();
    }

    /**
     * Executes geocoding request.
     * @param auxiliaryFragment used to deliver callbacks to living {@link MainActivity}.
     * @param query place name.
     * @param currentLocation approximate location used to increase priority of close places.
     * @param onSuccess on success callback.
     * @param onFailure on failure callback.
     */
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

    /**
     * Executes precise location request.
     * @param auxiliaryFragment used to deliver callbacks to living {@link MainActivity}.
     * @param locationManager provides location.
     * @param callback on success callback.
     */
    static void executeLocationRequest(@NonNull AuxiliaryFragment auxiliaryFragment,
                                       @NonNull IsochroneMapLocationManager locationManager,
                                       @NonNull Consumer<Coordinate> callback) {
        new LocationTask(
                auxiliaryFragment,
                locationManager,
                callback).execute();
    }

    /**
     * Executes approximate location request.
     * @param auxiliaryFragment used to deliver callbacks to living {@link MainActivity}.
     * @param locationManager provides location.
     * @param callback callback that handles both success an failure.
     */
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

                publishProgress(auxiliaryFragment.getString(R.string.download_map_message));
                Node startNode = MapStructureManager.getMapStructure(structureRequest);

                if (isCancelled()) {
                    return null;
                }

                publishProgress(auxiliaryFragment.getString(R.string.build_isochrone_message));
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
                response = new IsochroneResponse(
                        auxiliaryFragment.getString(R.string.download_map_error));
            } catch (NotEnoughNodesException e) {
                response = new IsochroneResponse(
                        auxiliaryFragment.getString(R.string.build_isochrone_error));
            } catch (InterruptedException ignored) {
                // InterruptedException occurs if task was cancelled
            }
            return null;
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
                publishProgress(auxiliaryFragment.getString(R.string.geocoding_message));
                result = Geocoder.getLocations(query, currentLocation);
                isSuccessful = true;
            } catch (IOException e) {
                isSuccessful = false;
            }
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
        private Consumer<Coordinate> callback;

        private LocationTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                             @NonNull IsochroneMapLocationManager locationManager,
                             @NonNull Consumer<Coordinate> callback) {
            super(auxiliaryFragment);
            this.locationManager = locationManager;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                publishProgress(auxiliaryFragment.getString(R.string.location_message));
                location = locationManager.getPreciseLocationBlocking();
                if (location != null) {
                    isSuccessful = true;
                }
            } catch (InterruptedException ignored) {
                // InterruptedException occurs if task was cancelled
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (isSuccessful) {
                callback.accept(location);
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
                publishProgress(auxiliaryFragment.getString(
                        R.string.approximate_location_message));
                location = locationManager.getApproximateLocation();
            } catch (InterruptedException ignored) {
                // InterruptedException occurs if task was cancelled
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            callback.accept(location);
        }
    }
}
