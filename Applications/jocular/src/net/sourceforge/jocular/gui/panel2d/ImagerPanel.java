/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum, Bryan Matthews
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
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.imager.Pixel;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.PropertyKey;

// Originated from:
// http://www.dreamincode.net/forums/topic/162774-java-image-manipulation-part-2-resizing/
@SuppressWarnings("serial")
public class ImagerPanel extends AbstractPanel implements OutputPanel {
	
	private Imager m_imager;
	private BufferedImage m_image;
	
	public ImagerPanel(Imager imager) {
		
		//TODO: These were found experimentally, I am sure there is a better
		//  way to do this
		m_scale = .0015;
		m_centre = new Point2D.Double(-300, -250);
		m_imager = imager;
		
		addMouseMotionListener(new PanelMouseMotionListener());
		addMouseWheelListener(new PanelMouseWheelListener());
	}
	
	public void setImager(Imager imager){
		m_imager = imager;
	}
	
	@Override
	public void updatePanel(){
		
		if(m_imager == null) return;
		
		m_image = ((ImageProperty)m_imager.getProperty(PropertyKey.IMAGE)).getValue();
		repaint();
	}
	
	
	@Override
	protected void drawPanel(Graphics2D g2){
		
		if(m_image == null) return;
		
		double width = getWidth();
		double height = getHeight();
		double image_width = m_image.getWidth();
		double image_height = m_image.getHeight();
		
		if(width/image_width > height/image_height){
			width = image_width/image_height*height;
		} else {
			height = image_height/image_width*width;
		}
		g2.drawImage(m_image, 0,0,(int)width,(int)height,0,0,(int)image_width,(int)image_height, null);
	}

	private Point2D.Double getPixelLocation(Point2D.Double mousePos){
			
		if (m_image == null) return mousePos;
		
		Point2D.Double pixelLoc = mousePos;
		
		// Scale to 0 - 1
		pixelLoc.x /= getWidth();
		// Calculate pixel location
		pixelLoc.x *= m_image.getWidth();

		// Scale to 0 - 1
		pixelLoc.y /= getHeight();
		// Calculate pixel location
		pixelLoc.y *= m_image.getHeight();
		
		
		return pixelLoc;
	}

	@Override
	protected String getToolTip(Point2D.Double p) {
		
		if(m_imager == null) return "";
		
		Point2D.Double pixelLoc = getPixelLocation(p);
		Pixel displayPixel = m_imager.getPixel((int) pixelLoc.x, (int) pixelLoc.y);
		
		if(displayPixel == null) return "";
		
		DecimalFormat format = new DecimalFormat("######.#");
		return "r: " + format.format(displayPixel.getRed(false)) + 
				" g: "+ format.format(displayPixel.getGreen(false)) + 
				" b: " + format.format(displayPixel.getBlue(false)) 
				//+ " loc: " + format.format(p.x)+","+format.format(p.y)
				;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public BufferedImage getImage() {
		return m_image;
	}
	
	public Imager getImager(){
		return m_imager;
	}

	@Override
	public void resetPanel() {
		// TODO Auto-generated method stub
		
	}
	
}

