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
package net.sourceforge.jocular.splines;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import net.sourceforge.jocular.graphs.GraphPanel;
import net.sourceforge.jocular.graphs.GraphPanel.DotType;
import net.sourceforge.jocular.graphs.GraphPanel.GraphType;
import net.sourceforge.jocular.gui.EquationField;
import net.sourceforge.jocular.math.Complex;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.ArrayProperty;
import net.sourceforge.jocular.properties.EnumArrayProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.undo.MultiEdit;
import net.sourceforge.jocular.undo.OpticsEdit;
import net.sourceforge.jocular.undo.PropertyEdit;

public class SplinePointDialog extends JDialog {

	protected static final String SPLINE_SERIES = "Spline";
	protected static final String POINT_SERIES = "Points";
	protected static final String SELECTED_POINT_SERIES = "Selected Point";
	protected static final String SPLINE_POINT_SERIES = "Spline Points";
	

	private OpticsProject m_project;
	private GraphPanel m_graph;
	private TableModel m_model;
	
	
	
	//private JButton m_fitButton;
	private String[] m_errorSeriesKeys;
	private String[] m_fitSeriesKeys;
	private JTable m_table;
	private SplineObject m_object;

	private EquationField m_simplifyField;


	public SplinePointDialog(Frame owner, OpticsProject project, SplineObject object){
		super(owner,"Edit Spline Points");
		m_project = project;
		m_object = object;
		m_object.addPropertyUpdatedListener(new PropertyUpdatedListener(){
			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				updateGraph();
				
			}
			
		});
		 
		 
		
		

		m_simplifyField = new EquationField(object, PropertyKey.SIMPLIFY_THRESHOLD, m_project);
		m_graph = new GraphPanel();
		m_graph.setGraphType(GraphType.XY_DOT);
		//m_graph.updateGraph("null", new double[] {0,1}, new double[] {0,1});
		m_graph.autoScale(true, false, true, false);
		m_graph.setPreferredSize(new Dimension(400,400));
		

		

		
		setSize(new Dimension(500,400));
		Box b = Box.createVerticalBox();
		b.add(m_graph);
		//b.add(Box.createVerticalGlue());
		
		
		m_table = new JTable(new SplinePointTableModel(m_object, m_project));
		SplineTableCellRenderer stcr = new SplineTableCellRenderer();
		SplinePointCellEditor stce = new SplinePointCellEditor();
//		AutofocusParameterCellEditor apce = new AutofocusParameterCellEditor(m_project, m_tableModel);
		m_table.setDefaultRenderer(EnumArrayProperty.class, stcr);
		m_table.setDefaultRenderer(EquationArrayProperty.class, stcr);
//		m_table.setDefaultRenderer(EquationArrayProperty.class, apcr);
		m_table.setDefaultEditor(EnumArrayProperty.class, stce);
		m_table.setDefaultEditor(EquationArrayProperty.class, stce);
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
				updateGraph();
				
			}
			
		});
		//b.add(m_table);
		b.add(new JScrollPane(m_table));
		//JPanel p4 = new JPanel(new GridLayout(1,4));
		Box p5 = Box.createHorizontalBox();
		b.add(p5);
		p5.add(new JLabel("Simplify Threshold"));
		p5.add(m_simplifyField);
		Box p4 = Box.createHorizontalBox();
		b.add(p4);
		JButton addRowButton = new JButton("Add point");
		addRowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = m_table.getSelectedRow();
				if(i < 0){
					i = m_object.getSplinePointCount();
				}
