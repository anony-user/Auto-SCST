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
package net.sourceforge.jocular.imager;

import static java.util.Arrays.asList;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Complex;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.AbstractOpticsObject;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OutputObject;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.settings.Settings;

public class Imager extends AbstractOpticsObject implements OutputObject{
	private final ArrayList<Pixel> m_pixels = new ArrayList<Pixel>();
	private EquationProperty m_xSize = new EquationProperty(0.01);
	private EquationProperty m_ySize= new EquationProperty(0.01);
	private IntegerProperty m_xDim = new IntegerProperty("100");
	private IntegerProperty m_yDim = new IntegerProperty("100");
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
	/** provide ability to either absorb photons or issue new ones as if the Imager is transparent*/
	private BooleanProperty m_absorbPhotons = new BooleanProperty("true");
	
	private double m_maxPixelValue;
	private double m_maxPixelMagnitude;
	
	public Imager(){
		setPositioner(new OffsetPositioner());	
		updateDimensions();
	}
	
	protected void updateDimensions(){
		m_pixels.clear();
		
		m_maxPixelValue = 0.0;
		m_maxPixelMagnitude = 0.0;
		
		for(int x = 0; x < m_xDim.getValue(); ++x){
			for(int y = 0; y < m_yDim.getValue(); ++y){
				if(m_pixelType.getValue() == PixelType.COLOUR){
					m_pixels.add(new ColourPixel());
				} else {
					m_pixels.add(new MonoPixel());
				}
			}
		}
		
	}

	@Override
	public OpticsObject makeCopy() {
		Imager result = new Imager();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}


	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		Photon p = pt.getPhoton();
		Vector3D intersection = Vector3D.INF;
		Vector3D yDir = getPositioner().getTransDirection();
		Vector3D xDir = Vector3D.getTrans(getPositioner().getDirection(),yDir);
		Vector3D distToImagerOrigin = getPositioner().getOrigin().subtract(p.getOrigin());
		Vector3D alongImagerNormal = distToImagerOrigin.getParallelComponent(getPositioner().getDirection());
		double d = alongImagerNormal.magSquared()/alongImagerNormal.dot(p.getDirection());
		if(d > 0){
			Vector3D vectToImagerPlane = p.getDirection().scale(d);
			Vector3D pointOnImagerPlane = vectToImagerPlane.add(p.getOrigin());
			Vector3D distAlongPlane = pointOnImagerPlane.subtract(getPositioner().getOrigin());
			double yDist = yDir.dot(distAlongPlane);
			double xDist = xDir.dot(distAlongPlane);
			//make sure the photon crosses plane within the imager size limits
			if(Math.abs(yDist) < m_ySize.getValue().getBaseUnitValue()/2 && Math.abs(xDist) < m_xSize.getValue().getBaseUnitValue()/2){
				intersection = pointOnImagerPlane;
			}
		}
		is.add(new PhotonInteraction(p, this, null, intersection, null, false, ""));
	}

	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		Vector3D yDir = getPositioner().getTransDirection();
		Vector3D xDir = getPositioner().getDirection().cross(yDir);
		Vector3D distAlongPlane = pi.getLocation().subtract(getPositioner().getOrigin());
		
		double yDist = yDir.dot(distAlongPlane);
		double xDist = xDir.dot(distAlongPlane);
	
		int y = convertToYPixelLocation(yDist);
		int x = convertToXPixelLocation(xDist);
		
		Pixel hitPixel = getPixel(x, y);
		
		if(hitPixel == null){
			throw new RuntimeException("Photon missed imager.");
		}		
