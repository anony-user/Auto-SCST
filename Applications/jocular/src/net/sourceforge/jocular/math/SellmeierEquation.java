/*******************************************************************************
 * Copyright (c) 2014, Kenneth MacCallum
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

import Jama.Matrix;
import Jama.QRDecomposition;

/**
 * Implements Sellmeier's imperical approximation for refractive index
 * n^2 - 1 = b1*lambda^2/(lambda^2 - c1) + b2*lambda^2/(lambda^2 - c2) + b3*lambda^2/(lambda^2 - c3)
 * Coefficients assume that wavelength is in microns.
 * @author kmaccallum
 *
 */
public class SellmeierEquation implements FunctionOfX {

	private final double m_b1;
	private final double m_b3;
	private final double m_b2;
	private final double m_c1;
	private final double m_c2;
	private final double m_c3;
	
	public SellmeierEquation(double b1, double b2, double b3, double c1, double c2, double c3){
		m_b1 = b1;
		m_b2 = b2;
		m_b3 = b3;
		m_c1 = c1*1.0e-12;
		m_c2 = c2*1.0e-12;
		m_c3 = c3*1.0e-12;
		
	}
	/**
	 * computes coefficients based on the provided data.
	 * 
	 * @param wavelength array of wavelength data
	 * @param index
	 * @param wavelengthMultiplier multiplier to convert wavelength data to metres, from whatever it is in: nanometers, microns, etc.
	 */
	public SellmeierEquation(double[] wavelength, double[] index, double wavelengthMultiplier) {
		if(wavelength.length != index.length){
			throw new RuntimeException("Wavelength and Index arrays are not the same length.");
		}
		double rmsError = Double.MAX_VALUE;
		int n = wavelength.length;
		Matrix residual = new Matrix(n, 1);
		Matrix lam2 = new Matrix(n, 1);
		for(int i = 0; i < n; ++i){
			residual.set(i,  0, Math.pow(index[i],2) - 1);
			lam2.set(i,  0,  Math.pow(wavelength[i]*wavelengthMultiplier, 2));
		}
		
		//compute first coeffs
		Matrix a = new Matrix(n, 2);
		Matrix y = residual.arrayTimes(lam2);
		a.setMatrix(0, n - 1, 0, 0, lam2);
		a.setMatrix(0, n - 1, 1, 1, residual);
		QRDecomposition qrd = new QRDecomposition(a);
		Matrix coeff = qrd.solve(y);
		m_b1 = coeff.get(0, 0);
		m_c1 = coeff.get(1, 0);
		
		//now compute new residual
		Matrix fit = lam2.times(m_b1);
		fit = fit.arrayRightDivide(lam2.minus(new Matrix(n, 1, m_c1)));
		residual = residual.minus(fit);
		rmsError = residual.normF();
		
		
		//now second coeffs
		a = new Matrix(n, 2);
		y = residual.arrayTimes(lam2);
		a.setMatrix(0, n - 1, 0, 0, lam2);
		a.setMatrix(0, n - 1, 1, 1, residual);
		qrd = new QRDecomposition(a);
		coeff = qrd.solve(y);
		m_b2 = coeff.get(0, 0);
		m_c2 = coeff.get(1, 0);
		
		//now compute new residual
		fit = lam2.times(m_b2);
		fit = fit.arrayRightDivide(lam2.minus(new Matrix(n, 1, m_c2)));
		residual = residual.minus(fit);
		rmsError = residual.normF();

		//now third coeffs
		a = new Matrix(n, 2);
		y = residual.arrayTimes(lam2);
		a.setMatrix(0, n - 1, 0, 0, lam2);
		a.setMatrix(0, n - 1, 1, 1, residual);
		qrd = new QRDecomposition(a);
		coeff = qrd.solve(y);
		m_b3 = coeff.get(0, 0);
		m_c3 = coeff.get(1, 0);
		
		//now compute new residual
		fit = lam2.times(m_b3);
		fit = fit.arrayRightDivide(lam2.minus(new Matrix(n, 1, m_c3)));
		residual = residual.minus(fit);
		rmsError = residual.normF();
		
	}
	@Override
	public double getValue(double x) {
		
		double x2 = Math.pow(x, 2);
		
		double result;
		result =  m_b1/(x2 - m_c1);
		result += m_b2/(x2 - m_c2);
		result += m_b3/(x2 - m_c3);
		result *= x2;
		result += 1;
		result = Math.sqrt(result);
		return result;
		
	}

}
