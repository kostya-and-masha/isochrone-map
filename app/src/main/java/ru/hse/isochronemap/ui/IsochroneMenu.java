package ru.hse.isochronemap.ui;

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

import com.warkiz.widget.IndicatorSeekBar;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.geocoding.Location;
import ru.hse.isochronemap.isochronebuilding.IsochroneRequestType;
import ru.hse.isochronemap.location.IsochroneMapLocationManager;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.TransportType;
import ru.hse.isochronemap.searchhistory.SearchDatabase;
import ru.hse.isochronemap.util.Consumer;
import ru.hse.isochronemap.util.CoordinateParser;

/** This fragment provides upper floating search/settings bar of the application. */
public class IsochroneMenu extends Fragment {
    // keys that are used to save state in a Bundle.
    private static final String MENU_MODE_KEY = "MENU_MODE";
    private static final String DATABASE_NAME_KEY = "HINTS_DB";
    private static final String SEARCH_FIELD_QUERY_KEY = "SEARCH_FIELD_QUERY";
    private static final String ADAPTER_MODE_KEY = "ADAPTER_MODE";
    private static final String ADAPTER_LIST_KEY = "ADAPTER_LIST";
    private static final String TRANSPORT_TYPE_KEY = "TRANSPORT_TYPE";
    private static final String ISOCHRONE_REQUEST_TYPE_KEY = "ISOCHRONE_REQUEST_TYPE";
    private static final String SEEK_BAR_PROGRESS_KEY = "SEEK_BAR_PROGRESS";

    private static final float BLACKOUT_VIEW_ENABLED_ALPHA = (float) 0.7;

    private View mainLayout;
    private SearchView searchField;

    private AuxiliaryFragment auxiliaryFragment;

    private RecyclerView resultsRecycler;
    private SearchResultsAdapter adapter = new SearchResultsAdapter();
    private String currentQuery = "";
    private SearchDatabase database;
    private IsochroneMapLocationManager isochroneMapLocationManager;

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

    private OnPlaceQueryListener onPlaceQueryListener;
    private View.OnClickListener onConvexHullButtonClickListener;
    private View.OnClickListener onHexagonalCoverButtonClickListener;

    private boolean isDrawn = false;
    private Mode currentMode = Mode.CLOSED;
    private View blackoutView;

    private Float seekBarProgress = null;
    private TransportType currentTransport = TransportType.FOOT;
    private IsochroneRequestType currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;

