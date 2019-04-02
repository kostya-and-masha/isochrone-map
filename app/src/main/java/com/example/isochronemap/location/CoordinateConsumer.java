package com.example.isochronemap.location;

import com.example.isochronemap.mapstructure.Coordinate;

//FIXME add javadoc
public interface CoordinateConsumer {
    void accept(Coordinate coordinate);
}
