package ru.hse.isochronemap.ui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.isochronebuilding.IsochroneBuilder;
import ru.hse.isochronemap.isochronebuilding.IsochronePolygon;
import ru.hse.isochronemap.isochronebuilding.NotEnoughNodesException;
import ru.hse.isochronemap.location.IsochroneMapLocationManager;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.IsochroneRequest;
import ru.hse.isochronemap.util.IsochroneResponse;

/** This class is used to manage GoogleMap. */
class MapManager implements OnMapReadyCallback {
    private static final float CLOSE_ZOOM_LEVEL = 14;
    private static final int CAMERA_ANIMATION_TIME = 2000;

    private static final String CAMERA_POSITION = "CAMERA_POSITION";
    private static final String MARKER_POSITION = "MARKER_POSITION";
    private static final String MARKER_TITLE = "MARKER_TITLE";

    private static final String CAMERA_LATITUDE = "CAMERA_LATITUDE";
    private static final String CAMERA_LONGITUDE = "CAMERA_LONGITUDE";
    private static final String CAMERA_ZOOM = "CAMERA_ZOOM";
    private static final String CAMERA_TILT = "CAMERA_TILT";
    private static final String CAMERA_BEARING = "CAMERA_BEARING";

    private MainActivity activity;
    private AuxiliaryFragment auxiliaryFragment;
    private IsochroneMapLocationManager locationManager;

    private GoogleMap map;
    private Marker currentPosition;
    private List<Polygon> isochronePolygons = new ArrayList<>();
    private List<Runnable> onMapReadyActions = new ArrayList<>();

    private CameraPosition savedCameraPosition;
    private LatLng savedMarkerPosition;
    private String savedMarkerTitle;
    private ArrayList<PolygonOptions> savedPolygonOptions;

    void setActivity(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    void setAuxiliaryFragment(@NonNull AuxiliaryFragment auxiliaryFragment) {
        this.auxiliaryFragment = auxiliaryFragment;
    }

    void setIsochroneMapLocationManager(@NonNull IsochroneMapLocationManager locationManager) {
        this.locationManager = locationManager;
    }

    void addOnMapReadyAction(@NonNull Runnable action) {
        if (isReady()) {
            action.run();
        } else {
            onMapReadyActions.add(action);
        }
    }

    @Nullable Coordinate getMarkerPosition() {
        if (currentPosition == null) {
            return null;
        }
        return new Coordinate(currentPosition.getPosition());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.setOnMapLongClickListener(position -> setCurrentPosition(new Coordinate(position)));
        map.setOnMarkerClickListener(event -> {
            updateMarkerTitle(currentPosition.getTitle());
            return true;
        });

        if (savedCameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPosition));
        } else if (locationManager.hasPermissions()) {
            // FIXME !!!!!!!!!!!!!!!!!!!!
            UIBlockingTaskExecutor.executeLocationRequest(
                    auxiliaryFragment,
                    locationManager,
                    location -> auxiliaryFragment.transferActionToMainActivity(
                            mainActivity -> mainActivity.initLocationCallback(location)),
                    () -> auxiliaryFragment.transferActionToMainActivity(
                            mainActivity -> {
                                Toast.makeText(mainActivity,
                                        "failed to get current location",
                                        Toast.LENGTH_LONG).show();
                            }
                    )
            );
        }

        setPadding();

        if (savedMarkerPosition != null) {
            currentPosition = map.addMarker(new MarkerOptions().position(savedMarkerPosition));
            updateMarkerTitle(savedMarkerTitle);
        }

        if (savedPolygonOptions != null) {
            for (PolygonOptions options : savedPolygonOptions) {
                isochronePolygons.add(map.addPolygon(options));
            }
        } else {
            savedPolygonOptions = new ArrayList<>();
        }

