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
package net.sourceforge.jocular.gui.panel2d;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sourceforge.jocular.graphs.GraphPanel;
import net.sourceforge.jocular.graphs.GraphPanel.GraphType;
import net.sourceforge.jocular.objects.SpectroPhotometer;

public class SpectroPhotometerPanel implements OutputPanel {
	private SpectroPhotometer m_spectroPhotometer;
	private GraphPanel m_graph = null;
	private String series1Key = "Series 1";
	public SpectroPhotometerPanel(SpectroPhotometer sp){
		m_spectroPhotometer = sp;
		m_graph = new GraphPanel();
		m_graph.setGraphType(GraphType.BAR);
		
		updatePanel();
		
	}
	@Override
	public Component getComponent() {
		return m_graph;
	}

	@Override
	public void updatePanel() {
		double xs[] = m_spectroPhotometer.getBinCentres();
		double ys[] = m_spectroPhotometer.getBinValues();
//		double yMin = Double.POSITIVE_INFINITY;
//		double yMax = Double.NEGATIVE_INFINITY;
//		for(double y : ys){
//			if(y > yMax){
//				yMax = y;
//			}
//			if(y < yMin){
//				yMin = y;
//			}
//		}
//		m_graph.setScale(xs[0], xs[xs.length - 1], yMin, yMax, false, GraphPanel.GraphType.XY_LINE);
		
		m_graph.updateGraph(series1Key, xs, ys);
		m_graph.autoScale(true, false, true, false);
		m_graph.repaint();

	}
	@Override
	public BufferedImage getImage() {
		Dimension d = m_graph.getSize();
		BufferedImage result = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = result.createGraphics();
		m_graph.paint(g);
		return result;
	}
	@Override
	public void zoomIn() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void zoomOut() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void resetPanel() {
		m_graph.zeroOffset();
		
	}
	
	

}
