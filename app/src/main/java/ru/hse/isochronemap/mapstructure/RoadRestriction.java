package ru.hse.isochronemap.mapstructure;

import java.util.ArrayList;
import java.util.List;

class RoadRestriction {
    // TODO extend restriction
    static final RoadRestriction PUBLIC_ACCESS = new RoadRestriction(
            "access",
            RoadRestriction.Type.TAG_DOES_NOT_MATCH,
            "no",
            "private"
    );

    private String tag;
    private Type restrictionType;
    private List<String> options = new ArrayList<>();

    RoadRestriction(String tag) {
        this.tag = tag;
        restrictionType = Type.TAG_EXISTS;
    }

    RoadRestriction(String tag, Type restrictionType, Object... options) {
        this.tag = tag;
        this.restrictionType = restrictionType;
        for (Object option : options) {
            this.options.add(option.toString());
        }
    }

    String getTag() {
        return tag;
    }

    String getRestrictionType() {
        return restrictionType.toString();
    }

    List<String> getOptions() {
        return options;
    }

    public enum Type {
        TAG_EXISTS(""),
        TAG_EQUALS("="),
        TAG_DOES_NOT_EQUAL("!="),
        TAG_MATCHES("~"),
        TAG_DOES_NOT_MATCH("!~");

        private String view;

        Type(String view) {
            this.view = view;
        }

        @Override
        public String toString() {
            return view;
        }
    }
}
