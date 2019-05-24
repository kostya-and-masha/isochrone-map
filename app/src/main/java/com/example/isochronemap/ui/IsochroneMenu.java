package com.example.isochronemap.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;

import com.example.isochronemap.R;
import com.example.isochronemap.geocoding.Geocoder;
import com.example.isochronemap.isochronebuilding.IsochroneRequestType;
import com.example.isochronemap.mapstructure.Coordinate;
import com.example.isochronemap.mapstructure.TransportType;
import com.example.isochronemap.util.CoordinateParser;
import com.example.isochronemap.util.TEMP_DummyResult;
import com.warkiz.widget.IndicatorSeekBar;

import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class IsochroneMenu extends ConstraintLayout {
    private View menuMainLayout;
    private SearchView searchField;

    private RecyclerView resultsRecycler;
    private SearchResultsAdapter adapter;

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

    private enum Mode {
        CLOSED,
        MAIN_SETTING,
        ADDITIONAL_SETTINGS,
        SEARCH
    }

    private boolean searchResultsSet = false;
    private boolean isDrawn = false;
    private Mode currentMode = Mode.CLOSED;
    private ImageView blackoutView;

    private float seekBarProgress = 0;
    private TransportType currentTransport = TransportType.FOOT;
    private IsochroneRequestType currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;

    public IsochroneMenu(Context context) {
        this(context, null);
    }

    public IsochroneMenu(Context context, AttributeSet attributes) {
        super(context, attributes);
        init(attributes);
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

    public void setOnConvexHullButtonClickListener(OnClickListener callerListener) {
        convexHullButton.setOnClickListener(a -> {
            callerListener.onClick(a);
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
            updateAdditionalSettingUI();
        });
    }

    public void setOnHexagonalCoverButtonClickListener(OnClickListener callerListener) {
        hexagonalCoverButton.setOnClickListener(a -> {
            callerListener.onClick(a);
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
            updateAdditionalSettingUI();
        });
    }

    public interface OnPlaceQueryListener {
        void OnPlaceQuery(Coordinate coordinate);
    }

    public void setOnPlaceQueryListener(OnPlaceQueryListener listener) {
        onPlaceQueryListener = listener;
    }


    private void init(AttributeSet attributes) {
        menuMainLayout = inflate(getContext(), R.layout.menu_main_layout, this);
        searchField = findViewById(R.id.search_field);

        resultsRecycler = findViewById(R.id.results_list);
        adapter = new SearchResultsAdapter();

        mainSettings = findViewById(R.id.main_settings);
        additionalSettings = findViewById(R.id.additional_settings);
        additionalSettingsButton = findViewById(R.id.additional_settings_button);
        menuButton = findViewById(R.id.menu_button);
        walkingButton = findViewById(R.id.walking_button);
        bikeButton = findViewById(R.id.bike_button);
        carButton = findViewById(R.id.car_button);
        convexHullButton = findViewById(R.id.convex_hull_button);
        hexagonalCoverButton = findViewById(R.id.hexagonal_cover_button);
        seekBar = findViewById(R.id.seekBar);
        blackoutView = findViewById(R.id.blackout_view);

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
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
            updateAdditionalSettingUI();
        });

        hexagonalCoverButton.setOnClickListener(view -> {
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
            updateAdditionalSettingUI();
        });

        searchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Coordinate coordinate = CoordinateParser.parseCoordinate(query);
                if (coordinate != null) {
                    if (onPlaceQueryListener != null) {
                        onPlaceQueryListener.OnPlaceQuery(coordinate);
                    }
                    currentMode = Mode.CLOSED;
                    updateModeUI(true);
                } else {
                    List<TEMP_DummyResult> list = TEMP_DummyResult.getMultipleDummies(30);
                    adapter.setItems(list);
                    updateModeUI(true);
                }
                clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<TEMP_DummyResult> list = TEMP_DummyResult.getMultipleDummies(10);
                adapter.setItems(list);
                searchResultsSet = true;
                if (isDrawn) {
                    updateModeUI(true);
                }
                return true;
            }
        });

        searchField.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                currentMode = Mode.SEARCH;
                if (!searchResultsSet) {
                    searchField.setQuery(searchField.getQuery(), false);
                } else if (isDrawn){
                    updateModeUI(true);
                }
            }
        });

        blackoutView.setOnClickListener(view -> {
            currentMode = Mode.CLOSED;
            updateModeUI(true);
        });

        resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsRecycler.setAdapter(adapter);

        adapter.setOnResultClickListener(result -> {
            onPlaceQueryListener.OnPlaceQuery(result.coordinate);
            currentMode = Mode.CLOSED;
            updateModeUI(true);
        });

        setUIUpdater();
    }

    private void setUIUpdater() {
        menuMainLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        menuMainLayout.getViewTreeObserver()
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
        switch (currentTransport) {
            //FIXME magic constants
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
        seekBar.setProgress(seekBarProgress);

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
                currentBorder = findViewById(R.id.convex_hull_border);
                otherButton = hexagonalCoverButton;
                otherBorder = findViewById(R.id.hexagonal_cover_border);
                break;
            case HEXAGONAL_COVER:
                currentButton = hexagonalCoverButton;
                currentBorder = findViewById(R.id.hexagonal_cover_border);
                otherButton = convexHullButton;
                otherBorder = findViewById(R.id.convex_hull_border);
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
            additionalSettingsButton.setVisibility(GONE);
            menuButton.setVisibility(VISIBLE);
            searchField.setVisibility(VISIBLE);
            searchField.clearFocus();
            adjustCardHeightAndBlackout(animate, 0, false);
            return;
        }

        mainSettings.setVisibility(INVISIBLE);
        additionalSettings.setVisibility(INVISIBLE);
        resultsRecycler.setVisibility(INVISIBLE);

        switch(currentMode) {
            case MAIN_SETTING:
                mainSettings.setVisibility(VISIBLE);
                menuButton.setVisibility(VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setImageTintList(
                        getContext().getColorStateList(R.color.colorPrimary));
                searchField.setVisibility(View.INVISIBLE);
                adjustCardHeightAndBlackout(animate, mainSettings.getHeight(), true);
                break;
            case ADDITIONAL_SETTINGS:
                additionalSettings.setVisibility(VISIBLE);
                menuButton.setVisibility(VISIBLE);
                additionalSettingsButton.setVisibility(View.VISIBLE);
                additionalSettingsButton.setImageTintList(
                        getContext().getColorStateList(R.color.colorDarkGrey));
                searchField.setVisibility(View.INVISIBLE);
                adjustCardHeightAndBlackout(animate, mainSettings.getHeight(), true);
                break;
            case SEARCH:
                resultsRecycler.setVisibility(VISIBLE);
                menuButton.setVisibility(GONE);
                additionalSettingsButton.setVisibility(GONE);
                adjustCardHeightAndBlackout(true, computeResultsRecyclerHeight(), true);
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

    static class SavedState extends BaseSavedState {
        private Mode currentMode = Mode.CLOSED;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentMode = (Mode)in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSerializable(currentMode);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);

        savedState.currentMode = currentMode;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        currentMode = savedState.currentMode;
    }

    private void adjustCardHeightAndBlackout(boolean animate,
                                             float contentHeight,
                                             boolean blackout) {
        CardView settingsCard = findViewById(R.id.settings_card);
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
