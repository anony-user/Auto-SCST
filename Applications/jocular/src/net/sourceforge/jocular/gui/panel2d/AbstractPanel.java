/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class AbstractPanel extends JPanel implements OutputPanel{

	protected double m_scale = 1.0;
	protected Point2D.Double m_centre = new Point2D.Double(0, 0);
	
	private AffineTransform getTransform(){
		AffineTransform t;
		Dimension panelSize = getSize();
		double w = panelSize.width;
		double h = panelSize.height;
		double min = Math.min(w, h);
		double s = min*m_scale;
		t = AffineTransform.getScaleInstance(s,-s);
		t.translate(1/m_scale*w/min/2, -1/m_scale*h/min/2);
		t.translate(m_centre.x, m_centre.y);
		return t;
	}
	
	public void paintComponent(Graphics g) {
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g2.setRenderingHint(RenderingHints.KEY_RENDERING,  RenderingHints.VALUE_RENDER_QUALITY);
//		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,  RenderingHints.VALUE_STROKE_PURE);
//		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,  RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//		g2.setRenderingHint(RenderingHints.KEY_DITHERING,  RenderingHints.VALUE_DITHER_ENABLE);
		
		Rectangle r = g2.getClipBounds();

		g2.setColor(Color.white);

		g2.fillRect(r.x, r.y, r.width, r.height);
		
		AffineTransform oldTransform = g2.getTransform();
		
		//Dimension panelSize = getSize();
		AffineTransform transform;
		transform = new AffineTransform(oldTransform);
		
		transform.concatenate(getTransform());
		g2.setTransform(transform);
		
		//draw stuff in here
		drawPanel(g2);

		//now set the transform back to what it was
		g2.setTransform(oldTransform);			
		
	}
	
	public void zoomIn(){
		m_scale *= 1.1;
		repaint();
	}
	public void zoomOut(){
		m_scale /= 1.1;
		repaint();
	}
	public void setScale(double s){
		m_scale = s;
		repaint();
	}
	
	protected Point2D.Double getMousePos(){
		Point2D.Double result = new Point2D.Double();
		Point p = getMousePosition();
		if(p != null){
			Point2D.Double p1 = new Point2D.Double(p.getX(), p.getY());
			try {
				AffineTransform t = getTransform().createInverse();
				t.transform(p1, result);
				//System.out.println("pos "+result.x+","+result.y);
				//result = result;
				
			} catch(NoninvertibleTransformException e){
				
			}
		}
		return result;		
	}
	
	protected abstract void drawPanel(Graphics2D g2);
	
	protected class PanelMouseWheelListener implements MouseWheelListener{

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int r = e.getWheelRotation();
			if(r > 0){
				zoomOut();
			} else if(r < 0){
				zoomIn();
			}
			Point2D.Double p = getMousePos();
			
			m_centre = new Point2D.Double((-0.1*p.x+0.9*m_centre.x), (-0.1*p.y+0.9*m_centre.y));
		}		
	}
	
	protected class PanelMouseMotionListener implements MouseMotionListener{

		@Override
		public void mouseDragged(MouseEvent me) {
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Point2D.Double p = getMousePos();
			
			repaint();
			setToolTipText(getToolTip(p));			
		}			
	}
	public abstract void updatePanel();
	
	protected abstract String getToolTip(Point2D.Double p);
	@Override
	public BufferedImage getImage() {
		Dimension d = getSize();
		BufferedImage result = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = result.createGraphics();
		paint(g);
		return result;
	}

	@Override
	public Component getComponent() {
		return this;
	}
	
}
