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

import java.util.ArrayList;
import java.util.Collection;

import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.AxisPositioner;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;

public class OpticsObjectGroup extends AbstractOpticsObject implements OpticsObject, OpticsObjectCombo {
	PropertyUpdatedListener m_objectListener;
	@Override
	public void setProperty(PropertyKey key, String s) {
		super.setProperty(key, s);
		switch(key){
//		case OUTSIDE_MATERIAL:
//			for(OpticsObject o : m_objects){
//				o.copyProperty(this, PropertyKey.OUTSIDE_MATERIAL);
//			}
//			break;
		default:
			break;
		}
		if(getPropertyKeys().contains(key)){
			firePropertyUpdated(key);
		}
		
	}
	

	@Override
	public void positionerUpdated() {
		super.positionerUpdated();
		for(OpticsObject o : getObjects()){
			o.updatePositioner(getPositioner());
		}
	}
//	@Override
//	public void firePropertyUpdated(PropertyKey key) {
//		super.firePropertyUpdated(key);
//		for(OpticsObject o : getObjects()){
//			o.firePropertyUpdated(key);
//		}
//	}


	@Override
	public void updatePositioner(ObjectPositioner parentPositioner) {
		super.updatePositioner(parentPositioner);
		for(OpticsObject o : getObjects()){
			o.updatePositioner(getPositioner());
		}
	}


	private final ArrayList<OpticsObject>m_objects = new ArrayList<OpticsObject>();
	//private OpticalMaterial m_outsideMaterial;
	
	public OpticsObjectGroup(){
		setPositioner(new AxisPositioner());
		m_objectListener = new PropertyUpdatedListener(){
			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				for(OpticsObject o : getObjects()){
					o.updatePositioner(getPositioner());
				}
			}
		};
	}
	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		
		for(OpticsObject o : m_objects){
			//if(pt.getObject() != o){
			o.getPossibleInteraction(pt, is);
		}
		return;
	}

//	@Override
//	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		//this should never be called because groups don't interact, just their children
//		//make sure this interaction came from this object
//		if(pi.getOpticsObject() != this){
//			return;
//		}
//		//don't bother interacting if there is no valid interaction
//		if(!pi.isValid()){
//			return;
//		}
//		//get the interaction with an object in this group
//		PhotonInteraction pi1 = pi.getWrappedInteraction();
//		OpticsObject oo = pi1.getOpticsObject();
//		oo.interact(pi1, pt);
		
		
//	}

//	@Override
//	public void setPositioner(ObjectPositioner op) {
//		
//		for(OpticsObject o : m_objects){
//			o.getPositioner().setPositioner(op);
//		}
//		super.setPositioner(op);
//	}

	/**
	 * 
	 * @param o
	 * @param pos indicates the position to add the new object. -1 makes it add to the end.
	 */
	public void add(OpticsObject o, int pos){
		//o.getPositioner().setPositioner(getPositioner());
		o.addPropertyUpdatedListener(m_objectListener);
		//o.copyProperty(this, PropertyKey.OUTSIDE_MATERIAL);
		if(pos > m_objects.size()){
			throw new RuntimeException("Index is greater than list size.");
		}
		if(pos < 0){
			m_objects.add(o);
		} else {
			m_objects.add(pos, o);
		}
		//o.getPositioner().update(getPositioner());
		
	}
	
	public void addToEnd(OpticsObject o){
		add(o, -1);
	}
	
	public void remove(OpticsObject o){
		m_objects.remove(o);
		o.removePropertyUpdatedListener(m_objectListener);
	}


	@Override
	public Collection<OpticsObject> getObjects(){
		return m_objects;
	}
	
	/**
	 * Returns all objects in the group
	 *   Removes any sub groups  
	 * @param includeSuppressedObjects TODO
	 */
	public Collection<OpticsObject> getFlattenedOpticsObjects(boolean includeSuppressedObjects){
		
		ArrayList<OpticsObject> optics = new ArrayList<OpticsObject>();
		
		for(OpticsObject optic : m_objects){
			if(includeSuppressedObjects || !optic.isSuppressed())
			if(optic instanceof OpticsObjectGroup){
				optics.addAll(((OpticsObjectGroup) optic).getFlattenedOpticsObjects(includeSuppressedObjects));
			}
			else{
				optics.add(optic);
			}			
		}
		
		return optics;
	}
	public Collection<OpticsObject> getFlattenedObjectsOfType(Class type, boolean includeSuppressedObjects){
		ArrayList<OpticsObject> optics = new ArrayList<OpticsObject>();
		
		for(OpticsObject optic : m_objects){
			if(includeSuppressedObjects || !optic.isSuppressed()){
				if(optic instanceof OpticsObjectGroup){
					optics.addAll(((OpticsObjectGroup) optic).getFlattenedObjectsOfType(type, includeSuppressedObjects));
				}
				if(optic.getClass() == type){
					optics.add(optic);
				}	
			}
		}
		
		return optics;
	}
//	public OpticsObject getObjectByName(String name){
//		OpticsObject result = null;
//		for(OpticsObject o : m_objects){
//			if(o.getProperty(PropertyKey.NAME).equals(name)){
//				result = o;
//				break;
//			} else if(o instanceof OpticsObjectGroup){
//				OpticsObjectGroup og = (OpticsObjectGroup)o;
//				OpticsObject r = og.getObjectByName(name);
//				if(r != null){
//					result = r;
//					break;
//				}
//			}
//		}
//		return result;
//	}
	@Override
	public OpticsObject makeCopy() {
		OpticsObjectGroup result = new OpticsObjectGroup();
		result.copyEverything(this);
		return result;
	}
	protected void copyEverything(OpticsObjectGroup g){
		setPositioner(g.getPositioner().makeCopy());
		copyProperties(g);
		setPositioner(g.getPositioner().makeCopy());
		for(OpticsObject o : g.getObjects()){
			add(o.makeCopy(), -1);
		}
	}
	
	public int getPos(OpticsObject o){
		return m_objects.indexOf(o);
	}
		
	public Collection<OutputObject> getOutputObjects(){
		
		ArrayList<OutputObject> os = new ArrayList<OutputObject>();
		
		for(OpticsObject optics : getFlattenedOpticsObjects(false)){
			if(optics instanceof OutputObject){
				os.add((OutputObject)optics);
			}
		}
		
		return os;
	}

	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	@Override
	public void setSelected(boolean s) {
		super.setSelected(s);
		for(OpticsObject o : m_objects){
			o.setSelected(s);
		}
	}
//	@Override
//	public boolean calcEquations() {
//		boolean result = super.calcEquations();
//		for(OpticsObject o : getObjects()){
//			if(!o.calcEquations()){
//				result = false;
//			}
//		}
//		return result;
//	}
	
	
}
