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
package net.sourceforge.jocular.math;


import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.Photon.PhotonSource;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.photons.Polarization;




public class OpticalCalcs {
	/**
	 * calculates the reflected and refracted ray that results from a ray hitting a refracting surface. Uses Snell's law:
	 * sin(theta1)/sin(theta2) = n2/n1
	 * @param surfaceNormal normal vector to the surface at the point where the ray hits
	 * @param intoObject TODO
	 * @param inPhoton incoming light ray. Should already be propagated to the intersection point
	 * @param nBegin index of refraction of the material at the side of the surface that the ray is starting
	 * @param nEnd index of refraction of the material at the other side of the surface. Use Double.POSITIVE_INFINITY if surface is purely reflective
	 * @return one ray randomly generated based on the probability of where 
	 */
	public static void calcReflectOrRefractRay(PhotonInteraction pi, PhotonTrajectory pt){
		//Vector3D surfaceNormal, PhotonTrajectory pt, OpticsObject o, boolean fromInside
		Vector3D surfaceNormal = pi.getNormal();
		OpticsObject o = pi.getInteractingObject();
		boolean fromInside = pi.isFromInside();
		Photon p = pt.getPhoton();
		
		if(pt.getInnerObjectCount() <= 1 && fromInside){
			//throw new RuntimeException("how is this possible?");
			System.out.println("OpticalCalcs.calcReflectOrRefractRay photon has somehow got out of an object without properly getting out.");
			return;
		}
		OpticsObject oBegin = (pi.getFromObject() != null) ? pi.getFromObject() : pt.getContainingObject();
		OpticsObject oEnd = (pi.getToObject() != null) ? pi.getToObject() : pt.getOuterObject();
		OpticalMaterial mBegin = oBegin.getMaterial();//fromInside ? pt.getOuterObject().getMaterial() : o.getMaterial();
		OpticalMaterial mEnd = oEnd.getMaterial();//fromInside ? pt.getOuterObject().getMaterial() : o.getMaterial();
		
		//if this interface goes from one material to the same material then propagate photon without change
		if(mBegin.equals(mEnd)){
			System.out.println("OpticaCalcs.calc... starting and ending materials are the same: "+mBegin);
			//throw new RuntimeException("starting and ending materials are the same: "+mBegin);
			Photon p2 = new Photon(p.getOrigin() ,p.getDirection(), p.getWavelength(), p.getPolarization(), PhotonSource.UNCHANGED, p.getIntensity(), pi.getToObject());
			PhotonInteraction pi2 = new PhotonInteraction(p2, pi.getFromObject(), pi.getToObject(), null, pi.getLocation(), pi.getNormal(), "passed through unchanged");
			pt.addPhoton(p2,pi2);
			return;
		}
//		if(o instanceof CylindricalSurface){
//			System.out.println("OpticaCalcs.calc... object is cylinder");
//			pt.addPhoton(new Photon(p.getOrigin() , surfaceNormal, p.getWavelength(), p.getPolarization(), PhotonSource.UNCHANGED, p.getIntensity()),mEnd, o);
//			return;
//		}
		
		
		
		
		
		Vector3D n = surfaceNormal.normalize();
		double nBegin;
		double nEnd;
		
		//get starting refractive index. Assume photon is already aligned with the material it's in
		if(mBegin.isIsotropic()){
			nBegin = mBegin.getOrdinaryRefractiveIndex(p.getWavelength());
		} else {
			//if there's energy in the A polarization component then assume that there's none in B
			if(p.getPolarization().getMagA() > 0){
				nBegin = mBegin.getExtraordinaryRefractiveIndex(p.getWavelength());
			} else {//assume it's all in B
				nBegin = mBegin.getOrdinaryRefractiveIndex(p.getWavelength());
			}	
		}
		
		//Resolve photon into one of two polarization states if final material is anisotropic
		//TODO: I see a problem in that if the photon reflects then it will effectively be polarized according to the
		//material that it never even entered.
		//this would probably be ok if the conservation of energy was preserved. I'm not sure if it is.
		if(mEnd.isIsotropic()){
			nEnd = mEnd.getOrdinaryRefractiveIndex(p.getWavelength());
		} else {
			Vector3D trans = o.getPositioner().getTransDirection();
			Polarization pol = p.getPolarization().resolveOntoAxis(trans);
			//create a new photon that now has all energy in one of the polarization axes of the material
			p = new Photon(p.getOrigin(), p.getDirection(), p.getWavelength(), pol, Photon.PhotonSource.TRANSMITTED, p.getIntensity(), p.getContainingObject());
			if(pol.getMagA() > 0){
				nEnd = mEnd.getExtraordinaryRefractiveIndex(p.getWavelength());
			} else {
				nEnd = mEnd.getOrdinaryRefractiveIndex(p.getWavelength());
			}
		}
		
				
//		//first calc the transmitted angle from Snell's law 
		double cosI = Math.abs(p.getDirection().dot(n));//negative sign to account for normal pointing out of surface
//		//double thetaI = Math.toDegrees(Math.acos(cosI));
//		if(Math.abs(cosI - 1) < 1e-15){
//			cosI = 1.0;
//		}
//		double sinI = Math.sqrt(1 - Math.pow(cosI,2));
//		if(Double.isNaN(sinI)){
//			throw new RuntimeException("sinI is NaN. cosI is "+cosI);
//		}
		double sinI = p.getDirection().cross(n).abs();
		double sinT = nEnd > 0.0  ?nBegin*sinI/nEnd : Double.MAX_VALUE;//if final refractive index is zero then force a reflection
		double cosT = 0;//this will be calced later providing we're below Brewster's angle
		Polarization pAligned = p.getPolarization().rotate(n);
		boolean willReflect = false;
		double rS;
		double rP;
		double tS;
		double tP;
		if(sinT >= 1){ //if TIR then force a reflection
			willReflect = true;
			rS = 1;
			rP = 1;
			tS = 0;
			tP = 0;
		} else {
			//double thetaT = Math.toDegrees(Math.asin(sinT));
			cosT = Math.sqrt(1 - Math.pow(sinT,2));
			
			
			
			//now calc reflection and transmission coefficients from Fresnel's law
			//double intSqrt = Math.sqrt(1 - Math.pow(nBegin/nEnd*sinI,2));
			
	//		double rS = (nBegin*cosI - nEnd*cosT)/(nBegin*cosI + nEnd*cosT);
	//		double rP = (nEnd*cosI - nBegin*cosT)/(nBegin*cosT + nEnd*cosI);
	//		double tS = 1 + rS;
	//		double tP = (2*nBegin*cosI)/(nBegin*cosT + nEnd*cosI);
			
	
			
			
			
		
			rS = Math.pow((nBegin*cosI - nEnd*cosT)/(nBegin*cosI+nEnd*cosT), 2);
			rP = Math.pow((nBegin*cosT - nEnd*cosI)/(nBegin*cosT + nEnd*cosI),2);
			tS = 1 - rS;
			tP = 1 - rP;
			
			
			
			//calc new Polarization aligned with the normal vector
			
			//calc new e-field magnitudes
			//System.out.println("OpticsCalc rS,rP,tS,tP = "+rS+","+rP+","+tS+","+tP);
			//now calc reflection vs refraction probability
			
			
	//		double pReflect = pAligned.getMagA()*Math.sqrt(Math.pow(rS,2) + pAligned.getMagB()*Math.pow(rP,2));
	//		double pRefract = pAligned.getMagB()*Math.sqrt(Math.pow(tS,2) + pAligned.getMagB()*Math.pow(tP,2));
			
	
			double pReflect = rS * pAligned.getMagA() + rP * pAligned.getMagB();
			double pRefract = tS * pAligned.getMagB() + tP * pAligned.getMagB();
			
			
			//if(Math.abs(Preflect + Prefract - 1) > 0.000000001){
			//	throw new RuntimeException("Reflect/ Refract probabilities do not add to one. Preflect: "+Preflect+", Prefract: "+Prefract);
			//}
			//System.out.println("Photon.calcReflect... Preflect: "+Preflect+", Prefract: "+Prefract+", Total: "+(Preflect+Prefract));
			//now decide if it will reflect or refract
			double prob = pReflect/(pReflect+pRefract);
			//System.out.println("OpticsCalc prop = "+prob);
			willReflect = ProbabilityResolver.resolve(prob);
		} 
		
		
		
		//calc component of photon direction parallel to normal
		Vector3D pn = p.getDirection().getParallelComponent(n);//n.scale(p.getDirection().dot(n));
		//calc component perp to normal
		Vector3D po = p.getDirection().subtract(pn);
		
		Vector3D dir;//this will be the direction of the new photon
		Vector3D origin = p.getOrigin();//this will be the origin
		Vector3D dirA;//this will be the main polarization axis of the photon
		Vector3D dirB;//this will be the second polarization axis of the photon
		double magA;//this will be the magnitude of the main polarization axis
		double magB;//this will be the magnitude of the second polarization axis
		double phaseA;
		double phaseB;
		Photon.PhotonSource pSource;//this will be the source type of the photon
		//OpticalMaterial mat;//this will be the medium that the photon is in
		if(willReflect){
			//calc direction of reflected photon
			dir = po.subtract(pn);
		
			//calc new polarization vectors
			dirB = pAligned.getDirB();
			dirA = dir.cross(dirB);
			//ensure that dirA is mostly pointing in the same direction to the original
			double dot = dirA.dot(pAligned.getDirA());
			if(dot < 0){
				dirA = dirA.neg();
			}
			magA = pAligned.getMagA()*rS*rS;
			magB = pAligned.getMagB()*rP*rP;
			//fudge the magnitudes for now
			double k = Math.sqrt(magA*magA + magB*magB);
			magA /= k;
			magB /= k;
			phaseA = -pAligned.getPhaseA();
			phaseB = pAligned.getPhaseB();
			
			pSource = Photon.PhotonSource.REFLECTED;
			//mat = mBegin;
		} else {
			//calc direction of refracted photon
			
			dir = pn.normalize().scale(cosT);
			dir = dir.add(po.normalize().scale(sinT));//this should be checked to be sure po is big enough so there's no errors
			
			//calc new polarization vectors
			dirB = pAligned.getDirB();
			dirA = dir.cross(dirB);
			//ensure that dirA is mostly pointing in the same direction to the original
			double dot = dirA.dot(pAligned.getDirA());
			if(dot < 0){
				dirA = dirA.neg();
			}
			magA = pAligned.getMagA()*tS*tS;
			magB = pAligned.getMagB()*tP*tP;
			//fudge the magnitudes for now
			double k = Math.sqrt(magA*magA + magB*magB);
			magA /= k;
			magB /= k;
			phaseA = pAligned.getPhaseA();
			phaseB = pAligned.getPhaseB();
			
			pSource = Photon.PhotonSource.REFRACTED;
			
			//mat = mEnd;
			
		}
		//System.out.println("OpticalCalcs willReflect,m1,m2,m = "+willReflect+","+MaterialKey.getKey(mBegin)+","+MaterialKey.getKey(mEnd)+","+MaterialKey.getKey(mat));
		Polarization newPol = new Polarization(dirA, dirB, magA, magB, phaseA, phaseB);
		//pt.addPhoton(new Photon(origin ,dir, p.getWavelength(), newPol, pSource, p.getIntensity()), o, fromInside);
		OpticsObject co;
		if(willReflect){
			co = oBegin;
		} else {
			co = oEnd;
		}
		pt.addPhoton(new Photon(origin ,dir, p.getWavelength(), newPol, pSource, p.getIntensity(), co), pi);
	}
}
