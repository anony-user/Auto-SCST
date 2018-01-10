/*******************************************************************************
 * Copyright (c) 2014, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.splines;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.objects.AbstractOpticsObject;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsSurface;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EnumArrayProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

public class ExtrudedSpline extends AbstractOpticsObject implements SplineObject, OpticsSurface {
	private SplineCoefficients[] m_coefficients = null;
	private EquationProperty m_thickness = new EquationProperty("10mm", this, PropertyKey.THICKNESS);
	private EquationArrayProperty m_xPoints = new EquationArrayProperty("0.0", this, PropertyKey.POINTS_X);
	private EquationArrayProperty m_yPoints = new EquationArrayProperty("0.0", this, PropertyKey.POINTS_Y);
	private EnumArrayProperty m_pointTypes = new EnumArrayProperty(PointType.CUSP, "CUSP");
	private boolean m_splineDirty = true;
	private EquationProperty m_simplifyThreshold = new EquationProperty("0.0", this, PropertyKey.SIMPLIFY_THRESHOLD);
	public ExtrudedSpline() {
		super();
	}
	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		new ExtrudedSplineInteractionCalculator(pt, this, is);
		
		

	}

	
	@Override
	public PhotonInteraction getPossibleInteraction(Photon p) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);

	}

	@Override
	public int getSplinePointCount() {
		int nx = m_xPoints.size();
		int ny = m_yPoints.size();
		int nt = m_pointTypes.size();
		
		int nm = (int)(Math.min(nx, Math.min(ny, nt)));
		//System.out.println("RotatedSpline.getSplinePointCount "+nz+","+nr+","+nt+","+nm);
		return nm;
	}
	@Override
	public void setProperty(PropertyKey key, String s){
		switch(key){
		case POINTS_TYPES:
			m_pointTypes = new EnumArrayProperty(PointType.CUSP, s);
			m_splineDirty = true;
			break;
		case POINTS_X:
			m_xPoints = new EquationArrayProperty(s, this, key);
			m_splineDirty = true;
			break;
		case POINTS_Y:
			m_yPoints = new EquationArrayProperty(s, this, key);
			m_splineDirty = true;
			break;
		case SIMPLIFY_THRESHOLD:
			m_simplifyThreshold = new EquationProperty(s, this, key);
			m_splineDirty = true;
			break;
		case THICKNESS:
			m_thickness = new EquationProperty(s, this, key);
			break;
		default:
			super.setProperty(key, s);
			break;
			
		}
		if(getPropertyKeys().contains(key)){
			firePropertyUpdated(key);
		}
	}
	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case POINTS_TYPES:
			result = m_pointTypes;
			break;
		case POINTS_X:
			result = m_xPoints;
			break;
		case POINTS_Y:
			result = m_yPoints;
			break;
		case SIMPLIFY_THRESHOLD:
			result = m_simplifyThreshold;
			break;
		case THICKNESS:
			result = m_thickness;
			break;
		default:
			result = super.getProperty(key);
			break;
			
		}
		return result;
	}
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.INSIDE_MATERIAL, PropertyKey.SIMPLIFY_THRESHOLD, PropertyKey.POINTS_X, PropertyKey.THICKNESS, PropertyKey.POINTS_Y, PropertyKey.POINTS_TYPES));
		return result;
	}
	@Override
	public OpticsObject makeCopy() {
		ExtrudedSpline result = new ExtrudedSpline();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	@Override
	public double[] getSplinePointIndepValues() {
		UnitedValue[] xvs = m_xPoints.getValue();
		int n = getSplinePointCount();
		double[] result = new double[n];
		
		for(int i = 0; i < n; ++i){
			result[i] = xvs[i].getBaseUnitValue();
		}
		return result;
	}
	@Override
	public double[] getSplinePointDepValues() {
		UnitedValue[] yvs = m_yPoints.getValue();
		int n = getSplinePointCount();
		double[] result = new double[n];
		
		for(int i = 0; i < n; ++i){
			result[i] = yvs[i].getBaseUnitValue();
		}
		return result;
	}
	
	@Override
	public PointType[] getSplinePointTypes() {
		int n = m_pointTypes.size();
		PointType[] result = new PointType[n];
		for(int i = 0; i < n; ++i){
			result[i] = (PointType)(m_pointTypes.getValue()[i]);
		}
		return result;
		//return (PointType[])(m_pointTypes.getValue());
	}
	@Override
	public SplineCoefficients[] getSplineCoefficients() {
		if(m_splineDirty){
			doInternalCalcs();
		}
		return m_coefficients;
	}
	@Override
	public void doInternalCalcs() {
		//ArrayList<SplineCoefficients> coeffs = new ArrayList<SplineCoefficients>();
		if(!m_xPoints.isDeferred() && !m_yPoints.isDeferred()){
		
			SplineCoefficients[] scs = SplineMath.fitCoefficients(this, m_simplifyThreshold.getValue().getBaseUnitValue());
			
			m_coefficients = scs;
			m_splineDirty = false;
		}
		
	}

	
	
	

}
