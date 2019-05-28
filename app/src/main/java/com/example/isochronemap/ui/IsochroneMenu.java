package com.example.isochronemap.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.isochronemap.R;
import com.example.isochronemap.geocoding.Consumer;
import com.example.isochronemap.geocoding.Geocoder;
import com.example.isochronemap.geocoding.Location;
import com.example.isochronemap.isochronebuilding.IsochroneRequestType;
import com.example.isochronemap.location.CoordinateConsumer;
import com.example.isochronemap.location.OneTimeLocationProvider;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.TransportType;
import com.example.isochronemap.searchhistory.SearchDatabase;
import com.example.isochronemap.util.CoordinateParser;
import com.warkiz.widget.IndicatorSeekBar;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class IsochroneMenu extends Fragment {
    private static final String MENU_MODE = "MENU_MODE";
    private static final String DATABASE_NAME = "HINTS_DB";
    private static final String SEARCH_FIELD_QUERY = "SEARCH_FIELD_QUERY";
    private static final String ADAPTER_MODE = "ADAPTER_MODE";
    private static final String ADAPTER_LIST = "ADAPTER_LIST";

    private View mainLayout;
    private SearchView searchField;

    private RecyclerView resultsRecycler;
    private SearchResultsAdapter adapter = new SearchResultsAdapter();
    private String currentQuery = "";
    private SearchDatabase database;

    private ConstraintLayout mainSettings;
    private ConstraintLayout additionalSettings;
    private ImageButton additionalSettingsButton;
    private ImageButton menuButton;
    private ImageButton walkingButton;
    private ImageButton bikeButton;
    private ImageButton carButton;
    private ImageButton convexHullButton;
    private ImageButton hexagonalCoverButton;
    private IndicatorSeekBar seekBar;

    private OnPlaceQueryListener onPlaceQueryListener = null;
    private View.OnClickListener onConvexHullButtonClickListener = null;
    private View.OnClickListener onHexagonalCoverButtonClickListener = null;

    private boolean isDrawn = false;
    private Mode currentMode = Mode.CLOSED;
    private ImageView blackoutView;

    private Float seekBarProgress = null;
    private TransportType currentTransport = TransportType.FOOT;
    private IsochroneRequestType currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;

    private enum Mode {
        CLOSED,
        MAIN_SETTING,
        ADDITIONAL_SETTINGS,
        SEARCH
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentMode = (Mode)savedInstanceState.getSerializable(MENU_MODE);
        }

        //Will not happen because onCreate is called after onAttach
        assert getActivity() != null;
        database = new SearchDatabase(getActivity(), DATABASE_NAME);
    }

    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.menu_main_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainLayout = view;
        init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putSerializable(MENU_MODE, currentMode);
        bundle.putSerializable(ADAPTER_MODE, adapter.getAdapterMode());
        bundle.putString(SEARCH_FIELD_QUERY, searchField.getQuery().toString());
        bundle.putSerializable(ADAPTER_LIST, adapter.getAdapterContentSerializable());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        database.close();
    }


    public void setPreferencesBeforeDrawn(TransportType transportType,
                                          IsochroneRequestType isochroneRequestType,
                                          float seekBarProgress) {
        if(isDrawn) {
            throw new IllegalStateException("Menu is already drawn");
        }

        currentTransport = transportType;
        currentRequestType = isochroneRequestType;
        this.seekBarProgress = seekBarProgress;
    }

    public TransportType getCurrentTransport() {
        return currentTransport;
    }

    public IsochroneRequestType getCurrentRequestType() {
        return currentRequestType;
    }

    public float getCurrentSeekBarProgress() {
        return seekBar.getProgressFloat();
    }

    public boolean closeEverything() {
        if (currentMode != Mode.CLOSED) {
            currentMode = Mode.CLOSED;
            updateModeUI(true);
            return true;
        }
        return false;
    }

    public void setOnConvexHullButtonClickListener(View.OnClickListener callerListener) {
        onConvexHullButtonClickListener = callerListener;
    }

    public void setOnHexagonalCoverButtonClickListener(View.OnClickListener callerListener) {
        onHexagonalCoverButtonClickListener = callerListener;
    }

    public interface OnPlaceQueryListener {
        void OnPlaceQuery(Coordinate coordinate);
    }

    public void setOnPlaceQueryListener(OnPlaceQueryListener listener) {
        onPlaceQueryListener = listener;
    }


    private void init(Bundle savedInstanceState) {
        mainSettings = mainLayout.findViewById(R.id.main_settings);
        additionalSettings = mainLayout.findViewById(R.id.additional_settings);
        additionalSettingsButton = mainLayout.findViewById(R.id.additional_settings_button);
        menuButton = mainLayout.findViewById(R.id.menu_button);
        walkingButton = mainLayout.findViewById(R.id.walking_button);
        bikeButton = mainLayout.findViewById(R.id.bike_button);
        carButton = mainLayout.findViewById(R.id.car_button);
        convexHullButton = mainLayout.findViewById(R.id.convex_hull_button);
        hexagonalCoverButton = mainLayout.findViewById(R.id.hexagonal_cover_button);
        seekBar = mainLayout.findViewById(R.id.seekBar);
        blackoutView = mainLayout.findViewById(R.id.blackout_view);

        menuButton.setOnClickListener(view -> {
            if (currentMode == Mode.CLOSED) {
                currentMode = Mode.MAIN_SETTING;
            } else {
                currentMode = Mode.CLOSED;
            }
            updateModeUI(true);
        });

        additionalSettingsButton.setOnClickListener(view -> {
            if (currentMode == Mode.ADDITIONAL_SETTINGS) {
                currentMode = Mode.MAIN_SETTING;
            } else {
                currentMode = Mode.ADDITIONAL_SETTINGS;
            }
            updateModeUI(true);
        });

        walkingButton.setOnClickListener(view -> {
            currentTransport = TransportType.FOOT;
            updateMainSettingUI();
        });

        carButton.setOnClickListener(view -> {
            currentTransport = TransportType.CAR;
            updateMainSettingUI();
        });

        bikeButton.setOnClickListener(view -> {
            currentTransport = TransportType.BIKE;
            updateMainSettingUI();
        });

        convexHullButton.setOnClickListener(view -> {
            if (onConvexHullButtonClickListener != null) {
                onConvexHullButtonClickListener.onClick(view);
            }
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
            updateAdditionalSettingUI();
        });

        hexagonalCoverButton.setOnClickListener(view -> {
            if (onHexagonalCoverButtonClickListener != null) {
                onHexagonalCoverButtonClickListener.onClick(view);
            }
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
            updateAdditionalSettingUI();
        });

        blackoutView.setOnClickListener(view -> {
            currentMode = Mode.CLOSED;
            updateModeUI(true);
        });

        initSearch(savedInstanceState);
        setUIUpdater();
    }

    private void initSearch(Bundle savedInstanceState) {
        searchField = mainLayout.findViewById(R.id.search_field);
        resultsRecycler = mainLayout.findViewById(R.id.results_list);

        Consumer<String> hintsUpdater = (hint -> {
            List<String> list = database.getSearchQueries(hint);
            adapter.setHints(list);
            if (isDrawn) {
                updateModeUI(true);
            }
        });

        if (savedInstanceState != null) {
            SearchResultsAdapter.AdapterMode mode =
                    (SearchResultsAdapter.AdapterMode)savedInstanceState
                            .getSerializable(ADAPTER_MODE);
            currentQuery = savedInstanceState.getString(SEARCH_FIELD_QUERY);

            if (mode == SearchResultsAdapter.AdapterMode.HINTS) {
                @SuppressWarnings("unchecked")
                List<String> content =
                        (List<String>) savedInstanceState.getSerializable(ADAPTER_LIST);
                adapter.setHints(content);
            } else {
                @SuppressWarnings("unchecked")
                List<Location> content =
                        (List<Location>) savedInstanceState.getSerializable(ADAPTER_LIST);
                adapter.setResults(content);
            }
        } else {
            hintsUpdater.accept(currentQuery);
        }

        searchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                database.putSearchQuery(query);
                Coordinate coordinate = CoordinateParser.parseCoordinate(query);

                if (coordinate != null) {
                    if (onPlaceQueryListener != null) {
                        onPlaceQueryListener.OnPlaceQuery(coordinate);
                    }
                    closeEverything();
                } else {
                    Consumer<List<Location>> onSuccessListener = list -> {
                        adapter.setResults(list);
                        updateModeUI(true);
                    };

                    Consumer<Exception> onFailureListener = exception -> {
                        Toast toast = Toast.makeText(getContext(),
                                "could not get search results",
                                Toast.LENGTH_LONG);
                        toast.show();
                    };

                    CoordinateConsumer coordinateCallback = position -> {
                            Geocoder.getLocations(query, position,
                                    onSuccessListener, onFailureListener);
                    };

                    if (OneTimeLocationProvider.hasPermissions(getContext())) {
                        OneTimeLocationProvider.getLocation(getContext(), coordinateCallback);
                    } else {
                        coordinateCallback.accept(null);
                    }
                }
                searchField.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals(currentQuery)) {
                    return true;
                }
                currentQuery = newText;
                hintsUpdater.accept(newText);
                return true;
            }
        });

        searchField.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                currentMode = Mode.SEARCH;
                if (isDrawn){
                    updateModeUI(true);
                }
            }
        });

        resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsRecycler.setAdapter(adapter);

        adapter.setOnResultClickListener(result -> {
            onPlaceQueryListener.OnPlaceQuery(result.coordinate);
            currentMode = Mode.CLOSED;
            updateModeUI(true);
        });

        adapter.setOnHintClickListener(hint -> {
            searchField.setQuery(hint, true);
        });

        adapter.setOnHintDeleteClickListener(hint -> {
            database.deleteSearchQuery(hint);
            hintsUpdater.accept(searchField.getQuery().toString());
        });
    }

    private void setUIUpdater() {
        mainLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mainLayout.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        updateAdditionalSettingUI();
                        updateMainSettingUI();
                        updateModeUI(false);
                        isDrawn = true;
                    }
                });
    }

    private void updateMainSettingUI() {
        ImageButton transportButton;

        float currentProgress =
                (seekBarProgress != null ? seekBarProgress : seekBar.getProgressFloat());
        seekBarProgress = null;

        switch (currentTransport) {
            //FIXME magic constants
            case FOOT:
                transportButton = walkingButton;
                seekBar.setMin(5);
                seekBar.setMax(40);
                seekBar.setTickCount(8);
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
        seekBar.setProgress(currentProgress);

        walkingButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
        bikeButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
        carButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));

        transportButton.setImageTintList(getContext().getColorStateList(R.color.colorDarkGrey));
    }

    private void updateAdditionalSettingUI() {
        ImageButton currentButton;
        ImageView currentBorder;
        ImageButton otherButton;
        ImageView otherBorder;
        switch (currentRequestType) {
            case CONVEX_HULL:
                currentButton = convexHullButton;
                currentBorder = mainLayout.findViewById(R.id.convex_hull_border);
                otherButton = hexagonalCoverButton;
                otherBorder = mainLayout.findViewById(R.id.hexagonal_cover_border);
                break;
            case HEXAGONAL_COVER:
                currentButton = hexagonalCoverButton;
                currentBorder = mainLayout.findViewById(R.id.hexagonal_cover_border);
                otherButton = convexHullButton;
                otherBorder = mainLayout.findViewById(R.id.convex_hull_border);
                break;
            default:
                throw new RuntimeException();
        }
        currentButton.setImageTintList(getContext().getColorStateList(R.color.colorDarkGrey));
        currentBorder.setImageTintList(getContext().getColorStateList(R.color.colorPrimaryDark));
        otherButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
        otherBorder.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
    }

    private void updateModeUI(boolean animate) {
        if (currentMode == Mode.CLOSED) {
            additionalSettingsButton.setVisibility(View.GONE);
            menuButton.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.VISIBLE);
            searchField.clearFocus();
            adjustCardHeightAndBlackout(animate, 0, false);
            return;
        }

        mainSettings.setVisibility(View.INVISIBLE);
        additionalSettings.setVisibility(View.INVISIBLE);
        resultsRecycler.setVisibility(View.INVISIBLE);

        switch(currentMode) {
            case MAIN_SETTING:
                mainSettings.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setImageTintList(
                        getContext().getColorStateList(R.color.colorPrimary));
                searchField.setVisibility(View.INVISIBLE);
                adjustCardHeightAndBlackout(animate, mainSettings.getHeight(), true);
                break;
            case ADDITIONAL_SETTINGS:
                additionalSettings.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setImageTintList(
                        getContext().getColorStateList(R.color.colorDarkGrey));
                searchField.setVisibility(View.INVISIBLE);
                adjustCardHeightAndBlackout(animate, mainSettings.getHeight(), true);
                break;
            case SEARCH:
                resultsRecycler.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.GONE);
                additionalSettingsButton.setVisibility(View.GONE);
                adjustCardHeightAndBlackout(true,
                        computeResultsRecyclerHeight(), true);
                break;
        }
    }

    private float computeResultsRecyclerHeight() {
        int itemCount = adapter.getItemCount();
        if (itemCount == 0) {
            return 0;
        }

        float padding = getResources().getDimension(R.dimen.card_hidden_part);
        float itemSize = getResources().getDimension(R.dimen.icons_size);
        return padding + itemCount * itemSize + padding;
    }

    private void adjustCardHeightAndBlackout(boolean animate,
                                             float contentHeight,
                                             boolean blackout) {
        CardView settingsCard = mainLayout.findViewById(R.id.settings_card);
        float translation = -settingsCard.getHeight() + contentHeight;
        if (translation > 0) translation = 0;
        float blackoutAlpha;

        if (blackout) {
            blackoutView.setClickable(true);
            blackoutAlpha = (float)0.7;
        } else {
            blackoutView.setClickable(false);
            blackoutAlpha = 0;
        }

        if (animate) {
            ObjectAnimator cardAnimation = ObjectAnimator.ofFloat(settingsCard,
                    "translationY", translation);
            ObjectAnimator blackoutAnimation = ObjectAnimator.ofFloat(blackoutView,
                    "alpha", blackoutAlpha);

            blackoutAnimation.setDuration(200);
            cardAnimation.setDuration(200);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(cardAnimation);
            animatorSet.play(blackoutAnimation);
            animatorSet.start();
        } else {
            settingsCard.setTranslationY(translation);
            blackoutView.setAlpha(blackoutAlpha);
        }
    }
}
