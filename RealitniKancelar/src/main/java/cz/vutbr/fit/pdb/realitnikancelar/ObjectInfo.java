/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.SortedSet;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;

/**
 *
 * @author Honza
 * Trida, ktera reprezentuje veskere informace o jednom objektu.
 * To zahrnuje jak veci z databaze, tak nejake pomocne informace jako jestli je objekt prave oznaceny.
 */
public class ObjectInfo {
    
    final public static int NUM_TYPES = 4;
    final public static String[] TYPES = { "Dům", "Řeka", "Autobusová zastávka", "Silnice" };
    
    public int id;
    public String nazev;
    public String typ;
    public boolean editable = true;
    public String popis;
    public ArrayList<Owner> majitele = new ArrayList<>();
    public ArrayList<Date> majitelOd = new ArrayList<>();
    public ArrayList<Date> majitelDo = new ArrayList<>();
    public Integer sektor;
    public Date existenceOd;
    public Date existenceDo;
    public Date rekonstrukceOd;
    public Date rekonstrukceDo;
    private static TreeSet<Integer> ids = new TreeSet<Integer>();
    public boolean selected = false; //jestli je objekt vybrany
    public boolean hovered = false; //jestli je prave nad objektem mys
    public boolean newObject; //jestli je to nove vytvoreny objekt, ktery jeste neni v DB
    public boolean modifiedGeometry; //jestli je to objekt z DB, kteremu byla v aplikaci modifikovana geometrie, a je potreba udelat update v DB
    public boolean modifiedInfo; //jestli je to objekt z DB, kteremu byly v aplikaci modifikovany informace, a je potreba udelat update v DB
    public boolean modifiedImage; //jestli je to objekt z DB, kteremu byl v aplikaci modifikovan obrazek, a je potreba udelat update v DB
    public boolean deletedObject; //pokud je true, tento objekt byl v aplikaci smazan, ignoruje se a z aplikace bude smazan az pri aktualizaci DB
    
    OrdImage imgIcon;
            
    //load je true, pokud budou data pro tento objekt nactena z DB
    @SuppressWarnings("deprecation")
    public ObjectInfo(boolean load) {
        this.id = nextId();
        this.nazev = "Novy objekt";
        this.typ = "Typ objektu";
        this.editable = true;
        this.popis = "popis";
        //this.majitele = new ArrayList<>();
        this.majitele.add(Owner.defaultOwner());
        this.sektor = 0;
        this.majitelOd.add(new Date(50, 1, 3));
        this.majitelDo.add(new Date(50, 1, 3));
        this.existenceOd = new Date(50, 1, 3);
        this.existenceDo = new Date(50, 1, 3);
        this.rekonstrukceOd = new Date(50, 1, 3);
        this.rekonstrukceDo = new Date(50, 1, 7);
        
        //pokud se nejedna o nacteni z DB, je novy
        this.newObject = !load;
        this.deletedObject = false;
        this.modifiedGeometry = false;
        this.modifiedInfo =  false;
        this.modifiedImage = false;
        
        ids.add(this.id);
        
    }

    private int nextId() {
        if (ids.size() != 0) {
            int last = ids.last();
            return last + 1;
        }
        return 0;
    }

    public static ObjectInfo createFromDB(ResultSet res) throws SQLException {
        ObjectInfo info = new ObjectInfo(true);
        info.id = res.getInt("id");
        info.nazev = res.getString("nazev");
        info.typ = res.getString("typ");
        info.editable = true;
        info.popis = res.getString("popis");
        info.majitele.add(Owner.getOwner(res.getInt("majitel")));
        info.sektor = res.getInt("sektor");
        info.majitelOd.add(res.getDate("majitelod"));
        info.majitelDo.add(res.getDate("majiteldo"));
        info.existenceOd = res.getDate("existenceOd");
        info.existenceDo = res.getDate("existenceDo");
        info.rekonstrukceOd = res.getDate("rekonstrukceOd");
        info.rekonstrukceDo = res.getDate("rekonstrukceDo");
        
        ids.add(info.id);
        //info.imgIcon = info.loadFotoFromDB();
        return info;
    }
    //obrazek tady taky bude
    
