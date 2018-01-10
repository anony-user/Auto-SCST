/*******************************************************************************
 * Copyright (c) 2013, Kenneth MacCallum, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.settings;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.PhotonColour;
import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.properties.StringProperty;
/**
 * A class to store jocular settings
 * @author tandk
 *
 */

public class Settings implements PropertyOwner {
	
	public enum NumberDisplay {EQUATION, METRIC, IMPERIAL};
	static final NumberDisplay DEFAULT_NUMBER_DISPLAY = NumberDisplay.EQUATION;
	static final int DEFAULT_TRAJECTORY_COUNT = 1;
	static final PhotonColour DEFAULT_PHOTON_COLOUR = PhotonColour.MATERIAL;
	static final String DEFAULT_MOST_PROBABLE = "false";
	static final String DEFAULT_USE_POLARIZATION = "false";
	static final String DEFAULT_FILE_PATH = "";
	public static final int DEFAULT_WIN_X_LOC = 0;
	public static final int DEFAULT_WIN_Y_LOC = 0;	
	public static final int DEFAULT_WINDOW_HEIGHT = 600;
	public static final int DEFAULT_WINDOW_WIDTH = 600;	
	public static final int DEFAULT_SPLIT_LOC = 0;
	public static final double MIN_DISTANCE =  10e-12;
	public static final double HC_CONSTANT = 1.98644568E-25;//
	public static final int DEFAULT_NUM_WRANGLER_THREADS = 1;
	
	public static final Settings SETTINGS = new Settings();
	private final Preferences m_prefs = Preferences.userNodeForPackage(Jocular.class);
	
	private IntegerProperty m_trajectoryCount = new IntegerProperty(String.valueOf(DEFAULT_TRAJECTORY_COUNT));
	private IntegerProperty m_trajectoryDisplayCount = new IntegerProperty(String.valueOf(DEFAULT_TRAJECTORY_COUNT));
	private EnumProperty m_photonColour = new EnumProperty(DEFAULT_PHOTON_COLOUR, DEFAULT_PHOTON_COLOUR.name());
	private BooleanProperty m_pickMostProbable = new BooleanProperty(DEFAULT_MOST_PROBABLE);
	private BooleanProperty m_usePolarizationInImager = new BooleanProperty(DEFAULT_USE_POLARIZATION);
	private EnumProperty m_numberDisplay = new EnumProperty(DEFAULT_NUMBER_DISPLAY, DEFAULT_NUMBER_DISPLAY.name());
	// These are only accessed internally
	private StringProperty m_defaultFilePath = new StringProperty(DEFAULT_FILE_PATH);
	private IntegerProperty m_imgWinXLoc = new IntegerProperty(String.valueOf(DEFAULT_WIN_X_LOC));
	private IntegerProperty m_imgWinYLoc = new IntegerProperty(String.valueOf(DEFAULT_WIN_Y_LOC));
	private IntegerProperty m_imgWinHeight = new IntegerProperty(String.valueOf(DEFAULT_WINDOW_HEIGHT));
	private IntegerProperty m_imgWinWidth = new IntegerProperty(String.valueOf(DEFAULT_WINDOW_WIDTH));
	private IntegerProperty m_mainWinXLoc = new IntegerProperty(String.valueOf(DEFAULT_WIN_X_LOC));
	private IntegerProperty m_mainWinYLoc = new IntegerProperty(String.valueOf(DEFAULT_WIN_Y_LOC));
	private IntegerProperty m_mainWinHeight = new IntegerProperty(String.valueOf(DEFAULT_WINDOW_HEIGHT));
	private IntegerProperty m_mainWinWidth = new IntegerProperty(String.valueOf(DEFAULT_WINDOW_WIDTH));
	
	private IntegerProperty m_mainWinSplitHorizontal = new IntegerProperty(String.valueOf(DEFAULT_SPLIT_LOC));
	private IntegerProperty m_mainWinSplitTree = new IntegerProperty(String.valueOf(DEFAULT_SPLIT_LOC));
	private IntegerProperty m_mainWinSplitPositioner = new IntegerProperty(String.valueOf(DEFAULT_SPLIT_LOC));
	public IntegerProperty m_mainWinSplitGeometry = new IntegerProperty(String.valueOf(DEFAULT_SPLIT_LOC));
	public IntegerProperty m_numWranglerThreads = new IntegerProperty(String.valueOf(DEFAULT_NUM_WRANGLER_THREADS));
	private Collection<PropertyUpdatedListener> m_propertyUpdatedListeners = new ArrayList<PropertyUpdatedListener>();
	public Settings(){
		super();
	}
	
