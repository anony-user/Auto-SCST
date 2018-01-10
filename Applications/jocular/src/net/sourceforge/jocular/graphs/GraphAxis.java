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
package net.sourceforge.jocular.graphs;

import java.util.ArrayList;

public class GraphAxis {
	protected double min;
	protected double max;
	protected boolean log;
	/**
	 * 0 means no gridlines
	 */
	protected double gridLineSpacing;
	public GraphAxis(double min, double max, boolean log, double gridLineSpacing){
		this.min = min;
		this.max = max;
		this.log = log;
		if(log && (min == 0 || max == 0)){
			throw(new RuntimeException("Cannot use zero limit with log scale."));
		}
		this.gridLineSpacing = gridLineSpacing;
	}
	public double scale(double n){
		double result = n;
		if(log){
			result = Math.log10(result);// should now be in the range -6 to 0
			result -= Math.log10(min);
			result /= Math.log10(max) - Math.log10(min);
		} else {
			result -= min;
			result /= max - min;
		}
		result *= .8;
		result += .1;
		
		if(result > 0.9){
			result = 0.9;
		} else if(result < 0.1){
			result = 0.1;
		}
		return result;
	}
	public double unScale(double n){
		double result = n;
		result -= .1;
		result /= .8;
		if(log){
			result *= Math.log10(max) - Math.log10(min);
			result += Math.log10(min);
			result = Math.pow(10,result);
		} else {
			result *= max - min;
			result += min;
		}
		return result;
	}
	public ArrayList<Double> getGridLines(){
		if(gridLineSpacing == 0){
			return null;
		}
		ArrayList<Double> result = new ArrayList<Double>();
		
		if(log){
			
			double n = Math.ceil(Math.log10(min));
			double m = Math.floor(Math.log10(max));
			if((m - n) > 100){//arbitrary limit to prevent system being bogged down
				for(double i = n; i <= m; i++){
					result.add(Math.pow(10,i));
					if(gridLineSpacing == 1){
						for(int j = 2; j < 10; j++ ){
							if(Math.pow(10,i)*j < max){
								result.add(Math.pow(10,i)*j);
							}
						}
					}
				}
			}
			
		} else {
			int n = (int)Math.ceil(min/gridLineSpacing);
			int m = (int)Math.floor(max/gridLineSpacing);
			//don't bother calculating right now
			for(double i = n; i <= m; i++){
				result.add(i*gridLineSpacing);
			}
		}
		return result;
	}
	
}