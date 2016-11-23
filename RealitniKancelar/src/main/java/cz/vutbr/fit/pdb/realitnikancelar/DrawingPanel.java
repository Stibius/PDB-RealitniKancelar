/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.geom.Line2D;


/**
 *
 * @author Honza
 * Trida pro platno, kam se kresli.
 */
public class DrawingPanel extends JPanel {
    
    public Color pointColor = new Color(0, 0, 0); 
    public Color lineColor = new Color(0, 0, 0); 
    public Color fillColor = new Color(0, 0, 0); 
    public Color backColor = new Color(0, 0, 0); 
    public Color selectionLineColor = new Color(0, 0, 0); 
    public Color selectionFillColor = new Color(0, 0, 0); 
    public int lineThickness; 
    public int pointThickness;  
    
    public BufferedImage image; //obrazek, kam se vsechno kresli
    
    DrawingPanel() { 
    
    }
    
    //nakresli mapu, ktera odpovida aktualnimu obsahu tridy Data
    public void createImageFromData() {
        this.setPreferredSize(new Dimension(Data.width, Data.height));
        image = new BufferedImage(Data.width, Data.height, BufferedImage.TYPE_INT_RGB);
       
        Graphics2D g = image.createGraphics();
        
        g.setColor(backColor);
        g.fillRect(0, 0, image.getWidth(), image.getHeight()); 
        
        for (int i = 0; i < Data.points.size(); i++) {
            if (Data.pointsInfo.get(i).hovered || Data.pointsInfo.get(i).selected)
            {
                g.setColor(selectionLineColor); 
            }
            else
            {
                g.setColor(pointColor); 
            }
            if (pointThickness == 1) {
                g.setStroke(new BasicStroke(pointThickness));
                g.drawLine(Data.points.get(i).x, Data.points.get(i).y, Data.points.get(i).x, Data.points.get(i).y);
            }
            else {
                g.fillOval(Data.points.get(i).x - (pointThickness / 2), 
                        Data.points.get(i).y - (pointThickness / 2), 
                        pointThickness, 
                        pointThickness);
            }
        }
        
        g.setStroke(new BasicStroke(lineThickness));
        
        for (int i = 0; i < Data.polylines.size(); i++) {
            if (Data.polylinesInfo.get(i).hovered || Data.polylinesInfo.get(i).selected)
            {
                g.setColor(selectionLineColor); 
            }
            else
            {
                g.setColor(lineColor); 
            }
            for (int j = 1; j < Data.polylines.get(i).size(); j++)
            {
                Line2D line = new Line2D.Double(
                        Data.polylines.get(i).get(j-1).x,
                        Data.polylines.get(i).get(j-1).y,
                        Data.polylines.get(i).get(j).x,
                        Data.polylines.get(i).get(j).y);
                g.draw(line);
            }
        }
        
        for (int i = 0; i < Data.rectangles.size(); i++) {
            if (Data.rectanglesInfo.get(i).selected)
            {
                g.setColor(selectionFillColor); 
            }
            else
            {
                g.setColor(fillColor); 
            }
            g.fillRect(Data.rectangles.get(i).x, Data.rectangles.get(i).y, Data.rectangles.get(i).width, Data.rectangles.get(i).height);
        }
        for (int i = 0; i < Data.rectangles.size(); i++) {
            if (Data.rectanglesInfo.get(i).hovered || Data.rectanglesInfo.get(i).selected)
            {
                g.setColor(selectionLineColor);
            }
            else
            {
                g.setColor(lineColor); 
            }
            g.drawRect(Data.rectangles.get(i).x, Data.rectangles.get(i).y, Data.rectangles.get(i).width, Data.rectangles.get(i).height);
        }
        
        for (int i = 0; i < Data.ellipses.size(); i++) {
            if (Data.ellipsesInfo.get(i).selected)
            {
                g.setColor(selectionFillColor); 
            }
            else
            {
                g.setColor(fillColor); 
            }
            g.fillOval((int)Data.ellipses.get(i).getX(), (int)Data.ellipses.get(i).getY(), (int)Data.ellipses.get(i).getWidth(), (int)Data.ellipses.get(i).getHeight());
        }
        for (int i = 0; i < Data.ellipses.size(); i++) {
            if (Data.ellipsesInfo.get(i).hovered || Data.ellipsesInfo.get(i).selected)
            {
                g.setColor(selectionLineColor);
            }
            else
            {
                g.setColor(lineColor); 
            }
            g.drawOval((int)Data.ellipses.get(i).getX(), (int)Data.ellipses.get(i).getY(), (int)Data.ellipses.get(i).getWidth(), (int)Data.ellipses.get(i).getHeight());
        }

        for (int i = 0; i < Data.polygons.size(); i++) {
            if (Data.polygonsInfo.get(i).selected)
            {
                g.setColor(selectionFillColor);
            }
            else
            {
                g.setColor(fillColor);
            }
            g.fillPolygon(Data.polygons.get(i));
        }
        for (int i = 0; i < Data.polygons.size(); i++) {
            if (Data.polygonsInfo.get(i).hovered || Data.polygonsInfo.get(i).selected)
            {
                g.setColor(selectionLineColor);
            }
            else
            {
                g.setColor(lineColor); 
            }
            g.drawPolygon(Data.polygons.get(i));
        }

        this.repaint();
    }

    //vola se pri prekreslovani platna
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        //vykresli aktualni obsah image
        g.drawImage(image, 0, 0, this);
    }  
}
