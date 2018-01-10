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
package net.sourceforge.jocular.sources;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.AbstractOpticsObject;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.photons.Polarization;
import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
/**
 * A hemispherical point source pointing in the direction of the positioner direction
 * @author tandk
 *
 */
public class HemiPointSource extends AbstractOpticsObject implements LightSource {
	public static final double STDEV_OVER_FWHM = 1.0d/(2.0d*Math.sqrt(2.0d*Math.log(2.0d)));
	private EquationProperty m_meanWavelength = new EquationProperty("600nm", this, PropertyKey.WAVELENGTH_MEAN);
	private EquationProperty m_fwhmWavelength = new EquationProperty("10nm", this, PropertyKey.WAVELENGTH_FWHM);
	double m_sdWavelength = 10e-9;
	
	private EquationProperty m_transHalfAngleLimit = new EquationProperty("30deg", this, PropertyKey.TRANS_ANGLE);
	private EquationProperty m_orthoHalfAngleLimit = new EquationProperty("30deg", this, PropertyKey.ORTHO_ANGLE);
	private EquationProperty m_phaseFwhm = new EquationProperty("10deg", this, PropertyKey.PHASE_FWHM);
	private static final Random random =  new Random(0);
	//private OpticalMaterial m_outsideMaterial;
	
	public HemiPointSource(){
		setPositioner(new OffsetPositioner());
		
	}
	double maxAngle = 0;
	
	@Override
	public void getNextPhoton(PhotonTrajectory pt) {
		Vector3D dir = SourceCalcs.calcPhotonDir(m_positioner, m_orthoHalfAngleLimit.getValue().getBaseUnitValue(), m_transHalfAngleLimit.getValue().getBaseUnitValue());
		
		
//		dir = dir.scale(Math.cos(pAngle));
//		trans = trans.scale(Math.sin(pAngle)*Math.cos(pOrthoAngle));
//		ortho = ortho.scale(Math.sin(pAngle)*Math.sin(pOrthoAngle));
//		dir = dir.scale(Math.cos(pAngle)).add(trans).add(ortho).normalize();
			
		double w = m_meanWavelength.getValue().getBaseUnitValue();
		double wSd = m_fwhmWavelength.getValue().getBaseUnitValue()*STDEV_OVER_FWHM;
		if(wSd != 0){
			w += random.nextGaussian()*wSd;
		}
		
		Photon p = new Photon(m_positioner.getOrigin(), dir, w, getNextPolarization(dir), Photon.PhotonSource.EMITTED, 1, pt.getMostOuterObject());
		PhotonInteraction newPi =new PhotonInteraction(null,this, null, null, m_positioner.getOrigin(), dir, "Emitted.");
		pt.addPhoton(p, newPi);
	}
	
	private Polarization getNextPolarization(Vector3D dir){
		
		//calculate direction of polarization vectors
		//first get basis set
		Vector3D bTrans = dir.cross(m_positioner.getTransDirection()).normalize();
		Vector3D bOrtho = Vector3D.getOrtho(dir, bTrans);//this should already be normal
		
		//now generate random linear combination of basis for final vectors
		//double rAngle = Math.PI/2;
		double rAngle = random.nextDouble()*2*Math.PI;
		
		Vector3D dirA = bTrans.scale(Math.sin(rAngle)).add(bOrtho.scale(Math.cos(rAngle)));
		Vector3D dirB = Vector3D.getOrtho(dir, dirA);
		
		//TODO: change these back at some point
		double magA = random.nextDouble();
		double magB = Math.sqrt(1 - Math.pow(magA,2));
		
		//calculate polarization phase
		double phaseA = 0;
		double phaseB = 0;
		double phaseSd = m_phaseFwhm.getValue().getBaseUnitValue() * STDEV_OVER_FWHM;
		
		//Always use the same starting phase for now to fudge interference
		if(phaseSd != 0){
			phaseA = random.nextGaussian()*phaseSd;
			phaseB = random.nextGaussian()*phaseSd;
			
			//if phase FWHM is non-zero then allow random polarization angle
			magA = random.nextDouble();
			magB = Math.sqrt(1 - Math.pow(magA,2));
		}
		
		return new Polarization(dirA, dirB, magA, magB, phaseA, phaseB );
	}


	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		// TODO Auto-generated method stub
	}
	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public double getIntesity() {
		// TODO Auto-generated method stub
		return 1;
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case WAVELENGTH_MEAN:
		case WAVELENGTH_FWHM:
		case WAVELENGTH_STDEV:
			return InputVerificationRules.verifyPositiveLength(s, this, key);
		case PHASE_FWHM:
		case TRANS_ANGLE:
		case ORTHO_ANGLE:
			return InputVerificationRules.verifyAngle(s, this, key);
			
		default:
			return super.trySetProperty(key, s);
		}
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case WAVELENGTH_MEAN:
			m_meanWavelength = new EquationProperty(s, this, key);
			break;
		case PHASE_FWHM:
			m_phaseFwhm = new EquationProperty(s, this, key);
			break;
		case WAVELENGTH_FWHM:
			m_fwhmWavelength = new EquationProperty(s, this, key);
			m_sdWavelength = m_fwhmWavelength.getValue().getBaseUnitValue()*STDEV_OVER_FWHM;
			break;
		case WAVELENGTH_STDEV:
			EquationProperty lp = new EquationProperty(s, this, key);
			m_fwhmWavelength = new EquationProperty(lp.getValue().getBaseUnitValue()/STDEV_OVER_FWHM);
			m_sdWavelength = m_fwhmWavelength.getValue().getBaseUnitValue()*STDEV_OVER_FWHM;
			break;
		case TRANS_ANGLE:
			m_transHalfAngleLimit = new EquationProperty(s, this, key);
			break;
		case ORTHO_ANGLE:
			m_orthoHalfAngleLimit = new EquationProperty(s, this, key);
			break;
		default:
			super.setProperty(key, s);
			break;
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case WAVELENGTH_MEAN:
			result = m_meanWavelength;
			break;
		case PHASE_FWHM:
			result = m_phaseFwhm;
			break;
		case WAVELENGTH_FWHM:
			result = m_fwhmWavelength;
			break;
		case WAVELENGTH_STDEV:
			result = new EquationProperty(m_fwhmWavelength.getValue().getBaseUnitValue()*STDEV_OVER_FWHM);
			break;
		case TRANS_ANGLE:
			result = m_transHalfAngleLimit;
			break;
		case ORTHO_ANGLE:
			result = m_orthoHalfAngleLimit;
			break;
		default:
			result = super.getProperty(key);
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.WAVELENGTH_MEAN, PropertyKey.WAVELENGTH_FWHM, PropertyKey.PHASE_FWHM, PropertyKey.TRANS_ANGLE, PropertyKey.ORTHO_ANGLE));//, PropertyKey.OUTSIDE_MATERIAL));
		return result;
	}


	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	@Override
	public OpticsObject makeCopy() {
		HemiPointSource result = new HemiPointSource();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}

}
