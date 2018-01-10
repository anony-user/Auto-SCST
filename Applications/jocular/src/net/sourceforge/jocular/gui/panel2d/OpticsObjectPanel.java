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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.project.OpticsProject;

@SuppressWarnings("serial")
public class OpticsObjectPanel extends AbstractPanel {
	public enum Plane {ZY_PLANE, ZX_PLANE, XY_PLANE, NONE};
	private final Plane m_plane;
	public enum PhotonColour {WAVELENGTH, POLARIZATION, MATERIAL, NUMBER_IN_SEQUENCE, PHOTON_SOURCE};
	private NumberFormat m_format = new DecimalFormat("#0.0000");
	Collection<Renderer2D> m_renderers = new ArrayList<Renderer2D>();
	
	public OpticsObjectPanel(Plane plane){
		
		m_plane = plane;
		
		m_scale = 10;
		m_centre = new Point2D.Double(-.05, 0);
		
		setBorder(new SoftBevelBorder(BevelBorder.RAISED) );
		setPreferredSize(new Dimension(640, 640));
		addMouseMotionListener(new PanelMouseMotionListener());
		addMouseWheelListener(new PanelMouseWheelListener());
	}	

	public void addRenderer(Renderer2D r){
		if(!m_renderers.contains(r)){
			m_renderers.add(r);
		}
	}
	
	@Override
	protected void drawPanel(Graphics2D g2){
		//draw stuff in here
		for(Renderer2D rend : m_renderers){
			rend.render(g2,  m_plane);
		}
	}
	
	@Override
	protected String getToolTip(Double p) {
		return m_format.format(p.x)+","+m_format.format(p.y);
	}
	
	protected static Point2D.Double mapVector(Vector3D v, Plane p){
		Point2D.Double result;
		switch(p){
		default:
		case ZY_PLANE:
			result = new Point2D.Double(v.z, v.y);
			break;
		case ZX_PLANE:
			result =  new Point2D.Double(v.z, v.x);
			break;
		case XY_PLANE:
			result =  new Point2D.Double(v.x, v.y);
			break;
		}
		return result;
	}
	
	public void setProject(OpticsProject p){
		
		clearPreviousProject();
		
		Renderer2D or = new OpticsObjectRenderer(p.getOpticsObject());
		Renderer2D pr = new PhotonRenderer(p.getWrangler());
		Renderer2D posr = new PositionerRenderer(p.getOpticsObject().getPositioner());		
		
		addRenderer(or);
		addRenderer(pr);
		addRenderer(posr);
			
	}
	
	private void clearPreviousProject(){
		m_renderers.clear();
	}	
	@Override
	public void updatePanel(){
	}	
	
	public Plane getPlane(){
		return m_plane;
	}

	@Override
	public void resetPanel() {
		// TODO Auto-generated method stub
		
	}
	
}
