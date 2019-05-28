package ru.hse.isochronemap.isochronebuilding;

import ru.hse.isochronemap.mapstructure.Coordinate;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

//TODO add javadoc
public class IsochronePolygon {
    private List<Coordinate> exteriorRing = new ArrayList<>();
    private List<List<Coordinate>> interiorRings = new ArrayList<>();

    public IsochronePolygon() {
    }

    //TODO check whether such parameter names are acceptable
    public IsochronePolygon(Polygon JTSPolygon, double ignoredHolesArea) {
        for (org.locationtech.jts.geom.Coordinate JTSCoordinate :
                JTSPolygon.getExteriorRing().getCoordinates()) {
            exteriorRing.add(new Coordinate(JTSCoordinate.y, JTSCoordinate.x));
        }
        GeometryFactory geometryFactory = new GeometryFactory();
        for (int i = 0; i < JTSPolygon.getNumInteriorRing(); i++) {
            if (geometryFactory.createPolygon(JTSPolygon.getInteriorRingN(i).getCoordinates())
                    .getArea() < ignoredHolesArea) {
                continue;
            }
            List<Coordinate> coordinateList = new ArrayList<>();
            for (org.locationtech.jts.geom.Coordinate JTSCoordinate :
                    JTSPolygon.getInteriorRingN(i).getCoordinates()) {
                coordinateList.add(new Coordinate(JTSCoordinate.y, JTSCoordinate.x));
            }
            interiorRings.add(coordinateList);
        }
    }

    public void setExteriorRing(@NotNull List<Coordinate> newRing) {
        exteriorRing.clear();
        exteriorRing.addAll(newRing);
    }

    public void addInteriorRing(@NotNull List<Coordinate> newRing) {
        List<Coordinate> coordinateList = new ArrayList<>(newRing);
        interiorRings.add(coordinateList);
    }

    public @NotNull List<Coordinate> getExteriorRing() {
        return exteriorRing;
    }

    public @NotNull List<List<Coordinate>> getInteriorRings() {
        return interiorRings;
    }
}
