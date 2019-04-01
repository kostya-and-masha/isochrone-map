package com.example.isochronemap;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
}
