package ru.hse.isochronemap.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.isochronebuilding.IsochroneRequestType;
import ru.hse.isochronemap.location.CachedLocationProvider;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.TransportType;
import ru.hse.isochronemap.util.IsochroneRequest;
import ru.hse.isochronemap.util.IsochroneResponse;

public class MainActivity extends AppCompatActivity {
    private static final String TASKS_FRAGMENT_TAG = "TASKS_FRAGMENT";
    private static final String MENU_FRAGMENT_TAG = "MENU_FRAGMENT";
    private static final String PROGRESS_BAR_STATE = "PROGRESS_BAR_STATE";
    private static final String BLACKOUT_VIEW_STATE = "BLACKOUT_VIEW_STATE";
    private static final String PERMISSIONS_DENIED = "PERMISSIONS_DENIED";
    public static final int INITIAL_PERMISSIONS_REQUEST = 1;
    public static final int GEOPOSITION_REQUEST = 2;

    private AuxiliaryFragment auxiliaryFragment;

    private MapWrapper map = new MapWrapper();

    private IsochroneMenu menu;
    private FloatingActionButton buildIsochroneButton;
    private FloatingActionButton geopositionButton;
    private ProgressBar progressBar;
    private View blackoutView;

    private CachedLocationProvider locationProvider;
    private boolean permissionsDenied;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix android bug which occurs if newly installed app is run from the confirmation window
        if (!isTaskRoot()
            && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
            && getIntent().getAction() != null
            && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        auxiliaryFragment = (AuxiliaryFragment) fragmentManager.findFragmentByTag(TASKS_FRAGMENT_TAG);
        if (auxiliaryFragment == null) {
            auxiliaryFragment = new AuxiliaryFragment();
            fragmentManager.beginTransaction().add(auxiliaryFragment, TASKS_FRAGMENT_TAG).commit();
            savedInstanceState = null;
        }


        buildIsochroneButton = findViewById(R.id.build_isochrone_button);
        geopositionButton = findViewById(R.id.geoposition_button);
        progressBar = findViewById(R.id.progress_bar);
        blackoutView = findViewById(R.id.main_blackout_view);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        TransportType currentTransport = TransportType.valueOf(
                sharedPreferences.getString("currentTransport", "FOOT"));
        IsochroneRequestType currentRequestType = IsochroneRequestType.valueOf(
                sharedPreferences.getString("currentRequestType", "HEXAGONAL_COVER"));
        float seekBarProgress =  sharedPreferences.getFloat("seekBarProgress", 10);
        map.restoreCameraPosition(sharedPreferences);

        menu = (IsochroneMenu) fragmentManager.findFragmentByTag(MENU_FRAGMENT_TAG);
        if (menu == null) {
            menu = IsochroneMenu.newInstance(currentTransport, currentRequestType, seekBarProgress);
            fragmentManager.beginTransaction()
                           .replace(R.id.menu_placeholder, menu, MENU_FRAGMENT_TAG)
                           .commit();
        }

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

        menu.setOnPlaceQueryListener(
                position -> map.setCurrentPositionAndMoveCamera(position));

        menu.setOnScreenBlockListener(enable -> {
            if (enable) {
                showProgressBar();
            } else {
                hideProgressBar();
            }
        });

        menu.setAuxiliaryFragment(auxiliaryFragment);

        SupportMapFragment mapFragment = (SupportMapFragment) Objects.requireNonNull(
                getSupportFragmentManager().findFragmentById(R.id.map));
        map.setActivity(this);
        map.setAuxiliaryFragment(auxiliaryFragment);
        mapFragment.getMapAsync(map);

        buildIsochroneButton.setOnClickListener(ignored -> {
            menu.closeEverything();
            buildIsochrone();
        });

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        locationProvider = new CachedLocationProvider(this);
        menu.setCachedLocationProvider(locationProvider);
        map.setCachedLocationProvider(locationProvider);

        geopositionButton.setOnClickListener(ignored -> {
            showProgressBar();
            showBlackoutView();
            menu.closeEverything();

            if (!locationProvider.hasPermissions()) {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        GEOPOSITION_REQUEST);
                return;
            }
            locationProvider.getPreciseLocation(
                    result -> auxiliaryFragment.transferActionToMainActivity(
                            activity -> activity.gpsButtonCallback(result))
            );
        });

