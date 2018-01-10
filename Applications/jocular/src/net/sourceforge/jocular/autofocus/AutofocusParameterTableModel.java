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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sourceforge.jocular.math.SolverUpdateEvent;
import net.sourceforge.jocular.math.SolverUpdateListener;
import net.sourceforge.jocular.math.SystemSolver;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.ArrayProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.properties.StringArrayProperty;

public class AutofocusParameterTableModel implements TableModel, PropertyUpdatedListener, SolverUpdateListener {
	private AutofocusSensor m_sensor;
	private OpticsProject m_project;
	private SystemSolver m_solver;
	private NumberFormat m_format =  new DecimalFormat("##0.0E0");
	private final CopyOnWriteArrayList<TableModelListener> m_listeners = new CopyOnWriteArrayList<TableModelListener>();
	public AutofocusParameterTableModel(OpticsProject project, SystemSolver solver){
		m_project = project;
		m_sensor = null;
		m_solver = solver;
		m_solver.addSolverUpdateListener(this);
		
	}
	@Override
	public void addTableModelListener(TableModelListener tml) {
		if(!m_listeners.contains(tml)){
			m_listeners.add(tml);
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		Class<?> result;
		
		switch(c){
		case 0:
			result = StringArrayProperty.class;
			break;
		case 1:
			result = StringArrayProperty.class;
			break;
		case 2:
			result = EquationArrayProperty.class;
			break;
		case 3:
			result = EquationArrayProperty.class;
			break;
		case 4:
			result = String.class;
			break;
		case 5:
			result = String.class;
			break;
		default:
			result = null;
		}
		return result;
	}

	@Override
	public int getColumnCount() {
		return 6;
	}

	@Override
	public String getColumnName(int c) {
		String result;
		
		switch(c){
		case 0:
			result = "Object";
			break;
		case 1:
			result = "Property";
			break;
		case 2:
			result = "Min Value";
			break;
		case 3:
			result = "Max Value";
			break;
		case 4:
			result = "Best Value";
			break;
		case 5:
			result = "Std. Dev.";
			break;
		default:
			result = "";
		}
		return result;
	}

	@Override
	public int getRowCount() {
		int result = 0;
	
		if(m_sensor != null){
			result = m_sensor.getParameterCount();
		}
		return result;
	}
	public void addRow(int i){
		//add new rows
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_NAMES,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_NAMES))).addRowToDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_PROPERTIES,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES))).addRowToDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MINIMUMS,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MINIMUMS))).addRowToDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MAXIMUMS,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MAXIMUMS))).addRowToDefiningString(i)));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.OBJECT_NAMES, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_NAMES))).addRowToDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.OBJECT_PROPERTIES, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES))).addRowToDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.MINIMUMS, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MINIMUMS))).addRowToDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.MAXIMUMS, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MAXIMUMS))).addRowToDefiningString(i));
		
//		m_sensor.addRowToArrayProperty(PropertyKey.OBJECT_NAMES, i);
//		m_sensor.addRowToArrayProperty(PropertyKey.OBJECT_PROPERTIES, i);
//		m_sensor.addRowToArrayProperty(PropertyKey.MINIMUMS, i);
//		m_sensor.addRowToArrayProperty(PropertyKey.MAXIMUMS, i);
		
		//fireModelUpdated();
	}
	public void deleteRow(int i) {
		if(i < 0){
			return;
		}
		//remove new rows
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_NAMES,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_NAMES))).removeRowFromDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_PROPERTIES,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES))).removeRowFromDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MINIMUMS,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MINIMUMS))).removeRowFromDefiningString(i)));
//		m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MAXIMUMS,  ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MAXIMUMS))).removeRowFromDefiningString(i)));
		
		m_project.addPropertyEdit(m_sensor,  PropertyKey.OBJECT_NAMES, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_NAMES))).removeRowFromDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.OBJECT_PROPERTIES, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES))).removeRowFromDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.MINIMUMS, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MINIMUMS))).removeRowFromDefiningString(i));
		m_project.addPropertyEdit(m_sensor,  PropertyKey.MAXIMUMS, ((ArrayProperty)(m_sensor.getProperty(PropertyKey.MAXIMUMS))).removeRowFromDefiningString(i));
		
		
