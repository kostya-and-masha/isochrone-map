package ru.hse.isochronemap.util;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.isochronebuilding.IsochronePolygon;
import ru.hse.isochronemap.mapstructure.TransportType;

/** This class represents response to isochrone request. **/
public class IsochroneResponse {
    public final boolean isSuccessful;
    public final List<IsochronePolygon> result;
    public final double travelTime;
    public final TransportType transportType;
    public final String errorMessage;

    public IsochroneResponse(@NonNull List<IsochronePolygon> polygons,
                              double travelTime,
                             @NonNull TransportType transportType) {
        isSuccessful = true;
        result = polygons;
        this.travelTime = travelTime;
        this.transportType = transportType;
        errorMessage = null;
    }

    public IsochroneResponse(@NonNull String message) {
        isSuccessful = false;
        result = null;
        travelTime = 0;
        transportType = null;
        errorMessage = message;
    }

    public @NonNull List<IsochronePolygon> getResult() {
        return Objects.requireNonNull(result);
    }

    public @NonNull String getErrorMessage() {
        return Objects.requireNonNull(errorMessage);
    }
}
