package com.example.isochronemap.mapstructure;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

class OverpassRequestBuilder {
    static String buildRequest(BoundingBox box,
                               TransportType transportType,
                               OverpassRequestType requestType) {
        String result = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(requestType.HEADING)
                   .append(boundingBoxString(box)).append(";")
                   .append("way");
            for (RoadRestriction restriction : transportType.getRestrictions()) {
                builder.append(restrictionString(restriction));
            }
            builder.append(";");
            builder.append(requestType.ENDING);
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
