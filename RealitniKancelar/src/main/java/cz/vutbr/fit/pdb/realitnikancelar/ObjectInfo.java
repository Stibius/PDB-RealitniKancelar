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
    public boolean selected = false; //jestli je objekt vybrany
    public boolean hovered = false; //jestli je prave nad objektem mys

    public static ObjectInfo create(ResultSet res) throws SQLException {
        ObjectInfo info = new ObjectInfo();
        info.id = res.getInt("id");
        info.nazev = res.getString("nazev");
        info.typ = res.getString("typ");
        info.editable = true;
        info.popis = res.getString("popis");
        info.majitel = res.getString("majitel");
        info.sektor = res.getString("sektor");
        info.obdobi = res.getDate("obdobiod");
        return info;
    }
    //obrazek tady taky bude
}
