/*******************************************************************************
 * Copyright (c) 2015,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.splines;

import net.sourceforge.jocular.objects.OpticsObject;

public interface SplineObject extends OpticsObject, SplineCoefficientsOwner{
	public enum PointType {CUSP, SMOOTH, SQUARE};
	/**
	 * get the number of data points defining this spline
	 * @return
	 */
	public int getSplinePointCount();
	/**
	 * get an array of the independent variable data. This is either X or Z data, depending on how spline is to be used.
	 * @return
	 */
	public double[] getSplinePointIndepValues();
	/**
	 * get an array of the dependent variable data. This is either Y or Radius data, depending on how spline is to be used.
	 * @return
	 */
	public double[] getSplinePointDepValues();
	/**
	 * get the spline coefficients for the specified segment of a parametric cubic spline
	 * The coefficients will be Ax, Bx, Cx, Dx, Ay, By, Cy, Dy.
	 * Where X(s) = Axs^3 + Bxs^2 + Cxs + Dx
	 * and Y(s) = Ays^3 + Bys^2 + Cys + Dy
	 * and s is valid for the range 0.0 to 1.0
	 * @param i
	 * @return a 2D array arranged [n][8] where the 8 are the coefficients for the nth segment
	 */
	
	public PointType[] getSplinePointTypes();


	
}
