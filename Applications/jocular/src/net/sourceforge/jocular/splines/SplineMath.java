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

import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;
import net.sourceforge.jocular.splines.SplineObject.PointType;

public class SplineMath {
	public enum BoundaryCondition {EXTRAPOLATE_2ND_DERIVATIVE, SMOOTH_MIRROR, SMOOTH_CLOSED}
	public enum EndPointType {CUSP, SMOOTH, VERT, HORIZ};
	private static final double SEGMENT_NUM = 20;
	//private static Matrix fit(double[] xs, double[] ys, BoundaryCondition bc0, BoundaryCondition bcn, boolean closedCurve){
	private static Matrix fit(double[] xs, double[] ys, EndPointType pt0, EndPointType ptn){
		EndPointType apt0 = pt0;
		
		EndPointType aptn = ptn;
		
		
		boolean closedCurve = (apt0 == EndPointType.SMOOTH) || (ptn == EndPointType.SMOOTH);
		if(xs.length != ys.length){
			throw new RuntimeException("X and Y data lengths are not equal: "+xs.length+", "+ys.length);
		}
		if(xs.length < 2){
			throw new RuntimeException("There must be at least 2 points to compute the spline. There is only "+xs.length +" point to fit.");
		}
		final int splineNum;
		boolean linearOrQuadratic = false;
		if(closedCurve){
			splineNum = xs.length;//we've got a continuous splined curve
		} else if(apt0 == EndPointType.CUSP && aptn == EndPointType.CUSP && xs.length <= 3){
			splineNum = 1;//we've got a single spline segment that will be fit to 2 or 3 points
			linearOrQuadratic = true;
		} else {
			//everything else, let's fit with a complete suite of points
			splineNum = xs.length -1;
		}
		
		//final int splineNum = closedCurve ? xs.length : ((xs.length == 3) ? 1 : xs.length - 1);//number of splines
		
		
		int n = splineNum;
		
		 n *= 8;//the number of rows in the matrix
		int m = n;//the number of columns in the matrix
		//set up matrices of the form b = a*c, where b and c are column vectors and a is square
		//c will be a column vector of all the solution coefficients
		Matrix a = new  Matrix(m,n, 0.0);
		Matrix c = new Matrix(m,1,0.0);
		Matrix result;
		int r = 0;
		if(linearOrQuadratic){
			if(xs.length == 2){
				//special case, there's only 2 points. A and B coefficients will be zero
//				a = new Matrix(8,8,0.0);
//				c = new Matrix(8,1,0.0);
				
	//			c.set(0, 0, 0.0);
	//			c.set(1, 0, 0.0);
				c.set(2, 0, xs[0]);
				c.set(3, 0, xs[1]);
	//			c.set(4, 0, 0.0);
	//			c.set(5, 0, 0.0);
				c.set(6, 0, ys[0]);
				c.set(7, 0, ys[1]);
				
				//1st row Ax = 0
				a.set(0, 0, 1.0);
				
				//2nd row Bx = 0
				a.set(1, 1, 1.0);
				
				//3rd row Dx = xs[0]
				a.set(2, 3, 1.0);
				
				//4th row Cx + Dx = xs[1]
				a.set(3, 2, 1.0);
				a.set(3, 3, 1.0);
				
				//5th row Ay = 0
				a.set(4, 4, 1.0);
				
				//6th row Bx = 0
				a.set(5, 5, 1.0);
				
				//7th row Dx = xs[0]
				a.set(6, 7, 1.0);
				
				//8th row Cx + Dx = xs[1]
				a.set(7, 6, 1.0);
				a.set(7, 7, 1.0);
				
				//System.out.println("SplineMath.fit. Array: "+a);
			} else {
				//this should be the case where there are three spline points fit with a quadratic

//				a = new Matrix(8,8,0.0);
//				c = new Matrix(8,1,0.0);
				
	//			c.set(0, 0, 0.0);
				c.set(1, 0, xs[0]);
				c.set(2, 0, xs[1]);
				c.set(3, 0, xs[2]);
	//			c.set(4, 0, 0.0);
				c.set(5, 0, ys[0]);
				c.set(6, 0, ys[1]);
				c.set(7, 0, ys[2]);
				
				//1st row Ax = 0
				a.set(0, 0, 1.0);
				
				//2nd row D = xs[0] 
				a.set(1, 3, 1.0);
				
				//3rd row Bx(0.5)^2 + Cx(0.5) + D = xs[1]
				a.set(2, 1, 0.25);
				a.set(2, 2, 0.5);
				a.set(2, 3, 1.0);
				
				//4th row Bx(1)^2 + Cx(1) + D = xs[2]
				a.set(3, 1, 1.0);
				a.set(3, 2, 1.0);
				a.set(3, 3, 1.0);
				
				//5th row Ax = 0
				a.set(4, 4, 1.0);
				
				//6th row D = xs[0] 
				a.set(5, 7, 1.0);
				
				//7th row Bx(0.5)^2 + Cx(0.5) + D = xs[1]
				a.set(6, 5, 0.25);
				a.set(6, 6, 0.5);
				a.set(6, 7, 1.0);
				
				//8th row Bx(1)^2 + Cx(1) + D = xs[2]
				a.set(7, 5, 1.0);
				a.set(7, 6, 1.0);
				a.set(7, 7, 1.0);
				
				
				//System.out.println("SplineMath.fit. Array: "+a);
			}
		} else {
			//this is the normal case
			
			for(int i = 0; i < splineNum; ++i){//step through segments
				//first a row for the spline X values when s = 0
				c.set(r, 0, xs[i]);
				a.set(r, i*8 + 0, 0.0);
				a.set(r, i*8 + 1, 0.0);
				a.set(r, i*8 + 2, 0.0);
				a.set(r, i*8 + 3, 1.0);
				++r;
				//now a row for the spline Y values when s = 0
				c.set(r, 0, ys[i]);
				a.set(r, i*8 + 4, 0.0);
				a.set(r, i*8 + 5, 0.0);
				a.set(r, i*8 + 6, 0.0);
				a.set(r, i*8 + 7, 1.0);
				++r;
				int in = i+1;
				if(in >= xs.length){
					in = 0;
				}
				//now a row for the spline X values when s = 1
				c.set(r, 0, xs[in]);
				a.set(r, i*8 + 0, 1.0);
				a.set(r, i*8 + 1, 1.0);
				a.set(r, i*8 + 2, 1.0);
				a.set(r, i*8 + 3, 1.0);
				++r;
				//now a row for the spline Y values when s = 1
				c.set(r, 0, ys[in]);
				a.set(r, i*8 + 4, 1.0);
				a.set(r, i*8 + 5, 1.0);
				a.set(r, i*8 + 6, 1.0);
				a.set(r, i*8 + 7, 1.0);
				++r;
				
				if(i < splineNum - 1){//can't apply this constraint for the last segment
					//now a row for the dXi(1)/ds = dXi+1(0)/ds values. I.e. first derivatives equal where splines meet.
					
					
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 0, 3.0);
					a.set(r, (i + 0)*8 + 1, 2.0);
					a.set(r, (i + 0)*8 + 2, 1.0);
					a.set(r, (i + 0)*8 + 3, 0.0);
					a.set(r, (i + 1)*8 + 0, -0.0);
					a.set(r, (i + 1)*8 + 1, -0.0);
					a.set(r, (i + 1)*8 + 2, -1.0);
					a.set(r, (i + 1)*8 + 3, -0.0);
					++r;
					//now a row for the dYi(1)/ds = dYi+1(0)/ds values. I.e. first derivatives equal where splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 4, 3.0);
					a.set(r, (i + 0)*8 + 5, 2.0);
					a.set(r, (i + 0)*8 + 6, 1.0);
					a.set(r, (i + 0)*8 + 7, 0.0);
					a.set(r, (i + 1)*8 + 4, -0.0);
					a.set(r, (i + 1)*8 + 5, -0.0);
					a.set(r, (i + 1)*8 + 6, -1.0);
					a.set(r, (i + 1)*8 + 7, -0.0);
					++r;
					//now a row for dXi^2(1)/d^2i = dXi+1^2(0)/d^2i values. I.e. second derivates equal wher splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 0, 6.0);
					a.set(r, (i + 0)*8 + 1, 2.0);
					a.set(r, (i + 0)*8 + 2, 0.0);
					a.set(r, (i + 0)*8 + 3, 0.0);
					a.set(r, (i + 1)*8 + 0, -0.0);
					a.set(r, (i + 1)*8 + 1, -2.0);
					a.set(r, (i + 1)*8 + 2, -0.0);
					a.set(r, (i + 1)*8 + 3, -0.0);
					++r;
					//now a row for dYi^2(1)/d^2i = dYi+1^2(0)/d^2i values. I.e. second derivates equal wher splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 4, 6.0);
					a.set(r, (i + 0)*8 + 5, 2.0);
					a.set(r, (i + 0)*8 + 6, 0.0);
					a.set(r, (i + 0)*8 + 7, 0.0);
					a.set(r, (i + 1)*8 + 4, -0.0);
					a.set(r, (i + 1)*8 + 5, -2.0);
					a.set(r, (i + 1)*8 + 6, -0.0);
					a.set(r, (i + 1)*8 + 7, -0.0);
					++r;			
				} else if(closedCurve){
					//if it's a closed curve then we have an additional segment from the last point back to the first
					
					//now a row for the dXn(1)/ds = dX0(0)/ds values. I.e. first derivatives equal where splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 0, 3.0);
					a.set(r, (i + 0)*8 + 1, 2.0);
					a.set(r, (i + 0)*8 + 2, 1.0);
					a.set(r, (i + 0)*8 + 3, 0.0);
					a.set(r, (0)*8 + 0, -0.0);
					a.set(r, (0)*8 + 1, -0.0);
					a.set(r, (0)*8 + 2, -1.0);
					a.set(r, (0)*8 + 3, -0.0);
					++r;
					//now a row for the dYi(1)/ds = dYi+1(0)/ds values. I.e. first derivatives equal where splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 4, 3.0);
					a.set(r, (i + 0)*8 + 5, 2.0);
					a.set(r, (i + 0)*8 + 6, 1.0);
					a.set(r, (i + 0)*8 + 7, 0.0);
					a.set(r, (0)*8 + 4, -0.0);
					a.set(r, (0)*8 + 5, -0.0);
					a.set(r, (0)*8 + 6, -1.0);
					a.set(r, (0)*8 + 7, -0.0);
					++r;
					//now a row for dXi^2(1)/d^2i = dXi+1^2(0)/d^2i values. I.e. second derivates equal wher splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 0, 6.0);
					a.set(r, (i + 0)*8 + 1, 2.0);
					a.set(r, (i + 0)*8 + 2, 0.0);
					a.set(r, (i + 0)*8 + 3, 0.0);
					a.set(r, (0)*8 + 0, -0.0);
					a.set(r, (0)*8 + 1, -2.0);
					a.set(r, (0)*8 + 2, -0.0);
					a.set(r, (0)*8 + 3, -0.0);
					++r;
					//now a row for dYi^2(1)/d^2i = dYi+1^2(0)/d^2i values. I.e. second derivates equal wher splines meet.
					c.set(r, 0, 0.0);
					a.set(r, (i + 0)*8 + 4, 6.0);
					a.set(r, (i + 0)*8 + 5, 2.0);
					a.set(r, (i + 0)*8 + 6, 0.0);
					a.set(r, (i + 0)*8 + 7, 0.0);
					a.set(r, (0)*8 + 4, -0.0);
					a.set(r, (0)*8 + 5, -2.0);
					a.set(r, (0)*8 + 6, -0.0);
					a.set(r, (0)*8 + 7, -0.0);
					++r;			
				}
				
			}
			if(!closedCurve){
				//now do the boundary conditions
				//namely i = 0 and i = pn -1 (bc0 and bcn)
				//switch(bcn){
				switch(aptn){
				//case EXTRAPOLATE_2ND_DERIVATIVE:
				case CUSP:
					if(splineNum == 1){
						//if there's only one spline then we can't we don't have enough points to solve for A
						c.set(r, 0, 0.0);
						a.set(r, 0, 1.0);
						++r;
						c.set(r, 0, 0.0);
						a.set(r, 4, 1.0);
						++r;
					} else {
						//2nd derivative is extrapolated from 2nd spline to 1st
						c.set(r, 0, 0.0);
						a.set(r, (splineNum - 1)*8 + 0, 6.0);
						a.set(r, (splineNum - 1)*8 + 1, 2.0);
						a.set(r, (splineNum - 1)*8 + 2, 0.0);
						a.set(r, (splineNum - 1)*8 + 3, 0.0);
						a.set(r, (splineNum - 2)*8 + 0, -12.0);
						a.set(r, (splineNum - 2)*8 + 1, -2.0);
						a.set(r, (splineNum - 2)*8 + 2, 0.0);
						a.set(r, (splineNum - 2)*8 + 3, 0.0);
						++r;
						c.set(r, 0, 0.0);
						a.set(r, (splineNum - 1)*8 + 4, 6.0);
						a.set(r, (splineNum - 1)*8 + 5, 2.0);
						a.set(r, (splineNum - 1)*8 + 6, 0.0);
						a.set(r, (splineNum - 1)*8 + 7, 0.0);
						a.set(r, (splineNum - 2)*8 + 4, -12.0);
						a.set(r, (splineNum - 2)*8 + 5, -2.0);
						a.set(r, (splineNum - 2)*8 + 6, 0.0);
						a.set(r, (splineNum - 2)*8 + 7, 0.0);
						++r;
					}
					break;
				//case SMOOTH_MIRROR:
//				case SQUARE:
//					throw new RuntimeException("This should not happen.");
				case VERT:
					c.set(r, 0, 0);
					a.set(r, (splineNum - 1)*8 + 0, 3.0);
					a.set(r, (splineNum - 1)*8 + 1, 2.0);
					a.set(r, (splineNum - 1)*8 + 2, 1.0);
					a.set(r, (splineNum - 1)*8 + 3, 0.0);
					++r;
					
					c.set(r, 0, 0.0);
					a.set(r, (splineNum - 1)*8 + 4, 6.0);
					a.set(r, (splineNum - 1)*8 + 5, 2.0);
					a.set(r, (splineNum - 1)*8 + 6, 0.0);
					a.set(r, (splineNum - 1)*8 + 7, 0.0);
					++r;
					break;
				case HORIZ:
					
					//now y coeffs
					c.set(r, 0, 0);
					a.set(r, (splineNum - 1)*8 + 4, 3.0);
					a.set(r, (splineNum - 1)*8 + 5, 2.0);
					a.set(r, (splineNum - 1)*8 + 6, 1.0);
					a.set(r, (splineNum - 1)*8 + 7, 0.0);
					++r;
					
					c.set(r, 0, 0.0);
					//a.set(r, (splineNum - 1)*8 + 1, 1.0);
					a.set(r, (splineNum - 1)*8 + 0, 6.0);
					a.set(r, (splineNum - 1)*8 + 1, 2.0);
					a.set(r, (splineNum - 1)*8 + 2, 0.0);
					a.set(r, (splineNum - 1)*8 + 3, 0.0);
					++r;
						
					break;
					
			
					
					
					
				}
				//switch(bc0){
				switch(apt0){
				//case EXTRAPOLATE_2ND_DERIVATIVE:
				case CUSP: 
					//2nd derivative is extrapolated from 2nd spline to 1st
					if(splineNum == 1){
						//if there's only one spline then we can't we don't have enough points to solve for A
						c.set(r, 0, 0.0);
						a.set(r, 0, 1.0);
						++r;
						c.set(r, 0, 0.0);
						a.set(r, 4, 1.0);
						++r;
					} else {
						c.set(r, 0, 0.0);
						a.set(r, (0)*8 + 0, 0.0);
						a.set(r, (0)*8 + 1, 2.0);
						a.set(r, (0)*8 + 2, 0.0);
						a.set(r, (0)*8 + 3, 0.0);
						a.set(r, (1)*8 + 0, 6.0);
						a.set(r, (1)*8 + 1, -2.0);
						a.set(r, (1)*8 + 2, 0.0);
						a.set(r, (1)*8 + 3, 0.0);
						++r;
						c.set(r, 0, 0.0);
						a.set(r, (0)*8 + 4, 0.0);
						a.set(r, (0)*8 + 5, 2.0);
						a.set(r, (0)*8 + 6, 0.0);
						a.set(r, (0)*8 + 7, 0.0);
						a.set(r, (1)*8 + 4, 6.0);
						a.set(r, (1)*8 + 5, -2.0);
						a.set(r, (1)*8 + 6, 0.0);
						a.set(r, (1)*8 + 7, 0.0);
						++r;
					}
					break;
					
				//case SMOOTH_MIRROR:
//				case SQUARE:
//					throw new RuntimeException("Should not happen here.");
	
				case VERT:
					//first x coefficients to solve for dx/ds
					c.set(r, 0, 0.0);
					a.set(r, (0)*8 + 0, 0.0);
					a.set(r, (0)*8 + 1, 0.0);
					a.set(r, (0)*8 + 2, 1.0);
					a.set(r, (0)*8 + 3, 0.0);
					++r;
					
					c.set(r, 0, 0.0);
					a.set(r, (0)*8 + 4, 0.0);
					a.set(r, (0)*8 + 5, 2.0);
					a.set(r, (0)*8 + 6, 0.0);
					a.set(r, (0)*8 + 7, 0.0);
					
					++r;
					break;
				case HORIZ:
					//now y coeffs
					c.set(r, 0, 0.0);
					a.set(r, (0)*8 + 4, 0.0);
					a.set(r, (0)*8 + 5, 0.0);
					a.set(r, (0)*8 + 6, 1.0);
					a.set(r, (0)*8 + 7, 0.0);
					++r;
					
					c.set(r, 0, 0.0);
					a.set(r, (0)*8 + 0, 0.0);
					a.set(r, (0)*8 + 1, 2.0);
					a.set(r, (0)*8 + 2, 0.0);
					a.set(r, (0)*8 + 3, 0.0);
					
					++r;
					
				
				
				
				
					break;
				}
			}
		}
		
