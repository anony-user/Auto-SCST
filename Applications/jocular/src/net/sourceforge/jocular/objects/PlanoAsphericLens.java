package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.splines.RotatedSpline;

public class PlanoAsphericLens extends RotatedSpline {
	private EquationProperty m_radius = new EquationProperty("25mm", this, PropertyKey.RADIUS);
	private EquationProperty m_diameter = new EquationProperty("25mm", this, PropertyKey.DIAMETER);
	private EquationProperty m_conicCoefficient = new EquationProperty("0.25", this, PropertyKey.CONIC_COEFF);
	private EquationArrayProperty m_polyCoefficents = new EquationArrayProperty("0,0", this, PropertyKey.POLY_COEFFS);
	private EquationProperty m_thickness = new EquationProperty("10mm", this, PropertyKey.THICKNESS);
	public enum Units {MM, M, INCHES};
	private EnumProperty m_units = new EnumProperty(Units.MM, Units.MM.name());
	private static final int SPLINE_POINT_COUNT = 20;
	
	public PlanoAsphericLens() {
		setProperty(PropertyKey.SIMPLIFY_THRESHOLD,"100e-12");
	}
	
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.THICKNESS, PropertyKey.DIAMETER, PropertyKey.RADIUS, PropertyKey.COEFF_UNITS, PropertyKey.CONIC_COEFF, PropertyKey.POLY_COEFFS, PropertyKey.INSIDE_MATERIAL));
		return result;
	}
	@Override
	public void setProperty(PropertyKey key, String s) {
		
		switch(key){
		case RADIUS:
			m_radius = new EquationProperty(s, this, key);
			doInternalCalcs();
			firePropertyUpdated(key);
			break;
		case DIAMETER:
			m_diameter = new EquationProperty(s, this, key);
			doInternalCalcs();
			firePropertyUpdated(key);
			break;
		case COEFF_UNITS:
			m_units = new EnumProperty(Units.MM, s);
			doInternalCalcs();
			firePropertyUpdated(key);
			break;
		case CONIC_COEFF:
			m_conicCoefficient = new EquationProperty(s, this, key);
			doInternalCalcs();
			firePropertyUpdated(key);
			break;
		case POLY_COEFFS:
			m_polyCoefficents = new EquationArrayProperty(s,  this,  key);
			doInternalCalcs();
			firePropertyUpdated(key);
			break;
		case THICKNESS:
			m_thickness = new EquationProperty(s, this, key);
			doInternalCalcs();
			firePropertyUpdated(key);
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
		case COEFF_UNITS:
			result = m_units;
			break;
		case CONIC_COEFF:
			result = m_conicCoefficient;
			break;
		case POLY_COEFFS:
			result = m_polyCoefficents;
			break;
		case DIAMETER:
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
	public OpticsObject makeCopy() {
		PlanoAsphericLens result = new PlanoAsphericLens();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	@Override
	public int getSplinePointCount() {
		return SPLINE_POINT_COUNT + 2;
	}
	@Override
	public double[] getSplinePointDepValues() {
		double rad = m_diameter.getValue().getBaseUnitValue()/2;
		double[] result = new double[getSplinePointCount()];
		for(int i = 0; i < SPLINE_POINT_COUNT; ++i){
			result[i] = ((double)i)*rad/(SPLINE_POINT_COUNT - 1);
		}
		result[SPLINE_POINT_COUNT] = rad;
		result[SPLINE_POINT_COUNT + 1] = 0;
		return result;
	}
	@Override
	public double[] getSplinePointIndepValues() {
		double thick = m_thickness.getValue().getBaseUnitValue();
		double rad = m_radius.getValue().getBaseUnitValue();
		double dia = m_diameter.getValue().getBaseUnitValue();
		double conic = m_conicCoefficient.getValue().getBaseUnitValue();
		Units u = (Units)m_units.getValue();
		double scale = 1.0;
		switch(u){
		case MM:
			scale = 1e3;
			break;
		default:
		case M:
			scale = 1;
			break;
		case INCHES:
			scale = 1.0/25.4e-3;
			break;
		}
		UnitedValue[] vas = m_polyCoefficents.getValue();
		double[] as = new double[vas.length];
		for(int i = 0; i < vas.length; ++i){
			as[i] = vas[i].getBaseUnitValue();
		}
		double[] result = new double[getSplinePointCount()];
		double[] rads = getSplinePointDepValues();
		result[0] = computeZOfR(0, rad, conic, as);
		
		for(int i = 1; i < SPLINE_POINT_COUNT; ++i){
			result[i] = computeZOfR(rads[i]*scale, rad*scale, conic, as)/scale;
			result[i] = -thick/2.0 + result[i] - result[0];
			if(Double.isNaN(result[i])){
				throw new RuntimeException("Spline value is NaN.");
			}
		}
		result[0] = -thick/2.0;
		result[SPLINE_POINT_COUNT] = thick/2.0;
		result[SPLINE_POINT_COUNT + 1] = thick/2.0;
		return result;
	}
	@Override
	public PointType[] getSplinePointTypes() {
		PointType[] result = new PointType[getSplinePointCount()];
		for(int i = 0; i < SPLINE_POINT_COUNT; ++i){
			result[i] = PointType.SMOOTH;
		}
		result[0] = PointType.SQUARE;
		result[SPLINE_POINT_COUNT - 1] = PointType.CUSP;
		result[SPLINE_POINT_COUNT] = PointType.CUSP;
		result[SPLINE_POINT_COUNT + 1] = PointType.CUSP;
		return result;
	}
	/**
	 * computes the z value for this lens in metres with the radius and r in metres too
	 * @param r
	 * @param rad
	 * @param conic
	 * @param as
	 * @return
	 */
	private double computeZOfR(double r, double rad, double conic, double[] as){
		double result = 0;
		double r2 = r*r;
		double sqr = 1 - (1 + conic)*r2/rad/rad;
		if(sqr >= 0){
			result = r2/rad/(1 + Math.sqrt(sqr));
			double rn = r2;
			for(int i = 0; i < as.length; ++i){
				result += rn*as[i];
				rn *= r2;
			}
		} else {
			result = 0;
		}
		return result;
	}

	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);
	}
	
	
	

}
