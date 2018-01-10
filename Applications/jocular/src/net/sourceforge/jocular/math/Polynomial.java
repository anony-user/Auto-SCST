/*******************************************************************************
 * Copyright (c) 2015,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.math;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing polynomials with real coefficients
 * Note that for a polynomial degree n, all n+1 coefficients are specified, even if zero
 * The polynomial can be represented as: sum(i = 0..n)(Ci*x^i).
 * I'm trying to make all operations that result in a polynomial with all coefficients equal to 0.0 to be the ZEROTH constant
 * So far the only purpose to this class is to root-find for 6th order polynomials
 * @author kenneth
 *
 */
public class Polynomial {
	public static final Polynomial ZEROTH = new Polynomial(new double[] {0}){
		@Override
		public String toString(){
			return "Zero";
		}
	};
	public static final Polynomial UNIT = new Polynomial(new double[] {1});
	public static final double EPSILON = 50e-15;
	private final double[] m_coeffs;
	/**
	 * Construct a new polynomial based on the specified coefficients
	 * @param coeffs coeffs[0] is the constant term. coeffs[n] is the coefficient of the x^n term
	 */
	private Polynomial(double[] coeffs){
		m_coeffs = coeffs;
		
	}
	public Complex evaluate(Complex x){
		Complex result = new Complex(0.0, 0.0);
		for(int i = m_coeffs.length - 1; i >= 0; --i){
			result = result.multiply(x);
			result = result.add(m_coeffs[i]);
		}
		return result;
	}
	/**
	 * evaluates this polynomial at a point x where:
	 * result = sum(i = 0..n)(Ci*x^i)
	 * @param x
	 * @return
	 */
	public double evaluate(double x){
		double result = 0.0;
		for(int i = m_coeffs.length - 1; i >= 0; --i){
			result *= x;
			result += m_coeffs[i];
		}
		return result;
	}
	public int order(){
		return m_coeffs.length - 1;
	}
	/**
	 * constructs a random polynomial with an order randomly chosen up to the specified order
	 * @param order
	 * @return
	 */
	public static Polynomial makeRandom(int order, double maxCoeff){
		int nOrder = (int)Math.round(Math.random()*((double)order));
		double[] cs = new double[nOrder + 1];
		for(int i = 0; i < cs.length; ++i){
			cs[i] = (Math.random()*2 - 1.0)*maxCoeff;
		}
		return makeFromCoefficients(cs);
	}
	
	/**
	 * returns the product of this poly with a specified poly.
	 * @param p
	 * @return
	 */
	public Polynomial multiplyBy(Polynomial p){
		if(p == ZEROTH || this == ZEROTH){
			return ZEROTH;
		}
		if(this == UNIT){
			return p;
		}
		if(p == UNIT){
			return this;
		}
		double[] result = new double[p.order()+order() + 1];
		for(int i = 0; i < result.length; ++i){
			result[i] = 0.0;
		}
		for(int i = 0; i < p.m_coeffs.length; ++i){
			for(int j = 0; j < m_coeffs.length; ++j){
				result[i + j] += p.m_coeffs[i]*m_coeffs[j];
			}
		}
		return makeFromCoefficients(result);
	}
	public Polynomial multiplyBy(double d){
		if(d == 0.0 || this == ZEROTH){
			return ZEROTH;
		}
		double[] result = new double[m_coeffs.length];
		for(int i = 0; i < result.length; ++i){
			result[i] = m_coeffs[i]*d;
		}
		
		return makeFromCoefficients(result);
	}
	public void printPlotData(double min, double max){
		for(double x = min; x < max; x += (max - min)/1000 ){
			System.out.println(""+x+", "+evaluate(x));
		}
	}
	public boolean isOrderGreaterThanOrEqualTo(Polynomial p){
		boolean result = true;
		if(order() >= p.order()){
			result = true;
		} else if(order() < p.order()){
			result = false;
		} else {
			result = false;
		}
		return result;
	}
	/**
	 * multiplies this polynomial by x^n. Causes n zero-valued coefficients to be added to the right, shifting the whole array to the left. 
	 * @param n
	 * @return
	 */
	public Polynomial leftShift(int n){
		double[] result = new double[m_coeffs.length + n];
		for(int i = 0; i < result.length; ++i){
			if(i < n){
				result[i] = 0.0;
			} else {
				result[i] = m_coeffs[i - n];
			}
		}
		return makeFromCoefficients(result);
	}
	/**
	 * multiplies this polynomial by x^n. Causes n zero-valued coefficients to be added to the right, shifting the whole array to the left. 
	 * @param n
	 * @return
	 */
	public Polynomial rightShift(int n){
		if(n > order()){
			return ZEROTH;
		}
		double[] result = new double[m_coeffs.length - n];
		for(int i = 0; i < result.length; ++i){
			if(i < n){
				result[i] = 0.0;
			} else {
				result[i] = m_coeffs[i - n];
			}
		}
		return makeFromCoefficients(result);
	}
	/**
	 * returns the result of dividing this poly by the specified poly
	 * @param p
	 * @return an array of polynomials. result[0] is the quotient and result[1] is the remainder which in the double case is always zero
	 */
	public Polynomial[] dividedBy(double d){
		Polynomial[] result = new Polynomial[2];
		result[1] = ZEROTH;
		result[0] = multiplyBy(1.0/d);
		return result;
		
	}
	/**
	 * returns the result of dividing this poly by the specified poly
	 * @param p
	 * @return an array of polynomials. result[0] is the quotient and result[1] is the remainder
	 */
	public Polynomial[] dividedBy(Polynomial p){
		int pOrder = p.order();
		//if p is just a constant then divide by a constant
		if(pOrder == 0){
			return dividedBy(p.get(0));
		}
		
		
		Polynomial[] result = new Polynomial[2];

		//if this poly is actually zero or of lower order than p then it won't divide
		if(this == ZEROTH || !isOrderGreaterThanOrEqualTo(p)){
			result[0] = ZEROTH;
			result[1] = this;
			return result;
		}
		int order = order();
		int dOrder = order - pOrder;
		//if p is zero then throw an exception
		if(p == ZEROTH){
			throw new RuntimeException("Divide by zero.");
		}
		
		
		
		double[] quotient = new double[m_coeffs.length];
		int n = 0;
		double[] remainder = new double[m_coeffs.length];
		System.arraycopy(m_coeffs, 0, remainder, 0, m_coeffs.length);
		
		for(int i = dOrder; i >= 0; --i){
			double r = remainder[i + pOrder];
			if(Math.abs(r) > EPSILON){
				r /= p.get(pOrder);
				quotient[i] = r;
				
				for(int j = pOrder; j >= 0; --j){
					if(j == pOrder){
						remainder[j + i] = 0;
					} else {
						remainder[j + i] -= r*p.get(j);
					}
				}
			} else {
				quotient[i] = 0;
			}
				
		
				
			
		}
		
		
		result[0] = Polynomial.makeFromCoefficients(quotient);
		result[1] = Polynomial.makeFromCoefficients(remainder);

		return result;
	}
	private Polynomial clearTopTerm() {
		if(m_coeffs.length <= 1){
			return this;
		}
		double[] coeffs = new double[m_coeffs.length - 1];
		for(int i = 0; i < coeffs.length; ++i){
			coeffs[i] = m_coeffs[i];
		}
		return makeFromCoefficients(coeffs);
	}
	public Polynomial subtract(Polynomial p){
		int size = (m_coeffs.length > p.m_coeffs.length) ? m_coeffs.length : p.m_coeffs.length;
		double[] result = new double[size];
		int zeroCount = 0;
		for(int i = 0; i < size; ++i){
			result[i] = 0.0;
			if(m_coeffs.length > i){
				result[i] += m_coeffs[i];
			}
			if(p.m_coeffs.length > i){
				result[i] -= p.m_coeffs[i];
			}
			if(result[i] == 0.0){
				++zeroCount;
			}
		}
		if(zeroCount == size){
			return ZEROTH;
		}
		return makeFromCoefficients(result);
		
	}
	public Polynomial add(Polynomial p){
		int size = (m_coeffs.length > p.m_coeffs.length) ? m_coeffs.length : p.m_coeffs.length;
		double[] result = new double[size];
		int zeroCount = 0;
		for(int i = 0; i < size; ++i){
			result[i] = 0.0;
			if(m_coeffs.length > i){
				result[i] += m_coeffs[i];
			}
			if(p.m_coeffs.length > i){
				result[i] += p.m_coeffs[i];
			}
			if(result[i] == 0.0){
				++zeroCount;
			}
		}
		if(zeroCount == size){
			return ZEROTH;
		}
		return makeFromCoefficients(result);
	}
	
