/*******************************************************************************
 * Copyright (c) 2013-2016 Kenneth MacCallum, Bryan Matthews
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
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sourceforge.jocular.autofocus.AutofocusDialog;
import net.sourceforge.jocular.clipboard.OpticsObjectTransferable;
import net.sourceforge.jocular.gui.panel2d.ImagerPanel;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.gui.panel2d.OutputPanel;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectKey;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.OutputObject;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.AxisPositioner;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedListener;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.settings.SettingKey;
import net.sourceforge.jocular.settings.Settings;
import net.sourceforge.jocular.settings.SettingsDialog;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.SplineObject;
import net.sourceforge.jocular.splines.SplinePointDialog;


/**
 * @author kmaccallum
 * 
 * This the main class
 *
 */
public class Jocular implements ProjectUpdatedListener{
	
	static final String VERSION = "0.039";
	final JocularWindow m_window;
	final ImagerWindow m_imagerWindow;
	static final String PROJECT_FILE_EXT = "jocproj";
	static final String FILE_PATH_PREF = "filePathPref";
	public static final String MAIN_ICON_PATH = "/net/sourceforge/jocular/icons/";
	public static final String MAIN_ICON = MAIN_ICON_PATH + "jocular_logo96.png";
	private OpticsProject m_project = new OpticsProject();
	private String fileChooserPath = "";
	private JFileChooser m_jfc;
	private JFileChooser m_imageFileChooser;
	//private final WranglerListener m_wranglerListener;
	
