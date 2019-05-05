package com.example.isochronemap.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;

import com.example.isochronemap.R;
import com.example.isochronemap.isochronebuilding.IsochroneRequestType;
import com.example.isochronemap.mapstructure.TransportType;
import com.warkiz.widget.IndicatorSeekBar;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class IsochroneMenu extends ConstraintLayout {
    private View menuMainLayout;
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


    private boolean menuButtonIsActivated = false;
    private boolean additionalSettingsButtonIsActivated = false;

    TransportType currentTransport = TransportType.FOOT;
    IsochroneRequestType currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;

    public IsochroneMenu(Context context) {
        this(context, null);
    }

    public IsochroneMenu(Context context, AttributeSet attributes) {
        super(context, attributes);
        init(attributes);
    }

    public void setCurrentPreferencesWithoutAnimation(TransportType transportType,
                                                      IsochroneRequestType isochroneRequestType,
                                                      float seekBarProgress) {
        currentTransport = transportType;
        currentRequestType = isochroneRequestType;
        updateUIWithoutAnimation();
        seekBar.setProgress(seekBarProgress);
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

    public void openMenu() {
        if (!menuButtonIsActivated) {
            menuButton.callOnClick();
        }
    }

    public void closeMenu() {
        if (menuButtonIsActivated) {
            menuButton.callOnClick();
        }
    }

    public void handleClickOutside(MotionEvent event) {
        Rect rectMenuBar = new Rect();
        Rect rectSettings = new Rect();
        findViewById(R.id.menu_bar_card).getGlobalVisibleRect(rectMenuBar);
        findViewById(R.id.settings_card).getGlobalVisibleRect(rectSettings);
        if (!rectMenuBar.contains((int) event.getX(), (int) event.getY())
                && !rectSettings.contains((int) event.getX(), (int) event.getY())) {
            closeMenu();
        }
    }

    public void setOnConvexHullButtonClickListener(OnClickListener callerListener) {
        convexHullButton.setOnClickListener(a -> {
            callerListener.onClick(a);
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
            updateIsochroneTypeDependingUI();
        });
    }

    public void setOnHexagonalCoverButtonClickListener(OnClickListener callerListener) {
        hexagonalCoverButton.setOnClickListener(a -> {
            callerListener.onClick(a);
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
            updateIsochroneTypeDependingUI();
        });
    }

    public void setOnSearchBarQueryTextListener(SearchView.OnQueryTextListener listener) {
        searchField.setOnQueryTextListener(listener);
    }

    private void init(AttributeSet attributes) {
        menuMainLayout = inflate(getContext(), R.layout.menu_main_layout, this);
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

        menuButton.setOnClickListener((a) -> {
            menuButtonIsActivated = !menuButtonIsActivated;
            if (menuButtonIsActivated & additionalSettingsButtonIsActivated) {
                additionalSettingsButtonIsActivated = false;
                updateAdditionalSettingsUI();
            }
            updateMenuStateUI(true);
        });

        settingsButton.setOnClickListener((a) -> {
            additionalSettingsButtonIsActivated = !additionalSettingsButtonIsActivated;
            updateAdditionalSettingsUI();
        });

        walkingButton.setOnClickListener((a) -> {
            currentTransport = TransportType.FOOT;
            updateTransportDependingUI();
        });

        carButton.setOnClickListener((a) -> {
            currentTransport = TransportType.CAR;
            updateTransportDependingUI();
        });

        bikeButton.setOnClickListener((a) -> {
            currentTransport = TransportType.BIKE;
            updateTransportDependingUI();
        });

        convexHullButton.setOnClickListener((a) -> {
            currentRequestType = IsochroneRequestType.CONVEX_HULL;
            updateIsochroneTypeDependingUI();
        });

        hexagonalCoverButton.setOnClickListener((a) -> {
            currentRequestType = IsochroneRequestType.HEXAGONAL_COVER;
            updateIsochroneTypeDependingUI();
        });

        updateUIWithoutAnimation();
    }

    private void updateUIWithoutAnimation() {
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
    }

    private void updateTransportDependingUI() {
        ImageButton transportButton;
        float progress = seekBar.getProgressFloat();
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
        seekBar.setProgress(progress);

        walkingButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
        bikeButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
        carButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));

        transportButton.setImageTintList(getContext().getColorStateList(R.color.colorDarkGrey));
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

    private void updateAdditionalSettingsUI() {
        if (additionalSettingsButtonIsActivated) {
            settingsButton.setImageTintList(getContext().getColorStateList(R.color.colorDarkGrey));
            mainSettings.setVisibility(View.INVISIBLE);
            additionalSettings.setVisibility(View.VISIBLE);
        } else {
            settingsButton.setImageTintList(getContext().getColorStateList(R.color.colorPrimary));
            additionalSettings.setVisibility(View.INVISIBLE);
            mainSettings.setVisibility(View.VISIBLE);
        }
    }

    private void updateMenuStateUI(boolean animate) {
        if (menuButtonIsActivated) {
            settingsButton.setVisibility(View.VISIBLE);
            searchField.setVisibility(View.INVISIBLE);
            showSettingsCard(animate);
        } else {
            settingsButton.setVisibility(View.GONE);
            searchField.setVisibility(View.VISIBLE);
            hideSettingsCard(animate);
        }
    }

    static class SavedState extends BaseSavedState {
        private boolean menuButtonIsActivated = false;
        private boolean settingsButtonIsActivated = false;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            boolean[] booleanArray = new boolean[2];
            in.readBooleanArray(booleanArray);
            menuButtonIsActivated = booleanArray[0];
            settingsButtonIsActivated = booleanArray[1];
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeBooleanArray(new boolean[] {menuButtonIsActivated, settingsButtonIsActivated});
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

        savedState.menuButtonIsActivated = this.menuButtonIsActivated;
        savedState.settingsButtonIsActivated = this.additionalSettingsButtonIsActivated;

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

        this.menuButtonIsActivated = savedState.menuButtonIsActivated;
        this.additionalSettingsButtonIsActivated = savedState.settingsButtonIsActivated;

        updateUIWithoutAnimation();
    }

    private void showSettingsCard(boolean animate) {
        CardView settingsCard = findViewById(R.id.settings_card);
        if (animate) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(settingsCard,
                    "translationY", mainSettings.getHeight());
            animation.setDuration(300);
            animation.start();
        } else {
            settingsCard.setTranslationY(mainSettings.getHeight());
        }
    }

    private void hideSettingsCard(boolean animate) {
        CardView settingsCard = findViewById(R.id.settings_card);
        if (animate) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(settingsCard,
                    "translationY", 0);
            animation.setDuration(300);
            animation.start();
        } else {
            settingsCard.setTranslationY(0);
        }
    }

}
