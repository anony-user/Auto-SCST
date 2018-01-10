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



/**
 * Indicates the path of one photon
 * @author kmaccallum
 *
 */
public class Photon {
	
	public enum PhotonSource {
		EMITTED, REFLECTED, REFRACTED, ABSORBED, TRANSMITTED, UNCHANGED, LOST, SCATTERED;
	}
	private final Vector3D m_origin;
	private final Vector3D m_direction;
	private final double wavelength;
	private final Polarization m_polarization;
	private final PhotonSource m_photonSource;
	private final double m_intensity;
	public static final double H = 6.6260695729E-34;//Planck's in J*s
	public static final double HC = 1.98644568E-25;//hc in J*m
	private int reflectionCount = 0;
	private final OpticsObject m_containingObject;
	/**
	 * 
	 * @param origin
	 * @param v
	 * @param w
	 * @param p
	 * @param ps
	 * @param intensity - this deviates from the concept of a photon but it saves a ton of computation
	 */
	public Photon(Vector3D origin, Vector3D v, double w, Polarization p, PhotonSource ps, double intensity, OpticsObject containingObject){
		m_origin = origin;
		m_direction = v.normalize();
		wavelength = w;
		//test polarization direction. It must be perp to v
		
		m_polarization = p;
		m_photonSource = ps;
		//System.out.println("Photon() "+ps);
		m_intensity = intensity;
		m_containingObject = containingObject;
	}
	public Polarization getPolarization(){
		return m_polarization;
	}
	public Vector3D getOrigin(){
		return m_origin;
	}
	public Vector3D getDirection(){
		return m_direction;
	}
	public double getIntensity(){
		return m_intensity;
	}
	public double getWavelength(){
		return wavelength;
	}
	public PhotonSource getPhotonSource(){
		return m_photonSource;
	}
	/**
	 * duplicate the specified photon
	 * @param r
	 */
	public Photon(Photon r){
		this(r.getOrigin(), r.getDirection(), r.getWavelength(), r.getPolarization(), r.getPhotonSource(), r.getIntensity(), r.getContainingObject());
	}
	public OpticsObject getContainingObject() {
		return m_containingObject;
	}
	public String toString(){
		return "Photon: "+m_photonSource+" org: "+m_origin+" dir: "+m_direction+" inside: "+m_containingObject;
	}
}
