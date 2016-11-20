/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import oracle.jdbc.pool.OracleDataSource;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

/**
 * @author Honza
 *         Tady budou ulozeny veskere informace o objektech.
 */
public class Data {
    public static class JGeometry2ShapeException extends Exception {
    }

    ;
    //rozmery mapy
    public static int width = 1000;
    public static int height = 1000;

    //pro kazdy typ geometrickeho objektu jsou dve pole
    //jedno s geometrickymi objekty a jedno s informacemi o tech objektech

    public static ArrayList<Point> points = new ArrayList<>();
    public static ArrayList<ObjectInfo> pointsInfo = new ArrayList<>();

    //lomena cara je reprezentovana jako pole bodu
    public static ArrayList<ArrayList<Point>> polylines = new ArrayList<>();
    public static ArrayList<ObjectInfo> polylinesInfo = new ArrayList<>();

    public static ArrayList<Ellipse2D> ellipses = new ArrayList<>();
    public static ArrayList<ObjectInfo> ellipsesInfo = new ArrayList<>();

    public static ArrayList<Rectangle> rectangles = new ArrayList<>();
    public static ArrayList<ObjectInfo> rectanglesInfo = new ArrayList<>();

    public static ArrayList<Polygon> polygons = new ArrayList<>();
    public static ArrayList<ObjectInfo> polygonsInfo = new ArrayList<>();

    //tady se budou potom nacitat data z databaze, zatim tady vytvarim nejake objekty rucne
    public static void loadData() {
        //zjisti se rozmery mapy
        width = 1000;
        height = 1000;

        try (Statement stmt = ConnectDialog.conn.createStatement()) {
            ResultSet res = stmt.executeQuery("SELECT * FROM OBJEKTY");
            while (res.next()) {
                loadShape(res);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //nejake objekty na testovani, protoze v databazi nic neni
        /*
        rectangles.add(new Rectangle(100, 100, 300, 200));
        rectanglesInfo.add(new ObjectInfo());
        rectanglesInfo.get(0).nazev = "Ahoj";

        rectangles.add(new Rectangle(500, 500, 300, 200));
        rectanglesInfo.add(new ObjectInfo());
        rectanglesInfo.get(1).nazev = "XXX";
        

        
        //vymazou se predchozi data
        points.clear();
        pointsInfo.clear();
        
        polylines.clear();
        polylinesInfo.clear();
        
        ellipses.clear();
        ellipsesInfo.clear();
        
        rectangles.clear();
        rectanglesInfo.clear();
        
        polygons.clear();
        polygonsInfo.clear();
        
        //pridame nejake objekty
        points.add(new Point(100, 400));
        pointsInfo.add(new ObjectInfo());
        
        ArrayList<Point> polyline = new ArrayList<Point>();
        polyline.add(new Point(500, 100));
        polyline.add(new Point(600, 200));
        polyline.add(new Point(550, 300));
        polylines.add(polyline);
        polylinesInfo.add(new ObjectInfo());
        
        ellipses.add(new Ellipse2D.Double(500, 400, 50, 100));
        ellipsesInfo.add(new ObjectInfo());
        
        rectangles.add(new Rectangle(100, 100, 300, 200));
        rectanglesInfo.add(new ObjectInfo());
        
        Polygon polygon = new Polygon();
        polygon.addPoint(700, 100);
        polygon.addPoint(800, 50);
        polygon.addPoint(900, 200);
        polygon.addPoint(850, 300);
        polygons.add(polygon);
        polygonsInfo.add(new ObjectInfo());
        */
    }

    private static void loadShape(ResultSet res) throws Exception, SQLException {
        byte[] image = new byte[0];
        JGeometry tempGeo;
        Shape shape;

        image = res.getBytes("geometrie");
        ObjectInfo info = ObjectInfo.create(res);

        //process shape
        tempGeo = JGeometry.load(image);
        shape = ShapeHelper.jGeometry2Shape(tempGeo);
        populatePanel(shape, info);
    }

    private static void populatePanel(Shape shape, ObjectInfo info) {
        if (shape instanceof Rectangle2D) {
            Rectangle rectangle = shape.getBounds();
            rectangles.add(rectangle);
            rectanglesInfo.add(info);
            return;
        } else if (shape instanceof Ellipse2D) {
            ellipses.add((Ellipse2D) shape);
            ellipsesInfo.add(info);
            return;
        }
    }

    // ukladani dat do DB
    public static void saveData() throws InvalidObjectException, SQLException {

        Shape current;
        ObjectInfo currentInfo;
        Connection conn = ConnectDialog.conn;

        Map<ObjectInfo, Shape> objects = mergeShapes();

        //vycistit objekty
        PreparedStatement del = conn.prepareStatement("DELETE FROM objekty");
        del.execute();

        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet())
        {
            currentInfo = entry.getKey();
            current = entry.getValue();

            JGeometry jGeo = ShapeHelper.shape2jGeometry(current);
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO objekty (id, geometrie) VALUES (?, ?)")) {
                stmt.setInt(1, currentInfo.id);

                STRUCT obj = JGeometry.store(conn, jGeo);
                stmt.setObject(2, obj);

                stmt.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Map mergeShapes() {
        Map<ObjectInfo, Shape> objects = new HashMap<>();
        for (int i = 0; i < rectangles.size(); i++)
            objects.put(rectanglesInfo.get(i), rectangles.get(i));
        for (int i = 0; i < ellipses.size(); i++)
            objects.put(ellipsesInfo.get(i), ellipses.get(i));
        for (int i = 0; i < polygons.size(); i++)
            objects.put(polygonsInfo.get(i), polygons.get(i));
        return objects;
    }
}
