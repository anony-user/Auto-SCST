package net.sourceforge.jocular.math;

import java.util.ArrayList;
import java.util.List;

/**
 * uses Sturm's theorem to home in on the solution to a polynomial
 * Any coefficients that are zero must be exactly equal to zero. A coefficient that is 0+epsilon will be treated as a real coefficient with a value of epsilon
 * If epsilon is actually an error then these coefficients should be first zeroed. See the {@link #roundToZero(double) roundToZero} method.
 * @author kenneth
 *
 */
public class SturmSolver {
	private final Polynomial m_poly;
	private final Polynomial m_deriv;
	private final Polynomial m_2ndDeriv;
	private final List<Polynomial> m_polys;
	private final List<RangeResult> m_roots = new ArrayList<RangeResult>();
	public SturmSolver(Polynomial p){
		double g = p.get(p.order());
		m_poly = p.multiplyBy(1.0/g).removeInsignificantCoefficients();
		m_deriv = m_poly.derivative();
		m_2ndDeriv = m_deriv.derivative();
		m_polys = makePolyList();
	}
	/**
	 * finds a solution between the min and max value
	 * @param min
	 * @param max
	 * @return either the solution or NaN.
	 */
	public boolean solve(double min, double max){
		m_roots.clear();
		RangeResult rr = new RangeResult(new PointResult(min), new PointResult(max));
		solveStep(rr);
		return m_roots.size() > 0;
	}
	private static int maxId = 0;
	private class PointResult {
		final double x;
		final double y;
		final double dy;
		final double ddy;
		final double x0;
		final boolean root;
		
		final int id;
		//final boolean zero;
		final int num;
		PointResult(double x){
			
			this.x = x;
			this.y = m_poly.evaluate(x);
			this.dy = m_deriv.evaluate(x);
			this.ddy = m_2ndDeriv.evaluate(x);
			this.num = signChangeCountAroundVal(x);
			this.id = maxId;
			this.root = calcIsRoot();
			++maxId;
			this.x0 = bestFitRoot();
		}
		boolean isRoot(){
			return root;
		}
		boolean calcIsRoot(){
			boolean result = (Math.abs(y) <= Polynomial.EPSILON*Math.abs(dy));
			return result;
		}
		public String toString(){
			return "PointResult "+x + (root? " root" : "");
		}
		public double bestFitRoot(){
			double result;
			double a = ddy/2.0;
			double b = dy - ddy*x;
			double c = y - (a*x+b)*x;
			
			if(ddy != 0){
				double dyOverddy = dy/ddy;
				double sqrt = dyOverddy*dyOverddy - 2.0*y/ddy;
				if(sqrt >= 0){
					sqrt = Math.sqrt(sqrt);
					double r1 = -dyOverddy + x - sqrt;
					double r2 = -dyOverddy + x + sqrt;
					//pick the closest root to x
					double r = (Math.abs(x - r1) < Math.abs(x - r2))? r1 : r2;
					result = r;
				} else {
					result = x;
				}
			} else {
				result = -c/b;
			}
			return result;
		}
	}
	private class RangeResult {
		PointResult pMin;
		PointResult pMax;
		PointResult pBest;
		RangeResult(PointResult min, PointResult max){
			pMin = min;
			pMax = max;
			pBest = getBest();
			
		}
		
		PointResult getBest(){
			
			PointResult result;
			if(Math.abs(pMin.y) < Math.abs(pMax.y)){
				result = pMin;
			} else {
				result = pMax;
			}
			if(pBest != null){
				if(Math.abs(pBest.y) < Math.abs(result.y)){
					result = pBest;
				}
			}
			return result;
		}
		
		public void setMin(RangeResult rr){
			pMin = rr.pMin;
			pBest = getBest();
		}
		public void setMax(RangeResult rr){
			pMax = rr.pMax;
			pBest = getBest();
		}
		double getMid(){
			return (pMin.x + pMax.x)/2.0;
		}
		public boolean equals(RangeResult rr){
			boolean result = rr.pMax == pMax;
			result |= rr.pMin == pMax;
			result |= rr.pMax == pMin;
			result |= rr.pMin == pMin;
			return result;
			
		}
		public boolean isEssentiallyEqual(RangeResult rr){
			boolean result = equals(rr);
			if(!result){
				double rrmin = rr.pMin.x;
				double rrmax = rr.pMax.x;
				double min = pMin.x;
				double max = pMax.x;
				
				double e1 = Math.abs(rrmin - min);
				double e2 = Math.abs(rrmin - max);
				double e3 = Math.abs(rrmax - min);
				double e4 = Math.abs(rrmax - max);
				double e = Math.min(e1, Math.min(e2, Math.min(e3, e4)));
				result |= e < Polynomial.EPSILON;
			}
			return result;
		}
//		double getUpperMid(){
//			return (min * 0.4 + max * 0.6);
//		}
		boolean isRootsInRange() {
			return (pMin.num - pMax.num) > 0;
		}
		boolean containsRoot(){
			boolean result = pMin.isRoot();
			result |= pMax.isRoot();
			result |= isRootsInRange();
			return result;
		}
		
