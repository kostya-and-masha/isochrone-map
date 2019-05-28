package ru.hse.isochronemap.mapstructure;

/** This class represents parameters of map structure request. **/
public class MapStructureRequest {
    private Coordinate startCoordinate;
    private double unconditionalAccessDistance;
    private double maximumDistance;
    private TransportType transportType;

    public MapStructureRequest() {}

    public MapStructureRequest(Coordinate startCoordinate,
                               double unconditionalAccessDistance,
                               double maximumDistance,
                               TransportType transportType) {
        this.startCoordinate = startCoordinate;
        this.unconditionalAccessDistance = unconditionalAccessDistance;
        this.maximumDistance = maximumDistance;
        this.transportType = transportType;
    }

    public Coordinate getStartCoordinate() {
        return startCoordinate;
    }

    public void setStartCoordinate(Coordinate startCoordinate) {
        this.startCoordinate = startCoordinate;
    }

    public double getUnconditionalAccessDistance() {
        return unconditionalAccessDistance;
    }

    public void setUnconditionalAccessDistance(double unconditionalAccessDistance) {
        this.unconditionalAccessDistance = unconditionalAccessDistance;
    }

    public double getMaximumDistance() {
        return maximumDistance;
    }

    public void setMaximumDistance(double maximumDistance) {
        this.maximumDistance = maximumDistance;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }
}
