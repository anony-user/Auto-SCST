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

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsObject;

public class PhotonInteraction implements Comparable<PhotonInteraction>{
	private final Photon m_photon;
	private final OpticsObject m_toObject;
	private final OpticsObject m_fromObject;
	private final Object m_refObject;
	private final Vector3D m_location;
	private final double m_distance;
	//private final PhotonInteraction m_wrappedInteraction;
	private final Vector3D m_normal;
	//private final boolean m_fromInside;
	
	private final String m_comment;
	/**
	 * 
	 * @param p
	 * @param object
	 * @param ref a reference that an object might choose to use to help compare interactions. 
	 * @param loc
	 * @param normal
	 * @param fromInside
	 * @param comment
	 */
	public PhotonInteraction(Photon p, OpticsObject object, Object ref, Vector3D loc, Vector3D normal, boolean fromInside, String comment){
		this(p, fromInside ? object : null, fromInside ? null : object, ref, loc, normal, comment);
	}
	/**
	 * 
	 * @param p
	 * @param fromObject the object the photon is potentially going to leave, null if there isn't on (i.e. it's only entering an object)
	 * @param toObject the object the photon is potentially going to transition to, null if there isn't one (i.e. it's only leaving an object)
	 * @param loc the coordinates of the interaction
	 * @param normal the normal vector of the object surface at the interaction
	 * @param comment some text to describe what sort of interaction it is
	 */
	public PhotonInteraction(Photon p, OpticsObject fromObject, OpticsObject toObject, Object ref, Vector3D loc, Vector3D normal, String comment){
		m_photon = p;
		m_toObject = toObject;
		m_fromObject = fromObject;
		m_refObject = ref;
		m_location = loc;
		if(m_photon == null){
			m_distance = 0;
		} else {
			m_distance = m_photon.getOrigin().distanceTo(m_location);
		}
		
		if(Double.isNaN(m_distance)){
			throw new RuntimeException("Distance is NaN. Object is "+toObject.getClass().getSimpleName()+", location is "+m_location);
		}
		//m_wrappedInteraction = null;
		m_normal = normal;

		m_comment = comment;
		
		
	}
	/**
	 * combines two interactions into one (i.e. skip the intermediate material). This is only used when transitioning between two closely (really, really closely) spaced objects.
	 * Note that the from object of one must equal the to object of the other
	 * @param pi
	 * @return
	 */
	public PhotonInteraction combine(PhotonInteraction pi){
		PhotonInteraction result = null;
		
		if(pi.m_toObject == m_fromObject){
			result = new PhotonInteraction(m_photon, pi.m_fromObject, m_toObject, null, m_location, m_normal, m_comment+" (added from Object)");
		} else if(pi.m_fromObject == m_toObject){
			result = new PhotonInteraction(m_photon, m_fromObject, pi.m_toObject, null, m_location, m_normal, m_comment+" (added from Object)");
		} else {
			throw new RuntimeException("Trying to combine interactions with no common object");
		}
		return result;
	} 
	public PhotonInteraction leaveObject(OpticsObject o){
		if(m_fromObject != null){
			throw new RuntimeException("This interaction already has a from object.");
		}
		return new PhotonInteraction(m_photon, o, m_toObject, null, m_location, m_normal, m_comment+" (left "+o.getName()+")");
	}
//	public PhotonInteraction(PhotonInteraction i, OpticsObject o){
//		m_photon = i.getPhoton();
//		m_object = o;
//		m_location = i.getLocation();
//		m_distance = i.getDistance();
//		m_normal = null;
//		m_fromInside = false;
//		m_wrappedInteraction = i;
//	}
//	private Photon getPhoton(){
//		return m_photon;
//	}
	public boolean isFromInside(){
		return (m_toObject == null);
	}
	public Vector3D getLocation(){
		return m_location;
	}
	public Vector3D getNormal(){
		return m_normal;
	}
	public double getDistance(){
		return m_distance;
	}
	public boolean isCloserThan(PhotonInteraction pi){
		return (this.compareTo(pi) < 0);
	}
//	public PhotonInteraction getWrappedInteraction(){
//		return m_wrappedInteraction;
//	}
	public OpticsObject getInteractingObject(){
		return (m_toObject == null) ? m_fromObject : m_toObject;
	}
	public OpticsObject getToObject(){
		return m_toObject;
	}
	public OpticsObject getFromObject(){
		return m_fromObject;
	}
	/**
	 * compares this interaction to another
	 * if the distance to this one is closer than the parameter then the returns -1
	 * if the distance to this one is equal to that of the parameter then returns 0
	 * else 1
	 * if parameter is null then returns -1
	 */
	public int compareTo(PhotonInteraction p){
		final int result;
		if(p == null){
			result = -1;
		} else if(m_distance < p.m_distance){
			result = -1;
		} else if(m_distance > p.m_distance){
			result = 1;
		} else {
			result = 0;
		}
		return result;
	}
	/**
	 * Check if this interaction is an actual interaction
	 * @return false if there was no interaction, true if there was
	 */
	public boolean isValid(){
		return ((m_location != null) && (m_location != Vector3D.INF));
	}
	public String toString(){
		String result = "PhotonInteraction: location = ";
		if(m_location == null){
			result += "null.";
		} else {
			result += m_location;
		}
		result += ". with object: "+getInteractingObject();
		result += "."+m_comment;
		return result;
	}
	
	public String getComment(){
		return m_comment;
	}
	public boolean isInteractingObject(PhotonInteraction last) {
		boolean result = false;
		if(m_toObject == last.getToObject() || m_fromObject == last.getToObject() || m_toObject == last.getFromObject() || m_fromObject == last.getFromObject()){
			result = true;
		}
			
			
		return result;

	}
	public Object getReferenceObject(){
		return m_refObject;
	}
	
}
