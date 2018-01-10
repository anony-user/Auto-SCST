/*******************************************************************************
 * Copyright (c) 2014 Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.autofocus;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.StringArrayProperty;
import net.sourceforge.jocular.settings.Settings;

public class AutofocusParameterCellRenderer extends JLabel implements
		TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		
		setBackground(table.getBackground());
		if(isSelected){
			setOpaque(true);
			setBorder(new LineBorder(Color.BLACK));
		} else {
			setOpaque(true);
			setBorder(null);
		}

		if(value instanceof StringArrayProperty){
			
			StringArrayProperty sap = (StringArrayProperty)value;
			if(sap.getValue().length > 0){
				setText(sap.getValue()[row]);
			} else {
				switch(column){
				case 0:
					setText("Click to specify object.");
					break;
				case 1:
					setText("Click to specify property");
					break;
				default:
					setText("null");
				}
				
			}
		} else if(value instanceof EquationArrayProperty){
			EquationArrayProperty eap = (EquationArrayProperty)value;
			if(eap.getDefiningStrings().length > 0){
				
			
				UnitedValue v = eap.getValue()[row];
				switch(Settings.SETTINGS.getNumberDisplay()){
				case EQUATION:
					setText(eap.getDefiningStrings()[row]);
					break;
				case METRIC:
					setText(v.getBaseUnitString());
					break;
				default:
				case IMPERIAL:
					setText("Imperial view not implemented yet.");
					break;
				}
				if(v.isError()){
					setBackground(new Color(1.0f, 0.5f, 0.5f));
					setToolTipText(v.getErrorText());
				} else if(v.isSimpleValue()){
					this.setBackground(new Color(0.5f, 1.0f, 0.5f));
				} else {
					this.setBackground(new Color(0.5f, 0.5f, 1.0f));
				}
			} else {
				setText("");
			}
				


		}
		return this;
	}

}
