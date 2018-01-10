/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum, Bryan Matthews
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.jocular.actions.ImagerMenuBar;
import net.sourceforge.jocular.actions.ImagerStatusBar;
import net.sourceforge.jocular.actions.ImagerToolBar;
import net.sourceforge.jocular.gui.panel2d.ImagerPanel;
import net.sourceforge.jocular.gui.panel2d.OutputPanel;
import net.sourceforge.jocular.gui.panel2d.SpectroPhotometerPanel;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OutputObject;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.photons.WranglerEvent;
import net.sourceforge.jocular.photons.WranglerListener;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedEvent.UpdateType;
import net.sourceforge.jocular.project.ProjectUpdatedListener;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.settings.SettingKey;
import net.sourceforge.jocular.settings.Settings;

@SuppressWarnings("serial")
public class ImagerWindow extends JFrame implements ProjectUpdatedListener, WranglerListener{
	static final String MAIN_ICON_PATH = "/net/sourceforge/jocular/icons/jocular_icon48.png";
	
	OpticsProject m_project;
	ArrayList<OutputObject> m_objects;
	
	
	final ImagerMenuBar m_menuBar;
	
	ImagerToolBar m_toolBar;
	ImagerStatusBar m_statusBar;
	JTabbedPane m_tabbedPane;
	//ImagerPanel m_imagerPanel;	
	BufferedImage m_image;
	Jocular m_app;
	ArrayList<OutputPanel> m_outputPanels = new ArrayList<OutputPanel>();
	
	public ImagerWindow(Jocular app){
		
		m_app = app;
		m_menuBar = new ImagerMenuBar();
		m_tabbedPane = new JTabbedPane();
		m_tabbedPane.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent e) {
				updatePanels();
				
			}
			
		});
		
		initializeDisplay();
		
		//m_imagerPanel = new ImagerPanel();
		
		initializePanels();
		initializeToolbar();
		initializeStatusBar();
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setProject(app.getProject());
		
	}
	
	public void setProject(OpticsProject project){
		
		m_project = project;
		m_project.addProjectUpdatedListener(this);
		m_project.getWrangler().addWranglerListener(this);
		
		
		projectUpdated(new ProjectUpdatedEvent(UpdateType.LOAD));
		
	}
	

	@Override
	public void projectUpdated(ProjectUpdatedEvent e) {
		
		if(e.getType() == UpdateType.SAVE) return;
		
		// Update the list of imagers
		m_objects = (ArrayList<OutputObject>) m_project.getOpticsObject().getOutputObjects();
		
		clearTabs();
		m_outputPanels.clear();
		
		for(OpticsObject o : m_objects){
			OutputPanel panel = null;
			if(o instanceof Imager){
				panel = new ImagerPanel((Imager)o);	
			} else if(o instanceof SpectroPhotometer){
				panel = new SpectroPhotometerPanel((SpectroPhotometer)o);
			} else {
				
			}
			
			//panel.setImager(imager);
			if(panel != null){
				m_outputPanels.add(panel);
				m_tabbedPane.insertTab(o.getProperty(PropertyKey.NAME).getDefiningString(), null, panel.getComponent(), null, m_outputPanels.indexOf(panel));
				
			}
		}		
		
	}
	
	
	/**
	 * Initializes the window  
	 */
	private void initializeDisplay(){
			
		int xLoc = Settings.SETTINGS.getImgWinXLoc();
		int yLoc = Settings.SETTINGS.getImgWinYLoc();
		int height = Settings.SETTINGS.getImgWinHeight();
		int width = Settings.SETTINGS.getImgWinWidth();

		setSize(new Dimension(width, height));
		setLocation(xLoc, yLoc);
		
		// Set up display				
		setTitle("jOcular Imager Display");
		setIconImage((new ImageIcon(getClass().getResource(MAIN_ICON_PATH))).getImage());		
		
		setJMenuBar(m_menuBar);
		
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
								
			}
			
		});
	}
	public void resetPanels(){
		for(OutputPanel op : m_outputPanels){
			op.resetPanel();
		}
	}
	public void reset(){
		
		setSize(new Dimension(Settings.DEFAULT_WINDOW_WIDTH, Settings.DEFAULT_WINDOW_HEIGHT));
		// This will place it right next to the main window
		setLocation(Settings.DEFAULT_WIN_X_LOC + Settings.DEFAULT_WINDOW_WIDTH, Settings.DEFAULT_WIN_Y_LOC);
		
	}
	
	public void exit(){
		
		if(isVisible() == false ) return;
		
		Settings.SETTINGS.setSetting(SettingKey.IMG_WIN_X_LOC, String.valueOf(getLocationOnScreen().x));
		Settings.SETTINGS.setSetting(SettingKey.IMG_WIN_Y_LOC, String.valueOf(getLocationOnScreen().y));
		Settings.SETTINGS.setSetting(SettingKey.IMG_WIN_HEIGHT, String.valueOf(getSize().height));
		Settings.SETTINGS.setSetting(SettingKey.IMG_WIN_WIDTH, String.valueOf(getSize().width));
		
		ImagerWindow.this.dispose();
	}
	
	/**
	 * Adds panels to the window
	 * 
	 * @param panels - List of panels to be displayed
	 */
	private void initializePanels(){		
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(m_tabbedPane, BorderLayout.CENTER);
	}


	/**
	 * Adds a toolbar to the window
	 * 
	 * @param actionList - List of actions on the toolbar
	 */
	private void initializeToolbar(){
		m_toolBar = new ImagerToolBar();
		getContentPane().add(m_toolBar, BorderLayout.NORTH);
	}
	
	private void initializeStatusBar(){
		m_statusBar = new ImagerStatusBar();
		getContentPane().add(m_statusBar, BorderLayout.SOUTH);
	}
	
	private void clearTabs(){
		
		m_tabbedPane.removeAll();
	}
	
	/**
	 *
	 * 
	 */
	public void renderDisplay(){
		
		updatePanels();
		updateCount();
		
		m_statusBar.repaint();
	}
	
	
	@Override
	public void wranglingUpdate(WranglerEvent e) {
		switch(e.getType()){
		case FINISHED:
			m_statusBar.setCurrentCount(m_project.getWrangler().getCurrentCount());
			m_statusBar.setStatusToReady();
			break;
		case ONGOING:
			m_statusBar.setTotalCounts(m_project.getWrangler().getTotalCounts());
			break;
		case STARTED:
			m_statusBar.setStatusToRunning();
			m_statusBar.setTotalCounts(m_project.getWrangler().getTotalCounts());
			break;
		}
		renderDisplay();
		
	}

	
	
	private void updatePanels(){
		
		//only update the visible tab
		//for(int i=0; i < m_tabbedPane.getTabCount(); i++){
			//ImagerPanel panel = (ImagerPanel) m_tabbedPane.getComponentAt(i);
			int i = m_tabbedPane.getSelectedIndex();
			if(i != -1){
				m_outputPanels.get(i).updatePanel();
			}
		//}
	}
	public OutputPanel getSelectedPanel(){
		OutputPanel result = null;
		int i = m_tabbedPane.getSelectedIndex();
		if(i != -1){
			result = m_outputPanels.get(i);
		}
		return result;
	}
	private void updateCount(){
		if(m_project.getWrangler().isWrangling()){
			m_statusBar.setCurrentCount(m_project.getWrangler().getCurrentCount());
		}
	}
	public Jocular getApp(){
		return m_app;
	}
	public void zoomIn(){
		getSelectedPanel().zoomIn();
	}
	public void zoomOut(){
		getSelectedPanel().zoomOut();
	}
}

