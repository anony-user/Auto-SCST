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

import net.sourceforge.jocular.math.Polynomial;
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

public class RotatedSpline extends AbstractOpticsObject implements SplineObject, OpticsSurface {
	private SplineCoefficients[] m_coefficients = null;
	private EquationArrayProperty m_zPoints = new EquationArrayProperty("0.0", this, PropertyKey.POINTS_X);
	private EquationArrayProperty m_radPoints = new EquationArrayProperty("0.0", this, PropertyKey.POINTS_Y);
	private EnumArrayProperty m_pointTypes = new EnumArrayProperty(PointType.CUSP, "CUSP");
	private boolean m_splineDirty = true;
	private EquationProperty m_simplifyThreshold = new EquationProperty("0.0", this, PropertyKey.SIMPLIFY_THRESHOLD);
	public RotatedSpline() {
		super();
	}
	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		new RotatedSplineInteractionCalculator(pt, this, is);
		
		

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
		int nz = m_zPoints.size();
		int nr = m_radPoints.size();
		int nt = m_pointTypes.size();
		
		int nm = (int)(Math.min(nz, Math.min(nr, nt)));
		//System.out.println("RotatedSpline.getSplinePointCount "+nz+","+nr+","+nt+","+nm);
		return nm;
	}
	@Override
	public void setProperty(PropertyKey key, String s){
		switch(key){
		case POINTS_TYPES:
			m_pointTypes = new EnumArrayProperty(PointType.CUSP, s);
			m_splineDirty = true;
			firePropertyUpdated(key);
			break;
		case POINTS_X:
			m_zPoints = new EquationArrayProperty(s, this, key);
			m_splineDirty = true;
			firePropertyUpdated(key);
			break;
		case POINTS_Y:
			m_radPoints = new EquationArrayProperty(s, this, key);
			m_splineDirty = true;
			firePropertyUpdated(key);
			break;
		case SIMPLIFY_THRESHOLD:
			m_simplifyThreshold = new EquationProperty(s, this, key);
			m_splineDirty = true;
			firePropertyUpdated(key);
			break;
		default:
			super.setProperty(key, s);
			break;
			
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
			result = m_zPoints;
			break;
		case POINTS_Y:
			result = m_radPoints;
			break;
		case SIMPLIFY_THRESHOLD:
			result = m_simplifyThreshold;
			break;
		default:
			result = super.getProperty(key);
			break;
			
		}
		return result;
	}
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.INSIDE_MATERIAL, PropertyKey.SIMPLIFY_THRESHOLD, PropertyKey.POINTS_X, PropertyKey.POINTS_Y, PropertyKey.POINTS_TYPES));
		return result;
	}
	@Override
	public OpticsObject makeCopy() {
		RotatedSpline result = new RotatedSpline();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	@Override
	public double[] getSplinePointIndepValues() {
		UnitedValue[] zvs = m_zPoints.getValue();
		int n = getSplinePointCount();
		double[] result = new double[n];
		
		for(int i = 0; i < n; ++i){
			double r = zvs[i].getBaseUnitValue();
			if(Double.isNaN(r)){
				throw new RuntimeException("Value is NaN.");
			}
			result[i] = r;
		}
		return result;
	}
	@Override
	public double[] getSplinePointDepValues() {
		UnitedValue[] rvs = m_radPoints.getValue();
		int n = getSplinePointCount();
		double[] result = new double[n];
		
		for(int i = 0; i < n; ++i){
			double r = rvs[i].getBaseUnitValue();
			if(Double.isNaN(r)){
				throw new RuntimeException("Value is NaN.");
			}
			result[i] = r;
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
		ArrayList<SplineCoefficients> coeffs = new ArrayList<SplineCoefficients>();
		SplineCoefficients[] scs = SplineMath.fitCoefficients(this, m_simplifyThreshold.getValue().getBaseUnitValue());
		int z = -1;
		for(int i = 0; i < scs.length; ++i){
			SplineCoefficients sc = scs[i];
			//if it looks like the spline coeffs being tested are on the z axis then don't keep them
			if(sc.ySpline.largestCoefficient() < Polynomial.EPSILON){
				z = i;
			}
		}
		int j = 0;
		for(int i = 0; i < scs.length; ++i){
			SplineCoefficients sc = scs[i];
			if(i < z){
				coeffs.add(sc);
			} else if(i > z){
				coeffs.add(j, sc);
				++j;
			}
		}
		m_coefficients = coeffs.toArray(new SplineCoefficients[coeffs.size()]);
		m_splineDirty = false;
		
	}

	
	
	

}
