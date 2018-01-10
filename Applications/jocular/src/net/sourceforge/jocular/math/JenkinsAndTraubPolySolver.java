package net.sourceforge.jocular.math;

import java.util.ArrayList;
import java.util.List;


public class JenkinsAndTraubPolySolver {
	private enum Stage {NO_SHIFT, FIXED_SHIFT, CHOOSE_SIGMA, LINEAR_VARIABLE_SHIFT, QUADRATIC_VARIABLE_SHIFT}
	//private static final double REAL_RATIO_LIMIT = 1e-12;
	private static final int MAX_TRIES = 40000;
	private int m_iterations = 0;
	public Polynomial factorOnce(Polynomial poly){
		//first make polynomial k
		Polynomial k = poly.derivative();//.dividedBy(poly.order())[0];
		Polynomial dydx = k;
		Polynomial z = Polynomial.makeFromCoefficients(new double[] {0.0, 1.0});
		Stage stage = Stage.NO_SHIFT;
		boolean notDone = true;
		int iter = 0;
		
		Complex s1 = null;
		Complex s2 = null;

		double tOld = Double.MAX_VALUE;
		
		
		double vOld = Double.MAX_VALUE;
		double sOld = Double.MAX_VALUE;
		boolean doQuad = true;
		double s = Double.NaN;
		Polynomial sigma = null;
		Polynomial noShiftK = null;
		double ps = Double.MAX_VALUE;
		
		Polynomial result = null;
		m_iterations = 0;
		final double epsilon = Math.abs(Polynomial.EPSILON*poly.maxCoeff());
		while(notDone && m_iterations < MAX_TRIES){
			++iter;
			//++ m_iterations;
			switch(stage){
			default:
			case NO_SHIFT:
				k = k.subtract(poly.multiplyBy(k.evaluate(0.0)/poly.evaluate(0.0))).dividedBy(z)[0];
				//double p0 = poly.evaluate(0);
				//double k0 = k.evaluate(0);
				//k = k.multiplyBy(p0/k0).subtract(poly).dividedBy(Polynomial.makeFromRealRoots(new double[] {s}))[0];
				if(iter > 5){
					stage = Stage.CHOOSE_SIGMA;
					
					noShiftK = k;
				}
				break;
			case CHOOSE_SIGMA:
				//TODO how to choose s1
				double a = Math.random()*2*Math.PI;
				double r = Math.random();
				//double r = calcBeta(poly);//Math.random();
				s1 = new Complex(r*Math.cos(a),r*Math.sin(a));
				s2 = s1.conjugate();
				sigma = Polynomial.makeFromComplex(s1);
				k = noShiftK;
				stage = Stage.FIXED_SHIFT;
				iter = 0;
				++m_iterations;
				break;
			case FIXED_SHIFT:
				k = makeNewK23(poly,k, sigma, s1, s2);
				//Polynomial kBar = k.dividedBy(k.get(k.order()))[0];
				//s = s1.subtract(poly.evaluate(s1).divide(kBar.evaluate(s1))).real();
				s = s1.subtract(poly.evaluate(s1).divide(k.evaluate(s1))).real();
				sigma = calcNewQuadSigma(poly, k, s1, s2);
				double thresh;
				if(m_iterations < 5){
					thresh = 0.5;
				} else if(m_iterations < 10){
					thresh = 0.5;
				} else {
					thresh = 0.5;
				}
//				if(Math.abs((tOld - s)/s) <= thresh){
//					stage = Stage.LINEAR_VARIABLE_SHIFT;
//					doQuad = false;
//					iter = 0;
//					break;
//				}
				tOld = s;
				
				double v = sigma.get(0);
				if(Math.abs((vOld - v)/v) <= thresh){
					stage = Stage.QUADRATIC_VARIABLE_SHIFT;
					doQuad = true;
					iter = 0;
					break;
				}
				vOld = v;
				
				if(iter > 10 || sigma.isNan()){
					stage = Stage.CHOOSE_SIGMA;
				}
				break;
			case QUADRATIC_VARIABLE_SHIFT:
				Complex[] cs = sigma.quadraticRoot();
				s1 = cs[0];
				s2 = cs[1];
				if(s1.isReal()){
					iter = 0;
					stage = Stage.LINEAR_VARIABLE_SHIFT;
					//if(s1.abs() < s2.abs()){
						//s = s1.subtract(poly.evaluate(s1).divide(k.evaluate(s1))).real();
					s = s1.real();
					//} else {
					//	s = s2.subtract(poly.evaluate(s1).divide(k.evaluate(s1))).real();
					//}
					break;
				}
				ps = Math.max(poly.evaluate(s1).divide(dydx.evaluate(s1)).abs(), poly.evaluate(s2).divide(dydx.evaluate(s2)).abs());
				if(ps < epsilon){
					notDone = false;
					result = sigma;
					break;
				}
				
				k = makeNewK23(poly, k, sigma, s1, s2);
				sigma = calcNewQuadSigma(poly, k, s1, s2);
				if(sigma.isNan()){
					stage = Stage.CHOOSE_SIGMA;
				}
				
				if(iter > 10){
					//notDone = false;
					stage = Stage.CHOOSE_SIGMA;
				}
				
				break;
			case LINEAR_VARIABLE_SHIFT:
				//compute s (it's real)
				//Polynomial kBar = k.dividedBy(k.get(k.order()))[0];

				double kc = k.get(k.order());
				//compute next k
				ps = poly.evaluate(s);
				double ks = k.evaluate(s);
				if(Math.abs(ps/dydx.evaluate(s)) < epsilon){
					notDone = false;
					result = Polynomial.makeFromRealRoots(new double[] {s});
					
				} else {
					//k = k.subtract(poly.multiplyBy(ks/ps)).dividedBy(Polynomial.makeFromRealRoots(new double[] {s}))[0];
					k = k.multiplyBy(ps/ks).subtract(poly).dividedBy(Polynomial.makeFromRealRoots(new double[] {s}))[0];
					//now compute the next s
					s -= ps/ks*kc;
	//				if(Math.abs((sOld - s)/s) > 0.5 && iter){
	//					stage = Stage.CHOOSE_SIGMA;
	//				}
					sOld = s;
				
					if(iter > 10){
						//notDone = false;
						stage = Stage.CHOOSE_SIGMA;
					}
				}
				
				break;
			}
		}
		
		if(m_iterations >= MAX_TRIES){
			throw new RuntimeException("Failed to solve in "+ m_iterations + " iterations. "+poly.toArrayString());
		}

		return result;
	}
	private double calcBeta(Polynomial p){
		double beta = 0;
		double[] cs = new double[p.order()+1];
		for(int i = 0; i < cs.length; ++i){
			cs[i] = Math.abs(p.get(i));
			if(i == 0){
				cs[i] = -cs[i];
			}
		}
		Polynomial p1 = Polynomial.makeFromCoefficients(cs);
		Polynomial p2 = p1.newtonSolve(0.0);
		if(p2.order() != 1){
			throw new RuntimeException("Order should be 1.");
		}
		beta = p2.linearRoot();
		return beta;
		
	}
	private Polynomial makeNewK23(Polynomial poly, Polynomial oldK, Polynomial sigma, Complex s1, Complex s2){
		Polynomial k = oldK;

		Complex ks1 = k.evaluate(s1);
		Complex ks2 = k.evaluate(s2);
		Complex ps1 = poly.evaluate(s1);
		Complex ps2 = poly.evaluate(s2);
		
		Complex a = ps1.multiply(ks2).subtract(ps2.multiply(ks1));
		Complex b = ks1.multiply(s2).multiply(ps2).subtract(ks2.multiply(s1).multiply(ps1));
		Complex c = s1.multiply(ps1).multiply(ps2).subtract(ps1.multiply(s2).multiply(ps2));
//		Complex k1 = a.divide(c);
//		Complex k0 = b.divide(c);
//		//in theory, a,b and c should all be real
//		if(Math.abs(k1.imag()/k1.real())  > REAL_RATIO_LIMIT || Math.abs(k0.imag()/k0.real()) > REAL_RATIO_LIMIT ){
//			//throw new RuntimeException("k1 & k0 are not all real. Args are: "+k1.arg()+", "+k0.arg());
//		}
//		Polynomial newK = Polynomial.makeFromCoefficients(new double[] {k0.real(), k1.real()});
//		newK = newK.multiplyBy(poly).add(k);
		Complex k0 = c.divide(a);
		Complex k1 = b.divide(a);
		Polynomial newK = k.multiplyBy(k0.real());
		newK = newK.add(Polynomial.makeFromCoefficients(new double[] {k1.real(), 1.0}).multiplyBy(poly));
		Polynomial[] nks = newK.dividedBy(sigma);
		//TODO check that remainder is zero I think
		if(nks[1].evaluate(1.0) > 1e-10){
			//throw new RuntimeException("Remainder is not zero.");
		}
		newK = nks[0];
		//newK = newK.dividedBy(newK.get(newK.order()))[0];
		return newK;
	}
	private Polynomial calcNewQuadSigma(Polynomial poly, Polynomial k, Complex s1, Complex s2){
		
		double p0 = poly.evaluate(0);
	
		Polynomial z = Polynomial.makeFromCoefficients(new double[] {0,1});
		//Polynomial z2 = Polynomial.makeFromCoefficients(new double[] {0,0,1});
		Polynomial k0 = k;
		Polynomial k1 = k0.subtract(poly.multiplyBy(k0.evaluate(0.0)/p0)).dividedBy(z)[0];
		Polynomial k2 = k1.subtract(poly.multiplyBy(k1.evaluate(0.0)/p0)).dividedBy(z)[0];
		Complex k0s1 = k0.evaluate(s1);
		Complex k1s1 = k1.evaluate(s1);
		Complex k2s1 = k2.evaluate(s1);
		Complex k0s2 = k0.evaluate(s2);
		Complex k1s2 = k1.evaluate(s2);
		Complex k2s2 = k2.evaluate(s2);
		
		
		Complex unitCoeff = k0s1.multiply(k1s2).subtract(k0s2.multiply(k1s1));
		Complex zCoeff = k0s2.multiply(k2s1).subtract(k0s1.multiply(k2s2));
		Complex z2Coeff = k1s1.multiply(k2s2).subtract(k2s1.multiply(k1s2));
		Complex den = k1s1.multiply(k2s2).subtract(k2s1.multiply(k1s2));
		double rUnit = unitCoeff.divide(den).real();
		double rZ = zCoeff.divide(den).real();
		double rZ2 = z2Coeff.divide(den).real();
		
		Polynomial newSigma = Polynomial.makeFromCoefficients(new double[] {rUnit, rZ, rZ2});
		
		return newSigma;
	}
	private class notNullList extends ArrayList<Polynomial> {
		public boolean add(Polynomial p){
			if(p == null){
				throw new RuntimeException("Element to add is null.");
			}
			return super.add(p);
		}
	}
	public List<Polynomial> factor(Polynomial poly){
		final double epsilon = Math.abs(Polynomial.EPSILON*poly.maxCoeff());
		List<Polynomial> result = new notNullList();
		Polynomial p1 = poly;
		double f = p1.get(p1.order());
		
		if(f != 1.0){
			p1 = p1.dividedBy(f)[0];
		}
		
		Polynomial dydx = p1.derivative();
		Polynomial[] ps;
		while(p1.order() > 3){
			
			Polynomial p2 = factorOnce(p1);
			
			double e = Double.NaN;
			if(p2.order() == 2){
				List<Polynomial> rs = p2.quadraticRealRoots();
				for(Polynomial r : rs){
					Polynomial r1 = refineRoot(poly, dydx, r);
					result.add(r1);
					ps = p1.dividedBy(r1);
					p1 = ps[0];
				}
			} else if(p2.order() == 1){
				Polynomial r1 = refineRoot(poly, dydx, p2);
				result.add(r1);
				ps = p1.dividedBy(r1);
				p1 = ps[0];
				
			} else {
				throw new RuntimeException("Order is greater than 2. "+p2.order());
			}
			
			//put test in here
			
			
//			if(minErr > epsilon){
//				
//				throw new RuntimeException("Remainder is "+ps[1]+"("+e+")"+", divisor is "+p2+", poly is "+p1.toArrayString());
//			}
			
		}
		List<Polynomial> rs = p1.cubicRealRoots();
		for(Polynomial r : rs){
			
			Polynomial r1 = refineRoot(poly, dydx, r);
			result.add(r1);
			
			
		}
		
		if(f != 1.0){
			result.add(Polynomial.makeFromCoefficients(new double[] {f}));
		}
		
		return result;
	}
	private Polynomial refineRoot(Polynomial p, Polynomial dydx, Polynomial root){
		Polynomial result;
		double ex = Double.MAX_VALUE;
		double minErr;
		
		if(root.order() == 1){
			result = root;
			double r = root.linearRoot();
			double e = Math.abs(p.evaluate(r)/dydx.evaluate(r));
			minErr = e;
			if(minErr > Polynomial.EPSILON){
				Complex cr = p.laguerreSolve(r, 5);
				ex = p.evaluate(cr).divide(dydx.evaluate(cr)).abs();
				
				if(ex < e){
					minErr = ex;
					r = cr.real();
					result = Polynomial.makeFromRealRoots(new double[] {r});
				}
			}
			
		} else if(root.order() == 2){
			Complex[] cs = root.quadraticRoot();
			double ecs0 = p.evaluate(cs[0]).divide(dydx.evaluate(cs[0])).abs();
			double ecs1 = p.evaluate(cs[1]).divide(dydx.evaluate(cs[1])).abs();
			
			
			Complex xcs0 = p.laguerreSolve(cs[0], 5);
			Complex xcs1 = p.laguerreSolve(cs[1], 5);
			double excs0 = p.evaluate(xcs0).divide(dydx.evaluate(xcs0)).abs();
			double excs1 = p.evaluate(xcs1).divide(dydx.evaluate(xcs1)).abs();
			
			if(excs0 < ecs0){
				cs[0] = xcs0;
			}
			if(excs1 < ecs1){
				cs[1] = xcs1;
			}
			
			result = Polynomial.makeFromComplexPair(cs[0], cs[1], true);
			
		} else if(root.order() == 0){
			result = root;
		} else {
			throw new RuntimeException("The order of this root is "+root.order());
		}
		return result;
	}
	static final int NUM_CYCLES = 1000000;
	public static void main (String args[]){
		JenkinsAndTraubPolySolver s = new JenkinsAndTraubPolySolver();
		//Polynomial p1 = Polynomial.makeFromRealRoots(new double[] {-0.9,-0.8,-0.5,-4}).multiplyBy(Polynomial.makeFromComplexPair(2, 3));
		//Polynomial p1 = Polynomial.makeFromCoefficients(new double[] {2.01, -8.035, 12.545, -5.505, -8.545, 12.54, -6.01, 1.0});
		//Polynomial p1 = Polynomial.makeFromCoefficients(new double[] {-0.4838459292677278, 1.2402104542523378, 0.26712048483322204, -0.8221843310812812, 0.9436488539486415, 1.0});
		//Polynomial p1 = Polynomial.makeFromCoefficients(new double[] {-954.0662998207641, 740.0628694340326, -0.0016376335605361447});
		Polynomial p1 = Polynomial.makeFromCoefficients(new double[] {-0.5274952329594953, -568.2009600579195, 18.883333836865233, -60.12587795905908, 0.9999999999999999});
		int badCount = 0;
		double worst = Double.MIN_NORMAL;
		Polynomial worstP = null;
		double worstRatio = Double.MIN_NORMAL;
		double worstEpsilon = Double.MIN_VALUE;
		double worstDx = Double.MIN_NORMAL;
		long startTime = System.currentTimeMillis();
		boolean firstPoly = true;
		for(int i = 0; i < NUM_CYCLES; ++i){
			if(i % 10000 == 0){
				System.out.println("JenkinsAndTraubPolySolver.main done "+i + " so far.");
			}
			List<Polynomial> ps = s.factor(p1);
			if(firstPoly){
				System.out.println("First poly solved in "+s.getIterations()+" iterations.");
			}
			firstPoly = false;
			String st = "";
			boolean firstTime = true;
			Polynomial test = Polynomial.UNIT;
			Polynomial dp1dx = p1.derivative();
			
			for(Polynomial p : ps){
				//System.out.println("JenkinsAndTraubPolySolver.main() result: "+p);
				if(!firstTime){
					st += "*";
				}
				st += "("+p+")";
				test = test.multiplyBy(p);
				double dx;
				if(p.order() == 1){
					double x = p.linearRoot();
					dx = p1.evaluate(x)/dp1dx.evaluate(x);
				} else if(p.order() == 2){
					Complex[] xs = p.quadraticRoot();
					
					double dx1 = p1.evaluate(xs[0]).abs()/dp1dx.evaluate(xs[0].abs());
					double dx2 = p1.evaluate(xs[1]).abs()/dp1dx.evaluate(xs[1].abs());
					
					dx = Math.max(dx1, dx2);
				} else {
					dx = 0;
				}
				if(dx > worstDx){
					worstDx = dx;
				}
				
			}
			
			double epsilon = p1.maxCoeff()*Polynomial.EPSILON;
			if(p1.distance(test) > worst){
				worst = p1.distance(test);
				worstP = p1;
				worstEpsilon = epsilon;
			}
			if(p1.distance(test)/epsilon > worstRatio){
				worstRatio = p1.distance(test)/epsilon;
			}
			
			if(p1.distance(test) > epsilon){
				++badCount;
				//System.out.println("JenkinsAndTraubPolySolver.main "+badCount+" of "+i+" with error "+p1.distance(test)+" > "+epsilon+" --> "+st+"\n");
			}
			
			
			
			p1 = Polynomial.makeRandom(6, 1000);
		}
		double polyPerSecond = 	1.0/(((double)(System.currentTimeMillis() - startTime))/NUM_CYCLES/1000.0);
		System.out.println("JenkinsAndTraubPolySolver.main poly per second: "+polyPerSecond );
		System.out.println("JenkinsAndTraubPolySolver.main worst dx: "+worstDx );
		System.out.println("JenkinsAndTraubPolySolver.main worst error: "+worst+", epsilon: "+worstEpsilon+" from poly "+worstP.toArrayString());
		System.out.println("JenkinsAndTraubPolySolver.main worst ratio: "+worstRatio);
		
	}
	public int getIterations(){
		return m_iterations;
	}
	
}
