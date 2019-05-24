package com.example.isochronemap;

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

import com.example.isochronemap.isochronebuilding.IsochroneBuilder;
import com.example.isochronemap.isochronebuilding.IsochronePolygon;
import com.example.isochronemap.isochronebuilding.IsochroneRequestType;
import com.example.isochronemap.isochronebuilding.NotEnoughNodesException;
import com.example.isochronemap.isochronebuilding.UnsupportedParameterException;
import com.example.isochronemap.location.OneTimeLocationProvider;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.TransportType;
import com.example.isochronemap.searchhistory.SearchDatabase;
import com.example.isochronemap.ui.IsochroneMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
    private static final float CLOSE_ZOOM_LEVEL = 14;
    private static final LatLng START_POSITION = new LatLng(59.980547, 30.324066);

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

        menu = findViewById(R.id.menu);
        buildIsochroneButton = findViewById(R.id.build_isochrone_button);
        geopositionButton = findViewById(R.id.geoposition_button);
        progressBar = findViewById(R.id.progress_bar);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        TransportType currentTransport = TransportType.valueOf(
                sharedPreferences.getString("currentTransport", "FOOT"));
        IsochroneRequestType currentRequestType = IsochroneRequestType.valueOf(
                sharedPreferences.getString("currentRequestType", "HEXAGONAL_COVER"));
        float seekBarProgress =  sharedPreferences.getFloat("seekBarProgress", 10);

        menu.setPreferencesBeforeDrawn(currentTransport,
                currentRequestType, seekBarProgress);

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        menu.setOnPlaceQueryListener(this::setCurrentPositionAndMoveCamera);

        buildIsochroneButton.setOnClickListener(ignored -> {
            menu.closeEverything();
            buildIsochrone();
        });

        geopositionButton.setOnClickListener(ignored -> {
            showProgressBar();
            menu.closeEverything();
            //FIXME magic constant
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 18);
                return;
            }
            OneTimeLocationProvider.getLocation(this, this::setCurrentPositionAndMoveCamera);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setPadding(14, 180, 14, 0);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                START_POSITION, DEFAULT_ZOOM_LEVEL
        ));
        map.setOnMapLongClickListener(latLng ->
                setCurrentPosition(new Coordinate(latLng.latitude, latLng.longitude))
        );
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
        //FIXME magic constant
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 18) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                OneTimeLocationProvider.getLocation(this, this::setCurrentPositionAndMoveCamera);
            } else {
                hideProgressBar();
                Toast toast = Toast.makeText(this, "give permissions please :(",
                        Toast.LENGTH_LONG);
                toast.show();
            }
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
                        toCoordinate(currentPosition.getPosition()),
                        getTravelTime(),
                        menu.getCurrentTransport(),
                        menu.getCurrentRequestType()
                )
        );
    }

    private void setCurrentPosition(Coordinate coordinate) {
        if (currentPosition == null) {
            currentPosition = map.addMarker(new MarkerOptions().position(toLatLng(coordinate)));
        } else {
            currentPosition.setPosition(toLatLng(coordinate));
        }
        buildIsochrone();
    }

    private void moveCameraToCurrentPosition() {
        map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(currentPosition.getPosition(), CLOSE_ZOOM_LEVEL),
                2000,
                null
        );
    }

    private void setCurrentPositionAndMoveCamera(Coordinate coordinate) {
        setCurrentPosition(coordinate);
        moveCameraToCurrentPosition();
    }

    private void setCurrentPolygons(List<IsochronePolygon> isochronePolygons) {
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
                LatLng latLng = toLatLng(coordinate);
                options.add(latLng);
                bounds.add(latLng);
            }

            for (List<Coordinate> coordinates : currentPolygon.getInteriorRings()) {
                List<LatLng> hole = new ArrayList<>();
                for (Coordinate point : coordinates) {
                    hole.add(toLatLng(point));
                }
                options.addHole(hole);
            }
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
    }

    private LatLng toLatLng(Coordinate coordinate) {
        return new LatLng(coordinate.latitudeDeg, coordinate.longitudeDeg);
    }

    private Coordinate toCoordinate(LatLng latLng) {
        return new Coordinate(latLng.latitude, latLng.longitude);
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