    /**
     * Creates IsochroneMenu instance with specified initial preferences. Both
     * IsochroneMapLocationManager and AuxiliaryFragment must be set after creation
     * (through specialized setters) in order for menu to work properly.
     */
    static @NonNull IsochroneMenu newInstance(@NonNull TransportType transportType,
                                              @NonNull IsochroneRequestType isochroneRequestType,
                                              float seekBarProgress) {
        IsochroneMenu menu = new IsochroneMenu();
        Bundle arguments = new Bundle();
        arguments.putSerializable(TRANSPORT_TYPE_KEY, transportType);
        arguments.putSerializable(ISOCHRONE_REQUEST_TYPE_KEY, isochroneRequestType);
        arguments.putFloat(SEEK_BAR_PROGRESS_KEY, seekBarProgress);
        menu.setArguments(arguments);
        return menu;
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyArguments();

        if (savedInstanceState != null) {
            currentMode = (Mode) savedInstanceState.getSerializable(MENU_MODE_KEY);
            currentTransport = (TransportType) savedInstanceState.getSerializable(
                    TRANSPORT_TYPE_KEY);
            currentRequestType = (IsochroneRequestType) savedInstanceState
                    .getSerializable(ISOCHRONE_REQUEST_TYPE_KEY);
            seekBarProgress = savedInstanceState.getFloat(SEEK_BAR_PROGRESS_KEY);
        }

        // Will not happen because onCreate is called after onAttach
        assert getActivity() != null;
        database = new SearchDatabase(getActivity(), DATABASE_NAME_KEY);
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.menu_main_layout, container, false);
    }

    /** {@inheritDoc} */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainLayout = view;
        initialize(savedInstanceState);
    }

    /** {@inheritDoc} */
    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putSerializable(MENU_MODE_KEY, currentMode);
        bundle.putSerializable(ADAPTER_MODE_KEY, adapter.getAdapterMode());
        bundle.putString(SEARCH_FIELD_QUERY_KEY, searchField.getQuery().toString());

        if (adapter.getAdapterMode() == SearchResultsAdapter.AdapterMode.HINTS) {
            bundle.putStringArrayList(ADAPTER_LIST_KEY, adapter.getHintsList());
        } else {
            bundle.putParcelableArrayList(ADAPTER_LIST_KEY, adapter.getResultsList());
        }

        bundle.putSerializable(TRANSPORT_TYPE_KEY, currentTransport);
        bundle.putSerializable(ISOCHRONE_REQUEST_TYPE_KEY, currentRequestType);
        bundle.putFloat(SEEK_BAR_PROGRESS_KEY, seekBar.getProgressFloat());
    }

    /** Returns current chosen transport type. */
    @NonNull TransportType getCurrentTransport() {
        return currentTransport;
    }

    /** Returns current chosen isochrone type. */
    @NonNull IsochroneRequestType getCurrentRequestType() {
        return currentRequestType;
    }

    /** Returns current seek bar value. */
    float getCurrentSeekBarProgress() {
        return seekBar.getProgressFloat();
    }

    /** Closes drop down menu. */
    boolean closeEverything() {
        if (currentMode != Mode.CLOSED) {
            currentMode = Mode.CLOSED;
            updateModeUI(true);
            return true;
        }
        return false;
    }

    /**
     * Sets {@link AuxiliaryFragment} which is used to deliver callbacks back to IsochroneMenu
     * even when fragment is recreated due to configuration changes. Must be set in order for menu
     * to work properly.
     */
    void setAuxiliaryFragment(@NonNull AuxiliaryFragment auxiliaryFragment) {
        this.auxiliaryFragment = auxiliaryFragment;
    }

    /**
     * Sets {@link IsochroneMapLocationManager} which is used to obtain approximate location
     * (necessary for place searching). Must be set in order for menu to work properly.
     */
    void setIsochroneMapLocationManager(@NonNull IsochroneMapLocationManager provider) {
        isochroneMapLocationManager = provider;
    }

    /** Sets listener which will be called if user chooses ConvexHull isochrone type. */
    void setOnConvexHullButtonClickListener(@Nullable View.OnClickListener callerListener) {
        onConvexHullButtonClickListener = callerListener;
    }

    /** Sets listener which will be called if user chooses HexagonalCover isochrone type. */
    void setOnHexagonalCoverButtonClickListener(@Nullable View.OnClickListener callerListener) {
        onHexagonalCoverButtonClickListener = callerListener;
    }

    /** {@see OnPlaceQueryListener} */
    void setOnPlaceQueryListener(@Nullable OnPlaceQueryListener listener) {
        onPlaceQueryListener = listener;
    }

    private void applyArguments() {
        if (getArguments() == null) {
            return;
        }

        currentTransport = (TransportType) getArguments().getSerializable(TRANSPORT_TYPE_KEY);
        if (currentTransport == null) {
            currentTransport = TransportType.FOOT;
        }

        currentRequestType =
                (IsochroneRequestType) getArguments().getSerializable(ISOCHRONE_REQUEST_TYPE_KEY);
        if (currentRequestType == null) {
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
        }

        seekBarProgress = getArguments().getFloat(SEEK_BAR_PROGRESS_KEY,
                                                  UIConstants.DEFAULT_SEEK_BAR_PROGRESS);
    }

    private void initialize(@Nullable Bundle savedInstanceState) {
        initializeViews();
        initializeSearch(savedInstanceState);
        setUIUpdater();
    }

    private void initializeViews() {
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
        blackoutView = mainLayout.findViewById(R.id.menu_blackout_view);

        initializeButtonsListeners();
    }

    private void initializeButtonsListeners() {
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
    }

    private void initializeSearch(Bundle savedInstanceState) {
        searchField = mainLayout.findViewById(R.id.search_field);
        resultsRecycler = mainLayout.findViewById(R.id.results_list);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsRecycler.setAdapter(adapter);

        Consumer<String> hintsUpdater = (hint -> {
            List<String> list = database.getSearchQueries(hint);
            adapter.setHints(list);
            if (isDrawn) {
                updateModeUI(true);
            }
        });

        if (savedInstanceState != null) {
            restoreAdapterAndQuery(savedInstanceState);
        } else {
            hintsUpdater.accept(currentQuery);
        }

        searchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                database.putSearchQuery(query);
                adapter.setHints(Collections.emptyList());
                updateModeUI(true);

                Coordinate coordinate = CoordinateParser.parseCoordinate(query);
                if (coordinate != null) {
                    if (onPlaceQueryListener != null) {
                        onPlaceQueryListener.OnPlaceQuery(coordinate);
                    }
                    closeEverything();
                } else {
                    submitPlaceName(query);
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
                if (isDrawn) {
                    updateModeUI(true);
                }
            }
        });

        adapter.setOnResultClickListener(result -> {
            onPlaceQueryListener.OnPlaceQuery(result.coordinate);
            currentMode = Mode.CLOSED;
            updateModeUI(true);
        });

        adapter.setOnHintClickListener(hint -> searchField.setQuery(hint, true));

        adapter.setOnHintDeleteClickListener(hint -> {
            database.deleteSearchQuery(hint);
            hintsUpdater.accept(searchField.getQuery().toString());
        });
    }

    private void restoreAdapterAndQuery(@NonNull Bundle savedInstanceState) {
        SearchResultsAdapter.AdapterMode mode =
                (SearchResultsAdapter.AdapterMode) savedInstanceState.getSerializable(
                        ADAPTER_MODE_KEY);

        currentQuery = savedInstanceState.getString(SEARCH_FIELD_QUERY_KEY);

        if (mode == SearchResultsAdapter.AdapterMode.HINTS) {
            List<String> content = savedInstanceState.getStringArrayList(ADAPTER_LIST_KEY);
            adapter.setHints(content);
        } else {
            List<Location> content = savedInstanceState.getParcelableArrayList(ADAPTER_LIST_KEY);
            adapter.setResults(content);
        }
    }

    private void submitPlaceName(String query) {
        Consumer<List<Location>> deliverToOnSuccessByAuxiliaryFragment =
                list -> auxiliaryFragment.transferActionToMainActivity(
                        activity -> activity.getMenu().onSuccessSearchResultsCallback(list));

        Runnable deliverToOnFailureByAuxiliaryFragment =
                () -> auxiliaryFragment.transferActionToMainActivity(
                        activity -> activity.getMenu().onFailureSearchResultsCallback());

        UIBlockingTaskExecutor.executeApproximateLocationRequest(
                auxiliaryFragment,
                isochroneMapLocationManager,
                location -> UIBlockingTaskExecutor.executeGeocodingRequest(
                        auxiliaryFragment,
                        query,
                        location,
                        deliverToOnSuccessByAuxiliaryFragment,
                        deliverToOnFailureByAuxiliaryFragment));
    }

    private void onSuccessSearchResultsCallback(@NonNull List<Location> list) {
        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

        adapter.setResults(list);
        if (list.size() == 0) {
            Toast.makeText(getContext(), getContext().getString(R.string.no_search_results_toast),
                           Toast.LENGTH_LONG).show();
        }
        updateModeUI(true);
    }

    private void onFailureSearchResultsCallback() {
        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

        Toast.makeText(getContext(), getContext().getString(R.string.search_failed_toast),
                       Toast.LENGTH_LONG).show();
    }

    private void setUIUpdater() {
        mainLayout.getViewTreeObserver()
                  .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                      @Override
                      public void onGlobalLayout() {
                          mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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

        UIConstants.TransportTypeSeekBarSettings settings;
        switch (currentTransport) {
            case FOOT:
                transportButton = walkingButton;
                settings = UIConstants.TransportTypeSeekBarSettings.FOOT;
                break;
            case BIKE:
                transportButton = bikeButton;
                settings = UIConstants.TransportTypeSeekBarSettings.BIKE;
                break;
            case CAR:
                transportButton = carButton;
                settings = UIConstants.TransportTypeSeekBarSettings.CAR;
                break;
            default:
                throw new RuntimeException();
        }

        seekBar.setMin(settings.getMinValue());
        seekBar.setMax(settings.getMaxValue());
        seekBar.setTickCount(settings.getTickCount());
        seekBar.setProgress(currentProgress);

        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

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

        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

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

        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

        switch (currentMode) {
            case MAIN_SETTING:
                mainSettings.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton
                        .setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
                searchField.setVisibility(View.INVISIBLE);
                adjustCardHeightAndBlackout(animate, mainSettings.getHeight(), true);
                break;
            case ADDITIONAL_SETTINGS:
                additionalSettings.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton
                        .setImageTintList(getContext().getColorStateList(R.color.colorDarkGrey));
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
                                             float contentHeight, boolean blackout) {
        CardView settingsCard = mainLayout.findViewById(R.id.settings_card);
        float translation = -settingsCard.getHeight() + contentHeight;
        if (translation > 0) translation = 0;
        float blackoutAlpha;

        if (blackout) {
            blackoutView.setClickable(true);
            blackoutAlpha = BLACKOUT_VIEW_ENABLED_ALPHA;
        } else {
            blackoutView.setClickable(false);
            blackoutAlpha = 0;
        }

        if (animate) {
            ObjectAnimator cardAnimation = ObjectAnimator.ofFloat(
                    settingsCard, UIConstants.TRANSLATION_Y_PROPERTY, translation);
            ObjectAnimator blackoutAnimation =
                    ObjectAnimator.ofFloat(blackoutView, UIConstants.ALPHA_PROPERTY, blackoutAlpha);

            blackoutAnimation.setDuration(UIConstants.ANIMATION_DURATION);
            cardAnimation.setDuration(UIConstants.ANIMATION_DURATION);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(cardAnimation);
            animatorSet.play(blackoutAnimation);
            animatorSet.start();
        } else {
            settingsCard.setTranslationY(translation);
            blackoutView.setAlpha(blackoutAlpha);
        }
    }

    private enum Mode {
        CLOSED, MAIN_SETTING, ADDITIONAL_SETTINGS, SEARCH
    }

    /** This listener is called when user chooses place in search results. */
    public interface OnPlaceQueryListener {
        void OnPlaceQuery(@NonNull Coordinate coordinate);
    }
}
