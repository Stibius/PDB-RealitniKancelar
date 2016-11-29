/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.SortedSet;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;
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
    public String sektor;
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
    
    //load je true, pokud budou data pro tento objekt nactena z DB
    public ObjectInfo(boolean load) {
        this.id = nextId();
        this.nazev = "Novy objekt";
        this.typ = "Typ objektu";
        this.editable = true;
        this.popis = "popis";
        this.majitele = new ArrayList<>();
        this.sektor = "sektor";
        this.majitelOd = new ArrayList<>();
        this.majitelDo = new ArrayList<>();
        this.existenceOd = new Date(0, 1, 1);
        this.existenceDo = null;
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

    public static ObjectInfo create(ResultSet res) throws SQLException {
        ObjectInfo info = new ObjectInfo(true);
        info.id = res.getInt("id");
        info.nazev = res.getString("nazev");
        info.typ = res.getString("typ");
        info.editable = true;
        info.popis = res.getString("popis");
        //info.majitel = res.getString("majitel");
        info.sektor = res.getString("sektor");
        //info.majitelOd = res.getDate("obdobiod");
        //info.majitelDo = res.getDate("obdobiod");
        info.existenceOd = res.getDate("obdobiod");
        info.existenceDo = res.getDate("obdobiod");
        info.rekonstrukceOd = res.getDate("obdobiod");
        info.rekonstrukceDo = res.getDate("obdobiod");
        
        ids.add(info.id);
        return info;
    }
    //obrazek tady taky bude
}
