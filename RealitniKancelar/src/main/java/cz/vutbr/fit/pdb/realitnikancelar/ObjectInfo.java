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

/**
 *
 * @author Honza
 * Trida, ktera reprezentuje veskere informace o jednom objektu.
 * To zahrnuje jak veci z databaze, tak nejake pomocne informace jako jestli je objekt prave oznaceny.
 */
public class ObjectInfo {
    public int id;
    public String nazev;
    public String typ;
    public boolean editable = true;
    public String popis;
    public String majitel;
    public String sektor;
    public Date obdobi;
    private static TreeSet<Integer> ids = new TreeSet<Integer>();
    public boolean selected = false; //jestli je objekt vybrany
    public boolean hovered = false; //jestli je prave nad objektem mys

    public ObjectInfo() {
        this.id = nextId();
        this.nazev = "Novy objekt";
        this.typ = "Typ objektu";
        this.editable = true;
        this.popis = "popis";
        this.majitel = "majitel";
        this.sektor = "sektor";
        this.obdobi = null;
        ids.add(this.id);
    }
    public ObjectInfo(Boolean load)
    {
        return;
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
        info.majitel = res.getString("majitel");
        info.sektor = res.getString("sektor");
        info.obdobi = res.getDate("obdobiod");
        ids.add(info.id);
        return info;
    }
    //obrazek tady taky bude
}
