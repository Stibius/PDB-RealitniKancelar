package cz.vutbr.fit.pdb.realitnikancelar;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by jiri on 3.12.16.
 */
public class Sektor {
    public Integer id;
    public String nazev;
    public JGeometry geometrie;

    public static Sektor testovaciSektor() throws SQLException {
        Sektor sektor = new Sektor();
        sektor.id = 0;
        sektor.nazev = "Testovaci sektor";
        sektor.geometrie = null;
        return sektor;
    }

}