        for (Runnable action : onMapReadyActions) {
            action.run();
        }
    }

    boolean isReady() {
        return map != null;
    }

    void saveInstanceState(@NonNull Bundle outState) {
        if (isReady()) {
            resetPadding();
            outState.putParcelable(CAMERA_POSITION, map.getCameraPosition());
            setPadding();
        } else {
            outState.putParcelable(CAMERA_POSITION, savedCameraPosition);
        }
        if (currentPosition != null) {
            outState.putParcelable(MARKER_POSITION, currentPosition.getPosition());
            outState.putString(MARKER_TITLE, currentPosition.getTitle());
        }
        auxiliaryFragment.setSavedPolygons(savedPolygonOptions);
    }

    void restoreInstanceState(@NonNull Bundle savedInstanceState) {
        CameraPosition savedPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
        if (savedPosition != null) {
            savedCameraPosition = savedPosition;
        }
        savedMarkerPosition = savedInstanceState.getParcelable(MARKER_POSITION);
        savedMarkerTitle = savedInstanceState.getString(MARKER_TITLE);
        savedPolygonOptions = auxiliaryFragment.getSavedPolygons();
        auxiliaryFragment.setSavedPolygons(null);
    }

    void saveCameraPosition(@NonNull SharedPreferences sharedPreferences) {
        if (isReady()) {
            resetPadding();
            CameraPosition position = map.getCameraPosition();
            sharedPreferences.edit()
                             .putFloat(CAMERA_LATITUDE, (float) position.target.latitude)
                             .putFloat(CAMERA_LONGITUDE, (float) position.target.longitude)
                             .putFloat(CAMERA_ZOOM, position.zoom)
                             .putFloat(CAMERA_TILT, position.tilt)
                             .putFloat(CAMERA_BEARING, position.bearing).apply();
            setPadding();
        }
    }

    void restoreCameraPosition(@NonNull SharedPreferences sharedPreferences) {
        if (sharedPreferences.contains(CAMERA_ZOOM)) {
            double latitude = sharedPreferences.getFloat(CAMERA_LATITUDE, 0);
            double longitude = sharedPreferences.getFloat(CAMERA_LONGITUDE, 0);
            float zoom = sharedPreferences.getFloat(CAMERA_ZOOM, 0);
            float tilt = sharedPreferences.getFloat(CAMERA_TILT, 0);
            float bearing = sharedPreferences.getFloat(CAMERA_BEARING, 0);
            savedCameraPosition = new CameraPosition(
                    new LatLng(latitude, longitude),
                    zoom,
                    tilt,
                    bearing
            );
        }
    }

    void setCurrentPosition(@Nullable Coordinate position) {
        if (position == null) {
            if (currentPosition != null) {
                currentPosition.setVisible(false);
            }
            return;
        }

        if (currentPosition == null) {
            currentPosition = map.addMarker(new MarkerOptions().position(position.toLatLng()));
        } else {
            currentPosition.setPosition(position.toLatLng());
        }
        currentPosition.setVisible(true);
        currentPosition.setTitle("");
        currentPosition.hideInfoWindow();
        activity.buildIsochrone();
    }

    void setCurrentPositionAndMoveCamera(@NonNull Coordinate position) {
        setCurrentPosition(position);
        map.animateCamera(
                CameraUpdateFactory.newLatLng(position.toLatLng()),
                CAMERA_ANIMATION_TIME,
                null);
    }

    public void initialMove(Coordinate position) {
        if (isReady() && savedCameraPosition == null) {
            map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(position.toLatLng(), CLOSE_ZOOM_LEVEL),
                    CAMERA_ANIMATION_TIME,
                    null);
        }
    }

    void setIsochronePolygons(@NonNull List<IsochronePolygon> isochronePolygons) {
        removeIsochronePolygons();

        LatLngBox bounds = new LatLngBox();
        for (IsochronePolygon currentPolygon : isochronePolygons) {
            PolygonOptions options = new PolygonOptions()
                    .strokeWidth(1)
                    .strokeColor(ContextCompat.getColor(activity, R.color.colorPolygonBorder))
                    .fillColor(ContextCompat.getColor(activity, R.color.colorPolygonFill));

            for (Coordinate coordinate : currentPolygon.getExteriorRing()) {
                LatLng latLng = coordinate.toLatLng();
                options.add(latLng);
                bounds.add(latLng);
            }

            for (List<Coordinate> coordinates : currentPolygon.getInteriorRings()) {
                List<LatLng> hole = new ArrayList<>();
                for (Coordinate point : coordinates) {
                    hole.add(point.toLatLng());
                }
                options.addHole(hole);
            }
            savedPolygonOptions.add(options);
            this.isochronePolygons.add(map.addPolygon(options));
        }

        map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                        new LatLngBounds(bounds.getMin(), bounds.getMax()), 0),
                CAMERA_ANIMATION_TIME, null);
    }

    void removeIsochronePolygons() {
        for (Polygon polygon : isochronePolygons) {
            polygon.remove();
        }
        isochronePolygons.clear();
        savedPolygonOptions.clear();
    }

    void updateMarkerTitle(@NonNull String title) {
        currentPosition.setTitle(title);
        if (title.equals("")) {
            currentPosition.hideInfoWindow();
        } else {
            currentPosition.showInfoWindow();
        }
    }

    private void setPadding() {
        int marginTop = convertDpToPixels(80);
        int marginSide = convertDpToPixels(5);
        map.setPadding(marginSide, marginTop, marginSide, 0);
    }

    private void resetPadding() {
        map.setPadding(0, 0, 0, 0);
    }

    private int convertDpToPixels(float dp) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static class LatLngBox {
        private double minLat = 1000;
        private double minLng = 1000;
        private double maxLat = -1000;
        private double maxLng = -1000;

        private void add(LatLng latLng) {
            minLat = Math.min(minLat, latLng.latitude);
            maxLat = Math.max(maxLat, latLng.latitude);
            minLng = Math.min(minLng, latLng.longitude);
            maxLng = Math.max(maxLng, latLng.longitude);
        }

        private LatLng getMin() {
            return new LatLng(minLat, minLng);
        }

        private LatLng getMax() {
            return new LatLng(maxLat, maxLng);
        }
    }
}
