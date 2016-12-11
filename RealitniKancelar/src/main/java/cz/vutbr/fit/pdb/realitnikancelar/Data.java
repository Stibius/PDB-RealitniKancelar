/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.org.apache.xpath.internal.operations.Bool;
import oracle.spatial.geometry.JGeometry;

import javax.swing.*;

/**
 * @author Honza
 *         Tady budou ulozeny veskere informace o objektech.
 */
public class Data {
    public static boolean line = false;
    public static String defaultDataPath;

    public static class JGeometry2ShapeException extends Exception {
    }

    ;
    //rozmery mapy
    public static int width = 1000;
    public static int height = 1000;

    //pro kazdy typ geometrickeho objektu jsou dve pole
    //jedno s geometrickymi objekty a jedno s informacemi o tech objektech

    public static ArrayList<Owner> owners = new ArrayList<>();
    public static ArrayList<Sektor> sectors = new ArrayList<>();

    public static ArrayList<Point> points = new ArrayList<>();
    public static ArrayList<ObjectInfo> pointsInfo = new ArrayList<>();

    //lomena cara je reprezentovana jako pole bodu
    public static ArrayList<ArrayList<Point>> polylines = new ArrayList<>();
    public static ArrayList<ObjectInfo> polylinesInfo = new ArrayList<>();

    public static ArrayList<Ellipse2D> circles = new ArrayList<>();
    public static ArrayList<ObjectInfo> circlesInfo = new ArrayList<>();

    public static ArrayList<Rectangle> rectangles = new ArrayList<>();
    public static ArrayList<ObjectInfo> rectanglesInfo = new ArrayList<>();

    public static ArrayList<Polygon> polygons = new ArrayList<>();
    public static ArrayList<ObjectInfo> polygonsInfo = new ArrayList<>();

    public static Boolean defaultData = false;

