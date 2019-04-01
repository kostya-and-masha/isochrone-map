package com.example.isochronemap;

import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng am = new LatLng(59.980547, 30.324066);
        mMap.addMarker(new MarkerOptions().position(am).title("Marker in AM"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(am, 10));

        PolygonOptions rectOptions = new PolygonOptions()
                .strokeWidth(1)
                .strokeColor(0xffbb0000)
                .fillColor(0x66ff0000)
                .add(
                        new LatLng(59.9, 30.3),
                        new LatLng(60, 30.3),
                        new LatLng(60, 30.4)
                );
        mMap.addPolygon(rectOptions);
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
}
