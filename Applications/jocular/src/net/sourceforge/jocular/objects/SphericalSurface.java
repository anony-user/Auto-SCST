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
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.MaterialProperty;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.settings.Settings;


/**
 * a concave or convex surface that can be up to one half hemisphere (and no bigger)
 * the positioner origin is specified as where the surface crosses the optical axis
 * @author kmaccallum
 *
 */
public class SphericalSurface extends AbstractOpticsObject implements OpticsObject, OpticsSurface {
	

	protected EquationProperty m_radius;//radius of sphere
	protected EquationProperty m_diameter;//diameter of sphere surface
	public enum SurfaceShape {CONCAVE("Concave"), CONVEX("Convex"), FLAT("Flat");
		private final String m_name;
		private SurfaceShape(String n){
			m_name = n;
		}
		public String toString(){
			return m_name;
		}
	}
	protected EnumProperty m_surfaceShape;
	MaterialProperty m_backMaterial;

	public SphericalSurface(){
		
		
		
		//set some default values
		m_radius = new EquationProperty("15m", this, PropertyKey.RADIUS);
		m_diameter = new EquationProperty("5m", this, PropertyKey.DIAMETER);
		m_surfaceShape = new EnumProperty(SurfaceShape.CONCAVE, SurfaceShape.CONCAVE.name());
		m_backMaterial = new MaterialProperty(MaterialKey.BOROSILICATE.name());
		//setProperty(PropertyKey.OUTSIDE_MATERIAL, MaterialKey.VACUUM.name());
		
		m_positioner = new OffsetPositioner();
		
	}
	


	public Vector3D getSphereCentre(){
		//if m_radius is > 0 then convex so sphere centre should be down the direction vector, else behind the direction vector
		Vector3D dir = getPositioner().getDirection();
		Vector3D orig = getPositioner().getOrigin();
		Vector3D result;
		switch((SurfaceShape)(m_surfaceShape.getValue())){
		case CONVEX:
			result = orig.add(dir.scale(m_radius.getValue().getBaseUnitValue()));
			break;
		case CONCAVE:
			result = orig.add(dir.scale(-m_radius.getValue().getBaseUnitValue()));
			break;
		default:
		case FLAT:
			result = orig;
			break;
				
		}
		
		
		return result;
	}

	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
//		Photon p = pt.getPhoton();
//		PhotonInteraction pi = getPossibleInteraction(p);
//		if(pi != null){
//			is.add(new PhotonInteraction(p, this, pi.getLocation(), pi.getNormal(), pi.isFromInside(), ""));
//		}
	}
	@Override
	public PhotonInteraction getPossibleInteraction(Photon p) {

		//Vector3D i = getIntersection(p.getOrigin(), p.getDirection());
		Vector3D origin = p.getOrigin();
		Vector3D direction = p.getDirection();
		final Vector3D pResult;
		Vector3D normal = null;
		boolean fromInside = false;
		switch((SurfaceShape)(m_surfaceShape.getValue())){
		default:
		case CONCAVE:
		case CONVEX:
			Vector3D c = getSphereCentre();
			Vector3D o = origin;
			Vector3D l = direction.normalize();//ensure photon direction is normalized. This should be true anyway.
			Vector3D d = c.subtract(o);//centre of sphere with photon origin as reference
			double dot = l.dot(d);
			//solve for the intersection using the cosine law
			double sqrt = Math.pow(dot,2) - d.magSquared() + Math.pow(m_radius.getValue().getBaseUnitValue(), 2);
			
			if(sqrt < 0){
				pResult = null;
			} else {
				//now we have two choices
				sqrt = Math.sqrt(sqrt);
				double dist1 = dot - sqrt;
				double dist2 = dot + sqrt;
				//now we have to figure out which one is THE one. So first calculate the locations of these points
				Vector3D p1 = o.add(l.scale(dist1));
				Vector3D p2 = o.add(l.scale(dist2));
			
				//let's work through p1 first
				//see if it's close enough to the axis (within diameter/2)
				Vector3D dir = getPositioner().getDirection();
				double dia1 = p1.subtract(c).getPerpendicularComponent(dir).abs() * 2.0;
				double dia2 = p2.subtract(c).getPerpendicularComponent(dir).abs() * 2.0;
				if(dia1 > m_diameter.getValue().getBaseUnitValue()){
					p1 = null;
				}
				if(dia2 > m_diameter.getValue().getBaseUnitValue()){
					p2 = null;
				}
				//check to see if they're both on the front surface
				if(!isOnFrontHemisphere(p1)){
					p1 = null;
				}
				if(!isOnFrontHemisphere(p2)){
					p2 = null;
				}
				//check that they're in front of the photon trajectory
				if(p1 != null){
					if(p1.subtract(origin).dot(direction) <= -Settings.MIN_DISTANCE){
						p1 = null;
					}
				}
				if(p2 != null){
					if(p2.subtract(origin).dot(direction) <= -Settings.MIN_DISTANCE){
						p2 = null;
					}
				}
				if(p1 != null && p2 != null){
					if(dist1 < dist2){
						pResult = p1;
					} else {
						pResult = p2;
					}
				} else if(p1 != null){
					pResult = p1;
				} else if(p2 != null){
					pResult = p2;
				} else {
					pResult = null;
				}
			}
			if(pResult != null){
				normal = getSphereCentre().subtract(pResult).normalize();
				if(m_surfaceShape.getValue() == SurfaceShape.CONCAVE){
					//System.out.println("SphericalSurface.getPossible... concave.");
					normal = normal.neg();
				}
			}
			//reverse normal if concave
			
			
			
				
			
			break;
		case FLAT:
			//Vector3D p2this = getPositioner().getOrigin().subtract(origin);
			//TODO:this test is wrong!
			//if(getPositioner().getDirection().getParallelComponent(p2this).dot(direction) <= 0){
			Vector3D sO = getPositioner().getOrigin();
			Vector3D sD = getPositioner().getDirection();
			Vector3D pO = origin;
			Vector3D pD = direction;
			//calc the cos of the angle between the surface normal vector and the photon direction
			double cos = sD.dot(pD);
			
			//calc the normal distance from the surface to the photon origin
			double oDist = sO.subtract(pO).dot(sD);
			double dDist = 0;
			if(cos != 0){
			
				dDist = oDist/cos;
			}
			if(dDist <= 0){
				pResult = null;
			} else {
				pResult = pO.add(pD.scale(dDist));
			}
			
//			if(p2this.dot(direction) <= 0){
//				//photon is not traveling towards this surface
//				pResult = null;
//			} else {
//				double normDist = Math.abs(p2this.dot(getPositioner().getDirection()));
//				double dotF = Math.abs(direction.dot(getPositioner().getDirection()));
//				double dist = normDist/dotF;
//				Vector3D pP;
//				//check to see that it's within the diameter of the lens
//				pP = origin.add(direction.scale(dist));
//				if(pP.subtract(getPositioner().getOrigin()).abs() > m_diameter.getValue()/2){
//					pResult = null;
//				} else {
//					pResult = pP;
//				}
//			}
			if(pResult != null){
				normal = getPositioner().getDirection();
				//System.out.println("SphericalSurface.getPossibleInteraction flat interaction at "+pResult);
			}
			break;
		}
		
		
		PhotonInteraction pi = null;
		if(pResult != null){
			//normal points to inside
			if(normal.dot(direction) > 0){
				fromInside = false;
			} else {
				fromInside = true;
			}
			pi = new PhotonInteraction(p, this, null, pResult, normal, fromInside, "Spherical Interaction: "+m_surfaceShape.getDefiningString());
		}
		return pi;
	}

	/**
	 * Checks if a point on the sphere is on the front hemisphere or not
	 * @param p
	 * @return
	 */
	private boolean isOnFrontHemisphere(Vector3D p){
		if(p == null){
			return false;
		}
		boolean result = false;
		switch((SurfaceShape)(m_surfaceShape.getValue())){
		case CONVEX:
		case CONCAVE:
			Vector3D c = this.getSphereCentre();
			Vector3D o = getPositioner().getOrigin();
			Vector3D deltaP = c.subtract(p);
			Vector3D deltaO = c.subtract(o);
			double dot = deltaP.dot(deltaO);
			if(dot >= 0.0){
				result = true;
			}
			break;
		case FLAT:
		default:
			result = true;
			break;
		}
		return result;
	}
	/**
	 * front to back means from outside material to inside material
	 * @param point
	 * @param direction
	 * @return
	 */