    //tady se budou potom nacitat data z databaze, zatim tady vytvarim nejake objekty rucne
    public static void loadData() {
        //Pokud chceme nahrat testovaci data
        if (defaultData) {
            loadDefaultData();
        }
        //vymazat vsechna stara data z aplikace
        removeAllFromApp();

        //zjisti se rozmery mapy
        width = 1000;
        height = 1000;

        //pridam nejake ownery kvuli testovani
        owners = Owner.loadOwners();
        ObjectInfo info = null;

        try (Statement stmt = ConnectDialog.conn.createStatement()) {
            /* Nactení sektoru */
            ResultSet res = stmt.executeQuery("SELECT * FROM sektor");
            while (res.next()) {
                //Nacitani sektoru
                loadSector(res);

            }
            /* Nacteni objektu */
            Statement stmt2 = ConnectDialog.conn.createStatement();
            ResultSet res2 = stmt2.executeQuery("SELECT * FROM OBJEKTY " +
                    "LEFT OUTER JOIN MAJITELE_OBJEKTY ON objekty.ID=majitele_objekty" +
                    ".IDOBJEKTU " +
                    "LEFT OUTER JOIN MAJITELE ON majitele_objekty.IDMAJITELE=majitele" +
                    ".id_majitele");
            while (res2.next()) {
                //pokud nemame zadne info, ObjectInfo neexistuje, tudiz ani objekt
                if (!ObjectInfo.ids.contains(res2.getInt("id"))) {
                    info = loadShape(res2);
                }
                //ObjectInfo mame, pridame jenom dalsi majitele, dalsi objekt nechceme
                else {
                    info.addOwner(res2);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadDefaultData() {
        new ConnectDialog(new javax.swing.JFrame(), true).setVisible(true);
        FileReader fileReader = null;
        System.out.println("Pracuji...");

        try {
            fileReader = new FileReader(new File(defaultDataPath));
            BufferedReader br = new BufferedReader(fileReader);

            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().equals(""))
                    continue;
                System.out.println(line);
                System.out.println("\n");
                line = line.replace(";","");
                Statement stmt2 = ConnectDialog.conn.createStatement();
                ResultSet res2 = stmt2.executeQuery(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Vkládám obrázky");
            ObjectInfo.saveDefaultFotoToDB(1,12,"img/hriste.jpg");
            ObjectInfo.saveDefaultFotoToDB(2,9,"img/fontana.jpg");
            ObjectInfo.saveDefaultFotoToDB(3,11,"img/zastavka.jpg");
            ObjectInfo.saveDefaultFotoToDB(4,0,"img/by.jpg");
            ObjectInfo.saveDefaultFotoToDB(5,3,"img/bytovka.jpg");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Hotovo!");
    }

    private static void loadSector(ResultSet res) throws Exception {
        Sektor sektor = new Sektor();
        byte[] image = new byte[0];
        JGeometry tempGeo;
        Shape shape;
        image = res.getBytes("geometrie");
        //process shape
        tempGeo = JGeometry.load(image);
        shape = ShapeHelper.jGeometry2Shape(tempGeo);
        //Prevedeme na polygon
        Polygon shapePoly = new Polygon();
        PathIterator iterator = shape.getPathIterator(null);
        float[] floats = new float[6];
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(floats);
            int x = (int) floats[0];
            int y = (int) floats[1];
            if (type != PathIterator.SEG_CLOSE) {
                shapePoly.addPoint(x, y);
            }
            iterator.next();
        }
        sektor.geometrie = shapePoly;
        sektor.id = res.getInt("id");
        sektor.nazev = res.getString("nazev");
        sectors.add(sektor);
    }

    private static ObjectInfo loadShape(ResultSet res) throws Exception, SQLException {
        ObjectInfo info = ObjectInfo.createFromDB(res);
        byte[] image = new byte[0];
        JGeometry tempGeo;
        Shape shape;
        image = res.getBytes("geometrie");
        //process shape
        tempGeo = JGeometry.load(image);
        shape = ShapeHelper.jGeometry2Shape(tempGeo);
        populatePanel(shape, info);
        return info;
    }

    private static void populatePanel(Shape shape, ObjectInfo info) {
        if (shape instanceof Rectangle2D) {
            Rectangle rectangle = shape.getBounds();
            rectangles.add(rectangle);
            rectanglesInfo.add(info);
            return;
        } else if (shape instanceof Ellipse2D) {
            circles.add((Ellipse2D) shape);
            circlesInfo.add(info);
            return;
        } else if (shape instanceof GeneralPath && !line) {
            Polygon shapePoly = new Polygon();
            PathIterator iterator = shape.getPathIterator(null);
            float[] floats = new float[6];
            while (!iterator.isDone()) {
                int type = iterator.currentSegment(floats);
                int x = (int) floats[0];
                int y = (int) floats[1];
                if (type != PathIterator.SEG_CLOSE) {
                    shapePoly.addPoint(x, y);
                }
                iterator.next();
            }
            polygons.add(shapePoly);
            polygonsInfo.add(info);
            return;
        } else if (line) {
            ArrayList<Point> body = new ArrayList<>();
            PathIterator iterator = shape.getPathIterator(null);
            float[] floats = new float[6];
            while (!iterator.isDone()) {
                int type = iterator.currentSegment(floats);
                int x = (int) floats[0];
                int y = (int) floats[1];
                if (type != PathIterator.SEG_CLOSE) {
                    Point bod = new Point(x, y);
                    body.add(bod);
                }
                iterator.next();
            }
            if (body.size() == 1) {
                points.add(body.get(0));
                pointsInfo.add(info);
            } else {
                polylines.add(body);
                polylinesInfo.add(info);
            }

        }

    }

    // ukladani dat do DB
    public static void saveData() throws InvalidObjectException, SQLException {
        //Vytvorime testovaci sektor, bacha, smaze vsechny ostatni
        Sektor.testovaciSektor();
        Map<ObjectInfo, Shape> objects = mergeShapes();
       
        /* KONTROLA VALIDITY GEOMETRIE */
        if (checkValidGeometry() == false) {
            return;
        }
        
        /* KONTROLA VALIDITY DAT VYSTAVBY, REKONSTRUKCE A DEMOLICE */
        if (checkValidDates(objects) == false) {
            return;
        }

        /* KONTROLA PREKRYVU MAJITELU */
        if (checkValidOwnerDates(objects) == false) {
            return;
        }

        /* MAJITELE */
        saveOwners();
        /* KONEC MAJITELU */

        /* OBJEKTY */
        saveObjects(objects);
        /* KONEC OBJEKTU */


        dataSaved();
    }
    
    private static Boolean checkValidGeometry()
    {
        /* kolize bodu s body */
        for (int i = 0; i < Data.points.size(); i++)
        {
            if (Data.pointsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.points.size(); j++)
            {
                if (i == j || Data.pointsInfo.get(j).deletedObject) continue;
                
                if (Data.points.get(i).equals(Data.points.get(j)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.pointsInfo.get(i).nazev + 
                        " a " + Data.pointsInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize bodu s lomenymi carami */
        for (int i = 0; i < Data.points.size(); i++)
        {
            if (Data.pointsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polylines.size(); j++)
            {
                if (Data.polylinesInfo.get(j).deletedObject) continue;
                
                for (int jj = 0; jj < Data.polylines.get(j).size() - 1; jj++)
                {
                    Line2D line = new Line2D.Double(
                            Data.polylines.get(j).get(jj),
                            Data.polylines.get(j).get(jj+1));
                    
                    if (line.ptSegDist(Data.points.get(i)) == 0.0)
                    {
                        JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.pointsInfo.get(i).nazev + 
                        " a " + Data.polylinesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        
        /* kolize bodu s obdelniky */
        for (int i = 0; i < Data.points.size(); i++)
        {
            if (Data.pointsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.rectangles.size(); j++)
            {
                if (Data.rectanglesInfo.get(j).deletedObject) continue;
                
                if (Data.rectangles.get(j).contains(Data.points.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.pointsInfo.get(i).nazev + 
                        " a " + Data.rectanglesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize bodu s kruznicemi */
        for (int i = 0; i < Data.points.size(); i++)
        {
            if (Data.pointsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.circles.size(); j++)
            {
                if (Data.circlesInfo.get(j).deletedObject) continue;
                
                if (Data.circles.get(j).contains(Data.points.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.pointsInfo.get(i).nazev + 
                        " a " + Data.circlesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize bodu s polygony */
        for (int i = 0; i < Data.points.size(); i++)
        {
            if (Data.pointsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polygons.size(); j++)
            {
                if (Data.polygonsInfo.get(j).deletedObject) continue;
                
                if (Data.polygons.get(j).contains(Data.points.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.pointsInfo.get(i).nazev + 
                        " a " + Data.polygonsInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize lomenych car s lomenymi carami */
        for (int i = 0; i < Data.polylines.size(); i++)
        {
            if (Data.polylinesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polylines.size(); j++)
            {
                if (i == j || Data.polylinesInfo.get(j).deletedObject) continue;
                
                for (int ii = 0; ii < Data.polylines.get(i).size() - 1; ii++)
                {
                    for (int jj = 0; jj < Data.polylines.get(j).size() - 1; jj++)
                    {
                        Line2D line1 = new Line2D.Double(
                            Data.polylines.get(i).get(ii),
                            Data.polylines.get(i).get(ii+1));
                        
                        Line2D line2 = new Line2D.Double(
                            Data.polylines.get(j).get(jj),
                            Data.polylines.get(j).get(jj+1));
                        
                        if (line1.intersectsLine(line2))
                        {
                            JOptionPane.showMessageDialog(null,
                            "Objekty " + Data.polylinesInfo.get(i).nazev + 
                            " a " + Data.polylinesInfo.get(j).nazev + 
                            " spolu kolidují!",
                            "Chyba!",
                            JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }
        }
        
        /* kolize lomenych car s obdelniky */
        for (int i = 0; i < Data.polylines.size(); i++)
        {
            if (Data.polylinesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.rectangles.size(); j++)
            {
                if (Data.rectanglesInfo.get(j).deletedObject) continue;
                
                for (int ii = 0; ii < Data.polylines.get(i).size() - 1; ii++)
                {
                    Line2D line = new Line2D.Double(
                        Data.polylines.get(i).get(ii),
                        Data.polylines.get(i).get(ii+1));
                        
                    if (Data.rectangles.get(j).intersectsLine(line))
                    {
                        JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.polylinesInfo.get(i).nazev + 
                        " a " + Data.rectanglesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        
        /* kolize lomenych car s kruznicemi */
        for (int i = 0; i < Data.polylines.size(); i++)
        {
            if (Data.polylinesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.circles.size(); j++)
            {
                if (Data.circlesInfo.get(j).deletedObject) continue;
                
                for (int ii = 0; ii < Data.polylines.get(i).size() - 1; ii++)
                {
                    Line2D line = new Line2D.Double(
                        Data.polylines.get(i).get(ii),
                        Data.polylines.get(i).get(ii+1));
                    
                    Point2D center = new Point2D.Double(
                            Data.circles.get(j).getCenterX(),
                            Data.circles.get(j).getCenterY()
                    );
                    
                    double dist = line.ptSegDist(center);
                        
                    if (dist <= Data.circles.get(j).getWidth() / 2)
                    {
                        JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.polylinesInfo.get(i).nazev + 
                        " a " + Data.circlesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        
        /* kolize lomenych car s polygony */
        for (int i = 0; i < Data.polylines.size(); i++)
        {
            if (Data.polylinesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polygons.size(); j++)
            {
                if (Data.polygonsInfo.get(j).deletedObject) continue;
                
                for (int ii = 0; ii < Data.polylines.get(i).size() - 1; ii++)
                {
                    for (int jj = 0; jj < Data.polygons.get(j).npoints - 1; jj++)
                    {
                        Line2D line1 = new Line2D.Double(
                            Data.polylines.get(i).get(ii),
                            Data.polylines.get(i).get(ii+1));
                        
                        Line2D line2 = new Line2D.Double(
                            new Point2D.Double(
                                    Data.polygons.get(j).xpoints[jj], 
                                    Data.polygons.get(j).ypoints[jj]),
                            new Point2D.Double(
                                    Data.polygons.get(j).xpoints[jj+1], 
                                    Data.polygons.get(j).ypoints[jj+1]));
                        
                        if (line1.intersectsLine(line2) ||
                                Data.polygons.get(j).contains(Data.polylines.get(i).get(ii)) ||
                                Data.polygons.get(j).contains(Data.polylines.get(i).get(ii+1)))
                        {
                            JOptionPane.showMessageDialog(null,
                            "Objekty " + Data.polylinesInfo.get(i).nazev + 
                            " a " + Data.polygonsInfo.get(j).nazev + 
                            " spolu kolidují!",
                            "Chyba!",
                            JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }
        }
        
        /* kolize obdelniku s obdelniky */
        for (int i = 0; i < Data.rectangles.size(); i++)
        {
            if (Data.rectanglesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.rectangles.size(); j++)
            {
                if (i == j || Data.rectanglesInfo.get(j).deletedObject) continue;
                
                if (Data.rectangles.get(j).intersects(Data.rectangles.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.rectanglesInfo.get(i).nazev + 
                        " a " + Data.rectanglesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize obdelniku s kruznicemi */
        for (int i = 0; i < Data.rectangles.size(); i++)
        {
            if (Data.rectanglesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.circles.size(); j++)
            {
                if (Data.circlesInfo.get(j).deletedObject) continue;
                
                if (Data.circles.get(j).intersects(Data.rectangles.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.rectanglesInfo.get(i).nazev + 
                        " a " + Data.circlesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize obdelniku s polygony */
        for (int i = 0; i < Data.rectangles.size(); i++)
        {
            if (Data.rectanglesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polygons.size(); j++)
            {
                if (Data.polygonsInfo.get(j).deletedObject) continue;
                
                if (Data.polygons.get(j).intersects(Data.rectangles.get(i)))
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.rectanglesInfo.get(i).nazev + 
                        " a " + Data.polygonsInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize kruznic s kruznicemi */
        for (int i = 0; i < Data.circles.size(); i++)
        {
            if (Data.circlesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.circles.size(); j++)
            {
                if (i == j || Data.circlesInfo.get(j).deletedObject) continue;
                
                Point2D point1 = new Point2D.Double(
                        Data.circles.get(i).getCenterX(),
                        Data.circles.get(i).getCenterY()
                );
                
                Point2D point2 = new Point2D.Double(
                        Data.circles.get(j).getCenterX(),
                        Data.circles.get(j).getCenterY()
                );
                
                double dist = point1.distance(point2);
                double r1 = Data.circles.get(i).getWidth() / 2;
                double r2 = Data.circles.get(j).getWidth() / 2;
                
                if (dist <= r1 + r2)
                {
                    JOptionPane.showMessageDialog(null,
                        "Objekty " + Data.circlesInfo.get(i).nazev + 
                        " a " + Data.circlesInfo.get(j).nazev + 
                        " spolu kolidují!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        /* kolize kruznic s polygony */
        for (int i = 0; i < Data.circles.size(); i++)
        {
            if (Data.circlesInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polygons.size(); j++)
            {
                if (Data.polygonsInfo.get(j).deletedObject) continue;
                
                for (int jj = 0; jj < Data.polygons.get(j).npoints - 1; jj++) {
                    Line2D line = new Line2D.Double(
                        new Point2D.Double(
                            Data.polygons.get(j).xpoints[jj], 
                            Data.polygons.get(j).ypoints[jj]),
                        new Point2D.Double(
                            Data.polygons.get(j).xpoints[jj+1], 
                            Data.polygons.get(j).ypoints[jj+1]));
                    
                    Point2D center = new Point2D.Double(
                            Data.circles.get(i).getCenterX(),
                            Data.circles.get(i).getCenterY());
                    
                    double dist = line.ptSegDist(center);

                    if (dist <= Data.circles.get(i).getWidth() / 2) {
                        JOptionPane.showMessageDialog(null,
                                "Objekty " + Data.circlesInfo.get(i).nazev
                                + " a " + Data.polygonsInfo.get(j).nazev
                                + " spolu kolidují!",
                                "Chyba!",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        
        /* kolize polygonu s polygony */
        for (int i = 0; i < Data.polygons.size(); i++)
        {
            if (Data.polygonsInfo.get(i).deletedObject) continue;
            
            for (int j = 0; j < Data.polygons.size(); j++)
            {
                if (i == j || Data.polygonsInfo.get(j).deletedObject) continue;
                
                if (polygons.get(i).contains(polygons.get(j).getBounds2D()))
                {
                    return false;
                }
                
                for (int ii = 0; ii < Data.polygons.get(i).npoints - 1; ii++) {
                    for (int jj = 0; jj < Data.polygons.get(j).npoints - 1; jj++) {
                        Line2D line1 = new Line2D.Double(
                                new Point2D.Double(
                                        Data.polygons.get(i).xpoints[ii],
                                        Data.polygons.get(i).ypoints[ii]),
                                new Point2D.Double(
                                        Data.polygons.get(i).xpoints[ii + 1],
                                        Data.polygons.get(i).ypoints[ii + 1]));
                        
                        Line2D line2 = new Line2D.Double(
                                new Point2D.Double(
                                        Data.polygons.get(j).xpoints[jj],
                                        Data.polygons.get(j).ypoints[jj]),
                                new Point2D.Double(
                                        Data.polygons.get(j).xpoints[jj + 1],
                                        Data.polygons.get(j).ypoints[jj + 1]));

                        if (line1.intersectsLine(line2)) {
                            JOptionPane.showMessageDialog(null,
                                    "Objekty " + Data.polygonsInfo.get(i).nazev
                                    + " a " + Data.polygonsInfo.get(j).nazev
                                    + " spolu kolidují!",
                                    "Chyba!",
                                    JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }
        }
        
        /* kolize objektu s hranicemi sektoru */
        for (int i = 0; i < Data.sectors.size(); i++)
        {
            for (int ii = 0; ii < Data.sectors.get(i).geometrie.npoints - 1; ii++)
            {
                Line2D line = new Line2D.Double(
                        new Point2D.Double(
                                Data.sectors.get(i).geometrie.xpoints[ii],
                                Data.sectors.get(i).geometrie.ypoints[ii]),
                        new Point2D.Double(
                                Data.sectors.get(i).geometrie.xpoints[ii + 1],
                                Data.sectors.get(i).geometrie.ypoints[ii + 1]));  
                
                for (int j = 0; j < Data.points.size(); j++)
                {
                    if (line.ptSegDist(Data.points.get(j)) == 0.0)
                    {
                        JOptionPane.showMessageDialog(null,
                                "Objekt " + Data.pointsInfo.get(j).nazev
                                + " protíná hranici sektoru!",
                                "Chyba!",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                
                for (int j = 0; j < Data.polylines.size(); j++)
                {
                    for (int jj = 0; jj < Data.polylines.get(j).size() - 1; jj++)
                    {
                        Line2D line2 = new Line2D.Double(
                            Data.polylines.get(j).get(jj),
                            Data.polylines.get(j).get(jj+1));
                        
                        if (line.intersectsLine(line2))
                        {
                            JOptionPane.showMessageDialog(null,
                                "Objekt " + Data.polylinesInfo.get(j).nazev
                                + " protíná hranici sektoru!",
                                "Chyba!",
                                JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
                
                for (int j = 0; j < Data.rectangles.size(); j++)
                {
                    if (line.intersects(Data.rectangles.get(j)))
                    {
                        JOptionPane.showMessageDialog(null,
                                "Objekt " + Data.rectanglesInfo.get(j).nazev
                                + " protíná hranici sektoru!",
                                "Chyba!",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                
                for (int j = 0; j < Data.circles.size(); j++)
                {
                    Point2D center = new Point2D.Double(
                            Data.circles.get(j).getCenterX(),
                            Data.circles.get(j).getCenterY());
                    
                    double dist = line.ptSegDist(center);

                    if (dist <= Data.circles.get(j).getWidth() / 2) 
                    {
                        JOptionPane.showMessageDialog(null,
                                "Objekt " + Data.circlesInfo.get(j).nazev
                                + " protíná hranici sektoru!",
                                "Chyba!",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                
                for (int j = 0; j < Data.polygons.size(); j++)
                {
                    for (int jj = 0; jj < Data.polygons.get(j).npoints - 1; jj++) {
                        Line2D line2 = new Line2D.Double(
                                new Point2D.Double(
                                        Data.polygons.get(j).xpoints[jj],
                                        Data.polygons.get(j).ypoints[jj]),
                                new Point2D.Double(
                                        Data.polygons.get(j).xpoints[jj + 1],
                                        Data.polygons.get(j).ypoints[jj + 1]));

                        if (line.intersectsLine(line2)) {
                            JOptionPane.showMessageDialog(null,
                                    "Objekt " + Data.polygonsInfo.get(j).nazev
                                    + " protíná hranici sektoru!",
                                    "Chyba!",
                                    JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }   
        }
        
        
        return true;
    }
    
    private static Boolean checkValidDates(Map<ObjectInfo, Shape> objects) {
        ObjectInfo currentInfo;
        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet()) {
            currentInfo = entry.getKey();

            if (currentInfo.existenceOd == null) {
                JOptionPane.showMessageDialog(null,
                        "U objektu " + currentInfo.nazev + " není uvedeno datum výstavby!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (currentInfo.existenceDo != null && !currentInfo.existenceDo.after(currentInfo.existenceOd)) {
                JOptionPane.showMessageDialog(null,
                        "U objektu " + currentInfo.nazev + " není datum demolice po datu výstavby!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (currentInfo.rekonstrukce != null && !currentInfo.rekonstrukce.after(currentInfo.existenceOd)) {
                JOptionPane.showMessageDialog(null,
                        "U objektu " + currentInfo.nazev + " není datum rekonstrukce po datu výstavby!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (currentInfo.existenceDo != null && currentInfo.rekonstrukce != null &&
                    !currentInfo.existenceDo.after(currentInfo.rekonstrukce)) {
                JOptionPane.showMessageDialog(null,
                        "U objektu " + currentInfo.nazev + " není datum demolice po datu rekonstrukce!",
                        "Chyba!",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }


    private static Boolean checkValidOwnerDates(Map<ObjectInfo, Shape> objects) {
        ObjectInfo currentInfo;
        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet()) {
            currentInfo = entry.getKey();
            for (int i = 0; i < currentInfo.majitele.size(); i++) {
                if (currentInfo.majitelOd.get(i) == null) {
                    JOptionPane.showMessageDialog(null, "Datum začátku vlastnictví objektu " + currentInfo.nazev +
                                    " majitelem " + currentInfo.majitele.get(i).jmeno +
                                    " není specifikováno!",
                            "Chyba!", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (currentInfo.majitelDo.get(i) != null &&
                        !currentInfo.majitelOd.get(i).before(currentInfo.majitelDo.get(i))) {
                    JOptionPane.showMessageDialog(null, "Datum začátku vlastnictví objektu " + currentInfo.nazev +
                                    " majitelem " + currentInfo.majitele.get(i).jmeno +
                                    " není dříve než datum konce tohoto vlastnictví!",
                            "Chyba!", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (!intervalWithin(currentInfo.majitelOd.get(i),
                        currentInfo.majitelDo.get(i),
                        currentInfo.existenceOd,
                        currentInfo.existenceDo)) {
                    JOptionPane.showMessageDialog(null, "Období vlastnictví objektu " + currentInfo.nazev +
                                    " majitelem " + currentInfo.majitele.get(i).jmeno +
                                    " není zcela obsaženo mezi datem výstavby a datem demolice tohoto objektu!",
                            "Chyba!", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            for (int i = 0; i < currentInfo.majitele.size(); i++) {
                for (int j = i + 1; j < currentInfo.majitele.size(); j++) {
                    Date StartA = currentInfo.majitelOd.get(i);
                    Date EndA = currentInfo.majitelDo.get(i);
                    Date StartB = currentInfo.majitelOd.get(j);
                    Date EndB = currentInfo.majitelDo.get(j);
                    if (dateOverlap(StartA, EndA, StartB, EndB)) {
                        JOptionPane.showMessageDialog(null, "Data vlastníků objektu " + currentInfo.nazev + " se " +
                                        "překrývají!",
                                "Chyba!", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Boolean intervalWithin(Date date1, Date date2, Date date3, Date date4) {
        Long StartA;
        Long EndA;
        Long StartB;
        Long EndB;

        if (date2 == null) {
            EndA = Long.MAX_VALUE;
        } else {
            EndA = date2.getTime();
        }
        if (date4 == null) {
            EndB = Long.MAX_VALUE;
        } else {
            EndB = date4.getTime();
        }
        StartA = date1.getTime();
        StartB = date3.getTime();

        if (StartA >= StartB && EndA <= EndB) {
            return true;
        } else {
            return false;
        }
    }

    private static Boolean dateOverlap(Date date, Date date1, Date date2, Date date3) {
        Long StartA;
        Long EndA;
        Long StartB;
        Long EndB;

        if (date1 == null) {
            EndA = Long.MAX_VALUE;
        } else {
            EndA = date1.getTime();
        }
        if (date3 == null) {
            EndB = Long.MAX_VALUE;
        } else {
            EndB = date3.getTime();
        }
        StartA = date.getTime();
        StartB = date2.getTime();

        /* (StartA <= EndB) and (EndA >= StartB) */
        if (
                (StartA < EndB)
                        &&
                        (EndA > StartB)
                ) {
            return true;
        }
        return false;
    }

    private static void saveOwners() {
        Owner currentOwner;
        for (int i = 0; i < owners.size(); i++) {
            currentOwner = owners.get(i);

            /* neni novy ani modifikovany ani smazany, preskocit */
            if (!currentOwner.modifiedOwner &&
                    !currentOwner.newOwner &&
                    !currentOwner.deletedOwner) {
                continue;
            }

            /* byl smazany v aplikaci, je treba ho smazat z DB */
            if (currentOwner.deletedOwner) {
                DatabaseHelper.deleteOwner(currentOwner);
                continue;
            }

            /* majitel je modifikovany */
            if (currentOwner.modifiedOwner) {
                DatabaseHelper.modifyOwner(currentOwner);
                continue;
            }
            /* majitel je novy */
            if (currentOwner.newOwner) {
                DatabaseHelper.newOwner(currentOwner);
                continue;
            }
        }
    }

    private static void saveObjects(Map<ObjectInfo, Shape> objects) throws SQLException {
        ObjectInfo currentInfo;
        Shape current;
        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet()) {
            currentInfo = entry.getKey();
            current = entry.getValue();

            /* neni novy ani modifikovany ani smazany, preskocit */
            if (!currentInfo.modifiedInfo &&
                    !currentInfo.modifiedGeometry &&
                    !currentInfo.modifiedImage &&
                    !currentInfo.newObject &&
                    !currentInfo.deletedObject) {
                continue;
            }

            /* Pokud je smazany, smazeme ho i z DB */
            if (currentInfo.deletedObject) {
                DatabaseHelper.deleteObject(currentInfo);
                continue;
            }

            /* Pokud je novy, udelame plny insert */
            if (currentInfo.newObject) {
                DatabaseHelper.newObject(current, currentInfo);
                continue;
            }

            /* Je zmenena geometrie, aktualizujeme */
            if (currentInfo.modifiedGeometry) {
                DatabaseHelper.modifyObjectGeometry(current, currentInfo);
            }

            /* Modifikovane informace, aktualizujeme */
            if (currentInfo.modifiedInfo) {
                DatabaseHelper.modifyObjectInfo(currentInfo);
            }
            /* Modifikovany obrazek */
            if (currentInfo.modifiedImage) {
                try {
                    currentInfo.saveFotoToDB();
                } catch (IOException ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static Map mergeShapes() {
        Map<ObjectInfo, Shape> objects = new HashMap<>();
        for (int i = 0; i < rectangles.size(); i++)
            objects.put(rectanglesInfo.get(i), rectangles.get(i));
        for (int i = 0; i < circles.size(); i++)
            objects.put(circlesInfo.get(i), circles.get(i));
        for (int i = 0; i < polygons.size(); i++)
            objects.put(polygonsInfo.get(i), polygons.get(i));
        for (int i = 0; i < points.size(); i++) {
            Path2D line = new Path2D.Double();
            Point2D p = points.get(i);

            line.moveTo(p.getX(), p.getY());

            for (int j = 1; j < points.size(); j++) {
                p = points.get(i);
                line.lineTo(p.getX(), p.getY());
            }
            objects.put(pointsInfo.get(i), line);
        }
        for (int i = 0; i < polylines.size(); i++) {
            Path2D line = new Path2D.Double();
            Point2D p = polylines.get(i).get(0);

            line.moveTo(p.getX(), p.getY());

            for (int j = 1; j < polylines.get(i).size(); j++) {
                p = polylines.get(i).get(j);
                line.lineTo(p.getX(), p.getY());
            }
            objects.put(polylinesInfo.get(i), line);
        }

        return objects;
    }

    //vola se po ulozeni dat do DB
    //pro vsechny objekty se nastavi, ze nejsou nove ani modifikovane
    //smazane se smazou z aplikace
    public static void dataSaved() {
        removeDeletedFromApp();

        for (int i = 0; i < owners.size(); i++) {
            owners.get(i).newOwner = false;
            owners.get(i).modifiedOwner = false;
        }
        for (int i = 0; i < points.size(); i++) {
            pointsInfo.get(i).newObject = false;
            pointsInfo.get(i).modifiedGeometry = false;
            pointsInfo.get(i).modifiedInfo = false;
            pointsInfo.get(i).modifiedImage = false;
        }
        for (int i = 0; i < polylines.size(); i++) {
            polylinesInfo.get(i).newObject = false;
            polylinesInfo.get(i).modifiedGeometry = false;
            polylinesInfo.get(i).modifiedInfo = false;
            polylinesInfo.get(i).modifiedImage = false;
        }
        for (int i = 0; i < rectangles.size(); i++) {
            rectanglesInfo.get(i).newObject = false;
            rectanglesInfo.get(i).modifiedGeometry = false;
            rectanglesInfo.get(i).modifiedInfo = false;
            rectanglesInfo.get(i).modifiedImage = false;
        }
        for (int i = 0; i < circles.size(); i++) {
            circlesInfo.get(i).newObject = false;
            circlesInfo.get(i).modifiedGeometry = false;
            circlesInfo.get(i).modifiedInfo = false;
            circlesInfo.get(i).modifiedImage = false;
        }
        for (int i = 0; i < polygons.size(); i++) {
            polygonsInfo.get(i).newObject = false;
            polygonsInfo.get(i).modifiedGeometry = false;
            polygonsInfo.get(i).modifiedInfo = false;
            polygonsInfo.get(i).modifiedImage = false;
        }

        JOptionPane.showMessageDialog(null,
                "Data byla úspěšně uložena do databáze!",
                "Úspěch!",
                JOptionPane.INFORMATION_MESSAGE);
    }

    //smaze vsechna data z aplikace
    public static void removeAllFromApp() {
        owners.clear();

        points.clear();
        pointsInfo.clear();

        polylines.clear();
        polylinesInfo.clear();

        rectangles.clear();
        rectanglesInfo.clear();

        circles.clear();
        circlesInfo.clear();

        polygons.clear();
        polygonsInfo.clear();

        sectors.clear();
    }

    //vymaze objekty s priznakem deleted z aplikace
    public static void removeDeletedFromApp() {
        for (int i = 0; i < owners.size(); i++) {
            if (owners.get(i).deletedOwner) {
                owners.remove(i);
                i--;
            }
        }
        for (int i = 0; i < points.size(); i++) {
            if (pointsInfo.get(i).deletedObject) {
                points.remove(i);
                pointsInfo.remove(i);
                i--;
            }
        }
        for (int i = 0; i < polylines.size(); i++) {
            if (polylinesInfo.get(i).deletedObject) {
                polylines.remove(i);
                polylinesInfo.remove(i);
                i--;
            }
        }
        for (int i = 0; i < rectangles.size(); i++) {
            if (rectanglesInfo.get(i).deletedObject) {
                rectangles.remove(i);
                rectanglesInfo.remove(i);
                i--;
            }
        }
        for (int i = 0; i < circles.size(); i++) {
            if (circlesInfo.get(i).deletedObject) {
                circles.remove(i);
                circlesInfo.remove(i);
                i--;
            }
        }
        for (int i = 0; i < polygons.size(); i++) {
            if (polygonsInfo.get(i).deletedObject) {
                polygons.remove(i);
                polygonsInfo.remove(i);
                i--;
            }
        }
    }
}
