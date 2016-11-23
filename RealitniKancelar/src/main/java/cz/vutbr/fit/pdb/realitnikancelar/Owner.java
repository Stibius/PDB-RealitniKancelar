/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.util.TreeSet;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Honza
 */
public class Owner {
    public int id;
    public String jmeno;
    public String adresa;
    private static TreeSet<Integer> ids = new TreeSet<Integer>();
    
    public Owner() {
        this.id = nextId();
        this.jmeno = "Novy majitel";
        this.adresa = "Adresa";
        ids.add(this.id);
    }
    
    public Owner(Boolean load)
    {
        return;
    }
    
    public Owner(String jmeno, String adresa)
    {
        this.id = nextId();
        this.jmeno = jmeno;
        this.adresa = adresa;
        ids.add(this.id);
    }
    
    private int nextId() {
        if (ids.size() != 0) {
            int last = ids.last();
            return last + 1;
        }
        return 0;
    }
    
    public static Owner create(ResultSet res) throws SQLException {
        Owner majitel = new Owner(true);
        majitel.id = res.getInt("id");
        majitel.jmeno = res.getString("nazev");
        majitel.adresa = res.getString("typ");
        ids.add(majitel.id);
        return majitel;
    }
    
    
}
