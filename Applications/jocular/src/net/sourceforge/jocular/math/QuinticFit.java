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



public class QuinticFit {
	private double m_a = 0;
	private double m_b = 0;
	private double m_c = 0;
	private double m_d = 0;
	private double m_e = 0;
	private double m_f = 0;
	
	public void fit(double[] x, double[] y){
		double[][] xs = new double[x.length][6];
		for(int i = 0; i < x.length; ++i){
			xs[i][0] = Math.pow(x[i],5);
			xs[i][1] = Math.pow(x[i],4);
			xs[i][2] = Math.pow(x[i],3);
			xs[i][3] = Math.pow(x[i], 2);
			xs[i][4] = x[i];
			xs[i][5] = 1.0;
		}
		Matrix my = new Matrix(y, y.length);
		Matrix mxs = new Matrix(xs);
		Matrix mxst = mxs.transpose();
		Matrix mi = mxst.times(mxs).inverse();
		Matrix mc = mi.times(mxst).times(my);
		m_a = mc.get(0, 0);
		m_b = mc.get(1, 0);
		m_c = mc.get(2, 0);
		m_d = mc.get(3, 0);
		m_e = mc.get(4, 0);
		m_f = mc.get(5, 0);
	}
	public double getValue(double x){
		return ((((m_a*x + m_b)*x+m_c)*x+m_d)*x+m_e)*x + m_f;
	}
	public double getMin(double xMin, double xMax){
		//TODO this should be solving a quartic for the quintic equation
		//find the quadratic solution to the derivative of this cubic
		//3a*x^2 + 2b*x + c
		//so x = (2b +/- sqrt(4b^2 - 12ac))/6a
		double x = xMin;
		double sqr = 4*m_b*m_b - 12*m_a*m_c;
		boolean canSolve = true;
		if(sqr > 0){
			double t2 = Math.sqrt(sqr)/6/m_a;
			double t1 = -m_b/3/m_a;
			double x1 = t1 + t2;
			double x2 = t1 - t2;
			System.out.println("CubicFit.getMin "+x1+", " + x2);
			if(x1 >= xMin && x1 <= xMax){
				x = x1;
			} else if(x2 >= xMin && x2 <= xMax){
				x = x2;
			} else {
				canSolve = false;
			}
		} else {
			canSolve = false;
		}
		double yMin = getValue(xMin);
		double yMax = getValue(xMax);
		double yX = getValue(x);
		if(yX < yMin && yX < yMax){
		} else if(yMin < yX && yMin < yMax){
			x = xMin;
		} else if(yMax < yX && yMax < yMin){
			x = xMax;
		}
		return x;
		
		
	}
	
}