//		if(x == 50 && y == 50){
//			Photon photon = pt.getPhoton();
//			Vector3D trans = getPositioner().getTransDirection();
//			Polarization p = photon.getPolarization().rotate(trans);
//			System.out.println("Imager.interact phase "+p.getPhaseB()*180/Math.PI+", "+photon.getPolarization().getDirA());
//		}
		synchronized(this){
			//Photon p = photon.
			hitPixel.addPhoton(pt.getPhoton(), getPositioner().getTransDirection());
		

			updateMaxValue(hitPixel);
		}
		

		if(m_absorbPhotons.getValue()){
			pt.absorb(0, this);
		} else {
			//make a new photon just like the incoming one
			pt.addPhoton(new Photon(pt.getPhoton()), pi);
		}
	}
	
	private int convertToXPixelLocation(double dist){
		
		// Move to center
		double xDist = dist + m_xSize.getValue().getBaseUnitValue()/2;
		// Scale to 0 - 1
		xDist /= m_xSize.getValue().getBaseUnitValue();
		// Get X pixel
		xDist *= (double) m_xDim.getValue();
		
		return (int)xDist;
	}
	
	
	private int convertToYPixelLocation(double dist){
			
		// Move to center
		double yDist = dist + m_ySize.getValue().getBaseUnitValue()/2;
		// Scale to 0 - 1
		yDist /= m_ySize.getValue().getBaseUnitValue();
		// Get Y pixel
		yDist *= (double) m_yDim.getValue();
		
		return (int)yDist;
	}
	
	public Pixel getPixel(int x, int y){
		
		if(x >= m_xDim.getValue() || y >= m_yDim.getValue() || x < 0 || y < 0){
			return null;
		}
		
		int i = x*m_yDim.getValue() + y;
		
		return m_pixels.get(i);
	}
	
	

	
	private BufferedImage getBlackScaleImage(){
		
		BufferedImage bi = new BufferedImage(m_xDim.getValue(), m_yDim.getValue(), BufferedImage.TYPE_INT_RGB);
		synchronized(this){
			boolean vNotM = Settings.SETTINGS.usePolarizationInImager();
			for(int x = 0; x < m_xDim.getValue(); ++x){
				for(int y = 0; y < m_yDim.getValue(); ++y){	
					int rgb;
				
					if(vNotM){
						rgb = getPixel(x, y).getRGBValue(m_maxPixelValue);		
					} else {
						rgb = getPixel(x, y).getRGBMagnitude(m_maxPixelMagnitude);	
					}
					
					bi.setRGB(x, y, rgb);
				}
			}
		}
		return bi;
	}
	
	public void clear(){
		//updateDimensions();
		for(Pixel p : m_pixels){
			p.clear();
		}
	}
	
	private void updateMaxValue(Pixel pixel){
		
		if(pixel.getMaxValue() > m_maxPixelValue){
			m_maxPixelValue = pixel.getMaxValue();
		}
		if(pixel.getMaxMagnitude() > m_maxPixelMagnitude){
			m_maxPixelMagnitude = pixel.getMaxMagnitude();
		}
	}

	
	public double getWidth(){
		return m_xSize.getValue().getBaseUnitValue();
	}
	public double getHeight(){
		return m_ySize.getValue().getBaseUnitValue();
	}
		
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case TRANS_SIZE:
		case ORTHO_SIZE:			
			return InputVerificationRules.verifyPositiveLength(s, this, key);
		case TRANS_DIM:
		case ORTHO_DIM:
			return InputVerificationRules.verifyPositiveInteger(s);
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
		case TRANS_DIM:
			m_yDim = new IntegerProperty(s);
			updateDimensions();
			break;
		case ORTHO_DIM:
			m_xDim = new IntegerProperty(s);
			updateDimensions();
			break;
		case ABSORBS_PHOTONS:
			m_absorbPhotons = new BooleanProperty(s);
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
		case TRANS_DIM:
			result = m_yDim;
			break;
		case ORTHO_DIM:
			result = m_xDim;
			break;
		case ABSORBS_PHOTONS:
			result = m_absorbPhotons;
			break;
		case IMAGE:
			result = new ImageProperty(getBlackScaleImage());			
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
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.PIXEL_TYPE, PropertyKey.TRANS_SIZE, PropertyKey.ORTHO_SIZE, PropertyKey.TRANS_DIM, PropertyKey.ORTHO_DIM, PropertyKey.ABSORBS_PHOTONS, PropertyKey.IMAGE));
		return result;
	}
	