	public Jocular(){
		//set UI font for combo boxes and labels to not be bold
		Font f = (Font)UIManager.get("Table.font");
		UIManager.put("Button.font", f);
		UIManager.put("CheckBox.font", f);
		UIManager.put("CheckBoxMenuItem.acceleratorFont", f);
		UIManager.put("CheckBoxMenuItem.font", f);
		UIManager.put("ColorChooser.font", f);
		UIManager.put("ComboBox.font",f);
		UIManager.put("EditorPane.font", f);
		UIManager.put("FormattedTextField.font", f);
		UIManager.put("IconButton.font", f);
		UIManager.put("InternalFrame.optionDialogTitleFont", f);
		UIManager.put("InternalFrame.paletteTitleFont", f);
		UIManager.put("InternalFrame.titleFont", f);
		UIManager.put("Label.font",f);
		UIManager.put("List.font", f);
		UIManager.put("Menu.acceleratorFont", f);
		UIManager.put("Menu.font", f);
		UIManager.put("MenuBar.font", f);
		UIManager.put("MenuItem.font", f);
		UIManager.put("OptionPane.font", f);
		UIManager.put("Panel.font", f);
		UIManager.put("PopupMenu.font", f);		
		UIManager.put("ProgressBar.font", f);
		UIManager.put("RadioButton.font", f);
		UIManager.put("RadioButtonMenuItem.font", f);
		UIManager.put("ScrollPane.font", f);
		UIManager.put("Slider.font", f);
		UIManager.put("Spinner.font", f);
		UIManager.put("TabbedPane.font", f);
		UIManager.put("Table.font", f);
		UIManager.put("TableHeader.font", f);
		UIManager.put("TextArea.font", f);
		UIManager.put("TextField.font", f);
		UIManager.put("TextPane.font", f);
		UIManager.put("TitledBorder.font", f);
		UIManager.put("ToggleButton.font", f);
		UIManager.put("ToolBar.font", f);
		UIManager.put("ToolTip.font", f);
		UIManager.put("Tree.font", f);
		UIManager.put("Viewport.font", f);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
//		m_wranglerListener = new WranglerListener() {
//			
//			@Override
//			public void wranglingUpdate() {
//				m_window.renderDisplay();
//				m_imagerWindow.renderDisplay();
//				
//			}
//			
//			@Override
//			public void wranglingOngoing() {
//				m_window.renderDisplay();
//				m_imagerWindow.renderDisplay();
//				
//			}
//			
//			@Override
//			public void wranglingComplete() {
//				m_window.renderDisplay();
//				m_imagerWindow.renderDisplay();
//				
//			}
//		};
		
		Settings.SETTINGS.initialize();
		fileChooserPath = Settings.SETTINGS.getDefaultFilePath();
		m_jfc = new JFileChooser(fileChooserPath);
		m_jfc.setFileFilter(new FileNameExtensionFilter("jOcular Project File",PROJECT_FILE_EXT));
		
		m_window = new JocularWindow(this);		
		
		
		m_imageFileChooser = new JFileChooser(fileChooserPath);
		m_imageFileChooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
						
		m_imagerWindow = new ImagerWindow(this);	
	}
			
	
	/**
	 * This the main entry point for the program
	 * 
	 * @param args - not used
	 *  
	 */
	public static void main(String[] args) {
				
		Jocular jocular = new Jocular();
		jocular.createTestProject();
		//jocular.runRenderer();	
	}
	
	
	public void testKM(){
//		PropertyManager.getInstance().updateEquations(getProject(), false);
//		Collection<OpticsObject> oos = getProject().getFlattenedOpticsObjects(true);
//		for(OpticsObject oo : oos){
//			for(PropertyKey key : oo.getPropertyKeys()){
//				oo.firePropertyUpdated(key);
//			}
//		}
//		PropertyManager.getInstance().updateEverything();
//		m_window.repaint();
//		m_window.updateEverything();
		PropertyManager.getInstance().undeferParsing(getProject());
		PropertyManager.getInstance().updateEquations(getProject(), true);
	}
	public void copyTrajectoryToClipboard(){
		//getProject().getOpticsObject().updatePositioner(null);
		//this.getProject().FireProjectUpdated(null);
		//m_window.test();
		PhotonTrajectory pt = m_project.getWrangler().getTrajectories().iterator().next();
		if(pt == null){
			return;
		}
		String s = "x, y, z, dx, dy, dz, in object, interacting object, photon source, #, reference object, surface normal x, y, z\n";
		for(int i = 0; i < pt.getNumberOfPhotons(); ++i){
			Photon p = pt.getPhoton(i);
			s += p.getOrigin().x + ", ";
			s += p.getOrigin().y + ", ";
			s += p.getOrigin().z + ", ";
			s += p.getDirection().x + ", ";
			s += p.getDirection().y + ", ";
			s += p.getDirection().z + ", ";
			s += p.getContainingObject()+", ";
			s += pt.getInteraction(i).getInteractingObject()+",";
			s += p.getPhotonSource() +", ";
			s += i+", ";
			s += pt.getInteraction(i).getReferenceObject()+", ";
			Vector3D norm = pt.getInteraction(i).getNormal();
			s += norm.x+", ";
			s += norm.y+", ";
			s += norm.z+", ";
			s += "\n";
			
		}
		StringSelection selection = new StringSelection(s);
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(selection, selection);
	}
	
	
	public void exportMaterials(){
		m_jfc.setFileFilter(new FileNameExtensionFilter("CSV File","csv"));
		if(m_jfc.showSaveDialog(m_window) != JFileChooser.APPROVE_OPTION){
			System.out.println("Jocular.exportMaterials cancelled.");
			return;
		}
		File f = m_jfc.getSelectedFile();
		Writer fw = null;
		try {
			NumberFormat nf = new DecimalFormat("0.0####E00");
			fw = new BufferedWriter(new FileWriter(f));
			MaterialKey mks[] = MaterialKey.values();
			String halfRow1 = "Wavelength (m)";
			String halfRow2 = "Wavelength (m)";
			for(MaterialKey mk : mks){
				halfRow1 += ", "+mk.name()+" N";
				halfRow2 += ", "+mk.name()+" Transmissivity";
			}
			fw.write(halfRow1 +", "+ halfRow2 + "\r\n");
			ObjectPositioner pos = new AxisPositioner();
			for(double w = 300e-9; w <= 2500e-6; w += 10e-9){
				halfRow1 = "" + nf.format(w);
				halfRow2 = "" + nf.format(w);
				for(MaterialKey mk : mks){
					halfRow1 += ", " + nf.format(mk.getMaterial().getOrdinaryRefractiveIndex(w));
					halfRow2 += ", " + nf.format(mk.getMaterial().getTransmissivity(w, Vector3D.Z_AXIS, pos));
				}
				fw.write(halfRow1 +", "+ halfRow2 + "\r\n");
				
			}
			fw.close();
			
		} catch(IOException e){
			
		}
	}
	
