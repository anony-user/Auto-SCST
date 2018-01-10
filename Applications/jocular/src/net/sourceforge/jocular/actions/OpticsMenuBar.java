/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.actions;

import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

@SuppressWarnings("serial")
public class OpticsMenuBar extends JMenuBar {

	private final JMenu toolsMenu = new JMenu("Tools");
	private final JMenu viewMenu = new JMenu("View");
	private final JMenu fileMenu = new JMenu("File");
	private final JMenu editMenu = new JMenu("Edit");
	private final JMenu simMenu = new JMenu("Sim");
	private final JMenu helpMenu = new JMenu("Help");
	
	public OpticsMenuBar(){
		
		fileMenu.setMnemonic(KeyEvent.VK_F);
		editMenu.setMnemonic(KeyEvent.VK_E);
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		viewMenu.setMnemonic(KeyEvent.VK_V);
		simMenu.setMnemonic(KeyEvent.VK_S);
		helpMenu.setMnemonic(KeyEvent.VK_H);
		
		fileMenu.add(OpticsAction.LOAD);
		fileMenu.add(OpticsAction.SAVE);
		fileMenu.add(OpticsAction.SAVE_AS);
		fileMenu.add(OpticsAction.NEW);
		fileMenu.add(OpticsAction.PRINT);
		fileMenu.add(OpticsAction.CAPTURE_SCREEN);
		fileMenu.add(OpticsAction.EXIT);
		
		
		
		
		
		editMenu.add(OpticsAction.UNDO);
		editMenu.add(OpticsAction.REDO);
		editMenu.addSeparator();
		editMenu.add(OpticsAction.CUT);
		editMenu.add(OpticsAction.COPY);
		editMenu.add(OpticsAction.PASTE);
		editMenu.add(OpticsAction.DELETE_OBJECT);
		editMenu.addSeparator();
		editMenu.add(makeAddSubMenu());
		
		
		viewMenu.add(OpticsAction.ZOOM_IN);
		viewMenu.add(OpticsAction.ZOOM_OUT);
		viewMenu.add(getViewPlaneSubMenu());
		viewMenu.add(getClipPlaneSubMenu());
		viewMenu.addSeparator();
		viewMenu.add(OpticsAction.SIZE_SPLITS);
		viewMenu.add(OpticsAction.RESET_WINDOWS);
		
		
		simMenu.add(OpticsAction.OPEN_IMAGER_WINDOW);
		simMenu.add(OpticsAction.CALC_PHOTONS);
		simMenu.add(OpticsAction.CALC_DISPLAY_PHOTONS);
		simMenu.add(OpticsAction.STOP_WRANGLER);

		
		
		//toolsMenu.add(cal.get(OpticsActionKeys.AUTOFOCUS));
		toolsMenu.add(OpticsAction.SETTINGS);
		toolsMenu.add(OpticsAction.COPY_TRAJECTORY);
		toolsMenu.add(OpticsAction.EXPORT_MATERIALS);
		
		helpMenu.add(OpticsAction.HELP);
		helpMenu.add(OpticsAction.WEBSITE);
		helpMenu.add(OpticsAction.ABOUT);
		
		add(fileMenu);
		add(editMenu);
		add(viewMenu);
		add(simMenu);
		add(toolsMenu);
		add(helpMenu);		
	}
	
	private JMenu getViewPlaneSubMenu(){
		JMenu panel1Menu = new JMenu("Select View Plane");
		JRadioButtonMenuItem panelXY = new JRadioButtonMenuItem(OpticsAction.SET_VIEW_PLANE_TO_XY);
		JRadioButtonMenuItem panelZY = new JRadioButtonMenuItem(OpticsAction.SET_VIEW_PLANE_TO_ZY);
		JRadioButtonMenuItem panelZX = new JRadioButtonMenuItem(OpticsAction.SET_VIEW_PLANE_TO_ZX);
		
		// This needs to match the selection in JocularWindow.initializeObjectPanels()
		panelZY.setSelected(true);
		
		ButtonGroup panel1Group = new ButtonGroup();
		panel1Group.add(panelZY);
		panel1Group.add(panelZX);
		panel1Group.add(panelXY);
		
		panel1Menu.add(panelZY);
		panel1Menu.add(panelZX);
		panel1Menu.add(panelXY);
		
		return panel1Menu;
	}
	private JMenu getClipPlaneSubMenu(){
		JMenu clipMenu = new JMenu("Select Clip Plane...");
		JRadioButtonMenuItem rbN = new JRadioButtonMenuItem(OpticsAction.SET_CLIP_PLANE_TO_NONE);
		JRadioButtonMenuItem rbXY = new JRadioButtonMenuItem(OpticsAction.SET_CLIP_PLANE_TO_XY);
		JRadioButtonMenuItem rbZX = new JRadioButtonMenuItem(OpticsAction.SET_CLIP_PLANE_TO_ZX);
		JRadioButtonMenuItem rbZY = new JRadioButtonMenuItem(OpticsAction.SET_CLIP_PLANE_TO_ZY);
		
		// This needs to match the selection in JocularWindow.initializeObjectPanels()
		rbN.setSelected(true);
		
		ButtonGroup clipGroup = new ButtonGroup();
		clipGroup.add(rbN);
		
		clipGroup.add(rbZY);
		clipGroup.add(rbZX);
		clipGroup.add(rbXY);
		
		clipMenu.add(rbN);
		clipMenu.add(rbZY);
		clipMenu.add(rbZX);
		clipMenu.add(rbXY);
		
		return clipMenu;
	}
	protected static JMenu makeAddSubMenu(){
		JMenu addMenu = new JMenu("Add...");
		addMenu.add(new JMenuItem(OpticsAction.ADD_SPHERICAL_LENS));
		addMenu.add(new JMenuItem(OpticsAction.ADD_GROUP));
		addMenu.add(new JMenuItem(OpticsAction.ADD_IMAGER));
		addMenu.add(new JMenuItem(OpticsAction.ADD_POINT_SOURCE));
		addMenu.add(new JMenuItem(OpticsAction.ADD_TRIANGULAR_PRISM));
		addMenu.add(new JMenuItem(OpticsAction.ADD_IMAGE_SOURCE));
		addMenu.add(new JMenuItem(OpticsAction.ADD_SIMPLE_APERTURE));
		addMenu.add(new JMenuItem(OpticsAction.ADD_SPECTROPHOTOMETER));
		addMenu.add(new JMenuItem(OpticsAction.ADD_AUTOFOCUS_SENSOR));
		addMenu.add(new JMenuItem(OpticsAction.ADD_ROTATED_SPLINE));
		addMenu.add(new JMenuItem(OpticsAction.ADD_EXTRUDED_SPLINE));
		addMenu.add(new JMenuItem(OpticsAction.ADD_PLANO_ASPHERIC_LENS));
		addMenu.add(new JMenuItem(OpticsAction.ADD_PART));
		return addMenu;
	}
}

