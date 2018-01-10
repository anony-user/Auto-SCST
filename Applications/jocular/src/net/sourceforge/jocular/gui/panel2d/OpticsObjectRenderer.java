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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.CylindricalSurface;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.PlanoAsphericLens;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.objects.SimpleAperture;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.objects.SphericalSurface;
import net.sourceforge.jocular.objects.SphericalSurface.SurfaceShape;
import net.sourceforge.jocular.objects.TriangularPrism;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;


public class OpticsObjectRenderer implements Renderer2D, OpticsObjectVisitor {
	private final OpticsObject m_object;
	
	public OpticsObjectRenderer(OpticsObject o){
		m_object = o;
	}
	
	
	
	@Override
	public void render(Graphics2D g, Plane p) {
		render(g, m_object, p);
		
	}

	private double m_scale;
	private Graphics2D m_g;
	private Plane m_p;
	private void setupRender(OpticsObject o){
		m_scale = 1.0/m_g.getTransform().getScaleX();
		if(o.isSelected()){
			m_g.setStroke(new BasicStroke((float)(2.0*m_scale)));
			m_g.setColor(Color.MAGENTA);
		} else {
			m_g.setStroke(new BasicStroke((float)m_scale));
			m_g.setColor(Color.black);
		}
		
		//g.setStroke(new BasicStroke((float)scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 100.0f));
		
	}

	private void render(Graphics2D g, OpticsObject o, Plane plane){
		if(o == null){
			return;
		}

		m_g = g;
		m_p = plane;
		//setupRender();

		o.accept(this);
//		if(o instanceof SphericalLens){
//			renderSphericalLens(g, (SphericalLens)o, plane);
//		
//		} else if(o instanceof OpticsObjectGroup){
//
////		if(o instanceof OpticsObjectCombo){
//			for(OpticsObject oi : ((OpticsObjectCombo)o).getObjects()){
//				render(g, oi, plane);
//			}
//		} else if(o instanceof SphericalSurface){
//			renderSphericalSurface(g, (SphericalSurface)o, plane);
//			
//		} else if(o instanceof CylindricalSurface){
//			renderCylindricalSurface(g, (CylindricalSurface)o, plane);
//		} else if(o instanceof Imager){
//			renderImager(g, o, plane);
//		} else if(o instanceof ImageSource){
//			renderImager(g, o, plane);
//		} else if(o instanceof HemiPointSource){
//			renderHemiPointSource(g, (HemiPointSource)o, plane);
//		} else if(o instanceof TriangularPrism){
//			renderTriangularPrism(g, (TriangularPrism)o, plane);
//		} else if(o instanceof SimpleAperture){
//			renderSimpleAperture(g, (SimpleAperture)o, plane);
//		}
	}
	private Vector3D getNormalVector(Plane p){
		Vector3D result;
		switch(p){
		default:
		case ZY_PLANE:
			result = Vector3D.X_AXIS;
			break;
		case ZX_PLANE:
			result = Vector3D.Y_AXIS;
			break;
		}
		return result;
	}

	@Override
	public void visit(SpectroPhotometer v) {
		setupRender(v);
		Vector3D o = v.getPositioner().getOrigin();
		Vector3D tDir = v.getPositioner().getTransDirection();
		Vector3D oDir = v.getPositioner().getOrthoDirection();
		double od = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		Point2D.Double pOdNew = null;
		Path2D.Double path = new Path2D.Double();
		for(double a = 0; a < Math.PI*2.0d+ Math.toRadians(15); a += Math.toRadians(15)){
			pOdNew = OpticsObjectPanel.mapVector(o.add(tDir.scale(od*Math.sin(a))).add(oDir.scale(od*Math.cos(a))), m_p);
			if(a != 0){
				path.lineTo(pOdNew.x, pOdNew.y);
			} else {
				path.moveTo(pOdNew.x, pOdNew.y);
			}
			
		}
		m_g.draw(path);
		
		
	}
	@Override
	public void visit(AutofocusSensor v) {
		setupRender(v);
		Vector3D o = v.getPositioner().getOrigin();
		Vector3D tDir = v.getPositioner().getTransDirection();
		Vector3D oDir = v.getPositioner().getOrthoDirection();
		double od = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		Point2D.Double pOdNew = null;
		Path2D.Double path = new Path2D.Double();
		for(double a = 0; a < Math.PI*2.0d+ Math.toRadians(15); a += Math.toRadians(15)){
			pOdNew = OpticsObjectPanel.mapVector(o.add(tDir.scale(od*Math.sin(a))).add(oDir.scale(od*Math.cos(a))), m_p);
			if(a != 0){
				path.lineTo(pOdNew.x, pOdNew.y);
			} else {
				path.moveTo(pOdNew.x, pOdNew.y);
			}
			
		}
		m_g.draw(path);
		
		
	}


