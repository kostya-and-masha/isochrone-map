package com.example.isochronemap.mapstructure;

import org.jetbrains.annotations.NotNull;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.impl.CircleImpl;
import org.locationtech.spatial4j.shape.impl.PointImpl;

class BoundingBox {
    final Coordinate minimum;
    final Coordinate maximum;

    BoundingBox(@NotNull Coordinate center, double radiusInKilometers) {
        Circle circle = new CircleImpl(
                new PointImpl(
                        center.longitudeDeg,
                        center.latitudeDeg,
                        SpatialContext.GEO),
                radiusInKilometers * DistanceUtils.KM_TO_DEG,
                SpatialContext.GEO
        );
        Rectangle rectangle = circle.getBoundingBox();
        minimum = new Coordinate(rectangle.getMinY(), rectangle.getMinX());
        maximum = new Coordinate(rectangle.getMaxY(), rectangle.getMaxX());
    }
}
