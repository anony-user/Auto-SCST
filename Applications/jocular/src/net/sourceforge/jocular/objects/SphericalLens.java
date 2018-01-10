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
package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.SphericalSurface.SurfaceShape;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.positioners.ReverseOffsetPositioner;
import net.sourceforge.jocular.project.OpticsObjectElement;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.MaterialProperty;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;


public class SphericalLens extends AbstractOpticsObject implements OpticsObject, OpticsSurface, OpticsObjectCombo, OpticsObjectElement {
	PropertyUpdatedListener m_objectListener;
	private final SphericalSurface m_frontSurface = new SphericalSurface();
	private final SphericalSurface m_backSurface = new SphericalSurface();
	private final CylindricalSurface m_outerSurface= new CylindricalSurface();

	//private  MaterialProperty m_insideMaterial = new MaterialProperty(MaterialKey.VACUUM.name());
	//private LengthProperty m_thickness = new LengthProperty("2cm");
	private EquationProperty m_thickness = new EquationProperty("2cm", this, PropertyKey.THICKNESS);
	private EquationProperty m_frontRadius = new EquationProperty("10cm", this, PropertyKey.FRONT_RADIUS);
	private EquationProperty m_backRadius = new EquationProperty("10cm", this, PropertyKey.BACK_RADIUS);
	private EquationProperty m_diameter = new EquationProperty("5cm", this, PropertyKey.DIAMETER);
	
