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
package net.sourceforge.jocular.gui.panel2d;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.PhotonColour;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.imager.OpticsColour;
//import net.sourceforge.jocular.imager.Pixel;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.photons.PhotonWrangler;
import net.sourceforge.jocular.settings.Settings;

public class PhotonRenderer implements Renderer2D {

	private final PhotonWrangler m_wrangler;
	
	public PhotonRenderer(PhotonWrangler pw){
		m_wrangler = pw;
	}
	
	
	@Override
	public void render(Graphics2D g, Plane plane) {
		PhotonColour pc = Settings.SETTINGS.getPhotonColourScheme();
		double scale = 1.0/g.getTransform().getScaleX();
		g.setStroke(new BasicStroke((float)scale));
		Collection<PhotonTrajectory> pts = m_wrangler.getTrajectories();
		
		for(PhotonTrajectory pt : pts){
			
			Point2D.Double pLast = null;
			for(int i = 0; i < pt.getNumberOfPhotons(); ++i){
				Photon p = pt.getPhoton(i);
				g.setColor(OpticsColour.getColour(pt, i, pc).getAwtColor());
				
				
				Point2D.Double pNew = OpticsObjectPanel.mapVector(p.getOrigin(), plane);
				
				if(pLast != null){
					Shape s = new Line2D.Double(pLast, pNew);
					g.draw(s);
				}
				pLast = pNew;
				
				//put a dot at the photon
				switch(p.getPhotonSource()){
				case LOST:
				case ABSORBED:
					//g.setColor(Color.black);
				case REFLECTED:
				
				default:
					Shape d = new Ellipse2D.Double(pNew.x-scale*2.5, pNew.y-scale*2.5,scale*5,scale*5);
					g.fill(d);
					break;
				case REFRACTED:
					//draw an X if it's refracted
					Shape l1 = new Line2D.Double(pNew.x-scale*4, pNew.y-scale*4, pNew.x+scale*4, pNew.y+scale*4);
					Shape l2 = new Line2D.Double(pNew.x-scale*4, pNew.y+scale*4, pNew.x+scale*4, pNew.y-scale*4);
					g.draw(l1);
					g.draw(l2);
					break;
				case EMITTED:
				case TRANSMITTED:
					Shape d2 = new Ellipse2D.Double(pNew.x-scale*2.5, pNew.y-scale*2.5,scale*5,scale*5);
					g.draw(d2);
					//don't put a dot for these
					break;
				}
				//draw a triangle around it if it's a combined interaction
				if(pt.isCombined(i)){
					final int S = 10;
					Point2D.Double p1 = new Point2D.Double(pNew.x, pNew.y + scale*S);
					Point2D.Double p2= new Point2D.Double(pNew.x - scale*S*0.86602540378, pNew.y - scale*S*0.5);
					Point2D.Double p3= new Point2D.Double(pNew.x + scale*S*0.86602540378, pNew.y - scale*S*0.5);
					Shape l1 = new Line2D.Double(p1,p2);
					Shape l2 = new Line2D.Double(p2,p3);
					Shape l3 = new Line2D.Double(p3,p1);
					g.draw(l1);
					g.draw(l2);
					g.draw(l3);
				}
				if(pt.getComment(i).contains("added from")){
					final double S = 20;
					Shape d2 = new Rectangle2D.Double(pNew.x-scale*S*.5, pNew.y-scale*S*.5,scale*S,scale*S);
					g.draw(d2);
				}
				 
			}
		}
	}
	

}
