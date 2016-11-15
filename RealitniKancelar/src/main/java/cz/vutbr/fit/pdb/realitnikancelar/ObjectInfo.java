/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

/**
 *
 * @author Honza
 * Trida, ktera reprezentuje veskere informace o jednom objektu.
 * To zahrnuje jak veci z databaze, tak nejake pomocne informace jako jestli je objekt prave oznaceny.
 */
public class ObjectInfo {
    public int id;
    public String name;
    public String type;
    public boolean editable = true;
    public String description;
    public String owner;
    public String sector;
    public boolean selected = false; //jestli je objekt vybrany
    public boolean hovered = false; //jestli je prave nad objektem mys
    //obrazek tady taky bude
}
