/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.sql.*;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Majitel
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
    
    /**
     * 
     * @param load je true, pokud budou data pro tento objekt nactena z DB
     */
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
    
    public Owner(Integer id, String jmeno, String adresa) {
        this.id = id;
        this.jmeno = jmeno;
        this.adresa = adresa;

        //pokud se nejedna o nacteni z DB, je novy
        this.newOwner = false;
        this.modifiedOwner = false;
        this.deletedOwner = false;

        ids.add(this.id);
    }
    
    /**
     * 
     * @param load je true, pokud budou data pro tento objekt nactena z DB
     */
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
        return 1;
    }
    
    public static Owner create(ResultSet res) throws SQLException {
        Owner majitel = new Owner(true);
        majitel.id = res.getInt("id");
        majitel.jmeno = res.getString("nazev");
        majitel.adresa = res.getString("typ");
        ids.add(majitel.id);
        
   
        
        
        return majitel;
    }


    public static Owner getOwner(Integer majitel) {
        if (majitel != null) {
            try (Statement stmt = ConnectDialog.conn.createStatement()) {
                ResultSet res = stmt.executeQuery("SELECT * FROM MAJITELE WHERE " +
                        "id_majitele" +
                        " = " +
                        ""+majitel);
                if (res.next()) {
                    return new Owner(res.getInt("id_majitele"), res.getString("jmeno"),res
                            .getString("adresa"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static Owner defaultOwner() {
        //Nastavi vychoziho majitele. Ma id 0 a je v SQL skriptu
        return Owner.getOwner(0);
    }

    public static ArrayList<Owner> loadOwners() {
        ArrayList<Owner> owns = new ArrayList<>();
        try (Statement stmt = ConnectDialog.conn.createStatement()) {
            ResultSet res = stmt.executeQuery("SELECT * FROM majitele");
            while (res.next()) {
                if (res.getInt("id_majitele") == 0) {
                    continue;
                }
                owns.add(new Owner(res.getInt("id_majitele"),res.getString("jmeno"), res
                        .getString("adresa")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return owns;
    }
}
