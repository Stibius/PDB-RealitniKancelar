/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.spatial.geometry.JGeometry;

import javax.swing.*;

/**
 * @author Honza
 *         Tady budou ulozeny veskere informace o objektech.
 */
public class Data {
    public static boolean line = false;

    public static class JGeometry2ShapeException extends Exception {
    }

    ;
    //rozmery mapy
    public static int width = 1000;
    public static int height = 1000;

    //pro kazdy typ geometrickeho objektu jsou dve pole
    //jedno s geometrickymi objekty a jedno s informacemi o tech objektech

    public static ArrayList<Owner> owners = new ArrayList<>();

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

        //vymazat vsechna stara data z aplikace
        removeAllFromApp();

        //zjisti se rozmery mapy
        width = 1000;
        height = 1000;

        //pridam nejake ownery kvuli testovani
        owners = Owner.loadOwners();
        ObjectInfo info = null;

        try (Statement stmt = ConnectDialog.conn.createStatement()) {
            ResultSet res = stmt.executeQuery("SELECT * FROM OBJEKTY " +
                    "LEFT OUTER JOIN MAJITELE_OBJEKTY ON objekty.ID=majitele_objekty" +
                    ".IDOBJEKTU " +
                    "LEFT OUTER JOIN MAJITELE ON majitele_objekty.IDMAJITELE=majitele" +
                    ".id_majitele");
            while (res.next()) {
                //pokud nemame zadne info, ObjectInfo neexistuje, tudiz ani objekt
                if (!ObjectInfo.ids.contains(res.getInt("id"))) {
                    info = loadShape(res);
                }
                //ObjectInfo mame, pridame jenom dalsi majitele, dalsi objekt nechceme
                else {
                    info.addOwner(res);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            ellipses.add((Ellipse2D) shape);
            ellipsesInfo.add(info);
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
    
    private static Boolean checkValidDates(Map<ObjectInfo, Shape> objects) {
        ObjectInfo currentInfo;
        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet()) {
            currentInfo = entry.getKey();
            
            if (currentInfo.existenceOd == null)
            {
                JOptionPane.showMessageDialog(null, 
                        "U objektu " + currentInfo.nazev + " není uvedeno datum výstavby!", 
                        "Chyba!", 
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (currentInfo.existenceDo != null && !currentInfo.existenceDo.after(currentInfo.existenceOd))
            {
                JOptionPane.showMessageDialog(null, 
                        "U objektu " + currentInfo.nazev + " není datum demolice po datu výstavby!", 
                        "Chyba!", 
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (currentInfo.rekonstrukce != null && !currentInfo.rekonstrukce.after(currentInfo.existenceOd))
            {
                JOptionPane.showMessageDialog(null, 
                        "U objektu " + currentInfo.nazev + " není datum rekonstrukce po datu výstavby!", 
                        "Chyba!", 
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (currentInfo.existenceDo != null && currentInfo.rekonstrukce != null &&
                    !currentInfo.existenceDo.after(currentInfo.rekonstrukce))
            {
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
                if (currentInfo.majitelOd.get(i) == null)
                {
                    JOptionPane.showMessageDialog(null, "Datum začátku vlastnictví objektu " + currentInfo.nazev + 
                            " majitelem " + currentInfo.majitele.get(i).jmeno +
                            " není specifikováno!",
                            "Chyba!", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                
                if (currentInfo.majitelDo.get(i) != null &&
                        !currentInfo.majitelOd.get(i).before(currentInfo.majitelDo.get(i)))
                {
                    JOptionPane.showMessageDialog(null, "Datum začátku vlastnictví objektu " + currentInfo.nazev + 
                            " majitelem " + currentInfo.majitele.get(i).jmeno +
                            " není dříve než datum konce tohoto vlastnictví!",
                            "Chyba!", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                
                if (!intervalWithin(currentInfo.majitelOd.get(i), 
                        currentInfo.majitelDo.get(i),
                        currentInfo.existenceOd,
                        currentInfo.existenceDo))
                {
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
    
    private static Boolean intervalWithin(Date date1, Date date2, Date date3, Date date4)
    {
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
        
        if (StartA >= StartB && EndA <= EndB)
        {
            return true;
        }
        else
        {
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
        for (int i = 0; i < ellipses.size(); i++)
            objects.put(ellipsesInfo.get(i), ellipses.get(i));
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
        for (int i = 0; i < ellipses.size(); i++) {
            ellipsesInfo.get(i).newObject = false;
            ellipsesInfo.get(i).modifiedGeometry = false;
            ellipsesInfo.get(i).modifiedInfo = false;
            ellipsesInfo.get(i).modifiedImage = false;
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

        ellipses.clear();
        ellipsesInfo.clear();

        polygons.clear();
        polygonsInfo.clear();
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
        for (int i = 0; i < ellipses.size(); i++) {
            if (ellipsesInfo.get(i).deletedObject) {
                ellipses.remove(i);
                ellipsesInfo.remove(i);
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
