/*
 * frmBase.java
 *
 * Created on April 6th, 2009, 9:56 AM
 */

/*
Copyright (c) 2008, George Blouin Jr. (skyhigh@solaris7.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of
conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list
of conditions and the following disclaimer in the documentation and/or other materials
provided with the distribution.
    * Neither the name of George Blouin Jr nor the names of contributors may be
used to endorse or promote products derived from this software without specific prior
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package BFB.GUI;

import BFB.*;
import BFB.Common.CommonTools;
import BFB.Common.Constants;
import BFB.IO.*;
import BFB.Preview.dlgPreview;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.Vector;
import java.util.logging.*;
import java.util.prefs.*;
import javax.swing.ImageIcon;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import ssw.components.Mech;

public class frmBase extends javax.swing.JFrame implements java.awt.datatransfer.ClipboardOwner {
    public Force topForce = new Force();
    public Force bottomForce = new Force();
    public Preferences Prefs;
    private dlgOpen dOpen;

    private TableModelListener ForceChanged = new TableModelListener() {
        public void tableChanged(TableModelEvent e) {
            Refresh();
        }
    };

    public frmBase() {
        initComponents();
        Prefs = Preferences.userNodeForPackage(this.getClass());
        
        //loadScenario( Prefs.get("CurrentBFBFile", "") );
        
        //Clear tracking data
        Prefs.put("CurrentBFBFile", "");

        dOpen = new dlgOpen(this, true);
        dOpen.setMechListPath(Prefs.get("ListPath", ""));
        dOpen.LoadList();
        
        topForce.addTableModelListener(ForceChanged);
        bottomForce.addTableModelListener(ForceChanged);
        
        Refresh();
    }

    public void Refresh() {
        //javax.swing.JOptionPane.showMessageDialog(this, "Refresh Fired");
        
        topForce.setupTable(tblTop);
        bottomForce.setupTable(tblBottom);

        topForce.OpForSize = bottomForce.Units.size();
        bottomForce.OpForSize = topForce.Units.size();
        
        setLogo( lblUnitLogoTop, new File(topForce.LogoPath) );
        setLogo( lblUnitLogoBottom, new File(bottomForce.LogoPath) );

        txtUnitNameTop.setText(topForce.ForceName);
        txtUnitNameBottom.setText(bottomForce.ForceName);

        lblForceMod.setText( String.format( "%1$,.2f", CommonTools.GetForceSizeMultiplier( topForce.Units.size(), bottomForce.Units.size() )) );

        if ( chkUseForceModifier.isSelected() ) {
            topForce.OpForSize = bottomForce.Units.size();
            bottomForce.OpForSize = topForce.Units.size();
        }
        
        lblUnitsTop.setText(topForce.Units.size()+"");
        lblTonnageTop.setText( String.format("%1$,.0f", topForce.TotalTonnage) );
        lblBaseBVTop.setText( String.format("%1$,.0f", topForce.TotalBaseBV) );
        lblTotalBVTop.setText( String.format("%1$,.0f", topForce.TotalForceBVAdjusted) );

        lblUnitsBottom.setText(bottomForce.Units.size()+"");
        lblTonnageBottom.setText( String.format("%1$,.0f", bottomForce.TotalTonnage) );
        lblBaseBVBottom.setText( String.format("%1$,.0f", bottomForce.TotalBaseBV) );
        lblTotalBVBottom.setText( String.format("%1$,.0f", bottomForce.TotalForceBVAdjusted) );
    }

    private void loadScenario( String filename ) {
        if ( filename.isEmpty() ) { return; }
        
        XMLReader reader = new XMLReader();
        Force[] forces;
        try {
            forces = reader.ReadFile(this, filename);
            topForce = forces[0];
            bottomForce = forces[1];

            topForce.addTableModelListener(ForceChanged);
            bottomForce.addTableModelListener(ForceChanged);

            topForce.RefreshBV();
            bottomForce.RefreshBV();

            Refresh();
            
        } catch ( Exception e ) {
            javax.swing.JOptionPane.showMessageDialog( this, "Issue loading file:\n " + e.getMessage() );
            return;
        }
    }

    private void updateLogo( javax.swing.JLabel lblLogo, Force force ) {
        FileSelector fs = new FileSelector();
        File Logo = fs.SelectImage(Prefs.get("LastOpenLogo", ""), "Select Logo");
        try {
            force.LogoPath = Logo.getCanonicalPath();
            setLogo(lblLogo, Logo);
        } catch (IOException ex) {
            //do nothing
        }
    }

    private void setLogo( javax.swing.JLabel lblLogo, File Logo ) {
        if ( Logo != null && ! Logo.getPath().isEmpty() ) {
            try {
               Prefs.put("LastOpenLogo", Logo.getPath().toString());
               ImageIcon icon = new ImageIcon(Logo.getPath());

                if( icon == null ) { return; }

                // See if we need to scale
                int h = icon.getIconHeight();
                int w = icon.getIconWidth();
                if ( w > lblLogo.getWidth() || h > lblLogo.getHeight() ) {
                    if ( h > lblLogo.getHeight() ) {
                        icon = new ImageIcon(icon.getImage().
                            getScaledInstance(-1, lblLogo.getHeight(), Image.SCALE_DEFAULT));
                    }
                    if ( w > lblLogo.getWidth() ) {
                        icon = new ImageIcon(icon.getImage().
                            getScaledInstance(lblLogo.getWidth(), -1, Image.SCALE_DEFAULT));
                    }
                }

                lblLogo.setIcon(icon);
            } catch ( Exception e ) {

            }
        }
    }

    private void editUnit( javax.swing.JTable Table, Force force ) {
        Unit u = (Unit) force.Units.get(Table.convertRowIndexToModel(Table.getSelectedRow()));
        dlgUnit dUnit = new dlgUnit(this, true, u);
        dUnit.setLocationRelativeTo(this);
        dUnit.setVisible(true);
        force.RefreshBV();
    }

    private void removeUnits( javax.swing.JTable Table, Force force ) {
         int[] rows = Table.getSelectedRows();
         Unit[] units = new Unit[rows.length];
         for (int i=0; i < rows.length; i++ ) {
             Unit u = (Unit) force.Units.get(Table.convertRowIndexToModel(rows[i]));
             units[i] = u;
         }
         for (int j=0; j < units.length; j++) {
             force.RemoveUnit(units[j]);
         }
    }

    private void validateChanges() {
        if ((topForce.isDirty) || (bottomForce.isDirty)) {
                    switch (javax.swing.JOptionPane.showConfirmDialog(this, "Would you like to save your changes?")) {
                        case javax.swing.JOptionPane.YES_OPTION:
                            this.mnuSaveActionPerformed(null);
                        case javax.swing.JOptionPane.CANCEL_OPTION:
                            return;
                    }
        }
    }

    private void OpenDialog( Force force ) {
        dOpen.setForce(force);
        dOpen.setLocationRelativeTo(this);
        dOpen.setVisible(true);
    }

    public void setScenario( String scenario ) {
        txtScenarioName.setText(scenario);
    }

    public void openForce( Force force ) {
        FileSelector openFile = new FileSelector();
        File forceFile = openFile.SelectFile(Prefs.get("LastOpenUnit", ""), "force", "Load Force");

        if (forceFile != null) {
            XMLReader reader = new XMLReader();
            try {
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                reader.ReadUnit( force, forceFile.getCanonicalPath() );
                force.RefreshBV();
                Refresh();

               Prefs.put("LastOpenUnit", forceFile.getCanonicalPath());
               this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            } catch (Exception e) {
               javax.swing.JOptionPane.showMessageDialog( this, "Issue loading file:\n " + e.getMessage() );
               return;
            }
        }
    }

    public void saveForce( Force force ) {
        if ( ! force.isSaveable() ) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a unit name and at least one unit before saving.");
            return;
        }
        FileSelector fs = new FileSelector();
        String dirPath = fs.GetDirectorySelection(Prefs.get("LastOpenUnit", ""));
        if ( dirPath.isEmpty() ) { return;}

        XMLWriter write = new XMLWriter();
        try {
            String filename = dirPath + File.separator + CommonTools.FormatFileName(force.ForceName) + ".force";
            write.SerializeForce(force, filename);
            javax.swing.JOptionPane.showMessageDialog( this, "Force written to " + filename );
        } catch (IOException ex) {
            Logger.getLogger(frmBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void toClipboard( Force[] forces ) {
        String data = "";

        for (Force force : forces ) {
            data += force.SerializeClipboard() + Constants.NL + Constants.NL;
        }

        java.awt.datatransfer.StringSelection export = new java.awt.datatransfer.StringSelection( data );
        java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents( export, this );
    }

    private void overrideSkill( Force force, int Gunnery, int Piloting ) {
        for ( int i=0; i < force.Units.size(); i++ ) {
            Unit u = (Unit) force.Units.get(i);
            u.Gunnery = Gunnery;
            u.Piloting = Piloting;
            u.Refresh();
        }
        force.RefreshBV();
    }














    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        btnNew = new javax.swing.JButton();
        btnLoad = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnPrint = new javax.swing.JButton();
        btnPrintUnits = new javax.swing.JButton();
        btnPreview = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        btnMULExport = new javax.swing.JButton();
        btnClipboard = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        lblScenarioName = new javax.swing.JLabel();
        txtScenarioName = new javax.swing.JTextField();
        chkUseForceModifier = new javax.swing.JCheckBox();
        lblForceMod = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        pnlBottom = new javax.swing.JPanel();
        spnBottom = new javax.swing.JScrollPane();
        tblBottom = new javax.swing.JTable();
        lblUnitNameBottom = new javax.swing.JLabel();
        txtUnitNameBottom = new javax.swing.JTextField();
        lblUnitLogoBottom = new javax.swing.JLabel();
        lblTotalBVBottom = new javax.swing.JLabel();
        lblUnitsBottom = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblTonnageBottom = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblBaseBVBottom = new javax.swing.JLabel();
        tlbBottom = new javax.swing.JToolBar();
        btnAddBottom1 = new javax.swing.JButton();
        btnEditBottom1 = new javax.swing.JButton();
        btnDeleteBottom1 = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        btnOpenBottom = new javax.swing.JButton();
        btnSaveBottom = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        btnClipboardBottom = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        txtBottomGun = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        txtBottomPilot = new javax.swing.JTextField();
        pnlTop = new javax.swing.JPanel();
        spnTop = new javax.swing.JScrollPane();
        tblTop = new javax.swing.JTable();
        lblUnitNameTop = new javax.swing.JLabel();
        txtUnitNameTop = new javax.swing.JTextField();
        lblUnitLogoTop = new javax.swing.JLabel();
        lblTotalBVTop = new javax.swing.JLabel();
        lblUnitsTop = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lblTonnageTop = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        lblBaseBVTop = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        tlbTop = new javax.swing.JToolBar();
        btnAddTop1 = new javax.swing.JButton();
        btnEditTop1 = new javax.swing.JButton();
        btnDeleteTop1 = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnOpenTop = new javax.swing.JButton();
        btnSaveTop = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        btnClipboardTop = new javax.swing.JButton();
        txtTopGun = new javax.swing.JTextField();
        txtTopPilot = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mnuNew = new javax.swing.JMenuItem();
        mnuLoad = new javax.swing.JMenuItem();
        mnuSave = new javax.swing.JMenuItem();
        mnuSaveAs = new javax.swing.JMenuItem();
        mnuExport = new javax.swing.JMenu();
        mnuExportMUL = new javax.swing.JMenuItem();
        mnuExportText = new javax.swing.JMenuItem();
        mnuExportClipboard = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        mnuPrintAll = new javax.swing.JMenuItem();
        mnuPrintForce = new javax.swing.JMenuItem();
        mnuPrintUnits = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        mnuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        mnuDesignBattleMech = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        mnuAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Battletech Force Balancer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/document--plus.png"))); // NOI18N
        btnNew.setToolTipText("New Scenario");
        btnNew.setFocusable(false);
        btnNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNew);

        btnLoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/folder-open-document.png"))); // NOI18N
        btnLoad.setToolTipText("Open Scenario");
        btnLoad.setFocusable(false);
        btnLoad.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLoad.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });
        jToolBar1.add(btnLoad);

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/disk-black.png"))); // NOI18N
        btnSave.setToolTipText("Save Scenario");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSave);
        jToolBar1.add(jSeparator1);

        btnPrint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/printer.png"))); // NOI18N
        btnPrint.setToolTipText("Print Sheet and Designs");
        btnPrint.setFocusable(false);
        btnPrint.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPrint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPrint);

        btnPrintUnits.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/printer--plus.png"))); // NOI18N
        btnPrintUnits.setToolTipText("Print Units");
        btnPrintUnits.setFocusable(false);
        btnPrintUnits.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPrintUnits.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPrintUnits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintUnitsActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPrintUnits);

        btnPreview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/projection-screen.png"))); // NOI18N
        btnPreview.setFocusable(false);
        btnPreview.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPreview.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviewActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPreview);
        jToolBar1.add(jSeparator4);

        btnMULExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/map--arrow.png"))); // NOI18N
        btnMULExport.setToolTipText("Export Forces to MUL");
        btnMULExport.setFocusable(false);
        btnMULExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnMULExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnMULExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMULExportActionPerformed(evt);
            }
        });
        jToolBar1.add(btnMULExport);

        btnClipboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/clipboard.png"))); // NOI18N
        btnClipboard.setToolTipText("Export Scenario to Clipboard");
        btnClipboard.setFocusable(false);
        btnClipboard.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClipboard.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClipboardActionPerformed(evt);
            }
        });
        jToolBar1.add(btnClipboard);

        jButton1.setText("Output All Info");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        lblScenarioName.setText("Scenario / Event Name: ");

        txtScenarioName.setToolTipText("Enter the name of the scenario or event");

        chkUseForceModifier.setSelected(true);
        chkUseForceModifier.setText("Use Force Size Modifier");
        chkUseForceModifier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkUseForceModifierActionPerformed(evt);
            }
        });

        lblForceMod.setText("0.00");

        pnlBottom.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Secondary Force Listing", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Trebuchet MS", 1, 12), new java.awt.Color(0, 51, 204))); // NOI18N

        tblBottom.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblBottom.setRowMargin(2);
        tblBottom.setShowVerticalLines(false);
        tblBottom.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblBottomMouseClicked(evt);
            }
        });
        spnBottom.setViewportView(tblBottom);

        lblUnitNameBottom.setText("Unit Name:");

        txtUnitNameBottom.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtUnitNameBottomFocusLost(evt);
            }
        });
        txtUnitNameBottom.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtUnitNameBottomKeyTyped(evt);
            }
        });

        lblUnitLogoBottom.setToolTipText("Unit or Faction Logo, Double Click to change");
        lblUnitLogoBottom.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lblUnitLogoBottom.setOpaque(true);
        lblUnitLogoBottom.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblUnitLogoBottomMouseClicked(evt);
            }
        });

        lblTotalBVBottom.setFont(new java.awt.Font("Verdana", 1, 12));
        lblTotalBVBottom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblTotalBVBottom.setText("0,000 BV");

        lblUnitsBottom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblUnitsBottom.setText("0");

        jLabel3.setText("Units");

        lblTonnageBottom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblTonnageBottom.setText("0");

        jLabel5.setText("Tons");

        jLabel7.setText("BV");

        lblBaseBVBottom.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBaseBVBottom.setText("0,000");

        tlbBottom.setFloatable(false);
        tlbBottom.setRollover(true);

        btnAddBottom1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--plus.png"))); // NOI18N
        btnAddBottom1.setToolTipText("Add Unit");
        btnAddBottom1.setFocusable(false);
        btnAddBottom1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddBottom1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddBottom1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddBottom1ActionPerformed(evt);
            }
        });
        tlbBottom.add(btnAddBottom1);

        btnEditBottom1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--pencil.png"))); // NOI18N
        btnEditBottom1.setToolTipText("Edit Unit");
        btnEditBottom1.setFocusable(false);
        btnEditBottom1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnEditBottom1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnEditBottom1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditBottom1ActionPerformed(evt);
            }
        });
        tlbBottom.add(btnEditBottom1);

        btnDeleteBottom1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--minus.png"))); // NOI18N
        btnDeleteBottom1.setToolTipText("Delete Unit");
        btnDeleteBottom1.setFocusable(false);
        btnDeleteBottom1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteBottom1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDeleteBottom1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteBottom1ActionPerformed(evt);
            }
        });
        tlbBottom.add(btnDeleteBottom1);
        tlbBottom.add(jSeparator5);

        btnOpenBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/folder-open-document.png"))); // NOI18N
        btnOpenBottom.setToolTipText("Open Force");
        btnOpenBottom.setFocusable(false);
        btnOpenBottom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenBottom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenBottomActionPerformed(evt);
            }
        });
        tlbBottom.add(btnOpenBottom);

        btnSaveBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/disk.png"))); // NOI18N
        btnSaveBottom.setToolTipText("Save Force");
        btnSaveBottom.setFocusable(false);
        btnSaveBottom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSaveBottom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSaveBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveBottomActionPerformed(evt);
            }
        });
        tlbBottom.add(btnSaveBottom);
        tlbBottom.add(jSeparator8);

        btnClipboardBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/clipboard.png"))); // NOI18N
        btnClipboardBottom.setToolTipText("Export Force to Clipboard");
        btnClipboardBottom.setFocusable(false);
        btnClipboardBottom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClipboardBottom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClipboardBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClipboardBottomActionPerformed(evt);
            }
        });
        tlbBottom.add(btnClipboardBottom);

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Override  G");

        txtBottomGun.setText("4");
        txtBottomGun.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtBottomGunFocusGained(evt);
            }
        });
        txtBottomGun.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBottomGunKeyReleased(evt);
            }
        });

        jLabel11.setText("P");

        txtBottomPilot.setText("5");
        txtBottomPilot.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtBottomPilotFocusGained(evt);
            }
        });
        txtBottomPilot.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBottomPilotKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout pnlBottomLayout = new javax.swing.GroupLayout(pnlBottom);
        pnlBottom.setLayout(pnlBottomLayout);
        pnlBottomLayout.setHorizontalGroup(
            pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBottomLayout.createSequentialGroup()
                .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlBottomLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblUnitLogoBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(7, 7, 7)
                        .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlBottomLayout.createSequentialGroup()
                                .addComponent(lblUnitNameBottom)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtUnitNameBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(78, 78, 78)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtBottomGun, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8)
                                .addComponent(jLabel11)
                                .addGap(3, 3, 3)
                                .addComponent(txtBottomPilot, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 269, Short.MAX_VALUE)
                                .addComponent(tlbBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(spnBottom, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBottomLayout.createSequentialGroup()
                        .addGap(109, 109, 109)
                        .addComponent(lblUnitsBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addGap(313, 313, 313)
                        .addComponent(lblTonnageBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(lblBaseBVBottom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 344, Short.MAX_VALUE)
                        .addComponent(lblTotalBVBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        pnlBottomLayout.setVerticalGroup(
            pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBottomLayout.createSequentialGroup()
                .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblUnitLogoBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlBottomLayout.createSequentialGroup()
                        .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnitNameBottom)
                                .addComponent(txtUnitNameBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtBottomGun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtBottomPilot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel10)
                                .addComponent(jLabel11))
                            .addComponent(tlbBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spnBottom, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUnitsBottom)
                    .addComponent(jLabel3)
                    .addComponent(lblTonnageBottom)
                    .addComponent(jLabel5)
                    .addComponent(lblBaseBVBottom)
                    .addComponent(jLabel7)
                    .addComponent(lblTotalBVBottom)))
        );

        pnlTop.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Primary Force Listing", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Trebuchet MS", 1, 12), new java.awt.Color(0, 51, 204))); // NOI18N

        tblTop.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblTop.setRowMargin(2);
        tblTop.setShowVerticalLines(false);
        tblTop.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblTopMouseClicked(evt);
            }
        });
        spnTop.setViewportView(tblTop);

        lblUnitNameTop.setText("Unit Name:");

        txtUnitNameTop.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtUnitNameTopFocusLost(evt);
            }
        });
        txtUnitNameTop.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtUnitNameTopKeyTyped(evt);
            }
        });

        lblUnitLogoTop.setToolTipText("Unit or Faction Logo, Double Click to change");
        lblUnitLogoTop.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lblUnitLogoTop.setOpaque(true);
        lblUnitLogoTop.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblUnitLogoTopMouseClicked(evt);
            }
        });

        lblTotalBVTop.setFont(new java.awt.Font("Verdana", 1, 12));
        lblTotalBVTop.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblTotalBVTop.setText("0,000 BV");

        lblUnitsTop.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblUnitsTop.setText("0");

        jLabel2.setText("Units");

        lblTonnageTop.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblTonnageTop.setText("0");

        jLabel4.setText("Tons");

        lblBaseBVTop.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblBaseBVTop.setText("0,000");

        jLabel6.setText("BV");

        tlbTop.setFloatable(false);
        tlbTop.setRollover(true);

        btnAddTop1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--plus.png"))); // NOI18N
        btnAddTop1.setToolTipText("Add Unit");
        btnAddTop1.setFocusable(false);
        btnAddTop1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddTop1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddTop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddTop1ActionPerformed(evt);
            }
        });
        tlbTop.add(btnAddTop1);

        btnEditTop1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--pencil.png"))); // NOI18N
        btnEditTop1.setToolTipText("Edit Unit");
        btnEditTop1.setFocusable(false);
        btnEditTop1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnEditTop1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnEditTop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditTop1ActionPerformed(evt);
            }
        });
        tlbTop.add(btnEditTop1);

        btnDeleteTop1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/shield--minus.png"))); // NOI18N
        btnDeleteTop1.setToolTipText("Delete Unit");
        btnDeleteTop1.setFocusable(false);
        btnDeleteTop1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteTop1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDeleteTop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteTop1ActionPerformed(evt);
            }
        });
        tlbTop.add(btnDeleteTop1);
        tlbTop.add(jSeparator6);

        btnOpenTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/folder-open-document.png"))); // NOI18N
        btnOpenTop.setToolTipText("Open Force");
        btnOpenTop.setFocusable(false);
        btnOpenTop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenTop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenTopActionPerformed(evt);
            }
        });
        tlbTop.add(btnOpenTop);

        btnSaveTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/disk.png"))); // NOI18N
        btnSaveTop.setToolTipText("Save Force");
        btnSaveTop.setFocusable(false);
        btnSaveTop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSaveTop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSaveTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveTopActionPerformed(evt);
            }
        });
        tlbTop.add(btnSaveTop);
        tlbTop.add(jSeparator7);

        btnClipboardTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/BFB/Images/clipboard.png"))); // NOI18N
        btnClipboardTop.setToolTipText("Export Force to Clipboard");
        btnClipboardTop.setFocusable(false);
        btnClipboardTop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClipboardTop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClipboardTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClipboardTopActionPerformed(evt);
            }
        });
        tlbTop.add(btnClipboardTop);

        txtTopGun.setText("4");
        txtTopGun.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtTopGunFocusGained(evt);
            }
        });
        txtTopGun.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtTopGunKeyReleased(evt);
            }
        });

        txtTopPilot.setText("5");
        txtTopPilot.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtTopPilotFocusGained(evt);
            }
        });
        txtTopPilot.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtTopPilotKeyReleased(evt);
            }
        });

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Override  G");

        jLabel9.setText("P");

        javax.swing.GroupLayout pnlTopLayout = new javax.swing.GroupLayout(pnlTop);
        pnlTop.setLayout(pnlTopLayout);
        pnlTopLayout.setHorizontalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTopLayout.createSequentialGroup()
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlTopLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblUnitLogoTop, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlTopLayout.createSequentialGroup()
                                .addComponent(lblUnitNameTop)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtUnitNameTop, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(78, 78, 78)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtTopGun, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8)
                                .addComponent(jLabel9)
                                .addGap(3, 3, 3)
                                .addComponent(txtTopPilot, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 270, Short.MAX_VALUE)
                                .addComponent(tlbTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(spnTop, javax.swing.GroupLayout.DEFAULT_SIZE, 922, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlTopLayout.createSequentialGroup()
                        .addGap(109, 109, 109)
                        .addComponent(lblUnitsTop, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addGap(307, 307, 307)
                        .addComponent(lblTonnageTop, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(lblBaseBVTop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 349, Short.MAX_VALUE)
                        .addComponent(lblTotalBVTop, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        pnlTopLayout.setVerticalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTopLayout.createSequentialGroup()
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblUnitLogoTop, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlTopLayout.createSequentialGroup()
                        .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnitNameTop)
                                .addComponent(txtUnitNameTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtTopGun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtTopPilot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel8)
                                .addComponent(jLabel9))
                            .addComponent(tlbTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spnTop, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUnitsTop)
                    .addComponent(jLabel2)
                    .addComponent(lblTonnageTop)
                    .addComponent(jLabel4)
                    .addComponent(lblBaseBVTop)
                    .addComponent(jLabel6)
                    .addComponent(lblTotalBVTop)))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlBottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlBottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Force Selections", jPanel1);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setTabSize(4);
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel1.setText("Scenario Notes:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1047, Short.MAX_VALUE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 521, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Scenario Information", jPanel2);

        jMenu1.setText("File");

        mnuNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mnuNew.setText("New");
        mnuNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuNewActionPerformed(evt);
            }
        });
        jMenu1.add(mnuNew);

        mnuLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        mnuLoad.setText("Load");
        mnuLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuLoadActionPerformed(evt);
            }
        });
        jMenu1.add(mnuLoad);

        mnuSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuSave.setText("Save");
        mnuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSaveActionPerformed(evt);
            }
        });
        jMenu1.add(mnuSave);

        mnuSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        mnuSaveAs.setText("Save As...");
        mnuSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSaveAsActionPerformed(evt);
            }
        });
        jMenu1.add(mnuSaveAs);

        mnuExport.setText("Export To...");

        mnuExportMUL.setText("MUL");
        mnuExportMUL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExportMULActionPerformed(evt);
            }
        });
        mnuExport.add(mnuExportMUL);

        mnuExportText.setText("Text");
        mnuExportText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExportTextActionPerformed(evt);
            }
        });
        mnuExport.add(mnuExportText);

        mnuExportClipboard.setText("Clipboard");
        mnuExportClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExportClipboardActionPerformed(evt);
            }
        });
        mnuExport.add(mnuExportClipboard);

        jMenu1.add(mnuExport);
        jMenu1.add(jSeparator2);

        mnuPrintAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        mnuPrintAll.setText("Print Sheet & Units");
        mnuPrintAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuPrintAllActionPerformed(evt);
            }
        });
        jMenu1.add(mnuPrintAll);

        mnuPrintForce.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        mnuPrintForce.setText("Print Sheet");
        mnuPrintForce.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuPrintForceActionPerformed(evt);
            }
        });
        jMenu1.add(mnuPrintForce);

        mnuPrintUnits.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        mnuPrintUnits.setText("Print Units");
        mnuPrintUnits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuPrintUnitsActionPerformed(evt);
            }
        });
        jMenu1.add(mnuPrintUnits);
        jMenu1.add(jSeparator3);

        mnuExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuExit.setText("Exit");
        mnuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExitActionPerformed(evt);
            }
        });
        jMenu1.add(mnuExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Design");

        mnuDesignBattleMech.setText("BattleMech");
        mnuDesignBattleMech.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuDesignBattleMechActionPerformed(evt);
            }
        });
        jMenu2.add(mnuDesignBattleMech);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("About");

        mnuAbout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        mnuAbout.setText("Battletech Force Balancer");
        mnuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuAboutActionPerformed(evt);
            }
        });
        jMenu3.add(mnuAbout);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1092, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblScenarioName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtScenarioName, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkUseForceModifier)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblForceMod)
                .addContainerGap(493, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblScenarioName)
                    .addComponent(txtScenarioName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkUseForceModifier)
                    .addComponent(lblForceMod))
                .addGap(6, 6, 6)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtUnitNameTopKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtUnitNameTopKeyTyped
    }//GEN-LAST:event_txtUnitNameTopKeyTyped

    private void txtUnitNameBottomKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtUnitNameBottomKeyTyped
    }//GEN-LAST:event_txtUnitNameBottomKeyTyped

    private void mnuLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuLoadActionPerformed
        if ((topForce.isDirty) || (bottomForce.isDirty)) {
                    switch (javax.swing.JOptionPane.showConfirmDialog(this, "Would you like to save your changes?")) {
                        case javax.swing.JOptionPane.YES_OPTION:
                            this.mnuSaveActionPerformed(null);
                        case javax.swing.JOptionPane.CANCEL_OPTION:
                            return;
                    }
        }

        FileSelector openFile = new FileSelector();
        File forceFile = openFile.SelectFile(Prefs.get("LastOpenBFBDirectory", ""), "bfb", "Load Force List");

        if (forceFile != null) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            try {
               loadScenario(forceFile.getCanonicalPath());

               Prefs.put("LastOpenBFBDirectory", forceFile.getCanonicalPath().replace(forceFile.getName(), ""));
               Prefs.put("LastOpenBFBFile", forceFile.getName());
               Prefs.put("CurrentBFBFile", forceFile.getPath());
            } catch (Exception e) {
               javax.swing.JOptionPane.showMessageDialog( this, "Issue loading file:\n " + e.getMessage() );
               return;
            } finally {
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
}//GEN-LAST:event_mnuLoadActionPerformed

    private void lblUnitLogoBottomMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblUnitLogoBottomMouseClicked
        if ( evt.getClickCount() == 2 ) { updateLogo(lblUnitLogoBottom, bottomForce); }
    }//GEN-LAST:event_lblUnitLogoBottomMouseClicked

    private void lblUnitLogoTopMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblUnitLogoTopMouseClicked
        if ( evt.getClickCount() == 2 ) { updateLogo(lblUnitLogoTop, topForce); }
    }//GEN-LAST:event_lblUnitLogoTopMouseClicked

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if ((topForce.isDirty) || (bottomForce.isDirty)) {
            switch (javax.swing.JOptionPane.showConfirmDialog(this, "Would you like to save your changes?")) {
                case javax.swing.JOptionPane.YES_OPTION:
                    this.mnuSaveActionPerformed(null);
                case javax.swing.JOptionPane.NO_OPTION:
                    dOpen.dispose();
                    this.dispose();
                case javax.swing.JOptionPane.CANCEL_OPTION:
                    return;
            }
        } else {
            dOpen.dispose();
            this.dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    private void txtUnitNameTopFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtUnitNameTopFocusLost
        topForce.ForceName = txtUnitNameTop.getText();
    }//GEN-LAST:event_txtUnitNameTopFocusLost

    private void txtUnitNameBottomFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtUnitNameBottomFocusLost
        bottomForce.ForceName = txtUnitNameBottom.getText();
    }//GEN-LAST:event_txtUnitNameBottomFocusLost

    private void mnuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExitActionPerformed
        formWindowClosing(null);
    }//GEN-LAST:event_mnuExitActionPerformed

    private void mnuNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuNewActionPerformed
        if ((topForce.isDirty) || (bottomForce.isDirty)) {
                    switch (javax.swing.JOptionPane.showConfirmDialog(this, "Would you like to save your changes?")) {
                        case javax.swing.JOptionPane.YES_OPTION:
                            this.mnuSaveActionPerformed(null);
                        case javax.swing.JOptionPane.CANCEL_OPTION:
                            return;
                    }
        }

        Prefs.put("CurrentBFBFile", "");
        this.topForce.Clear();
        this.bottomForce.Clear();
        this.txtScenarioName.setText("");
        this.Refresh();
    }//GEN-LAST:event_mnuNewActionPerformed

    private void mnuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSaveActionPerformed
        if ( txtScenarioName.getText().isEmpty() ) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a scenario name before saving.");
            return;
        }
        
        if ( !topForce.isSaveable() || !bottomForce.isSaveable() ) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a force name and at least one unit in each list before saving.");
            return;
        }
        
        try {
            File file;
            if ( !Prefs.get("CurrentBFBFile", "").isEmpty() ) {
                file = new File(Prefs.get("CurrentBFBFile", ""));
            } else {
                FileSelector selector = new FileSelector();
                file = selector.SelectFile(txtScenarioName.getText() + ".bfb", "bfb", "Save");
                if (file == null) {
                    return;
                }
            }
            String filename = file.getCanonicalPath();
            if ( ! filename.endsWith(".bfb") ) { filename += ".bfb";}

            XMLWriter write = new XMLWriter(txtScenarioName.getText(), this.topForce, this.bottomForce);
            write.WriteXML(filename);
            Prefs.put("LastOpenBFBFile", filename);
            Prefs.put("CurrentBFBFile", filename);
            javax.swing.JOptionPane.showMessageDialog(this, "Forces saved to " + filename);
        } catch (java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_mnuSaveActionPerformed

    private void mnuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuAboutActionPerformed
        dlgAbout About = new dlgAbout();
        About.setTitle("About Battletech Force Balancer");
        About.setLocationRelativeTo(this);
        About.setVisible(true);
}//GEN-LAST:event_mnuAboutActionPerformed

    private void mnuPrintForceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuPrintForceActionPerformed
       Printer printer = new Printer(this);
       printer.setJobName(this.txtScenarioName.getText());
       printer.setTitle(this.txtScenarioName.getText());
       printer.Print();
}//GEN-LAST:event_mnuPrintForceActionPerformed

    private void mnuPrintUnitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuPrintUnitsActionPerformed
        Vector forces = new Vector();
        forces.add(topForce);
        forces.add(bottomForce);

        for (int f = 0; f <= forces.size()-1; f++){
            ssw.print.Printer printer = new ssw.print.Printer();
            Force printForce = (Force) forces.get(f);

            printer.setLogoPath(printForce.LogoPath);
            printer.setJobName(printForce.ForceName);

            for (int i = 0; i < printForce.Units.size(); ++i) {
                Unit u = (Unit) printForce.Units.get(i);
                u.LoadMech();
                Mech m = u.m;
                if (m != null) {
                    printer.AddMech(m, u.Mechwarrior, u.Gunnery, u.Piloting, true, true, true);
                }
            }

            printer.Print();

        }
    }//GEN-LAST:event_mnuPrintUnitsActionPerformed

    private void mnuDesignBattleMechActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuDesignBattleMechActionPerformed
        ssw.gui.frmMain SSW = new ssw.gui.frmMain();
        SSW.setLocationRelativeTo(null);
        SSW.setVisible(true);
    }//GEN-LAST:event_mnuDesignBattleMechActionPerformed

    private void mnuPrintAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuPrintAllActionPerformed
        mnuPrintForceActionPerformed(evt);
        mnuPrintUnitsActionPerformed(evt);
}//GEN-LAST:event_mnuPrintAllActionPerformed

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        mnuLoadActionPerformed(evt);
}//GEN-LAST:event_btnLoadActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        mnuNewActionPerformed(evt);
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        mnuSaveActionPerformed(evt);
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintActionPerformed
        mnuPrintAllActionPerformed(evt);
}//GEN-LAST:event_btnPrintActionPerformed

    private void tblTopMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblTopMouseClicked
        if ( evt.getClickCount() == 2 ) { editUnit(tblTop, topForce); }
    }//GEN-LAST:event_tblTopMouseClicked

    private void tblBottomMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblBottomMouseClicked
        if ( evt.getClickCount() == 2 ) { editUnit(tblBottom, bottomForce); }
    }//GEN-LAST:event_tblBottomMouseClicked

    private void btnMULExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMULExportActionPerformed
        MULWriter mw = new MULWriter();
        FileSelector fs = new FileSelector();
        String dir = "";
        dir = fs.GetDirectorySelection(Prefs.get("MULDirectory", ""));
        if ( dir.isEmpty() ) { return; }

        Prefs.put("MULDirectory", dir);
        mw.setForce(topForce);
        try {
            mw.Write( dir + topForce.ForceName );
        } catch (IOException ex) {
            //do nothing
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to save " + topForce.ForceName + "\n" + ex.getMessage() );
        }

        mw.setForce(bottomForce);
        try {
            mw.Write( dir + bottomForce.ForceName );
        } catch ( IOException ex ) {
            //do nothing
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to save " + bottomForce.ForceName + "\n" + ex.getMessage() );
        }

        javax.swing.JOptionPane.showMessageDialog(this, "Your forces have been exported to " + dir);
}//GEN-LAST:event_btnMULExportActionPerformed

    private void chkUseForceModifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkUseForceModifierActionPerformed
        lblForceMod.setVisible( chkUseForceModifier.isSelected() );
        topForce.useUnevenForceMod = chkUseForceModifier.isSelected();
        bottomForce.useUnevenForceMod = chkUseForceModifier.isSelected();
        topForce.RefreshBV();
        bottomForce.RefreshBV();
    }//GEN-LAST:event_chkUseForceModifierActionPerformed

    private void btnOpenBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenBottomActionPerformed
        openForce( bottomForce );
    }//GEN-LAST:event_btnOpenBottomActionPerformed

    private void btnSaveBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveBottomActionPerformed
        bottomForce.ForceName = txtUnitNameBottom.getText();
        saveForce( bottomForce );
    }//GEN-LAST:event_btnSaveBottomActionPerformed

    private void btnOpenTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenTopActionPerformed
        openForce( topForce );
}//GEN-LAST:event_btnOpenTopActionPerformed

    private void btnSaveTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveTopActionPerformed
        topForce.ForceName = txtUnitNameTop.getText();
        saveForce( topForce );
}//GEN-LAST:event_btnSaveTopActionPerformed

    private void btnAddBottom1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddBottom1ActionPerformed
        OpenDialog(bottomForce);
    }//GEN-LAST:event_btnAddBottom1ActionPerformed

    private void btnAddTop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddTop1ActionPerformed
        OpenDialog(topForce);
    }//GEN-LAST:event_btnAddTop1ActionPerformed

    private void btnEditTop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditTop1ActionPerformed
        editUnit(tblTop, topForce);
    }//GEN-LAST:event_btnEditTop1ActionPerformed

    private void btnEditBottom1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditBottom1ActionPerformed
        editUnit(tblBottom, bottomForce);
    }//GEN-LAST:event_btnEditBottom1ActionPerformed

    private void btnDeleteTop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteTop1ActionPerformed
        removeUnits( tblTop, topForce );
    }//GEN-LAST:event_btnDeleteTop1ActionPerformed

    private void btnDeleteBottom1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteBottom1ActionPerformed
        removeUnits( tblBottom, bottomForce );
    }//GEN-LAST:event_btnDeleteBottom1ActionPerformed

    private void btnClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClipboardActionPerformed
        toClipboard( new Force[]{ topForce, bottomForce } );
    }//GEN-LAST:event_btnClipboardActionPerformed

    private void btnClipboardTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClipboardTopActionPerformed
        toClipboard( new Force[]{ topForce } );
    }//GEN-LAST:event_btnClipboardTopActionPerformed

    private void btnClipboardBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClipboardBottomActionPerformed
        toClipboard( new Force[]{ bottomForce } );
    }//GEN-LAST:event_btnClipboardBottomActionPerformed

    private void mnuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSaveAsActionPerformed
        if ( txtScenarioName.getText().isEmpty() ) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a scenario name before saving.");
            return;
        }

        if ( !topForce.isSaveable() || !bottomForce.isSaveable() ) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a force name and at least one unit in each list before saving.");
            return;
        }

        try {
            File file;
            FileSelector selector = new FileSelector();
            file = selector.SelectFile("", "bfb", "Save");
            if (file == null) {
                return;
            }

            String filename = file.getCanonicalPath();
            if ( ! filename.endsWith(".bfb") ) { filename += ".bfb";}

            XMLWriter write = new XMLWriter(txtScenarioName.getText(), this.topForce, this.bottomForce);
            write.WriteXML(filename);
            Prefs.put("LastOpenBFBFile", filename);
            Prefs.put("CurrentBFBFile", filename);
            javax.swing.JOptionPane.showMessageDialog(this, "Forces saved to " + filename);
        } catch (java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
        }
}//GEN-LAST:event_mnuSaveAsActionPerformed

    private void mnuExportClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExportClipboardActionPerformed
        toClipboard( new Force[]{ topForce, bottomForce } );
    }//GEN-LAST:event_mnuExportClipboardActionPerformed

    private void mnuExportMULActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExportMULActionPerformed
        btnMULExportActionPerformed(evt);
    }//GEN-LAST:event_mnuExportMULActionPerformed

    private void mnuExportTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExportTextActionPerformed
        TXTWriter txtWrite = new TXTWriter( new Force[]{ topForce, bottomForce } );
        FileSelector fs = new FileSelector();
        File filename = fs.SelectFile(Prefs.get("TXTDirectory", ""), "txt", "Save");
        if ( filename == null ) { return; }

        try {
            txtWrite.Write(filename.getCanonicalPath());

            Prefs.put("TXTDirectory", filename.getCanonicalPath());
            javax.swing.JOptionPane.showMessageDialog(this, "Your forces have been exported to " + filename.getCanonicalPath());
        } catch (IOException ex) {
            //do nothing
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to save \n" + ex.getMessage() );
        }
    }//GEN-LAST:event_mnuExportTextActionPerformed

    private void btnPrintUnitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintUnitsActionPerformed
        mnuPrintUnitsActionPerformed(evt);
}//GEN-LAST:event_btnPrintUnitsActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        String data = "";
        data += topForce.SerializeData();
        data += bottomForce.SerializeData();

        java.awt.datatransfer.StringSelection export = new java.awt.datatransfer.StringSelection( data );
        java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents( export, this );
    }//GEN-LAST:event_jButton1ActionPerformed

    private void txtTopGunKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTopGunKeyReleased
        if ( !txtTopGun.getText().isEmpty() && !txtTopPilot.getText().isEmpty() ) {
            overrideSkill( topForce, Integer.parseInt(txtTopGun.getText()), Integer.parseInt(txtTopPilot.getText()) );
        }
}//GEN-LAST:event_txtTopGunKeyReleased

    private void txtTopGunFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtTopGunFocusGained
        txtTopGun.selectAll();
    }//GEN-LAST:event_txtTopGunFocusGained

    private void txtTopPilotKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTopPilotKeyReleased
        if ( !txtTopGun.getText().isEmpty() && !txtTopPilot.getText().isEmpty() ) {
            overrideSkill( topForce, Integer.parseInt(txtTopGun.getText()), Integer.parseInt(txtTopPilot.getText()) );
        }
    }//GEN-LAST:event_txtTopPilotKeyReleased

    private void txtTopPilotFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtTopPilotFocusGained
        txtTopPilot.selectAll();
    }//GEN-LAST:event_txtTopPilotFocusGained

    private void txtBottomGunFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBottomGunFocusGained
        txtBottomGun.selectAll();
}//GEN-LAST:event_txtBottomGunFocusGained

    private void txtBottomGunKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBottomGunKeyReleased
        if ( !txtBottomGun.getText().isEmpty() && !txtBottomPilot.getText().isEmpty() ) {
            overrideSkill( bottomForce, Integer.parseInt(txtBottomGun.getText()), Integer.parseInt(txtBottomPilot.getText()) );
        }
}//GEN-LAST:event_txtBottomGunKeyReleased

    private void txtBottomPilotFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtBottomPilotFocusGained
        txtBottomPilot.selectAll();
}//GEN-LAST:event_txtBottomPilotFocusGained

    private void txtBottomPilotKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBottomPilotKeyReleased
        if ( !txtBottomGun.getText().isEmpty() && !txtBottomPilot.getText().isEmpty() ) {
            overrideSkill( bottomForce, Integer.parseInt(txtBottomGun.getText()), Integer.parseInt(txtBottomPilot.getText()) );
        }
}//GEN-LAST:event_txtBottomPilotKeyReleased

    private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviewActionPerformed
        topForce.sortForPrinting();
        bottomForce.sortForPrinting();
        Printer printer = new Printer(this);
        printer.setTitle(txtScenarioName.getText());

        dlgPreview preview = new dlgPreview(lblScenarioName.getText(), this, printer.Preview(), 0.0);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        preview.setSize(dim.width, dim.height-30);
        //preview.setSize(1024, 768);
        preview.setLocationRelativeTo(null);
        preview.setVisible(true);
    }//GEN-LAST:event_btnPreviewActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddBottom1;
    private javax.swing.JButton btnAddTop1;
    private javax.swing.JButton btnClipboard;
    private javax.swing.JButton btnClipboardBottom;
    private javax.swing.JButton btnClipboardTop;
    private javax.swing.JButton btnDeleteBottom1;
    private javax.swing.JButton btnDeleteTop1;
    private javax.swing.JButton btnEditBottom1;
    private javax.swing.JButton btnEditTop1;
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnMULExport;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnOpenBottom;
    private javax.swing.JButton btnOpenTop;
    private javax.swing.JButton btnPreview;
    private javax.swing.JButton btnPrint;
    private javax.swing.JButton btnPrintUnits;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSaveBottom;
    private javax.swing.JButton btnSaveTop;
    private javax.swing.JCheckBox chkUseForceModifier;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JLabel lblBaseBVBottom;
    private javax.swing.JLabel lblBaseBVTop;
    private javax.swing.JLabel lblForceMod;
    private javax.swing.JLabel lblScenarioName;
    private javax.swing.JLabel lblTonnageBottom;
    private javax.swing.JLabel lblTonnageTop;
    private javax.swing.JLabel lblTotalBVBottom;
    private javax.swing.JLabel lblTotalBVTop;
    private javax.swing.JLabel lblUnitLogoBottom;
    private javax.swing.JLabel lblUnitLogoTop;
    private javax.swing.JLabel lblUnitNameBottom;
    private javax.swing.JLabel lblUnitNameTop;
    private javax.swing.JLabel lblUnitsBottom;
    private javax.swing.JLabel lblUnitsTop;
    private javax.swing.JMenuItem mnuAbout;
    private javax.swing.JMenuItem mnuDesignBattleMech;
    private javax.swing.JMenuItem mnuExit;
    private javax.swing.JMenu mnuExport;
    private javax.swing.JMenuItem mnuExportClipboard;
    private javax.swing.JMenuItem mnuExportMUL;
    private javax.swing.JMenuItem mnuExportText;
    private javax.swing.JMenuItem mnuLoad;
    private javax.swing.JMenuItem mnuNew;
    private javax.swing.JMenuItem mnuPrintAll;
    private javax.swing.JMenuItem mnuPrintForce;
    private javax.swing.JMenuItem mnuPrintUnits;
    private javax.swing.JMenuItem mnuSave;
    private javax.swing.JMenuItem mnuSaveAs;
    private javax.swing.JPanel pnlBottom;
    private javax.swing.JPanel pnlTop;
    private javax.swing.JScrollPane spnBottom;
    private javax.swing.JScrollPane spnTop;
    private javax.swing.JTable tblBottom;
    private javax.swing.JTable tblTop;
    private javax.swing.JToolBar tlbBottom;
    private javax.swing.JToolBar tlbTop;
    private javax.swing.JTextField txtBottomGun;
    private javax.swing.JTextField txtBottomPilot;
    private javax.swing.JTextField txtScenarioName;
    private javax.swing.JTextField txtTopGun;
    private javax.swing.JTextField txtTopPilot;
    private javax.swing.JTextField txtUnitNameBottom;
    private javax.swing.JTextField txtUnitNameTop;
    // End of variables declaration//GEN-END:variables

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        //do nothing
    }

}