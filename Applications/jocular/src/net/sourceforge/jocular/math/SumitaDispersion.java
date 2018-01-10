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

/**
 * Implements Sellmeier's imperical approximation for refractive index
 * n^2 - 1 = b1*lambda^2/(lambda^2 - c1) + b2*lambda^2/(lambda^2 - c2) + b3*lambda^2/(lambda^2 - c3)
 * Coefficients assume that wavelength is in microns.
 * @author kmaccallum
 *
 */
public class SumitaDispersion implements FunctionOfX {

	private final double m_a0;
	private final double m_a1;
	private final double m_a2;
	private final double m_a3;
	private final double m_a4;
	private final double m_a5;
	
	
	public SumitaDispersion(double a0, double a1, double a2, double a3, double a4, double a5){
		m_a0 = a0;
		m_a1 = a1;
		m_a2 = a2;
		m_a3 = a3;
		m_a4 = a4;
		m_a5 = a5;
		
	}

	@Override
	public double getValue(double x) {
		
		double x2 = Math.pow(x*1e6, 2);
		double x4 = x2*x2;
		double x6 = x4*x2;
		double x8 = x6*x2;
		
		double result;
		result =  m_a0;
		result += m_a1*x2;
		result += m_a2/x2;
		result += m_a3/x4;
		result += m_a4/x6;
		result += m_a5/x8;
		result = Math.sqrt(result);
		return result;
		
	}

}
