/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreePath;

import net.sourceforge.jocular.actions.OpticsAction;
import net.sourceforge.jocular.actions.OpticsMenuBar;
import net.sourceforge.jocular.actions.OpticsToolBar;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.gui.panel3d.OpticsObjectPanel3d;
import net.sourceforge.jocular.gui.tables.OpticsObjectTree;
import net.sourceforge.jocular.gui.tables.OpticsPropertyTable;
import net.sourceforge.jocular.gui.tables.OpticsPropertyTableModel;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsObjectKey;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.photons.WranglerEvent;
import net.sourceforge.jocular.photons.WranglerListener;
import net.sourceforge.jocular.positioners.ObjectPositionerKey;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedListener;
import net.sourceforge.jocular.settings.SettingKey;
import net.sourceforge.jocular.settings.Settings;


@SuppressWarnings("serial")
public class JocularWindow extends JFrame implements WranglerListener, ProjectUpdatedListener{
	static final String MAIN_ICON_PATH = "/net/sourceforge/jocular/icons/jocular_icon48.png";
	
	//final OpticsActionList m_actionList;
	//final ArrayList<OpticsObjectPanel> m_objectPanels;
	private OpticsObjectPanel3d m_3dPanel;
	final OpticsPropertyTableModel m_objectPropertyTableModel = new OpticsPropertyTableModel();
	
	final OpticsPropertyTableModel m_positionerPropertyTableModel = new OpticsPropertyTableModel();
	//final OpticsObjectTreeModel m_objectTreeModel = new OpticsObjectTreeModel();
	final OpticsMenuBar m_menuBar;
	final JTable m_objectPropertyTable = new OpticsPropertyTable(m_objectPropertyTableModel);
	final JTable m_positionerPropertyTable =new OpticsPropertyTable(m_positionerPropertyTableModel);
	final OpticsObjectTree m_tree;
	OpticsObject m_selectedObject = null;
	final JComboBox<?> m_positionerCombo;
	final Jocular m_app;
	final JSplitPane mainSplitPane;
	final JSplitPane treeSplitPane;
	final JSplitPane positionerSplitPane;
	final JSplitPane geometrySplitPane;
	
	OpticsToolBar m_toolBar;