	private OpticsProject loadProjectFromDisk(){
		OpticsProject project = null;
		m_jfc.setFileFilter(new FileNameExtensionFilter("jOcular Project File",PROJECT_FILE_EXT));
		if(m_jfc.showOpenDialog(m_window) == JFileChooser.APPROVE_OPTION){
			
			Settings.SETTINGS.setSetting(SettingKey.DEFAULT_FILE_PATH, m_jfc.getSelectedFile().getPath());
			

			
			project = OpticsProject.loadProject(m_jfc.getSelectedFile());		
			
			project.getOpticsObject().updatePositioner(null);
			PropertyManager.getInstance().undeferParsing(project);
			
		}
		return project;
	}
	public void open(){
		OpticsProject p = loadProjectFromDisk();
		setProject(p);
		PropertyManager.getInstance().undeferParsing(getProject());
		PropertyManager.getInstance().updateEquations(getProject(), true);
		
		
//		PropertyManager.getInstance().updateAllInterests();
		Collection<OpticsObject> oos = getProject().getFlattenedOpticsObjects(true);
		for(OpticsObject oo : oos){
			for(PropertyKey key : oo.getPropertyKeys()){
				oo.firePropertyUpdated(key);
			}
		}
		m_window.updateEverything();
	}
	
	
	public void newProject(){
		setProject(new OpticsProject());
		PropertyManager.getInstance().undeferParsing(getProject());
		PropertyManager.getInstance().updateEquations(getProject(), true);
	}
	
	
	public void showSettings(){
		SettingsDialog s = new SettingsDialog(m_window, Settings.SETTINGS, new OpticsProject());
		s.setVisible(true);
	}
	
	
	public void save(boolean showDialog){
		if(showDialog || m_project.getFileName() == ""){
			m_jfc.setSelectedFile(m_project.getFile());
			if(m_jfc.showSaveDialog(m_window) == JFileChooser.APPROVE_OPTION){
				
				String fileName = m_jfc.getSelectedFile().getPath();
				
				Settings.SETTINGS.setSetting(SettingKey.DEFAULT_FILE_PATH, fileName);		
				
				// Ensure file name ends with correct extension
				if(!m_jfc.getFileFilter().accept(m_jfc.getSelectedFile())){
					fileName = fileName.concat("." + PROJECT_FILE_EXT);
				}			
				File f = new File(fileName);
				if(f.exists()){
					if(JFileChooser.APPROVE_OPTION != JOptionPane.showConfirmDialog(m_window, "File \""+fileName+"\" exists.", "Overwrite?", JOptionPane.OK_CANCEL_OPTION)){
						return;
					}
				}
				m_project.setFileName(fileName);
				m_project.save();
			}
			
		} else {
			m_project.save();
		}		
	}
	
	
	public void cut(){
		copy();
		deleteObject();
	}
	
	
	public void copy(){
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		OpticsObject o = m_window.getSelectedObject();
		if(o == null){
			return;
		}

		OpticsObjectTransferable oot = new OpticsObjectTransferable(o);
		clipboard.setContents(oot, oot);
		
	}
	
	
	public void paste(){
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
		Object oClip = null;
		for(DataFlavor f:flavors){
			try {
				Object oTest = clipboard.getData(f);
				if(oTest instanceof OpticsObject){
					oClip = oTest;
					break;
				}
			} catch (UnsupportedFlavorException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		if(oClip == null){
			return;
		}
		m_window.addObject((OpticsObject)oClip);		
	}
	
	
	public void printScreen(){
		//TODO: Add body
	}
	
	
	public void captureScreen(){
		BufferedImage i = m_imagerWindow.getSelectedPanel().getImage();
		JFileChooser jfc = new JFileChooser();
		//jfc.setFileFilter(new FileNameExtensionFilter("JPG, GIF, PNG Images", "jpg", "gif","png"));
		jfc.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
		if(JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(m_window)){
			try{
				ImageIO.write(i, "png", jfc.getSelectedFile());
			} catch(IOException e){
				System.out.println("Jocular.captureScreen could not write file.");
			}
		}
		
	}
	
	
	public void undo(){
		m_project.undo();
		//m_window.repaint();
		m_window.updateEverything();
		
	}
	
	
	public void redo(){
		m_project.redo();
		//m_window.repaint();
		m_window.updateEverything();
	}
	
	
	public void sizeSplits(){
		m_window.setTableSplitRatio();
	}
	
	
	public void exit(){
		
		// Ensure imager window is closed here
		// This will also allow it to set any internal Settings;
		m_imagerWindow.exit();
		
		Settings.SETTINGS.store();
	}
	
	
	public void zoomIn(){
		if(m_imagerWindow.isFocused()){
			m_imagerWindow.zoomIn();
		} else {
			m_window.zoomIn();
		}
	}
	public void zoomOut(){
		if(m_imagerWindow.isFocused()){
			m_imagerWindow.zoomOut();
		} else {
			m_window.zoomOut();
		}
	}
	
	
	public void addObject(OpticsObjectKey keyToAdd){
		m_window.addObject(keyToAdd);
	}
	
	
	public void deleteObject(){
		m_window.deleteObject();
	}
	public void moveInTree(boolean upNotDown){
		m_window.moveInTree(upNotDown);
	}
	
	
	
	
	
	public void calcAFewPhotons(){
		
		m_window.setCalcPhotonsEnabled(false);
						
		m_project.getWrangler().wrangle(m_project.getOpticsObject(), Settings.SETTINGS.getPhotonTrajectoryDisplayCount());
	}
	
	
	public void calcPhotons(){
		
		m_window.setCalcPhotonsEnabled(false);
		m_project.getWrangler().wrangle(m_project.getOpticsObject(), Settings.SETTINGS.getPhotonTrajectoryCount());				
		
	}
	
	
	public void clearImagers(){
		// Clear imagers
		stopWrangler();
		ArrayList<OutputObject> os = (ArrayList<OutputObject>) m_project.getOpticsObject().getOutputObjects();
		
		for(OutputObject o : os){
			o.clear();
		}
		m_imagerWindow.resetPanels();
		m_imagerWindow.renderDisplay();
	}
	

	public void setProject(OpticsProject p){
		if(p == null){
			return;
		}
		if(m_project != null){
			//m_project.getWrangler().removeWranglerListener(m_wranglerListener);
			PropertyManager.getInstance().removeProject(m_project);
		}
		m_project = p;	
		//m_project.getWrangler().addWranglerListener(m_wranglerListener);
		m_window.setProject(p);
		m_imagerWindow.setProject(p);	
		
		
		PropertyManager.getInstance().addProject(m_project);
		//m_project.getOpticsObject().calcEquations();
		PropertyManager.getInstance().updateEquations(m_project.getOpticsObject(), true);
		m_project.addProjectUpdatedListener(this);
		
		
	}
	
	
	/**
	 * Creates a set of optics for testing purposes
	 * 
	 */
	private void createTestProject(){
		
		//OpticsProject project = OpticsProjectParser.parseProjectFile(TESTPROJECT_FILE_PATH);		
		setProject(new OpticsProject());
	}

	
//	private void runRenderer(){
//		//kluge to make panel update regularly.
//		// TODO: Replace with listeners
//		Timer t = new Timer(1000, new ActionListener(){
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {			
//				if(m_project.getWrangler().isWrangling()){
//					m_window.renderDisplay();
//					m_imagerWindow.renderDisplay();
//				}
//			}
//			
//		});
//		t.start();
//	}	

	/**
	 * @return the version formatted for display
	 */
	public static String getFormattedVersion(){
		String revision = VERSION;
		
		return revision;
	}
	public OpticsProject getProject(){
		return m_project;
	}

//	public OpticsActionList getActionList() {
//		return m_window.m_actionList;
//	}

	public void openImagerWindow() {
		
		m_imagerWindow.setVisible(true);		
	}
	
	public void resetWindows() {
		
		m_window.reset();
		m_imagerWindow.reset();
	}
	

	public void stopWrangler(){
		
		m_project.stopWrangler();
	}
	
	public void setViewPlane(OpticsObjectPanel.Plane plane){
				
		//m_window.setViewPlane(plane);
		m_window.set3DViewPlane(plane);
	}
	public void setClipPlane(Plane plane) {
		m_window.set3DClipPlane(plane);
		
	}
	public void objectInfo(){
		OpticsObject o = m_window.getSelectedObject();
		String s = "";
		if(o instanceof SphericalLens){
			SphericalLens sl = (SphericalLens)o;
			double n400 = sl.getFocalLength(400e-9);
			double n500 = sl.getFocalLength(500e-9);
			double n600 = sl.getFocalLength(600e-9);
			double d = ((EquationProperty)(sl.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
			s += "Focal Length, f-number\n";
			s += "400nm --> "+n400+"\n";
			s += "500nm --> "+n500+"\n";
			s += "600nm --> "+n600+"\n";
			s += "\n";
			s += "f-number\n";
			s += "400nm --> "+n400/d+"\n";
			s += "500nm --> "+n500/d+"\n";
			s += "600nm --> "+n600/d+"\n";
		} else if(o instanceof SpectroPhotometer){
			SpectroPhotometer sp = (SpectroPhotometer)o;
			s += "Histogram:\nWavelength (nm),\tValue (J)\n";
			double[] bins = sp.getBinValues();
			double[] ws = sp.getBinCentres();
			for(int i = 0; i < bins.length; ++i){
				s += ws[i] + ",\t" + bins[i] + "\r\n"; 
			}
		}
		JTextArea jta = new JTextArea(s,20,20);
		jta.setEditable(false);
		JOptionPane.showMessageDialog(m_window, new JScrollPane(jta));
	}
	
	public void defineImage() {

		OpticsObject o = m_window.getSelectedObject();
		if(o instanceof ImageSource){
			ImageSource is = (ImageSource)o;
			is.setProperty(PropertyKey.IMAGE, defineImage(m_window).getDefiningString());
		}		
	}
	public static ImageProperty defineImage(Component c) {
		ImageProperty result = null;
	
		
		JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(new FileNameExtensionFilter("JPG, GIF, PNG Images", "jpg", "gif","png"));
		if(JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(c)){
			File f = jfc.getSelectedFile();
			try {
				BufferedImage i = ImageIO.read(f);
				if(i.getPropertyNames() != null){
					for(String n : i.getPropertyNames()){
						System.out.print(n+"("+i.getProperty(n)+") ");
					}
				} else {
					System.out.println("No image properties");
				}
				result = (new ImageProperty(i));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return result;

	}
	

	@Override
	public void projectUpdated(ProjectUpdatedEvent e) {
		m_window.updateTitle(m_project);
	}
	public void toggleSuppression() {
		OpticsObject o = m_window.getSelectedObject();
		boolean s = o.isSuppressed();
		//m_project.addAndDoEdit(new PropertyEdit(m_project, o, PropertyKey.SUPPRESSED, Boolean.toString(!s)));
		m_project.addPropertyEdit(o, PropertyKey.SUPPRESSED, Boolean.toString(!s));
		
	}


	public void loadImagerData() {
		OutputPanel op = m_imagerWindow.getSelectedPanel();
		if(op instanceof ImagerPanel){
			ImagerPanel ip = (ImagerPanel)op;
			Imager i = ip.getImager();
			fileChooserPath = Settings.SETTINGS.getDefaultFilePath();
			JFileChooser jfc = new JFileChooser(fileChooserPath);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("DAT raw data file", "dat");
			jfc.setFileFilter(filter);
			if(jfc.showOpenDialog(m_window) == JFileChooser.APPROVE_OPTION){
				File f = jfc.getSelectedFile();
				try{
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
					i.addContents(ois);
					ois.close();
				} catch(IOException e){
					System.out.println("Jocular.loadImageData IOException "+e);
				}
			}
		}
		
	}


	public void saveImagerData() {
		OutputPanel op = m_imagerWindow.getSelectedPanel();
		if(op instanceof ImagerPanel){
			ImagerPanel ip = (ImagerPanel)op;
			Imager i = ip.getImager();
			fileChooserPath = Settings.SETTINGS.getDefaultFilePath();
			JFileChooser jfc = new JFileChooser(fileChooserPath);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("DAT raw data file", "dat");
			jfc.setFileFilter(filter);
			if(jfc.showSaveDialog(m_window) == JFileChooser.APPROVE_OPTION){
				File f = jfc.getSelectedFile();
				try{
					ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(f));
					i.writeContents(ois);
					ois.close();
				} catch(IOException e){
					System.out.println("Jocular.saveImageData IOException "+e);
				}
			}
		}
		
	}

	/**
	 * cause property tables to stop editing. This is useful if a new object is selected on the tree for instance
	 */
	public void stopEditing() {
		m_window.stopEditing();
		
	}


	public void autofocus() {
		AutofocusDialog afd = new AutofocusDialog(m_window, getProject());
		afd.setVisible(true);
	}


	public void splineDialog() {
		OpticsObject o = m_window.getSelectedObject();
		if(o instanceof SplineObject){
			SplinePointDialog spd = new SplinePointDialog(m_window, getProject(), (SplineObject)o);
			spd.setVisible(true);
		}
		
	}
	public void about(){
		Icon i = (new ImageIcon(getClass().getResource(MAIN_ICON)));
		JTabbedPane jtp = new JTabbedPane(JTabbedPane.BOTTOM);
		JTextArea jta1 = new JTextArea("jOcular version "+Jocular.getFormattedVersion());
		JTextArea jta2 = new JTextArea("Copyright (c) 2013-2016, Kenneth MacCallum, Bryan Matthews\n" +
				"All rights reserved.\n" +
				"Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\n" +
				"     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\n" +
				"     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. \n" +
				"THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, " +
				"BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n" +
				"IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, " +
				"EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; " +
				"LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, " +
				"WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, " +
				"EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.");
		jta1.setEditable(false);
		jta2.setEditable(false);
		jta2.setLineWrap(true);
		jta2.setWrapStyleWord(true);
		//jta1.setLineWrap(true);
		//jta2.setLineWrap(true);
		jtp.add("Version",new JScrollPane(jta1, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		jtp.add("License",new JScrollPane(jta2,  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		jtp.setPreferredSize(new Dimension(300,200));
		JOptionPane.showMessageDialog(null, jtp, "About", JOptionPane.INFORMATION_MESSAGE, i);
	}


	public void help() {
		try {
			Desktop.getDesktop().browse(new URI("https://sourceforge.net/p/jocular/wiki/jOcular%20Help/"));
		} catch (IOException | URISyntaxException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		
	}


	public void website() {
		try {
			Desktop.getDesktop().browse(new URI("http://jocular.sourceforge.net"));
		} catch (IOException | URISyntaxException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		
	}
	public void redraw(){
		m_window.renderDisplay();
	}

	/**
	 * Adds a project as if it is a sub-part of the current project. This will allow libraries of standard parts and assemblies to
	 * be developed.
	 */
	public void addPart() {
		OpticsProject project = loadProjectFromDisk();
		OpticsPart part = new OpticsPart();
		
		
		
		
		part.setProperty(PropertyKey.FILE_NAME, PropertyManager.getRelativePath(part, project.getFile()));
		m_window.addObject(part);
		
		//part.setPositioner(new OffsetPositioner());
		//m_window.updateEverything();
		//getProject().fireNodeUpdate(part, getProject().getOpticsObject(), NodeUpdateType.CHANGE, getProject().getOpticsObject().getPos(part));
		//getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.POSITION, part));
		
		part.firePropertyUpdated(PropertyKey.POSITIONER);
		m_window.updateEverything();
//		PropertyManager.getInstance().updateEquations(part,false);
//		PropertyManager.getInstance().updateEquations(project, true);
	}


	
	
}

