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

import java.awt.Color;
import java.awt.image.BufferedImage;
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
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

/**
 * @author kmaccallum
 *
 */
public class ImageSource extends AbstractOpticsObject implements LightSource {
	private EquationProperty m_meanWavelength = new EquationProperty("600nm", this, PropertyKey.WAVELENGTH_MEAN);
	private EquationProperty m_fwhmWavelength = new EquationProperty("10nm", this, PropertyKey.WAVELENGTH_FWHM);
	double m_sdWavelength = 10e-9;
	private EquationProperty m_transHalfAngleLimit = new EquationProperty("30deg", this, PropertyKey.TRANS_ANGLE);
	private EquationProperty m_orthoHalfAngleLimit = new EquationProperty("0deg", this, PropertyKey.ORTHO_ANGLE);
	private EquationProperty m_phaseFwhm = new EquationProperty("10deg", this, PropertyKey.PHASE_FWHM);
	private EquationProperty m_xSize = new EquationProperty("10cm", this, PropertyKey.ORTHO_SIZE);
	private EquationProperty m_ySize= new EquationProperty("10cm", this, PropertyKey.TRANS_SIZE);
	private ImageProperty m_image = new ImageProperty();
	private BooleanProperty m_greyScale = new BooleanProperty("true");
	private EquationProperty m_apertureDistance = new EquationProperty("10cm", this, PropertyKey.APERTURE_DISTANCE);
	private EquationProperty m_apertureDiameter = new EquationProperty("1cm", this, PropertyKey.APERTURE_DIAMETER);
	public enum ImageSourceEmissionPattern {ANGLE("Angle"), APERTURE("Aperture");
		private final String m_name;
		private ImageSourceEmissionPattern(String n){
			m_name = n;
		}
		public String toString(){
			return m_name;
		}
	}
	public enum PixelType {COLOUR("Colour"), MONO("Monochrome");
	private final String m_name;
	private PixelType(String n){
		m_name = n;
	}
	public String toString(){
		return m_name;
	}
};
/** switch between colour and black and white pixels*/	
private EnumProperty m_pixelType = new EnumProperty(PixelType.MONO, PixelType.MONO.name());
	private EnumProperty m_emissionPattern = new EnumProperty(ImageSourceEmissionPattern.ANGLE, ImageSourceEmissionPattern.ANGLE.name());
	