	public Polynomial derivative(){
		if(order() == 0){
			return ZEROTH;
		}
		double[] result = new double[m_coeffs.length - 1];
		for(int i = 0; i < result.length; ++i){
			result[i] = m_coeffs[i + 1]*((double)i + 1.0);
		}
		return makeFromCoefficients(result);
		
	}
	public static Polynomial makeFromCoefficients(double[] coeffs){
		

		if(coeffs[coeffs.length - 1] != 0.0){
			return new Polynomial(coeffs);
		} else {
			int n = 0;
			for(int i = coeffs.length - 1; i >= 0; --i){
				if(coeffs[i] == 0){
					++n;
				} else {
					break;
					
				}
			}
			if(n == coeffs.length){
				return ZEROTH;
			}
			double[] result = new double[coeffs.length - n];
			for(int i = 0; i < result.length; ++i){
				result[i] = coeffs[i];
			}
			return new Polynomial(result);
		}
	}
	public static Polynomial makeFromRealRoots(double[] roots){
		if(roots.length < 1){
			return ZEROTH;
		}
		Polynomial result = UNIT;
		for(double r : roots){
			result = result.multiplyBy(makeFromCoefficients(new double[] {-r , 1.0}));
		}
		return result;
	}
	/**
	 * makes a quadratic with complex roots that are complex conjugates: r+i*c and r - i*c
	 * @param r
	 * @param c
	 * @return
	 */
	public static Polynomial makeFromComplexPair(double r, double c){
		return makeFromCoefficients(new double[] {c*c + r*r, -2.0*r, 1.0});
	}
	public static Polynomial makeFromComplexPair(Complex c1, Complex c2){
		
		return makeFromComplexPair(c1, c2, false);
	}
	public static Polynomial makeFromComplexPair(Complex c1, Complex c2, boolean force){
		Complex p0 = c1.multiply(c2);
		Complex p1 = c1.neg().subtract(c2);
		if(!force){
			if(p0.imag() > EPSILON || p1.imag() > EPSILON){
				throw new RuntimeException("Specified complex numbers don't combine to yeild real coefficients: "+ c1 + ", "+c2);
			}
		}
		return makeFromCoefficients(new double[] {p0.real(), p1.real(), 1.0});
	}
	/**
	 * creates a new polynomial with the same coefficients except that any less than epsilon will be set to zero
	 * @param epsilon
	 * @return
	 */
	public Polynomial roundToZero(double epsilon){
		double[] result = new double[m_coeffs.length];
		int n = 0;
		for(int i = 0; i < result.length; ++i){
			if(Math.abs(m_coeffs[i]) <= epsilon){
				result[i] = 0.0;
				++n;
			} else {
				result[i] = m_coeffs[i];
			}
		}
		if(n == result.length){
			return ZEROTH;
		}
		return makeFromCoefficients(result);
	}
	/**
	 * returns the number of real roots within the interval [min, max]
	 * uses Sturm's Theorem
	 * any coefficients that are zero must be exactly equal to zero. A coefficient that is 0+epsilon will be treated as a real coefficient with a value of epsilon
	 * If epsilon is actually an error then these coefficients should be first zeroed. See the {@link #roundToZero(double) roundToZero} method. 
	 * @param min
	 * @param max
	 * @return
	 */
	public int numRoots(double min, double max){
		//first check that min and max are not zeros
		if(Math.abs(evaluate(min)) < EPSILON){
			return 1;//TODO: think carefully about what to really return
		}
		if(Math.abs(evaluate(max)) < EPSILON){
			return 1;//TODO: think carefully about what to really return
		}
		//ensure max is greater than min
		if(max <= min){
			return 0;
		}
		if(order() == 0){
			return 0;
		}
		
		//normalize the polynomial so the highest order coeff is 1.0
		Polynomial poly = multiplyBy(1.0/get(order()));
		//now construct the polynomial sequence required by Sturm's Theorem
		List<Polynomial> ps = new ArrayList<Polynomial>();
		boolean keepGoing = true;
		int i = 0;
		//Polynomial lastTerm;
		while(keepGoing){
			switch(i){
			case 0:
				ps.add(i, poly);
				break;
			case 1:
				ps.add(i, poly.derivative());
				break;
			default:
				//set last term to the remainder of the division of the last two elements
				ps.add(i, ps.get(i - 2).dividedBy(ps.get(i - 1))[1].multiplyBy(-1.0));
				if(ps.get(i) == ZEROTH){
					keepGoing = false;
				}
			}
			
			++i;
			if(i > order()){//stop this just in case it goes too far
				keepGoing = false;
			}
		}
		//now count sign changes in the sequences 
		int minSign = 0;
		int maxSign = 0;
		int signChangeCount = 0;
		for(Polynomial p : ps){
			double pMin = p.evaluate(min);
			double pMax = p.evaluate(max);
			if(pMin > EPSILON){
				if(minSign < 0){
					++signChangeCount;
				}
				minSign = 1;
			} else if(pMin < -EPSILON){
				if(minSign > 0){
					++signChangeCount;
				}
				minSign = -1;
			}
			if(pMax > EPSILON){
				if(maxSign < 0){
					--signChangeCount;
				}
				maxSign = 1;
			} else if(pMax < -EPSILON){
				if(maxSign > 0){
					--signChangeCount;
				}
				maxSign = -1;
			}
		}
		return signChangeCount;
		
	}
	public double getMostSignificantCoefficient(){
		return m_coeffs[m_coeffs.length - 1];
	}
	@Override
	public String toString(){
		String result = "";
		boolean firstTime = true;
		NumberFormat nf = new DecimalFormat("##0.############E0");
		for(int i = order(); i >= 0; --i){
			if(!firstTime){
				result += " + ";
			} else {
				firstTime = false;
			}
			if(Double.isNaN(m_coeffs[i])){
				result += "NaN";
			} else {
				result += nf.format(m_coeffs[i]);
			}
			//result += m_coeffs[i];
			if(i > 1){
				result += "*x^"+i;
			} else if(i == 1){
				result += "*x";
			}
		}
		return result;
	}
	/**
	 * 
	 * @param i
	 * @return the coefficient of the nth term. That's the term with x^n. Returns 0.0 for all higher terms than are present
	 */
	public double get(int n){
		if(n >= m_coeffs.length){
			return 0.0;
		}
		return m_coeffs[n];
	}
	public double maxCoeff(){
		double result = Double.MIN_NORMAL;
		for(double c : m_coeffs){
			if(Math.abs(c) > result){
				result = Math.abs(c);
			}
		}
		return result;
	}
	/**
	 * not sure if this is the best name but this will take the specified polynomial and apply it as an argument to this polynomial.
	 * @param p
	 * @return a big polynomial.
	 */
	public Polynomial absorb(Polynomial p){
		Polynomial result = ZEROTH;
		for(int i = 0; i < m_coeffs.length; ++i){
			double c = m_coeffs[i];
			result = result.add(p.power(i).multiplyBy(c));
		}
		return result;
	}
	/**
	 * Creates a polynomial that is this one to the nth power. n must be an integer.
	 * @param n
	 * @return
	 */
	public Polynomial power(int n){
		Polynomial result = UNIT;
		for(int i = 0; i < n; ++i){
			result = result.multiplyBy(this);
		}
		return result;
	}

