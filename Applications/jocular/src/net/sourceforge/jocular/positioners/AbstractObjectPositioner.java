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
package net.sourceforge.jocular.positioners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsID;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;

public abstract class AbstractObjectPositioner implements ObjectPositioner {

	private OpticsID m_id = new OpticsID();
	private Collection<PropertyUpdatedListener> m_propertyUpdatedListeners = new ArrayList<PropertyUpdatedListener>();
	
	//protected ObjectPositioner m_positioner;
	private Vector3D m_origin = Vector3D.ORIGIN;
	private Vector3D m_direction = Vector3D.Z_AXIS;
	private Vector3D m_transDirection = Vector3D.Y_AXIS;
	private Vector3D m_orthoDirection = Vector3D.X_AXIS;
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s) {
		
		return new VerificationResult(true, "");
	}
	
	@Override
	public void copyProperty(PropertyOwner owner, PropertyKey k) {
		Property<?> p = owner.getProperty(k);
		if(p == null){
			return;
		}
		String s = p.getDefiningString();
		if(s == null){
			throw new RuntimeException("s is null");
		}
		setProperty(k, s);
	}
	@Override
	public void copyProperties(PropertyOwner owner){
		for(PropertyKey k : getPropertyKeys()){
			copyProperty(owner, k);
		}
	}
	
	protected Vector3D calcOrigin(ObjectPositioner parentPositioner){
		return parentPositioner.getOrigin();
		
	}
	protected Vector3D calcDirection(ObjectPositioner parentPositioner){
		return parentPositioner.getDirection();
	}
	protected Vector3D calcTransDirection(ObjectPositioner parentPositioner){
		return parentPositioner.getTransDirection();
	}
	protected Vector3D calcOrthoDirection(ObjectPositioner parentPositioner){
		return Vector3D.getOrtho(m_direction, m_transDirection);
	}
	
	@Override
	public void update(ObjectPositioner parentPositioner) {
		m_origin = calcOrigin(parentPositioner);
		m_direction = calcDirection(parentPositioner);
		m_transDirection = calcTransDirection(parentPositioner);
		m_orthoDirection = calcOrthoDirection(parentPositioner);
		if(m_origin == null){
			throw new RuntimeException("AbstractObjectPositioner.update() origin is null");
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
		PropertyUpdatedEvent e = new PropertyUpdatedEvent(this, key);
		for(PropertyUpdatedListener listener : m_propertyUpdatedListeners){
			listener.propertyUpdated(e);
		}
	}
	
	
	@Override
	public Vector3D getOrigin() {
		return m_origin;
	}
	@Override
	public Vector3D getDirection() {
		return m_direction;
	}
	@Override
	public Vector3D getTransDirection() {
		return m_transDirection;
	}
	
	@Override
	public Vector3D getOrthoDirection() {
		return m_orthoDirection;
	}
//	@Override
//	public void setPositioner(ObjectPositioner op) {
//		m_positioner = op;
//		
//	}
//	@Override
//	public ObjectPositioner getPositioner(){
//		return m_positioner;
//	}
	
	
	@Override
	public OpticsID getID(){
		return m_id;
	}
	
	@Override
	public void setID(OpticsID id){
		m_id = id;
	}	
	
//	@Override
//	public OpticsID getLinkID(){
//		return m_positioner.getID();
//	}
	@Override
	public Vector3D transform(Vector3D p) {
		Vector3D result = getOrigin();
		result = result.add(getDirection().scale(p.z));
		result = result.add(getTransDirection().scale(p.y));
		result = result.add(getOrthoDirection().scale(p.x));
		return result;
	}
	@Override
	public List<Property<?>> getProperties() {
		ArrayList<Property<?>> result = new ArrayList<Property<?>>();
		for(PropertyKey pk : getPropertyKeys()){
			result.add(getProperty(pk));
		}
		return result;
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
		return o.getPositioner() == this;
	}
	@Override
	public void doInternalCalcs(){
		
	}
	
	
}
