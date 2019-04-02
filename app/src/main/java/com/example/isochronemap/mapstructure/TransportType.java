package com.example.isochronemap.mapstructure;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/** Class represents list of available transport types. **/
public enum TransportType implements Serializable {
    FOOT(
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
                    RoadType.TERTIARY_LINK
            ),
            new RoadRestriction(
                    "foot",
                    RoadRestriction.Type.TAG_DOES_NOT_EQUAL,
                    "no"
            ),
            RoadRestriction.PUBLIC_ACCESS
    ),
    CAR(
            new RoadRestriction("highway"),
            new RoadRestriction(
                    "highway",
                    RoadRestriction.Type.TAG_DOES_NOT_MATCH,
                    RoadType.PEDESTRIAN,
                    RoadType.FOOTWAY,
                    RoadType.STEPS,
                    RoadType.PATH
            ),
            RoadRestriction.PUBLIC_ACCESS
    ),
    BIKE {
        // TODO implementation
        @Override
        public List<RoadRestriction> getRestrictions() {
            throw new UnsupportedOperationException();
        }
    };

    private List<RoadRestriction> restrictions;

    TransportType(RoadRestriction ... restrictions) {
        this.restrictions = Arrays.asList(restrictions);
    }

    public List<RoadRestriction> getRestrictions() {
        return restrictions;
    }
}