	private static final Random random =  new Random(0);
	
	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);

	}
	
	@Override
	public void getNextPhoton(PhotonTrajectory pt) {
		Vector3D dir = m_positioner.getDirection();
		Vector3D trans = m_positioner.getTransDirection();
		Vector3D ortho = m_positioner.getOrthoDirection();
		
		BufferedImage image = m_image.getValue();
		
		Vector3D origin = null;
		double w = 600e-9;
		double i = 0.0d;
		//figure out which pixel we're going to send
		//first choose location
		while(i == 0.0d){
			double orthoPos = random.nextDouble();
			double transPos = random.nextDouble();
			int xI = (int)Math.floor(orthoPos*(image.getWidth()));
			int yI = (int)Math.floor(transPos*(image.getHeight()));
			
			Color c = new Color(image.getRGB(xI, yI), true);
			double a = c.getAlpha();
			double b = c.getRed()*0.3/255.0+c.getGreen()*0.59/255.0+c.getBlue()*0.11/255.0;
			b *= a;
			if(m_pixelType.getValue() == PixelType.COLOUR){
				
				switch(random.nextInt(3)){
				case 0://red
					
					i = (double)(c.getRed())/255.0d*a;
					w = 625e-9;
					break;
				case 1://green
					i = (double)(c.getGreen())/255.0d*a;
					w = 525e-9;
					break;
				case 2://blue
					i = (double)(c.getBlue())/255.0d*a;
					w = 450e-9;
					break;
				}
			} else {
				w = m_meanWavelength.getValue().getBaseUnitValue();
				double wSd = m_fwhmWavelength.getValue().getBaseUnitValue()*HemiPointSource.STDEV_OVER_FWHM;
				if(wSd != 0){
					w += random.nextGaussian()*wSd;
				}
				 
				i = (double)b;
			}
			orthoPos = (orthoPos - 0.5)*m_xSize.getValue().getBaseUnitValue();
			transPos = (transPos - 0.5)*m_ySize.getValue().getBaseUnitValue();
			origin = m_positioner.getOrigin().add(trans.scale(transPos)).add(ortho.scale(orthoPos));
		}
			
		
		
		
		
		
		
		//Calculate direction of photon
		if(m_emissionPattern.getValue() == ImageSourceEmissionPattern.ANGLE){
//			//compute the photon direction based on a uniform cone of emission defined by trans and ortho half angle limit properties
//			double a = random.nextDouble()*2*Math.PI;
//			double r = Math.sqrt(random.nextDouble());//square root this to account for increased power density to smaller radii
//			double pAngle = r*Math.sin(a)*m_transHalfAngleLimit.getValue().getBaseUnitValue();
//			double pOrthoAngle = r*Math.cos(a)*m_orthoHalfAngleLimit.getValue().getBaseUnitValue();
//			
//			dir = dir.scale(Math.cos(pAngle));
//			trans = trans.scale(Math.sin(pAngle));		
//			//ortho = ortho.scale(Math.sin(pAngle));
//			
//			//trans = trans.scale(Math.cos(pOrthoAngle));
//			dir = dir.scale(Math.cos(pOrthoAngle));
//			ortho = ortho.scale(Math.sin(pOrthoAngle));
//	
//			
//			dir = dir.add(trans).add(ortho);
			dir = SourceCalcs.calcPhotonDir(m_positioner, m_orthoHalfAngleLimit.getValue().getBaseUnitValue(), m_transHalfAngleLimit.getValue().getBaseUnitValue());
		} else {
			//compute the photon direction based on all emitted light passing through an aperture defined by aperture distance and diameter properties
			double apRad = m_apertureDiameter.getValue().getBaseUnitValue()/2;
			double pointRad = apRad*random.nextDouble();
			
			double pointAngle = random.nextDouble()*2*Math.PI;
			Vector3D apPoint = m_positioner.getOrigin().add(dir.scale(m_apertureDistance.getValue().getBaseUnitValue()));
			double tScale = pointRad*Math.sin(pointAngle);
			double oScale = pointRad*Math.cos(pointAngle);
		
			dir = apPoint.add(trans.scale(tScale)).add(ortho.scale(oScale)).subtract(origin).normalize();
		}
		
		
//		dir = dir.scale(Math.cos(pAngle));
//		trans = trans.scale(Math.sin(pAngle)*Math.cos(pOrthoAngle));
//		ortho = ortho.scale(Math.sin(pAngle)*Math.sin(pOrthoAngle));
//		dir = dir.scale(Math.cos(pAngle)).add(trans).add(ortho).normalize();
		
		//calculate direction of polarization vectors
		//first get basis set
		Vector3D bTrans = dir.cross(m_positioner.getTransDirection()).normalize();
		Vector3D bOrtho = Vector3D.getOrtho(dir, bTrans);//this should already be normal
		//now generate random linear combination of basis for final vectors
		
		double rAngle = 0;//random.nextDouble()*2*Math.PI;
		
		
		
		
		//TODO: change these back at some point
		double magA = 1;//random.nextDouble();
		double magB = 0;//Math.sqrt(1 - Math.pow(magA,2));
		
		//calculate polarization phase
		double phaseA = 0;
		double phaseB = 0;
		double phaseSd = m_phaseFwhm.getValue().getBaseUnitValue() * HemiPointSource.STDEV_OVER_FWHM;
		//always use the same starting phase for now to fudge interference
//		if(phaseSd != 0){
//			phaseA = random.nextGaussian()*phaseSd;
//			phaseB = random.nextGaussian()*phaseSd;
//			//if phase FWHM is non-zero then allow random polarization angle
//			magA = random.nextDouble();
//			magB = Math.sqrt(1 - Math.pow(magA,2));
//			rAngle = random.nextDouble()*2*Math.PI;
//		}
		Vector3D dirA = bTrans.scale(Math.sin(rAngle)).add(bOrtho.scale(Math.cos(rAngle)));
		Vector3D dirB = dir.cross(dirA);
		
		
		Photon p = new Photon(origin, dir, w, new Polarization(dirA, dirB, magA, magB, phaseA, phaseB ), Photon.PhotonSource.EMITTED, i, pt.getMostOuterObject());
		//return new PhotonTrajectory(p, m_outsideMaterial.getMaterial(), null);
		PhotonInteraction newPi =new PhotonInteraction(null, this, null, null, origin, dir, "Emitted.");
		pt.addPhoton(p, newPi);
	}

	
	@Override
	public double getIntesity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
	}

	
	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		// TODO Auto-generated method stub

	}
	protected void updateDimensions(){
		
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case TRANS_SIZE:
		case ORTHO_SIZE:
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
		case TRANS_SIZE:
			m_ySize = new EquationProperty(s, this, key);
			updateDimensions();
			break;
		case ORTHO_SIZE:
			m_xSize = new EquationProperty(s, this, key);
			updateDimensions();
			break;
		case WAVELENGTH_MEAN:
			m_meanWavelength = new EquationProperty(s, this, key);
			break;
		case PHASE_FWHM:
			m_phaseFwhm = new EquationProperty(s, this, key);
			break;
		case WAVELENGTH_FWHM:
			m_fwhmWavelength = new EquationProperty(s, this, key);
			m_sdWavelength = m_fwhmWavelength.getValue().getBaseUnitValue()*HemiPointSource.STDEV_OVER_FWHM;
			break;
		case WAVELENGTH_STDEV:
			EquationProperty lp = new EquationProperty(s, this, key);
			m_fwhmWavelength = new EquationProperty(lp.getValue().getBaseUnitValue()/HemiPointSource.STDEV_OVER_FWHM);
			m_sdWavelength = m_fwhmWavelength.getValue().getBaseUnitValue()*HemiPointSource.STDEV_OVER_FWHM;
			break;
		case TRANS_ANGLE:
			m_transHalfAngleLimit = new EquationProperty(s, this, key);
			break;
		case ORTHO_ANGLE:
			m_orthoHalfAngleLimit = new EquationProperty(s, this, key);
			break;
		case IMAGE:
			m_image = new ImageProperty(s);
			break;
		case GREY_SCALE:
			m_greyScale = new BooleanProperty(s);
			break;
		case EMISSION_PATTERN:
			m_emissionPattern = new EnumProperty(ImageSourceEmissionPattern.ANGLE, s);
			break;
		case APERTURE_DIAMETER:
			m_apertureDiameter = new EquationProperty(s, this, key);
			break;
		case APERTURE_DISTANCE:
			m_apertureDistance = new EquationProperty(s, this, key);
			break;
		case PIXEL_TYPE:
			m_pixelType = new EnumProperty(PixelType.MONO,s);
			updateDimensions();
			break;
		default:
			super.setProperty(key, s);
			break;
		}
		
		if(getPropertyKeys().contains(key)){
			firePropertyUpdated(key);
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case TRANS_SIZE:
			result = m_ySize;
			break;
		case ORTHO_SIZE:
			result = m_xSize;
			break;
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
			result = new EquationProperty(m_fwhmWavelength.getValue().getBaseUnitValue()*HemiPointSource.STDEV_OVER_FWHM);
			break;
		case TRANS_ANGLE:
			result = m_transHalfAngleLimit;
			break;
		case ORTHO_ANGLE:
			result = m_orthoHalfAngleLimit;
			break;
		case IMAGE:
			result = m_image;
			break;
		case GREY_SCALE:
			result = m_greyScale;
			break;
		case EMISSION_PATTERN:
			result = m_emissionPattern;
			break;
		case APERTURE_DIAMETER:
			result = m_apertureDiameter;
			break;
		case APERTURE_DISTANCE:
			result = m_apertureDistance;
			break;
		case PIXEL_TYPE:
			result = m_pixelType;
			break;
		default:
			result = super.getProperty(key);
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.TRANS_SIZE, PropertyKey.ORTHO_SIZE, PropertyKey.IMAGE, PropertyKey.GREY_SCALE, PropertyKey.WAVELENGTH_MEAN, PropertyKey.WAVELENGTH_FWHM
				, PropertyKey.PHASE_FWHM, PropertyKey.PIXEL_TYPE, PropertyKey.EMISSION_PATTERN, PropertyKey.APERTURE_DIAMETER, PropertyKey.APERTURE_DISTANCE, PropertyKey.TRANS_ANGLE, PropertyKey.ORTHO_ANGLE));//, PropertyKey.OUTSIDE_MATERIAL));
		return result;
	}
	

	@Override
	public OpticsObject makeCopy() {
		ImageSource result = new ImageSource();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		
		return result;
	}
}
