/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.SortedSet;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;
import javax.imageio.ImageIO;
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
    public Date rekonstrukce;
    public static TreeSet<Integer> ids = new TreeSet<Integer>();
    public boolean selected = false; //jestli je objekt vybrany
    public boolean hovered = false; //jestli je prave nad objektem mys
    public boolean newObject; //jestli je to nove vytvoreny objekt, ktery jeste neni v DB
    public boolean modifiedGeometry; //jestli je to objekt z DB, kteremu byla v aplikaci modifikovana geometrie, a je potreba udelat update v DB
    public boolean modifiedInfo; //jestli je to objekt z DB, kteremu byly v aplikaci modifikovany informace, a je potreba udelat update v DB
    public boolean modifiedImage; //jestli je to objekt z DB, kteremu byl v aplikaci modifikovan obrazek, a je potreba udelat update v DB
    public boolean deletedObject; //pokud je true, tento objekt byl v aplikaci smazan, ignoruje se a z aplikace bude smazan az pri aktualizaci DB
    public boolean rotateImage; //jestli se bude otacet obrazek
    
    BufferedImage imgIcon;
    String imgPath;
    
    public int plocha;
    public int obvod;
    public String nejblizsiZastavka;

    /**
     * Inicializace prazdneho ObjectInfo
     */
    public ObjectInfo() {
        this.deletedObject = false;
        this.modifiedGeometry = false;
        this.modifiedInfo =  false;
        this.modifiedImage = false;
        this.rotateImage = false;
        this.newObject = false; //implicitne se nejedna o novy objekt
    }

    /**
     * Inicializace noveho ObjectInfo
     * @param load
     */
    //load je true, pokud budou data pro tento objekt nactena z DB
    @SuppressWarnings("deprecation")
    public ObjectInfo(boolean load) {
        this.id = nextId();
        this.nazev = "Novy objekt";
        this.typ = "Typ objektu";
        this.editable = true;
        this.popis = "popis";
        this.majitele = new ArrayList<>();
        this.sektor = 0;
        this.majitelOd.add(new Date(50, 10, 3));
        this.majitelDo.add(new Date(50, 10, 3));
        this.existenceOd = new Date(50, 10, 3);
        this.existenceDo = null;
        this.rekonstrukce = null;
        
        //pokud se nejedna o nacteni z DB, je novy
        this.newObject = !load;
        this.deletedObject = false;
        this.modifiedGeometry = false;
        this.modifiedInfo =  false;
        this.modifiedImage = false;
        this.rotateImage = false;
        this.imgPath = "./img/nophoto.jpg";
        
        ids.add(this.id);
        
        this.plocha = 0;
        this.obvod = 0;
        this.nejblizsiZastavka = "";
        
    }

    private int nextId() {
        if (ids.size() != 0) {
            int last = ids.last();
            return last + 1;
        }
        return 0;
    }

    public static ObjectInfo createFromDB(ResultSet res) throws SQLException {
        ObjectInfo info = new ObjectInfo();
        info.id = res.getInt("id");
        info.nazev = res.getString("nazev");
        info.typ = res.getString("typ");
        info.editable = true;
        info.popis = res.getString("popis");
        info.addOwner(res); //prida majitele a doby od do
        info.sektor = res.getInt("sektor");
        info.existenceOd = res.getDate("existenceOd");
        info.existenceDo = res.getDate("existenceDo");
        info.rekonstrukce = res.getDate("rekonstrukce");
        
        ids.add(info.id);
        //info.imgIcon = info.loadFotoFromDB();
        info.plocha = info.getArea(res.getInt("id"));
        info.obvod = info.getCircuit(res.getInt("id"));
        info.nejblizsiZastavka = info.getNearestBusStop(res.getInt("id"));
        return info;
    }
    
    /**
     * Funkce vytvoří nový prostor pro obrázek v databázi a následně ho tam uloží.
     * Funkce také updatuje stávající obrázek.
     * Bere obrázek z lokálního disku.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */

    public void saveFotoToDB() throws SQLException, IOException {
        //Vytvoření místa v databázi pro nový obrázek
        if(this.newObject){
            this.saveNewFotoToDB();
        }
        else{
            ConnectDialog.conn.setAutoCommit(false);    
            // retrieve the previously created ORDImage object for future updating
            Statement stmt2 = ConnectDialog.conn.createStatement();
            String selSQL = "select img from obrazky where objekt="+this.id+" for update";
            OracleResultSet rset = (OracleResultSet) stmt2.executeQuery(selSQL);
            if(!rset.next()){
                this.saveNewFotoToDB();
            }
            else{
                OrdImage imgProxy = (OrdImage)
                rset.getORAData("img", OrdImage.getORADataFactory());
                rset.close();
                stmt2.close();

                // load the media data from a file to the ORDImage Java object
                imgProxy.loadDataFromFile(this.imgPath);
                if (this.rotateImage){
                     imgProxy.process("rotate=90");                   
                }
                // set the properties of the Oracle Mm object from the Java object
                imgProxy.setProperties();

                // update the table with ORDImage Java object (data already loaded)
                String updateSQL1 = "update obrazky set"+
                " img=? where objekt = "+this.id;
                OraclePreparedStatement pstmt = (OraclePreparedStatement)
                        ConnectDialog.conn.prepareStatement(updateSQL1);
                pstmt.setORAData(1, imgProxy);
                pstmt.executeUpdate();
                pstmt.close();

                // update the table with StillImage object and features
                Statement stmt3 = ConnectDialog.conn.createStatement();
                String updateSQL2 = "update obrazky p set"+
                " p.img_si=SI_StillImage(p.img.getContent()) where objekt = "+this.id;
                stmt3.executeUpdate(updateSQL2);
                String updateSQL3 = "update obrazky p set"+
                " p.img_ac=SI_AverageColor(p.img_si),"+
                " p.img_ch=SI_ColorHistogram(p.img_si),"+
                " p.img_pc=SI_PositionalColor(p.img_si),"+
                " p.img_tx=SI_Texture(p.img_si) where objekt = "+this.id;
                stmt3.executeUpdate(updateSQL3);
                stmt3.close();

                ConnectDialog.conn.commit(); // commit the thransaction
                ConnectDialog.conn.setAutoCommit(true); 
            }    
        }
    }
    /**
     * Funkce uloží nový obrázek do databáze.
     * @throws SQLException
     * @throws IOException 
     */
    public void saveNewFotoToDB() throws SQLException, IOException{
        ConnectDialog.conn.setAutoCommit(false);
        int imgID = 0;
        // insert a new record with an empty ORDImage object
        Statement stmt1 = ConnectDialog.conn.createStatement();
        ResultSet res = stmt1.executeQuery("SELECT obrazky_seq.NEXTVAL from DUAL");         
        while (res.next()) {
                imgID = Integer.parseInt(res.getString(1));
        }   
        String insertSQL = "insert into obrazky(id, objekt, img) values"+
                " ("+imgID+","+this.id+", ordsys.ordimage.init())";
        stmt1.executeUpdate(insertSQL);
        stmt1.close();

        // retrieve the previously created ORDImage object for future updating
        Statement stmt2 = ConnectDialog.conn.createStatement();
        String selSQL = "select img from obrazky where objekt="+this.id+" for update";
        OracleResultSet rset = (OracleResultSet) stmt2.executeQuery(selSQL);
        rset.next();
        OrdImage imgProxy = (OrdImage)
                rset.getORAData("img", OrdImage.getORADataFactory());
        rset.close();
        stmt2.close();

        // load the media data from a file to the ORDImage Java object
        imgProxy.loadDataFromFile(this.imgPath);
        if (this.rotateImage){
            imgProxy.process("rotate=90");
        }
        // set the properties of the Oracle Mm object from the Java object
        imgProxy.setProperties();

        // update the table with ORDImage Java object (data already loaded)
        String updateSQL1 = "update obrazky set"+
        " img=? where objekt = "+this.id;
        OraclePreparedStatement pstmt = (OraclePreparedStatement)
                ConnectDialog.conn.prepareStatement(updateSQL1);
        pstmt.setORAData(1, imgProxy);
        pstmt.executeUpdate();
        pstmt.close();

        // update the table with StillImage object and features
        Statement stmt3 = ConnectDialog.conn.createStatement();
        String updateSQL2 = "update obrazky p set"+
        " p.img_si=SI_StillImage(p.img.getContent()) where objekt = "+this.id;
        stmt3.executeUpdate(updateSQL2);
        String updateSQL3 = "update obrazky p set"+
        " p.img_ac=SI_AverageColor(p.img_si),"+
        " p.img_ch=SI_ColorHistogram(p.img_si),"+
        " p.img_pc=SI_PositionalColor(p.img_si),"+
        " p.img_tx=SI_Texture(p.img_si) where objekt = "+this.id;
        stmt3.executeUpdate(updateSQL3);
        stmt3.close();

        ConnectDialog.conn.commit(); // commit the thransaction
        ConnectDialog.conn.setAutoCommit(true);
    }
    /**
     * Funkce načte obrázek z databáze.
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    public void loadFotoFromDB () throws SQLException, IOException {
        Statement stmt = ConnectDialog.conn.createStatement();
        OracleResultSet rset = (OracleResultSet) stmt.executeQuery(
                    "select * from obrazky where objekt = "+this.id);
            
        if(rset.next()){
            int id = rset.getInt("id");
            OrdImage imgProxy = (OrdImage)
                     rset.getORAData("img", OrdImage.getORADataFactory());
            rset.close();
            imgProxy.getDataInFile("./img/out.jpg");
            BufferedImage img = ImageIO.read(new File("./img/out.jpg"));
            this.imgIcon = img;
            this.imgPath = "./img/out.jpg";
        }
        else{
             BufferedImage img = ImageIO.read(new File("./img/nophoto.jpg"));
             this.imgIcon = img;
             this.imgPath = "./img/nophoto.jpg";
        }
        stmt.close();
        rset.close();
    }
    
    /**
     * Funkce stáhne obrázek z databáze a uloží je.
     * @param path
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    public void saveFotoFromDB (String path) throws SQLException, IOException {
        Statement stmt = ConnectDialog.conn.createStatement();
        OracleResultSet rset = (OracleResultSet) stmt.executeQuery(
                    "select * from obrazky where objekt = "+this.id);
            
        if(rset.next()){
            int id = rset.getInt("id");
            OrdImage imgProxy = (OrdImage)
                     rset.getORAData("img", OrdImage.getORADataFactory());
            rset.close();
            imgProxy.getDataInFile(path);
        }
        stmt.close();
        rset.close();
    }
    
    /**
     * Funkce najde 4 podobné obrázky objektů pro vybraný objekt.
     * @throws java.sql.SQLException
     */
    public Object[][] findSimilarFoto () throws SQLException{
        Statement stmt = ConnectDialog.conn.createStatement();
        Object[][] imagesList = new Object [4][2];
        int i = 0;
        String simSQL = "SELECT src.id as source, dst.id as destination, dst.img as img, name.nazev as name, SI_ScoreByFtrList(new SI_FeatureList(src.img_ac,0.3,src.img_ch,0.3, src.img_pc,0.1,src.img_tx,0.3), dst.img_si) as similarity FROM obrazky src, obrazky dst, objekty name WHERE src.id <> dst.id AND src.objekt = "+this.id+" AND dst.objekt = name.id ORDER BY similarity ASC";
        OracleResultSet rset = (OracleResultSet) stmt.executeQuery(simSQL);
        while(rset.next()){
            int j = 0;
            if (i <= 3){
                OrdImage imgProxy = (OrdImage)
                     rset.getORAData("img", OrdImage.getORADataFactory());        
                String name = rset.getString(4);
                imagesList[i][j] = name;
                imagesList[i][j+1] = imgProxy;
                i++;
            }
            else {
            break;
        }
            
        }
        stmt.close();
        rset.close();
        return imagesList;
    }

    public void addOwner(ResultSet res) {
        try {
            if (res.getObject("idmajitele") != null) {
                //je null, tak se nic
                if (res.getInt("idmajitele") == 0) {
                    this.majitele.add(null);
                }
                else {
                    this.majitele.add(Owner.getOwner(res.getInt("idmajitele")));
                }
                this.majitelOd.add(res.getDate("majitelod"));
                this.majitelDo.add(res.getDate("majiteldo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Funkce zjistí obvod vybraného objektu
     * @param id
     * @return
     * @throws SQLException 
     */
    public int getCircuit(int id) throws SQLException{
        Statement stmt = ConnectDialog.conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT SDO_GEOM.SDO_LENGTH(geometrie, 1) obvod " +
           "FROM objekty WHERE id = "+id);
        if (rset.next()){
            int id_sektor = rset.getInt("obvod");
            stmt.close();
            rset.close();
            return id_sektor;
        }
        else{
            stmt.close();
            rset.close();
            return 0;
        }    
    }
    /**
     * Funkce zjisti obsah vybraného objektu
     * @param id
     * @return
     * @throws SQLException 
     */
    public int getArea(int id) throws SQLException{
        Statement stmt = ConnectDialog.conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT SDO_GEOM.SDO_AREA(geometrie, 1) obsah " +
           "FROM objekty WHERE id = "+id);
        if (rset.next()){
            int id_sektor = rset.getInt("obsah");
            stmt.close();
            rset.close();
            return id_sektor;
        }
        else{
            stmt.close();
            rset.close();
            return 0;
        }
    }
    /**
     * Funkce najde nejbližší autobusovou zastávku a vrátí i její vzddálenost od
     * objektu.
     * @param id
     * @return
     * @throws SQLException 
     */
    public String getNearestBusStop(int id) throws SQLException{
        Statement stmt = ConnectDialog.conn.createStatement();
        String bus = "";
        String SQLfindBus = "SELECT /*+ LEADING(p) INDEX(o OBJEKT_GEOMETRIE_SIDX) */" +
                " o.nazev, p.typ, p.nazev as bus, SDO_NN_DISTANCE(1) dist" +
                " FROM objekty o, objekty p WHERE o.id="+id+
                " AND MDSYS.SDO_NN(o.geometrie,p.geometrie,'SDO_NUM_RES=5', 1)='TRUE'"+
                " AND o.id <> p.id AND p.typ LIKE 'Autobusová zastávka' ORDER BY dist";
        ResultSet rset = stmt.executeQuery(SQLfindBus);
        if (rset.next()){
            bus = rset.getString("bus")+" vzdálenost: "+rset.getInt("dist");
            stmt.close();
            rset.close();
            return bus;
        }
        //zastavka je daleko, hledej ve větší vzdálenosti
        else{
            SQLfindBus = "SELECT /*+ LEADING(p) INDEX(o OBJEKT_GEOMETRIE_SIDX) */"+
                " o.nazev, p.typ, p.nazev as bus ,SDO_NN_DISTANCE(1) dist"+
                " FROM objekty o, objekty p WHERE o.id="+id+
                " AND MDSYS.SDO_NN(o.geometrie,p.geometrie,'sdo_batch_size=20 ',1)='TRUE'"+
                " AND o.id <> p.id AND ROWNUM <=2  AND p.typ LIKE 'Autobusová zastávka'";
            ResultSet rset2 = stmt.executeQuery(SQLfindBus);
            if(rset2.next()){
                bus = rset2.getString("bus")+" vzdálenost: "+rset2.getInt("dist");
                stmt.close();
                rset2.close();
                return bus;
            }
        }
        return bus;
    }
}