	public JocularWindow(Jocular app){
		super();
		
		m_app = app;
		m_tree = new OpticsObjectTree(app);
		//m_actionList = new OpticsActionList(app);
		//m_actionList.addKeyBindings(getRootPane());
		OpticsAction.addKeyBindings(getRootPane());
		m_menuBar = new OpticsMenuBar();
		
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		treeSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		positionerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		geometrySplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		//m_objectPanels = new ArrayList<OpticsObjectPanel>();
//		m_objectPanels.add(new OpticsObjectPanel(OpticsObjectPanel.Plane.XY_PLANE));
//		m_objectPanels.add(new OpticsObjectPanel(OpticsObjectPanel.Plane.ZY_PLANE));
//		m_objectPanels.add(new OpticsObjectPanel(OpticsObjectPanel.Plane.ZX_PLANE));		
		m_3dPanel = new OpticsObjectPanel3d();
		m_positionerCombo = new JComboBox<Enum<?>>(ObjectPositionerKey.values());
		m_positionerCombo.setEnabled(false);
		m_positionerCombo.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				TableCellEditor tce = m_positionerPropertyTable.getCellEditor();
				if(tce != null){
					tce.stopCellEditing();
			
				}
				TreePath tp = m_tree.getLeadSelectionPath();
				if(tp == null || m_selectedObject == null){
					return;
				}
											
				ObjectPositionerKey k = (ObjectPositionerKey)m_positionerCombo.getSelectedItem();
				
				// bam - 121013 - This is here to prevent multiple edits from being added when the positioner
				//  hasn't actually changed.
				if(k == ObjectPositionerKey.getKey(m_selectedObject.getPositioner())){
					// bam - 121013 - This line still appears to be needed
					m_positionerPropertyTableModel.setPropertyOwner(m_selectedObject.getPositioner(), m_app.getProject());
					return;
				}
				
				OpticsProject p = m_app.getProject();
				//p.addAndDoEdit(new PositionerEdit(m_selectedObject, k, p));
				p.addPositionerEdit(m_selectedObject, k);
				if(m_selectedObject != null){
					m_positionerPropertyTableModel.setPropertyOwner(m_selectedObject.getPositioner(), m_app.getProject());
				}
				
			}
			
		});
		setProject(m_app.getProject());
		//Initialize and display window
		initializeDisplay();
		initializePanels();
		initializeToolbar();
		
		setVisible(true);	
		
		
	}
	
	public void setProject(OpticsProject p){
		
//		for(OpticsObjectPanel oop : m_objectPanels){
//			oop.setProject(p);
//		}
		
		m_3dPanel.setProject(p);

		m_tree.setProject(p);
		updateTitle(p);
		setCalcPhotonsEnabled(true);
		
		p.addProjectUpdatedListener(this);
		p.getWrangler().addWranglerListener(this);
	}
	
	public void updateTitle(OpticsProject p){
		
		String projectName = "";
		if(p != null){
			
			projectName += " - ";			
			
			String n = p.getFileName();
			if(n.length() > 0){
				projectName += p.getFileName();
			}
			
			if(p.isDirty()){
				projectName += "*";
			}
		}
		setTitle("jOcular" +projectName);
	}
	
	public void zoomIn(){
//		for(OpticsObjectPanel panel : m_objectPanels){
//			panel.zoomIn();
//		}
		m_3dPanel.zoomIn();
	}
	
	public void zoomOut(){
//		for(OpticsObjectPanel panel : m_objectPanels){
//			panel.zoomOut();
//		}	
		m_3dPanel.zoomOut();	
	}
	

	
	/**
	 * Initializes the window  
	 */
	private void initializeDisplay(){
		
		int xLoc = Settings.SETTINGS.getMainWinXLoc();
		int yLoc = Settings.SETTINGS.getMainWinYLoc();
		int height = Settings.SETTINGS.getMainWinHeight();
		int width = Settings.SETTINGS.getMainWinWidth();

		setSize(new Dimension(width, height));
		setLocation(xLoc, yLoc);
		
		// Set up display
		String revision = Jocular.getFormattedVersion();		
		setTitle("jOcular " + revision);
		setIconImage((new ImageIcon(getClass().getResource(MAIN_ICON_PATH))).getImage());		
		
		setJMenuBar(m_menuBar);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
								
			}
			
		});
	}
	
	// These are all handled in the main window
	//  Need to make sure the SplitPanes do not grab the action first.
	private void disableKeyHandlers(JComponent component){
		
		KeyStroke ks;
		
		ks = KeyStroke.getKeyStroke("control C");
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(ks, "disableCopy");
		component.getActionMap().put("disableCopy", null);
		
		ks = KeyStroke.getKeyStroke("control X");
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(ks, "disableCut");
		component.getActionMap().put("disableCut", null);
		
		ks = KeyStroke.getKeyStroke("control V");
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(ks, "disablePaste");
		component.getActionMap().put("disablePaste", null);
		
		ks = KeyStroke.getKeyStroke("DELETE");
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(ks, "disableDelete");
		component.getActionMap().put("disableDelete", null);
		
		ks = KeyStroke.getKeyStroke("F8");
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(ks, "disableF8");
		component.getActionMap().put("disableF8", null);
	}
	
	//private JPanel m_2DViewPanels = new JPanel();
	private void initializeGeometryView(){
//
//		m_2DViewPanels = new JPanel(new CardLayout());
//		CardLayout cl = (CardLayout)(m_2DViewPanels.getLayout());
//				
//		for(OpticsObjectPanel oop : m_objectPanels){
//			m_2DViewPanels.add(oop);
//			cl.addLayoutComponent(oop, ("panel" + String.valueOf(oop.getPlane())));
//		}
		
		//setViewPlane(OpticsObjectPanel.Plane.ZX_PLANE);
		set3DViewPlane(OpticsObjectPanel.Plane.ZX_PLANE);
		
		//geometrySplitPane.add(m_2DViewPanels);
		geometrySplitPane.add(m_3dPanel);
		
		Dimension d = getSize();
		int y = d.height/2;
		
		int dividerLocation = Settings.SETTINGS.getMainWinSplitGeometry();
		
		if (dividerLocation == 0){
			geometrySplitPane.setDividerLocation(y);
		} else{
			geometrySplitPane.setDividerLocation(dividerLocation);
		}
	}
	
