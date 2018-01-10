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
package net.sourceforge.jocular.gui.panel3d;

import de.jreality.scene.SceneGraphComponent;
import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.imager.Imager;
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
import net.sourceforge.jocular.objects.TriangularPrism;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;
import net.sourceforge.jocular.splines.SplineCoefficients;

/**
 * A factory for generating 3D geometry from an OpticsObject. 
 * 
 * The idea is to assemble geometry pieces as needed to create the desired shape.
 * 
 * The appearance and position of the geometry should be set elsewhere. 
 * 
 * 
 * @author Bryan Matthews
 *
 */
public class OpticsObject3DFactory implements OpticsObjectVisitor {
	
	SceneGraphComponent m_component3D;
	
	public SceneGraphComponent render(OpticsObject o){
		o.accept(this);
		
		return m_component3D;
	}
	
	@Override
	public void visit(Imager v) {
		
		double length = ((EquationProperty)(v.getProperty(PropertyKey.TRANS_SIZE))).getValue().getBaseUnitValue();
		double height = ((EquationProperty)(v.getProperty(PropertyKey.ORTHO_SIZE))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createRectangle(length, height);
	}

	@Override
	public void visit(SphericalSurface v) {
		// A spherical surface is always part of a spherical lens		
	}

	@Override
	public void visit(SphericalLens v) {
		
		double frontRadius = ((EquationProperty)(v.getProperty(PropertyKey.FRONT_RADIUS))).getValue().getBaseUnitValue();
		double backRadius = ((EquationProperty)(v.getProperty(PropertyKey.BACK_RADIUS))).getValue().getBaseUnitValue();
		double diameter = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		double thickness = ((EquationProperty)(v.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
		SphericalSurface.SurfaceShape frontShape = (SphericalSurface.SurfaceShape) (v.getProperty(PropertyKey.FRONT_SHAPE).getValue());
		SphericalSurface.SurfaceShape backShape = (SphericalSurface.SurfaceShape) (v.getProperty(PropertyKey.BACK_SHAPE).getValue());
		
		m_component3D = OpticsObjects3D.createSphericalLens(frontRadius, frontShape, backRadius, backShape, diameter, thickness);
		
	}
	@Override
	public void visit(RotatedSpline v) {
		SplineCoefficients[] scs = v.getSplineCoefficients();
		if(scs == null || scs.length < 1){
			m_component3D = OpticsObjects3D.createSphere(0.010);
		} else {
			m_component3D = OpticsObjects3D.createRotatedSpline(v.getSplineCoefficients());
		}
	}
	@Override
	public void visit(PlanoAsphericLens v) {
		visit((RotatedSpline)v);
	}
	@Override
	public void visit(ExtrudedSpline v) {
		SplineCoefficients[] scs = v.getSplineCoefficients();
		if(scs == null || scs.length < 1){
			m_component3D = OpticsObjects3D.createSphere(0.010);
		} else {
			double thickness = ((EquationProperty)(v.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
			m_component3D = OpticsObjects3D.createExtrudedSpline(v.getSplineCoefficients(), thickness);
		}
	}
	@Override
	public void visit(HemiPointSource v) {

		m_component3D = OpticsObjects3D.createSphere(0.001);		
	}

	@Override
	public void visit(CylindricalSurface v) {
		// A cylindrical surface is always part of a spherical lens		
	}

	@Override
	public void visit(OpticsObjectGroup v) {
		// Object groups are not drawn		
	}
	@Override
	public void visit(OpticsPart v) {
		// Object groups are not drawn		
	}
	@Override
	public void visit(ProjectRootGroup v) {
		// Object groups are not drawn		
	}

	@Override
	public void visit(TriangularPrism v) {
		
		double width = ((EquationProperty)(v.getProperty(PropertyKey.WIDTH))).getValue().getBaseUnitValue();
		double length = ((EquationProperty)(v.getProperty(PropertyKey.LENGTH))).getValue().getBaseUnitValue();
		double angle1 = ((EquationProperty)(v.getProperty(PropertyKey.ANGLE_1))).getValue().getBaseUnitValue();
		double angle2 = ((EquationProperty)(v.getProperty(PropertyKey.ANGLE_2))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createTriangularPrism(width, length, (angle1), (angle2));
	}

	@Override
	public void visit(ImageSource v) {
		
		double length = ((EquationProperty)(v.getProperty(PropertyKey.TRANS_SIZE))).getValue().getBaseUnitValue();
		double height = ((EquationProperty)(v.getProperty(PropertyKey.ORTHO_SIZE))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createRectangle(length, height);	
	}

	@Override
	public void visit(SimpleAperture v) {
		
		double outerDiameter = ((EquationProperty)(v.getProperty(PropertyKey.OUTER_DIAMETER))).getValue().getBaseUnitValue();
		double innerDiameter = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createSimpleAperture(outerDiameter/2, innerDiameter/2);
	}

	@Override
	public void visit(SpectroPhotometer v) {		
		double diameter = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createCircle(diameter/2);

		
	}
	@Override
	public void visit(AutofocusSensor v) {		
		double diameter = ((EquationProperty)(v.getProperty(PropertyKey.DIAMETER))).getValue().getBaseUnitValue();
		
		m_component3D = OpticsObjects3D.createCircle(diameter/2);

		
	}
}
