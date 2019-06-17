package ru.hse.isochronemap.ui;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.geocoding.Geocoder;
import ru.hse.isochronemap.geocoding.Location;
import ru.hse.isochronemap.isochronebuilding.IsochroneBuilder;
import ru.hse.isochronemap.isochronebuilding.NotEnoughNodesException;
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
                                 @NonNull Consumer<Exception> onFailure) {
        new GeocodingTask(
                auxiliaryFragment,
                query,
                currentLocation,
                onSuccess,
                onFailure).execute();
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
        private Consumer<Exception> onFailure;

        private List<Location> result;
        private Exception exception;

        private GeocodingTask(@NonNull AuxiliaryFragment auxiliaryFragment,
                              @NonNull String query,
                              @Nullable Coordinate currentLocation,
                              @NonNull Consumer<List<Location>> onSuccess,
                              @NonNull Consumer<Exception> onFailure) {
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
            } catch (IOException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (exception != null) {
                onFailure.accept(exception);
            } else {
                onSuccess.accept(result);
            }
        }
    }
}
