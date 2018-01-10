package net.sourceforge.jocular.gui.tables;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sourceforge.jocular.input_verification.PropertyInputVerifier;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;

public class OpticsPropertyTableModel extends PropertyInputVerifier implements TableModel {
	private final CopyOnWriteArrayList<TableModelListener> m_listeners = new CopyOnWriteArrayList<TableModelListener>();
	private PropertyOwner m_owner = null;
	private OpticsProject m_project = null;
	
	public OpticsPropertyTableModel(){
		
	}
	public void setPropertyOwner(PropertyOwner o, OpticsProject p){
		m_owner = o;
		m_project = p;
		fireUpdate();
	}

	@Override
	public void addTableModelListener(TableModelListener tml) {
		m_listeners.addIfAbsent(tml);
	}
	public void fireUpdate(){
		for(TableModelListener l : m_listeners){
			l.tableChanged(new TableModelEvent(this));
		}
	}
	

	@Override
	public Class<?> getColumnClass(int c) {
		Class<?> result;
		switch(c){
		case 0:
			result = String.class;
			break;
		case 1:
			result = Property.class;
			break;
		default:
			result = null;
			break;
		}
		return result;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int i) {
		String result;
		switch(i){
		case 0:
			result = "Property";
			break;
		case 1:
			result = "Value";
			break;
		default:
			result = "N/A";
			break;
		}
		return result;
	}

	@Override
	public int getRowCount() {
		int result = 0;
		if(m_owner != null){
			result = m_owner.getPropertyKeys().size();
		}
		return result;
	}
	public PropertyKey getPropertyKey(int r){
		return m_owner.getPropertyKeys().get(r);
	}
	@Override
	public Object getValueAt(int r, int c) {
		if(m_owner == null){
			return null;
		}
		PropertyKey pk = m_owner.getPropertyKeys().get(r);
		Object result;
		switch(c){
		case 0:
			result = pk.toString();
			break;
		case 1:
			result = m_owner.getProperty(pk);
			break;
		default:
			result = null;
			break;
		}
		
		return result;
	}

	@Override
	public boolean isCellEditable(int r, int c) {
		boolean result = false;
		if(c == 1){
			result = true;
		}
		return result;
	}

	@Override
	public void removeTableModelListener(TableModelListener tml) {
		m_listeners.remove(tml);

	}

	@Override
	public void setValueAt(Object s, int r, int c) {
		

		if(m_project == null || m_owner == null){
			return;
		}

		
		// A bit of a hack

		if(s instanceof ImageProperty){
			s = ((ImageProperty)s).getDefiningString();
		}
		
		String value = s.toString();
		if(s instanceof Enum){
			value = ((Enum<?>)s).name();
		}
		
		//m_project.addAndDoEdit(new PropertyEdit(m_project, m_owner, m_owner.getPropertyKeys().get(r), value));
		m_project.addPropertyEdit(m_owner, m_owner.getPropertyKeys().get(r), value);
	}
		
	public PropertyInputVerifier getInputVerifier(int row, int column){
		
		m_propertyKeyToVerify = m_owner.getPropertyKeys().get(row);
		
		return this;
	}

	@Override        
	public VerificationResult verify(JComponent input) {
		String text = null;

		if (input instanceof JTextField) {
			text = ((JTextField) input).getText();
		} else {
			return new VerificationResult(true, "");
		}
		
		return m_owner.trySetProperty(m_propertyKeyToVerify, text);
	}
}
