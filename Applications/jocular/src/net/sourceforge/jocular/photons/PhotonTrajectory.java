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
package net.sourceforge.jocular.photons;

import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.photons.Photon.PhotonSource;

public class PhotonTrajectory {
	private final List<Photon> m_photons = new Vector<Photon>();
	private final List<PhotonInteraction> m_interactions = new Vector<PhotonInteraction>();
	private final List<OpticalMaterial> m_materials = new Vector<OpticalMaterial>();
	//private final List<OpticsObject> m_objects = new Vector<OpticsObject>();
	private final Stack<OpticsObject> m_inObject = new Stack<OpticsObject>();
	private int i = 0;
	private static final int MAX_PHOTON_NUMBER = 1000;
	
	public PhotonTrajectory(OpticsObject outerObject){
		//addPhoton(p, om, o, false);
		m_inObject.push(outerObject);
	}
	
	public Photon getPhoton(){
		return m_photons.get(i);
	}
	public OpticalMaterial getMaterial(){
		return m_inObject.peek().getMaterial();
		//return getMaterial(i);
	}
	/**
	 * Indicates if this photon was a result of a combined interaction.
	 * In other words, was the interaction that created this photon two interactions combined.
	 * @param i
	 * @return
	 */
	public boolean isCombined(int j){
		PhotonInteraction pi = m_interactions.get(j);
		return pi.getFromObject() != null && pi.getToObject() != null;
	}
	/**
	 * this is the last object that interacted in this trajectory
	 * @return
	 */
	public OpticsObject getObject(){
		return m_interactions.get(i).getInteractingObject();
		//return m_objects.get(i);
	}
	public int getNumberOfPhotons(){
		return i+1;
	}

	/**
	 * this is the object that the present photon is travelling through
	 * @return
	 */
	public OpticsObject getContainingObject(){
		OpticsObject result = m_inObject.peek();
		OpticsObject po = getPhoton().getContainingObject();
//		if(result != po){
//			throw new RuntimeException("Photon thinks it is in "+po + " but stack thinks it is in "+result);
//		}
		return result;
	}
	public OpticsObject getMostOuterObject(){
		return m_inObject.firstElement();
	}
	public OpticsObject getOuterObject(){
		int j = m_inObject.size() - 2;
		if(j < 0){
			return null;
			//throw new RuntimeException("No object to leave: "+i);
		}
		return m_inObject.get(j);
		
	}
	/**
	 * gets the object that is the "source" of the indexed photon. This means that it was an interaction with the returned object that "created" the photon.
	 * @param j
	 * @return
	 */
	public OpticsObject getSourceObject(int j){
		return m_interactions.get(j).getInteractingObject();
		//return m_objects.get(j);
	}
	public String getComment(int j){
		return m_interactions.get(j).getComment();
		
	}
	public Photon getPhoton(int j){
		return m_photons.get(j);
	}
	public int getInnerObjectCount(){
		return m_inObject.size();
	}
	public OpticalMaterial getMaterial(int j){
		if(j >= m_materials.size()){
			
			OpticalMaterial om = m_inObject.peek().getMaterial();
			
			return om;
		}

		return m_materials.get(j);
	}
	
	public void addPhoton(Photon p,  PhotonInteraction pi){
		
		
		
		
		if(!failedToPropagate()){
			if(i > MAX_PHOTON_NUMBER){
				throw new RuntimeException("Photon number has exceeded limit.");
			}
			if(p == null){
				throw new RuntimeException("Photon is null.  ");
			}
			//grab last material just in case

			m_interactions.add(pi);
			m_photons.add(p);
			
			//m_objects.add(pi.getInteractingObject());
			
			switch(p.getPhotonSource()){
			case REFRACTED:
				if(pi.getFromObject() != null){
					OpticsObject o1 = m_inObject.pop();
					//System.out.println("PhotonTrajectory.addPhoton exiting object: "+o1);
					if(o1 != pi.getFromObject()){
						throw new RuntimeException("Object that photon was in doesn't match one interacting with it: "+o1+" and "+pi.getInteractingObject());
					}
					
				}
				if(pi.getToObject() != null){
					if(getContainingObject() instanceof SphericalLens){
						throw new RuntimeException("Entering a lens in a lens.");
					}
					if(pi.getToObject() != getContainingObject()){
					
						m_inObject.push(pi.getToObject());
					} else {
						throw new RuntimeException("Why are we heading to an object that we're already in?");
					}
					//System.out.println("PhotonTrajectory.addPhoton entering object: "+o);
				}

				break;
			case EMITTED:
			case REFLECTED:
			case TRANSMITTED:
			case SCATTERED:
			case UNCHANGED:
			case ABSORBED:
			case LOST:
			default:
		
				//System.out.println("PhotonTrajectory.addPhoton. Photon source: "+p.getPhotonSource()+", object: "+o);
				break;
			}
			
			m_materials.add(getMaterial());
			
			i = m_photons.size()-1;
		} else {
			throw new RuntimeException("Photon already absorbed.");
		}
	}
	public Collection<Photon> getPhotons(){
		return m_photons;
	}
	public boolean isPhotonLost(){
		if(i == 0){
			return false;
		}
		PhotonSource ps = getPhoton().getPhotonSource();
		return (ps == PhotonSource.ABSORBED || ps == PhotonSource.LOST);
	}
	public boolean failedToPropagate(){
		if(i == 0){
			return false;
		}
		PhotonSource ps = getPhoton().getPhotonSource();
		return (ps == PhotonSource.ABSORBED || ps == PhotonSource.LOST || ps == PhotonSource.SCATTERED);
	}
	public String toString(){
		return "PhotonTrajectory: "+i+" photons. Last one was "+getPhoton();
	}
	public void losePhoton(){
		Photon p = getPhoton();
		// TODO: Implement better indication that photon has missed interacting
		// with any object
		Vector3D o = p.getOrigin().add(p.getDirection().scale(1));
		Vector3D d = p.getDirection();
		double w = p.getWavelength();
		Polarization pol = p.getPolarization();
		//OpticalMaterial om = getMaterial();
		
		Photon newP = new Photon(o, d, w, pol, PhotonSource.LOST, p.getIntensity(), p.getContainingObject());
		PhotonInteraction pi =new PhotonInteraction(getPhoton(),getContainingObject(), null, null, o, d, "Lost");
		addPhoton(newP, pi);
	}
	public void absorb(double distance, OpticsObject oo){
		Photon p = getPhoton();
		Vector3D o = p.getOrigin().add(p.getDirection().scale(distance));
		Vector3D d = p.getDirection();
		double w = p.getWavelength();
		Polarization pol = p.getPolarization();
		//OpticalMaterial om = getMaterial();
		
		Photon newP = new Photon(o, d, w, pol, PhotonSource.ABSORBED, p.getIntensity(), oo);
		PhotonInteraction pi =new PhotonInteraction(getPhoton(),getContainingObject(), null, null, o, d, "Absorbed"); 
		addPhoton(newP, pi);
	}

	public PhotonInteraction getInteraction() {
		return m_interactions.get(i);
	}
	public PhotonInteraction getInteraction(int j) {
		return m_interactions.get(j);
	}
	
}
