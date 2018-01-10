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
import java.util.List;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;


public class CylindricalSurface extends AbstractOpticsObject implements OpticsObject, OpticsSurface {
	

	protected EquationProperty m_diameter = new EquationProperty(0);
	protected EquationProperty m_length = new EquationProperty(0);

	
	
	/**
	 * creates a new Cylindrical surface.
	 */
	public CylindricalSurface(){
		
		m_positioner = new OffsetPositioner();
	}


	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		Photon p = pt.getPhoton();
		PhotonInteraction pi = getPossibleInteraction(p);
		if(pi != null){
			is.add(new PhotonInteraction(p, this, null, pi.getLocation(), pi.getNormal(), pi.isFromInside(), pi.getComment()));
		}
	}
	@Override
	public PhotonInteraction getPossibleInteraction(Photon p) {
		//PhotonInteraction result = null;
		//Photon p = pt.getPhoton();
		Vector3D po = p.getOrigin();
		Vector3D pd = p.getDirection();
		Vector3D co = getPositioner().getOrigin();
		Vector3D cd = getPositioner().getDirection();
		
		//calculate cylinder centre that is in plane perpendicular to cylinder direction and in plane with photon origin
		Vector3D poI = po.subtract(co);//vector from photon origin to cylinder origin
		
		//now work in frame (II) which is the plane perpendicular to the cylinder containing the photon origin
		//Vector3D coII = co.add(cd.scale(poIDot));//shift the cylinder origin to the plane of the normal component of the photon
		Vector3D pdII = pd.getPerpendicularComponent(cd);//get component of photon direction perpendicular to cylinder direction
		double pdIImag = pdII.abs();//get component of photon direction perpendicular to cylinder direction
		if(pdIImag < 1e-20){//if the pert component is essentially zero then the photon will not cross.
			return null;
		}
		Vector3D poII = poI.getPerpendicularComponent(cd);
		
		//now setup to solve the cosine law C^2 = A^2 + B^2 + 2ABcos(theta)
		//C is cylinder radius = r
		//A is distance along pdNormII from poII to cylinder = what we're solving for
		//B is distance from poII to cylinder centre = poII.abs()
		//Bcos(theta) = pdNormII.dot(poII)
		
		//to solve we setup a quadratic of the form ax^2 + bx + c = 0 where:
		//x is A above
		//a = 1
		//b = 2Bcos(theta) = 2*pdII.dot(poII)/pdIImag
		//c = B^2 - C^2 = poII.magSquared() - r^2
		//and then x = (-b +/- sqrt(b^2 - 4c))/2
		//first calc sqrt
		double r = m_diameter.getValue().getBaseUnitValue()/2.0d;
		double b = 2*pdII.dot(poII)/pdIImag;
		double c = poII.magSquared() - Math.pow(r, 2);
		double sqrt = b*b-4*c;
		
		if(sqrt < 0){
			return null;
		}
		sqrt = Math.sqrt(sqrt);

		//now calc x, which is to say A of the cosine law.
		double x1 = (-b + sqrt)/2;
		double x2 = (-b - sqrt)/2;
		//if both solutions are less than zero then we have no intersection
		double x;
		if(x1 < 0 && x2 < 0){
			return null;
		} else if(x1 < 0){
			x = x2;
		} else if(x2 < 0){
			x = x1;
		} else {
			x = (x1<x2)?x1:x2;
		}
	
		//now scale pd by this and add to original photon origin
		Vector3D pdScaled = pd.scale(x/pdIImag);
		Vector3D intersection = po.add(pdScaled);
		
		//now see if it falls within the length of this cylinder
		double d = Math.abs(intersection.subtract(co).dot(cd));
		if(d > m_length.getValue().getBaseUnitValue()/2){
			return null;
		}
		Vector3D n = intersection.subtract(co).getPerpendicularComponent(cd).normalize();
		boolean fromInside = n.dot(pd) >= 0;
		PhotonInteraction result = new PhotonInteraction(p, this, null, intersection, n, fromInside,"Cylinder interaction");
		return result;
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case DIAMETER:
			m_diameter = new EquationProperty(s, this, key);
			if(m_diameter.getValue().getBaseUnitValue() < 0){
				throw new RuntimeException("Diameter of CylindricalSurface cannot be less than zero. "+m_diameter.getDefiningString());
			}
			break;
		case THICKNESS:
			m_length = new EquationProperty(s, this, key);
			if(m_length.getValue().getBaseUnitValue() < 0){
				throw new RuntimeException("Thickness of CylindricalSurface cannot be less than zero. "+m_length.getDefiningString());
			}
			break;
			
		default:
			super.setProperty(key, s);
			break;
		}
	}

	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	
	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case DIAMETER:
			result = m_diameter;
			break;
		case THICKNESS:
			result = m_length;
			break;
		default:
			result = super.getProperty(key);
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.RADIUS, PropertyKey.DIAMETER, PropertyKey.SPHERICAL_SHAPE, PropertyKey.INSIDE_MATERIAL));
		return result;
	}
	
//	@Override
//	public List<Property<?>> getProperties() {
//		List<Property<?>> result = super.getProperties();
//		result.addAll(asList(m_diameter, m_length));
//		return result;
//	}
}
