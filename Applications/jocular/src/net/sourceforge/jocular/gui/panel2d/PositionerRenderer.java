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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.positioners.ObjectPositioner;

/**
 * @author kmaccallum
 *
 */
public class PositionerRenderer implements Renderer2D {
	private final ObjectPositioner m_positioner;
	public PositionerRenderer(ObjectPositioner op){
		m_positioner = op;
	}
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.gui.Renderer2D#render(java.awt.Graphics2D, net.sourceforge.jocular.gui.OpticsObjectPanel.Plane)
	 */
	@Override
	public void render(Graphics2D g, Plane p) {
		
		Vector3D vP1 = m_positioner.getOrigin();
		if(vP1 == null){
			throw new RuntimeException("Positioner does not have vectors set:"+m_positioner);
		}
		Vector3D vP2 = m_positioner.getDirection().scale(.5).add(vP1);
		
		Point2D.Double p1 = OpticsObjectPanel.mapVector(vP1, p);
		Point2D.Double p2 = OpticsObjectPanel.mapVector(vP2, p);
		
		Shape s = new Line2D.Double(p1, p2);

		//Rectangle2D r = g.getClipBounds().getBounds2D();
		//double w = r.getWidth();
		float scale = (float)(1.0/g.getTransform().getScaleX());
		//float[] dash = {scale*10, scale*5};
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke((float)scale));
		//Path2D.Double p = new Path2D.Double();
		
		//g.setStroke(new BasicStroke(scale, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		g.draw(s);
	}

}
