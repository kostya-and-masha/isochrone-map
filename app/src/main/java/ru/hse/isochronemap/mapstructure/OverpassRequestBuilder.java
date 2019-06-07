package ru.hse.isochronemap.mapstructure;

import java.util.Iterator;

import androidx.annotation.NonNull;

class OverpassRequestBuilder {
    static @NonNull String buildRequest(@NonNull BoundingBox box,
                                        @NonNull TransportType transportType,
                                        @NonNull OverpassRequestType requestType) {
        StringBuilder builder = new StringBuilder();
        builder.append(requestType.HEADING)
               .append(boundingBoxString(box)).append(";")
               .append("way");
        for (RoadRestriction restriction : transportType.getRestrictions()) {
            builder.append(restrictionString(restriction));
        }
        builder.append(";");
        builder.append(requestType.ENDING);
        return builder.toString();
    }

    private static @NonNull String boundingBoxString(@NonNull BoundingBox box) {
        return "[bbox:"
               + Double.toString(box.minimum.latitude) + ","
               + Double.toString(box.minimum.longitude) + ","
               + Double.toString(box.maximum.latitude) + ","
               + Double.toString(box.maximum.longitude) + "]";
    }

    private static @NonNull String restrictionString(@NonNull RoadRestriction restriction) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\"").append(restriction.getTag()).append("\"")
               .append(restriction.getRestrictionType());
        Iterator<String> it = restriction.getOptions().iterator();
        if (it.hasNext()) {
            builder.append("\"")
                   .append(it.next());
            while (it.hasNext()) {
                builder.append("|").append(it.next());
            }
            builder.append("\"");
        }
        builder.append("]");
        return builder.toString();
    }
}
