package ru.hse.isochronemap.ui;

/** Constants that are used by multiple classes in UI package. **/
class UIConstants {
    /** Duration for all UI animations in milliseconds. */
    static final int ANIMATION_DURATION = 200;

    /** translationY property name. */
    static final String TRANSLATION_Y_PROPERTY = "translationY";

    /** alpha property name. */
    static final String ALPHA_PROPERTY = "alpha";

    /** Default progress for seek bars. */
    static final int DEFAULT_SEEK_BAR_PROGRESS = 10;

    /** This enum holds predefined settings for IsochroneMenu seek bar */
    enum TransportTypeSeekBarSettings {
        FOOT(5, 40, 8),
        BIKE(5, 15, 3),
        CAR(5, 15, 3);

        private final float minValue;
        private final float maxValue;
        private final int tickCount;

        TransportTypeSeekBarSettings(float minValue, float maxValue, int tickCount) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.tickCount = tickCount;
        }

        float getMinValue() {
            return minValue;
        }

        float getMaxValue() {
            return maxValue;
        }

        int getTickCount() {
            return tickCount;
        }
    }
}
