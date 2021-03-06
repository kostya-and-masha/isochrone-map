package ru.hse.isochronemap.mapstructure;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

/** Class represents list of available transport types. **/
public enum TransportType implements Serializable {
    FOOT(
            5,
            new RoadRestriction(
                    "highway",
                    RoadRestriction.Type.TAG_MATCHES,
                    RoadType.PEDESTRIAN,
                    RoadType.FOOTWAY,
                    RoadType.STEPS,
                    RoadType.PATH,
                    RoadType.ROAD,
                    RoadType.TRACK,
                    RoadType.RESIDENTIAL,
                    RoadType.LIVING_STREET,
                    RoadType.SERVICE,
                    RoadType.UNCLASSIFIED,
                    RoadType.PRIMARY,
                    RoadType.SECONDARY,
                    RoadType.TERTIARY,
                    RoadType.PRIMARY_LINK,
                    RoadType.SECONDARY_LINK,
                    RoadType.TERTIARY_LINK),
            new RoadRestriction(
                    "foot",
                    RoadRestriction.Type.TAG_DOES_NOT_EQUAL,
                    "no"),
            RoadRestriction.PUBLIC_ACCESS
    ),
    CAR(
            35,
            new RoadRestriction("highway"),
            new RoadRestriction(
                    "highway",
                    RoadRestriction.Type.TAG_DOES_NOT_MATCH,
                    RoadType.PEDESTRIAN,
                    RoadType.FOOTWAY,
                    RoadType.STEPS,
                    RoadType.PATH),
            RoadRestriction.PUBLIC_ACCESS
    ),
    BIKE(
            17,
            new RoadRestriction(
                    "highway",
                    RoadRestriction.Type.TAG_MATCHES,
                    RoadType.PEDESTRIAN,
                    RoadType.FOOTWAY,
                    RoadType.PATH,
                    RoadType.ROAD,
                    RoadType.TRACK,
                    RoadType.RESIDENTIAL,
                    RoadType.LIVING_STREET,
                    RoadType.SERVICE,
                    RoadType.UNCLASSIFIED,
                    RoadType.PRIMARY,
                    RoadType.SECONDARY,
                    RoadType.TERTIARY,
                    RoadType.PRIMARY_LINK,
                    RoadType.SECONDARY_LINK,
                    RoadType.TERTIARY_LINK),
            RoadRestriction.PUBLIC_ACCESS
    );

    private final double averageSpeed; // in km/h
    private final List<RoadRestriction> restrictions;

    TransportType(double averageSpeed, RoadRestriction ... restrictions) {
        this.averageSpeed = averageSpeed;
        this.restrictions = Arrays.asList(restrictions);
    }

    public @NonNull List<RoadRestriction> getRestrictions() {
        return restrictions;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }
}
