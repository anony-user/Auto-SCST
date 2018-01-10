/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
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



public class QuadraticFit {
	private Polynomial m_fit;
//	private double m_a = 0;
//	private double m_b = 0;
//	private double m_c = 0;

	
	public void fit(double[] x, double[] y){
		double[][] xs = new double[x.length][3];
		for(int i = 0; i < x.length; ++i){

			xs[i][2] = Math.pow(x[i], 2);
			xs[i][1] = x[i];
			xs[i][0] = 1.0;
		}
		Matrix my = new Matrix(y, y.length);
		Matrix mxs = new Matrix(xs);
		Matrix mxst = mxs.transpose();
		Matrix mi = mxst.times(mxs).inverse();
		Matrix mc = mi.times(mxst).times(my);
//		m_a = mc.get(0, 0);
//		m_b = mc.get(1, 0);
//		m_c = mc.get(2, 0);
		m_fit = Polynomial.makeFromCoefficients(mc.getRowPackedCopy());

	}
	public double getValue(double x){
		return m_fit.evaluate(x);
	}
//	public double getMin(double xMin, double xMax){
//		double result;
//		double x = -m_b/2/m_a;
//		double yMin = getValue(xMin);
//		double yMax = getValue(xMax);
//		double yX = getValue(x);
//		
//		if(yX < yMin && yX < yMax){
//			result = x;
//		} else if(yMin < yX && yMin < yMax){
//			result = xMin;
//		} else {
//			result = xMax;
//		}
//		
//		return result;
//		
//		
//	}
	
}
