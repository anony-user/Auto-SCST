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
package net.sourceforge.jocular.math;


public class LookupTable  implements FunctionOfX  {
	
	private final double[][] c;
	private final double xMin;
	private final double xMax;
	private final double dx;
	public LookupTable(double[] xData, double[] yData){
		this(xData, yData, xData.length, true);
	}
	
	public LookupTable(double[] xData, double[] yData, int size, boolean linearNotLog){
		this(xData, yData, size, linearNotLog, false);
	}
	public LookupTable(double[] xData, double[] yData, int size, boolean linearNotLog, boolean zeroSlopeAtEndpoints){
		if(xData.length != yData.length){
			throw (new RuntimeException("Both arrays are not the same size in LookupTable"));
		}
		if(linearNotLog){
			int len = xData.length;
			xMin = xData[0];
			xMax = xData[len - 1];
			dx = (xMax - xMin)/(size - 1);
			double[] slope = new double[len];
			double[] y = new double[size];
			double[] dy = new double[size];
			//compute the slope for most points
			for(int i = 1; i < len - 1; i++){
				slope[i] = (yData[i + 1] - yData[i - 1])/(xData[i + 1] - xData[i - 1]);
			}
			//now extrapolate the endpoints, if required
			if(zeroSlopeAtEndpoints){
				slope[0] = 0;
				slope[len - 1] = 0;
			} else {
				slope[0] = slope[1] - ((slope[2] - slope[1])/(xData[2] - xData[1])*(xData[1] - xData[0]));
				slope[len - 1] = slope[len - 2] + ((slope[len - 3] - slope[len - 2])/(xData[len - 3] - xData[len - 2])*(xData[len - 2] - xData[len - 1]));
			}
			//now compute the lookup table values for each x value
			for(int i = 0; i < size - 1; ++i){
				double x = getXValue(i);
				int n = lookup(xData, x);

				
				double[] coeffs = solveCubicSpline(xData[n], xData[n + 1], yData[n], yData[n + 1], slope[n], slope[n + 1]);
				y[i] = calcSplineValue(coeffs, x);
				dy[i] = calcSplineSlope(coeffs, x);
				if(i == size - 2){
					y[i + 1] = calcSplineValue(coeffs, getXValue(i + 1));
					dy[i + 1] = calcSplineSlope(coeffs, getXValue(i + 1));
				}
				
			}

			c = new double[size][4];
			//now calc final values
			double[] coeffs = new double[4];
			for(int i = 0; i < size; ++i){
				
				if(i < size - 1){//reuse the penultimate coeffs if we're at the last index
					coeffs = solveCubicSpline(getXValue(i), getXValue(i + 1), y[i], y[i + 1], dy[i], dy[i + 1]);
				}
				c[i][0] = coeffs[0];
				c[i][1] = coeffs[1];
				c[i][2] = coeffs[2];
				c[i][3] = coeffs[3];
			}
		} else {
			throw (new RuntimeException("Log not supported yet."));
		}
	}
	@Override
	public double getValue(double x){
		double[] coeffs = c[getIndex(x)];
		return calcSplineValue(coeffs, x);
	}
	private double calcSplineValue(double[] coeffs, double x){
		double result = coeffs[3];
		result *= x;
		result += coeffs[2];
		result *= x;
		result += coeffs[1];
		result *= x;
		result += coeffs[0];
		return result;
	}
	private int lookup(double[] xs, double x){
		int i;
		for(i = 0; i < xs.length; ++i){
			if(xs[i] > x){
				break;
			}
		}
		return (i - 1);
	}
	private double calcSplineSlope(double[] coeffs, double x){
		double result = 3 * coeffs[3];
		result *= x;
		result += 2 * coeffs[2];
		result *= x;
		result += coeffs[1];
		return result;
	}
	/**
	 * computes the 4 coefficient required for a cubic fit, given values and slopes at two points.
	 * @param x0
	 * @param x1
	 * @param y0
	 * @param y1
	 * @param dy0
	 * @param dy1
	 * @return
	 */
	private double[] solveCubicSpline(double x0, double x1, double y0, double y1, double dy0, double dy1){
		double[] result = new double[4];
		double[] c0 = {Math.pow(x0, 3), Math.pow(x1, 3), 3 * Math.pow(x0, 2), 3 * Math.pow(x1,  2)};
		double[] c1 = {Math.pow(x0, 2), Math.pow(x1, 2), 1, 1};
		double[] c2 = {x0, x1, 1, 1};
		double[] c3 = {1, 1, 0, 0};
		double[] y = {y0, y1, dy0, dy1};
		double detA = determinant4(columnsToMatrix4(c0, c1, c2, c3));
		
		result[3] = determinant4(columnsToMatrix4(y, c1, c2, c3))/detA;
		result[2] = determinant4(columnsToMatrix4(c0, y, c2, c3))/detA;
		result[1] = determinant4(columnsToMatrix4(c0, c1, y, c3))/detA;
		result[0] = determinant4(columnsToMatrix4(c0, c1, c2, y))/detA;
		return result;
	}
	private double[][] columnsToMatrix4(double[] c0, double[] c1, double[] c2, double[] c3){
		if(c0.length != 4 || c1.length != 4 || c2.length != 4 || c3.length != 4){
			throw (new RuntimeException("LookupTable.columnsToMatrix4 requires arrays of 4 elements."));
		}
		double[][] result = new double[4][4];
		for(int i = 0; i < 4; ++i){
			result[i][0] = c0[i];
			result[i][1] = c1[i];
			result[i][2] = c2[i];
			result[i][3] = c3[i];
		}
		return result;
	}
	private double determinant4(double[][] a){
		if(a[0].length != 4 || a.length != 4){
			throw new RuntimeException("Matrix input to determinant4 does not have dimensions 4x4");
		}
		double result = 0;
		double sign0 = -1;
		for(int i0 = 0; i0 < 4; ++i0){
			sign0 = -sign0;
			double sign1 = -1;
			for(int i1 = 0; i1 < 4; ++i1){
				if(i1 != i0){
					sign1 = -sign1;
					double sign2 = -1;
					for(int i2 = 0; i2 < 4; ++i2){
						if(i2 != i1 && i2 != i0){
							sign2 = -sign2;
							for(int i3 = 0; i3 < 4; ++i3){
								if(i3 != i2 && i3 != i1 && i3 != i0){
									result += sign0*sign1*sign2*a[0][i0]*a[1][i1]*a[2][i2]*a[3][i3];
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	private int getIndex(double x){
		double temp = x;
		if(temp > xMax){
			temp = xMax;
		} else if(temp < xMin){
			temp = xMin;
		}
		temp -= xMin;
		temp /= dx;
		return (int)temp;
	}
	private double getXValue(int i){
		double result = (double)i;
		result *= dx;
		result += xMin;
		return result;
	}
}
