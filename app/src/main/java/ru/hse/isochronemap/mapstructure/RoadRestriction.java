package ru.hse.isochronemap.mapstructure;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

class RoadRestriction {
    static final RoadRestriction PUBLIC_ACCESS = new RoadRestriction(
            "access",
            RoadRestriction.Type.TAG_DOES_NOT_MATCH,
            "no",
            "private"
    );

    private final String tag;
    private final Type restrictionType;
    private final List<String> options = new ArrayList<>();

    RoadRestriction(@NonNull String tag) {
        this.tag = tag;
        restrictionType = Type.TAG_EXISTS;
    }

    RoadRestriction(@NonNull String tag,
                    @NonNull Type restrictionType,
                    @NonNull Object... options) {
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

        private final String view;

        Type(@NonNull String view) {
            this.view = view;
        }

        @Override
        public @NonNull String toString() {
            return view;
        }
    }
}
