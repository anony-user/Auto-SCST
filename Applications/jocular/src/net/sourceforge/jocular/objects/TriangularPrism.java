package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

public class TriangularPrism extends AbstractOpticsObject implements OpticsSurface {
	private EquationProperty m_baseWidth = new EquationProperty("10mm", this, PropertyKey.WIDTH);//width of base of triangle
	private EquationProperty m_angle1 = new EquationProperty("30deg", this, PropertyKey.ANGLE_1);//left angle of triangle
	private EquationProperty m_angle2 = new EquationProperty("90deg", this, PropertyKey.ANGLE_2);//right angle of triangle
	private EquationProperty m_length = new EquationProperty("10mm", this, PropertyKey.LENGTH);//length triangle is extruded to make the prism
	
	private Vector3D[] m_topTriangleVertices = new Vector3D[3];
	private Vector3D[] m_bottomTriangleVertices = new Vector3D[3];
	public TriangularPrism(){
		doInternalCalcs();
	}
	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);

	}
	public Vector3D[][] getVertices(){
		Vector3D[][] result = new Vector3D[2][3];
		result[0][0] = getPositioner().transform(m_topTriangleVertices[0]);
		result[0][1] = getPositioner().transform(m_topTriangleVertices[1]);
		result[0][2] = getPositioner().transform(m_topTriangleVertices[2]);
		result[1][0] = getPositioner().transform(m_bottomTriangleVertices[0]);
		result[1][1] = getPositioner().transform(m_bottomTriangleVertices[1]);
		result[1][2] = getPositioner().transform(m_bottomTriangleVertices[2]);
		return result;
	}

	@Override
	public PhotonInteraction getPossibleInteraction(Photon p) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		PhotonInteraction result = null;
		double distance = Double.POSITIVE_INFINITY;
		
		PhotonInteraction i;
		double d;
		Vector3D[][] ps = this.getVertices();
		Vector3D[][] testVectors = {{ps[0][0], ps[0][1], ps[0][2]},//test top
				{ps[1][2], ps[1][1], ps[1][0]},//test bottom
				
				{ps[0][0], ps[0][2], ps[1][2], ps[1][0]},//test one square side
				{ps[0][2], ps[0][1], ps[1][1], ps[1][2]},//test one square side
				{ps[0][1], ps[0][0], ps[1][0], ps[1][1]},//test one square side
				
				
				
		};
		Photon p = pt.getPhoton();
		boolean fromInside = pt.getContainingObject() == this;
		for(Vector3D[] ts : testVectors){
			i = getTriangleIntersection(ts, p, fromInside);
			if(i != null){
				
	
					
					is.add(i);
			
			}
		}
		
		
	}


	@Override
	public void doInternalCalcs(){
		double l = m_length.getValue().getBaseUnitValue();
		double w = m_baseWidth.getValue().getBaseUnitValue();
		double a1 = m_angle1.getValue().getBaseUnitValue();
		double a2 = m_angle2.getValue().getBaseUnitValue();
		double a3 = Math.PI - a1 - a2;
		double l2 = w/Math.sin(a3)*Math.sin(a2);//use sine law to figure out side lengths
		double l1 = w/Math.sin(a3)*Math.sin(a1);
		double p1z = l2*Math.sin(a1);
		double p1x = -(l2*Math.cos(a1) - l1*Math.cos(a2))/2;
		
		m_topTriangleVertices[0] = new Vector3D(w/2, l/2, 0);
		m_topTriangleVertices[1] = new Vector3D(p1x, l/2, p1z);
		m_topTriangleVertices[2] = new Vector3D(-w/2, l/2, 0);
		m_bottomTriangleVertices[0] = new Vector3D(w/2, -l/2, 0);
		m_bottomTriangleVertices[1] = new Vector3D(p1x, -l/2, p1z);
		m_bottomTriangleVertices[2] = new Vector3D(-w/2, -l/2, 0);
	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case LENGTH:
		case WIDTH:
			return InputVerificationRules.verifyPositiveLength(s, this, key);
		case ANGLE_1:
		case ANGLE_2:			
			return InputVerificationRules.verifyAngle(s, this, key);
			
		default:
			return super.trySetProperty(key, s);
		}
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
				
		switch(key){
		case LENGTH:
			m_length = new EquationProperty(s, this, key);
			break;
		case WIDTH:
			m_baseWidth = new EquationProperty(s, this, key);
			break;
		case ANGLE_1:
			m_angle1 = new EquationProperty(s, this, key);
			break;
		case ANGLE_2:
			m_angle2 = new EquationProperty(s, this, key);
			break;
		default:
			super.setProperty(key, s);
			break;
		}
		doInternalCalcs();
		
		if(getPropertyKeys().contains(key)){
			firePropertyUpdated(key);
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case LENGTH:
			result = m_length;
			break;
		case WIDTH:
			result = m_baseWidth;
			break;
		case ANGLE_1:
			result = m_angle1;
			break;
		case ANGLE_2:
			result = m_angle2;
			break;
		default:
			result = super.getProperty(key);
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.INSIDE_MATERIAL , PropertyKey.WIDTH, PropertyKey.LENGTH, PropertyKey.ANGLE_1, PropertyKey.ANGLE_2));//, PropertyKey.OUTSIDE_MATERIAL));
		return result;
	}
	
//	@Override
//	public List<Property<?>> getProperties() {
//		List<Property<?>> result = super.getProperties();
//		result.addAll(asList(m_length, m_baseWidth, m_angle1, m_angle2));
//		return result;
//	}

	@Override
	public OpticsObject makeCopy() {
		TriangularPrism result = new TriangularPrism();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	/**
	 *
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param origin
	 * @param direction
	 * @return an intersection point between the specified triangle and the path defined by the origin and direction. Null if none exists
	 */
	protected PhotonInteraction getTriangleIntersection(Vector3D[] p, Photon ph, boolean fromInside){
		Vector3D origin = ph.getOrigin();
		Vector3D direction = ph.getDirection();
		//first find where it crosses the plane defined by these points
		//to do that, first calc normal to plane
		Vector3D n = p[1].subtract(p[0]).cross(p[2].subtract(p[0])).normalize();
		//boolean fromInside = false;
		
		//correct the direction. It should always point towards the origin of the photon
		double dop = origin.subtract(p[0]).dot(n);//distance from photon origin to plane
		
		if(dop < 0){
			//todo: I don't think we need to do this anymore
			n = n.neg();
			dop = - dop;
			//fromInside = true;//for this to have happened then the photon must have originated inside the material
			//System.out.println("TriangularPrism.getTriangleIntersection from inside");
			
		}
		
		
		//if photon is parallel to plane  or pointing away then it won't ever cross
		double d = -n.dot(direction);//normalized distance along direction to plane. This is cos of angle between direction and normal vector
		double s = dop/d;
		if(s <= 0 || Double.isInfinite(s) || Double.isNaN(s)){
			//System.out.println("TriangularPrism.getTriangleIntersection discarded because it's pointing away from prism."+origin);
			return null;
		
		}
		Vector3D i = direction.scale(s).add(origin);//this should be the intersection
		
		boolean inside = true;
		//now see if it's within the specifed points
		for(int j = 0; j < p.length; ++j){
			int k = (j+1)%p.length;
			double dot = p[j].subtract(p[k]).dot(i.subtract(p[k]));
			if(dot < 0){
				inside = false;
				break;
			}
		}
		if(inside){
			return new PhotonInteraction(ph, this, p,  i, n, fromInside, "Triangular prism interaction");
		} else {
			return null;
		}
		
	}


}
