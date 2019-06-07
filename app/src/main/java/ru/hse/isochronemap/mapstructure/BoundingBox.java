package ru.hse.isochronemap.mapstructure;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.impl.CircleImpl;
import org.locationtech.spatial4j.shape.impl.PointImpl;

import androidx.annotation.NonNull;

/** This class represents map region bounding box. **/
public class BoundingBox {
    /** Box minimum coordinate. **/
    public final Coordinate minimum;
    /** Box maximum coordinate. **/
    public final Coordinate maximum;

    /**
     * Constructs new bounding box.
     * @param minimum
     * @param maximum
     */
    public BoundingBox(@NonNull Coordinate minimum, @NonNull Coordinate maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Constructs new bounding box.
     * @param center bounding box center
     * @param radiusInKilometers bounding box radius in kilometers
     */
    public BoundingBox(@NonNull Coordinate center, double radiusInKilometers) {
        Circle circle = new CircleImpl(
                new PointImpl(
                        center.longitude,
                        center.latitude,
                        SpatialContext.GEO),
                radiusInKilometers * DistanceUtils.KM_TO_DEG,
                SpatialContext.GEO
        );
        Rectangle rectangle = circle.getBoundingBox();
        minimum = new Coordinate(rectangle.getMinY(), rectangle.getMinX());
        maximum = new Coordinate(rectangle.getMaxY(), rectangle.getMaxX());
    }
}
