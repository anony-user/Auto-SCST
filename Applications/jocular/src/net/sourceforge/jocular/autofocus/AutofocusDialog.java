/*******************************************************************************
 * Copyright (c) 2014, Kenneth MacCallum
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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.jocular.graphs.GraphPanel;
import net.sourceforge.jocular.graphs.GraphPanel.DotType;
import net.sourceforge.jocular.graphs.GraphPanel.GraphType;
import net.sourceforge.jocular.math.MultiMinimumSolver;
import net.sourceforge.jocular.math.SolverUpdateEvent;
import net.sourceforge.jocular.math.SolverUpdateListener;
import net.sourceforge.jocular.math.SystemSolver;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.StringArrayProperty;

public class AutofocusDialog extends JDialog {

	protected static final String ERROR_SERIES = "Error";
	protected static final String FIT_SERIES = "Fit";
	private  static final int NUM_PHOTONS_PER_CALC = 2000;
	private OpticsProject m_project;
	private GraphPanel m_graph;
	
	private JComboBox<AutofocusSensor> m_autofocusCombo;
	private final SystemSolver m_solver = new MultiMinimumSolver();
	//private SystemSolver m_solver = new SimpleMinimumSolver();
	//private final SystemToSolve m_system;
	private final AutofocusParameterTableModel m_tableModel;
	private JButton m_startButton;
	private String[] m_errorSeriesKeys;
	private String[] m_fitSeriesKeys;
	private JTable m_table;


	public AutofocusDialog(Frame owner, OpticsProject project){
		super(owner,"Autofocus Solver");
		m_project = project;
		 m_tableModel = new AutofocusParameterTableModel(m_project, m_solver);
		 
		
		m_solver.addSolverUpdateListener(new SolverUpdateListener(){

			@Override
			public void solverUpdated(SolverUpdateEvent e) {
				if(m_solver.isSolved()){
					JOptionPane.showMessageDialog(AutofocusDialog.this, "System solved.");
				}
				//make sure button text is correct
				if(m_solver.isRunning()){
					m_startButton.setText("Stop");
					
				} else {
					m_startButton.setText("Start");
				}
				updateGraph();
				
			}
			
		});
		m_graph = new GraphPanel();
		m_graph.setGraphType(GraphType.XY_DOT);
		//m_graph.updateGraph("null", new double[] {0,1}, new double[] {0,1});
		m_graph.autoScale(true, false, true, false);
		m_graph.setPreferredSize(new Dimension(400,400));
		m_autofocusCombo = new JComboBox<AutofocusSensor>();
		m_autofocusCombo.setRenderer(new ObjectComboRenderer());


		m_autofocusCombo.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				//updateAutofocusCombo();
				m_tableModel.setSensor(m_autofocusCombo.getItemAt(m_autofocusCombo.getSelectedIndex()));
				
			}
			
		});
//		m_autofocusCombo.addActionListener(new ActionListener(){
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				updateAutofocusCombo();
//				
//			}
//			
//			
//		});
//		m_autofocusCombo.addPopupMenuListener(new PopupMenuListener(){
//
//			@Override
//			public void popupMenuCanceled(PopupMenuEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
//				updateAutofocusCombo();
//				
//			}
//			
//		});
		
		setSize(new Dimension(500,400));
		Box b = Box.createVerticalBox();
		b.add(m_graph);
		//b.createVerticalGlue();
		JPanel p1 = new JPanel();
		p1.add(new JLabel("Autofocus Sensor:"));
		p1.add(m_autofocusCombo);
		b.add(p1);
//		JPanel p2names = new JPanel(new GridLayout(1,4));
//		p2names.add(new JLabel("Object"));
//		p2names.add(new JLabel("Property"));
//		p2names.add(new JLabel("Min"));
//		p2names.add(new JLabel("Max"));
//		b.add(p2names);
//		JPanel p2 = new JPanel(new GridLayout(1,4));
//		b.add(p2);
		m_table = new JTable(m_tableModel);
		AutofocusParameterCellRenderer apcr = new AutofocusParameterCellRenderer();
		AutofocusParameterCellEditor apce = new AutofocusParameterCellEditor(m_project, m_tableModel);
		m_table.setDefaultRenderer(StringArrayProperty.class, apcr);
		m_table.setDefaultRenderer(EquationArrayProperty.class, apcr);
		m_table.setDefaultEditor(StringArrayProperty.class, apce);
		m_table.setDefaultEditor(EquationArrayProperty.class, apce);
		//m_table.setPreferredSize(new Dimension(400,50));
		m_table.setRowSelectionAllowed(true);
		m_table.addMouseListener(new MouseAdapter(){

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				Point p = e.getPoint();
				int r = m_table.rowAtPoint(p);
				m_table.getSelectionModel().setSelectionInterval(r, r);
			}
			
		});
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(m_table.isEditing()){
					m_table.getCellEditor().stopCellEditing();
				}
				
			}
			
		});
		b.add(new JScrollPane(m_table));
		//JPanel p4 = new JPanel(new GridLayout(1,4));
		Box p4 = Box.createHorizontalBox();
		b.add(p4);
		JButton addRowButton = new JButton("Add parameter");
		addRowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				
				m_tableModel.addRow(m_tableModel.getRowCount());
			}
			
		});
		JButton deleteRowButton = new JButton("Delete parameter");
		deleteRowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = m_table.getSelectedRow();
				if(i < 0){
					JOptionPane.showMessageDialog(AutofocusDialog.this, "Must select row in order to delete.");
				} else {
					m_tableModel.deleteRow(i);
				}
			}
		});
		p4.add(addRowButton);
		p4.add(deleteRowButton);
		p4.add(new JLabel());
		p4.add(new JLabel());
		//JPanel p3 = new JPanel();
		Box p3 = Box.createHorizontalBox();
		m_startButton = new JButton("Start");
		m_startButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				runAutofocus();
				
			}
			
		});
		JButton closeButton = new JButton("Close");
		
		closeButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				//m_solver.reset();
				m_solver.stop();
				dispose();
				
			}
			
		});
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateAutofocusCombo();
				m_solver.reset();
				m_graph.removeAllSeries();
				//updateGraph();
			}
			
		});
		JButton acceptButton = new JButton("Accept");
		acceptButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				//m_algorithm.accept();
			
				//double[] bvs = new double[m_solver.getParameterCount()];
				for(int i = 0; i < m_solver.getParameterCount(); ++i){
					//bvs[i] = m_solver.getBestParameterValue(i);
					PropertyKey pk = PropertyManager.getInstance().getPropertyKey(m_tableModel.getPropertyOwner(i), m_tableModel.getPropertyName(i));
					//m_tableModel.getProject().addAndDoEdit(new PropertyEdit(m_tableModel.getProject(), m_tableModel.getPropertyOwner(i), pk, Double.toString(m_solver.getBestParameterValue(i))));
					m_tableModel.getProject().addPropertyEdit(m_tableModel.getPropertyOwner(i), pk, Double.toString(m_solver.getBestParameterValue(i)));
				}
//				//m_system.accept(bvs);
//				@Override
//				public void accept(double[] bestValues) {
//					for(int i = 0; i < getParameterCount(); ++i){
//							
//					}
//					
//					
//				}
				
			}

			
			
		});
		m_startButton.setToolTipText("Start & Stop sovler.");
		clearButton.setToolTipText("Clear the state of the solver. Also stops it if it's running.");
		acceptButton.setToolTipText("Accepts the current best values.");
		closeButton.setToolTipText("Close this dialog.");
		p3.add(m_startButton);
		p3.add(clearButton);
		p3.add(acceptButton);
		p3.add(closeButton);
		
		
		b.add(p3);
		
		add(b);
		updateAutofocusCombo();
		//m_algorithm = new SimpleMinimumSolver(this);
		
		
		
	}
	private void updateAutofocusCombo(){
		
		AutofocusSensor afs = m_autofocusCombo.getItemAt(m_autofocusCombo.getSelectedIndex());
		m_autofocusCombo.removeAllItems();
		
		Collection<OpticsObject> os = m_project.getFlattenedOpticsObjects(false);
		
		for(OpticsObject o : os){
			if(o instanceof AutofocusSensor){
				m_autofocusCombo.addItem((AutofocusSensor)o);
				
			}
		}
		if(afs != null){
			m_autofocusCombo.setSelectedItem(afs);
			
		} else {
			m_autofocusCombo.setSelectedItem(0);
		}
	}
	protected static void updateObjectCombo(JComboBox<ObjectComboItem> jcb, OpticsProject project){
		ObjectComboItem oo = jcb.getItemAt(jcb.getSelectedIndex());
		jcb.removeAllItems();
		
		Collection<OpticsObject> os = project.getFlattenedOpticsObjects(false);
		
		for(OpticsObject o : os){
			jcb.addItem(new ObjectComboItem(o,null));
			jcb.addItem(new ObjectComboItem(o,o.getPositioner()));
		}
		ObjectComboItem soci = null;
		if(oo != null){
			for(int i = 0; i < jcb.getItemCount(); ++i){
				if(oo.equals(jcb.getItemAt(i))){
					soci = jcb.getItemAt(i);
				}
			}
			if(soci != null){
				jcb.setSelectedItem(soci);
			}
		} else {
			jcb.setSelectedItem(0);
		}
	}
	protected static void updatePropertyCombo(JComboBox<ObjectComboItem> ocb, JComboBox<PropertyKey> pcb){
		if(ocb.getItemCount() == 0){
			return;
		}
		PropertyKey pk = pcb.getItemAt(pcb.getSelectedIndex());
		int ioci = ocb.getSelectedIndex();
		if(ioci == -1){
			ioci = 0;
		}
		Collection<PropertyKey> ps = null;
		if(ocb.getItemCount() != 0){
			ps = ocb.getItemAt(ioci).getPropertyKeys();
			pcb.removeAll();
		}
		
		ObjectComboItem oci = ocb.getItemAt(ocb.getSelectedIndex());
		
		Collection<PropertyKey> ps2 = new ArrayList<PropertyKey>();
		for(PropertyKey pkt : ps){
			if(oci.getPropertyOwner().getProperty(pkt) instanceof EquationProperty){
				ps2.add(pkt);
			}
		}
		
		ps = ps2;
		
		for(PropertyKey pkt : ps){
			boolean exists = false;
			for(int i = 0; i < pcb.getItemCount(); ++i){
				if(pcb.getItemAt(i) == pkt){
					exists = true;
					break;
				}
			}
			if(!exists){
				pcb.addItem(pkt);
			}
		}
		int i = 0;
		while(ps.size() < pcb.getItemCount()){
			if(!ps.contains(pcb.getItemAt(i))){
				pcb.removeItemAt(i);
			} else {
				++i;
			}
		}
		if(pk == null){
			pcb.setSelectedIndex(0);
		} else {
			pcb.setSelectedItem(pk);
		}
		
		
	}
	protected class ObjectComboRenderer extends JLabel implements ListCellRenderer<AutofocusSensor> {

		@Override
		public Component getListCellRendererComponent(JList<? extends AutofocusSensor> list, AutofocusSensor value,	int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
	            setBackground(list.getSelectionBackground());
	            setForeground(list.getSelectionForeground());
	        } else {
	            setBackground(list.getBackground());
	            setForeground(list.getForeground());
	        }

			String s = value.getName();
			if(s.equals("")){
				s = value.getID().toHashString();
			}
			setText(value.getName());
				
			
		
			return this;
			
		}
		
	}
	protected static class ObjectItemComboRenderer extends JLabel implements ListCellRenderer<ObjectComboItem> {

		@Override
		public Component getListCellRendererComponent(JList<? extends ObjectComboItem> list, ObjectComboItem value,	int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
	            setBackground(list.getSelectionBackground());
	            setForeground(list.getSelectionForeground());
	        } else {
	            setBackground(list.getBackground());
	            setForeground(list.getForeground());
	        }
			
			setText(value.toString());
	
			return this;
			
		}
		
	}
	
	protected static class ObjectComboItem {
		private final OpticsObject m_object;
		private final ObjectPositioner m_pos;
		ObjectComboItem(OpticsObject oo, ObjectPositioner op){
			if(oo == null){
				throw new RuntimeException("Object is null.");
			}
			m_object = oo;
			m_pos = op;
		}
		public String toString(){
			if(m_pos == null){
				return m_object.getName();
			} else {
				return m_object.getName()+" - Pos";
			}
		}
		public Collection<PropertyKey> getPropertyKeys(){
			if(m_pos == null){
				return m_object.getPropertyKeys();
			} else {
				return m_pos.getPropertyKeys();
			}
		}
		public PropertyOwner getPropertyOwner(){
			if(m_pos == null){
				return m_object;
			} else {
				return m_pos;
			}
		}
		public boolean equals(ObjectComboItem oci){
			return oci.m_object == m_object && oci.m_pos == m_pos;
		}
	}
	private void runAutofocus(){
		if(m_solver.isRunning()){
			m_solver.stop();
			m_startButton.setText("Start");
		} else {
			int n = m_tableModel.getRowCount();
			m_errorSeriesKeys = new String[n];
			m_fitSeriesKeys = new String[n];
			for(int i = 0; i < n; ++i){
				m_errorSeriesKeys[i] = "Error Series "+i;
				m_fitSeriesKeys[i] = "Fit Series "+i;
			}
			m_solver.solve(new AutofocusSystemToSolve(m_tableModel, NUM_PHOTONS_PER_CALC));
			m_startButton.setText("Stop");
		}
	}


	

	



	protected void updateGraph(){
		int n = m_solver.getParameterCount();
		for(int i = 0; i < n; ++i){
			double[] xs = m_solver.getParameterPlotValues(i);
			double[] ys = m_solver.getErrorPlotValues();
			m_graph.updateGraph(m_errorSeriesKeys[i], xs, ys);
			m_graph.setSeriesType(m_errorSeriesKeys[i], GraphType.XY_DOT);
			m_graph.setSeriesDotType(m_errorSeriesKeys[i], DotType.CIRCLE);
			m_graph.setSeriesSize(m_errorSeriesKeys[i], 5.0, 1.0);
			m_graph.updateGraph(m_fitSeriesKeys[i], m_solver.getFitXPlotValues(i), m_solver.getFitYPlotValues());
			m_graph.setSeriesType(m_fitSeriesKeys[i], GraphType.XY_LINE);
			//m_graph.setSeriesDotType(m_fitSeriesKeys[i], DotType.CROSS);
			m_graph.setSeriesSize(m_fitSeriesKeys[i], 5.0, 1.0);
		}
		
		m_graph.autoScale(true, false, true, false);
		m_graph.repaint();
	}

	
	
	
}
