/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.settings;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;

import net.sourceforge.jocular.gui.tables.OpticsPropertyTable;
import net.sourceforge.jocular.gui.tables.OpticsPropertyTableModel;
import net.sourceforge.jocular.project.OpticsProject;

@SuppressWarnings("serial")
public class SettingsDialog extends JDialog {
	private final OpticsPropertyTableModel m_model = new OpticsPropertyTableModel();
	private OpticsPropertyTable m_table = new OpticsPropertyTable(m_model);
	
	public SettingsDialog(Frame owner, Settings s, OpticsProject p){
		super(owner);
		m_model.setPropertyOwner(s, p);
		
		
		
		initializeDisplay();
		initializePanels();
	}
	
	private void initializeDisplay(){
		setSize(new Dimension(300,300));
	}
	
	private void initializePanels(){
		
		Box b = new Box(BoxLayout.Y_AXIS);
		b.add(new JScrollPane(m_table));		
		
		JPanel panel = new JPanel();
		
		panel.add(createOKButton());
		panel.add(createResetButton());
		
		b.add(panel);
		add(b);
	}


	private JButton createOKButton(){
		JButton okButton = new JButton("Done");
		okButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				TableCellEditor tce = m_table.getCellEditor();
				if(tce != null){
					tce.stopCellEditing();
				}
				dispose();
				
			}
			
		});
		
		return okButton;
	}
	
	private JButton createResetButton(){
		JButton okButton = new JButton("Reset");
		okButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				resetValuesToDefault();
				
			}
			
		});
		
		return okButton;
	}
	
	private void resetValuesToDefault(){
		
		int result = JOptionPane.showConfirmDialog(null, "This will reset all settings to their default values.\nA restart is required for changes to take effect.", "Confirm Reset", JOptionPane.OK_CANCEL_OPTION);
		
		if(result == JOptionPane.OK_OPTION){
			Settings.SETTINGS.restoreDefaults();
		}
		
	}
}