	public @Deprecated List<Polynomial> factor2(){
		List<Polynomial> result = new ArrayList<Polynomial>();
	
		Polynomial dividend = this;
	
		double k = get(order());
		if(k != 1.0){
			
			Polynomial pk = makeFromCoefficients(new double[] {k});
			dividend = dividend.dividedBy(pk)[0];
			result.add(pk);
		}
		
		
		while(dividend.order() > 2){
			Complex c = dividend.laguerreSolve(0.5, 10);
			Polynomial d = makeFromComplex(c);
			if(c.isReal()){
				Complex newC = laguerreSolve(c, 0);
				if(newC.divide(c).abs() > 1.1){
					System.out.println("Polynomial.factor2 probably found different root: "+c+", "+newC);
				}
				d = makeFromComplex(newC);
				
				result.add(d);
			} else {
				double u = d.get(1)/d.get(2);
				double v = d.get(0)/d.get(2);
				d = bairstowSolve(u, v);
				
				result.addAll(d.quadraticRealRoots());
			}
			
			
			
			dividend = dividend.dividedBy(d)[0];
			
		}
		if(dividend.order() == 2){
			result.addAll(dividend.quadraticRealRoots());
		} else {
			
			result.add(dividend);
		
			
			
		}
		return result;
	}
	public List<Polynomial> factor(){
		JenkinsAndTraubPolySolver js = new JenkinsAndTraubPolySolver();
		return js.factor(this);
	}
	/**
	 * limit for iterations of numerical solving algorithms, like Newton's or Bairstow's method
	 */
	private static final int ITERATION_LIMIT = 30;
	private static final int NEWTON_ITERATION_LIMIT = 100;

