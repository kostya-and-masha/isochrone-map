package ru.hse.isochronemap.isochronebuilding;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;

/**
 * This class represents polygons
 * returned by {@link ru.hse.isochronemap.isochronebuilding.IsochroneBuilder}.
 */
public class IsochronePolygon {
    private List<Coordinate> exteriorRing = new ArrayList<>();
    private List<List<Coordinate>> interiorRings = new ArrayList<>();

    /** Creates an empty polygon. */
    public IsochronePolygon() {
    }

    /**
     * Creates a polygon from JTS {@link Polygon}
     *
     * @param JTSPolygon       source polygon.
     * @param ignoredHolesArea interior holes of this area (or less) would be ignored.
     */
    public IsochronePolygon(@NonNull Polygon JTSPolygon, double ignoredHolesArea) {
        for (org.locationtech.jts.geom.Coordinate JTSCoordinate : JTSPolygon.getExteriorRing()
                                                                            .getCoordinates()) {
            exteriorRing.add(new Coordinate(JTSCoordinate.y, JTSCoordinate.x));
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        for (int i = 0; i < JTSPolygon.getNumInteriorRing(); i++) {
            if (geometryFactory.createPolygon(JTSPolygon.getInteriorRingN(i).getCoordinates())
                               .getArea() < ignoredHolesArea) {
                continue;
            }
            List<Coordinate> coordinateList = new ArrayList<>();
            for (org.locationtech.jts.geom.Coordinate JTSCoordinate : JTSPolygon.getInteriorRingN(i)
                                                                                .getCoordinates()) {
                coordinateList.add(new Coordinate(JTSCoordinate.y, JTSCoordinate.x));
            }
            interiorRings.add(coordinateList);
        }
    }

    /** Sets exterior polygon border. */
    public void setExteriorRing(@NonNull List<Coordinate> newRing) {
        exteriorRing.clear();
        exteriorRing.addAll(newRing);
    }

    /** Adds hole to polygon */
    public void addInteriorRing(@NonNull List<Coordinate> newRing) {
        List<Coordinate> coordinateList = new ArrayList<>(newRing);
        interiorRings.add(coordinateList);
    }

    public @NonNull List<Coordinate> getExteriorRing() {
        return exteriorRing;
    }

    public @NonNull List<List<Coordinate>> getInteriorRings() {
        return interiorRings;
    }
}
