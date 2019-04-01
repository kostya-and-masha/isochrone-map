package com.example.isochronemap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.isochronemap.isochronebuilding.IsochroneBuilder;
import com.example.isochronemap.isochronebuilding.NotEnoughNodesException;
import com.example.isochronemap.isochronebuilding.UnsupportedParameterException;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.MapStructure;
import com.example.isochronemap.mapstructure.MapStructureManager;
import com.example.isochronemap.mapstructure.MapStructureRequest;
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

import java.io.IOException;
import java.util.IllegalFormatConversionException;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap map;
    private Marker currentPosition;
    private Polygon currentPolygon;

    private boolean menuButtonIsActivated = false;
    private SearchView searchField;
    private ConstraintLayout menuSection;
    private ImageButton walkingButton;
    private ImageButton bikeButton;
    private ImageButton carButton;
    Transport currentTransport = Transport.WALKING;

    private enum Transport {WALKING, BIKE, CAR}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchField = findViewById(R.id.search_field);
        menuSection = findViewById(R.id.menu_section);
        walkingButton = findViewById(R.id.walking_button);
        bikeButton = findViewById(R.id.bike_button);
        carButton = findViewById(R.id.car_button);
        TransportButton(walkingButton);
        closeSettings();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // FIXME magic constants
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(59.980547, 30.324066), 10
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

        // test
        MapStructureRequest request = new MapStructureRequest(
                coordinate,
                0.100,
                2.5,
                TransportType.FOOT
        );
        new AsyncMapRequest().execute(request);
    }

    private void setCurrentPolygon(List<Coordinate> coordinates) {
        if (currentPolygon != null) {
            currentPolygon.remove();
        }
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

    public void TransportButton(View view) {
        walkingButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        bikeButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        carButton.setImageTintList(getResources().getColorStateList(R.color.colorPrimary, getTheme()));

        if (walkingButton.equals(view)) {
            currentTransport = Transport.WALKING;
        } else if (bikeButton.equals(view)) {
            currentTransport = Transport.BIKE;
        } else if (carButton.equals(view)) {
            currentTransport = Transport.CAR;
        }

        ((ImageButton)view).setImageTintList(getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
    }

    public void toggleMenu(View view) {
        menuButtonIsActivated ^= true;
        if (menuButtonIsActivated) {
            ((ImageView)view).setImageTintList(getResources().getColorStateList(R.color.colorPrimaryDark, getTheme()));
            searchField.setVisibility(View.INVISIBLE);

            TransitionManager.beginDelayedTransition(menuSection);
            openSettings();
        } else {
            ((ImageView)view).setImageTintList(getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
            searchField.setVisibility(View.VISIBLE);

            TransitionManager.beginDelayedTransition(menuSection);
            closeSettings();
        }
    }

    private void openSettings() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(menuSection);
        constraintSet.clear(R.id.settings, ConstraintSet.BOTTOM);
        constraintSet.connect(R.id.settings, ConstraintSet.TOP, R.id.space, ConstraintSet.TOP);
        constraintSet.applyTo(menuSection);
    }

    private void closeSettings() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(menuSection);
        constraintSet.clear(R.id.settings, ConstraintSet.TOP);
        constraintSet.connect(R.id.settings, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraintSet.applyTo(menuSection);
    }
    public void positionButton(View view) {
        Toast toast = Toast.makeText(this, "i am geoposition button", Toast.LENGTH_LONG);
        toast.show();
    }

    public void buildIsochroneButton(View view) {
        Toast toast = Toast.makeText(this, "i am build isochrone button", Toast.LENGTH_LONG);
        toast.show();
    }

    private class AsyncMapRequest extends AsyncTask<MapStructureRequest, Integer, List<Coordinate>> {
        @Override
        protected List<Coordinate> doInBackground(MapStructureRequest ... request) {
            try {
                MapStructure structure = MapStructureManager.getMapStructure(request[0]);
                return IsochroneBuilder.getIsochronePolygon(structure, 0.2, TransportType.FOOT);
            } catch (UnsupportedParameterException e) {
                throw new IllegalStateException("Unsupported parameter");
            } catch (IOException e) {
                throw new IllegalStateException("IO exception");
            } catch (NotEnoughNodesException e) {
                throw new IllegalStateException("Not enough nodes");
            }
        }

        @Override
        protected void onPostExecute(List<Coordinate> coordinates) {
            setCurrentPolygon(coordinates);
        }
    }
}
