package cz.vutbr.fit.pdb.realitnikancelar;

import oracle.spatial.geometry.JGeometry;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manipulace s objekty SHAPE a SDO_GEOMETRY
 *
 * @author jiri
 */
public class ShapeHelper {

    public static int SHAPE_TYPE_POLYGON = 1;
    public static int SHAPE_TYPE_CIRCLE = 2;
    public static int SHAPE_TYPE_LINE = 3;
    public static int SHAPE_TYPE_RECTANGLE = 4;

    public static int DB_SHAPE_CIRCLE = 4;
    public static int DB_SHAPE_RECTANGLE = 3;
    public static int DB_SHAPE_POLYGON_OR_LINE = 1;

    public static int DB_TYPE_LINE = 2002;
    public static int DB_TYPE_POLYGON = 2003;

    public static int DB_INFO_LINE = 2;
    public static int DB_INFO_OTHER = 1003;

    /**
     * Prevod Jgeometry na Shape
     *
     * @param jGeometry jGeometry
     * @return shape
     * @throws Data.JGeometry2ShapeException Spatny typ tvaru
     */
    public static Shape jGeometry2Shape(JGeometry jGeometry) throws
            Data.JGeometry2ShapeException {

        Shape shape;

        switch (jGeometry.getType()) {
            case JGeometry.GTYPE_CURVE:
                shape = jGeometry.createShape();
                Data.line = true;
                break;
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

    /**
     * Prevod tvaru na jGeometry
     *
     * @param shape tvar typu Shape
     * @return Jgeometry tvaru
     * @throws InvalidObjectException spatny typ tvaru
     */
    public static JGeometry shape2jGeometry(Shape shape) throws InvalidObjectException {

        int dbType;
        int dbInfoType;
        int dbShape;
        int shapeType;
        JGeometry res;

        if (shape instanceof Ellipse2D) {
            dbType = DB_TYPE_POLYGON;
            dbInfoType = DB_INFO_OTHER;

            shapeType = SHAPE_TYPE_CIRCLE;
            dbShape = DB_SHAPE_CIRCLE;
        } else if (shape instanceof Rectangle) {
            dbType = DB_TYPE_POLYGON;
            dbInfoType = DB_INFO_OTHER;

            shapeType = SHAPE_TYPE_RECTANGLE;
            dbShape = DB_SHAPE_RECTANGLE;
        } else if (shape instanceof Polygon) {
            dbType = DB_TYPE_POLYGON;
            dbInfoType = DB_INFO_OTHER;

            shapeType = SHAPE_TYPE_POLYGON;
            dbShape = DB_SHAPE_POLYGON_OR_LINE;
        } else if (shape instanceof Path2D) {
            dbType = DB_TYPE_LINE;
            dbInfoType = DB_INFO_LINE;

            shapeType = SHAPE_TYPE_LINE;
            dbShape = DB_SHAPE_POLYGON_OR_LINE;
        } else
            throw new InvalidObjectException("Invalid shape type");

        List<Double> result = new ArrayList<Double>();
        double[] coords = new double[6];
        if (shapeType != SHAPE_TYPE_RECTANGLE) {
            for (PathIterator pi = shape.getPathIterator(null); !pi.isDone(); pi.next()) {
                //reset pole
                Arrays.fill(coords, 0.0);
                //typ segmentu
                int type = pi.currentSegment(coords);
                //polygon?
                if (shapeType == SHAPE_TYPE_POLYGON || shapeType == SHAPE_TYPE_LINE) {
                    if (type != PathIterator.SEG_LINETO && type != PathIterator.SEG_MOVETO) {
                        continue;
                    }
                    result.add(coords[0]);
                    result.add(coords[1]);
                } else if (shapeType == SHAPE_TYPE_CIRCLE) {
                    if (type != PathIterator.SEG_CUBICTO) {
                        continue;
                    }
                    result.add(coords[4]);
                    result.add(coords[5]);
                }
            }
        } else {
            Rectangle rec = shape.getBounds();
            result.add(rec.getMinX());
            result.add(rec.getMinY());
            result.add(rec.getMaxX());
            result.add(rec.getMaxY());
        }

        if (shapeType == SHAPE_TYPE_POLYGON) {
            double x, y;
            x = result.get(0);
            y = result.get(1);

            result.add(x);
            result.add(y);
        }

        if (shapeType == SHAPE_TYPE_CIRCLE) {
            double[] ordinates = new double[6];
            for (int i = 0; i < 6; i++) {
                ordinates[i] = result.get(i);
            }
            res = new JGeometry(dbType, 0, new int[]{1, dbInfoType, dbShape}, ordinates);
        } else {
            double[] ordinates = new double[result.size()];
            int k = 0;
            for (Double d : result) {
                ordinates[k++] = d;
            }
            res = new JGeometry(dbType, 0, new int[]{1, dbInfoType, dbShape}, ordinates);
        }

        return res;

    }
}
