package cz.vutbr.fit.pdb.realitnikancelar;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sektor
 *
 * @author jiri
 */
public class Sektor {
    public Integer id;
    public String nazev;
    public Polygon geometrie;

    public static Sektor testovaciSektor() throws SQLException {
        Sektor sektor = new Sektor();
        sektor.id = 0;
        sektor.nazev = "Nový sektor";
        sektor.geometrie = null;
        return sektor;
    }

}
