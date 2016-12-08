package cz.vutbr.fit.pdb.realitnikancelar;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import java.awt.*;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper pro manipulaci s databazi
 * Created by jiri on 6.12.16.
 */
public class DatabaseHelper {
    private static Connection conn = ConnectDialog.conn;

    /**
     * Smaze majitele z databaze
     *
     * @param currentOwner
     */
    public static void deleteOwner(Owner currentOwner) {
        String query = "DELETE FROM majitele WHERE id_majitele='" + currentOwner.id + "'";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Upravi majitele v databazi
     *
     * @param currentOwner
     */
    public static void modifyOwner(Owner currentOwner) {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE majitele SET " +
                "jmeno = ?, adresa = ? WHERE id_majitele = ?")) {
            stmt.setString(1, currentOwner.jmeno);
            stmt.setString(2, currentOwner.adresa);
            stmt.setInt(3, currentOwner.id);
            stmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prida noveho majitele
     *
     * @param currentOwner
     */
    public static void newOwner(Owner currentOwner) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO " +
                "majitele (id_majitele, jmeno, adresa) VALUES (?, ?, ?)")) {
            stmt.setInt(1, currentOwner.id);
            stmt.setString(2, currentOwner.jmeno);
            stmt.setString(3, currentOwner.adresa);
            stmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Smaze objekt z databaze
     *
     * @param currentInfo
     */
    public static void deleteObject(ObjectInfo currentInfo) {
        String query = "DELETE FROM objekty WHERE id='" + currentInfo.id + "'";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vlozi kompletne novy objekt
     *
     * @param current
     * @param currentInfo
     */
    public static void newObject(Shape current, ObjectInfo currentInfo) {
        JGeometry jGeo = null;
        try {
            jGeo = ShapeHelper.shape2jGeometry(current);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }

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
            if (currentInfo.existenceDo != null) {
                stmt.setDate(9, new java.sql.Date(currentInfo.existenceDo.getTime()));
            } else {
                stmt.setNull(9, Types.DATE);
            }
            if (currentInfo.existenceDo != null) {
                stmt.setDate(10, new java.sql.Date(currentInfo.rekonstrukce.getTime()));
            } else {
                stmt.setNull(10, Types.DATE);
            }
            stmt.execute();
            
            /*Updatujeme sektor*/
            DatabaseHelper.setSector(currentInfo.id);
            /* Ted tabulku 'majitele_objekty' */
            stmt = conn.prepareStatement("INSERT INTO majitele_objekty " +
                    "(IDOBJEKTU,IDMAJITELE,MAJITELOD,MAJITELDO) VALUES (?,?,?,?)");

            for (int i = 0; i < currentInfo.majitele.size(); i++) {
                stmt.setInt(1, currentInfo.id);
                if (currentInfo.majitele.get(i) == null) {
                    stmt.setInt(2, 0);
                } else {
                    stmt.setInt(2, currentInfo.majitele.get(i).id);
                }
                stmt.setDate(3, new java.sql.Date(currentInfo.majitelOd.get(i)
                        .getTime()));
                if (currentInfo.majitelDo.get(i) != null) {
                    stmt.setDate(4, new java.sql.Date(currentInfo.majitelDo.get(i)
                            .getTime()));
                }
                else {
                    stmt.setNull(4, Types.DATE);
                }

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Funkce zjisti, zda objekt leží v nějakém sektoru, pokud ano, vrátí jeho id.
     * @param id
     * @return 
     */
    public static void setSector (int id) throws SQLException{
        Statement stmt = ConnectDialog.conn.createStatement();
        
        ResultSet rset = stmt.executeQuery("SELECT s.id FROM objekty o, sektor s " +
            " WHERE SDO_RELATE(o.geometrie, s.geometrie, 'mask=INSIDE')='TRUE' "+
            "AND s.id <> 0 AND o.id="+id);
        if (rset.next()){
            String updateSQL = "UPDATE objekty SET sektor = "+rset.getInt("id")+
                " WHERE id = "+id;
            ResultSet rset2 = stmt.executeQuery(updateSQL);
            rset2.next();           
        }
        else{
            
        }
    }

    public static void modifyObjectGeometry(Shape current, ObjectInfo currentInfo) {
        JGeometry jGeo = null;
        try {
            jGeo = ShapeHelper.shape2jGeometry(current);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
        //Nejspis se to musi vsechno vyjmenovat pokud chceme doplnovat pozdeji
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE objekty SET " +
                "geometrie = ? WHERE id = ?")) {
            STRUCT obj = JGeometry.store(conn, jGeo);
            stmt.setObject(1, obj);
            stmt.setInt(2, currentInfo.id);

            stmt.execute();
            //kontrola sektoru
            DatabaseHelper.setSector(currentInfo.id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void modifyObjectInfo(ObjectInfo currentInfo) {
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
            if (currentInfo.existenceDo != null) {
                stmt.setDate(7, new java.sql.Date(currentInfo.existenceDo.getTime()));
            } else {
                stmt.setNull(7, Types.DATE);
            }
            if (currentInfo.rekonstrukce!= null) {
                stmt.setDate(8, new java.sql.Date(currentInfo.rekonstrukce.getTime()));
            } else {
                stmt.setNull(8, Types.DATE);
            }
            stmt.setInt(9, currentInfo.id);

            stmt.execute();

            /* Ted tabulka 'majitele_objekty'
            Kvuli vsem moznym zmenam v majitelich, obdobich, poradi atd je
            nejlepsi udelat full-refresh
            */
            Statement delStmt = conn.createStatement();
            delStmt.executeQuery("DELETE FROM majitele_objekty WHERE idobjektu = " +
                    "'" + currentInfo.id + "'");
                    /* Ted je vlozime nazpet */
            stmt = conn.prepareStatement("INSERT INTO majitele_objekty " +
                    "(IDOBJEKTU,IDMAJITELE,MAJITELOD,MAJITELDO) VALUES (?,?,?,?)");

            for (int i = 0; i < currentInfo.majitele.size(); i++) {
                stmt.setInt(1, currentInfo.id);
                if (currentInfo.majitele.get(i) != null) {
                    stmt.setInt(2, currentInfo.majitele.get(i).id);
                } else {
                    //vychozi majitel je 0, musi byt v databazi
                    stmt.setInt(2, 0);
                }
                stmt.setDate(3, new java.sql.Date(currentInfo.majitelOd.get(i)
                        .getTime()));
                if (currentInfo.majitelDo.get(i) != null) {
                    stmt.setDate(4, new java.sql.Date(currentInfo.majitelDo.get(i)
                            .getTime()));
                }
                else {
                    stmt.setNull(4, Types.DATE);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