	/**
	 * 
	 * @param frontRadius radius of front surface of lens. 
	 * @param backRadius radius of back surface of lens.
	 * @param diameter
	 * @param thickness the thickness of the lens measured along the centre-line
	 */
	public SphericalLens(){
		m_objectListener = new PropertyUpdatedListener(){
			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
//				for(OpticsObject o : getObjects()){
//					o.updatePositioner(getPositioner());
//				}
				
				firePropertyUpdated(e.getPropertyKey());
			}
		};
		//m_frontSurface.addPropertyUpdatedListener(m_objectListener);
		//m_backSurface.addPropertyUpdatedListener(m_objectListener);
		//m_outerSurface.addPropertyUpdatedListener(m_objectListener);
		setSubObjectProperties();
		m_frontSurface.setPositioner(new OffsetPositioner());
		m_backSurface.setPositioner(new ReverseOffsetPositioner());
		m_outerSurface.setPositioner(new OffsetPositioner());		
		setPositioner(new OffsetPositioner());
		setProperty(PropertyKey.FRONT_RADIUS,"15cm");
		setProperty(PropertyKey.BACK_RADIUS,"20cm");
		setProperty(PropertyKey.DIAMETER,"5cm");
		setProperty(PropertyKey.THICKNESS,"5mm");
		setProperty(PropertyKey.INSIDE_MATERIAL,MaterialKey.BOROSILICATE.name());
	}
	public Vector3D getFrontSphereCentre(){
		return m_frontSurface.getSphereCentre();
	}
	public Vector3D getBackSphereCentre(){
		return m_backSurface.getSphereCentre();
	}
	
	@Override
	public void updatePositioner(ObjectPositioner parentPositioner) {
		super.updatePositioner(parentPositioner);
		m_frontSurface.updatePositioner(getPositioner());
		m_backSurface.updatePositioner(getPositioner());
		m_outerSurface.updatePositioner(getPositioner());
	}
	@Override
	public void positionerUpdated() {
		super.positionerUpdated();
		m_frontSurface.updatePositioner(getPositioner());
		m_backSurface.updatePositioner(getPositioner());
		m_outerSurface.updatePositioner(getPositioner());
		
	}
	@Override
	public PhotonInteraction getPossibleInteraction(Photon p) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void doInternalCalcs(){	
		//System.out.println("SphericalLens.updateDimensions "+getName());
		if(m_frontSurface == null || m_backSurface == null || m_outerSurface == null){
			return;
		}
		//double frontRadius = (Double)(m_frontSurface.getProperty(PropertyKey.RADIUS).getValue());
		//double backRadius = (Double)(m_backSurface.getProperty(PropertyKey.RADIUS).getValue());
		//double diameter = (Double)(m_outerSurface.getProperty(PropertyKey.DIAMETER).getValue());
		double frontRadius = m_frontRadius.getValue().getBaseUnitValue();
		double backRadius = m_backRadius.getValue().getBaseUnitValue();
		double diameter = m_diameter.getValue().getBaseUnitValue();
		double thickness = m_thickness.getValue().getBaseUnitValue();
		
		if(frontRadius < diameter/2 || backRadius < diameter/2){
			//throw(new RuntimeException("Lens diameter is larger than front or back radius. Diameter: "+diameter+", front radius: "+frontRadius+", back back radius:"+backRadius));
		}
		

		double t1 = frontRadius - Math.sqrt(Math.pow(frontRadius, 2) - Math.pow(diameter/2, 2));
		double t2 = backRadius - Math.sqrt(Math.pow(backRadius, 2) - Math.pow(diameter/2, 2));
		
		SurfaceShape frontShape = (SurfaceShape)(m_frontSurface.getProperty(PropertyKey.SPHERICAL_SHAPE).getValue());
		SurfaceShape backShape = (SurfaceShape)(m_backSurface.getProperty(PropertyKey.SPHERICAL_SHAPE).getValue());
		
		switch(frontShape){
		case CONVEX:
			t1 = -t1;
			break;
		case FLAT:
			t1 = 0;
			break;
		case CONCAVE:
		default:
			break;
		}
		switch(backShape){
		case CONVEX:
			t2 = -t2;
			break;
		case FLAT:
			t2 = 0;
			break;
		case CONCAVE:
		default:
			break;
		}
		//System.out.println("SphericalLens t1 = "+ t1 + ", t2 = "+t2);
		double l = thickness + t1 + t2;
		
		if(l < 0){
			//throw(new RuntimeException("Lens thickness is not enough for specified diameter or radii, by "+(-l)+" m"));
		}
		if(Double.isInfinite(t2 + t1 + thickness) || Double.isNaN(t2 + t1 + thickness)){
			//throw new RuntimeException("t2 = "+t1+", t1 = "+t1+", thickness = "+thickness);
			t2 = 0;
			t1 = 0;

		}
		m_frontSurface.getPositioner().setProperty(PropertyKey.DIR_OFFSET, String.valueOf(-thickness/2));
		m_backSurface.getPositioner().setProperty(PropertyKey.DIR_OFFSET, String.valueOf(thickness/2));
		m_outerSurface.getPositioner().setProperty(PropertyKey.DIR_OFFSET, String.valueOf((t2 - t1)/2));
		
		if(l > 0)
			m_outerSurface.setProperty(PropertyKey.THICKNESS, String.valueOf(l));
	}
	


	@Override
	public void setPositioner(ObjectPositioner op) {
		super.setPositioner(op);
//		if(m_frontSurface != null){
//			m_frontSurface.getPositioner().setPositioner(op);
//		}
//		if(m_backSurface != null){
//			m_backSurface.getPositioner().setPositioner(op);
//		}
//		if(m_outerSurface != null){
//			m_outerSurface.getPositioner().setPositioner(op);
//		}
		
	}
	


	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
