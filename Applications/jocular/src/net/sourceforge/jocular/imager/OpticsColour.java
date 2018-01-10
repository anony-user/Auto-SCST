/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
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

import java.awt.Color;

import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.PhotonColour;
import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.photons.Polarization;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;

public class OpticsColour {
	protected static final double IR_W = 700e-9;
	protected static final double RED_W = 625e-9;
	protected static final double YELLOW_W = 550e-9;
	protected static final double GREEN_W = 525e-9;
	protected static final double CYAN_W = 480e-9;
	protected static final double BLUE_W = 450e-9;
	protected static final double VIOLET_W = 400e-9;
	protected static final double UV_W = 200e-9;
	public static final OpticsColour RED = new OpticsColour(1, 0, 0);
	public static final OpticsColour GREEN = new OpticsColour(0, 1, 0);
	public static final OpticsColour BLUE = new OpticsColour(0, 0, 1);
	public static final OpticsColour WHITE = new OpticsColour(1, 1, 1);
	public static final OpticsColour BLACK = new OpticsColour(0, 0, 0);
	public static final OpticsColour CYAN = new OpticsColour(0, 1, 1);
	public static final OpticsColour MAGENTA = new OpticsColour(1, 0, 1);
	public static final OpticsColour DARK_MAGENTA = new OpticsColour(0.5, 0, 0.5);
	public static final OpticsColour YELLOW = new OpticsColour(1, 1, 0);
	public static final OpticsColour ORANGE = new OpticsColour(1, 0.5, 0);
	public static final OpticsColour GREY = new OpticsColour(0.5, 0.5, 0.5);
	public static final OpticsColour PINK = new OpticsColour(1.0, 0.5, 0.5);
	private static final OpticsColour DARK_BLUE = new OpticsColour(0, 0, 0.5);
	private static final OpticsColour PEACH = new OpticsColour(1,  0.5, 0.25);
	
	private double m_red = 0;
	private double m_green = 0;
	private double m_blue = 0;
	
	public OpticsColour(double red, double green, double blue){
		m_red = red;
		m_green = green;
		m_blue = blue;
		if(m_red > 1.0){
			throw new RuntimeException("Red is > 1.0");
		}
		if(m_green > 1.0){
			throw new RuntimeException("Green is > 1.0");
		}
		if(m_blue > 1.0){
			throw new RuntimeException("Blue is > 1.0");
		}
	}
	
	
	public double getRed(){
		return m_red;
	}
	
	
	public double getGreen(){
		return m_green;
	}
	
	
	public double getBlue(){
		return m_blue;
	}
	
	public de.jreality.shader.Color getShaderColour(){
		return new de.jreality.shader.Color((float) m_red, (float) m_green, (float) m_blue);
	}
	public java.awt.Color getAwtColor(){
		return new java.awt.Color((float) m_red, (float) m_green, (float) m_blue);
	}
	public Color getColour(){
		return new Color((float) m_red, (float) m_green, (float) m_blue);
	}
	/**
	 * returns a colour based on the specified wavelength in nanometres.
	 * @param w
	 * @return
	 */
	static public OpticsColour getColorFromWavelength(double w){
		double red = 0;
		double green = 0;
		double blue = 0;
		
		//coefficients for cubic fits to the peaks of the function
		//these were imperically derived from a picture of a spectrum on wikipedia
		final double[] r1Coeff = {1.0281E+21, -2.0444E+15, 1.3430E+9, -2.9097E+2};
		final double[] r2Coeff = {-4.5752E+21, 5.5868E+15, -2.2649E+9, 3.0495E+2};
		final double[] gCoeff = {-1.7498E+19, -8.8893E+13, 1.1016E+8, -3.0044E+1};
		final double[] bCoeff = {-1.0889E+21, 1.2198E+15, -4.3481E+8, 4.8758E+1};
		if(w > 530e-9 && w < 700e-9){
			red = getPosCubicValue(r1Coeff, w);
		} else if(w > 380e-9 && w < 520e-9){
			red = getPosCubicValue(r2Coeff, w);
		}
		if(w > 450e-9 && w < 620e-9){
			green = getPosCubicValue(gCoeff, w);
		}
		if(w > 380e-9 && w < 510e-9){
			blue = getPosCubicValue(bCoeff, w);
		}

		return new OpticsColour(red, green, blue);
	}
	/**
	 * calculates the value of a cubic equation of the form:
	 * Ax^3 + Bx^2 + Cx +D
	 * @param coeffs 4 element array where coeffs[0] = A ... coeffs[3] = D
	 * @param x
	 * @return
	 */
	private static double getPosCubicValue(double[] coeffs, double x){
		double result = coeffs[0];
		result *= x;
		result += coeffs[1];
		result *= x;
		result += coeffs[2];
		result *= x;
		result += coeffs[3];
		if(result < 0){
			result = 0;
		} else if(result > 1.0){
			result = 1.0;
		}
		return result;
	}
	
