package ru.hse.isochronemap.mapstructure;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.isochronebuilding.IsochroneBuilder;
import ru.hse.isochronemap.util.IsochroneRequest;

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

    /** Constructs map structure request from isochrone request. **/
    public MapStructureRequest(@NonNull IsochroneRequest request) {
        this(request.coordinate,
             IsochroneBuilder.UNCONDITIONAL_ACCESS_DISTANCE,
             request.transportType.getAverageSpeed() * request.travelTime,
             request.transportType);
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