//		PhotonInteraction pi = null;
//		Photon p = pt.getPhoton();
//		for(OpticsObject o : getObjects()){
//			PhotonInteraction pi2 = ((OpticsSurface)o).getPossibleInteraction(p);
////			if(pi2 != null){
////				System.out.println("SphericalLens.getPossible... distance: "+pi2.getDistance());
////			}
//			if(pi2 == null || pi2.getDistance() <= Settings.MIN_DISTANCE){
//			} else if(pi == null){
//				pi = pi2;
//			} else if(pi2.isCloserThan(pi)){
//				pi = pi2;
//			}
//		}
//		if(pi != null){
//			boolean fromInside = pt.getContainingObject() == this;
//			//boolean fromInside = pi.isFromInside();
//			pi = new PhotonInteraction(p, this, pi.getLocation(), pi.getNormal(), fromInside, pi.getComment());
//			is.add(pi, this);
//		}
		
		Photon p = pt.getPhoton();
		boolean fromInside = pt.getContainingObject() == this;
		for(OpticsObject o : getObjects()){
			PhotonInteraction pi = ((OpticsSurface)o).getPossibleInteraction(p);
			if(pi != null){
				
				is.add(new PhotonInteraction(p, this, o, pi.getLocation(), pi.getNormal(), fromInside, pi.getComment()));
			}
			
		}
		
	}




	@Override
	public Collection<OpticsObject> getObjects(){
		ArrayList<OpticsObject> result = new ArrayList<OpticsObject>();
		result.add(m_frontSurface);
		result.add(m_backSurface);
		result.add(m_outerSurface);
		return result;
	}



	@Override
	public OpticsObject makeCopy() {
		SphericalLens result = new SphericalLens();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case FRONT_RADIUS:
		case BACK_RADIUS:	
		case DIAMETER:
		case THICKNESS:
			return InputVerificationRules.verifyPositiveLength(s, this, key);
			
		default:
			return super.trySetProperty(key, s);
		}
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
				
		switch(key){
		case FRONT_RADIUS:
//			if(m_frontSurface != null){
//				m_frontSurface.setProperty(PropertyKey.RADIUS, s);
//				updateDimensions();				
//			}
			m_frontRadius = new EquationProperty(s, this, key);
			doInternalCalcs();
			break;
		case FRONT_SHAPE:
			if(m_frontSurface != null){
				m_frontSurface.setProperty(PropertyKey.SPHERICAL_SHAPE, s);
				doInternalCalcs();
			}
			break;
		case BACK_RADIUS:
//			if(m_backSurface != null){
//				m_backSurface.setProperty(PropertyKey.RADIUS, s);
//				updateDimensions();
//			}
			m_backRadius = new EquationProperty(s, this, key);
			doInternalCalcs();
			break;
		case BACK_SHAPE:
			if(m_backSurface != null){
				m_backSurface.setProperty(PropertyKey.SPHERICAL_SHAPE, s);
				doInternalCalcs();
			}
			break;
		case DIAMETER:
//			if(m_frontSurface != null){
//				m_frontSurface.setProperty(PropertyKey.DIAMETER, s);
//			}
//			if(m_backSurface != null){
//				m_backSurface.setProperty(PropertyKey.DIAMETER, s);
//			}
//			if(m_outerSurface != null){
//				m_outerSurface.setProperty(PropertyKey.DIAMETER, s);
//			}
			m_diameter = new EquationProperty(s, this, key);
			doInternalCalcs();
			break;
		case THICKNESS:
			//m_thickness = new LengthProperty(s);
			m_thickness = new EquationProperty(s, this, key);
			
			doInternalCalcs();
			break;
		case INSIDE_MATERIAL:
			super.setProperty(PropertyKey.INSIDE_MATERIAL, s);
			if(m_frontSurface != null){
				m_frontSurface.setProperty(PropertyKey.INSIDE_MATERIAL, s);
			}
			if(m_backSurface != null){
				m_backSurface.setProperty(PropertyKey.INSIDE_MATERIAL, s);
			}
			if(m_outerSurface != null){
				m_outerSurface.setProperty(PropertyKey.INSIDE_MATERIAL, s);
			}
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
		Property<?> result;
		switch(key){
		case FRONT_RADIUS:
			//result = m_frontSurface.getProperty(PropertyKey.RADIUS);
			result = m_frontRadius;
			break;
		case FRONT_SHAPE:
			result = m_frontSurface.getProperty(PropertyKey.SPHERICAL_SHAPE);
			break;
		case BACK_RADIUS:
			//result = m_backSurface.getProperty(PropertyKey.RADIUS);
			result = m_backRadius;
			break;
		case BACK_SHAPE:
			result = m_backSurface.getProperty(PropertyKey.SPHERICAL_SHAPE);
			break;
		case DIAMETER:
			//result = m_outerSurface.getProperty(PropertyKey.DIAMETER);
			result = m_diameter;
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
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.DIAMETER, PropertyKey.THICKNESS, PropertyKey.FRONT_SHAPE, PropertyKey.FRONT_RADIUS, PropertyKey.BACK_SHAPE, PropertyKey.BACK_RADIUS, PropertyKey.INSIDE_MATERIAL));//, PropertyKey.OUTSIDE_MATERIAL));
		return result;
	}

