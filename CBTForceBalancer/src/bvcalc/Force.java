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

package bvcalc;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import org.w3c.dom.Node;

/**
 *
 * @author gblouin
 */
public class Force extends AbstractTableModel {
    public Vector Units = new Vector();
    public String ForceName = "";
    public float TotalBaseBV = 0.0f,
                 TotalModifier = 0.0f,
                 TotalTonnage = 0.0f,
                 TotalC3BV = 0.0f,
                 TotalSkillBV = 0.0f,
                 TotalModifierBV = 0.0f,
                 TotalAdjustedBV = 0.0f,
                 TotalForceBV = 0.0f,
                 UnevenForceMod = 0.0f,
                 TotalForceBVAdjusted = 0.0f;
    public int  NumC3 = 0,
                OpForSize = 0;
    public boolean isDirty = false,
                    useUnevenForceMod = false;

    public Force(){
    
    }

    public Force(Node ForceNode) throws Exception {
        this.ForceName = ForceNode.getAttributes().getNamedItem("name").getTextContent();
        for (int i=0; i < ForceNode.getChildNodes().getLength(); i++) {
            Node n = ForceNode.getChildNodes().item(i);
            if (n.getNodeName().equals("unit")) {
                try {
                    Units.add(new Unit(n));
                } catch (Exception e) {
                    throw e;
                }
            }
        }
    }

    public void RefreshBV() {
        Unit u;
        NumC3 = 0;
        TotalBaseBV = 0.0f;
        TotalModifier = 0.0f;
        TotalTonnage = 0.0f;
        TotalC3BV = 0.0f;
        TotalSkillBV = 0.0f;
        TotalModifierBV = 0.0f;
        TotalAdjustedBV = 0.0f;
        TotalForceBV = 0.0f;
        for( int i = 0; i < Units.size(); i++ ) {
            u = (Unit) Units.get( i );
            TotalBaseBV += u.BaseBV;
            TotalModifier += u.MiscMod;
            TotalTonnage += u.Tonnage;
            TotalSkillBV += u.SkillsBV;
            TotalModifierBV += u.ModifierBV;
            TotalAdjustedBV += u.TotalBV;
            if (u.UsingC3) {
                NumC3++;
            }
        }
        
        if (NumC3 > 0){
            TotalC3BV += (TotalAdjustedBV * 0.05) * NumC3;
        }
        
        TotalForceBV += TotalAdjustedBV + TotalC3BV;
        UnevenForceMod = CommonTools.GetForceSizeMultiplier(OpForSize, Units.size());
        if (Units.size() > OpForSize) {
            TotalForceBVAdjusted = TotalForceBV * UnevenForceMod;
        } else {
            TotalForceBVAdjusted = TotalForceBV;
        }

        fireTableDataChanged();
    }
    
    public void AddUnit( Unit u ) {
        if( ! Units.contains( u ) ) {
            u.Refresh();
            Units.add( u );
            RefreshBV();
        }
        isDirty = true;
    }
    
    public void RemoveUnit( Unit u ){
        Units.remove(u);
        RefreshBV();
        isDirty = true;
    }

    public void SerializeXML(BufferedWriter file) throws IOException {
        String tab = "    ";
        Unit u = null;

        file.write( tab + "<force name=\"" + this.ForceName + "\">" );
        file.newLine();

        for (int i = 0; i <= this.Units.size() - 1; i++) {
            u = (Unit) Units.get(i);
            file.write(CommonTools.tab + CommonTools.tab + "<unit>");
            file.newLine();

            u.SerializeXML(file);

            file.write(CommonTools.tab + CommonTools.tab + "</unit>");
            file.newLine();
        }

        file.write( tab + "</force>" );
        file.newLine();
        isDirty = false;
    }

    public void RenderPrint(PrintSheet p) {
        p.setFont(CommonTools.SectionHeaderFont);
        p.WriteStr(ForceName, 0);
        p.NewLine();
        p.NewLine();
        p.setFont(CommonTools.ItalicFont);
        
        //Output column Headers
        p.WriteStr("Name", 60);
        p.WriteStr("Pilot", 140);
        p.WriteStr("Type", 60);
        p.WriteStr("Tonnage", 50);
        p.WriteStr("Base BV", 50);
        p.WriteStr("G/P", 30);
        //p.WriteStr("Skills BV", 50);
        p.WriteStr("Modifier", 40);
        //p.WriteStr("Pre-C3 BV", 50);
        p.WriteStr("Use C3", 50);
        //p.WriteStr("C3 BV", 30);
        p.WriteStr("Total BV", 50);
        p.WriteStr("Force BV", 0);
        p.NewLine();
        p.setFont(CommonTools.PlainFont);
        
        for (int i=0; i < Units.size(); i++) {
            Unit u = (Unit) Units.get(i);
            u.RenderPrint(p);
        }

        p.WriteLine();

        //Outut Totals
        p.setFont(CommonTools.ItalicFont);
        p.WriteStr(Units.size() + " Units", 60);
        p.WriteStr("", 140);
        p.WriteStr("", 60);
        p.WriteStr(String.format("%1$,.2f", TotalTonnage), 50);
        p.WriteStr(String.format("%1$,.0f", TotalBaseBV), 50);
        p.WriteStr("", 30);
        //p.WriteStr(String.format("%1$,.0f", TotalSkillBV), 50);
        p.WriteStr("", 40);
        //p.WriteStr(String.format("%1$,.0f", TotalAdjustedBV ), 50);
        p.WriteStr("", 50);
        //p.WriteStr(String.format("%1$,.0f", TotalC3BV), 30);
        p.setFont(CommonTools.BoldFont);
        p.WriteStr(String.format("%1$,.0f", TotalForceBV), 50);
        p.WriteStr(String.format("%1$,.0f", TotalForceBVAdjusted), 0);
        p.NewLine();
        p.setFont(CommonTools.PlainFont);
    }

    @Override
    public String getColumnName( int col ) {
        switch( col ) {
            case 0:
                return "Unit";
            case 1:
                return "Type";
            case 2:
                return "Mechwarrior";
            case 3:
                return "Tons";
            case 4:
                return "G";
            case 5:
                return "P";
            case 6:
                return "Mod";
            case 7:
                return "C3";
            case 8:
                return "BV";
        }
        return "";
    }
    public int getRowCount() { return Units.size(); }
    public int getColumnCount() { return 9; }
    public Object getValueAt( int row, int col ) {
        Unit u = (Unit) Units.get( row );
        switch( col ) {
            case 0:
                return u.TypeModel;
            case 1:
                return Constants.UnitTypes[u.UnitType];
            case 2:
                return u.Mechwarrior;
            case 3:
                return u.Tonnage;
            case 4:
                return u.Gunnery;
            case 5:
                return u.Piloting;
            case 6:
                return u.MiscMod;
            case 7:
                if( u.UsingC3 ) {
                    return "Yes";
                } else {
                    return "No";
                }
            case 8:
                return String.format( "%1$,.0f", u.TotalBV );
        }
        return null;
    }
    @Override
    public boolean isCellEditable( int row, int col ) {
        return false;
    }
    @Override
    public void setValueAt( Object value, int row, int col ) {
        return;
    }
}