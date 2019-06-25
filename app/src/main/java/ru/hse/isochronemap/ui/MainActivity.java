package ru.hse.isochronemap.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.isochronebuilding.IsochroneRequestType;
import ru.hse.isochronemap.location.IsochroneMapLocationManager;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.TransportType;
import ru.hse.isochronemap.util.Constants;
import ru.hse.isochronemap.util.IsochroneRequest;
import ru.hse.isochronemap.util.IsochroneResponse;

/** This is the main Activity of the application. */
public class MainActivity extends AppCompatActivity {
    // fragments' tags
    private static final String TASKS_FRAGMENT_TAG = "TASKS_FRAGMENT";
    private static final String MENU_FRAGMENT_TAG = "MENU_FRAGMENT";

    // keys that are used to save state in a Bundle
    private static final String PROGRESS_BAR_STATE_KEY = "PROGRESS_BAR_STATE";
    private static final String BLACKOUT_VIEW_STATE_KEY = "BLACKOUT_VIEW_STATE";
    private static final String PERMISSIONS_DENIED_KEY = "PERMISSIONS_DENIED";
    private static final String STATUS_TEXT_KEY = "STATUS_TEXT";

    // keys that are used to save current settings in SharedPreferences
    private static final String TRANSPORT_PREFERENCES_KEY = "currentTransport";
    private static final String REQUEST_TYPE_PREFERENCES_KEY = "currentRequestType";
    private static final String SEEK_BAR_PROGRESS_PREFERENCES_KEY = "seekBarProgress";

    // permissions request keys
    private static final int INITIAL_PERMISSIONS_REQUEST = 1;
    private static final int GEOLOCATION_REQUEST = 2;

    private SharedPreferences sharedPreferences;
    private AuxiliaryFragment auxiliaryFragment;
    private MapManager map = new MapManager();
    private IsochroneMenu menu;
    private IsochroneMapLocationManager locationManager;
    private boolean permissionsDenied;

    private Button cancelButton;
    private FloatingActionButton buildIsochroneButton;
    private FloatingActionButton geolocationButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private View blackoutView;

    /** {@inheritDoc} */
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

        cancelButton = findViewById(R.id.cancel_button);
        buildIsochroneButton = findViewById(R.id.build_isochrone_button);
        geolocationButton = findViewById(R.id.geolocation_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        blackoutView = findViewById(R.id.main_blackout_view);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        locationManager = new IsochroneMapLocationManager(this);

        initializeAuxiliaryFragment();
        if (auxiliaryFragment.wasDead()) {
            savedInstanceState = null;
        }

        initializeMenu();
        initializeMap();
        initializeButtonsListeners();

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        requestPermissionsInitiallyIfNeeded();
    }

    /** {@inheritDoc} */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        map.saveInstanceState(outState);
        outState.putBoolean(PROGRESS_BAR_STATE_KEY, progressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean(BLACKOUT_VIEW_STATE_KEY, blackoutView.getVisibility() == View.VISIBLE);
        outState.putBoolean(PERMISSIONS_DENIED_KEY, permissionsDenied);
        outState.putString(STATUS_TEXT_KEY, statusText.getText().toString());
        super.onSaveInstanceState(outState);
    }

    /** {@inheritDoc} */
    @Override
    public void onBackPressed() {
        if (!menu.closeEverything()) {
            super.onBackPressed();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences
                .edit()
                .putString(TRANSPORT_PREFERENCES_KEY, menu.getCurrentTransport().toString())
                .putString(REQUEST_TYPE_PREFERENCES_KEY, menu.getCurrentRequestType().toString())
                .putFloat(SEEK_BAR_PROGRESS_PREFERENCES_KEY, menu.getCurrentSeekBarProgress())
                .apply();
        map.saveCameraPosition(sharedPreferences);
    }

    /** {@inheritDoc} */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length == 0) {
            return;
        }

