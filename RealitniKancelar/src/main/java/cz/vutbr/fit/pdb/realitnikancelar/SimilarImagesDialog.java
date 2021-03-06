/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vutbr.fit.pdb.realitnikancelar;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import oracle.ord.im.OrdImage;

/**
 * Dialog podobných obrázků
 * @author Honza
 */
public class SimilarImagesDialog extends javax.swing.JDialog {

    /**
     * Creates new form SimilarImagesDialog
     * @param parent parent
     * @param modal modal
     */
    public SimilarImagesDialog(java.awt.Frame parent, boolean modal, Object [][] similarImages) {
        super(parent, modal);
        initComponents();
        
        for (int i = 0; i <= 3; i++) {
            switch (i) {
                case 0:
                    {   
                        String name = (String)similarImages[i][0];
                        OrdImage getImg = (OrdImage)similarImages[i][1];
                        if(getImg != null){
                            try {
                                getImg.getDataInFile("./img/similars/out.jpg");
                                BufferedImage img = ImageIO.read(new File("./img/similars/out.jpg"));
                                jLabel1.setText(name);
                                ((DrawingPanel)imagePanel1).image = img;
                            } catch (SQLException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }                            
                        }      
                    }
                    break;
                case 1:
                    {
                        String name = (String)similarImages[i][0];
                        OrdImage getImg = (OrdImage)similarImages[i][1];
                        if(getImg != null){
                            try {
                                getImg.getDataInFile("./img/similars/out.jpg");
                                BufferedImage img = ImageIO.read(new File("./img/similars/out.jpg"));
                                jLabel2.setText(name);
                                ((DrawingPanel)imagePanel2).image = img;
                            } catch (SQLException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }                    
                    break;
                case 2:
                    {
                        String name = (String)similarImages[i][0];
                        OrdImage getImg = (OrdImage)similarImages[i][1];
                        if(getImg != null){
                            try {
                                getImg.getDataInFile("./img/similars/out.jpg");
                                BufferedImage img = ImageIO.read(new File("./img/similars/out.jpg"));
                                jLabel4.setText(name);
                                ((DrawingPanel)imagePanel3).image = img;
                            } catch (SQLException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } 
                    break;
                default:
                    {
                        String name = (String)similarImages[i][0];
                        OrdImage getImg = (OrdImage)similarImages[i][1];
                        if(getImg != null){
                            try {
                                getImg.getDataInFile("./img/similars/out.jpg");
                                BufferedImage img = ImageIO.read(new File("./img/similars/out.jpg"));
                                jLabel5.setText(name);
                                ((DrawingPanel)imagePanel4).image = img;
                            } catch (SQLException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(SimilarImagesDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }                     
                    break;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        imagePanel4 = new DrawingPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        imagePanel2 = new DrawingPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        imagePanel1 = new DrawingPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        imagePanel3 = new DrawingPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Podobné obrázky");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel3.setText("Podobné obrázky");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel1.setText("1. podobnost");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("2. podobnost");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        getContentPane().add(jLabel2, gridBagConstraints);

        imagePanel4.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout imagePanel4Layout = new javax.swing.GroupLayout(imagePanel4);
        imagePanel4.setLayout(imagePanel4Layout);
        imagePanel4Layout.setHorizontalGroup(
            imagePanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
        );
        imagePanel4Layout.setVerticalGroup(
            imagePanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );

        jScrollPane3.setViewportView(imagePanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        getContentPane().add(jScrollPane3, gridBagConstraints);

        imagePanel2.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout imagePanel2Layout = new javax.swing.GroupLayout(imagePanel2);
        imagePanel2.setLayout(imagePanel2Layout);
        imagePanel2Layout.setHorizontalGroup(
            imagePanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
        );
        imagePanel2Layout.setVerticalGroup(
            imagePanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );

        jScrollPane4.setViewportView(imagePanel2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        getContentPane().add(jScrollPane4, gridBagConstraints);

        jLabel4.setText("3. podobnost");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        getContentPane().add(jLabel4, gridBagConstraints);

        jLabel5.setText("4. podobnost");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        getContentPane().add(jLabel5, gridBagConstraints);

        imagePanel1.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout imagePanel1Layout = new javax.swing.GroupLayout(imagePanel1);
        imagePanel1.setLayout(imagePanel1Layout);
        imagePanel1Layout.setHorizontalGroup(
            imagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
        );
        imagePanel1Layout.setVerticalGroup(
            imagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );

        jScrollPane5.setViewportView(imagePanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        getContentPane().add(jScrollPane5, gridBagConstraints);

        imagePanel3.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout imagePanel3Layout = new javax.swing.GroupLayout(imagePanel3);
        imagePanel3.setLayout(imagePanel3Layout);
        imagePanel3Layout.setHorizontalGroup(
            imagePanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
        );
        imagePanel3Layout.setVerticalGroup(
            imagePanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );

        jScrollPane6.setViewportView(imagePanel3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(jScrollPane6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SimilarImagesDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SimilarImagesDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SimilarImagesDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SimilarImagesDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                SimilarImagesDialog dialog = new SimilarImagesDialog(new javax.swing.JFrame(), true, new Object[4][2]);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel imagePanel1;
    private javax.swing.JPanel imagePanel2;
    private javax.swing.JPanel imagePanel3;
    private javax.swing.JPanel imagePanel4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    // End of variables declaration//GEN-END:variables
}
