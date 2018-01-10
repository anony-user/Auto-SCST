package net.sourceforge.jocular.math;
/**
 * Implements Cauchy's imperical approximation for refractive index
 * @author kmaccallum
 *
 */
public class CauchyEquation implements FunctionOfX {

	private final double m_b;
	private final double m_c1;
	private final double m_c2;
	
	public CauchyEquation(double b, double c1, double c2){
		m_b = b;
		m_c1 = c1;
		m_c2 = c2;
		
	}
	@Override
	public double getValue(double x) {
		
		double xSquared = 1/(x*x);
		double result = m_c2*xSquared;
		result += m_c1;
		result *= xSquared;
		result += m_b;
		return result;
		
	}

}