    /**
     * Funkce vytvoří nový prostor pro obrázek v databázi a následně ho tam uloží. 
     * Bere obrázek z lokálního disku.
     * @param filename
     * @throws java.sql.SQLException
     */
    public void saveFotoToDB(String filename) throws SQLException {
        ConnectDialog.conn.setAutoCommit(false);
        System.out.println("nový objekt: "+this.newObject+"\n");
        try {
            int imgID;
            String insertSQL;
            //Vytvoření místa v databázi pro nový obrázek
            try (Statement stmt1 = ConnectDialog.conn.createStatement()) {
                //Vytvoření místa v databázi pro nový obrázek
                ResultSet res = stmt1.executeQuery("SELECT obrazky_seq.NEXTVAL from DUAL");
                imgID = 0;
                while (res.next()) {
                    imgID = Integer.parseInt(res.getString(1));
                }   insertSQL = "INSERT INTO obrazky(id, objekt, img) VALUES"+
                        " ("+imgID+","+this.id+",ordsys.ordimage.init())";
                stmt1.executeUpdate(insertSQL);
            }
            
            OrdImage imgProxy;
            //Načtení vytvořeného místa pro nahrání obrázku
            try ( 
                    Statement stmt2 = ConnectDialog.conn.createStatement()) {
                String selSQL = "SELECT img FROM obrazky WHERE id = "+imgID+
                        " FOR UPDATE";
                try (OracleResultSet rset = (OracleResultSet) stmt2.executeQuery(selSQL)) {
                    rset.next();
                    imgProxy = (OrdImage) rset.getORAData("img", OrdImage.getORADataFactory());
                }
            }
            
            //Načtení obrázku z disku do databáze
            imgProxy.loadDataFromFile(filename);
            imgProxy.setProperties();
            
            //Update tabulky
            String updateSQL1 = "UPDATE obrazky SET"+" img=? WHERE id = "+imgID;
            System.out.println("insertSQL: "+insertSQL+"\n");
            try (OraclePreparedStatement pstmt = (OraclePreparedStatement)
                    ConnectDialog.conn.prepareStatement (updateSQL1)) {
                pstmt.setORAData (1,imgProxy) ;
                pstmt.executeUpdate() ;
            }
            
            try ( //Update tabulky StillImage
                    Statement stmt3 = ConnectDialog.conn.createStatement()) {
                String updateSQL2 = "UPDATE obrazky p SET " +
                        " p.img_si = SI_StillImage(p.img.getContent())where id = " + imgID ;
                stmt3.executeUpdate(updateSQL2) ;
                String updateSQL3 = "UPDATE obrazky p SET " +
                        " p.img_ac=SI_AverageColor(p.img_si), " +
                        " p.img_ch=SI_ColorHistogram(p.img_si), " +
                        " p.img_pc=SI_PositionalColor(p.img_si), " +
                        " p.img_tx=SI_Texture(p.img_si) where id = " + imgID;
                stmt3.executeUpdate(updateSQL3);
            }
            ConnectDialog.conn.commit();
            ConnectDialog.conn.setAutoCommit(true);

        } catch (SQLException e) {
            System.err.println("SQLexc: "+e.getMessage());
        } catch (NumberFormatException | IOException e) {
            System.err.println("Exeption: "+e.getMessage());
        }
    }
    /**
     * Funkce načte obrázek z databáze a vrátí jej.
     */
    public OrdImage loadFotoFromDB () throws SQLException {
        try (Statement stmt = ConnectDialog.conn.createStatement()) {
            OracleResultSet rset = (OracleResultSet) stmt.executeQuery(
                    "select * from obrazky where objekt = 0");
            
            rset.next();
            int id = rset.getInt("id");
            OrdImage imgProxy = (OrdImage)
                    rset.getORAData("img", OrdImage.getORADataFactory());
            // retrieve the media attributes
            int height = imgProxy.getHeight();
            int width = imgProxy.getWidth();
            System.out.println("Photo "+ id +": "+ height +"x"+ width);

            //imgProxy.getDataInFile("./car"+ id +"-out.jpg");
            
            
            rset.close();
            return imgProxy;
        }
    }
    
    /**
     * Funkce updatuje obrázek uložený v databázi, provádí i otočení.
     */
    public void updateFotoToDB () throws SQLException {
        
    }
    
    /**
     * Funkce najde 4 podobné obrázky objektů pro vybraný objekt.
     */
    public void findSimilarFoto (){
        
    }
}