		boolean isAtRoot() {
			boolean result;
			double e = Math.abs(pMax.x - pMin.x);
			result = e <= Polynomial.EPSILON;
			if(!result){
				if(pMax.isRoot() && pMin.isRoot()){
					double e2 = Math.abs(pMin.x0 - pMax.x0);
					if(e2 < Polynomial.EPSILON*100000.0){
						if(e < Polynomial.EPSILON*100000.0){
							result = true;
						}
					}
				}
			}
			
			return result;
		}
		public String toString(){
			return "RangeResult "+pMin.x+" : "+pMax.x+(containsRoot()?"*":"");
		}
		public double getRoot(){
			double result;
			
			result = pBest.x;
			
			return result;
		}
	}
	private void solveStep(RangeResult rr){
		
//		if(rr.pMin.isRoot() && rr.pMax.isRoot()){
//			System.out.println("SturmSolver.solveStep both ponts of range are roots. "+rr);
//		}
		
		if(rr == null){
		} else if(rr.isAtRoot()){
			
			addRoot(rr);
		} else if(!rr.containsRoot()){
		} else {
		
		
			PointResult mid = new PointResult(rr.getMid());
//			if(rr.pMin.isRoot() && rr.pMax.isRoot()){
//				System.out.println("SturmSolver.solveStep both roots are still the same");
//			}
			RangeResult rrMax = new RangeResult(mid, rr.pMax);
			rr.pMax = mid;
			
			
			
			solveStep(rr);
			solveStep(rrMax);
			
		}
		
	}
	/**
	 * returns the number of real roots within the interval [min, max]
	 * uses Sturm's Theorem
 
	 * @param min
	 * @param max
	 * @return
	 */
	private int numRoots(double min, double max){
		return signChangeCountAroundVal(min) - signChangeCountAroundVal(max);
	}
	private synchronized void addRoot(RangeResult r) {
		boolean found = false;
		
		
		for(RangeResult rr : m_roots){
			if(rr.pMin == r.pMax){
				rr.setMin(r);
				found = true;
			} else if(rr.pMax == r.pMin){
				rr.setMax(r);
				found = true;
				break;
			} else if(Math.abs(rr.getRoot() - r.getRoot()) < Polynomial.EPSILON){//rr.equals(r)){
				found = true;
				if(rr.pMax.x < r.pMin.x){
					rr.setMax(r);
				} else if(rr.pMin.x > r.pMax.x){
					rr.setMin(r);
				}
			}
		}
		if(!found){
			m_roots.add(r);
		}
		
		
	}
	public double[] getRoots(){
		double[] result = new double[m_roots.size()];
		for(int i = 0; i < m_roots.size(); ++i){
			result[i] = m_roots.get(i).pMin.x;
		}
		return result;
	}
	public List<Polynomial> getRealRoots(){
		List<Polynomial> result = new ArrayList<Polynomial>();
		for(RangeResult d : m_roots){
			result.add(Polynomial.makeFromRealRoots(new double[] {d.getRoot()}));
		}
		return result;
	}
	public static void main (String args[]){
		//Polynomial p1 = Polynomial.makeFromRealRoots(new double[] {0.9,0.8,-0.5,-4}).multiplyBy(Polynomial.makeFromComplexPair(2, 3));
		Polynomial p1 = Polynomial.makeFromRealRoots(new double[] {0,0.5, 0.5, 1.0});//.multiplyBy(Polynomial.makeFromComplexPair(.5, .1));
		//Polynomial p1 = Polynomial.makeFromCoefficients(new double[] {0, 0.2});
		//p1 = Polynomial.parseString("0.00003069*x^5 + 0.00005434*x^4 + 0.00002564*x^3 + -0.00006318*x^2 + -0.00004614*x + -0.00000135");
		//p1 = Polynomial.parseString("510.7227891E-9*x^5 + 178.0612245E-9*x^4 + 35.84345805E-6*x^3 + -19.76857143E-6*x^2 + -76.31748016E-6*x + -103.5833333E-6");
		p1 = Polynomial.parseString("-5.42882554047509E-6*x^6 + -17.8558722976245E-6*x^5 + 148.319840372031E-6*x^4 + 1.65332150903931E-6*x^3 + -445.133303772598E-6*x^2 + 0E0*x + 191.356656138809E-9");
	
		//[x=−0.020734387146505,x=0.020735977106041,x=−1.657480556286023,x=2.602925441556307,x=2.728903790474054,x=−6.963435937088681]
//		p1 = p1.dividedBy(Polynomial.parseString("1*x+1.657480556286023"))[0];
//		p1 = p1.dividedBy(Polynomial.parseString("1*x+0.020734387146505"))[0];
//		p1 = p1.dividedBy(Polynomial.parseString("1*x-0.020735977106041"))[0];
//		p1 = p1.dividedBy(Polynomial.parseString("1*x+6.963435937088681"))[0];
//		p1 = p1.dividedBy(Polynomial.parseString("1*x-2.728903790474054"))[0];
//		p1 = p1.dividedBy(Polynomial.parseString("1*x-2.602925441556307"))[0];

		p1 = Polynomial.parseString("2.29306845777999E-3*x^6 + -8.77347931672343E-3*x^5 + 8.3920236942572E-3*x^4 + 429.88884886378E-6*x^3 + -922.396058695927E-6*x^2 + 0E0*x + 20.1481580009546E-6");
		////[x=0.17999588317834,x=−0.16791228742781,x=−0.26114381820723,x=0.31635989894961,x=1.764595866739104,x=1.994191413289727]
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {0.17999588317834}))[0];
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {-0.16791228742781}))[0];
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {-0.26114381820723}))[0];
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {0.31635989894961}))[0];
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {1.764595866739104}))[0];
		p1 = p1.dividedBy(Polynomial.makeFromRealRoots(new double[] {1.994191413289727}))[0];
		List<Polynomial> rs = factor(p1, 0.0, 1.0);
		Polynomial r = null;
		if(rs.size() > 0){
			r = rs.get(0);
		}
		System.out.println("SturmSolver.main found "+rs.size() +" roots: "+r);
		
		
		
	}
	public static List<Polynomial> factor(Polynomial p, double min, double max){
		List<Polynomial> result;
		if(p.order() <= 3){
			List<Polynomial> ps = p.cubicRealRoots();
			result = new ArrayList<Polynomial>();
			for(Polynomial tp : ps){
				if(tp.order() == 1){
					double r = tp.linearRoot();
					if(r >= min && r <= max){
						result.add(tp);
					}
				}
				
			}
		} else {
			SturmSolver ss = new SturmSolver(p);
			int startMaxId = maxId;
			ss.solve(min, max);
			result = ss.getRealRoots();
			if((maxId - startMaxId) > 500){
				System.out.println("SturmSolver.factor "+maxId+" points calc'ed");
				System.out.println("SturmSolver.factor "+p);
				System.out.println("SturmSolver.factor "+result);
			}
//			String s = "";
//			for(RangeResult r : ss.m_roots){
//				s += r.getRoot() + " ";
//			}
//			System.out.println("SturmSolver.factor roots: "+s);
		}
		return result;
	}


	/**
	 * Count the sign changes in the polynomial list at the specified point
	 * @param v
	 * @return
	 */
	private int signChangeCountAroundVal(double v){
		int signChangeCount = 0;
		double lastSign = 0;
		for(Polynomial p : m_polys){
			double pMin = p.evaluate(v);
			
			if(pMin > Polynomial.EPSILON){
				if(lastSign < 0){
					++signChangeCount;
				}
				lastSign = 1;
			} else if(pMin < -Polynomial.EPSILON){
				if(lastSign > 0){
					++signChangeCount;
				}
				lastSign = -1;
			}
		}
		return signChangeCount;
	}
	/**
	 * Determines if the specified value is a zero of the polynomial or not
	 * @param v
	 * @return true if the specified value is a zero of the polynomial
	 */
	boolean isZero(double x){
		boolean result = false;
		double v = m_poly.evaluate(x);
		double d = m_deriv.evaluate(x);
		
		
		if(Math.abs(v) < Polynomial.EPSILON*Math.abs(d)){
			result = true;
		}
		return result;
	}
	private List<Polynomial> makePolyList(){
		//now construct the polynomial sequence required by Sturm's Theorem
		List<Polynomial> ps = new ArrayList<Polynomial>();
		boolean keepGoing = true;
		int i = 0;
		//Polynomial lastTerm;
		while(keepGoing){
			switch(i){
			case 0:
				ps.add(i, m_poly);
				break;
			case 1:
				ps.add(i, m_deriv);
				break;
			default:
				//set last term to the remainder of the division of the last two elements
				ps.add(i, ps.get(i - 2).dividedBy(ps.get(i - 1))[1].multiplyBy(-1.0));
				//ps.add(i, ps.get(i - 2).dividedBy(ps.get(i - 1))[1]);
				if(ps.get(i) == Polynomial.ZEROTH){
					keepGoing = false;
				}
			}
			
			++i;
			if(i > m_poly.order()){//stop this just in case it goes too far
				keepGoing = false;
			}
		}
		return ps;
	}
	
	
}
