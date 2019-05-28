package ru.hse.isochronemap;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import ru.hse.isochronemap.R;

import ru.hse.isochronemap.isochronebuilding.IsochroneBuilder;
import ru.hse.isochronemap.isochronebuilding.IsochronePolygon;
import ru.hse.isochronemap.isochronebuilding.IsochroneRequestType;
import ru.hse.isochronemap.isochronebuilding.NotEnoughNodesException;
import ru.hse.isochronemap.isochronebuilding.UnsupportedParameterException;
import ru.hse.isochronemap.location.OneTimeLocationProvider;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.TransportType;
import ru.hse.isochronemap.ui.IsochroneMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM_LEVEL = 10;
    private static final LatLng START_POSITION = new LatLng(59.980547, 30.324066);

    private static final String CAMERA_POSITION = "CAMERA_POSITION";
    private static final String MARKER_POSITION = "MARKER_POSITION";
    private static final String POLYGON_OPTIONS = "POLYGON_OPTIONS";
    private static final String PROGRESS_BAR_STATE = "PROGRESS_BAR_STATE";
    public static final int INITIAL_PERMISSIONS_REQUEST = 1;
    public static final int GEOPOSITION_REQUEST = 2;

    private CameraPosition initialCameraPosition;
    private LatLng initialMarkerPosition;
    private ArrayList<PolygonOptions> currentPolygonOptions;

    private GoogleMap map;
    private Marker currentPosition;
    private List<Polygon> currentPolygons = new ArrayList<>();

    private IsochroneMenu menu;
    private FloatingActionButton buildIsochroneButton;
    private FloatingActionButton geopositionButton;
    private ProgressBar progressBar;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menu = (IsochroneMenu)getSupportFragmentManager().findFragmentById(R.id.menu);
        buildIsochroneButton = findViewById(R.id.build_isochrone_button);
        geopositionButton = findViewById(R.id.geoposition_button);
        progressBar = findViewById(R.id.progress_bar);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        TransportType currentTransport = TransportType.valueOf(
                sharedPreferences.getString("currentTransport", "FOOT"));
        IsochroneRequestType currentRequestType = IsochroneRequestType.valueOf(
                sharedPreferences.getString("currentRequestType", "HEXAGONAL_COVER"));
        float seekBarProgress =  sharedPreferences.getFloat("seekBarProgress", 10);

        menu.setPreferencesBeforeDrawn(currentTransport, currentRequestType, seekBarProgress);

        if (currentRequestType == IsochroneRequestType.HEXAGONAL_COVER) {
            ((ImageButton)findViewById(R.id.build_isochrone_button)).setImageResource(
                    R.drawable.ic_hexagonal_button_24dp);
        } else {
            ((ImageButton)findViewById(R.id.build_isochrone_button)).setImageResource(
                    R.drawable.ic_convex_hull_button_24dp);
        }

        menu.setOnHexagonalCoverButtonClickListener(ignored ->
                ((ImageButton)findViewById(R.id.build_isochrone_button)).setImageResource(
                R.drawable.ic_hexagonal_button_24dp)
        );

        menu.setOnConvexHullButtonClickListener(ignored ->
                ((ImageButton)findViewById(R.id.build_isochrone_button)).setImageResource(
                R.drawable.ic_convex_hull_button_24dp)
        );

        menu.setOnPlaceQueryListener(this::setCurrentPosition);

        menu.setOnScreenBlockListener(enable -> {
            if (enable) {
                showProgressBar();
            } else {
                hideProgressBar();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildIsochroneButton.setOnClickListener(ignored -> {
            menu.closeEverything();
            buildIsochrone();
        });

        geopositionButton.setOnClickListener(ignored -> {
            showProgressBar();
            menu.closeEverything();
            //FIXME magic constant
            if (!OneTimeLocationProvider.hasPermissions(this)) {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        GEOPOSITION_REQUEST);
                return;
            }
            OneTimeLocationProvider.getLocation(this, this::setCurrentPosition);
        });

        //FIXME logic is to complex!!!
        if (!OneTimeLocationProvider.hasPermissions(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    INITIAL_PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        initialCameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
        initialMarkerPosition = savedInstanceState.getParcelable(MARKER_POSITION);
        currentPolygonOptions = savedInstanceState.getParcelableArrayList(POLYGON_OPTIONS);
        if (savedInstanceState.getBoolean(PROGRESS_BAR_STATE)) {
            showProgressBar();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (map != null) {
            outState.putParcelable(CAMERA_POSITION, map.getCameraPosition());
        }
        if (currentPosition != null) {
            outState.putParcelable(MARKER_POSITION, currentPosition.getPosition());
        }
        outState.putParcelableArrayList(POLYGON_OPTIONS, currentPolygonOptions);
        outState.putBoolean(PROGRESS_BAR_STATE, progressBar.getVisibility() == View.VISIBLE);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setPadding(14, 180, 14, 0);
        map.setOnMapLongClickListener(position -> setCurrentPosition(new Coordinate(position)));

        if (initialCameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(initialCameraPosition));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    START_POSITION, DEFAULT_ZOOM_LEVEL
            ));
        }

        if (initialMarkerPosition != null) {
            currentPosition = map.addMarker(new MarkerOptions().position(initialMarkerPosition));
        }

        if (currentPolygonOptions != null) {
            for (PolygonOptions options : currentPolygonOptions) {
                currentPolygons.add(map.addPolygon(options));
            }
        } else {
            currentPolygonOptions = new ArrayList<>();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //FIXME magic constant literals
        sharedPreferences.edit()
                .putString("currentTransport", menu.getCurrentTransport().toString())
                .putString("currentRequestType", menu.getCurrentRequestType().toString())
                .putFloat("seekBarProgress", menu.getCurrentSeekBarProgress())
                .apply();
    }

    @Override
    public void onBackPressed() {
        if (!menu.closeEverything()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEOPOSITION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                OneTimeLocationProvider.getLocation(this, this::setCurrentPosition);
            } else {
                hideProgressBar();
                //FIXME move to Util class
                Toast toast = Toast.makeText(this, "give permissions please :(",
                        Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (requestCode == INITIAL_PERMISSIONS_REQUEST
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(this, "give permissions please :(",
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void buildIsochrone() {
        showProgressBar();
        removeCurrentPolygons();
        if (currentPosition == null) {
            hideProgressBar();
            Toast toast = Toast.makeText(
                    this,
                    "please choose location",
                    Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        new AsyncMapRequest().execute(
                new IsochroneRequest(
                        new Coordinate(currentPosition.getPosition()),
                        getTravelTime(),
                        menu.getCurrentTransport(),
                        menu.getCurrentRequestType()
                )
        );
    }

    private void setCurrentPosition(Coordinate position) {
        if (currentPosition == null) {
            currentPosition = map.addMarker(new MarkerOptions().position(position.toLatLng()));
        } else {
            currentPosition.setPosition(position.toLatLng());
        }
        buildIsochrone();
    }

    private void setCurrentPolygons(@NonNull List<IsochronePolygon> isochronePolygons) {
        removeCurrentPolygons();

        LatLngBox bounds = new LatLngBox();
        for (IsochronePolygon currentPolygon : isochronePolygons) {
            PolygonOptions options = new PolygonOptions()
                    .strokeWidth(1)
                    .strokeColor(ContextCompat
                            .getColor(this, R.color.colorPolygonBorder))
                    .fillColor(ContextCompat
                            .getColor(this, R.color.colorPolygonFill));

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
            currentPolygonOptions.add(options);
            currentPolygons.add(map.addPolygon(options));
        }

        map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                        new LatLngBounds(bounds.getMin(), bounds.getMax()), 0
                ),
                2000,
                null
        );
    }

    private void removeCurrentPolygons() {
        for (Polygon polygon : currentPolygons) {
            polygon.remove();
        }
        currentPolygons.clear();
        currentPolygonOptions.clear();
    }

    private double getTravelTime() {
        return menu.getCurrentSeekBarProgress() / 60.0;
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private class AsyncMapRequest extends AsyncTask<IsochroneRequest, Integer, IsochroneResponse> {
        @Override
        protected IsochroneResponse doInBackground(IsochroneRequest ... isochroneRequest) {
            IsochroneRequest request = isochroneRequest[0];
            try {
                return new IsochroneResponse(
                        IsochroneBuilder.getIsochronePolygons(
                                request.coordinate,
                                request.travelTime,
                                request.transportType,
                                request.isochroneType
                        )
                );
            } catch (UnsupportedParameterException e) {
                return new IsochroneResponse(e.getMessage());
            } catch (IOException e) {
                return new IsochroneResponse("failed to download map");
            } catch (NotEnoughNodesException e) {
                return new IsochroneResponse("cannot build isochrone in this area");
            }
        }

        @Override
        protected void onPostExecute(IsochroneResponse response) {
            hideProgressBar();
            if (response.isSuccessful) {
                setCurrentPolygons(response.getResult());
            } else {
                Toast toast = Toast.makeText(
                        MainActivity.this,
                        response.getErrorMessage(),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private static class IsochroneRequest {
        private final Coordinate coordinate;
        private final double travelTime;
        private final TransportType transportType;
        private final IsochroneRequestType isochroneType;

        private IsochroneRequest(Coordinate coordinate, double travelTime,
                                TransportType transportType, IsochroneRequestType isochroneType) {
            this.coordinate = coordinate;
            this.travelTime = travelTime;
            this.transportType = transportType;
            this.isochroneType = isochroneType;
        }
    }

    private static class IsochroneResponse {
        private final boolean isSuccessful;
        private final List<IsochronePolygon> result;
        private final String errorMessage;

        private IsochroneResponse(List<IsochronePolygon> polygons) {
            isSuccessful = true;
            result = polygons;
            errorMessage = null;
        }

        private IsochroneResponse(String message) {
            isSuccessful = false;
            result = null;
            errorMessage = message;
        }

        private @NotNull List<IsochronePolygon> getResult() {
            return Objects.requireNonNull(result);
        }

        private @NotNull String getErrorMessage() {
            return Objects.requireNonNull(errorMessage);
        }
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