//	public void setViewPlane(OpticsObjectPanel.Plane plane){
//			
//		CardLayout cl = (CardLayout)(m_2DViewPanels.getLayout());
//		cl.show(m_2DViewPanels, ("panel" + String.valueOf(plane)));
//		
//		
//	}
	
	public void set3DViewPlane(OpticsObjectPanel.Plane plane){
		m_3dPanel.setViewPlane(plane);
	}
	public void set3DClipPlane(Plane plane) {
		m_3dPanel.setClipPLane(plane);
		
	}
	/**
	 * Adds panels to the window
	 * 
	 * @param panels - List of panels to be displayed
	 */
	private void initializePanels(){		
		
		getContentPane().setLayout(new BorderLayout());
		
		initializeGeometryView();
		
		initializeTreeView();		
				
		mainSplitPane.add(treeSplitPane);
		mainSplitPane.add(geometrySplitPane);

		int mainDividerLocation = Settings.SETTINGS.getMainWinSplitHorizontal();
		
		if (mainDividerLocation != 0){
			mainSplitPane.setDividerLocation(mainDividerLocation);
		}
		
		getContentPane().add(mainSplitPane, BorderLayout.CENTER);
	
		validate();
		
		disableKeyHandlers(mainSplitPane);
		disableKeyHandlers(treeSplitPane);
		disableKeyHandlers(positionerSplitPane);
	}
	
	private void initializeTreeView(){
		m_tree.addTreeSelectionListener(new TreeSelectionListener(){

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				
				// Complete editing on old selection
				if(m_positionerPropertyTable.isEditing()){
					 TableCellEditor cellEditor = m_positionerPropertyTable.getCellEditor(m_positionerPropertyTable.getEditingRow(), m_positionerPropertyTable.getEditingColumn());
					 cellEditor.stopCellEditing();
				}
				// Complete editing on old selection
				if(m_objectPropertyTable.isEditing()){
					 TableCellEditor cellEditor = m_objectPropertyTable.getCellEditor(m_objectPropertyTable.getEditingRow(), m_objectPropertyTable.getEditingColumn());
					 cellEditor.stopCellEditing();
				}
				// Get new selection
				TreePath tp = e.getNewLeadSelectionPath();
				if(tp != null && tp.getLastPathComponent() instanceof OpticsObject){
					m_selectedObject = (OpticsObject)(tp.getLastPathComponent());
					m_objectPropertyTableModel.setPropertyOwner(m_selectedObject, m_app.getProject());
//					
					if(m_selectedObject == null){
						m_positionerCombo.setEnabled(false);
					} else {
						m_positionerCombo.setSelectedItem(ObjectPositionerKey.getKey(m_selectedObject.getPositioner()));
						m_positionerCombo.setEnabled(true);
					}
					
					if(m_selectedObject != null){
						m_positionerPropertyTableModel.setPropertyOwner(m_selectedObject.getPositioner(), m_app.getProject());
					}
					
				} else {
					m_selectedObject = null;
					m_objectPropertyTableModel.setPropertyOwner(null, m_app.getProject());
					m_positionerPropertyTableModel.setPropertyOwner(null, m_app.getProject());
				}
				
				
				
			}
			
		});
		
		initializePropertyPanels();
		
		treeSplitPane.add(new JScrollPane(m_tree));
		treeSplitPane.add(positionerSplitPane);
		
		Dimension d = getSize();
		int y = d.height*2/7;
		
		int treeDividerLocation = Settings.SETTINGS.getMainWinSplitTree();
		
		if (treeDividerLocation == 0){
			treeSplitPane.setDividerLocation(y);
		} else{
			treeSplitPane.setDividerLocation(treeDividerLocation);
		}
	}

	private void initializePropertyPanels(){
		
		JPanel posPanel = new JPanel(new GridLayout(1,2));		
		posPanel.add(new JLabel("Positioner"));
		posPanel.add(m_positionerCombo);
		posPanel.setMaximumSize(new Dimension(500,30));
		
		Box b2 = new Box(BoxLayout.Y_AXIS);
		b2.add(posPanel);		
		b2.add(new JScrollPane(m_positionerPropertyTable));
		
		positionerSplitPane.add(new JScrollPane(m_objectPropertyTable));
		positionerSplitPane.add(b2);
		
		Dimension d = getSize();
		int y = d.height*2/7;
		
		int positionerDividerLocation = Settings.SETTINGS.getMainWinSplitPositioner();
		
		if (positionerDividerLocation == 0){
			positionerSplitPane.setDividerLocation(y);
		} else {
			positionerSplitPane.setDividerLocation(positionerDividerLocation);
		}
	}

	/**
	 * Adds a toolbar to the window
	 * 
	 * @param actionList - List of actions on the toolbar
	 */
	private void initializeToolbar(){
		m_toolBar = new OpticsToolBar(m_app);
		getContentPane().add(m_toolBar, BorderLayout.NORTH);
	}
	
	public void reset(){
		
		setSize(new Dimension(Settings.DEFAULT_WINDOW_WIDTH, Settings.DEFAULT_WINDOW_HEIGHT));
		setLocation(Settings.DEFAULT_WIN_X_LOC, Settings.DEFAULT_WIN_Y_LOC);

	}
	
	public void exit(){
		
		if(m_app.getProject().isDirty()){
			Object[] options = {"Discard", "Cancel"};
			int result = JOptionPane.showOptionDialog(this, "Project has unsaved Changes! Click Discard to exit anyway or cancel to go back and save.", "Discard Changes?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		
			if(result != 0){
				return;
			}
		}
		
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_X_LOC, String.valueOf(getLocationOnScreen().x));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_Y_LOC, String.valueOf(getLocationOnScreen().y));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_HEIGHT, String.valueOf(getSize().height));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_WIDTH, String.valueOf(getSize().width));
		
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_SPLIT_HORIZ, String.valueOf(mainSplitPane.getDividerLocation()));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_SPLIT_TREE, String.valueOf(treeSplitPane.getDividerLocation()));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_SPLIT_POS, String.valueOf(positionerSplitPane.getDividerLocation()));
		Settings.SETTINGS.setSetting(SettingKey.MAIN_WIN_SPLIT_GEO, String.valueOf(geometrySplitPane.getDividerLocation()));
		
		m_app.exit();
		dispose();
		
		System.exit(0);
	}
	
	public void setCalcPhotonsEnabled(boolean f){
		OpticsAction.CALC_PHOTONS.setEnabled(f);
	}
	


	public void setTableSplitRatio(){
		
		Dimension d = getSize();
		int y = d.height*2/7;
		positionerSplitPane.setDividerLocation(y);
		treeSplitPane.setDividerLocation(y);
		System.out.println("JocularWindow.setTableSplitRatio jsp1,jsp2 divider: "+treeSplitPane.getDividerLocation()+", "+positionerSplitPane.getDividerLocation());
		
	}
	/**
	 * Renders all of the panels, trajectories and optics
	 * 
	 */
	protected void renderDisplay(){
		
//		for(OpticsObjectPanel panel : m_objectPanels){
//			panel.repaint();
//		}	
		//m_3dPanel.updateEverything();
		m_3dPanel.updateDisplay();
		repaint();
		
	}
	public void updateEverything(){
		m_3dPanel.updateEverything();
	}
	
	protected OpticsObject getSelectedObject(){
		TreePath path = m_tree.getSelectionPath();
		if(path == null){
			System.out.println("JocularWindows nothing selected.");
			return null;
		}
		return (OpticsObject)path.getLastPathComponent();
	}
	
	protected OpticsObject[] getSelectedObjects(){
		ArrayList<OpticsObject> os = new ArrayList<OpticsObject>();
		TreePath[] paths = m_tree.getSelectionPaths();
		for(TreePath tp : paths){
			os.add((OpticsObject)tp.getLastPathComponent());
		}
		OpticsObject[] result = new OpticsObject[os.size()];
		return os.toArray(result);
	}
	
	protected OpticsObjectGroup getClosestGroup(){
		TreePath path = m_tree.getSelectionPath();
		if(path == null){
			System.out.println("JocularWindows nothing selected.");
			return  m_app.getProject().getOpticsObject();
		}

		//get closest group
		OpticsObjectGroup g = m_app.getProject().getOpticsObject();
		for(int i = path.getPathCount() - 1; i >= 0; --i){
			Object o = path.getPathComponent(i);
			if(o instanceof OpticsObjectGroup && !(o instanceof OpticsPart)){
				g = (OpticsObjectGroup)path.getPathComponent(i);
				break;
			}
		}
		return g;
	}
	
	public void addObject(OpticsObject oToAdd){
		OpticsObject o = getSelectedObject();
		OpticsObjectGroup g = getClosestGroup();
		int pos;
		if(o != null && g != null){
			pos = g.getPos(o)+1;
		} else {
			pos = g.getObjects().size();
		}
		//m_app.getProject().addAndDoEdit(new OpticsObjectEdit(getClosestGroup(), oToAdd, pos,  null, m_app.getProject()));
		m_app.getProject().addOpticsObjectEdit(getClosestGroup(), oToAdd, pos, null);
	}
	
	public void addObject(OpticsObjectKey objectKeyToAdd){
		//m_tree.addObject(m_app);
		
		
		
		OpticsObjectKey key;
		if(objectKeyToAdd != null){
			key = objectKeyToAdd;
		} else {
			key = (OpticsObjectKey)JOptionPane.showInputDialog(this, "Select Object to add", "Add object", JOptionPane.QUESTION_MESSAGE, null, OpticsObjectKey.values(), OpticsObjectKey.OPTICS_GROUP);
		}
		
		
		if(key != null){
			addObject(key.getNewObject());
		}
	}
	public void moveInTree(boolean upNotDown){
		OpticsObject o = getSelectedObject();
		
		if(o == null){
			return;
		}
		
		TreePath path = m_tree.getSelectionPath().getParentPath();
		Object p = path.getLastPathComponent();
		if(!(p instanceof OpticsObjectGroup)){
			return;
		}
		OpticsObjectGroup g = (OpticsObjectGroup)p;
		if(o == null || g == null){
			return;
		}
		//m_app.getProject().addAndDoEdit(new OpticsMoveEdit(m_app.getProject(), g, o, upNotDown));
		m_app.getProject().addObjectMoveEdit(g, o, upNotDown);
		
	}
	
	public void deleteObject(){
		OpticsObject o = getSelectedObject();
		
		if(o == null){
			return;
		}
		
		TreePath path = m_tree.getSelectionPath().getParentPath();
		Object p = path.getLastPathComponent();
		if(!(p instanceof OpticsObjectGroup)){
			return;
		}
		OpticsObjectGroup g = (OpticsObjectGroup)p;
		if(o == null || g == null){
			return;
		}
		//m_app.getProject().addAndDoEdit(new OpticsObjectEdit(g , null, -1, o, m_app.getProject()));
		m_app.getProject().addOpticsObjectEdit(g, null, -1, o);
	}


	

	@Override
	public void wranglingUpdate(WranglerEvent e) {
		switch(e.getType()){
		case FINISHED:
			renderDisplay();
			setCalcPhotonsEnabled(true);
			break;
		case ONGOING:
			renderDisplay();
			break;
		case STARTED:
			break;
		}
		//m_3dPanel.updatePhotons();
		
	}

	@Override
	public void projectUpdated(ProjectUpdatedEvent e) {
		// bam-121013-This could happen because a new type of positioner has been selected.
		//  Especially through an undo or redo event.  Need to update the selected positioner.
		switch(e.getType()){
		case POSITION:
			
			if(m_selectedObject == e.getOpticsObject()){

				// bam - 121013 - Only update if the positioner itself has changed
				if(m_positionerCombo.getSelectedIndex() == -1 || m_positionerCombo.getSelectedItem() != ObjectPositionerKey.getKey(m_selectedObject.getPositioner())){
					m_positionerCombo.setSelectedItem(ObjectPositionerKey.getKey(m_selectedObject.getPositioner()));
				}				
			}			
			break;
		case ADD:
		case CHANGE:
		case LOAD:
		case REMOVE:
			updateEverything();
			break;
		}		
	}

	public void stopEditing() {
		TableCellEditor tce = m_objectPropertyTable.getCellEditor();
		if(tce != null){
			tce.cancelCellEditing();
		}
		tce = m_positionerPropertyTable.getCellEditor();
		if(tce != null){
			tce.cancelCellEditing();
		}
		
	}
	public Jocular getApp(){
		return m_app;
	}

	public void test() {
		m_3dPanel.test();
		
	}

	
}