//	@Override
//	public List<Property<?>> getProperties() {
//		List<Property<?>> result = super.getProperties();
//		result.addAll(asList(m_ySize, m_xSize, m_yDim, m_xDim, m_absorbPhotons));
//		return result;
//	}


	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	/**
	 * If the contents of the supplied object stream contains two ints, a boolean and a bunch of doubles,
	 * this will construct a bunch of pixels
	 * then the pixels will get added to this imager, 
	 * assuming:
	 * the first int must equal m_xDim
	 * the second int must equal m_yDim
	 * the boolean must match m_pixelType, where true == ColourPixel and false = MonoPixel
	 * If colour then there must be 9 * m_xDim*yDim doubles7
	 * 	the doubles are: mag(r), real(r), im(r), mag(g), real(g), im(g), mag(b), real(b), im(b)
	 * If mono then there must be 2 * m_xDim*yDim doubles
	 * 	the doubles are: mag(p), real(p), im(p)
	 * 
	 * @param stream
	 * @return true if it worked. False if one of the conditions above was not met
	 */
	public boolean addContents(ObjectInputStream stream) throws IOException{
		ArrayList<Double> doubles = new ArrayList<Double>();
		boolean result = true;
		boolean colourNotMono = false;
		int x = -1;
		int y = -1;
		
		x = stream.readInt();
		y = stream.readInt();
		colourNotMono = stream.readBoolean();
		PixelType pt = colourNotMono?PixelType.COLOUR:PixelType.MONO;
		
		if(y != m_yDim.getValue()){
			result = false;
			System.out.println("Imager.addContents: y Dim loaded: "+y+", should be: "+m_yDim);
		}
		if(x != m_xDim.getValue()){
			result = false;
			System.out.println("Imager.addContents: x Dim loaded: "+x+", should be: "+m_xDim);
		}
		if(pt != m_pixelType.getValue()){
			result = false;
			System.out.println("Imager.addContents: pixel type loaded: "+pt+", should be: "+m_pixelType);
		}
		try{
			while(stream.available() != -1){
				doubles.add(stream.readDouble());
			}
		} catch(EOFException e){
			
		}
			
		
		if(colourNotMono){
			if(doubles.size() != 15*x*y){
				result = false;
				System.out.println("Imager.addContents: pixel num was: "+doubles.size()+", should be: "+15*x*y);
			}
		} else {
			if(doubles.size() != 5*x*y){
				result = false;
				System.out.println("Imager.addContents: pixel num was: "+doubles.size()+", should be: "+5*x*y);
			}
		}
		if(result == true){
			ArrayList<ColourPoint> cps = new ArrayList<ColourPoint>();
			for(int j = 0; j < doubles.size(); j += 5){
				double mag = doubles.get(j);
				Complex main = new Complex(doubles.get(j+1), doubles.get(j+2));
				Complex other = new Complex(doubles.get(j+3), doubles.get(j+4));
				
				cps.add(new ColourPoint(mag, main, other));
			}
			if(colourNotMono){
				for(int j = 0; j < m_pixels.size(); ++j){
					ColourPoint cp0 = cps.get(j*3);
					ColourPoint cp1 = cps.get(j*3+1);
					ColourPoint cp2 = cps.get(j*3+2);
					Pixel p = m_pixels.get(j); 
					p.addColourPoints(new ColourPoint[] {cp0, cp1, cp2});
					updateMaxValue(p);
				}
			} else {
				for(int j = 0; j < m_pixels.size(); ++j){
					ColourPoint cp0 = cps.get(j);
					Pixel p = m_pixels.get(j); 
					p.addColourPoints(new ColourPoint[] {cp0});
					updateMaxValue(p);
				}
			}
		}
		return result;
	}
	public boolean writeContents(ObjectOutputStream stream) throws IOException{
		boolean result = true;
		
		stream.writeInt(m_xDim.getValue());
		stream.writeInt(m_yDim.getValue());
		stream.writeBoolean(m_pixelType.getValue() == PixelType.COLOUR);
		for(Pixel p : m_pixels){
			ColourPoint[] cps = p.getColourPoints();
			for(ColourPoint cp : cps){
				stream.writeDouble(cp.getMagnitude());
				stream.writeDouble(cp.getMainAxis().real());
				stream.writeDouble(cp.getMainAxis().imag());
				stream.writeDouble(cp.getOtherAxis().real());
				stream.writeDouble(cp.getOtherAxis().imag());
			}
			
		}
		
		return result;
	}
}
