package com.example.isochronemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.isochronemap.isochronebuilding.IsochroneBuilder;
import com.example.isochronemap.isochronebuilding.NotEnoughNodesException;
import com.example.isochronemap.isochronebuilding.UnsupportedParameterException;
import com.example.isochronemap.location.OneTimeLocationProvider;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.TransportType;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.warkiz.widget.IndicatorSeekBar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM_LEVEL = 10;
    private static final LatLng START_POSITION = new LatLng(59.980547, 30.324066);

    private GoogleMap map;
    private Marker currentPosition;
    private Polygon currentPolygon;

    private boolean menuButtonIsActivated = false;
    private SearchView searchField;
    private ConstraintLayout settingsLayout;
    private ImageButton menuButton;
    private ImageButton walkingButton;
    private ImageButton bikeButton;
    private ImageButton carButton;
    private IndicatorSeekBar seekBar;
    TransportType currentTransport = TransportType.FOOT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchField = findViewById(R.id.search_field);
        settingsLayout = findViewById(R.id.settings_layout);
        menuButton = findViewById(R.id.menu_button);
        walkingButton = findViewById(R.id.walking_button);
        bikeButton = findViewById(R.id.bike_button);
        carButton = findViewById(R.id.car_button);
        seekBar = findViewById(R.id.seekBar);

        updateTransportDependingUI();
        updateSettingsStateUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //FIXME code duplication
        searchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String queryWithoutCommas = query.replace(',', '.');
                Scanner scanner = new Scanner(queryWithoutCommas).useLocale(Locale.US);
                //scanner.useDelimiter("(\\s|;|,)+"); does not work with russian locale

                if (!scanner.hasNextDouble()) {
                    Toast toast = Toast.makeText(
                            MainActivity.this, "wrong format", Toast.LENGTH_LONG);
                    toast.show();
                    return false;
                }
                double latitude = scanner.nextDouble();

                if (!scanner.hasNextDouble()) {
                    Toast toast = Toast.makeText(
                            MainActivity.this, "wrong format", Toast.LENGTH_LONG);
                    toast.show();
                    return false;
                }
                double longitude = scanner.nextDouble();

                if (scanner.hasNext()) {
                    Toast toast = Toast.makeText(
                            MainActivity.this, "wrong format", Toast.LENGTH_LONG);
                    toast.show();
                    return false;
                }
                setCurrentPositionAndMoveCamera(new Coordinate(latitude, longitude));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Rect rectMenuBar = new Rect();
        Rect rectSettings = new Rect();
        findViewById(R.id.menu_bar_card).getGlobalVisibleRect(rectMenuBar);
        findViewById(R.id.settings_card).getGlobalVisibleRect(rectSettings);
        if (!rectMenuBar.contains((int) event.getX(), (int) event.getY())
                && !rectSettings.contains((int) event.getX(), (int) event.getY())
                && menuButtonIsActivated) {
            toggleMenu(menuButton);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setPadding(0, 180, 0, 0);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                START_POSITION, DEFAULT_ZOOM_LEVEL
        ));
        map.setOnMapLongClickListener(latLng ->
            setCurrentPosition(new Coordinate(latLng.latitude, latLng.longitude))
        );
    }

    private LatLng toLatLng(Coordinate coordinate) {
        return new LatLng(coordinate.latitudeDeg, coordinate.longitudeDeg);
    }

    private Coordinate toCoordinate(LatLng latLng) {
        return new Coordinate(latLng.latitude, latLng.longitude);
    }

    private void buildIsochrone() {
        removeCurrentPolygon();
        if (currentPosition == null) {
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
                        currentTransport
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
        map.moveCamera(CameraUpdateFactory.newLatLng(currentPosition.getPosition()));
    }

    private void setCurrentPositionAndMoveCamera(Coordinate coordinate) {
        setCurrentPosition(coordinate);
        moveCameraToCurrentPosition();
    }

    private void setCurrentPolygon(List<Coordinate> coordinates) {
        removeCurrentPolygon();
        // FIXME magic constants
        PolygonOptions options = new PolygonOptions()
                .strokeWidth(1)
                .strokeColor(ContextCompat
                        .getColor(this, R.color.colorPrimaryDarkTransparent))
                .fillColor(ContextCompat
                        .getColor(this, R.color.colorPrimaryDarkTransparent));
        for (Coordinate coordinate : coordinates) {
            options.add(toLatLng(coordinate));
        }
        currentPolygon = map.addPolygon(options);
    }

    private void removeCurrentPolygon() {
        if (currentPolygon != null) {
            currentPolygon.remove();
            currentPolygon = null;
        }
    }

    public void TransportButton(View view) {
        if (walkingButton.equals(view)) {
            currentTransport = TransportType.FOOT;
        } else if (bikeButton.equals(view)) {
            currentTransport = TransportType.BIKE;
        } else if (carButton.equals(view)) {
            currentTransport = TransportType.CAR;
        }
        updateTransportDependingUI();
    }

    public void toggleMenu(View view) {
        menuButtonIsActivated ^= true;
        TransitionManager.beginDelayedTransition(settingsLayout);
        updateSettingsStateUI();
    }

    private void openSettings() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(settingsLayout);
        constraintSet.clear(R.id.settings_card, ConstraintSet.BOTTOM);
        constraintSet.connect(R.id.settings_card, ConstraintSet.TOP, R.id.space_for_settings, ConstraintSet.TOP);
        constraintSet.applyTo(settingsLayout);
    }

    private void closeSettings() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(settingsLayout);
        constraintSet.clear(R.id.settings_card, ConstraintSet.TOP);
        constraintSet.connect(R.id.settings_card, ConstraintSet.BOTTOM, R.id.space_for_settings, ConstraintSet.TOP);
        constraintSet.applyTo(settingsLayout);
    }

    public void positionButton(View view) {
        //FIXME magic constant
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 18);
            return;
        }
        OneTimeLocationProvider.getLocation(this, this::setCurrentPositionAndMoveCamera);
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
                Toast toast = Toast.makeText(this, "give permissions please :(",
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    public void buildIsochroneButton(View view) {
        buildIsochrone();
    }

    @Override
    public void onBackPressed() {
        if (menuButtonIsActivated) {
            toggleMenu(menuButton);
            return;
        }
        super.onBackPressed();
    }

    private double getTravelTime() {
        return seekBar.getProgress() / 60.0;
    }

    private static class IsochroneRequest {
        private final Coordinate coordinate;
        private final double travelTime;
        private final TransportType transportType;

        private IsochroneRequest(Coordinate coordinate, double travelTime,
                                TransportType transportType) {
            this.coordinate = coordinate;
            this.travelTime = travelTime;
            this.transportType = transportType;
        }
    }

    private static class IsochroneResponse {
        private final boolean isSuccessful;
        private final List<Coordinate> result;
        private final String errorMessage;

        private IsochroneResponse(List<Coordinate> coordinates) {
            isSuccessful = true;
            result = coordinates;
            errorMessage = null;
        }

        private IsochroneResponse(String message) {
            isSuccessful = false;
            result = null;
            errorMessage = message;
        }

        private @NotNull List<Coordinate> getResult() {
            return Objects.requireNonNull(result);
        }

        private @NotNull String getErrorMessage() {
            return Objects.requireNonNull(errorMessage);
        }
    }

    private class AsyncMapRequest extends AsyncTask<IsochroneRequest, Integer, IsochroneResponse> {
        @Override
        protected IsochroneResponse doInBackground(IsochroneRequest ... isochroneRequest) {
            IsochroneRequest request = isochroneRequest[0];
            // TODO remove it
            if (request.transportType != TransportType.FOOT) {
                return new IsochroneResponse("this transport is not supported yet");
            }
            try {
                return new IsochroneResponse(
                        IsochroneBuilder.getIsochronePolygon(
                                request.coordinate,
                                request.travelTime,
                                request.transportType
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
            if (response.isSuccessful) {
                setCurrentPolygon(response.getResult());
            } else {
                Toast toast = Toast.makeText(
                        MainActivity.this,
                        response.getErrorMessage(),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //FIXME magic string literals
        menuButtonIsActivated = savedInstanceState.getBoolean("menuButton");
        currentTransport = (TransportType)savedInstanceState
                .getSerializable("currentTransport");
        updateSettingsStateUI();
        updateTransportDependingUI();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("menuButton", menuButtonIsActivated);
        outState.putSerializable("currentTransport", currentTransport);
    }

    private void updateTransportDependingUI() {
        ImageButton transportButton;
        IndicatorSeekBar seekBar = findViewById(R.id.seekBar);
        switch (currentTransport) {
            case FOOT:
                transportButton = walkingButton;
                seekBar.setMin(10);
                seekBar.setMax(40);
                seekBar.setTickCount(7);
                break;
            case CAR:
                transportButton = carButton;
                seekBar.setMin(10);
                seekBar.setMax(30);
                seekBar.setTickCount(5);
                break;
            case BIKE:
                transportButton = bikeButton;
                seekBar.setMin(10);
                seekBar.setMax(30);
                seekBar.setTickCount(5);
                break;
            default:
                throw new RuntimeException();
        }

        walkingButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        bikeButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        carButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));

        transportButton.setImageTintList(
                getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
    }

    private void updateSettingsStateUI() {
        if (menuButtonIsActivated) {
            menuButton.setImageTintList(
                    getResources().getColorStateList(R.color.colorPrimaryDark, getTheme()));
            searchField.setVisibility(View.INVISIBLE);
            openSettings();
        } else {
            menuButton.setImageTintList(
                    getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
            searchField.setVisibility(View.VISIBLE);
            closeSettings();
        }
    }
}