//	private boolean isFrontToBack(Vector3D point, Vector3D direction){
//		boolean result;
//		SurfaceShape s = (SurfaceShape)m_surfaceShape.getValue();
//		
//		switch(s){
//		case CONVEX:
//			result = getSphereCentre().subtract(point).dot(direction) >= 0;
//			break;
//		case CONCAVE:
//			result = getSphereCentre().subtract(point).dot(direction) < 0;
//			break;
//		case FLAT:
//			result = getPositioner().getDirection().dot(direction) >= 0;
//			break;
//		default:
//			throw new RuntimeException("Surface Shape not defined when this code was written.");
//		}
//		return result;
//	}
//	private Vector3D getNormal(Vector3D point, Vector3D direction) {
//		//now calc normal vector if we still have an intersecting point
//		Vector3D result;
//		switch((SurfaceShape)(m_surfaceShape.getValue())){
//		default:
//		case CONVEX:
//		case CONCAVE:
//			if(point != null){
//				result = getSphereCentre().subtract(point).normalize();//point.subtract(getSphereCentre());
////				if(direction.dot(result) > 0){
////					result = result.neg();
////				}
//			} else {
//				result = null;
//			}
//			break;
//		case FLAT:
//			result = getPositioner().getDirection();
////			if(direction.dot(result) > 0){
////				result = result.neg();
////			}
//			break;
//		}
//		return result;
//	}

	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case RADIUS:
			m_radius = new EquationProperty(s, this, key);
			if(m_radius.getValue().getBaseUnitValue() < 0){
				throw new RuntimeException("Radius of SphericalSurface cannot be less than zero. "+m_radius.getDefiningString());
			}
			break;
		case DIAMETER:
			m_diameter = new EquationProperty(s, this, key);
			if(m_diameter.getValue().getBaseUnitValue() < 0){
				throw new RuntimeException("Diameter of SphericalSurface cannot be less than zero. "+m_diameter.getDefiningString());
			}
			break;
		case SPHERICAL_SHAPE:
			m_surfaceShape = new EnumProperty(SurfaceShape.CONCAVE, s);
			break;
		case INSIDE_MATERIAL:
			m_backMaterial = new MaterialProperty(s);
			break;
		default:
			super.setProperty(key, s);
			break;
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case RADIUS:
			result = m_radius;
			break;
		case DIAMETER:
			result = m_diameter;
			break;
		case SPHERICAL_SHAPE:
			result = m_surfaceShape;
			break;
		case INSIDE_MATERIAL:
			result = m_backMaterial;
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
//		result.addAll(asList(m_radius, m_diameter, m_surfaceShape, m_backMaterial));
//		return result;
//	}


	public void accept(OpticsObjectVisitor visitor){
		visitor.visit(this);
	}
	
}
