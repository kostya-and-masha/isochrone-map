package ru.hse.isochronemap.mapstructure;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;

enum RoadType {
    PEDESTRIAN("pedestrian"),
    FOOTWAY("footway"),
    STEPS("steps"),
    ROAD("road"),
    PATH("path"),
    TRACK("track"),
    RESIDENTIAL("residential"),
    LIVING_STREET("living_street"),
    SERVICE("service"),
    UNCLASSIFIED("unclassified"),
    PRIMARY("primary"),
    SECONDARY("secondary"),
    TERTIARY("tertiary"),
    PRIMARY_LINK("primary_link"),
    SECONDARY_LINK("secondary_link"),
    TERTIARY_LINK("tertiary_link");

    private String stringName;

    RoadType(@NonNull String name) {
        stringName = name;
    }

    @Override
    public @NotNull String toString() {
        return stringName;
    }
}