	public void initialize(){
		
		for(PropertyKey key : getPropertyKeys()){
			String s = m_prefs.get(key.name(), "");
			
			if(s != ""){
				setProperty(key, s);
			}
		}
		
		// These are not displayed in the settings window
		for(SettingKey key : getSettingKeys()){
			String s = m_prefs.get(key.name(), "");
			
			if(s != ""){
				setSetting(key, s);
			}
		}
	}
	@Override
	public void addPropertyUpdatedListener(PropertyUpdatedListener listener){
		
		if(!m_propertyUpdatedListeners.contains(listener))
			m_propertyUpdatedListeners.add(listener);
	}
	
	@Override
	public void removePropertyUpdatedListener(PropertyUpdatedListener listener) {
		m_propertyUpdatedListeners.remove(listener);
	}
	@Override
	public void firePropertyUpdated(PropertyKey key){
		for(PropertyUpdatedListener listener : m_propertyUpdatedListeners){
			listener.propertyUpdated(new PropertyUpdatedEvent(this, key));
		}
	}
	public void store(){
		
		for(PropertyKey key : getPropertyKeys()){
			m_prefs.put(key.name(), getProperty(key).getDefiningString());
		}
		
		for(SettingKey key : getSettingKeys()){
			m_prefs.put(key.name(), getSetting(key).getDefiningString());
		}
	}
	
