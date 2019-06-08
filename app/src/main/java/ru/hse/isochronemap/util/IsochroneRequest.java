package ru.hse.isochronemap.util;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.isochronebuilding.IsochroneRequestType;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.mapstructure.TransportType;

/** This class represents isochrone request. **/
public class IsochroneRequest {
    public final Coordinate coordinate;
    public final double travelTime;
    public final TransportType transportType;
    public final IsochroneRequestType isochroneType;

    public IsochroneRequest(@NonNull Coordinate coordinate,
                             double travelTime,
                             @NonNull TransportType transportType,
                             @NonNull IsochroneRequestType isochroneType) {
        this.coordinate = coordinate;
        this.travelTime = travelTime;
        this.transportType = transportType;
        this.isochroneType = isochroneType;
    }
}