//				m_object.addRowToArrayProperty(PropertyKey.POINTS_X, i);
//				m_object.addRowToArrayProperty(PropertyKey.POINTS_Y, i);
//				m_object.addRowToArrayProperty(PropertyKey.POINTS_TYPES, i);
				//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_X, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_X))).addRowToDefiningString(i));
				//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_Y, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_Y))).addRowToDefiningString(i));
				//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_TYPES, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_TYPES))).addRowToDefiningString(i));
				m_project.addAndDoEdit(new MultiEdit(m_project, new OpticsEdit[] {new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_X,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_X))).addRowToDefiningString(i)),
						new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_Y,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_Y))).addRowToDefiningString(i)),
						new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_TYPES,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_TYPES))).addRowToDefiningString(i))}));
			}
			
		});
		JButton deleteRowButton = new JButton("Delete point");
		deleteRowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = m_table.getSelectedRow();
				if(i < 0){
					JOptionPane.showMessageDialog(SplinePointDialog.this, "Must select row in order to delete.");
				} else {
//					m_object.removeRowFromArrayProperty(PropertyKey.POINTS_X, i);
//					m_object.removeRowFromArrayProperty(PropertyKey.POINTS_Y, i);
//					m_object.removeRowFromArrayProperty(PropertyKey.POINTS_TYPES, i);
					//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_X, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_X))).removeRowFromDefiningString(i));
					//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_Y, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_Y))).removeRowFromDefiningString(i));
					//m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_TYPES, ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_TYPES))).removeRowFromDefiningString(i));
					m_project.addAndDoEdit(new MultiEdit(m_project, new OpticsEdit[] {new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_X,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_X))).removeRowFromDefiningString(i)),
							new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_Y,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_Y))).removeRowFromDefiningString(i)),
							new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_TYPES,  ((ArrayProperty)(m_object.getProperty(PropertyKey.POINTS_TYPES))).removeRowFromDefiningString(i))}));
				}
			}
		});
		JButton pasteTableButton = new JButton("Paste Table");
		pasteTableButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				getTableFromClipboard();
			}
		});
		p4.add(addRowButton);
		p4.add(deleteRowButton);
		p4.add(pasteTableButton);
		p4.add(new JLabel());
		p4.add(new JLabel());
		//JPanel p3 = new JPanel();
		Box p3 = Box.createHorizontalBox();
