package com.example.isochronemap.mapstructure;

import org.jetbrains.annotations.NotNull;

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
    UNCLASSIFIED("unclassified");

    private String stringName;

    RoadType(String name) {
        stringName = name;
    }

    @Override
    public @NotNull String toString() {
        return stringName;
    }
}