	static public OpticsColour getColorFromWavelengthOld(double w){
		double red = 0;
		double green = 0;
		double blue = 0;
		
		if(w < UV_W){
			// (w < UV_W)
			red = 0.5;
			blue = 0.5;
		} else if(w < VIOLET_W){
			//violet to UV transition (UV_W < w < VIOLET_W)
			red = 0.5;
			blue = 1 - 0.5*(VIOLET_W - w)/(VIOLET_W - UV_W);			
		} else if(w < BLUE_W){
			//blue to violet transition (VIOLET_W < w < BLUE_W)			
			blue = 1.0;
			red = 0.5*(BLUE_W - w)/(BLUE_W - VIOLET_W);
		} else if(w < CYAN_W){
			//cyan to blue transition (BLUE_W < w < CYAN_W);
			blue = 1.0;
			green = 1 - (CYAN_W - w)/(CYAN_W - BLUE_W);
		} else if(w < GREEN_W){
			//green to cyan transition (CYAN_W < w < GREEN_W)
			green = 1.0;
			blue = (GREEN_W - w)/(GREEN_W - CYAN_W);
		} else if(w < YELLOW_W){
			//yellow to green transition (GREEN < w < YELLOW_W)
			green = 1.0;
			red = (w - GREEN_W)/(YELLOW_W - GREEN_W);
		} else if(w < RED_W){
			//red to yellow transition (YELLOW_W < w < RED_W)
			red = 1.0;
			green = (RED_W - w)/(RED_W - YELLOW_W);
		} else if(w < IR_W){
			//IR transition region (RED_W < w < IR_W)
			red = 0.5 + (IR_W - w)/(IR_W - RED_W)/2.0;
		} else {
			//(w >= IR_W)
			red = 0.5;
		}
		
		return new OpticsColour(red, green, blue);	
	}
				
	static public OpticsColour getColourFromPolarization(Polarization pol){
		
		double red = 0.0;
		double green = 0.0;
		double blue = 0.0;
		
		
		
		// Simple calculation for now.  Assumes polarization plane is perpendicular
		//  to Z-Axis.
		Polarization pa = pol.resolveOntoAxis(Vector3D.Y_AXIS);
		red = pa.getMagA();
		green = pa.getMagB();
		
		
		return new OpticsColour(red, green, blue);	
	}
	public static OpticsColour getColour(PhotonTrajectory pt, int i, PhotonColour pc){
		Photon p = pt.getPhoton(i);
		OpticsColour result;
		OpticsColour[] cs = {OpticsColour.RED, OpticsColour.BLUE, OpticsColour.GREEN, OpticsColour.CYAN, OpticsColour.GREY, OpticsColour.MAGENTA, OpticsColour.BLACK, OpticsColour.ORANGE, OpticsColour.YELLOW, OpticsColour.PINK, OpticsColour.DARK_BLUE, OpticsColour.DARK_MAGENTA, OpticsColour.PEACH};
		switch(pc){
		default:
		case NUMBER_IN_SEQUENCE:
			result = cs[i % cs.length];
			break;
		case WAVELENGTH:
			double w = p.getWavelength();		
			result = OpticsColour.getColorFromWavelength(w);	
			break;
		case POLARIZATION:			
			result = OpticsColour.getColourFromPolarization(p.getPolarization());
			break;
		case MATERIAL:
			OpticalMaterial mat = pt.getMaterial(i);
			int m = MaterialKey.getKey(mat).ordinal();
			m = m % cs.length;
			result = cs[m];
			break;
		case PHOTON_SOURCE:
			switch(p.getPhotonSource()){
			case ABSORBED:
				result = OpticsColour.BLACK;
				break;
			case EMITTED:
				result = OpticsColour.YELLOW;
				break;
			case LOST:
				result = OpticsColour.GREY;
				break;
			case REFLECTED:
				result = OpticsColour.GREEN;
				break;
			case REFRACTED:
				result = OpticsColour.RED;
				break;
			case SCATTERED:
				result = OpticsColour.PINK;
				break;
			case TRANSMITTED:
				result = OpticsColour.CYAN;
				break;
			case UNCHANGED:
				result = OpticsColour.ORANGE;
				break;
			default:
				result = OpticsColour.BLUE;
				break;
			}
			break;
		}
		return result;
		
	}
	
}
