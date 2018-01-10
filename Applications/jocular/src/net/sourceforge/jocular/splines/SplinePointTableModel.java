package net.sourceforge.jocular.splines;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.ArrayProperty;
import net.sourceforge.jocular.properties.EnumArrayProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;

public class SplinePointTableModel implements TableModel {
	private final SplineObject m_object;
	protected static final int INDEX_COLUMN = 0;
	protected static final int INDEP_VAR_COLUMN = 1;
	protected static final int DEP_VAR_COLUMN = 2;
	protected static final int POINT_TYPE_COLUMN = 3;
	
	private final OpticsProject m_project;
	private List<TableModelListener> m_listeners = new CopyOnWriteArrayList<TableModelListener>();
	public SplinePointTableModel(SplineObject so, OpticsProject project){
		super();
		m_project = project;
		m_object = so;
		m_object.addPropertyUpdatedListener(new PropertyUpdatedListener(){

			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				fireTableUpdate();
			}
			
		});
	}
	@Override
	public void addTableModelListener(TableModelListener listener) {
		if(!m_listeners.contains(listener)){
			m_listeners.add(listener);
		}

	}

	@Override
	public Class<?> getColumnClass(int col) {
		Class<?> result = null;
		switch(col){
		case INDEX_COLUMN:
			result = Integer.class;
			break;
		case DEP_VAR_COLUMN:
		case INDEP_VAR_COLUMN:
			result = EquationArrayProperty.class;
			break;
		case POINT_TYPE_COLUMN:
			result = EnumArrayProperty.class;
			break;
		default:
			
		}
		return result;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int col) {
		String result = null;
		switch(col){
		case INDEX_COLUMN:
			result = "Index";
			break;
		case DEP_VAR_COLUMN:
			if(m_object instanceof RotatedSpline){
				result = "Radius";
			} else {
				result = "Y";
			}
			break;
		case INDEP_VAR_COLUMN:
			if(m_object instanceof RotatedSpline){
				result = "Z";
			} else {
				result = "X";
			}
			break;
		case POINT_TYPE_COLUMN:
			result = "Point Type";
			break;
		default:
			
		}
		return result;
	}

	@Override
	public int getRowCount() {
		EquationArrayProperty eap = (EquationArrayProperty)m_object.getProperty(PropertyKey.POINTS_X);
		//return m_object.getSplinePointCount();
		return eap.getValue().length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Object result = null;
		switch(col){
		case INDEX_COLUMN:
			result = row;
			break;
			
		case INDEP_VAR_COLUMN:
			result = m_object.getProperty(PropertyKey.POINTS_X);
			break;
		case DEP_VAR_COLUMN:
			result = m_object.getProperty(PropertyKey.POINTS_Y);
			break;
		case POINT_TYPE_COLUMN:
			result = m_object.getProperty(PropertyKey.POINTS_TYPES);
			break;
		default:
			
		}
		return result;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if(col == INDEX_COLUMN){
			return false;
		}
		return true;
	}

	@Override
	public void removeTableModelListener(TableModelListener listener) {
		m_listeners.remove(listener);

	}
	private void fireTableUpdate(){
		TableModelEvent e = new TableModelEvent(this);
		for(TableModelListener l : m_listeners){
			l.tableChanged(e);
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if(!(value instanceof String[])){
			return;
		}
		String[] ss = (String[])value;
		String s = "";
		for(String ts : ss){
			s += "\""+ts+"\",";
		}
		System.out.println("SplinePointTableModel.setValueAt "+s);
		ArrayProperty eap;
		PropertyKey key;
		switch(col){
		case INDEP_VAR_COLUMN:
			key = PropertyKey.POINTS_X;
			eap = new EquationArrayProperty(ss, m_object, key);
			m_project.addPropertyEdit(m_object, key, eap.getDefiningString());
			break;
		case DEP_VAR_COLUMN:
			key = PropertyKey.POINTS_Y;
			eap = new EquationArrayProperty(ss, m_object, key);
			m_project.addPropertyEdit(m_object, key, eap.getDefiningString());
			//m_object.setProperty(PropertyKey.POINTS_X,eap.getDefiningString());
			break;
		case POINT_TYPE_COLUMN:
			key = PropertyKey.POINTS_TYPES;
			eap = new EnumArrayProperty(SplineObject.PointType.values()[0], ss);
			m_project.addPropertyEdit(m_object, key, eap.getDefiningString());
			System.out.println("SplinePointTableModel.setValueAt \""+eap.getDefiningString()+"\"");
			break;
		default:
			
		}

	}

}
