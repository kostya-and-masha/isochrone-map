package com.example.isochronemap.geocoding;

import com.example.isochronemap.mapstructure.Coordinate;

/** This class represents location suggested by {@link Geocoder}. **/
public class Location {
    public final String name;
    public final Coordinate coordinate;

    public Location(String name, Coordinate coordinate) {
        this.name = name;
        this.coordinate = coordinate;
    }
}
