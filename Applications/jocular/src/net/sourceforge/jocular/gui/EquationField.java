/*******************************************************************************
 * Copyright (c) 2015, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.settings.Settings;

public class EquationField extends JTextField {
	PropertyOwner m_owner;
	PropertyKey m_key;
	OpticsProject m_project;
	/**
	 * make this private so it can't be used.
	 */
	private EquationField(){
		
	}
	public EquationField(PropertyOwner owner, PropertyKey key, OpticsProject project){
		m_owner = owner;
		m_key = key;
		m_project = project;
		updateText();
		
		addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_project.addPropertyEdit(m_owner, m_key, getText());	
			
			}
			
		});
		m_owner.addPropertyUpdatedListener(new PropertyUpdatedListener(){

			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				updateText();
				
			}
			
		});
	}
	private void updateText(){
		EquationProperty p = (EquationProperty)m_owner.getProperty(m_key);
		
		if(p == null){
			setText("");
		} else {
			
			
			UnitedValue v = p.getValue();
			switch(Settings.SETTINGS.getNumberDisplay()){
			case EQUATION:
				setText(p.getDefiningString());
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
				setToolTipText(v.getBaseUnitString());
			} else {
				this.setBackground(new Color(0.5f, 0.5f, 1.0f));
				setToolTipText(v.getBaseUnitString());
			}

		}
		
	}
}