	public @Deprecated List<Polynomial> factor3(){
		ArrayList<Polynomial> result = new ArrayList();
		double k = get(order());
		//first get rid of first term coefficient if it's not 1
		if(Math.abs(k - 1) > EPSILON && order() > 0){
			result.addAll(multiplyBy(1/k).factor());
			result.add(makeFromCoefficients(new double[] {k}));
		} else {
			switch(order()){
			case 0:
			case 1:
				result.add(this);
				break;
			case 2:
				result.addAll(quadraticRealRoots());
				break;
			default:
				Polynomial r;
//				if(isEven()){
//					r = bairstowSolve(0.0, 0.0);
//				}
				Complex c  = laguerreSolve(1.0,10);
				
				double u = 0.0; 
				double v = 0.0;
				if(evaluate(c).abs() < EPSILON){
					r = makeFromComplex(c);
				} else if(!c.isReal()){
					//System.out.println("Polynomial.factor 1 error: "+evaluate(c).abs()+" with estimated root: "+c);
					r = makeFromComplex(c);
					u = r.get(1)/r.get(2);
					v = r.get(0)/r.get(2);
					r = bairstowSolve(u, v);
					//System.out.println("Polynomial.factor 2 error (real): "+evaluate(r.quadraticRoot()[0]).abs());
					
				} else {
					//System.out.println("Polynomial.factor 1 error: "+evaluate(c).abs()+" with estimated root: "+c);
					r = makeFromComplex(laguerreSolve(c.abs(),10000));
					//System.out.println("Polynomial.factor 2 error (real): "+evaluate(r.quadraticRoot()[0]).abs());

					
				}
//				if(r == null){//if bairstow fails then try newton
//					r = bairstowSolve(0.0, 0.0);
//				}
//				if(r == null){
//					r = newtonSolve(0.0);
//				}
				if(r == null){
					throw new RuntimeException("Solving failed Bairstow and Newton for polynomial "+toArrayString());
				}
				if(r.order() > 1){
					result.addAll(r.quadraticRealRoots());
				} else {
					result.add(r);
				}
				
				//result.addAll(r.factor());
				result.addAll(this.dividedBy(r)[0].factor());
				break;
			}
		}
		return result;
	}
	private boolean isEven() {
		return (order()%2 == 0);
	}
	protected List<Polynomial> cubicRealRoots(){
		
		if(order() > 3){
			throw new RuntimeException("Order must be less that 3, not "+order());
		} else if(order() < 3){
			return quadraticRealRoots();
		}
		ArrayList<Polynomial> result = new ArrayList<Polynomial>();
		double a = m_coeffs[3];
		double b = m_coeffs[2];
		double c = m_coeffs[1];
		double d = m_coeffs[0];
		
		double delta = 18*a*b*c*d - 4*b*b*b*d + b*b*c*c - 4*a*c*c*c - 27*a*a*d*d;
		double delta0 = b*b - 3.0*a*c;
		double delta1 = 2*b*b*b - 9*a*b*c + 27*a*a*d;
		
		
		double r;
		Polynomial p;
		if(delta == 0){
			if(delta0 == 0){
				r = -b/3/a;
				p = Polynomial.makeFromRealRoots(new double[] {r});
				result.add(p);
				result.addAll(dividedBy(p)[0].quadraticRealRoots());
			} else {
				r =  (9*a*d -b*c)/2/delta0;
				p = Polynomial.makeFromRealRoots(new double[] {r});
				result.add(p);
				result.add(p);
				r = (4*a*b*c - 9*a*a*d - b*b*b)/a/delta0;
				p = Polynomial.makeFromRealRoots(new double[] {r});
				result.add(p);
			}
		} else {
			double capC;
			if(delta0 == 0){
				capC = Math.pow(delta1, 1.0/3.0);
				r = (b + capC + delta0/capC)/-3.0/a;
				p = Polynomial.makeFromRealRoots(new double[] {r});
				result.add(p);
				Polynomial[] ps = dividedBy(p);
				result.addAll(ps[0].quadraticRealRoots());
			
			} else {
				//Complex sqr1 = new Complex(delta1*delta1 - 4.0*delta0*delta0*delta0);
				Complex sqr1 = new Complex(-27*a*a*delta);
				sqr1 = sqr1.sqrt();
				Complex sqr2 = sqr1.add(delta1);
				Complex sqr3 = sqr1.neg().add(delta1);
				if(sqr2.abs() < sqr3.abs()){
					sqr2 = sqr3;
				}
				
				Complex u1 = new Complex(1);
				Complex u2 = new Complex(-0.5, Math.sqrt(3.0/4.0));
				Complex u3 = new Complex(-0.5, -Math.sqrt(3.0/4.0));
				Complex cc = sqr2.divide(2).rootn(3.0);
				Complex cdelta0 = new Complex(delta0);
				Complex u1c = u1.multiply(cc);
				Complex u2c = u2.multiply(cc);
				Complex u3c = u3.multiply(cc);
				Complex cr1 = u1c.add(b).add(cdelta0.divide(u1c)).divide(-3.0*a);
				Complex cr2 = u2c.add(b).add(cdelta0.divide(u2c)).divide(-3.0*a);
				Complex cr3 = u3c.add(b).add(cdelta0.divide(u3c)).divide(-3.0*a);
				double i1 = Math.abs(cr1.imag());///cr1.real());
				double i2 = Math.abs(cr2.imag());///cr2.real());
				double i3 = Math.abs(cr3.imag());///cr3.real());
				double i;
				Complex cr;
				double p0, p1;
				double qe;
				Complex cri;
				Complex crii;
				if(i2 < i1 && i2 < i3){
					cr = cr2;
					cri = cr1;
					crii = cr3;
					i = i2;
					
					
				} else if(i3 < i1 && i3 < i2){
					cr = cr3;
					cri = cr1;
					crii = cr2;
					i = i3;
					
				} else {
					cr = cr1;
					cri = cr2;
					crii = cr3;
					i = i1;
					
				}
				
				qe = cri.subtract(crii).sqr().real();
				if(cr.isNaN()){
					throw new RuntimeException("Chosen root is NaN for "+this);
				}
				
				
				double e = Math.abs(evaluate(cr.real()));
				double ei;
				double eii;
				if(qe > 0){
					ei = evaluate(cri.real());
					eii = evaluate(crii.real());
				} else {
					ei = evaluate(cri).abs();
					eii = evaluate(crii).abs();
				}
				
				
//				if(e > EPSILON){
//					Complex crx = laguerreSolve(cr, 5);
//					Complex crxi = laguerreSolve(cri, 5);
//					Complex crxii = laguerreSolve(crii, 5);
//					double exi = evaluate(crxi).abs();				
//					double exii = evaluate(crxii).abs();
//
//					
//					double ex = evaluate(crx).abs();
//					
//					if(ex < e){
//						cr = crx;
//					}
//					if(exi < ei){
//						cri = crxi;
//					}
//					if(exii < eii){
//						crii = crxii;
//					}
//					
//					
//				}
				
				
				//Double e = evaluate(cr).abs();
				
				//if(cr.onlyImag().abs() < EPSILON){
					p = makeFromRealRoots(new double[] {cr.real()});
//				} else {
//					throw new RuntimeException("This should be real, not complex. "+cr);
//				}
				
				result.add(p);
				//if qe is less than zero then the two other roots are imaginary
				if(qe < 0){
					result.add(makeFromComplexPair(cri, crii, true));
				} else {
					double ri = cri.real();
					double rii = crii.real();
					result.add(makeFromRealRoots(new double[] {ri}));
					result.add(makeFromRealRoots(new double[] {rii}));
				}
					
					
					
				
				
			}
			
			
		}
		
		
		return result;
	}
	protected List<Polynomial> quadraticRealRoots(){
		ArrayList<Polynomial> result = new ArrayList<Polynomial>();
		if(order() > 2){
			return null;
		} else if(order() < 2){
			result.add(this);
			return result;
		}
		double a = m_coeffs[2];
		double b = m_coeffs[1];
		double c = m_coeffs[0];
		double b2a = b/2.0/a;
		double sqr = b2a*b2a - c/a;
		if(sqr < 0){
			result.add(this);
		} else {
			sqr = Math.sqrt(sqr);
			double r1 = -b2a + sqr;
			double r2 = -b2a - sqr;
//			double xr1 = laguerreSolve(r1, 5).real();
//			double xr2 = laguerreSolve(r2, 5).real();
//			double e1 = Math.abs(evaluate(r1));
//			double e2 = Math.abs(evaluate(r2));
//			double xe1 = Math.abs(evaluate(xr1));
//			double xe2 = Math.abs(evaluate(xr2));
//			if(e1 > xe1){
//				r1 = xr1;
//			}
//			if(e2 > xe2){
//				r2 = xr2;
//			}
//			double epsilon = EPSILON*maxCoeff();
//			if(e1 >  epsilon && e2 > epsilon){
////				Polynomial[] ps = dividedBy(makeFromRealRoots(new double[] {e2}));
////				double xxr2 = ps[0].linearRoot();
////				double xxe2 = Math.abs(evaluate(xxr2));
//				throw new RuntimeException("Bad fit: "+e1+", "+e2);
//			}
			result.add(makeFromRealRoots(new double[] {r1}));
			result.add(makeFromRealRoots(new double[] {r2}));
		}
		return result;
		
	}
	protected Complex[] quadraticRoot(){
		if(order() == 1){
			Complex[] result = new Complex[1];
			result[0] = new Complex(-get(0)/get(1));
			return result;
		} else if(order() != 2){
			return null;
			
		}
		Complex[] result = new Complex[2];
		double a = m_coeffs[2];
		double b = m_coeffs[1];
		double c = m_coeffs[0];
		double negb2a = -b/2.0/a;
		double sqr = (b*b/4.0/a - c)/a;
		if(sqr >= 0){
			result[0] = new Complex(negb2a+Math.sqrt(sqr),0.0);
			result[1] = new Complex(negb2a-Math.sqrt(sqr),0.0);
		} else {
			result[0] = new Complex(negb2a).add((new Complex(sqr)).sqrt());
			result[1] = new Complex(negb2a).subtract((new Complex(sqr)).sqrt());
		}
		return result;
	}
	protected static int countRealRootsFromFactors(List<Polynomial> ps, double min, double max){
		int result = 0;
		for(Polynomial p : ps){
			if(p.order() == 1){
				double r = -p.get(0)/p.get(1);
				if(r >= min && r <= max){
					++result;
				}
			}
		}
		return result;
	}
	/**
	 * Solves by newtons method for one root.
	 * Probably better to use bairstow solve or factor
	 * @param guess
	 * @return
	 */
	public Polynomial newtonSolve(double guess){
		Polynomial dThis = this.derivative();
		double error = Double.MAX_VALUE;
		double result = guess;
		int n = 0;
		double lastError = Double.MAX_VALUE;
		double lastGuess = guess;
		boolean done = false;
		while(!done){
			++n;
			error = this.evaluate(result);
			result = result - error/dThis.evaluate(result);
			//double rError = Math.abs()
			if(Math.abs(error/lastError + 1.0) < 0.3){
				result = (result + lastGuess)/2.0;
			}
			if(result == lastGuess){
				done = true;
			}
			lastGuess = result;
			lastError = error;
			
			System.out.println("Polynomial.newtonSolve: guess: "+result+", error: "+error);
			if(Math.abs(error) <= EPSILON || n > NEWTON_ITERATION_LIMIT){
				done = true;
			}
		}
		if(n >= NEWTON_ITERATION_LIMIT){
			return null;
		}
		return makeFromRealRoots(new double[] {result});
	}

