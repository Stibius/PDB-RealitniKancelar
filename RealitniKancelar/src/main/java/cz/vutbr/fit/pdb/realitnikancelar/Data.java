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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

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
            ResultSet res = stmt.executeQuery("SELECT * FROM OBJEKTY LEFT OUTER JOIN MAJITELE_OBJEKTY ON objekty.ID=majitele_objekty.IDOBJEKTU LEFT OUTER JOIN MAJITELE ON majitele_objekty.IDMAJITELE=majitele.id_majitele");
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
        Shape current;
        ObjectInfo currentInfo;
        Connection conn = ConnectDialog.conn;

        for (int i = 0; i < owners.size(); i++) {
            Owner currentOwner = owners.get(i);
            if (!currentOwner.modifiedOwner &&
                    !currentOwner.newOwner &&
                    !currentOwner.deletedOwner) {
                continue; //neni novy ani modifikovany ani smazany, preskocit
            }

            if (currentOwner.deletedOwner) {
                //byl smazany v aplikaci, je treba ho smazat z DB
                if (currentOwner.deletedOwner) {
                    String query = "DELETE FROM majitele WHERE id_majitele='" + currentOwner
                            .id +
                            "'";
                    try {
                        Statement stmt = conn.createStatement();
                        stmt.executeQuery(query);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            //je modifikovany
            if (currentOwner.modifiedOwner) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE majitele SET " +
                        "jmeno = ?, adresa = ? WHERE id_majitele = ?")) {
                    stmt.setString(1, currentOwner.jmeno);
                    stmt.setString(2, currentOwner.adresa);
                    stmt.setInt(3, currentOwner.id);
                    stmt.execute();
                    continue;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //je novy
            if (currentOwner.newOwner) {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO " +
                                "majitele (id_majitele, jmeno, adresa) VALUES (?, ?, ?)")) {
                    stmt.setInt(1, currentOwner.id);
                    stmt.setString(2, currentOwner.jmeno);
                    stmt.setString(3, currentOwner.adresa);
                    stmt.execute();
                    continue;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        Map<ObjectInfo, Shape> objects = mergeShapes();

        for (Map.Entry<ObjectInfo, Shape> entry : objects.entrySet()) {
            currentInfo = entry.getKey();
            current = entry.getValue();

            if (!currentInfo.modifiedInfo &&
                    !currentInfo.modifiedGeometry &&
                    !currentInfo.modifiedImage &&
                    !currentInfo.newObject &&
                    !currentInfo.deletedObject) {
                continue; //neni novy ani modifikovany ani smazany, preskocit
            }

            /* Pokud je smazany, smazeme ho i z DB */
            if (currentInfo.deletedObject) {
                String query = "DELETE FROM objekty WHERE id='" + currentInfo.id + "'";
                try {
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }

            /* Pokud je novy, udelame plny insert */
            if (currentInfo.newObject) {
                JGeometry jGeo = ShapeHelper.shape2jGeometry(current);

                try {
                    /* Nejdriv naplnime tabulku 'objekty' */
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO objekty (id," +
                            " nazev,typ,editable,popis,sektor,geometrie," +
                            "existenceOd,existenceDo,rekonstrukce) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?)" +
                            "");
                    STRUCT obj = JGeometry.store(conn, jGeo);
                    stmt.setInt(1, currentInfo.id);
                    stmt.setString(2, currentInfo.nazev);
                    stmt.setString(3, currentInfo.typ);
                    stmt.setBoolean(4, currentInfo.editable);
                    stmt.setString(5, currentInfo.popis);
                    stmt.setInt(6, currentInfo.sektor);
                    stmt.setObject(7, obj);
                    stmt.setDate(8, new java.sql.Date(currentInfo.existenceOd.getTime()));
                    stmt.setDate(9, new java.sql.Date(currentInfo.existenceDo.getTime()));
                    stmt.setDate(10, new java.sql.Date(currentInfo.rekonstrukce.getTime()));
                    stmt.execute();

                    /* Ted tabulku 'majitele_objekty' */
                    stmt = conn.prepareStatement("INSERT INTO majitele_objekty " +
                            "(IDOBJEKTU,IDMAJITELE,MAJITELOD,MAJITELDO) VALUES (?,?,?,?)");

                    for (int i = 0; i < currentInfo.majitele.size(); i++) {
                        stmt.setInt(1, currentInfo.id);
                        stmt.setInt(2, currentInfo.majitele.get(i).id);
                        stmt.setDate(3, new java.sql.Date(currentInfo.majitelOd.get(i)
                                .getTime()));
                        stmt.setDate(4, new java.sql.Date(currentInfo.majitelDo.get(i)
                                .getTime()));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();

                    /* Ted obrazek */
                    if (currentInfo.modifiedImage) {
                        try {
                            currentInfo.saveFotoToDB();
                        } catch (IOException ex) {
                            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    continue;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /* Je zmenena geometrie, aktualizujeme */
            if (currentInfo.modifiedGeometry) {
                JGeometry jGeo = ShapeHelper.shape2jGeometry(current);
                //Nejspis se to musi vsechno vyjmenovat pokud chceme doplnovat pozdeji
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE objekty SET " +
                        "geometrie = ? WHERE id = ?")) {
                    STRUCT obj = JGeometry.store(conn, jGeo);
                    stmt.setObject(1, obj);
                    stmt.setInt(2, currentInfo.id);

                    stmt.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /* Modifikovane informace, aktualizujeme */
            if (currentInfo.modifiedInfo) {
                /* Nejdriv update tabulky 'objekty' */
                try {
                    PreparedStatement stmt = conn.prepareStatement("UPDATE objekty " +
                            "SET nazev=?,typ=?,editable=?,popis=?,sektor=?," +
                            "existenceOd=?,existenceDo=?,rekonstrukce=?" +
                            "WHERE id = ?");

                    stmt.setString(1, currentInfo.nazev);
                    stmt.setString(2, currentInfo.typ);
                    stmt.setBoolean(3, currentInfo.editable);
                    stmt.setString(4, currentInfo.popis);
                    stmt.setInt(5, currentInfo.sektor);
                    stmt.setDate(6, new java.sql.Date(currentInfo.existenceOd.getTime()));
                    stmt.setDate(7, new java.sql.Date(currentInfo.existenceDo.getTime()));
                    stmt.setDate(8, new java.sql.Date(currentInfo.rekonstrukce.getTime()));
                    stmt.setInt(9, currentInfo.id);

                    stmt.execute();

                    /* Ted tabulka 'majitele_objekty'
                    Kvuli vsem moznym zmenam v majitelich, obdobich, poradi atd je
                    nejlepsi udelat full-refresh
                     */
                    Statement delStmt = conn.createStatement();
                    delStmt.executeQuery("DELETE FROM majitele_objekty WHERE idobjektu = " +
                            "'"+currentInfo.id + "'");
                    /* Ted je vlozime nazpet */
                    stmt = conn.prepareStatement("INSERT INTO majitele_objekty " +
                            "(IDOBJEKTU,IDMAJITELE,MAJITELOD,MAJITELDO) VALUES (?,?,?,?)");

                    for (int i = 0; i < currentInfo.majitele.size(); i++) {
                        stmt.setInt(1, currentInfo.id);
                        stmt.setInt(2, currentInfo.majitele.get(i).id);
                        stmt.setDate(3, new java.sql.Date(currentInfo.majitelOd.get(i)
                                .getTime()));
                        stmt.setDate(4, new java.sql.Date(currentInfo.majitelDo.get(i)
                                .getTime()));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //modifikace obrázku
            if (currentInfo.modifiedImage) {
                try {
                    currentInfo.saveFotoToDB();
                } catch (IOException ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        dataSaved();
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
