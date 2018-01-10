/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum, Bryan Matthews
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

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.Polarization;

public class ColourPixel implements Pixel {
	private ColourPoint m_redPoint;
	private ColourPoint m_bluePoint;
	private ColourPoint m_greenPoint;
	
	//private final boolean m_usePolarization;
		
	public ColourPixel(){
				
		//m_usePolarization = Settings.SETTINGS.usePolarizationInImager();
		m_redPoint = new ColourPoint();
		m_bluePoint = new ColourPoint();
		m_greenPoint = new ColourPoint();
	}
	
	@Override
	public void clear(){
		m_redPoint = new ColourPoint();
		m_bluePoint = new ColourPoint();
		m_greenPoint = new ColourPoint();
	}
	
	
	/**
	 * Adds a photon to this pixel. 
	 * @param wavelength
	 * @param p polarization - assumed to be aligned with imager
	 */
	@Override
	public void addPhoton(Photon photon, Vector3D trans){
		Polarization p = photon.getPolarization().rotate(trans);
		double w = photon.getWavelength();
		OpticsColour colour = OpticsColour.getColorFromWavelength(w);
		double i = photon.getIntensity();
		
		m_redPoint.addPhoton(colour.getRed()*i, p);
		m_bluePoint.addPhoton(colour.getBlue()*i, p);
		m_greenPoint.addPhoton(colour.getGreen()*i, p);
		
	}
	@Override
	public double getMaxMagnitude(){
		
		return Math.max(m_redPoint.getMagnitude(), Math.max(m_greenPoint.getMagnitude(),  m_bluePoint.getMagnitude()));
	}
	@Override
	public double getMaxValue(){				
		return Math.max(m_redPoint.getValue(), Math.max(m_greenPoint.getValue(),  m_bluePoint.getValue()));
		

	}
	@Override
	public OpticsColour getNormalColour(){
		
		double red = m_redPoint.getMagnitude();
		double green = m_greenPoint.getMagnitude();
		double blue = m_bluePoint.getMagnitude();
		double max = Math.max(red, Math.max(green, blue));
		OpticsColour oc = new OpticsColour(red/max, green/max, blue/max);
		return oc;
	}
	@Override
	public int getRGBMagnitude(double scale){
		int red = (int)(m_redPoint.getMagnitude()/scale*255);
		int green = (int)(m_greenPoint.getMagnitude()/scale*255);
		int blue = (int)(m_bluePoint.getMagnitude()/scale*255);			
		
		return (red << 16)|(green << 8)|blue;
	}
	@Override
	public int getRGBValue(double scale){
		
		int red = (int)(m_redPoint.getValue()/scale*255);
		int green = (int)(m_greenPoint.getValue()/scale*255);
		int blue = (int)(m_bluePoint.getValue()/scale*255);			
		
		return (red << 16)|(green << 8)|blue;

	}
	@Override
	public double getRed(boolean valueNotMagnitude){
		if(valueNotMagnitude){
			return m_redPoint.getValue();
		}
		else{
			return m_redPoint.getMagnitude();
		}
			
	}
	@Override
	public double getBlue(boolean valueNotMagnitude){
		if(valueNotMagnitude){
			return m_bluePoint.getValue();
		}
		else{
			return m_bluePoint.getMagnitude();	
		}
		
	}
	@Override
	public double getGreen(boolean valueNotMagnitude){
		if(valueNotMagnitude){
			return m_greenPoint.getValue();
		}
		else{
			return m_greenPoint.getMagnitude();
		}
	}
	@Override
	public void addColourPoints(ColourPoint[] cps){
		//for now only add the pixel if it's the same type
		if(cps.length == 3){
			m_redPoint.addColourPoint(cps[0]);
			m_bluePoint.addColourPoint(cps[1]);
			m_greenPoint.addColourPoint(cps[2]);
		}
	}
	@Override
	public ColourPoint[] getColourPoints() {
		return new ColourPoint[] {m_redPoint, m_greenPoint, m_bluePoint};
	}
	
}
