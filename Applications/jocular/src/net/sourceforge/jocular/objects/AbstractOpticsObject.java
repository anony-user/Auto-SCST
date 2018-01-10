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
package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.math.OpticalCalcs;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.MaterialProperty;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.properties.StringProperty;

public abstract class AbstractOpticsObject implements OpticsObject {
	

	protected ObjectPositioner m_positioner;
	//protected MaterialProperty m_outsideMaterial;
	protected MaterialProperty m_insideMaterial;
	private StringProperty m_name;
	private boolean m_selected;
	private BooleanProperty m_suppressed;
	private OpticsID m_id = new OpticsID();
	private Collection<PropertyUpdatedListener> m_propertyUpdatedListeners = new ArrayList<PropertyUpdatedListener>();
	private PropertyUpdatedListener m_positonerListener;

	public AbstractOpticsObject(){
		m_positioner = new OffsetPositioner();
		//m_outsideMaterial = new MaterialProperty(MaterialKey.VACUUM.name());
		m_insideMaterial = new MaterialProperty(MaterialKey.VACUUM.name());
		m_suppressed = new BooleanProperty("false");
		m_name = new StringProperty("");
		m_positonerListener = new PropertyUpdatedListener(){
			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				firePropertyUpdated(PropertyKey.POSITIONER);
				positionerUpdated();
			}
			
		};
	}

	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		OpticalCalcs.calcReflectOrRefractRay(pi,pt);
	}
	/**
	 * called when this object's positioner has changed
	 */
	public void positionerUpdated(){
	}

	@Override
	public void updatePositioner(ObjectPositioner parentPositioner) {
		getPositioner().update(parentPositioner);
		
	}

	@Override
	public void setPositioner(ObjectPositioner op) {
		//ObjectPositioner pp = m_positioner.getPositioner();
		m_positioner.removePropertyUpdatedListener(m_positonerListener);
		m_positioner = op;
		m_positioner.addPropertyUpdatedListener(m_positonerListener);
		//m_positioner.setPositioner(pp);
		firePropertyUpdated(PropertyKey.POSITIONER);
	}

	@Override
	public ObjectPositioner getPositioner() {
		return m_positioner;
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s) {
		
		return new VerificationResult(true, "");
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case NAME:
			m_name = new StringProperty(s);
			break;
//		case OUTSIDE_MATERIAL:
//			m_outsideMaterial = new MaterialProperty(s);
//			break;
		case INSIDE_MATERIAL:
			m_insideMaterial = new MaterialProperty(s);
			break;
		case SUPPRESSED:
			m_suppressed = new BooleanProperty(s);
			break;
		default:
			break;
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case NAME:
			result = m_name;
			break;
//		case OUTSIDE_MATERIAL:
//			result = m_outsideMaterial;
//			break;
		case INSIDE_MATERIAL:
			result = m_insideMaterial;
			break;
		case SUPPRESSED:
			result = m_suppressed;
			break;
		default:
			result=null;
			break;
		}
		return result;
	}
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED));//, PropertyKey.OUTSIDE_MATERIAL));
		return result;
	}
	
	@Override
	public List<Property<?>> getProperties() {
		ArrayList<Property<?>> result = new ArrayList<Property<?>>();
		for(PropertyKey pk : getPropertyKeys()){
			result.add(getProperty(pk));
		}
		//ArrayList<Property<?>> result = new ArrayList<Property<?>>(asList(m_name, m_insideMaterial));
		return result;
	}


	@Override
	public OpticsObject makeCopy() {
		throw new RuntimeException(getClass().getCanonicalName()+".makeCopy not implemented yet.");
	}
	
	@Override
	public void copyProperties(PropertyOwner o){		
		for(PropertyKey k : getPropertyKeys()){
			copyProperty(o,k);
		}
	}
	@Override
	public void copyProperty(PropertyOwner owner, PropertyKey k) {
		Property<?> p = owner.getProperty(k);
		if(p != null){
			setProperty(k, p.getDefiningString());}
	}
	
	@Override
	public boolean isSelected() {
		return m_selected;
	}
	
	@Override
	public void setSelected(boolean s){
		m_selected = s;
		
		firePropertyUpdated(PropertyKey.SELECTED);
	}
	
	@Override
	public OpticsID getID(){
		return m_id;
	}
	
	@Override
	public void setID(OpticsID id){
		m_id = id;
	}
	
	@Override
	public OpticsID getLinkID(){
		return m_positioner.getID();
	}

	@Override
	public OpticalMaterial getMaterial() {
		return m_insideMaterial.getValue();
	}
	
	
	@Override
	public boolean isSuppressed() {
		return m_suppressed.getValue();
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
		
		Property p = getProperty(key);
		PropertyUpdatedEvent e = new PropertyUpdatedEvent(this, key);
		for(PropertyUpdatedListener listener : m_propertyUpdatedListeners){
			listener.propertyUpdated(e);
		}
	}

//	@Override
//	public boolean calcEquations() {
//		boolean result = true;
//		getPositioner().calcEquations();
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
	public String getName(){
		String result;
		Property p = getProperty(PropertyKey.NAME);
		if(p == null){
			result = "";
		} else {
			result = p.getDefiningString();
		}
		return result;
	}
//	@Override
//	public void addRowToArrayProperty(PropertyKey key, int i){
//		Property p = getProperty(key);
//		if(p instanceof ArrayProperty){
//			setProperty(key,((ArrayProperty)p).addRowToDefiningString(i));
//		}
//	}
//	@Override
//	public void removeRowFromArrayProperty(PropertyKey key, int i){
//		Property p = getProperty(key);
//		if(p instanceof ArrayProperty){
//			setProperty(key,((ArrayProperty)p).removeRowFromDefiningString(i));
//		}
//	}

	@Override
	public boolean isSame(OpticsObject o) {
		return o == this;
	}
	@Override
	public void doInternalCalcs(){
		
	}
	
	
}
