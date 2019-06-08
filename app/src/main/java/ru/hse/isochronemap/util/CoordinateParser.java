package ru.hse.isochronemap.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.mapstructure.Coordinate;

/** This class provides static method which parses geographic coordinate. */
public class CoordinateParser {
    private static String LATITUDE_REGEXP =
            "^[+-]?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$";
    private static String LONGITUDE_REGEXP =
            "^[+-]?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$";

    /**
     * Parses geographic coordinate.
     *
     * @param input parses latitude and longitude separated by whitespace only
     *              or by comma followed by whitespace.
     * @return Coordinate on success or null if input could not be parsed.
     */
    public static @Nullable
    Coordinate parseCoordinate(@NonNull String input) {
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
