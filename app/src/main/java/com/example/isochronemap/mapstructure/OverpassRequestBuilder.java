package com.example.isochronemap.mapstructure;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

class OverpassRequestBuilder {
    private static final String HEADING = "[out: json]";
    private static final String ENDING = "(._;>;);out body qt;";

    static String buildRequest(BoundingBox box, TransportType type) {
        String result = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(HEADING)
                   .append(boundingBoxString(box)).append(";")
                   .append("way");
            for (RoadRestriction restriction : type.getRestrictions()) {
                builder.append(restrictionString(restriction));
            }
            builder.append(";");
            builder.append(ENDING);
            result = builder.toString();
            result = URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
        return result;
    }

    private static String boundingBoxString(BoundingBox box) {
        return "[bbox:" +
                Double.toString(box.minimum.latitudeDeg) + "," +
                Double.toString(box.minimum.longitudeDeg) + "," +
                Double.toString(box.maximum.latitudeDeg) + "," +
                Double.toString(box.maximum.longitudeDeg) + "]";
    }

    private static String restrictionString(RoadRestriction restriction) {
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