//	@Override
//	public List<Property<?>> getProperties() {
//		List<Property<?>> result = super.getProperties();
//		result.addAll(m_frontSurface.getProperties());
//		result.addAll(m_backSurface.getProperties());
//		result.addAll(m_outerSurface.getProperties());
//		result.add(m_thickness);
//		return result;
//	}

	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	public double getFocalLength(double w){
		double r1 = ((EquationProperty)(getProperty(PropertyKey.FRONT_RADIUS))).getValue().getBaseUnitValue();
		double r2 = ((EquationProperty)(getProperty(PropertyKey.BACK_RADIUS))).getValue().getBaseUnitValue();
		double oneOverR1 = 1.0d/r1;
		double oneOverR2 = -1.0d/r2;
		double t = ((EquationProperty)(getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
		SurfaceShape shape1 = (SurfaceShape)getProperty(PropertyKey.FRONT_SHAPE).getValue();
		SurfaceShape shape2 = (SurfaceShape)getProperty(PropertyKey.BACK_SHAPE).getValue();
		switch(shape1){
		case CONVEX:
		default:
			break;
		case CONCAVE:
			oneOverR1 = -oneOverR1;
			break;
		case FLAT:
			oneOverR1 = 0.0d;
			break;
		}
		switch(shape2){
		case CONVEX:
		default:
			break;
		case CONCAVE:
			oneOverR2 = -oneOverR2;
			break;
		case FLAT:
			oneOverR2 = 0.0d;
			break;
		}
		OpticalMaterial m = ((MaterialProperty)getProperty(PropertyKey.INSIDE_MATERIAL)).getValue();
		double n = m.getOrdinaryRefractiveIndex(w);
		double oneOverF = (n - 1.0d)*(oneOverR1 - oneOverR2 + (n - 1.0d)/n*t*oneOverR1*oneOverR2);
		return 1.0d/oneOverF;
		
	}
//	@Override
//	public boolean calcEquations() {
//		boolean result = super.calcEquations();
//		updateDimensions();
//		for(OpticsObject o : getObjects()){
//			if(!o.calcEquations()){
//				result = false;
//			}
//		}
//		return result;
//		
//	}
//	@Override
//	public boolean calcEquation(PropertyKey key) {
//		boolean result = super.calcEquation(key);
//		updateDimensions();
//		//System.out.println("SphericalLens.calcEquation: "+getProperty(PropertyKey.THICKNESS));
//		for(OpticsObject o : getObjects()){
//			if(!o.calcEquations()){
//				result = false;
//				
//			}
//		}
//		return result;
//	}
	private void setSubObjectProperties(){
		String id = getID().toHashString();
		m_frontSurface.setProperty(PropertyKey.RADIUS, id+">front radius");
		m_frontSurface.setProperty(PropertyKey.DIAMETER, id+">diameter");
		m_backSurface.setProperty(PropertyKey.RADIUS, id+">back radius");
		m_backSurface.setProperty(PropertyKey.DIAMETER, id+">diameter");
		m_outerSurface.setProperty(PropertyKey.DIAMETER, id+">diameter");
		
	}
	@Override
	public void setID(OpticsID id) {
		super.setID(id);
		setSubObjectProperties();
	}
	
	
}