	@Override
	public void visit(Imager i) {
		setupRender(i);
		//first get the extents of the imager
		double w2 = (((EquationProperty)(i.getProperty(PropertyKey.ORTHO_SIZE))).getValue().getBaseUnitValue())/2;
		double h2 = (((EquationProperty)(i.getProperty(PropertyKey.TRANS_SIZE))).getValue().getBaseUnitValue())/2;
		Vector3D o = i.getPositioner().getOrigin();
		Vector3D trans = i.getPositioner().getTransDirection().scale(h2);
		Vector3D ortho = i.getPositioner().getOrthoDirection().scale(w2);
		
		Point2D.Double p1 = OpticsObjectPanel.mapVector(o.add(trans).add(ortho), m_p);
		Point2D.Double p2 = OpticsObjectPanel.mapVector(o.add(trans).subtract(ortho), m_p);
		Point2D.Double p3 = OpticsObjectPanel.mapVector(o.subtract(trans).subtract(ortho), m_p);
		Point2D.Double p4 = OpticsObjectPanel.mapVector(o.subtract(trans).add(ortho), m_p);
		
		
		Shape s1 = new Line2D.Double(p1,p2);
		Shape s2 = new Line2D.Double(p2,p3);
		Shape s3 = new Line2D.Double(p3,p4);
		Shape s4 = new Line2D.Double(p4,p1);
		m_g.draw(s1);
		m_g.draw(s2);
		m_g.draw(s3);
		m_g.draw(s4);
		
	}



