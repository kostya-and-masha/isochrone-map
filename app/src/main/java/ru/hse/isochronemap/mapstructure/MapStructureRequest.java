package ru.hse.isochronemap.mapstructure;

import androidx.annotation.NonNull;

/** This class represents parameters of map structure request. **/
public class MapStructureRequest {
    private Coordinate startCoordinate;
    private double unconditionalAccessDistance;
    private double maximumDistance;
    private TransportType transportType;

    /**
     * Constructs new map structure request.
     * @param startCoordinate travel start location
     * @param unconditionalAccessDistance all nodes closer to {@code startCoordinate} then
     *                                    this value are considered to be accessible from it.
     * @param maximumDistance limits map size
     * @param transportType transport type
     */
    public MapStructureRequest(@NonNull Coordinate startCoordinate,
                               double unconditionalAccessDistance,
                               double maximumDistance,
                               @NonNull TransportType transportType) {
        this.startCoordinate = startCoordinate;
        this.unconditionalAccessDistance = unconditionalAccessDistance;
        this.maximumDistance = maximumDistance;
        this.transportType = transportType;
    }

    @NonNull Coordinate getStartCoordinate() {
        return startCoordinate;
    }

    double getUnconditionalAccessDistance() {
        return unconditionalAccessDistance;
    }

    double getMaximumDistance() {
        return maximumDistance;
    }

    @NonNull TransportType getTransportType() {
        return transportType;
    }
}
