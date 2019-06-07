package ru.hse.isochronemap.ui;

/** This enum holds predefined settings for IsochroneMenu seek bar */
enum TransportTypeSeekBarSettings {
    FOOT(5, 40, 8), BIKE(5, 15, 3), CAR(5, 15, 3);

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
