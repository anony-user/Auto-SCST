package net.sourceforge.jocular.splines;

import java.util.List;

import net.sourceforge.jocular.math.Polynomial;
import net.sourceforge.jocular.math.SturmSolver;

/**
 * A handy class to store the coefficients for a cubic spline of the form
 * X(s) = ax*s^3 + bx*s^2 + cx*s + dx
 * Y(s) = ay*s^3 + by*s^2 + cy*s + dy
 * @author kmaccallum
 *
 */
public class SplineCoefficients {
	public final Polynomial xSpline;
	public final Polynomial ySpline;

	public SplineCoefficients(double ax, double bx, double cx, double dx, double ay, double by, double cy, double dy){

		xSpline = Polynomial.makeFromCoefficients(new double[] {dx, cx, bx, ax});
		ySpline = Polynomial.makeFromCoefficients(new double[] {dy, cy, by, ay});
		
		
	}
	
	public double calcX(double s){
		if(s < -Polynomial.EPSILON || s > 1+Polynomial.EPSILON){
			throw new RuntimeException("Value of s must be between 0 and 1, not "+s);
		}

		return xSpline.evaluate(s);
	}
	public double calcY(double s){
		if(s < -Polynomial.EPSILON || s > 1+Polynomial.EPSILON){
			throw new RuntimeException("Value of s must be between 0 and 1, not "+s);
		}

		return ySpline.evaluate(s);
	}
	public boolean isStraight(){
		return (xSpline.order() < 2 && ySpline.order() < 2);
	}
	/**
	 * computes the best parameter to achieve the specified X,Y coordinate.
	 * @param x
	 * @param y
	 * @return
	 */
	public double bestFitParameter(double x, double y){
		double result = 0;
		Polynomial px = xSpline.subtract(Polynomial.makeFromCoefficients(new double[] {x}));
		Polynomial py = ySpline.subtract(Polynomial.makeFromCoefficients(new double[] {y}));
		Polynomial polyToMin = px.multiplyBy(px).add(py.multiplyBy(py)).derivative();
		//List<Polynomial> sols = polyToMin.factor();
		
		List<Polynomial> sols = SturmSolver.factor(polyToMin, 0.0, 1.0);
		sols.add(Polynomial.makeFromRealRoots(new double[] {0.0}));
		sols.add(Polynomial.makeFromRealRoots(new double[] {1.0}));
		double bestErrSq = Double.MAX_VALUE;
		
		for(int i = 0; i <  sols.size(); ++i){
			
			Polynomial r = sols.get(i);
			if(r.order() == 1){
				double s = r.linearRoot();
				//if(s >= 0.0 - Polynomial.EPSILON && s <= 1.0 + Polynomial.EPSILON){
					double ex = px.evaluate(s);
					double ey = py.evaluate(s);
					double errSq = ey*ey + ex*ex;
					if(errSq < bestErrSq){
						result = s;
						bestErrSq = errSq;
					}
				//}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "SplineCoefficients x: "+xSpline+"; y: "+ySpline;
	}
	
	
}