		//System.out.println("SplineMath.fit a = "+matrixToString(a));
		//System.out.println("SplineMath.fit c = "+matrixToString(c));
		try {
			result = a.inverse().times(c);
			//System.out.println("SplineMath.fit result = "+matrixToString(result));
			
		} catch(RuntimeException e){
			
			throw e;
		}

		
		return result;
	}
	/**
	 * will see if we can use a lower quality fit with fewer points
	 * @param xs
	 * @param ys
	 * @param pt0
	 * @param ptn
	 * @param error
	 * @return
	 */
	private static List<SplineCoefficients> approxFit(double[] xs, double[] ys, EndPointType pt0, EndPointType ptn, double error){
		//double error = 1e-12;
		List<SplineCoefficients> result = new ArrayList<SplineCoefficients>();
		if(error == 0.0){//|| pt0 == PointType.SQUARE || ptn == PointType.SQUARE){
			return convertCoefficients(fit(xs, ys, pt0, ptn));
		}
		EndPointType apt0 = pt0;
		EndPointType aptn = ptn;
		
		//setup
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		indexes.add(0);
		indexes.add(xs.length - 1);
		boolean done = false;
		while(!done){
			//fit to specified points
			double[] nxs = new double[indexes.size()];
			double[] nys = new double[indexes.size()];
			int k = 0;
			for(Integer i : indexes){
				nxs[k] = xs[i];
				nys[k] = ys[i];
				++k;
				
			}
			result = convertCoefficients(fit(nxs, nys, apt0, aptn));
			
			
			//check error
			double maxErrSq = 0.0;
			int maxErrIndex = -1;
			//for each data point
			for(int i = 0; i < xs.length; ++i){
				double minErrSq = Double.MAX_VALUE;
				//find the best fit for the series of splines we're testing now
				for(int j = 0; j < result.size(); ++j){
					SplineCoefficients sc = result.get(j);
					double s = sc.bestFitParameter(xs[i], ys[i]);
					double ex = sc.calcX(s) - xs[i];
					double ey = sc.calcY(s) - ys[i];
					double errSq = ex*ex + ey*ey;
					if(errSq < minErrSq){
						minErrSq = errSq;
					} else if(Double.isNaN(errSq)){
						throw new RuntimeException("Err is NaN.");
					}
				}
				if(maxErrSq < minErrSq){
					maxErrSq = minErrSq;
					if(maxErrSq == Double.MAX_VALUE){
						throw new RuntimeException("Somehow we're setting the maximum computed error to MAX_VALUE.");
						
					}
					maxErrIndex = i;
				}
			}
			
			if(maxErrSq > error*error){
				done = false;
				//add index of maximum error to the indexes list
				for(int j = 0; j < indexes.size(); ++j){
					if(indexes.get(j) > maxErrIndex){
						indexes.add(j, maxErrIndex);
						break;
					} else if(indexes.get(j) == maxErrIndex){
						int m;
						
						if(j == 0){
							m = 0;
						} else {
							m = (indexes.get(j - 1) + indexes.get(j))/2;
						}
						indexes.add(j, m);
						break;
					}
				}
			} else {
				done = true;
			}
		}
		
		
		
		
		return result;
	
	}
	
	private static String matrixToString(Matrix a){
		String s = "[";
		boolean rowStart = true;
		for(int rc = 0; rc < a.getRowDimension(); ++rc){
			rowStart = true;
			for(int cc = 0; cc < a.getColumnDimension(); ++cc){
				if(!rowStart){
					
					s += ",";
					
				
				}
				rowStart = false;
				s += a.get(rc, cc);
			}
			if(rc == a.getRowDimension() - 1){
				s += "]\n";
			} else {
				s += ";\n";
			}
		}
		return s;
	}
	private static SplineCoefficients[] fitWithOnlyStraightLines(SplineObject object){
		double[] xs = object.getSplinePointIndepValues();
		double[] ys = object.getSplinePointDepValues();
		List<SplineCoefficients> result = new ArrayList<SplineCoefficients>();
		for(int i = 0; i < xs.length; ++i){
			int j = (i + 1) % xs.length;
			double[] sxs = new double[2];
			double[] sys = new double[2];
			sxs[0] = xs[i];
			sys[0] = ys[i];
			sxs[1] = xs[j];
			sys[1] = ys[j];
			result.addAll(convertCoefficients(fit(sxs,sys, EndPointType.CUSP, EndPointType.CUSP)));
		}
		
		
		return result.toArray(new SplineCoefficients[result.size()]);
	}
	public static SplineCoefficients[] fitCoefficients(SplineObject object, double error){
		SplineCoefficients[] result;
		try {
			result = tryFitCoefficients(object, error);
		} catch(RuntimeException e){
			e.printStackTrace();
			result = fitWithOnlyStraightLines(object);
		}
		return result;
	}
	private static SplineCoefficients[] tryFitCoefficients(SplineObject object, double error){
		List<SplineCoefficients> result = new ArrayList<SplineCoefficients>();
		double[] xs = object.getSplinePointIndepValues();
		double[] ys = object.getSplinePointDepValues();
		PointType[] pts = object.getSplinePointTypes();
		final int n = xs.length;
		if(n < 2){
			throw new RuntimeException("Must have more than 2 points");
		}
		//SplineCoefficients[] result = new SplineCoefficients[n];
		
		//first find the first cusp node
		int firstCuspIndex = -1;
		
		for(int i = 0; i < n; ++i){
			if(pts[i] == PointType.CUSP || pts[i] == PointType.SQUARE){
				firstCuspIndex = i;
				break;
			}
		}
		//now if there's no cusp-point then fit the whole thing, otherwise fit each cusp-point bounded segment
		if(firstCuspIndex == -1){
			//result.addAll(convertCoefficients(fit(xs,ys, BoundaryCondition.SMOOTH_CLOSED, BoundaryCondition.SMOOTH_CLOSED, true)));
			result.addAll(approxFit(xs,ys, EndPointType.SMOOTH, EndPointType.SMOOTH, error));
			//result.addAll(convertCoefficients(fit(xs,ys, PointType.SMOOTH, PointType.SMOOTH)));
		} else {
			int iStart = firstCuspIndex;
			int iEnd = (firstCuspIndex + 1) % n;
			while(iEnd != firstCuspIndex){
				//System.out.println("SplineMath.fitCoefficients # coeffs:"+result.size());
				int  j = iStart;
				int sn = 1;
				for(int i = 0; i <= n; ++i){//note we're wrapping all the way to the start
					++j;
					++sn;
					if(j >= n){
						j -= n;
					}
					if(pts[j] == PointType.CUSP || pts[j] == PointType.SQUARE){
						iEnd = j;
						break;//we've found the next cusp so stop
					}
					
					
				}
				Matrix coeffs;
				
				double[] sxs = new double[sn];
				double[] sys = new double[sn];
				j = iStart;
				for(int i = 0; i < sn; ++i){
					
					if(j >= n){
						j -= n;
					}
					sxs[i] = xs[j];
					sys[i] = ys[j];
					++j;
				}
				EndPointType pt0;
				EndPointType ptn;
				switch(pts[iStart]){
				default:
				case CUSP:
					pt0 = EndPointType.CUSP;
					break;
				case SMOOTH:
					pt0 = EndPointType.SMOOTH;
					break;
				case SQUARE:
					double dx = Math.abs(xs[0] - xs[1]);
					double dy = Math.abs(ys[0] - ys[1]);
				
					if(dy > dx){
						pt0 = EndPointType.VERT;
					} else {
						pt0 = EndPointType.HORIZ;
							
					}
					break;
				}
				
				switch(pts[iEnd]){
				default:
				case CUSP:
					ptn = EndPointType.CUSP;
					break;
				case SMOOTH:
					ptn = EndPointType.SMOOTH;
					break;
				case SQUARE:
					
					int c= xs.length;
					double dx = Math.abs(xs[c - 2] - xs[c - 1]);
					double dy = Math.abs(ys[c - 2] - ys[c - 1]);
				
					if(dy > dx){
						ptn = EndPointType.VERT;
					} else {
						ptn = EndPointType.HORIZ;
							
					}
					break;
				}
				if(sn == 2){
					//System.out.println("SplineMath.fitCoefficients fitting "+sn+" points: "+iStart+" to "+iEnd);
					//result.addAll(convertCoefficients(fit(sxs, sys, BoundaryCondition.EXTRAPOLATE_2ND_DERIVATIVE, BoundaryCondition.EXTRAPOLATE_2ND_DERIVATIVE, false)));
					try{
						//result.addAll(convertCoefficients(fit(sxs, sys, pts[iStart], pts[iEnd])));
						result.addAll(approxFit(sxs, sys, pt0, ptn, error));
					} catch(RuntimeException e){
						result.addAll(approxFit(sxs, sys, EndPointType.CUSP, EndPointType.CUSP, error));
						//result.addAll(convertCoefficients(fit(sxs, sys, PointType.CUSP, PointType.CUSP)));
					}
				} else {
					//result.addAll(convertCoefficients(fit(sxs, sys, pts[iStart], pts[iEnd])));
					result.addAll(approxFit(sxs, sys, pt0, ptn, error));
				}
				iStart = iEnd;
				
			}
		}
		return result.toArray(new SplineCoefficients[result.size()]);
	}
	/**
	 * Takes a matrix that presumably was just used to solve for some spline coefficents and returns
	 * a list of spline coefficients
	 * @param a
	 * @return
	 */
	private static List<SplineCoefficients> convertCoefficients(Matrix a){
		int r = a.getRowDimension();
		int c = a.getColumnDimension();
		
		if(c != 1){
			throw new RuntimeException("Number of columns should be 1, not "+c);
		}
		if(r % 8 != 0){
			throw new RuntimeException("Number of rows should be a multiple of 8: "+r);
		}
		List<SplineCoefficients> result = new ArrayList<SplineCoefficients>();
		for(int i = 0; i < r; i += 8){
			result.add(new SplineCoefficients(	a.get(i + 0, 0),
												a.get(i + 1, 0),
												a.get(i + 2, 0),
												a.get(i + 3, 0),
												a.get(i + 4, 0),
												a.get(i + 5, 0),
												a.get(i + 6, 0),
												a.get(i + 7, 0)));
												
		}
		return result;
		
		
	}
	/**
	 * computes array of points that can be used to display the specified spline object 
	 * @param segNum indicates how many points to use to display curved sections
	 * @return a 2 by n array where result[0] gives an array of x values and result[1] gives y values
	 */
	public static double[][] calcSplinePoints(SplineCoefficients[] scs, int segNum){
		 if(scs == null){
			 return null;
		 }
		 int n = 0;
		 for(SplineCoefficients sc : scs){
			 if(sc.isStraight()){
				 n += 1;
			 } else {
				 n += segNum;
			 }
		 }
		 //++n;
		 double[][] result = new double[2][n];
		 int i = 0;
		 //String  = "";
		 for(SplineCoefficients sc : scs){
			 if(sc.isStraight()){
				result[0][i] = sc.calcX(0);
				result[1][i] = sc.calcY(0);
//				result[0][i+1] = sc.calcX(1);
//				result[1][i+1] = sc.calcY(1);
				
				i += 1;
			 } else {
				 double s = 0;
				 int iStop = i + segNum;
				 double ds = 1.0/((double)(segNum));
				 while(i < iStop){
					 
					 result[0][i] = sc.calcX(s);
					 result[1][i] = sc.calcY(s);
					 s += ds;
					++i; 
				 }
			 }
		 }
//		 result[0][i] = result[0][0];
//		 result[1][i] = result[1][0];
		 return result;
		
	}
	public static double getSplineParameterMax(SplineCoefficients[] scs){
		double result = scs.length;
		return result;
	}
	public static double calcSplineXValue(SplineCoefficients[] scs, double s){
		int i = (int)Math.floor(s);
		if(i >= scs.length){
			i = scs.length - 1;
		}
		double p = s - (double)i;
		if(p > 1.0){
			p = 1.0;
		}
		return scs[i].calcX(p);
	}
	public static double calcSplineYValue(SplineCoefficients[] scs, double s){
		int i = (int)Math.floor(s);
		if(i >= scs.length){
			i = scs.length - 1;
		}
		double p = s - (double)i;
		if(p > 1.0){
			p = 1.0;
		}
		return scs[i].calcY(p);	
	}

}