	public Complex laguerreSolve(double guess, int maxIterations){
		return laguerreSolve(new Complex(guess, 0.0), maxIterations);
	}
	/**
	 * finds a root of this polynomial. Either finds a single real root or a complex conjugate quadratic.
	 * @param guess
	 * @param maxIterations if <= 0 then runs to completion if possible or returns null if not. If > 0 then runs specified iterations and returns result 
	 * @return
	 */
	public Complex laguerreSolve(Complex guess, int maxIterations){
		
		Polynomial d = derivative();
		Polynomial dd = d.derivative();
		double n = order();
		Complex result = guess;
		boolean done = false;
		Complex lastResult = result;
		int i = 0;
		ArrayList<Complex> iters = new ArrayList<Complex>();
		
		while(!done){
			iters.add(result);
			++i;
			Complex p = evaluate(result);
			if(p.abs() == 0.0){
				done = true;
			} else {
//				Complex dp = d.evaluate(result);
//				Complex ddp = dd.evaluate(result);
//				Complex h = dp.multiply(dp).multiply(n - 1.0).subtract(p.multiply(ddp.multiply(n))).multiply(n -1);
				
				Complex g = d.evaluate(result).divide(p);
				Complex h = g.multiply(g).subtract(dd.evaluate(result).divide(p));
				Complex den = h.multiply(n).subtract(g.multiply(g)).multiply(n - 1);
				//System.out.println("Polynomial.laguerreSolver den = "+den);
				den = den.sqrt();
//				System.out.println("Polynomial.laguerreSolver sqrt(den) = "+den);
				Complex a = new Complex(n, 0.0);
				Complex a1 = a.divide(g.add(den));
				Complex a2 = a.divide(g.subtract(den));
				if(a1.isNaN()){
					a = a2;
				} else if(a2.isNaN()){
					a = a1;
				} else if(a1.abs() > a2.abs()){
					a = a2;
					
				} else {
					a = a1;
				}
				
				result = result.subtract(a);
				double resultRatio = Math.abs(Math.abs(result.abs()/lastResult.abs()) - 1); 
				if(resultRatio < EPSILON){
					done = true;
					//System.out.println("Polynomial.laguerreSolve no real change in result after "+n+" cycles. Error = "+p.abs());
				}
				if(result.equals(lastResult)){
					done = true;
				}
				if(maxIterations > 0 && i >= maxIterations){
					done = true;
				}
				
				//System.out.println("Polynomial.laguerreSolve result = "+result+" abs(p): "+p.abs()+", den = "+den+", g = "+g+", h = "+h+", a = "+a);
				if(p.abs() < EPSILON){
					done = true;
				}
				if(result.isNaN()){
					throw new RuntimeException("Result is NaN.");
				}
				if(i > ITERATION_LIMIT && done != true){
					
					
					
					
					done = true;
					
					System.out.println("Polynomial.laguerreSolve failed with error "+p.abs()+" and result ratio "+resultRatio+ " when solving polynomial "+toArrayString());
					result = null;
					//throw new RuntimeException("Iteration limit reached for polynomial "+this+" and resulting error is still "+p);
					
				}
				lastResult = result;
				
			}
			
		}
		if(result == null){
			return null;
		}
		//Polynomial pResult = makeFromComplex(result);
		//System.out.println("Polynomial.laguerreSolve solution: "+pResult);
		if(Math.abs(result.imag()) < EPSILON){
			result = result.onlyReal();
		}
		return result;
	}
	public Polynomial bairstowSolve(Polynomial p){
		if(p.order() != 2){
			return null;
		}
		return bairstowSolve(p.get(1), p.get(0));
	}
	public Polynomial bairstowSolve(double uGuess, double vGuess){
		double u = uGuess;
		double v = vGuess;
		boolean done = false;
		int n = 0;
		while(!done){
			++n;
			Polynomial test = Polynomial.makeFromCoefficients(new double[] {v, u, 1.0});
			
			Polynomial[] qr = dividedBy(test);
			double c = qr[1].get(1);
			double d = qr[1].get(0);
			qr = qr[0].dividedBy(test);
			double g = qr[1].get(1);
			double h = qr[1].get(0);
			double k = (v*g*g + h*h - h*u*g);
			if(k == 0){
				throw new RuntimeException("k is 0");
			}
			double du = (-c*h + g*d)/k;
			double dv = (-g*v*c + g*u*d - h*d)/k;
			u -= du;
			v -= dv;
			if(Math.abs(du) < EPSILON && Math.abs(dv) < EPSILON){
				done = true;
			}
			double error = evaluate(test.quadraticRoot()[0]).abs();
			if(error < EPSILON){
				done = true;
			}
			if(n > ITERATION_LIMIT && done != true){
				//throw new RuntimeException("Iteration limit reached on polynomial "+this);
				System.out.println("Polynomial.bairstowSolve failed with error: "+(Math.abs(du)+Math.abs(dv)));
				return null;

			}
			if(Double.isNaN(u) || Double.isNaN(v)){
				//throw new RuntimeException("Solving failed on polynomial "+this);
				return null;
			}
			//System.out.println("Polynomial.bairstowSolve u,v, c, d = "+u+", "+v+", "+c+", "+d);
			
		}
	
		return Polynomial.makeFromCoefficients(new double[] {v, u, 1.0});
	}
	/**
	 * makes a real coeficient polynomial from the specified complex number
	 * if c is actually real then a 1st order polynomial results
	 * if c is complex then a second order poly is constructed with c and its conjugate
	 * @param c
	 */
	protected static  Polynomial makeFromComplex(Complex c){
		Polynomial result;
		if(c == null){
			return null;
		}
		if(c.isReal()){
			result = makeFromRealRoots(new double[] {c.real()});
		} else {
			double c1 = -2*c.real();
			double c0 = c.absSq();
			
			result = makeFromCoefficients(new double[] {c0, c1, 1.0});
		}
		return result;
	}
	public static Polynomial multiplyAll(List<Polynomial> ps){
		Polynomial result = UNIT;
		for(Polynomial p : ps){
			result = result.multiplyBy(p);
		}
		return result;
	}
	public String toArrayString(){
		String result = "{";
		
		for(int i = 0; i < m_coeffs.length; ++i){
			if(i != 0){
				result += ", ";
			}
			
			result += m_coeffs[i];
		}
		result += "}";
		return result;
	}
	private static String arrayStringFromRootList(List<Polynomial> ps){
		String result = "";
		for(Polynomial p : ps){
			result += "("+p+")";
		}
		return result;
	}
	public double largestCoefficient(){
		double result = Double.MIN_NORMAL;
		for(double c : m_coeffs){
			if(Math.abs(c) > result){
				result = Math.abs(c);
			}
		}
		return result;
	}
	public boolean isNan() {
		boolean result = false;
		for(double d : m_coeffs){
			if(Double.isNaN(d)){
				result = true;
				break;
			}
		}
		return result;
	}
	public double linearRoot(){
		if(order() != 1){
			throw new RuntimeException("Order must be 1, not "+order());
		}
		return -m_coeffs[0]/m_coeffs[1];
	}
	public double distance(Polynomial p){
		if(order() != p.order()){
			return Double.MAX_VALUE;
		}
		double result = 0;
		for(int i = 0; i < m_coeffs.length; ++i){
			result += Math.pow(m_coeffs[i] - p.m_coeffs[i], 2);
		}
		return Math.sqrt(result);
	}
	private static void cubeRootTest(){
		int badCount = 0;
		Polynomial p3 = makeFromCoefficients(new double[] {-954.0662998207641, 740.0628694340326, -0.0016376335605361447});
		for(int i = 0; i < 1000000; ++i){
			//make a cubic and root it
			
			p3 = p3.dividedBy(p3.get(p3.order()))[0];
			List<Polynomial> roots = p3.cubicRealRoots();
			//now multiply the roots together
			Polynomial test = UNIT;
			for(Polynomial p : roots){
				test = test.multiplyBy(p);
			}
			//test should be same as p3
			//for now let's just compare the strings
			double e = test.distance(p3);
			if(e > 1e-08){
				
				++badCount;
				System.out.println("So far "+badCount+" of "+i+" roots have been bad. "+e+", "+p3+" ---> "+test);
				System.out.println("     "+p3.toArrayString());
			}
			p3 = Polynomial.makeRandom(3, 1000);
		}
	}
	public static void main (String args[]){
		Polynomial p1,p2, p3, p4, p5, p6, p7, p8, p9, p10;
		//p = new Polynomial(new double[] {2.0, 1.0, 3.0});
		//p = Polynomial.makeFromComplexPair(3, 2);
		p1 = Polynomial.makeFromRealRoots(new double[] {-0.9,-0.8,-0.5,-4}).multiplyBy(Polynomial.makeFromComplexPair(2, 3));
		p2 = Polynomial.makeFromRealRoots(new double[] {3,2,4});
		p3 = Polynomial.makeFromRealRoots(new double[] { 1, 1,});
		p4 = Polynomial.makeFromCoefficients(new double[] {590.129348046111, 774.5739602889239, 920.9974504798464, -849.5819426370368, 634.5223738278904, 65.11549151468743});
		p5 = Polynomial.makeFromCoefficients(new double[] {5,4,-6,10,11, 22, 88, 40, -80,-2});
		p6 = makeFromCoefficients(new double[] {0.62639, -0.82037,-0.02824, 1});
		p7 = makeFromCoefficients(new double[] {-979.96296,515.47495,906.66731,954.50358,-510.97924,633.75022,-212.82336});
		//p8 = makeFromCoefficients(new double[] {1.55487, 1.75239, 0.26388, 0.02389, 1.66402, 1});
		//p8 = makeFromCoefficients(new double[] {24.53022,-119.66346, 96.0249, 48.67825, 231.46746, 1});
		p8 = makeFromCoefficients(new double[] {0.8855898648893807, -2.7648988374959007, 2.6424531065208012, 1.5770850501030125, 0.9133455717735042, 2.7771209635420036, 1.0});
		p9 = makeFromCoefficients(new double[] {-421.00948, -846.92191, -628.65323, -496.33089});
		p10 = makeFromCoefficients(new double[] {166.28373178, -248.67006036, -183.3958513, -719.92635351, 245.08813552, -436.52736086, 326.29226962});
		//cubeRootTest();
		System.out.println("P1 = "+p1);
		System.out.println("P2 = "+p2);
		Polynomial[] qs = p1.dividedBy(p2);
		System.out.println("Quotient: "+qs[0]);
		System.out.println("Remainder: "+qs[1]);
		System.out.println("P1 should be: "+qs[0].multiplyBy(p2).add(qs[1]));
		System.out.println("Greater than: "+p1.isOrderGreaterThanOrEqualTo(p2));
		System.out.println("Zero: "+ZEROTH);
		System.out.println("Sturm: "+p1.numRoots(0,1));
		System.out.println("Square: "+ Polynomial.makeFromRealRoots(new double[] {3,4}).power(2));
		System.out.println("Solve: " + p2.newtonSolve(2.5));
		System.out.println("Quadratic Solve: "+Polynomial.makeFromRealRoots(new double[] {3,4}).quadraticRealRoots().get(0));
		System.out.println("Laguerre Solve " + p4 + " --> " + p4.laguerreSolve(0.0, 0));
		//System.out.println("MultiSolve: "+p1.multiSolve());
//		System.out.println("Newton solve square: "+p6 + " -> "+p6.newtonSolve(0.0));
//		System.out.println("Newton solve square: "+p7 + " -> "+p7.newtonSolve(0.0));
		int sturmCount = 0;
		int badCount = 0;
		JenkinsAndTraubPolySolver solver = new JenkinsAndTraubPolySolver();
		long startTime = System.currentTimeMillis();
		final int NUM_CYCLES = 10000000;
		long totalIterations = 0;
		for(int i = 0; i < NUM_CYCLES; ++i){
			if(i % 1000000 == 0){
				double iPer = totalIterations;
				iPer /= i;
				double tPer = (double)(System.currentTimeMillis() - startTime);
				tPer /= ((double)i)*1000.0;
				//tPer = tPer
				System.out.print("Polynomial.main done "+i + " so far, with "+ iPer+" iterations and "+tPer+" seconds per poly.\r");
			}
			p5 = makeRandom(6, 1000);
			//System.out.println("Bairstow solv "+p5+" --> "+p5.bairstowSolve(11.0/6.0, -33.0/6.0));
			
			
			int nSturm = p5.numRoots(0.0, 1.0);
			if(true){//nSturm > 1){
				List<Polynomial> ps = null;
				try {
					//ps = p5.factor();
					//ps = p5.factor2();
					ps = solver.factor(p5);
					totalIterations += solver.getIterations();
					int nCount = countRealRootsFromFactors(ps, 0.0, 1.0);
					
					double r = Double.MIN_NORMAL;
					Polynomial result = UNIT;
					for(Polynomial p : ps){
						Complex[] cs = p.quadraticRoot();
						double tr;
						if(cs != null && cs.length > 0){
							for(Complex c : cs){
								tr = p.evaluate(c).abs();
								if(tr > r){
									r = tr;
								}
							}
						}
						
						
						
						result = result.multiplyBy(p);
					}
					
					if(p5.subtract(result).largestCoefficient() < EPSILON){
						r = 0.0;
					}
					String doubleRoots = "";
					for(int j = 0; j < ps.size(); ++j){
						Polynomial f1 = ps.get(j);
						if(f1.order() == 1){//don't bother testing if order is 2
							double r1 = -f1.get(0)/f1.get(1);
		
							
							for(int k = j; k < ps.size(); ++k){
								Polynomial f2 = ps.get(k);
								double r2 = -f2.get(0)/f2.get(1);
								if(Math.abs(r2 - r1) < EPSILON){
									doubleRoots = "double-rooted ("+r2+")";
									break;
								}
							}
						}
						
					}
					if(r > 1e-12){
						System.out.println("Remainder "+r+" too large for Factors of "+p5.subtract(result).toArrayString());
//						for(Polynomial p : ps){
//							System.out.println("    "+p);
//						}
						//System.out.println(" = "+result));
					}
					
					
					if(nSturm <= 0 && nCount > 0){
						++sturmCount;
						System.out.println(""+sturmCount+" This " + doubleRoots+" has "+nCount+" not "+nSturm+" roots between 0.0 and 1.0. "+arrayStringFromRootList(ps));
						System.out.println("          "+p5+" should equal "+multiplyAll(ps));
						//System.out.println("It actually has "+nCount+" roots in that range.");
						//throw new RuntimeException("Root count does not match");
					}
				} catch(RuntimeException e){
					++badCount;
					System.out.println("Polynomial.main bad root: "+p5.toArrayString()+" "+e);
					e.printStackTrace();
				}
				
			}
		}
		double polyPerSecond = 	1.0/(((double)(System.currentTimeMillis() - startTime))/NUM_CYCLES/1000.0);
		System.out.println("JenkinsAndTraubPolySolver.main poly per second: "+polyPerSecond );
		System.out.println("Polynomial.main num bad roots: "+badCount);
		System.out.println("Numroots: "+p8.numRoots(0.0, 1.0));
		//p8.printPlotData(-40, 40);
		//List<Polynomial> ps = p8.factor();
		//System.out.println("Numroots: "+p8.numRoots(0.0, 1.0));
		//System.out.println(""+p8+" ---> "+multiplyAll(ps));
		//System.out.println("Solving problem: "+p6.bairstowSolve(1.0, 1.0));
		System.out.println("PHEW! DONE.");
		
	}
	/**
	 * Parse the specified string and create a polynomial
	 * Assumes a format like the toString result
	 * it will be a function of 'x'
	 * '^' means "raised to the power of"
	 * e.g. 4*x^4 + 5*x^3 - 6*x^2 + 7*x + 8
	 * @param string
	 * @return
	 */
	public static Polynomial parseString(String s) {
		
		String t = s.trim().replace(" ", "");//remove all whitespace
		t = t.toLowerCase();
		t = t.replace("e-", "en");
		t = t.replace("+-", "-");
		t = t.replace("+", ",+");
		t = t.replace("-", ",-");
		t = t.replace("*", "");
		t = t.replace("x^", "x");
		t = t.replace("x,", "x1,");
		t = t.replace("n", "-");
		if(t.startsWith(",")){
			t = t.substring(1);
		}
		String [] ts = t.split(",");
		double[] ds = new double[ts.length];
		int[] is = new int[ts.length];
		for(int i = 0; i < ts.length; ++i){
			
			int j = ts[i].indexOf("x");
			if(j != -1){
				String c = ts[i].substring(0, j);
				String x = ts[i].substring(j+1);
				ds[i] = Double.parseDouble(c);
				is[i] = Integer.parseInt(x);
			} else {
				if(!ts[i].equals("")){
					ds[i] = Double.parseDouble(ts[i]);
					is[i] = 0;
				}
			}
			
			
		}
		//find maximum order
		int maxI = 0;
		for(int i : is){
			if(i > maxI){
				maxI = i;
			}
		}
		double[] coeffs = new double[maxI+ 1];
		for(int i = 0; i < coeffs.length; ++i){
			coeffs[i] = 0.0;
		}
		for(int i = 0; i < ds.length; ++i){
			coeffs[is[i]] = ds[i];
		}
		return Polynomial.makeFromCoefficients(coeffs);
	}
	public Polynomial removeInsignificantCoefficients() {
		int n = m_coeffs.length;
		double[] cs = new double[n];
		double max =0;
		for(int i = 0; i < n; ++i){
			cs[i] = m_coeffs[i];
			if(Math.abs(cs[i]) > Math.abs(max)){
				max = cs[i];
			}
		}
		for(int i = 0; i < n; ++i){
			cs[i] /= max;
			if(Math.abs(cs[i]) < EPSILON){
				cs[i] = 0;
			}
		}
		return Polynomial.makeFromCoefficients(cs);
	}
	
}
