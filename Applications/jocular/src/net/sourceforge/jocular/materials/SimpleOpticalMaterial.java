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
package net.sourceforge.jocular.materials;

import java.awt.Color;
import java.util.Random;

import net.sourceforge.jocular.math.FunctionOfX;
//import net.sourceforge.jocular.math.LookupTable;
import net.sourceforge.jocular.math.ProbabilityResolver;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.Photon.PhotonSource;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.photons.Polarization;
import net.sourceforge.jocular.positioners.ObjectPositioner;

public class SimpleOpticalMaterial implements OpticalMaterial {
	public static final SimpleOpticalMaterial BOROSILICATE = new BoroSilicate();
	public static final SimpleOpticalMaterial N_BK7 = new Nbk7();
	public static final SimpleOpticalMaterial MAGNESIUM_FLUORIDE = new MagnesiumFluoride();
	public static final SimpleOpticalMaterial VACUUM = new Vacuum();
	public static final SimpleOpticalMaterial SF10 = new Sf10Material();
	public static final SimpleOpticalMaterial POLYCARBONATE = new Polycarbonate();
	public static final SimpleOpticalMaterial CALCITE = new Calcite();
	public static final SimpleOpticalMaterial TOPAS5013LS = new Topas5013LS10();
	public static final SimpleOpticalMaterial SHINYMETAL = new ShinyMetal();
	public static final SimpleOpticalMaterial F2 = new f2Material();
	public static final SimpleOpticalMaterial LBAL35 = new lBal35();
	public static final SimpleOpticalMaterial N_SF5 = new nSf5();
	public static final SimpleOpticalMaterial N_SF2 = new nSf2();
	public static final SimpleOpticalMaterial N_SK2 = new nSk2();
	public static final SimpleOpticalMaterial SF18 = new nSf18();
	
	protected final FunctionOfX m_refractiveIndexOrdinary;//this should be a function of wavelength
	protected final FunctionOfX m_refractiveIndexEx;//this should be a function of wavelength
	protected final FunctionOfX m_transmissivity;
	protected final FunctionOfX m_scatteringCoefficient;//this is essentially the probability that a photon will scatter in this material within the distance of 1mm. If null then ignored
	private static final Random random =  new Random(0);
		
	/**
	 * 
	 * @param refractiveIndex the refractive index as a function of wavelength
	 * @param transmissivity the transmissivity as a function of wavelength. This is essentially the probability of a photon being absorbed internally over a 1mm distance
	 * @param scatteringCoeff the scattering coefficient as a function of wavelength. this is essentially the probability that a photon will scatter in this material within the distance of 1mm. If null then ignored.
	 */
	public SimpleOpticalMaterial(FunctionOfX refractiveIndex, FunctionOfX transmissivity, FunctionOfX scatteringCoeff){
		m_refractiveIndexOrdinary = refractiveIndex;
		m_refractiveIndexEx = null;
		m_transmissivity = transmissivity;
		m_scatteringCoefficient = scatteringCoeff;
		
	}
	/**
	 * 
	 * @param nO the  the refractive index in the ordinary polarization direction as a function of wavelength
	 * @param nE the refractive index of the extraordinary polarization direction as a function of wavelength
	 * @param transmissivity the transmissivity as a function of wavelength. This is essentially the probability of a photon being absorbed internally over a 1mm distance
	 * @param scatteringCoeff This is essentially the probability that a photon will scatter in this material within the distance of 1mm. If null then ignored.
	 */
	public SimpleOpticalMaterial(FunctionOfX nO, FunctionOfX nE, FunctionOfX transmissivity, FunctionOfX scatteringCoeff){
		m_refractiveIndexOrdinary = nO;
		m_refractiveIndexEx = nE;
		m_transmissivity = transmissivity;
		m_scatteringCoefficient = scatteringCoeff;
		
	}	
	
	@Override
	public double getOrdinaryRefractiveIndex(double wavelength) {
		return m_refractiveIndexOrdinary.getValue(wavelength);
	}
	@Override
	public double getExtraordinaryRefractiveIndex(double wavelength) {
		return m_refractiveIndexEx.getValue(wavelength);
	}
//	@Override
//	public double getRefractiveIndex(double wavelength, Vector3D  polarizationAngle, ObjectPositioner materialPositioner) {
//		if(m_refractiveIndexEx != null){
//			double nO = m_refractiveIndexOrdinary.getValue(wavelength);
//			double nE = m_refractiveIndexEx.getValue(wavelength);
//			double a = polarizationAngle.angleBetween(materialPositioner.getTransDirection());
//			return Math.abs(Math.cos(a))*nO + Math.abs(Math.sin(a))*nE;
//		} else {
//			return m_refractiveIndexOrdinary.getValue(wavelength);
//		}
//	}
	@Override
	public double getTransmissivity(double wavelength, Vector3D  polarizationAngle, ObjectPositioner materialPositioner) {
		double result = m_transmissivity.getValue(wavelength);;
		if(result < 0){
			result = 0;
		}
		return result;
	}

	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		Photon input = pt.getPhoton();
		Vector3D p = pi.getLocation();
		