//		m_fitButton = new JButton("Fit");
//		m_fitButton.addActionListener(new ActionListener(){
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				
//				m_object.calcSplineCoefficients();
//				updateGraph();
//			}
//			
//		});
		JButton closeButton = new JButton("Close");
		
		closeButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				//m_solver.reset();
				
				dispose();
				
			}
			
		});
		
		
		
		//m_fitButton.setToolTipText("Start & Stop sovler.");
		
		closeButton.setToolTipText("Close this dialog.");
		//p3.add(m_fitButton);
		
		p3.add(closeButton);
		
		
		b.add(p3);
		
		add(b);
	

		updateGraph();
	}

	

	



	protected void getTableFromClipboard() {
		String ts = "";
		boolean failed = false;
		try{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents = clipboard.getContents(null);
			if(contents.isDataFlavorSupported(DataFlavor.stringFlavor)){
				ts = (String)contents.getTransferData(DataFlavor.stringFlavor);
			}
		} catch(UnsupportedFlavorException ufe){
			ufe.printStackTrace();
			failed = true;
		} catch(IOException ioe){
			ioe.printStackTrace();
			failed = true;
		}
		if(failed){
			return;
		}
	    System.out.println("SplinePointDialog.getTableFromClipboard() "+ts);
	    
	    String[] rows = ts.split("\n");
	    String[] table = new String[] {"", "", ""};
	    boolean firstRow = true;
	    for(int r = 0; r < rows.length; ++r){
	    	String[] cs = rows[r].split("\t");
	    	if(cs.length < 2){
	    		failed = true;
	    		break;
	    	}
	    	if(!firstRow){
	    		table[0] += EquationArrayProperty.SEPARATOR;
	    		table[1] += EquationArrayProperty.SEPARATOR;
	    		table[2] += EnumArrayProperty.SEPARATOR;
	    		firstRow = false;
	    	}
	    	firstRow = false;
	    	table[0] += cs[0];
	    	table[1] += cs[1];
	    	table[2] += SplineObject.PointType.SMOOTH.name();
	    	
	    }
	    if(failed){
			return;
		}
	    
//	    m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_X, table[0]);
//	    m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_Y, table[1]);
//	    m_project.addPropertyEdit(m_object,  PropertyKey.POINTS_TYPES, table[2]);
		m_project.addAndDoEdit(new MultiEdit(m_project, new OpticsEdit[] {new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_X,  table[0]),
				new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_Y,  table[1]),
				new PropertyEdit(m_project, m_object,  PropertyKey.POINTS_TYPES,  table[2])}));
	}







	protected void updateGraph(){
		//TODO update graph data points
		double[] xs = m_object.getSplinePointIndepValues();
		double[] ys = m_object.getSplinePointDepValues();
		m_graph.updateGraph(POINT_SERIES, xs, ys);
		m_graph.setSeriesSize(POINT_SERIES, 5.0, 1.0);
		m_graph.setSeriesType(POINT_SERIES, GraphType.XY_DOT);
		m_graph.setSeriesDotType(POINT_SERIES, DotType.CIRCLE);
		m_graph.setSeriesColor(POINT_SERIES, Color.GREEN);
		double[][] sps = SplineMath.calcSplinePoints(m_object.getSplineCoefficients(),40);
		if(sps != null){
			m_graph.updateGraph(SPLINE_SERIES, sps[0], sps[1]);
			m_graph.setSeriesSize(SPLINE_SERIES,5.0, 1.0);
			m_graph.setSeriesType(SPLINE_SERIES, GraphType.XY_CONTINUOUS);
			m_graph.setSeriesColor(SPLINE_SERIES, Color.RED);
			//m_graph.setSeriesDotType(SPLINE_SERIES, DotType.CROSS);
		}
		sps = SplineMath.calcSplinePoints(m_object.getSplineCoefficients(),1);
		if(sps != null){
			m_graph.updateGraph(SPLINE_POINT_SERIES, sps[0], sps[1]);
			m_graph.setSeriesSize(SPLINE_POINT_SERIES,5.0, 1.0);
			m_graph.setSeriesType(SPLINE_POINT_SERIES, GraphType.XY_DOT);
			m_graph.setSeriesColor(SPLINE_POINT_SERIES, Color.RED);
			m_graph.setSeriesDotType(SPLINE_POINT_SERIES, DotType.CROSS);
		}
		
		
		
		//m_object.calcSplineCoefficients();
		double[][] pts = getSelectedPoints();
		m_graph.updateGraph(SELECTED_POINT_SERIES, pts[0], pts[1]);
		m_graph.setSeriesColor(SELECTED_POINT_SERIES, Color.MAGENTA);
		m_graph.setSeriesType(SELECTED_POINT_SERIES, GraphType.XY_DOT);
		m_graph.setSeriesDotType(SELECTED_POINT_SERIES, DotType.SQUARE);
		m_graph.setSeriesSize(SELECTED_POINT_SERIES,10.0, 10.0);
		m_graph.autoScale(true, false, true, false);
		m_graph.repaint();
	}







	private double[][] getSelectedPoints() {
		int[] sr = m_table.getSelectedRows();
		int n = sr.length;
		ArrayList<Complex> cs = new ArrayList<Complex>();
		EquationArrayProperty ex = (EquationArrayProperty)(m_object.getProperty(PropertyKey.POINTS_X));
		EquationArrayProperty ey = (EquationArrayProperty)(m_object.getProperty(PropertyKey.POINTS_Y));;
		for(int i = 0; i < n; ++i){
			int j = sr[i];
			if(j < n){
				cs.add(new Complex(ex.getValue()[j].getBaseUnitValue(), ey.getValue()[j].getBaseUnitValue()));
			} 
		}
		double[][] result = new double[2][cs.size()];
		for(int i = 0; i < cs.size(); ++i){
			Complex c = cs.get(i);
			result[0][i] = c.real();
			result[1][i] = c.imag();
		}
		return result;
	}

	
	
	
}
