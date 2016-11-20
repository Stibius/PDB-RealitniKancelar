package cz.vutbr.fit.pdb.realitnikancelar;

import oracle.spatial.geometry.JGeometry;

import java.awt.*;

/**
 * Created by jiri
 * Manipulace s objekty SHAPE<->SDO_GEOMETRY
 */
public class ShapeHelper {

    public static Shape jGeometry2Shape(JGeometry jGeometry) throws
            Data.JGeometry2ShapeException {
        Shape shape;
        // check a type of JGeometry object
        switch (jGeometry.getType()) {
            // it is a polygon
            case JGeometry.GTYPE_POLYGON:
                shape = jGeometry.createShape();
                break;
            // it is something else (we do not know how to convert)
            default:
                throw new Data.JGeometry2ShapeException();
        }
        return shape;
    }
}
