/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import javafx.scene.shape.Polyline;

/**
 *
 * @author Honza
 * Tady budou ulozeny veskere informace o objektech.
 */
public class Data {
    
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
        
    }
    
    //tady se budou potom ukladat data do databze, zatim tu neni nic
    public static void saveData() {
        
    }
}