		Vector3D pStart = input.getOrigin();
		double d = p.subtract(pStart).abs();
		double pAbsorb = 1-Math.pow(m_transmissivity.getValue(input.getWavelength()), d/1e-3);//calc probability of absorption based on distance
		double pScatter = (m_scatteringCoefficient == null)? 0 : Math.pow(m_scatteringCoefficient.getValue(input.getWavelength()), d/1e-3);
		if((ProbabilityResolver.resolve(pAbsorb))){
			//System.out.println("SimpleOpticalMaterial.interact: absorbing photon.");
			pt.absorb(d*pAbsorb, null);
		} else if((ProbabilityResolver.resolve(pScatter))){
			//scatter the photon. So new polarization, new direction vector, new origin. Retain wavelength, phase
			Vector3D pDir = (new Vector3D(random.nextDouble(), random.nextDouble(), random.nextDouble())).normalize();
			Vector3D pTrans = new Vector3D(random.nextDouble(), random.nextDouble(), random.nextDouble());
			pTrans = pDir.getPerpendicularComponent(pTrans);
			Vector3D pOrtho = Vector3D.getOrtho(pDir, pTrans);
			double pPolMagTrans = random.nextDouble();
			double pPolMagOrtho = Math.sqrt(1 - pPolMagTrans*pPolMagTrans);
			Photon pOld = pt.getPhoton();
			Polarization pPolOld = pOld.getPolarization();
			Polarization pPol = new Polarization(pTrans, pOrtho, pPolMagTrans, pPolMagOrtho, pPolOld.getPhaseA(), pPolOld.getPhaseB());
			//randomly generate distance that photon is to be scattered from
			double dNew = random.nextDouble()*d;//this is actually the distance back from the intended interaction that the photon scatters
			Vector3D pOrg = pi.getLocation().subtract(pOld.getDirection().scale(dNew));
			Photon newP = new Photon(pOrg, pDir, pOld.getWavelength(), pPol, PhotonSource.SCATTERED, pOld.getIntensity(), input.getContainingObject());
			PhotonInteraction newPi =new PhotonInteraction(pt.getPhoton(),pt.getContainingObject(), null, null, pOrg, pDir, "Absorbed.");
			pt.addPhoton(newP, newPi);

		} else {
			double w = input.getWavelength();
			//rotate photon polarization to align with material axes
			
			Polarization polTranslated;
			Polarization pol;
			double dPhase;
			//if birefringent then calc each polarization component
			if(!isIsotropic()){
				Vector3D trans = pi.getInteractingObject().getPositioner().getTransDirection();
				pol = input.getPolarization().resolveOntoAxis(trans);
				//pol will either have all energy in mag A or B. These directions will align with the extrordinary (trans) or ordinary (ortho) directions of the material
				if(pol.getMagA() > 0.0d){
					dPhase = d/w*2*Math.PI*m_refractiveIndexEx.getValue(w);
				} else {
					dPhase = d/w*2*Math.PI*m_refractiveIndexOrdinary.getValue(w);
				}
				
			} else {
				pol = input.getPolarization();
				dPhase = d/w*2*Math.PI*m_refractiveIndexOrdinary.getValue(w);
				
			}
			polTranslated = new Polarization(pol.getDirA(), pol.getDirB(), pol.getMagA(), pol.getMagB(), (pol.getPhaseA() + dPhase), (pol.getPhaseB()+dPhase));
			
			
			
		
			
			 
			//Polarization pol2 = new Polarization(pol1.getDirA(), pol1.getDirB(), pol1.getMagA(), pol1.getMagB(), (pol1.getPhaseA() + dPhase), (pol1.getPhaseB()+dPhase));
			
			//now make a new photon with the new polarization
			Photon newP = new Photon(p, input.getDirection(), input.getWavelength(), polTranslated, Photon.PhotonSource.TRANSMITTED, input.getIntensity(), input.getContainingObject());
			PhotonInteraction newPi =new PhotonInteraction(pt.getPhoton(),pt.getContainingObject(), null, null, p, input.getDirection(), "Scattered.");
			pt.addPhoton(newP, newPi);
		}
		return;
	}
	
	
	public Color getColour(){
		return Color.RED;
	}
	public de.jreality.shader.Color getShaderColour(){
		Color c = getColour();
		return new de.jreality.shader.Color(c.getRed(), c.getGreen(), c.getBlue());
	}
	@Override
	public boolean isIsotropic(){
		return (m_refractiveIndexEx == null);
	}


}
