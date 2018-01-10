package net.sourceforge.jocular.gui.tables;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import net.sourceforge.jocular.input_verification.PropertyInputVerifier;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

@SuppressWarnings("serial")
public class OpticsPropertyTable extends JTable {
	
	private final OpticsPropertyTableModel m_model;
	
	public OpticsPropertyTable(OpticsPropertyTableModel model){
		super(model);
		m_model = model;
		setDefaultRenderer(Property.class, new PropertyCellRenderer());
		setDefaultRenderer(PropertyKey.class, new PropertyKeyCellRenderer());
		
		setDefaultEditor(Property.class, PropertyCellEditorFactory.getDefaultEditor());
		
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		
		int r = this.rowAtPoint(event.getPoint());
		
		return m_model.getPropertyKey(r).getTooltip();
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		
		Property<?> p = (Property<?>) m_model.getValueAt(row, column);
		PropertyInputVerifier verifier = m_model.getInputVerifier(row, column);
		
		PropertyCellEditorFactory editorFactory = new PropertyCellEditorFactory();
		TableCellEditor result = editorFactory.getEditor(p, verifier);
		
		return result;
	}
	
	// bam 131103 - The following method was taken from http://tips4java.wordpress.com/2008/10/20/table-select-all-editor/	
	@Override
	public boolean editCellAt(int row, int column, EventObject e)
	{
		boolean result = super.editCellAt(row, column, e);
		selectAll(e);

		return result;
	}
	
	private void selectAll(EventObject e){
		
		final Component editor = getEditorComponent();

		if (editor == null || ! (editor instanceof JTextField)){
			return;
		}

		if (e == null){
			((JTextField)editor).selectAll();
			return;
		}

		//  Typing in the cell was used to activate the editor
		if (e instanceof KeyEvent){
			((JTextField)editor).selectAll();
			return;
		}

		//  F2 was used to activate the editor
		if (e instanceof ActionEvent){
			((JTextField)editor).selectAll();
			return;
		}

		//  A mouse click was used to activate the editor.
		//  Generally this is a double click and the second mouse click is
		//  passed to the editor which would remove the text selection unless
		//  we use the invokeLater()
		if (e instanceof MouseEvent){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					((JTextField)editor).selectAll();
				}
			});
		}
	}

	
	protected class PropertyKeyCellRenderer extends DefaultTableCellRenderer {
		@Override
		protected void setValue(Object value) {
			super.setValue(((PropertyKey)value).toString());
		}
		
	}
	
}
