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

import java.awt.Color;

import net.sourceforge.jocular.graphs.GraphPanel.DotType;
import net.sourceforge.jocular.graphs.GraphPanel.GraphType;

public class GraphSeries {
	public final double[] x;
	public final double[] y;
	//private double[] xOut;
	protected Color color;
	private GraphType seriesType;
	private double m_dotSize = 20;
	private DotType m_dotType = DotType.TRIANGLE;
	private double m_lineWidth = 1;
	
	
	public GraphSeries(double[] xs, double[] ys, Color c){
		x = xs.clone();
		y = ys.clone();
		color = c;
		seriesType = GraphType.FROM_GRAPH;		// default to using graph level type
	}
	public int size(){
		return Math.min(x.length, y.length);
	}
	public double getX(int i){
		int ti = i;
		if(i < 0){
			ti += x.length;
		} else if(i >= x.length){
			ti -= x.length;
		}
		return x[ti];
	}
	public double getY(int i){
		int ti = i;
		if(i < 0){
			ti += y.length;
		} else if(i >= y.length){
			ti -= y.length;
		}
		return y[ti];
	}
	public Color getColor(){
		return color;
	}
	public void setType(GraphType t){
		seriesType = t;
	}
	public GraphType getType() {
		return seriesType;
	}
	public void setDotType(DotType dt){
		m_dotType = dt;
	}
	public DotType getDotType(){
		return m_dotType;
	}
	public void setDotSize(double s){
		m_dotSize = s;
	}
	public double getDotSize(){
		return m_dotSize;
	}
	public double getLineWidth(){
		return m_lineWidth ;
	}
	public void setLineWidth(double w){
		m_lineWidth = w;
	}
	
	
}