        if (!permissionsDenied && !locationProvider.hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    INITIAL_PERMISSIONS_REQUEST);
        }
    }

    // onRestoreInstanceState does not fit
    // because some parameters have to be restored in onCreate
    private void restoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        map.restoreInstanceState(savedInstanceState);
        auxiliaryFragment.setSavedPolygons(null);
        if (savedInstanceState.getBoolean(PROGRESS_BAR_STATE)) {
            showProgressBar();
        }
        if (savedInstanceState.getBoolean(BLACKOUT_VIEW_STATE)) {
            showBlackoutView();
        }
        permissionsDenied = savedInstanceState.getBoolean(PERMISSIONS_DENIED);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        map.saveInstanceState(outState);
        outState.putBoolean(PROGRESS_BAR_STATE, progressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean(BLACKOUT_VIEW_STATE, blackoutView.getVisibility() == View.VISIBLE);
        outState.putBoolean(PERMISSIONS_DENIED, permissionsDenied);
        super.onSaveInstanceState(outState);
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
        map.saveCameraPosition(sharedPreferences);
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

        if (permissions.length == 0) {
            return;
        }

        if (requestCode == GEOPOSITION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationProvider.getPreciseLocation(
                        result -> auxiliaryFragment.transferActionToMainActivity(
                                activity -> activity.gpsButtonCallback(result))
                );
            } else {
                hideProgressBar();
                hideBlackoutView();
                //FIXME move to Util class
                Toast toast = Toast.makeText(this, "give permissions please :(",
                        Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (requestCode == INITIAL_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (map.isReady()) {
                    locationProvider.getPreciseLocation(
                            result -> auxiliaryFragment.transferActionToMainActivity(
                                    activity -> activity.map.initialMove(result)));
                }
            } else {
                Toast toast = Toast.makeText(this, "give permissions please :(",
                                             Toast.LENGTH_LONG);
                toast.show();
                permissionsDenied = true;
            }
        }
    }

    void buildIsochrone() {
        map.removeCurrentPolygons();
        if (map.getCurrentPosition() == null) {
            Toast toast = Toast.makeText(
                    this,
                    "please choose location",
                    Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        showProgressBar();
        showBlackoutView();

        map.updateMarkerTitle("");
        map.executeAsyncMapRequest(
                new IsochroneRequest(
                        map.getCurrentPosition(),
                        getTravelTime(),
                        menu.getCurrentTransport(),
                        menu.getCurrentRequestType()));
    }

    private double getTravelTime() {
        return menu.getCurrentSeekBarProgress() / 60.0;
    }

    private void showBlackoutView() {
        blackoutView.setVisibility(View.VISIBLE);
    }

    private void hideBlackoutView() {
        blackoutView.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    public IsochroneMenu getMenu() {
        return menu;
    }

    //FIXME PLS!!!! mb move to enum?????
    private static String transportTypeInSentence(@NonNull TransportType transportType) {
        if (transportType == TransportType.FOOT) {
            return "on foot";
        } else {
            return "by " + transportType.name().toLowerCase();
        }
    }

    public void gpsButtonCallback(@NonNull Coordinate coordinate) {
        if (map.isReady()) {
            map.setCurrentPositionAndMoveCamera(coordinate);
        } else {
            map.addOnMapReadyAction(() -> gpsButtonCallback(coordinate));
        }
    }

    public void asyncMapRequestCallback(IsochroneResponse response) {
        if (!map.isReady()) {
            map.addOnMapReadyAction(() -> asyncMapRequestCallback(response));
            return;
        }

        hideProgressBar();
        hideBlackoutView();
        if (response.isSuccessful) {
            map.setCurrentPolygons(response.getResult());
            map.updateMarkerTitle(
                    Math.round(response.travelTime * 60)
                    + " min "
                    + transportTypeInSentence(Objects.requireNonNull(response.transportType)));
        } else {
            Toast toast = Toast.makeText(
                    MainActivity.this,
                    response.getErrorMessage(),
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void initLocationCallback(@NonNull Coordinate coordinate) {
        if (map.isReady()) {
            map.initialMove(coordinate);
        } else {
            map.addOnMapReadyAction(() -> initLocationCallback(coordinate));
        }
    }
}