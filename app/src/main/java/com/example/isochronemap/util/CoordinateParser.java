package com.example.isochronemap.util;

import com.example.isochronemap.mapstructure.Coordinate;

public class CoordinateParser {
    private static String LATITUDE_REGEXP =
            "^[+-]?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$";
    private static String LONGITUDE_REGEXP =
            "^[+-]?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$";

    public static Coordinate parseCoordinate(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length != 2) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(parts[0]);
        if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        String latitude = stringBuilder.toString();
        String longitude = parts[1];

        if (!latitude.matches(LATITUDE_REGEXP)) {
            return null;
        }
        if (!longitude.matches(LONGITUDE_REGEXP)) {
            return null;
        }

        return new Coordinate(Double.parseDouble(latitude), Double.parseDouble(longitude));
    }
}
