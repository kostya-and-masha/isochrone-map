package com.example.isochronemap;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.isochronemap.isochronebuilding.IsochroneBuilder;
import com.example.isochronemap.isochronebuilding.IsochronePolygon;
import com.example.isochronemap.isochronebuilding.IsochroneRequestType;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.warkiz.widget.IndicatorSeekBar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM_LEVEL = 10;
    private static final float CLOSE_ZOOM_LEVEL = 14;
    private static final LatLng START_POSITION = new LatLng(59.980547, 30.324066);

    private GoogleMap map;
    private Marker currentPosition;
    private List<Polygon> currentPolygons = new ArrayList<>();

    private SearchView searchField;
    private ConstraintLayout mainSettings;
    private ConstraintLayout additionalSettings;
    private ImageButton settingsButton;
    private ImageButton menuButton;
    private ImageButton walkingButton;
    private ImageButton bikeButton;
    private ImageButton carButton;
    private ImageButton convexHullButton;
    private ImageButton hexagonalCoverButton;
    private IndicatorSeekBar seekBar;

    SharedPreferences sharedPreferences;
    private boolean menuButtonIsActivated = false;
    private boolean settingsButtonIsActivated = false;
    TransportType currentTransport = TransportType.FOOT;
    IsochroneRequestType currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchField = findViewById(R.id.search_field);
        mainSettings = findViewById(R.id.main_settings);
        additionalSettings = findViewById(R.id.additional_settings);
        settingsButton = findViewById(R.id.settings_button);
        menuButton = findViewById(R.id.menu_button);
        walkingButton = findViewById(R.id.walking_button);
        bikeButton = findViewById(R.id.bike_button);
        carButton = findViewById(R.id.car_button);
        convexHullButton = findViewById(R.id.convex_hull_button);
        hexagonalCoverButton = findViewById(R.id.hexagonal_cover_button);
        seekBar = findViewById(R.id.seekBar);

        if (savedInstanceState != null) {
            menuButtonIsActivated = savedInstanceState.getBoolean("menuButtonState");
            settingsButtonIsActivated = savedInstanceState.getBoolean("settingsButtonState");
        }

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        currentTransport = TransportType.valueOf(
                sharedPreferences.getString("currentTransport", "FOOT"));
        currentRequestType = IsochroneRequestType.valueOf(
                sharedPreferences.getString("currentRequestType", "HEXAGONAL_COVER"));
        seekBar.setProgress(
                sharedPreferences.getFloat("seekBarProgress", 10));

        updateIsochroneTypeDependingUI();
        updateTransportDependingUI();
        updateAdditionalSettingsUI();
        findViewById(R.id.settings_card).getViewTreeObserver()
                .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                findViewById(R.id.settings_card).getViewTreeObserver()
                        .removeOnPreDrawListener(this);
                updateMenuStateUI(false);
                return true;
            }
        });

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

        map.setPadding(14, 180, 14, 0);
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
        removeCurrentPolygons();
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
                        currentTransport,
                        currentRequestType
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

    public void transportButtonsClick(View view) {
        if (walkingButton.equals(view)) {
            currentTransport = TransportType.FOOT;
        } else if (bikeButton.equals(view)) {
            currentTransport = TransportType.BIKE;
        } else if (carButton.equals(view)) {
            currentTransport = TransportType.CAR;
        }
        updateTransportDependingUI();
    }

    public void isochroneTypeButtonClick(View view) {
        if (convexHullButton.equals(view)) {
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
        } else if (hexagonalCoverButton.equals(view)) {
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
        }
        updateIsochroneTypeDependingUI();
    }

    public void toggleSettings(View view) {
        settingsButtonIsActivated ^= true;
        updateAdditionalSettingsUI();
    }

    public void toggleMenu(View view) {
        menuButtonIsActivated ^= true;
        if (menuButtonIsActivated) {
            settingsButtonIsActivated = false;
            updateAdditionalSettingsUI();
        }
        updateMenuStateUI(true);
    }



    public void positionButton(View view) {
        if (menuButtonIsActivated) {
            toggleMenu(menuButton);
        }
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
        if (menuButtonIsActivated) {
            toggleMenu(menuButton);
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        //FIXME magic constant literals
        sharedPreferences.edit()
                .putString("currentTransport", currentTransport.toString())
                .putString("currentRequestType", currentRequestType.toString())
                .putFloat("seekBarProgress", seekBar.getProgressFloat())
                .apply();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("menuButtonState", menuButtonIsActivated);
        bundle.putBoolean("settingsButtonState", settingsButtonIsActivated);
    }

    private void updateTransportDependingUI() {
        ImageButton transportButton;
        float progress = seekBar.getProgressFloat();
        switch (currentTransport) {
            case FOOT:
                transportButton = walkingButton;
                seekBar.setMin(10);
                seekBar.setMax(40);
                seekBar.setTickCount(7);
                break;
            case CAR:
                transportButton = carButton;
                seekBar.setMin(5);
                seekBar.setMax(15);
                seekBar.setTickCount(3);
                break;
            case BIKE:
                transportButton = bikeButton;
                seekBar.setMin(5);
                seekBar.setMax(15);
                seekBar.setTickCount(3);
                break;
            default:
                throw new RuntimeException();
        }
        seekBar.setProgress(progress);

        walkingButton.setImageTintList(getResources().getColorStateList(
                R.color.colorPrimary, getTheme()));
        bikeButton.setImageTintList(getResources().getColorStateList(
                R.color.colorPrimary, getTheme()));
        carButton.setImageTintList(getResources().getColorStateList(
                R.color.colorPrimary, getTheme()));

        transportButton.setImageTintList(
                getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
    }

    private void updateIsochroneTypeDependingUI() {
        ImageButton currentButton;
        ImageView currentBorder;
        ImageButton otherButton;
        ImageView otherBorder;
        switch (currentRequestType) {
            case CONVEX_HULL:
                currentButton = convexHullButton;
                currentBorder = findViewById(R.id.convex_hull_border);
                otherButton = hexagonalCoverButton;
                otherBorder = findViewById(R.id.hexagonal_cover_border);
                ((ImageButton)findViewById(R.id.update_isochrone_button)).setImageResource(
                        R.drawable.ic_convex_hull_button_24dp);
                break;
            case HEXAGONAL_COVER:
                currentButton = hexagonalCoverButton;
                currentBorder = findViewById(R.id.hexagonal_cover_border);
                otherButton = convexHullButton;
                otherBorder = findViewById(R.id.convex_hull_border);
                ((ImageButton)findViewById(R.id.update_isochrone_button)).setImageResource(
                        R.drawable.ic_hexagonal_button_24dp);
                break;
            default:
                throw new RuntimeException();
        }
        currentButton.setImageTintList(
                getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
        currentBorder.setImageTintList(
                getResources().getColorStateList(R.color.colorPrimaryDark, getTheme()));
        otherButton.setImageTintList(
                getResources().getColorStateList(R.color.colorPrimary, getTheme()));
        otherBorder.setImageTintList(
                getResources().getColorStateList(R.color.colorPrimary, getTheme()));
    }

    private void updateAdditionalSettingsUI() {
        if (settingsButtonIsActivated) {
            settingsButton.setImageTintList(
                    getResources().getColorStateList(R.color.colorDarkGrey, getTheme()));
            mainSettings.setVisibility(View.INVISIBLE);
            additionalSettings.setVisibility(View.VISIBLE);
        } else {
            settingsButton.setImageTintList(
                    getResources().getColorStateList(R.color.colorPrimary, getTheme()));
            additionalSettings.setVisibility(View.INVISIBLE);
            mainSettings.setVisibility(View.VISIBLE);
        }
    }

    private void updateMenuStateUI(boolean animate) {
        if (menuButtonIsActivated) {
            settingsButton.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.INVISIBLE);
            openSettings(animate);
        } else {
            settingsButton.setVisibility(View.GONE);
            searchField.setVisibility(View.VISIBLE);
            closeSettings(animate);
        }
    }

    private void openSettings(boolean animate) {
        CardView settingsCard = findViewById(R.id.settings_card);
        if (animate) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(settingsCard,
                    "translationY", mainSettings.getHeight());
            animation.setDuration(400);
            animation.start();
        } else {
            settingsCard.setTranslationY(mainSettings.getHeight());
        }
    }

    private void closeSettings(boolean animate) {
        CardView settingsCard = findViewById(R.id.settings_card);
        if (animate) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(settingsCard,
                    "translationY", 0);
            animation.setDuration(400);
            animation.start();
        } else {
            settingsCard.setTranslationY(0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
