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

import net.sourceforge.jocular.math.ProbabilityResolver;
import net.sourceforge.jocular.math.Vector3D;


/**
 * Class to store the two polarization vectors and their respective phases of a photon
 * the A direction and phase is consisdered the main axis
 * the B direction and phase is orthogonol to A. The abs(A + B) should be 1
 * @author kmaccallum
 *
 */
public class Polarization {
	private final Vector3D m_dirA; //primary polarization direction
	private final Vector3D m_dirB; //secondary polarization direction this will be orthogonal to dirA
	private final double m_magA;
	private final double m_magB;
	private final double m_phaseA; //the phase in radians of the primary E field at this point. Assumes sine wave.
	private final double m_phaseB; //the phase in radians of the secondary E field at this point. Assumes sine wave.
	
	public Polarization(Vector3D a, Vector3D b, double magA, double magB, double pa, double pb){
		m_dirA = a.normalize();
		m_dirB = b.normalize();
		m_magA = magA;
		m_magB = magB;
		m_phaseA = pa;
		m_phaseB = pb;
		//check orthogonality
		if(Math.abs(m_dirA.dot(m_dirB)) > 0.0000000001){
			throw(new RuntimeException("Polarization vectors are not orthogonal"));
		}
		//check for magnitude of 1
		if(Math.abs(magA*magA + magB*magB - 1) > 0.0000000001){
			//throw(new RuntimeException("Magnitude of vectors is not 1. A: "+magA+", B: "+magB));
			System.out.println("Polarization() Magnitude of vectors is not 1. A: "+magA+", B: "+magB);
			double m = Math.sqrt(magA*magA + magB*magB);
			magA /= m;
			magB /= m;
		}
		//if(pa > Math.PI*2 || pb > Math.PI*2){
		//	throw new RuntimeException("phase is greater than 2*PI");
		//}
	}
	/**
	 * calculates an equivalent polarization so that A is aligned with v. If v is normal to a surface, then A is perpendicular and B is parallel
	 * This could probably be simplified a ton with some trig and linear algebra fussing
	 * @param v the vector to align with
	 * @return
	 */
	public Polarization rotate(Vector3D v){
		//all vectors should be in the same plane.
		//First ensure this by calculating new v in plane of a and b
		double vA = v.dot(m_dirA);
		double vB = v.dot(m_dirB);
		double m2 = (vA*vA + vB*vB);
		//if we're signifincatly normal to the surface then there's no point rotating.
		if(m2 < 0.000000000001){
			return this;
		}
		Vector3D par = m_dirA.scale(vA).add(m_dirB.scale(vB)).normalize();
		//Calc normal of that plane
		Vector3D normal = Vector3D.getDir(m_dirA, m_dirB);//.normalize();//result is naturally normal
		//compute the orthogonal vector to v
		Vector3D perp = Vector3D.getOrtho(normal, par);
		//first calc the amount of each vector to add to
		double aPar = par.dot(m_dirA)*m_magA;
		double bPar = par.dot(m_dirB)*m_magB;
		double aPerp = perp.dot(m_dirA)*m_magA;
		double bPerp = perp.dot(m_dirB)*m_magB;
		//System.out.println("Parallel Basis: " + par + ", Perpendicular Basis: "+ perp);
		
		//now calc new normal component
		double parComp = ampSinAdd(aPar, bPar, m_phaseA, m_phaseB);
		double parPhase = phaseSinAdd(aPar, bPar, m_phaseA, m_phaseB);
		
		//now calc new ortho component
		double perpComp = ampSinAdd(aPerp, bPerp, m_phaseA, m_phaseB);
		double perpPhase = phaseSinAdd(aPerp, bPerp, m_phaseA, m_phaseB);

		return (new Polarization(par, perp, parComp, perpComp, parPhase, perpPhase));
	}
	private double ampSinAdd(double amp1, double amp2, double phase1, double phase2){
		double a1 = Math.abs(amp1);
		double a2 = Math.abs(amp2);
		double p1 = (amp1 >= 0)?phase1:(phase1 + Math.PI);
		double p2 = (amp2 >= 0)?phase2:(phase2 + Math.PI);
		return Math.sqrt(a1*a1+a2*a2+2*a1*a2*Math.cos(p1 - p2));
	}
	private double phaseSinAdd(double amp1, double amp2, double phase1, double phase2){
		double a1 = Math.abs(amp1);
		double a2 = Math.abs(amp2);
		double p1 = (amp1 >= 0)?phase1:(phase1 + Math.PI);
		double p2 = (amp2 >= 0)?phase2:(phase2 + Math.PI);
		return Math.atan2(a1*Math.sin(p1)+a2*Math.sin(p2),a1*Math.cos(p1)+a2*Math.cos(p2));
	}
	public double getMagA(){
		return m_magA;
	}
	public double getMagB(){
		return m_magB;
	}
	public Vector3D getDirA(){
		return m_dirA;
	}
	public Vector3D getDirB(){
		return m_dirB;
	}
	public double getPhaseA(){
		return m_phaseA;
	}
	public double getPhaseB(){
		return m_phaseB;
	}
	/**
	 * computes a new polarization projected onto either trans or ortho using relative magnitudes of components of rotated polarization
	 * @param trans - this is the primary axis to rotate the polarization to
	 * @return - whether to resolve onto the trans axis (true) or else the ortho axis
	 */
	public Polarization resolveOntoAxis(Vector3D trans){
		Polarization pol = rotate(trans);
		double magA = pol.getMagA();
		
		//either dump all energy into magA or magB depending on probability
		if(ProbabilityResolver.resolve(magA*magA)){
			pol = new Polarization(pol.getDirA(), pol.getDirB(), 1.0d, 0.0d, pol.getPhaseA(), pol.getPhaseB());
		} else {
			pol = new Polarization(pol.getDirA(), pol.getDirB(), 0.0d, 1.0d, pol.getPhaseA(), pol.getPhaseB());
		}
		return pol;
	}
}