	@Override
	public void visit(SphericalSurface ss) {
		setupRender(ss);
		double dia = ((EquationProperty)(ss.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		Vector3D orig = ss.getPositioner().getOrigin();
		Vector3D dir = ss.getPositioner().getDirection();
		Point2D.Double o = OpticsObjectPanel.mapVector(orig, m_p);
		Point2D.Double d = OpticsObjectPanel.mapVector(dir, m_p);
		if(ss.getProperty(PropertyKey.SPHERICAL_SHAPE).getValue() == SurfaceShape.FLAT){
			Point2D.Double t = OpticsObjectPanel.mapVector(dir.cross(getNormalVector(m_p).scale(dia/2)), m_p);
			Shape s = new Line2D.Double(o.x+t.x, o.y+t.y, o.x-t.x, o.y - t.y);
			m_g.draw(s);
		} else {
			Point2D.Double sc = OpticsObjectPanel.mapVector(ss.getSphereCentre(), m_p);
			double a = -Math.toDegrees(Math.atan2(d.y, d.x));
			if(ss.getProperty(PropertyKey.SPHERICAL_SHAPE).getValue() == SphericalSurface.SurfaceShape.CONVEX){
				a += 180.0;
			}
			double rad = ((EquationProperty)(ss.getProperty(PropertyKey.RADIUS))).getValue().getBaseUnitValue();

			double a2 = Math.toDegrees(Math.asin(dia/2.0/rad));
			
			
			Path2D.Double p = new Path2D.Double();
			double startA = Math.toRadians(a-a2);
			double endA = Math.toRadians(a+a2);
			if(startA > endA){
				double t = startA;
				startA = endA;
				endA =t;
			}
			double stepA = (endA - startA)/50;
			double an = startA;
			for(int i = 0; i <= 50 ; ++i){
				double x = sc.x + rad*Math.cos(an);
				double y = sc.y + rad*Math.sin(an);
				an += stepA;
				if(i == 0){
					p.moveTo(x, y);
				} else {
					p.lineTo(x, y);
				}
			}
			
			;
			m_g.draw(p);
			
		
			
		}
		
		
		
		
	}



	@Override
	public void visit(SphericalLens sl) {
		setupRender(sl);
		Vector3D orig = sl.getPositioner().getOrigin();
		Vector3D dir = sl.getPositioner().getDirection();
		double dia = ((EquationProperty)(sl.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		double[] rads = {((EquationProperty)(sl.getProperty(PropertyKey.FRONT_RADIUS))).getValue().getBaseUnitValue(), ((EquationProperty)(sl.getProperty(PropertyKey.BACK_RADIUS))).getValue().getBaseUnitValue()};
		boolean[] flats = {sl.getProperty(PropertyKey.FRONT_SHAPE).getValue() == SurfaceShape.FLAT, sl.getProperty(PropertyKey.BACK_SHAPE).getValue() == SurfaceShape.FLAT};
		Vector3D[] centres = {sl.getFrontSphereCentre(), sl.getBackSphereCentre()};
		double t = ((EquationProperty)(sl.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
		
		
		Vector3D trans = sl.getPositioner().getTransDirection();
		Vector3D ortho = sl.getPositioner().getOrthoDirection();
		Path2D.Double pathO = new Path2D.Double();
		Path2D.Double pathT = new Path2D.Double();
		Point2D.Double firstPointO = null;
		Point2D.Double firstPointT = null;
		for(int i = 0; i < 2; ++i){
			double rad = rads[i];
			
			boolean flat = flats[i];
			Vector3D centre;

			centre = centres[i];
			Vector3D radDir = orig.add(dir.scale((i == 0)?-t/2:t/2)).subtract(centre).normalize();
			double aStart;
			double aEnd;
			double aStep;
			int n;
			if(flat){
				n = 1;
				rad = dia/2;
				if(i == 0){
					aStart = -Math.PI/2;
				} else {
					aStart = Math.PI/2;
				}
				aEnd = -aStart;
			} else {
				
				n = 20;
				if(rad == 0){
					aStart = -Math.PI/2;
				} else if(i == 0){
					aStart = -Math.asin(dia/2/rad);
				} else {
					aStart = Math.asin(dia/2/rad);
					
				}
			}
			aEnd = -aStart;
			
			aStep = (aEnd - aStart)/n;
			
			for(int j = 0; j <= n; ++j){
				double a = aStart + aStep*j;
				
			//for(double a = aStart; a < aEnd+aStep; a += aStep){
				Vector3D thisPointO = centre.add(radDir.scale(rad*Math.cos(a))).add(ortho.scale(rad*Math.sin(a)));
				Vector3D thisPointT = centre.add(radDir.scale(rad*Math.cos(a))).add(trans.scale(rad*Math.sin(a)));
				Point2D.Double pO = OpticsObjectPanel.mapVector(thisPointO, m_p);
				Point2D.Double pT = OpticsObjectPanel.mapVector(thisPointT, m_p);
				if(firstPointO == null){
					pathO.moveTo(pO.x, pO.y);
					pathT.moveTo(pT.x, pT.y);
					firstPointO = pO;
					firstPointT = pT;
				} else {
					pathO.lineTo(pO.x, pO.y);
					pathT.lineTo(pT.x, pT.y);
				}
			}
		}
		pathO.lineTo(firstPointO.x, firstPointO.y);
		pathT.lineTo(firstPointT.x, firstPointT.y);
		m_g.draw(pathO);
		m_g.draw(pathT);
		
	}


	@Override
	public void visit(PlanoAsphericLens v){
		defaultVisit(v);
	}
	@Override
	public void visit(RotatedSpline v) {
		defaultVisit(v);
		
	}
	@Override
	public void visit(ExtrudedSpline v) {
		defaultVisit(v);
		
	}

	private void defaultVisit(OpticsObject o){
		setupRender(o);
		Point2D.Double p = OpticsObjectPanel.mapVector(o.getPositioner().getOrigin(), m_p);
		
		m_scale = m_g.getTransform().getScaleX();
		double rad = 10.0/m_scale;
		Shape s = new Ellipse2D.Double(p.x - rad/2, p.y - rad/2, rad, rad);
		m_g.fill(s);
	}

	@Override
	public void visit(HemiPointSource hps) {
		defaultVisit(hps);
	}



	@Override
	public void visit(CylindricalSurface cs) {
		setupRender(cs);
		double rad = (((EquationProperty)(cs.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue())/2.0;
		double lenOver2 = (((EquationProperty)(cs.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue())/2.0;
		//System.out.println("OpticsObjectRenderer.renderCylindricalSurface rad = "+rad+", len = "+len);
		Point2D.Double o = OpticsObjectPanel.mapVector(cs.getPositioner().getOrigin(), m_p);
		Point2D.Double t = OpticsObjectPanel.mapVector(cs.getPositioner().getDirection().scale(lenOver2), m_p);
		Point2D.Double d = OpticsObjectPanel.mapVector(cs.getPositioner().getDirection().cross(getNormalVector(m_p)).scale(rad),m_p);
		
		Shape s = new Line2D.Double(o.x+d.x-t.x, o.y+d.y-t.y, o.x+d.x+t.x, o.y+d.y+t.y);
		m_g.draw(s);
		s = new Line2D.Double(o.x-d.x-t.x, o.y-d.y-t.y, o.x-d.x+t.x, o.y-d.y+t.y);
		m_g.draw(s);
		
	}



	@Override
	public void visit(OpticsObjectGroup v) {
		for(OpticsObject o : v.getObjects()){
			if(!o.isSuppressed()){
				o.accept(this);
			}
		}
		
	}

	@Override
	public void visit(OpticsPart v) {
		for(OpticsObject o : v.getObjects()){
			if(!o.isSuppressed()){
				o.accept(this);
			}
		}
		
	}

	@Override
	public void visit(ProjectRootGroup v) {
		for(OpticsObject o : v.getObjects()){
			if(!o.isSuppressed()){
				o.accept(this);
			}
		}
	}



	@Override
	public void visit(TriangularPrism tp) {
		setupRender(tp);
		Vector3D[][] ps = tp.getVertices();
		
		
		Point2D.Double p00 = OpticsObjectPanel.mapVector(ps[0][0], m_p);
		Point2D.Double p01 = OpticsObjectPanel.mapVector(ps[0][1], m_p);
		Point2D.Double p02 = OpticsObjectPanel.mapVector(ps[0][2], m_p);
		Point2D.Double p10 = OpticsObjectPanel.mapVector(ps[1][0], m_p);
		Point2D.Double p11 = OpticsObjectPanel.mapVector(ps[1][1], m_p);
		Point2D.Double p12 = OpticsObjectPanel.mapVector(ps[1][2], m_p);
		
		//draw top triangle
		m_g.draw(new Line2D.Double(p00, p01));
		m_g.draw(new Line2D.Double(p01, p02));
		m_g.draw(new Line2D.Double(p02, p00));
		//draw bottom triangle
		m_g.draw(new Line2D.Double(p10, p11));
		m_g.draw(new Line2D.Double(p11, p12));
		m_g.draw(new Line2D.Double(p12, p10));
		//draw lines connecting top to bottom
		m_g.draw(new Line2D.Double(p00, p10));
		m_g.draw(new Line2D.Double(p01, p11));
		m_g.draw(new Line2D.Double(p02, p12));
		
	}



	@Override
	public void visit(ImageSource i) {
		setupRender(i);
		//first get the extents of the imager
		double w2 = (((EquationProperty)(i.getProperty(PropertyKey.ORTHO_SIZE))).getValue().getBaseUnitValue())/2;
		double h2 = (((EquationProperty)(i.getProperty(PropertyKey.TRANS_SIZE))).getValue().getBaseUnitValue())/2;
		Vector3D o = i.getPositioner().getOrigin();
		Vector3D trans = i.getPositioner().getTransDirection().scale(h2);
		Vector3D ortho = i.getPositioner().getOrthoDirection().scale(w2);
		
		Point2D.Double p1 = OpticsObjectPanel.mapVector(o.add(trans).add(ortho), m_p);
		Point2D.Double p2 = OpticsObjectPanel.mapVector(o.add(trans).subtract(ortho), m_p);
		Point2D.Double p3 = OpticsObjectPanel.mapVector(o.subtract(trans).subtract(ortho), m_p);
		Point2D.Double p4 = OpticsObjectPanel.mapVector(o.subtract(trans).add(ortho), m_p);
		
		
		Shape s1 = new Line2D.Double(p1,p2);
		Shape s2 = new Line2D.Double(p2,p3);
		Shape s3 = new Line2D.Double(p3,p4);
		Shape s4 = new Line2D.Double(p4,p1);
		m_g.draw(s1);
		m_g.draw(s2);
		m_g.draw(s3);
		m_g.draw(s4);

	}



	@Override
	public void visit(SimpleAperture sa) {
		setupRender(sa);
		Vector3D o = sa.getPositioner().getOrigin();
		Vector3D tDir = sa.getPositioner().getTransDirection();
		Vector3D oDir = sa.getPositioner().getOrthoDirection();
		double od = ((EquationProperty)(sa.getProperty(PropertyKey.OUTER_DIAMETER))).getValue().getBaseUnitValue();
		
		Point2D.Double pOdNew = null;
		Path2D.Double path = new Path2D.Double();
		for(double a = 0; a < Math.PI*2.0d+ Math.toRadians(15); a += Math.toRadians(15)){
			pOdNew = OpticsObjectPanel.mapVector(o.add(tDir.scale(od*Math.sin(a))).add(oDir.scale(od*Math.cos(a))), m_p);
			if(a != 0){
				path.lineTo(pOdNew.x, pOdNew.y);
			} else {
				path.moveTo(pOdNew.x, pOdNew.y);
			}
			
		}
		m_g.draw(path);
		
	}



	
	
}