        if (requestCode == GEOLOCATION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                moveToCurrentLocation();
            } else {
                showToast(getString(R.string.give_permissions_toast));
            }
        } else if (requestCode == INITIAL_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (map.isReady()) {
                    UIBlockingTaskExecutor.executeApproximateLocationRequest(
                            auxiliaryFragment,
                            locationManager,
                            location -> auxiliaryFragment.transferActionToMainActivity(
                                    mainActivity -> mainActivity.initLocationCallback(location)));
                }
            } else {
                showToast(getString(R.string.give_permissions_toast));
                permissionsDenied = true;
            }
        }
    }

    /** This getter is used to transfer asynchronous callbacks to IsochroneMenu */
    public IsochroneMenu getMenu() {
        return menu;
    }

    /** This method is used as a callback to . */
    public void gpsButtonCallback(@NonNull Coordinate coordinate) {
        if (map.isReady()) {
            map.setCurrentPositionAndMoveCamera(coordinate);
        } else {
            map.addOnMapReadyAction(() -> gpsButtonCallback(coordinate));
        }
    }

    /** This method is used as a callback for asynchronous map request. */
    public void asyncMapRequestCallback(IsochroneResponse response) {
        if (!map.isReady()) {
            map.addOnMapReadyAction(() -> asyncMapRequestCallback(response));
            return;
        }

        hideOnBackgroundActionUI();

        if (response.isSuccessful) {
            map.setIsochronePolygons(response.getResult());
            map.updateMarkerTitle(
                    Math.round(response.travelTime * 60) + " min " + transportTypeInSentence(
                            Objects.requireNonNull(response.transportType)));
        } else {
            showToast(response.getErrorMessage());
        }
    }

    /** This method is used as a callback and invoked to set initial location. */
    public void initLocationCallback(@Nullable Coordinate coordinate) {
        if (coordinate == null) {
            showToast(getString(R.string.could_not_get_init_location_toast));
            return;
        }
        if (map.isReady()) {
            map.initialMove(coordinate);
        } else {
            map.addOnMapReadyAction(() -> initLocationCallback(coordinate));
        }
    }

    private void initializeAuxiliaryFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        auxiliaryFragment =
                (AuxiliaryFragment) fragmentManager.findFragmentByTag(TASKS_FRAGMENT_TAG);
        if (auxiliaryFragment == null) {
            auxiliaryFragment = new AuxiliaryFragment();
            fragmentManager.beginTransaction().add(auxiliaryFragment, TASKS_FRAGMENT_TAG).commit();
        }
    }

    private void initializeMap() {
        map.restoreCameraPosition(sharedPreferences);
        SupportMapFragment mapFragment = (SupportMapFragment) Objects
                .requireNonNull(getSupportFragmentManager().findFragmentById(R.id.map));
        map.setActivity(this);
        map.setAuxiliaryFragment(auxiliaryFragment);
        map.setIsochroneMapLocationManager(locationManager);
        mapFragment.getMapAsync(map);
    }

    private void initializeMenu() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        TransportType currentTransport = TransportType.valueOf(
                sharedPreferences.getString(TRANSPORT_PREFERENCES_KEY, TransportType.FOOT.name()));
        IsochroneRequestType currentRequestType = IsochroneRequestType.valueOf(
                sharedPreferences.getString(REQUEST_TYPE_PREFERENCES_KEY,
                                            IsochroneRequestType.HEXAGONAL_COVER.name()));
        float seekBarProgress =
                sharedPreferences.getFloat(SEEK_BAR_PROGRESS_PREFERENCES_KEY,
                                           UIConstants.DEFAULT_SEEK_BAR_PROGRESS);

        menu = (IsochroneMenu) fragmentManager.findFragmentByTag(MENU_FRAGMENT_TAG);
        if (menu == null) {
            menu = IsochroneMenu.newInstance(currentTransport, currentRequestType, seekBarProgress);
            fragmentManager.beginTransaction()
                           .replace(R.id.menu_placeholder, menu, MENU_FRAGMENT_TAG).commit();
        }

        if (currentRequestType == IsochroneRequestType.HEXAGONAL_COVER) {
            buildIsochroneButton.setImageResource(R.drawable.ic_hexagonal_button_24dp);
        } else {
            buildIsochroneButton.setImageResource(R.drawable.ic_convex_hull_button_24dp);
        }

        menu.setOnHexagonalCoverButtonClickListener(ignored -> buildIsochroneButton
                .setImageResource(R.drawable.ic_hexagonal_button_24dp));

        menu.setOnConvexHullButtonClickListener(ignored -> buildIsochroneButton
                .setImageResource(R.drawable.ic_convex_hull_button_24dp));

        menu.setOnPlaceQueryListener(position -> map.setCurrentPositionAndMoveCamera(position));
        menu.setAuxiliaryFragment(auxiliaryFragment);
        menu.setIsochroneMapLocationManager(locationManager);
    }

    private void initializeButtonsListeners() {
        buildIsochroneButton.setOnClickListener(ignored -> buildIsochrone());
        cancelButton.setOnClickListener(ignored -> cancelCurrentAction());
        geolocationButton.setOnClickListener(ignored -> {
            if (!locationManager.hasPermissions()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, GEOLOCATION_REQUEST);
                return;
            }
            moveToCurrentLocation();
        });
    }

    private void requestPermissionsInitiallyIfNeeded() {
        if (!permissionsDenied && !locationManager.hasPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, INITIAL_PERMISSIONS_REQUEST);
        }
    }

    // onRestoreInstanceState does not fit
    // because some parameters have to be restored in onCreate
    private void restoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        map.restoreInstanceState(savedInstanceState);
        auxiliaryFragment.setSavedPolygons(null);
        if (savedInstanceState.getBoolean(PROGRESS_BAR_STATE_KEY)) {
            showProgressBarWithText();
            showCancelButton(false);
        }
        if (savedInstanceState.getBoolean(BLACKOUT_VIEW_STATE_KEY)) {
            showBlackoutView();
        }
        permissionsDenied = savedInstanceState.getBoolean(PERMISSIONS_DENIED_KEY);
        statusText.setText(savedInstanceState.getString(STATUS_TEXT_KEY));
    }

    private void moveToCurrentLocation() {
        map.setCurrentPosition(null);
        map.removeIsochronePolygons();
        UIBlockingTaskExecutor.executeLocationRequest(
                auxiliaryFragment,
                locationManager,
                location -> auxiliaryFragment.transferActionToMainActivity(
                        mainActivity -> mainActivity.gpsButtonCallback(location)));
    }

    void buildIsochrone() {
        map.removeIsochronePolygons();
        if (map.getMarkerPosition() == null) {
            showToast(getString(R.string.choose_location_toast));
            return;
        }

        map.updateMarkerTitle("");
        UIBlockingTaskExecutor.executeIsochroneRequest(
                auxiliaryFragment,
                new IsochroneRequest(
                        map.getMarkerPosition(),
                        getTravelTime(),
                        menu.getCurrentTransport(),
                        menu.getCurrentRequestType()));
    }

    void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    void showOnBackgroundActionUI() {
        updateActionMessage("");

        showBlackoutView();
        showProgressBarWithText();
        showCancelButton(true);
    }

    void hideOnBackgroundActionUI() {
        hideBlackoutView();
        hideProgressBarWithText();
        hideCancelButton(true);
    }

    void updateActionMessage(@NonNull String message) {
        statusText.setText(message);
    }

    void cancelCurrentAction() {
        auxiliaryFragment.cancelCurrentAction();
        hideOnBackgroundActionUI();
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

    private void showProgressBarWithText() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
    }

    private void hideProgressBarWithText() {
        progressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
    }

    private void showCancelButton(boolean animate) {
        if (animate
            && cancelButton.getVisibility() == View.INVISIBLE
            && cancelButton.getTranslationY() == 0) {
            hideCancelButton(false);
        }
        cancelButton.setVisibility(View.VISIBLE);
        changeCancelButtonTranslationY(animate, 0);
    }

    private void hideCancelButton(boolean animate) {
        float newTranslationY = getResources().getDimension(R.dimen.ui_margin)
                                + cancelButton.getHeight();
        changeCancelButtonTranslationY(animate, newTranslationY);
    }

    private void changeCancelButtonTranslationY(boolean animate, float translationY) {
        if (!animate) {
            cancelButton.setTranslationY(translationY);
        } else {
            ObjectAnimator animator = ObjectAnimator.ofFloat(cancelButton,
                                                             UIConstants.TRANSLATION_Y_PROPERTY,
                                                             translationY);
            animator.setDuration(UIConstants.ANIMATION_DURATION);
            animator.start();
        }
    }

    private static String transportTypeInSentence(@NonNull TransportType transportType) {
        if (transportType == TransportType.FOOT) {
            return "on foot";
        } else {
            return "by " + transportType.name().toLowerCase();
        }
    }
}