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
    public boolean newOwner; //jestli je to nove vytvoreny owner, ktery jeste neni v DB
    public boolean modifiedOwner; //jestli je to owner z DB, ktery byl v aplikaci modifikovany, a je potreba udelat update v DB
    public boolean deletedOwner; //pokud je true, tento majitel byl v aplikaci smazan, ignoruje se a z aplikace bude smazan az pri aktualizaci DB
    private static TreeSet<Integer> ids = new TreeSet<Integer>();
    
    //load je true, pokud budou data pro tento objekt nactena z DB
    public Owner(boolean load) {
        this.id = nextId();
        this.jmeno = "Novy majitel";
        this.adresa = "Adresa";
        
        //pokud se nejedna o nacteni z DB, je novy
        this.newOwner = !load;
        this.modifiedOwner = false;
        this.deletedOwner = false;
        
        ids.add(this.id);
    }
    
    //load je true, pokud byla data pro tento objekt nactena z DB
    public Owner(String jmeno, String adresa, boolean load)
    {
        this.id = nextId();
        this.jmeno = jmeno;
        this.adresa = adresa;
        
        //pokud se nejedna o nacteni z DB, je novy
        this.newOwner = !load;
        this.modifiedOwner = false;
        this.deletedOwner = false;
        
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
