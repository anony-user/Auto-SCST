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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.StringArrayProperty;

public class AutofocusParameterCellEditor implements TableCellEditor {
	OpticsProject m_project;
	AutofocusParameterTableModel m_model;
	//Object m_value = null;
	String[] m_definingStrings = new String[0];
	int m_indexEditing = 0;
	int m_editingColumn = 0;
	protected static final String POSITIONER_SUFFIX = " - Pos";
	JComboBox<String> m_stringCombo = new JComboBox<String>();
	JTextField m_textField = new JTextField();
	
	public AutofocusParameterCellEditor(OpticsProject project, AutofocusParameterTableModel model){
		m_project = project;
		m_model = model;
		m_stringCombo.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				//m_value = m_stringCombo.getItemAt(m_stringCombo.getSelectedIndex());
				fireEditingStopped();
			}
			
		});

		m_textField.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
//				if(m_definingStrings != null){
//					m_definingStrings[m_indexEditing] = m_textField.getText();
//				}
				//m_value = m_textField.getText();
				fireEditingStopped();
			}
			
		});
	}
	private List<CellEditorListener> m_listeners = new CopyOnWriteArrayList<CellEditorListener>();
	@Override
	public void addCellEditorListener(CellEditorListener listener) {
		if(!m_listeners.contains(listener)){
			m_listeners.add(listener);
		}

	}

	@Override
	public void cancelCellEditing() {
		fireEditingCancelled();
	}

	@Override
	public Object getCellEditorValue() {
		String v = null;
		switch(m_editingColumn){
		case 0:
			v = m_stringCombo.getItemAt(m_stringCombo.getSelectedIndex());
			break;
		case 1:
			v = m_stringCombo.getItemAt(m_stringCombo.getSelectedIndex());
			break;
		case 2:
			v = m_textField.getText();
			break;
		case 3:
			v = m_textField.getText();
			break;
		default:
			break;
		}
		
		m_definingStrings[m_indexEditing] = v;
		
		return m_definingStrings;
	}

	@Override
	public boolean isCellEditable(EventObject e) {
		return true;
	}

	@Override
	public void removeCellEditorListener(CellEditorListener listener) {
		m_listeners.remove(listener);

	}

	@Override
	public boolean shouldSelectCell(EventObject e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopCellEditing() {
		fireEditingStopped();
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		Component result = null;
		switch(column){
		case 0:
			if(value instanceof StringArrayProperty){
				StringArrayProperty sap = (StringArrayProperty)value;
				m_indexEditing = row;
				m_editingColumn = column;
				m_definingStrings = sap.getValue();
				
			}
			
			updateComboWithObjects();
			result = m_stringCombo;
			break;
		case 1:
			if(value instanceof StringArrayProperty){
				StringArrayProperty sap = (StringArrayProperty)value;
				m_indexEditing = row;
				m_editingColumn = column;
				m_definingStrings = sap.getValue();
				
			}
			updateComboWithProperties(m_model.getObjectName(row));
			result = m_stringCombo;
			break;
		case 2:
			if(value instanceof EquationArrayProperty){
				EquationArrayProperty eap = (EquationArrayProperty)value;
				m_indexEditing = row;
				m_editingColumn = column;
				m_definingStrings = eap.getDefiningStrings();
				m_textField.setText(m_definingStrings[row]);
				result = m_textField;
			}
			break;
		case 3:
			if(value instanceof EquationArrayProperty){
				EquationArrayProperty eap = (EquationArrayProperty)value;
				m_definingStrings = eap.getDefiningStrings();
				m_textField.setText(eap.getDefiningStrings()[row]);
				m_indexEditing = row;
				m_editingColumn = column;
				result = m_textField;
			}
			break;
		default:
			break;
		}
		return result;
	}

	private void updateComboWithObjects() {
		m_stringCombo.removeAllItems();
		m_stringCombo.setSelectedItem(null);
		Collection<OpticsObject> os = m_project.getFlattenedOpticsObjects(false);
		
		for(OpticsObject o : os){
			String n = o.getName();
			if(n.trim().equals("")){
				n = o.getID().toHashString();
			}
			m_stringCombo.addItem(n);
			m_stringCombo.addItem(n + POSITIONER_SUFFIX);
		}
		
	}

	private void updateComboWithProperties(String objectName) {
		m_stringCombo.removeAllItems();
		m_stringCombo.setSelectedItem(null);
		PropertyOwner po = getPropertyOwner(m_project, objectName);
		List<PropertyKey> pks = po.getPropertyKeys();
		//only use property keys that reference equation properties
		List<PropertyKey> pks2 = new ArrayList<PropertyKey>();
		for(PropertyKey pk : pks){
			if(po.getProperty(pk) instanceof EquationProperty){
				pks2.add(pk);
			}
		}
		for(PropertyKey pk : pks2){
			m_stringCombo.addItem(pk.getDescription());
		}
		
	}
	private static PropertyOwner getPropertyOwner(OpticsProject project, String s){
		PropertyOwner result = null;
		String name = s;
		boolean objectNotPositioner = true;
		if(s.endsWith(POSITIONER_SUFFIX)){
			objectNotPositioner = false;
			name = s.substring(0,s.length() - POSITIONER_SUFFIX.length());
			
		}
		List<OpticsObject> os = PropertyManager.getInstance().getObjectByName(project.getOpticsObject(), name);
		if(os.size() != 1){
			throw new RuntimeException("Object not specified enough. \""+ s + "\"");
		}
		if(objectNotPositioner){
			result = os.get(0);
		} else {
			result = os.get(0).getPositioner();
		}
		return result;
	}
	private void fireEditingStopped(){
		for(CellEditorListener ce : m_listeners){
			ce.editingStopped(new ChangeEvent(this));
		}
	}
	private void fireEditingCancelled(){
		for(CellEditorListener ce : m_listeners){
			ce.editingStopped(new ChangeEvent(this));
		}
	}
}
