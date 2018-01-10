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
package net.sourceforge.jocular.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.sourceforge.jocular.ImagerWindow;
import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.JocularWindow;


public enum OpticsAction implements Action {
		ABOUT("About", null, "jocular_icon24.png.png", "Call up an about dialog.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).about();}*/),
		ADD_AUTOFOCUS_SENSOR("Autofocus Sensor", null, "addObject.png", "Add an autofocus sensor to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.AUTOFOCUS_SENSOR);}*/),
		ADD_GROUP("Group", null, "addObject.png", "Add a group to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.OPTICS_GROUP);}*/),
		ADD_IMAGE_SOURCE("Image Source", null, "addObject.png", "Add an image source to the tree.", KeyEvent.VK_I/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.IMAGE_SOURCE);}*/),
		ADD_IMAGER("Imager", null, "addObject.png", "Add an imager to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.IMAGER);}*/),
		ADD_OBJECT("Add Object", null, "addObject.png", "Add an object to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(null);}*/),
		ADD_PART("Add Part", null, "addObject.png", "Add a project as a sub-part of this project.", KeyEvent.VK_P/*/* , ae -> {getApp(ae).addPart();}*/),
		ADD_PLANO_ASPHERIC_LENS("Add Plano-aspheric Lens", null, "addObject.png", "Add a plano-aspheric lens to the tree.", KeyEvent.VK_P/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.PLANO_ASPHERIC_LENS);}*/),
		ADD_POINT_SOURCE("Point Source", null, "addObject.png", "Add a point source to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.POINT_SOURCE);}*/),
		ADD_ROTATED_SPLINE("Rotated Spline", null, "addObject.png", "Add an object defined by a rotated spline curve.", KeyEvent.VK_R/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.ROTATED_SPLINE);}*/),
		ADD_EXTRUDED_SPLINE("Extruded Spline", null, "addObject.png", "Add an object defined by an extruded spline curve.", KeyEvent.VK_R/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.EXTRUDED_SPLINE);}*/),
		ADD_SIMPLE_APERTURE("Simple Aperture", null, "simpleAperture.png", "Add a simple aperture to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.SIMPLE_APERTURE);}*/),
		ADD_SPECTROPHOTOMETER("Spectrophotometer", null, "addObject.png", "Add a spectrophotometer to the tree.", KeyEvent.VK_S/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.SPECTROPHOTOMETER);}*/),
		ADD_SPHERICAL_LENS("Spherical Lens", null, "addObject.png", "Add a spherical lens to the tree.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.SPHERICAL_LENS);}*/),
		ADD_TRIANGULAR_PRISM("Triangular Prism", null, "addObject.png", "Add a triangular prism to the tree.", KeyEvent.VK_P/*/* , ae -> {getApp(ae).addObject(OpticsObjectKey.TRIANGULAR_PRISM);}*/),
		AUTOFOCUS("Autofocus", null, "autofocus.png", "Vary an object parameter to optimize image focus.", KeyEvent.VK_A/*/* , ae -> {getApp(ae).autofocus();}*/),
		CALC_DISPLAY_PHOTONS("Calc a Few Photons", null, "calcPhotons24.png", "Calculate just a few more photons", KeyEvent.VK_F, "F6"/*/* , ae -> {getApp(ae).calcAFewPhotons();}*/),
		CALC_PHOTONS("Calc Photons", null, "calcManyPhotons24.png", "Calculate more photons", KeyEvent.VK_C, "F5"/*/* , ae -> {getApp(ae).calcPhotons();}*/),
		CAPTURE_SCREEN("Capture Screen", null, "captureScreen.png", "Grab the screen and save to an image file", KeyEvent.VK_A/*/* , ae -> {getApp(ae).captureScreen();}*/),
		CLEAR_IMAGERS("Clear", null, "clear24.png", "Clear imager data.", KeyEvent.VK_C/* , ae -> {getApp(ae).clearImagers();}*/),
		COPY("Copy", null, "copy.png", "Copy the selected tree object to the clipboard.", KeyEvent.VK_C, "control C"/* , ae -> {getApp(ae).copy();}*/),
		COPY_TRAJECTORY("Copy trajectory", null, "copy.png", "Copy the last photon trajectory to the clipboard as a CSV.", KeyEvent.VK_C, "control T"/* , ae -> {getApp(ae).copyTrajectoryToClipboard();}*/),
		CUT("Cut", null, "cut.png", "Cut the selected tree object to the clipboard.", KeyEvent.VK_U, "control X"/* , ae -> {getApp(ae).cut();}*/),
		DEFINE_IMAGE("Define image", null, "defineImage.png", "Define the image that this Image Source displays.", KeyEvent.VK_M/* , ae -> {getApp(ae).defineImage();}*/),
		DELETE_OBJECT("Delete Object", null, "deleteObject.png", "Delete an object from the tree.", KeyEvent.VK_D, "DELETE"/* , ae -> {getApp(ae).deleteObject();}*/),
		EXIT("Exit", null, "exit24.png", "Exit the application", KeyEvent.VK_X/* , ae -> {getApp(ae).exit();}*/),
		EXPORT_MATERIALS("Export Materials", null, "export.png", "Export a CSV of all Materials.", KeyEvent.VK_E/* , ae -> {getApp(ae).exportMaterials();}*/),
		HELP("Help", null, "help24.png", "Link to the jOcular help website.", KeyEvent.VK_H/* , ae -> {getApp(ae).help();}*/),
		LOAD("Open",null, "load24.png", "Open a project", KeyEvent.VK_O/* , ae -> {getApp(ae).open();}*/),
		LOAD_IMAGER_DATA("Load Imager Data", null, "loadImagerData.png","Load data into imager calculated from a previous run.", KeyEvent.VK_L, "control alt L"/* , ae -> {getApp(ae).loadImagerData();}*/),
		MOVE_DOWN_IN_TREE("Move Down", null, "moveDown.png", "Move the selected object down in the project tree.", KeyEvent.VK_D, "control D"/* , ae -> {getApp(ae).moveInTree(false);}*/),
		MOVE_UP_IN_TREE("Move Up", null, "moveUp.png", "Move the selected object up in the project tree.", KeyEvent.VK_U, "control U"/* , ae -> {getApp(ae).moveInTree(true);}*/),
		NEW("New",null, "new24.png", "Start a new project", KeyEvent.VK_N/* , ae -> {getApp(ae).newProject();}*/),
		NULL("Null",null, null,"An action was referenced that does not exist in the action list.", KeyEvent.VK_N/* , ae -> {getApp(ae);}*/),
		OBJECT_INFO("Object Info", null, "info.png", "Show details of the selected object.", KeyEvent.VK_I/* , ae -> {getApp(ae).objectInfo();}*/),
		OPEN_IMAGER_WINDOW("Open an Imager Window", null, "imager24.png", "Opens a new imager windower.", KeyEvent.VK_I, "alt shift I"/* , ae -> {getApp(ae).openImagerWindow();}*/),
		PASTE("Paste", null, "paste.png", "Paste the clipboard contents to the selected tree object.", KeyEvent.VK_P, "control V"/* , ae -> {getApp(ae).paste();}*/),
		PRINT("Print", null, "print.png", "Prints the dialog to a printer", KeyEvent.VK_P/* , ae -> {getApp(ae).printScreen();}*/),
		REDO("Redo", null, "redo24.png", "Redo last operation that was undone", KeyEvent.VK_R, "ctrl Y"/* , ae -> {getApp(ae).redo();}*/),
		REDRAW("Redraw", null, "redraw.png", "Redraw view", KeyEvent.VK_R, "ctrl shift R"/* , ae -> {getApp(ae).redraw();}*/),
		RESET_WINDOWS("Reset Windows", null, "reset_windows.png", "Reset all windows to their default location, size, etc.", KeyEvent.VK_R, "alt shift R"/* , ae -> {getApp(ae).resetWindows();}*/),
		SAVE("Save", null,  "save24.png", "Saves the current project", KeyEvent.VK_S, "control S"/* , ae -> {getApp(ae).save(false);}*/),
		SAVE_AS("Save As", null,  "save24.png", "Saves the current data with a new name", KeyEvent.VK_A/* , ae -> {getApp(ae).save(true);}*/),
		SAVE_IMAGER_DATA("Save Imager Data", null, "saveImagerData.png","Save data from imager for a future run.", KeyEvent.VK_S, "control alt S"/* , ae -> {getApp(ae).saveImagerData();}*/),
		SET_CLIP_PLANE_TO_NONE("No Clip", null, "noClip24.png", "Set clip plane to none", KeyEvent.VK_N, "shift 0"/* , ae -> {getApp(ae).setClipPlane(OpticsObjectPanel.Plane.NONE);}*/),
		SET_CLIP_PLANE_TO_ZY("ZY Clip", null, "zyClip24.png", "Set clip plane to ZY plane", KeyEvent.VK_Z, "shift 1"/* , ae -> {getApp(ae).setClipPlane(OpticsObjectPanel.Plane.ZY_PLANE);}*/),
		SET_CLIP_PLANE_TO_ZX("ZX Clip", null, "zxClip24.png", "Set clip plane to ZX plane", KeyEvent.VK_Y, "shift 2"/* , ae -> {getApp(ae).setClipPlane(OpticsObjectPanel.Plane.ZX_PLANE);}*/),
		SET_CLIP_PLANE_TO_XY("XY Clip", null, "xyClip24.png", "Set clip plane to XY plane", KeyEvent.VK_X, "shift 3"/* , ae -> {getApp(ae).setClipPlane(OpticsObjectPanel.Plane.XY_PLANE);}*/),
		SET_VIEW_PLANE_TO_ZY("ZY Plane", null, "zyPlane24.png", "Set optics geomety panels to ZY plane", KeyEvent.VK_Y, "ctrl 1"/* , ae -> {getApp(ae).setViewPlane(OpticsObjectPanel.Plane.ZY_PLANE);}*/),
		SET_VIEW_PLANE_TO_ZX("ZX Plane", null, "zxPlane24.png", "Set optics geomety panels to ZX plane", KeyEvent.VK_Z, "ctrl 2"/* , ae -> {getApp(ae).setViewPlane(OpticsObjectPanel.Plane.ZX_PLANE);}*/),
		SET_VIEW_PLANE_TO_XY("XY Plane", null, "xyPlane24.png", "Set optics geomety panels to XY plane", KeyEvent.VK_X,"ctrl 3"/* , ae -> {getApp(ae).setViewPlane(OpticsObjectPanel.Plane.XY_PLANE);}*/),
		SETTINGS("Settings",null,"settings24.png","Show the settings dialog.", KeyEvent.VK_S, "alt shift S"/* , ae -> {getApp(ae).showSettings();}*/),
		SIZE_SPLITS("Size Splits", null, "sizeSplits.png", "Size the split panes.", KeyEvent.VK_N/* , ae -> {getApp(ae).sizeSplits();}*/),
		SPLINE_DIALOG("Edit Spline", null, "editSpline.png", "Edit the points that define the spline curve of this object.", KeyEvent.VK_S/* , ae -> {getApp(ae).splineDialog();}*/),
		STOP_WRANGLER("Stop", null, "stop24.png", "Stop wrangler.", KeyEvent.VK_S, "F8"/* , ae -> {getApp(ae).stopWrangler();}*/),
		SUPPRESS("Toggle object suppression", null, "suppress.png", "Toggle whether this object is used in calculations or not.", KeyEvent.VK_U, "control U"/* , ae -> {getApp(ae).toggleSuppression();}*/),
		TEST_ACTION("KM Test", null, "kmTest.png", "Run some test code.", KeyEvent.VK_T/* , ae -> {getApp(ae).testKM();}*/),
		UNDO("Undo", null, "undo24.png", "Undo last operation", KeyEvent.VK_U, "ctrl Z"/* , ae -> {getApp(ae).undo();}*/),
		WEBSITE("jOcular Website", null, "jocular_icon24.png", "Call up the jOcular web site.", KeyEvent.VK_W, "F2"/* , ae -> {getApp(ae).website();}*/),
		ZOOM_IN("Zoom in", null, "zoomIn24.png", "Zoom in view", KeyEvent.VK_I, "typed +"/* , ae -> {getApp(ae).zoomIn();}*/),
		ZOOM_OUT("Zoom out", null, "zoomOut24.png", "Zoom out view", KeyEvent.VK_O, "typed -"/* , ae -> {getApp(ae).zoomOut();}*/);
		
	private String m_description;
	private String m_deselectedName;
	private KeyStroke m_hotKey;
	private String m_iconName;
	private Icon m_icon;
	private int m_mnemonic;
	private int m_selectedMnemonicIndex;
	private ActionListener m_listener;
	private String m_name;
	private boolean m_selected;
	private boolean m_enabled;
	private PropertyChangeSupport m_support = new PropertyChangeSupport(this);
	
	private OpticsAction(String name, String deselectedName, String iconName, String description, int mnemonic /*, ActionListener listener*/){
		m_name = name;
		m_deselectedName = deselectedName;
		m_description = description;
		m_iconName = iconName;
		m_icon = getIcon(iconName);
		m_mnemonic = mnemonic;
		m_hotKey = null;
		m_enabled = true;
		//m_listener = listener;
		setMnemonicIndeces(mnemonic);
	}
	
	private OpticsAction(String name, String deselectedName, String iconName, String description, int mnemonic, String hotKey/*, ActionListener listener*/){
		m_name = name;
		m_deselectedName = deselectedName;
		m_description = description;
		m_iconName = iconName;
		m_icon = getIcon(iconName);
		m_mnemonic = mnemonic;
		m_enabled = true;
		
		m_hotKey = KeyStroke.getKeyStroke(hotKey);
		//m_listener = listener;
		setMnemonicIndeces(mnemonic);
	}
	
	
	String getDescription(){
		return m_description;
	}
	
	String getDeselectedName(){
		return m_deselectedName;
	}
	
	KeyStroke getHotKey(){
		return m_hotKey;
	}
	
	String getIconName(){
		return m_iconName;
	}
	
	int getMnemonic(){
		return m_mnemonic;
	}
	
	
	static private Icon getIcon(String n){
		if(n == null){
			return null;
		}
		
		Icon result = null;
		Class<? extends String> c = null;
		URL url = null;
						
		c = n.getClass();
		
		if(c != null){
			url = c.getResource(Jocular.MAIN_ICON_PATH+n);
		}
		if(url != null){
			result = new ImageIcon(url);
		}
		return result;

	}
	private void setMnemonicIndeces(int mnemonic){
		String key = KeyEvent.getKeyText(mnemonic).toLowerCase();
		m_selectedMnemonicIndex = m_name.toLowerCase().indexOf(key);
		
	}
	public String getName(){		
		return m_name;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		m_listener.actionPerformed(e);
	}
	@Override
	public Object getValue(String key) {
		Object result;
		switch(key){
		case Action.ACCELERATOR_KEY:
			result = m_hotKey;
			break;
		case Action.ACTION_COMMAND_KEY:
			result = null;//m_hotKey;//TODO: don't know what to return here
			break;
		default:
		case Action.DEFAULT:
			result = null;
			break;
		case Action.DISPLAYED_MNEMONIC_INDEX_KEY:
			result = m_selectedMnemonicIndex;
			break;
		case Action.LARGE_ICON_KEY:
			result = m_icon;
			break;
		case Action.LONG_DESCRIPTION:
			result = m_description;
			break;
		case Action.MNEMONIC_KEY:
			result = m_mnemonic;
			break;
		case Action.NAME:
			result = getName();
			break;
		case Action.SELECTED_KEY:
			result = m_selected;
			break;
		case Action.SHORT_DESCRIPTION:
			result = m_description;
			break;
		case Action.SMALL_ICON:
			result = m_icon;
			break;
			
		}
		return result;
	}
	public boolean isSelected(){
		return m_selected;
	}
	public void setSelected(boolean v){
		boolean oldValue = m_selected;
		m_selected = v;
		m_support.firePropertyChange(Action.SELECTED_KEY, oldValue, v);
	}
	@Override
	public void putValue(String key, Object value) {
		Object oldValue = getValue(key);
		switch(key){
		case Action.ACCELERATOR_KEY:
			m_hotKey = (KeyStroke)value;
			break;
		case Action.ACTION_COMMAND_KEY:
			System.out.println("OrthoCompassActionKey ACTION_COMMAND_KEY setting to value \""+value+"\"");
			break;
		default:
		case Action.DEFAULT:
			break;
		case Action.DISPLAYED_MNEMONIC_INDEX_KEY:
			System.out.println("OrthoCompassActionKey DISPLAYED_MNEMONIC_INDEX_KEY setting to "+value);
			m_selectedMnemonicIndex = (Integer)value;
			break;
		case Action.LARGE_ICON_KEY:
			m_icon = (Icon)value;
			break;
		case Action.LONG_DESCRIPTION:
			m_description = value.toString();
			break;
		case Action.MNEMONIC_KEY:
			m_mnemonic = (Integer)value;
			break;
		case Action.NAME:
			m_name = value.toString();
			break;
		case Action.SELECTED_KEY:
			m_selected = (Boolean)value;
			break;
		case Action.SHORT_DESCRIPTION:
			m_description = value.toString();
			break;
		case Action.SMALL_ICON:
			m_icon = (Icon)value;
			break;
			
		}
		m_support.firePropertyChange(key, oldValue, getValue(key));
		
	}
	@Override
	public void setEnabled(boolean b) {
		m_enabled = b;
		
	}
	@Override
	public boolean isEnabled() {
		return m_enabled;
	}
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		m_support.addPropertyChangeListener(listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		m_support.removePropertyChangeListener(listener);
	}
	public static void addKeyBindings(JComponent component){
		
		for(OpticsAction key : values()){
			
			
			
			if(key.m_hotKey != null){
			
				component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key.m_hotKey, key.getName());
				component.getActionMap().put(key.getName(), key);
			}
		}	
	}
	public static Jocular getApp(ActionEvent ae){
		Jocular result = null;
		JFrame jf = null;
		
		if(ae.getSource() instanceof java.awt.Component){
			java.awt.Component c = (java.awt.Component)(ae.getSource());
			
			while(c.getParent() != null){
				c = c.getParent();
			
				if(c instanceof JPopupMenu){
					c = ((JPopupMenu) c).getInvoker();
				}
				if(c instanceof JFrame){
					jf = (JFrame)c;
					break;
				}
			}
		}
		if(jf instanceof JocularWindow){
			result = ((JocularWindow)jf).getApp();
		} else if(jf instanceof ImagerWindow){
			result = ((ImagerWindow)jf).getApp();
		} else {
			throw new RuntimeException("Action source is a "+jf.getClass().getName());
		}
		if(result == null){
			throw new RuntimeException("Could not determine Jocular instance that spawned this ActionEvent:" + ae.toString());
		}
		return result;
	}
	

	

}
