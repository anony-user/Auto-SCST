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
package net.sourceforge.jocular.splines;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import net.sourceforge.jocular.properties.ArrayProperty;
import net.sourceforge.jocular.properties.EnumArrayProperty;

public class SplinePointCellEditor implements TableCellEditor {
	private List<CellEditorListener> m_listeners = new CopyOnWriteArrayList<CellEditorListener>();



	String[] m_definingStrings = new String[0];
	int m_indexEditing = 0;
	int m_editingColumn = 0;
	protected static final String POSITIONER_SUFFIX = " - Pos";
	
	JComboBox<SplineObject.PointType> m_pointTypeCombo = new JComboBox<SplineObject.PointType>(SplineObject.PointType.values());
	
	JTextField m_textField = new JTextField();

	public SplinePointCellEditor(){


		m_pointTypeCombo.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				fireEditingStopped();
			}
			
		});

		m_textField.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				fireEditingStopped();
			}
			
		});
	}
	
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
		System.out.println("SplinePointCellEditor.getCellEditorValue!!!");
		switch(m_editingColumn){
		case SplinePointTableModel.INDEP_VAR_COLUMN:
			m_definingStrings[m_indexEditing] = m_textField.getText();
			break;
		case SplinePointTableModel.DEP_VAR_COLUMN:
			m_definingStrings[m_indexEditing] = m_textField.getText();
			break;
		case SplinePointTableModel.POINT_TYPE_COLUMN:
			m_definingStrings[m_indexEditing] = m_pointTypeCombo.getItemAt(m_pointTypeCombo.getSelectedIndex()).name();
			System.out.println("SplinePointCellEditor.getCellEditorValue "+m_definingStrings[m_indexEditing]);
			break;
		default:
			break;
		}
		
		
		
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
		m_definingStrings = ((ArrayProperty)value).getDefiningStrings();
		m_indexEditing = row;
		m_editingColumn = column;
		switch(column){
		case SplinePointTableModel.INDEP_VAR_COLUMN:
			m_textField.setText(m_definingStrings[row]); 
			result = m_textField;
			break;
		case SplinePointTableModel.DEP_VAR_COLUMN:
			m_textField.setText(m_definingStrings[row]);
			result = m_textField;
			break;
		case SplinePointTableModel.POINT_TYPE_COLUMN:
			m_pointTypeCombo.setSelectedItem(((EnumArrayProperty)value).getValue()[row]);
			result = m_pointTypeCombo;
			break;
		default:
			break;
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
