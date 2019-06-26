package ru.hse.isochronemap.util;

import org.junit.Test;

import ru.hse.isochronemap.mapstructure.Coordinate;

import static org.junit.Assert.*;

public class CoordinateParserTest {
    @Test
    public void testParsesWithLongWhitespace() {
        double latitude = 59.923996;
        double longitude = 30.360171;
        Coordinate expectedCoordinate = new Coordinate(latitude, longitude);
        Coordinate parsedCoordinate =
                CoordinateParser.parseCoordinate(" " + latitude + " \t  \t" + longitude + " ");
        assertTrue(expectedCoordinate.equalsWithPrecision(parsedCoordinate));
    }

    @Test
    public void testParsesWithComma() {
        double latitude = 48.857579;
        double longitude = 2.351382;
        Coordinate expectedCoordinate = new Coordinate(latitude, longitude);
        Coordinate parsedCoordinate =
                CoordinateParser.parseCoordinate(latitude + ", \t " + longitude);
        assertTrue(expectedCoordinate.equalsWithPrecision(parsedCoordinate));
    }

    @Test
    public void testParsesNegativeValues() {
        double latitude = -34.618034;
        double longitude = -58.417158;
        Coordinate expectedCoordinate = new Coordinate(latitude, longitude);
        Coordinate parsedCoordinate =
                CoordinateParser.parseCoordinate(latitude + " " + longitude);
        assertTrue(expectedCoordinate.equalsWithPrecision(parsedCoordinate));
    }

    @Test
    public void testDoesNotParseWrongLatitude() {
        double latitude = 91;
        double longitude = 0;
        Coordinate parsedCoordinate =
                CoordinateParser.parseCoordinate(latitude + " " + longitude);
        assertNull(parsedCoordinate);
    }

    @Test
    public void testDoesNotParseWrongLongitude() {
        double latitude = 0;
        double longitude = 181;
        Coordinate parsedCoordinate =
                CoordinateParser.parseCoordinate(latitude + " " + longitude);
        assertNull(parsedCoordinate);
    }
}