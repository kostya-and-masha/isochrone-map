package ru.hse.isochronemap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

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

    private static final String TASKS_FRAGMENT_TAG = "TASKS_FRAGMENT";
    private static final String CAMERA_POSITION = "CAMERA_POSITION";
    private static final String MARKER_POSITION = "MARKER_POSITION";
    private static final String MARKER_TITLE = "MARKER_TITLE";
    private static final String PROGRESS_BAR_STATE = "PROGRESS_BAR_STATE";
    private static final String BLACKOUT_VIEW_STATE = "BLACKOUT_VIEW_STATE";
    private static final String PERMISSIONS_DENIED = "PERMISSIONS_DENIED";
    public static final int INITIAL_PERMISSIONS_REQUEST = 1;
    public static final int GEOPOSITION_REQUEST = 2;

    private AuxiliaryFragment auxiliaryFragment;

    private CameraPosition initialCameraPosition;
    private LatLng initialMarkerPosition;
    private String initialMarkerTitle;
    private Runnable onMapReadyAction;
    private ArrayList<PolygonOptions> currentPolygonOptions;

    private GoogleMap map;
    private Marker currentPosition;
    private List<Polygon> currentPolygons = new ArrayList<>();


    private IsochroneMenu menu;
    private FloatingActionButton buildIsochroneButton;
    private FloatingActionButton geopositionButton;
    private ProgressBar progressBar;
    private View blackoutView;

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
        }

        menu = (IsochroneMenu)getSupportFragmentManager().findFragmentById(R.id.menu);
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

        menu.setOnPlaceQueryListener(this::setCurrentPositionAndMoveCamera);

        menu.setOnScreenBlockListener(enable -> {
            if (enable) {
                showProgressBar();
            } else {
                hideProgressBar();
            }
        });

        menu.setAuxiliaryFragment(auxiliaryFragment);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildIsochroneButton.setOnClickListener(ignored -> {
            menu.closeEverything();
            buildIsochrone();
        });

        geopositionButton.setOnClickListener(ignored -> {
            showProgressBar();
            showBlackoutView();
            menu.closeEverything();
            //FIXME magic constant
            if (!OneTimeLocationProvider.hasPermissions(this)) {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        GEOPOSITION_REQUEST);
                return;
            }
            OneTimeLocationProvider.getLocation(this,
                    result -> auxiliaryFragment.transferActionToMainActivity(
                            activity -> activity.gpsButtonCallback(result))
            );
        });

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        if (!permissionsDenied && !OneTimeLocationProvider.hasPermissions(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    INITIAL_PERMISSIONS_REQUEST);
        }
    }

    // onRestoreInstanceState does not fit
    // because some parameters have to be restored in onCreate
    private void restoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        initialCameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
        initialMarkerPosition = savedInstanceState.getParcelable(MARKER_POSITION);
        initialMarkerTitle = savedInstanceState.getString(MARKER_TITLE);
        currentPolygonOptions = auxiliaryFragment.getSavedPolygons();
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
        if (map != null) {
            resetPadding();
            outState.putParcelable(CAMERA_POSITION, map.getCameraPosition());
            setPadding();
        }
        if (currentPosition != null) {
            outState.putParcelable(MARKER_POSITION, currentPosition.getPosition());
            outState.putString(MARKER_TITLE, currentPosition.getTitle());
        }
        auxiliaryFragment.setSavedPolygons(currentPolygonOptions);
        outState.putBoolean(PROGRESS_BAR_STATE, progressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean(BLACKOUT_VIEW_STATE, blackoutView.getVisibility() == View.VISIBLE);
        outState.putBoolean(PERMISSIONS_DENIED, permissionsDenied);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapLongClickListener(position -> setCurrentPosition(new Coordinate(position)));

        if (initialCameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(initialCameraPosition));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    START_POSITION, DEFAULT_ZOOM_LEVEL
            ));
        }

        setPadding();

        if (initialMarkerPosition != null) {
            currentPosition = map.addMarker(new MarkerOptions().position(initialMarkerPosition));
            updateMarkerTitle(initialMarkerTitle);
        }

        if (currentPolygonOptions != null) {
            for (PolygonOptions options : currentPolygonOptions) {
                currentPolygons.add(map.addPolygon(options));
            }
        } else {
            currentPolygonOptions = new ArrayList<>();
        }

        //zhopa
        if (onMapReadyAction != null) {
            onMapReadyAction.run();
            onMapReadyAction = null;
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

        if (permissions.length == 0) {
            return;
        }

        if (requestCode == GEOPOSITION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                OneTimeLocationProvider.getLocation(this,
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
        } else if (requestCode == INITIAL_PERMISSIONS_REQUEST
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(this, "give permissions please :(",
                    Toast.LENGTH_LONG);
            toast.show();
            permissionsDenied = true;
        }
    }

    private void buildIsochrone() {
        removeCurrentPolygons();
        if (currentPosition == null) {
            Toast toast = Toast.makeText(
                    this,
                    "please choose location",
                    Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        showProgressBar();
        showBlackoutView();

        updateMarkerTitle("");
        new AsyncMapRequest(auxiliaryFragment).execute(
                new IsochroneRequest(
                        new Coordinate(currentPosition.getPosition()),
                        getTravelTime(),
                        menu.getCurrentTransport(),
                        menu.getCurrentRequestType()
                )
        );
    }

    private void setPadding() {
        int marginTop = convertDpToPixels(80);
        int marginSide = convertDpToPixels(5);
        map.setPadding(marginSide, marginTop, marginSide, 0);
    }

    private void resetPadding() {
        map.setPadding(0, 0, 0, 0);
    }

    public int convertDpToPixels(float dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void setCurrentPosition(Coordinate position) {
        if (currentPosition == null) {
            currentPosition = map.addMarker(new MarkerOptions().position(position.toLatLng()));
        } else {
            currentPosition.setPosition(position.toLatLng());
        }
        currentPosition.setTitle("");
        currentPosition.hideInfoWindow();
        buildIsochrone();
    }

    private void setCurrentPositionAndMoveCamera(Coordinate position) {
        setCurrentPosition(position);
        map.animateCamera(
                CameraUpdateFactory.newLatLng(currentPosition.getPosition()),
                2000,
                null
        );
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

    private void updateMarkerTitle(String title) {
        currentPosition.setTitle(title);
        if (title.equals("")) {
            currentPosition.hideInfoWindow();
        } else {
            currentPosition.showInfoWindow();
        }
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

    public void gpsButtonCallback(Coordinate coordinate) {
        if (map == null) {
            onMapReadyAction = () -> gpsButtonCallback(coordinate);
            return;
        }

        setCurrentPositionAndMoveCamera(coordinate);
    }

    private static class AsyncMapRequest extends AsyncTask<IsochroneRequest, Integer, IsochroneResponse> {
        AuxiliaryFragment auxiliaryFragment;

        private AsyncMapRequest(AuxiliaryFragment auxiliaryFragment) {
            super();
            this.auxiliaryFragment = auxiliaryFragment;
        }

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
                        ),
                        request.travelTime,
                        request.transportType
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
            auxiliaryFragment.transferActionToMainActivity(activity ->
                    activity.asyncMapRequestCallback(response));
        }
    }

    public void asyncMapRequestCallback(IsochroneResponse response) {
        if (map == null) {
            onMapReadyAction = () -> asyncMapRequestCallback(response);
            return;
        }

        hideProgressBar();
        hideBlackoutView();
        if (response.isSuccessful) {
            setCurrentPolygons(response.getResult());
            updateMarkerTitle(Math.round(
                    response.travelTime * 60) +
                    " min " +
                    transportTypeInSentence(response.transportType));
        } else {
            Toast toast = Toast.makeText(
                    MainActivity.this,
                    response.getErrorMessage(),
                    Toast.LENGTH_LONG);
            toast.show();
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

    static class IsochroneResponse {
        private final boolean isSuccessful;
        private final List<IsochronePolygon> result;
        private final double travelTime;
        private final TransportType transportType;
        private final String errorMessage;

        private IsochroneResponse(List<IsochronePolygon> polygons,
                                  double travelTime, TransportType transportType) {
            isSuccessful = true;
            result = polygons;
            this.travelTime = travelTime;
            this.transportType = transportType;
            errorMessage = null;
        }

        private IsochroneResponse(String message) {
            isSuccessful = false;
            result = null;
            travelTime = 0;
            transportType = null;
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
