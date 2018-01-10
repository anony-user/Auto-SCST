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
package net.sourceforge.jocular.splines;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.Polynomial;
import net.sourceforge.jocular.math.SturmSolver;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;

public class RotatedSplineInteractionCalculator {
	//private double m_bestS;//the indep parameter for the location on the spline where the intersection occurs
	//private double m_bestP;//the indep parameter for the photon trajectory where the intersection occurs. Essentiall the distance the photon must travel to intersect
	public RotatedSplineInteractionCalculator(PhotonTrajectory pt, RotatedSpline s, InteractionSorter is){
		computeInteractions(pt, s, is);
	}
	/**
	 * computes the distance along the p trajectory and the spline parameter of the LineToLineDistance of the first intersection of the specified rotated spline (r axis) 
	 * If no intersection occurs then Double.MAX_VALUE
	 * 
	 * 	now we have a relationship between the distance from the photon trajectory and a given distance down the rotational axis of the spline object
	 *
	 *	it's defined by equations of the form:
	 *	(1) z(p) = d + e*p 
	 *	(2) r^2(p) = a*p^2 + b*p + c
	 *	 where p is the parametric variable for the photon trajectory
	 *	
	 *	now we need to compute an equation to solve for the intersection with this spline object
	 *	the spline segment is defined by the following equations:
	 *	(3) z(s) = Ax*s^3 + Bx*s^2 + Cx*s + Dx
	 *	(4) r(s) = Ay*s^3 + By*s^2 + Cy*s + Dy
	 *	where s is a parameter that ranges from 0 to 1
	 *	
	 *	so combine (1)&(3) and (2)&(4) by their equalities we get
	 *	(5)  d + e*p = Ax*s^3 + Bx*s^2 + Cx*s + Dx
	 *	 and
	 *	(6) a*p^2 + b*p +c = (Ay*s^3 + By*s^2 + Cy*s + Dy)^2
	 *	
	 *	this can be solved in the following way:
	 *	by solving (5) for p
	 *	(7) p = Ax/e*s^3 + Bx/e*s^2 + Cx/e*s + Dx/e - d/e
	 *	now sub (7) into the lhs of (6) and a 6th order polynomial of s will result.
	 *	solve that.
	 *	note that there's a special case where e == 0. This can be handled by simply solving the equation:
	 *	 d = Ax*s^3 + Bx*s^2 + Cx*s + Dx.
	 *	
	 *	once s has been determined. Then use either (5) or (6) to solve for p. Note that (6) must be used if e == 0.
	 *	
	 *	coefficients for (1) & (2) are determined in the following way
	 *	d = (po - ro).rd ; e = pd.rd
	 *	a = M.M ; b = 2*M.N ; c = N.N
	 *	where M = pd - rd*e
	 *	and N = po - ro - rd*d
	 *	where po, pd, ro, rd are the input parameters of this method
	 *	note that '.' represents the dot product
	 *	
	 * @param ro spline origin
	 * @param rd spline direction (orientation)
	 * @param po photon origin
	 * @param pd photon direction of travel
	 * @param sc
	 * @return
	 */
	private SplineIntersectionResult computeRotationalIntersection(Vector3D ro, Vector3D rd, Vector3D po, Vector3D pd, SplineCoefficients sc){


		
		double d = po.subtract(ro).dot(rd);
		double e = pd.dot(rd);
		
		//Vector3D m = pd.add(rd.scale(e));//should be subtract?
		//Vector3D n = po.subtract(ro).add(rd.scale(d));//not right either?
		Vector3D m = pd.subtract(rd.scale(e));//should be subtract?
		Vector3D n = po.subtract(ro).subtract(rd.scale(d));//not right either?
		double a = m.dot(m);
		double b = 2*m.dot(n);
		double c = n.dot(n);
		
		
		Polynomial rSquaredOfP = Polynomial.makeFromCoefficients(new double[] {c, b, a});//p2rhs
		Polynomial zOfP = Polynomial.makeFromCoefficients(new double[] {d, e});//p3rhs
		//Polynomial pOfZ = Polynomial.makeFromCoefficients(new double[] {)
		boolean eIsZero = Math.abs(e) < Polynomial.EPSILON;
		
		Polynomial zOfS = sc.xSpline;//p3rhs
		Polynomial rOfS = sc.ySpline;//p4rhs
		
		
		Polynomial polyOfSToSolveOfS;
		Polynomial pOfS = null;
		if(eIsZero){
			//first handle the special case
			polyOfSToSolveOfS = zOfS.subtract(Polynomial.makeFromCoefficients(new double[] {d}));
		} else {
			pOfS = zOfS.subtract(Polynomial.makeFromCoefficients(new double[] {d})).multiplyBy(1.0/e);
			Polynomial rSquaredOfS = rSquaredOfP.absorb(pOfS);
			Polynomial rSquaredOfS2 = rOfS.power(2);
			polyOfSToSolveOfS = rSquaredOfS.subtract(rSquaredOfS2);
			
		}
		
		
		
		//now I just need to solve this polynomial of at most 6th degree for the smallest photon distance.
		
		
//		Polynomial rhs = ltld.getCurve().absorb(sc.xSpline);
//		Polynomial lhs = sc.ySpline.power(2);
//		Polynomial polyToSolve = rhs.subtract(lhs);
		
		//first check if there are any roots when s is between 0.0 and 1.0. If not, then there's no point looking
//		if(polyOfSToSolveOfS.numRoots(0.0, 1.0) < 1){
//			return SplineIntersectionResult.NULL;
//		}
		
		//List<Polynomial> sRoots = polyOfSToSolveOfS.factor();
		List<Polynomial> sRoots = SturmSolver.factor(polyOfSToSolveOfS, 0, 1);

		double m_bestP = Double.MAX_VALUE;
		double m_bestS = Double.NaN;
		for(Polynomial root : sRoots){
			//don't worry about roots of order 2 or 0, nether of those are useful solutions
			if(root.order() == 1){
				double s = root.linearRoot();
				double e0 = s + Polynomial.EPSILON;
				double e1 = s - 1.0 - Polynomial.EPSILON;
				if(e0 > 0 && e1 < 0){
					double testP = Double.MAX_VALUE;
					if(eIsZero){
						double rs2 = Math.pow(rOfS.evaluate(s), 2.0);
						Polynomial pt = rSquaredOfP.subtract(Polynomial.makeFromCoefficients(new double[]{rs2}));
						
						List<Polynomial> testPs = pt.factor();
						if(testPs.size() == 2){
							double tp1 = testPs.get(0).linearRoot();
							double tp2 = testPs.get(1).linearRoot();
							if(tp1 < 0 && tp2 < 0){
							} else if(tp1 < 0){
								testP = tp2;
							} else if(tp2 < 0){
								testP = tp1;
							} else if(tp2 > tp1){
								testP = tp1;
							} else {
								testP = tp2;
							}
						}
					} else {

						testP = pOfS.evaluate(s);
					}
					//if the newly calc'ed intersection is in front of the photon origin and a closer distance than the last one then make it the current best choice
					if(m_bestP > testP && testP > Polynomial.EPSILON){
						m_bestP = testP;
						m_bestS = s;
					}
				}
			}
		}
		
		//s
		//return !Double.isNaN(m_bestS);
		return new SplineIntersectionResult(sc, m_bestS, m_bestP);
	}
	protected static class SplineIntersectionResult {
		final SplineCoefficients sc;
		final double s;
		final double p;
		public static final SplineIntersectionResult NULL = new SplineIntersectionResult(null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		SplineIntersectionResult(SplineCoefficients sc, double s, double p){
			this.sc = sc;
			this.s = s;
			this.p = p;
		}
		public boolean isValid(){
			return !(Double.isNaN(p) || Double.isNaN(s) || Double.isInfinite(s) || Double.isInfinite(p));
		}
	}
	public void computeInteractions(PhotonTrajectory pt, SplineCoefficientsOwner object, InteractionSorter is){
		Photon p = pt.getPhoton();
		Vector3D po = p.getOrigin();
		Vector3D pd = p.getDirection();
		Vector3D ro = object.getPositioner().getOrigin();
		Vector3D rd = object.getPositioner().getDirection();
		
		SplineCoefficients[] scs = object.getSplineCoefficients();
		double pMinDist = Double.MAX_VALUE;//this will track the minimum distance the photon needs to travel before intersecting the spline
		double sMinDist = Double.MAX_VALUE;
		SplineCoefficients bestSpline = null;
		ArrayList<SplineIntersectionResult> sirs = new ArrayList<SplineIntersectionResult>();
		for(SplineCoefficients sc : scs){
			if(sc.ySpline.largestCoefficient() > Polynomial.EPSILON){
				//this is just on the z axis so ignore
				SplineIntersectionResult sir = computeRotationalIntersection(ro, rd, po, pd, sc);
				//add the result into a sorted list
				if(sir.isValid()){
					addInteraction(sir, po, pd, ro, rd, object, is, pt);
//					if(sirs.size() == 0){
//						sirs.add(sir);
//					} else {
//						for(int i = 0; i < sirs.size(); ++i){
//					
//							if(sirs.get(i).p > sir.p){
//								sirs.add(0, sir);
//								break;
//							} else if(i == sirs.size() - 1){
//								sirs.add(sir);
//								break;
//							}
//						}
//					}
					
				}
			}
		}
		boolean oddNumber = (sirs.size() & 0x1) != 0;
			
//		if(sirs.size() != 0){
//			
//		
//			if(pt.getContainingObject() == object){
//				//we're already in the object
//				if(!oddNumber){//is the number of potential interactions not odd? It should be if we're already in the object 
//					if(sirs.get(0).p < Polynomial.EPSILON*10000){
//						sirs.remove(0);
//					} else {
//						System.out.println("RotatedSplineInteractionCalculator.computeInteraction somehow we have an even number of intersections when we're already in the object.");
//					}
//				}
//			} else {
//				//we're not in the object
//				if(oddNumber){
//					if(sirs.get(0).p < Polynomial.EPSILON*10000){
//						sirs.remove(0);
//					} else {
//						System.out.println("RotatedSplineInteractionCalculator.computeInteraction somehow we have an odd number of intersections when we're not in the object.");
//					}
//					
//				}
//			}
//		}
//		if(sirs.size() != 0){
		
//			addInteraction(sir, po, pd, ro, rd, object, is);
//			
//		}
		


	}
	protected void addInteraction(SplineIntersectionResult sir, Vector3D po, Vector3D pd, Vector3D ro, Vector3D rd, SplineCoefficientsOwner object, InteractionSorter is, PhotonTrajectory pt){
		double pMinDist = sir.p;
		double sMinDist = sir.s;
		Photon p = pt.getPhoton();
		SplineCoefficients bestSpline = sir.sc;
		Vector3D loc = pd.scale(pMinDist).add(po);
		//calc the position on the object's rotational axis that corresponds to the interaction location
		double z = loc.subtract(ro).dot(rd);
		Vector3D vz = ro.add(rd.scale(z));
		//now compute the normal vector from that point to the interaction location
		Vector3D nOrtho = loc.subtract(vz).normalize();
		
		//the surface normal vector will be:
		// rd/(-dz/ds) + nOrtho/(-dr/ds)
		double dzds = -bestSpline.xSpline.derivative().evaluate(sMinDist);
		double drds = bestSpline.ySpline.derivative().evaluate(sMinDist);
		Vector3D norm = rd.scale(drds).add(nOrtho.scale(dzds));

		norm = norm.normalize();
		
		boolean fromInside = pt.getContainingObject() == object;
		PhotonInteraction pi = new PhotonInteraction(p, object, sir.sc, loc, norm, fromInside, "Rotated spline interaction");
		is.add(pi);
	}
	Vector3D getRotatedSplineNormal(SplineObject object){
		return null;
	}
}