//		m_sensor.removeRowFromArrayProperty(PropertyKey.OBJECT_NAMES, i);
//		m_sensor.removeRowFromArrayProperty(PropertyKey.OBJECT_PROPERTIES, i);
//		m_sensor.removeRowFromArrayProperty(PropertyKey.MINIMUMS, i);
//		m_sensor.removeRowFromArrayProperty(PropertyKey.MAXIMUMS, i);
		//fireModelUpdated();
		
	}

	@Override
	public Object getValueAt(int r, int c) {
		if(m_sensor == null){
			return "null";
		}
		Object result;
		switch(c){
		case 0:
			result = ((StringArrayProperty)m_sensor.getProperty(PropertyKey.OBJECT_NAMES));
			break;
		case 1:
			result = ((StringArrayProperty)m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES));
			break;
		case 2:
			result = ((EquationArrayProperty)m_sensor.getProperty(PropertyKey.MINIMUMS));
			break;
		case 3:
			result = ((EquationArrayProperty)m_sensor.getProperty(PropertyKey.MAXIMUMS));
			break;
		case 4:
			result = m_format.format(m_solver.getBestParameterValue(r));
			break;
		case 5:
			result = m_format.format(m_solver.getStandardDeviation(r));
			break;
		default:
			result = null;
			break;
		}
		return result;
	}

	@Override
	public boolean isCellEditable(int r, int c) {
		boolean result;
		switch(c){
		case 0:
		case 1:
		case 2:
		case 3:
			result =  true;
			break;
		default:
			result =  false;
			break;
		}
		return result;
	}

	@Override
	public void removeTableModelListener(TableModelListener tml) {
		m_listeners.remove(tml);

	}

	@Override
	public void setValueAt(Object v, int r, int c) {
		System.out.println("AutofocusParameterTableModel.setValueAt value is "+v.getClass().getName()+" -> "+v.toString());
		
		switch(c){
		case 0:
			if(v instanceof String[]){
				
				String[] ss = (String[])v;
				String s = (new StringArrayProperty(ss)).getDefiningString();
				//System.out.println("AutofocusParameterTableModel.setValue at \""+s+"\"");
				//m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_NAMES, (new StringArrayProperty(ss)).getDefiningString()));
				m_project.addPropertyEdit(m_sensor, PropertyKey.OBJECT_NAMES, s);
				//m_sensor.setProperty(PropertyKey.OBJECT_NAMES, (new StringArrayProperty(ss)).getDefiningString());
			}
			
			break;
		case 1:
			if(v instanceof String[]){
				String[] ss = (String[])v;
				//m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.OBJECT_PROPERTIES, (new StringArrayProperty(ss)).getDefiningString()));
				String s = (new StringArrayProperty(ss)).getDefiningString();
				m_project.addPropertyEdit(m_sensor, PropertyKey.OBJECT_PROPERTIES, s);
				//m_sensor.setProperty(PropertyKey.OBJECT_PROPERTIES, (new StringArrayProperty(ss)).getDefiningString());
			}
			break;
		case 2:
			if(v instanceof String[]){
				String[] ss = (String[])v;
				String s = (new EquationArrayProperty(ss, m_sensor, PropertyKey.MINIMUMS)).getDefiningString();
				//m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MINIMUMS, eap.getDefiningString()));
				m_project.addPropertyEdit(m_sensor, PropertyKey.MINIMUMS, s);
				//m_sensor.setProperty(PropertyKey.MINIMUMS, eap.getDefiningString());
			}
			break;
		case 3:
			if(v instanceof String[]){
				String[] ss = (String[])v;
				String s = (new EquationArrayProperty(ss, m_sensor, PropertyKey.MAXIMUMS)).getDefiningString();
				//m_project.addAndDoEdit(new PropertyEdit(m_project, m_sensor, PropertyKey.MAXIMUMS, eap.getDefiningString()));
				m_project.addPropertyEdit(m_sensor, PropertyKey.MAXIMUMS, s);
				//m_sensor.setProperty(PropertyKey.MAXIMUMS, eap.getDefiningString());
			}
			break;
		default:
			break;
		}

	}
	public void setSensor(AutofocusSensor s) {
		if(m_sensor != null){
			m_sensor.removePropertyUpdatedListener(this);
		}
		m_sensor = s;
		if(s != null){
			
			m_sensor.addPropertyUpdatedListener(this);
		}
		fireModelUpdated();
		
		
	}
	
	@Override
	public void propertyUpdated(PropertyUpdatedEvent e) {
		fireModelUpdated();
		
	}
	protected void fireColumnUpdated(){
		for(TableModelListener tml : m_listeners){
			//tml.tableChanged(new TableModelEvent(this,0,getRowCount() - 1, 4,5));
			tml.tableChanged(new TableModelEvent(this));
		}
	}
	protected void fireModelUpdated(){
		for(TableModelListener tml : m_listeners){
			tml.tableChanged(new TableModelEvent(this));
		}
	}
	protected String getObjectName(int row){
		return ((StringArrayProperty)m_sensor.getProperty(PropertyKey.OBJECT_NAMES)).getValue()[row];
	}
	protected String getPropertyName(int row){
		return ((StringArrayProperty)m_sensor.getProperty(PropertyKey.OBJECT_PROPERTIES)).getValue()[row];
	}
	public AutofocusSensor getSensor(){
		return m_sensor;
	}
	protected PropertyOwner getPropertyOwner(int i){
		PropertyOwner result;
		String oName = getObjectName(i);
		boolean isPos = oName.endsWith(AutofocusParameterCellEditor.POSITIONER_SUFFIX);
		if(isPos){
			oName = oName.substring(0,oName.length() - AutofocusParameterCellEditor.POSITIONER_SUFFIX.length());
		}
		List<OpticsObject> list = PropertyManager.getInstance().getObjectByName(m_project.getOpticsObject(), oName);
		if(list.size() != 1){
			throw new RuntimeException("More or less than one object found: "+list.size()+ " of name \""+oName+"\"");
		}
		
		if(isPos){
			result = list.get(0).getPositioner();
		} else {
			result = list.get(0);
		}
		return result;
	}
	public OpticsProject getProject(){
		return m_project; 
	}
	@Override
	public void solverUpdated(SolverUpdateEvent e) {
		fireColumnUpdated();
		
	}
	
	
}
