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
        Connection conn = ConnectDialog.conn;
        PreparedStatement del = conn.prepareStatement("DELETE FROM sektor");
        del.execute();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO sektor (id," +
                " nazev,geometrie) VALUES (0,'Test sektor',NULL)");
        {
            stmt.execute();
        }
        return sektor;
    }

}
