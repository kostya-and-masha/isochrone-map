package com.example.isochronemap;

import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.isochronemap.isochronebuilding.IsochroneBuilder;
import com.example.isochronemap.isochronebuilding.NotEnoughNodesException;
import com.example.isochronemap.isochronebuilding.UnsupportedParameterException;
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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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
    private ProgressBar seekBar;
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

        TransportButton(walkingButton);
        closeSettings();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Rect rect = new Rect();
        findViewById(R.id.menu_bar_layout).getHitRect(rect);
        if (!rect.contains((int) event.getX(), (int) event.getY())
                && menuButtonIsActivated) {
            toggleMenu(menuButton);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

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

    private void setCurrentPosition(Coordinate coordinate) {
        if (currentPosition == null) {
            currentPosition = map.addMarker(new MarkerOptions().position(toLatLng(coordinate)));
        } else {
            currentPosition.setPosition(toLatLng(coordinate));
        }
        removeCurrentPolygon();

        new AsyncMapRequest().execute(
                new IsochroneRequest(
                        coordinate,
                        getTravelTime(),
                        currentTransport
                )
        );
    }

    private void setCurrentPolygon(List<Coordinate> coordinates) {
        removeCurrentPolygon();
        // FIXME magic constants
        PolygonOptions options = new PolygonOptions()
                .strokeWidth(1)
                .strokeColor(0xffbb0000)
                .fillColor(0x66ff0000);
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
        walkingButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        bikeButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        carButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));

        if (walkingButton.equals(view)) {
            currentTransport = TransportType.FOOT;
        } else if (bikeButton.equals(view)) {
            currentTransport = TransportType.BIKE;
        } else if (carButton.equals(view)) {
            currentTransport = TransportType.CAR;
        }

        ((ImageButton)view).setImageTintList(getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
    }

    public void toggleMenu(View view) {
        menuButtonIsActivated ^= true;
        if (menuButtonIsActivated) {
            ((ImageView)view).setImageTintList(getResources().getColorStateList(R.color.colorPrimaryDark, getTheme()));
            searchField.setVisibility(View.INVISIBLE);

            TransitionManager.beginDelayedTransition(settingsLayout);
            openSettings();
        } else {
            ((ImageView)view).setImageTintList(getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
            searchField.setVisibility(View.VISIBLE);

            TransitionManager.beginDelayedTransition(settingsLayout);
            closeSettings();
        }
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
        Toast toast = Toast.makeText(this, "i am geoposition button", Toast.LENGTH_LONG);
        toast.show();
    }

    public void buildIsochroneButton(View view) {
        Toast toast = Toast.makeText(this, "i am build isochrone button", Toast.LENGTH_LONG);
        toast.show();
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
        // FIXME
        return 15.0 / 60.0;
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

        public @NotNull List<Coordinate> getResult() {
            return Objects.requireNonNull(result);
        }

        public @NotNull String getErrorMessage() {
            return Objects.requireNonNull(errorMessage);
        }
    }

    private class AsyncMapRequest extends AsyncTask<IsochroneRequest, Integer, IsochroneResponse> {
        @Override
        protected IsochroneResponse doInBackground(IsochroneRequest ... isochroneRequest) {
            IsochroneRequest request = isochroneRequest[0];
            // TODO remove it
            if (request.transportType != TransportType.FOOT) {
                return new IsochroneResponse("This transport is not supported yet");
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
                return new IsochroneResponse("Failed to download map");
            } catch (NotEnoughNodesException e) {
                return new IsochroneResponse("Cannot build isochrone in this area");
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
}