	public void restoreDefaults(){
//		
//		for(PropertyKey key : getPropertyKeys()){
//			setProperty(key, getPropertyDefault(key));
//		}
		
		for(SettingKey key : getSettingKeys()){
			setSetting(key, getSettingDefault(key));
		}
		
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s) {
		switch(key){
		case TRAJECTORY_COUNT:
		case TRAJECTORY_DISPLAY_COUNT:
			return InputVerificationRules.verifyPositiveInteger(s);
		default:
			return new VerificationResult(true, "");
		}		
	}
	
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case TRAJECTORY_COUNT:
			m_trajectoryCount = new IntegerProperty(s);
			break;
		case TRAJECTORY_DISPLAY_COUNT:
			m_trajectoryDisplayCount = new IntegerProperty(s);
			break;
		case PHOTON_COLOUR_SCHEME:
			m_photonColour = new EnumProperty(PhotonColour.MATERIAL,s);
			break;
		case PICK_MOST_PROBABLE:
			m_pickMostProbable = new BooleanProperty(s);
			break;
		case USE_POLARIZATION:
			m_usePolarizationInImager = new BooleanProperty(s);
			break;
		case WRANGLER_THREAD_COUNT:
			m_numWranglerThreads = new IntegerProperty(s);
			break;
		case EQUATION_DISPLAY:
			m_numberDisplay = new EnumProperty(NumberDisplay.EQUATION,s);
			break;
		default:
			break;
		}
		firePropertyUpdated(key);
	}
	
	public void setSetting(SettingKey key, String s){
		
		switch(key){
		case IMG_WIN_HEIGHT:
			m_imgWinHeight = new IntegerProperty(s);
			break;
		case IMG_WIN_WIDTH:
			m_imgWinWidth = new IntegerProperty(s);
			break;
		case IMG_WIN_X_LOC:
			m_imgWinXLoc = new IntegerProperty(s);
			break;
		case IMG_WIN_Y_LOC:
			m_imgWinYLoc = new IntegerProperty(s);
			break;
		case MAIN_WIN_HEIGHT:
			m_mainWinHeight = new IntegerProperty(s);
			break;
		case MAIN_WIN_WIDTH:
			m_mainWinWidth = new IntegerProperty(s);
			break;
		case MAIN_WIN_X_LOC:
			m_mainWinXLoc = new IntegerProperty(s);
			break;
		case MAIN_WIN_Y_LOC:
			m_mainWinYLoc = new IntegerProperty(s);
			break;
		case MAIN_WIN_SPLIT_HORIZ:
			m_mainWinSplitHorizontal = new IntegerProperty(s);
			break;			
		case MAIN_WIN_SPLIT_TREE:
			m_mainWinSplitTree = new IntegerProperty(s);
			break;
		case MAIN_WIN_SPLIT_POS:
			m_mainWinSplitPositioner = new IntegerProperty(s);
			break;
		case MAIN_WIN_SPLIT_GEO:
			m_mainWinSplitGeometry = new IntegerProperty(s);
			break;
		case DEFAULT_FILE_PATH:
			m_defaultFilePath = new StringProperty(s);
			break;
			
		default:
			break;
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case TRAJECTORY_COUNT:
			result = m_trajectoryCount;
			break;
		case TRAJECTORY_DISPLAY_COUNT:
			result = m_trajectoryDisplayCount;
			break;
		case PHOTON_COLOUR_SCHEME:
			result = m_photonColour;
			break;
		case PICK_MOST_PROBABLE:
			result = m_pickMostProbable;
			break;
		case USE_POLARIZATION:
			result = m_usePolarizationInImager;
			break;
		case WRANGLER_THREAD_COUNT:
			result = m_numWranglerThreads;
			break;
		case EQUATION_DISPLAY:
			result = m_numberDisplay;
			break;
		default:
			result = null;
			
		}
		return result;
	}
	
	public Property<?> getSetting(SettingKey key){
		
		Property<?> result;
		
		switch(key){
		case IMG_WIN_HEIGHT:
			result = m_imgWinHeight;
			break;
		case IMG_WIN_WIDTH:
			result = m_imgWinWidth;
			break;
		case IMG_WIN_X_LOC:
			result = m_imgWinXLoc;
			break;
		case IMG_WIN_Y_LOC:
			result = m_imgWinYLoc;
			break;
		case MAIN_WIN_HEIGHT:
			result = m_mainWinHeight;
			break;
		case MAIN_WIN_WIDTH:
			result = m_mainWinWidth;
			break;
		case MAIN_WIN_X_LOC:
			result = m_mainWinXLoc;
			break;
		case MAIN_WIN_Y_LOC:
			result = m_mainWinYLoc;
			break;
		case MAIN_WIN_SPLIT_HORIZ:
			result = m_mainWinSplitHorizontal;
			break;			
		case MAIN_WIN_SPLIT_TREE:
			result = m_mainWinSplitTree;
			break;
		case MAIN_WIN_SPLIT_POS:
			result = m_mainWinSplitPositioner;
			break;
		case MAIN_WIN_SPLIT_GEO:
			result = m_mainWinSplitGeometry;
			break;
		case DEFAULT_FILE_PATH:
			result = m_defaultFilePath;
			break;
		default:
			result = null;
			
		}
		return result;		
	}
	
	private String getSettingDefault(SettingKey key) {
		String result;
		switch(key){
		case IMG_WIN_HEIGHT:
			result = String.valueOf(DEFAULT_WINDOW_HEIGHT);
			break;
		case IMG_WIN_WIDTH:
			result = String.valueOf(DEFAULT_WINDOW_WIDTH);
			break;
		case IMG_WIN_X_LOC:
			result = String.valueOf(DEFAULT_WIN_X_LOC);
			break;
		case IMG_WIN_Y_LOC:
			result = String.valueOf(DEFAULT_WIN_Y_LOC);
			break;
		case MAIN_WIN_HEIGHT:
			result = String.valueOf(DEFAULT_WINDOW_HEIGHT);
			break;
		case MAIN_WIN_WIDTH:
			result = String.valueOf(DEFAULT_WINDOW_WIDTH);
			break;
		case MAIN_WIN_X_LOC:
			result = String.valueOf(DEFAULT_WIN_X_LOC);
			break;
		case MAIN_WIN_Y_LOC:
			result = String.valueOf(DEFAULT_WIN_Y_LOC);
			break;
		case MAIN_WIN_SPLIT_HORIZ:
		case MAIN_WIN_SPLIT_TREE:
		case MAIN_WIN_SPLIT_POS:
		case MAIN_WIN_SPLIT_GEO:
			result = String.valueOf(DEFAULT_SPLIT_LOC);
			break;	
		
		case DEFAULT_FILE_PATH:
			result = DEFAULT_FILE_PATH;
			break;
		default:
			result = null;
			
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.TRAJECTORY_COUNT, PropertyKey.TRAJECTORY_DISPLAY_COUNT,PropertyKey.PHOTON_COLOUR_SCHEME, PropertyKey.PICK_MOST_PROBABLE, PropertyKey.USE_POLARIZATION, PropertyKey.WRANGLER_THREAD_COUNT, PropertyKey.EQUATION_DISPLAY));
		return result;
	}
	
	@Override
	public List<Property<?>> getProperties() {
		ArrayList<Property<?>> result = new ArrayList<Property<?>>();
		for(PropertyKey key : getPropertyKeys()){
			result.add(getProperty(key));
		}
		return result;
	}
	
	private SettingKey[] getSettingKeys() {
		SettingKey[] result = {SettingKey.DEFAULT_FILE_PATH, SettingKey.IMG_WIN_HEIGHT, SettingKey.IMG_WIN_WIDTH, SettingKey.IMG_WIN_X_LOC, SettingKey.IMG_WIN_Y_LOC, SettingKey.MAIN_WIN_HEIGHT, SettingKey.MAIN_WIN_WIDTH, SettingKey.MAIN_WIN_X_LOC, SettingKey.MAIN_WIN_Y_LOC, SettingKey.MAIN_WIN_SPLIT_HORIZ, SettingKey.MAIN_WIN_SPLIT_TREE,SettingKey.MAIN_WIN_SPLIT_POS,SettingKey.MAIN_WIN_SPLIT_GEO};
		return result;
	}
	

	@Override
	public void copyProperties(PropertyOwner o){		
		for(PropertyKey k : getPropertyKeys()){
			copyProperty(o,k);
		}
	}

	@Override
	public void copyProperty(PropertyOwner owner, PropertyKey k) {
		// TODO Auto-generated method stub

	}
	public Integer getPhotonTrajectoryCount(){
		return m_trajectoryCount.getValue();
	}
	public int getPhotonTrajectoryDisplayCount(){
		return m_trajectoryDisplayCount.getValue();
	}
	public PhotonColour getPhotonColourScheme(){
		return (PhotonColour)m_photonColour.getValue();
	}
	public boolean isPickMostProbable(){
		return m_pickMostProbable.getValue();
	}
	public boolean usePolarizationInImager(){
		return m_usePolarizationInImager.getValue();
	}
	public int getNumberWranglerThreads(){
		return m_numWranglerThreads.getValue();
	}
	
	// These are not displayed in the settings window
	public String getDefaultFilePath(){
		return m_defaultFilePath.getValue();
	}
	public int getImgWinXLoc(){
		return m_imgWinXLoc.getValue();
	}
	public int getImgWinYLoc(){
		return m_imgWinYLoc.getValue();
	}
	public int getImgWinHeight(){
		return m_imgWinHeight.getValue();
	}
	public int getImgWinWidth(){
		return m_imgWinWidth.getValue();
	}
	public int getMainWinXLoc(){
		return m_mainWinXLoc.getValue();
	}
	public int getMainWinYLoc(){
		return m_mainWinYLoc.getValue();
	}
	public int getMainWinHeight(){
		return m_mainWinHeight.getValue();
	}
	public int getMainWinWidth(){
		return m_mainWinWidth.getValue();
	}	
	public int getMainWinSplitHorizontal(){
		return m_mainWinSplitHorizontal.getValue();
	}
	public int getMainWinSplitTree(){
		return m_mainWinSplitTree.getValue();
	}
	public int getMainWinSplitPositioner(){
		return m_mainWinSplitPositioner.getValue();
	}
	public int getMainWinSplitGeometry(){
		return m_mainWinSplitGeometry.getValue();
	}
	public NumberDisplay getNumberDisplay(){
		return (NumberDisplay)m_numberDisplay.getValue();
	}
//	@Override
//	public boolean calcEquations() {
//		boolean result = true;
//		//getPositioner().calcEquations();
//		for(PropertyKey pk : getPropertyKeys()){
//			if(!calcEquation(pk)){
//				result = false;
//			}
//			
//		}
//		return result;
//		
//	}
//	@Override
//	public boolean calcEquation(PropertyKey key) {
//		boolean result = true;
//		Property p = getProperty(key);
//		if(p instanceof EquationProperty){
//			EquationProperty ep = (EquationProperty)p;
//			ep.calcValue(this, key);
//			if(ep.getValue() != UnitedValue.NAN){
//				firePropertyUpdated(key);
//				result = true;
//			}
//		}
//		return result;
//		
//	}

	@Override
	public boolean isSame(OpticsObject o) {
		return false;
	}
	@Override
	public void doInternalCalcs(){
		
	}
	
		
}
