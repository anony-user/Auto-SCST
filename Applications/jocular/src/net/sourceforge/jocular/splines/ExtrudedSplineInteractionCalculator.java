package net.sourceforge.jocular.splines;

import java.util.List;

import net.sourceforge.jocular.math.Polynomial;
import net.sourceforge.jocular.math.SturmSolver;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.PropertyKey;

public class ExtrudedSplineInteractionCalculator {
	public enum ExtrusionEnd {FRONT, BACK};
	/**
	 * Computes possible interactions with the defined extruded spline
	 * @param pt
	 * @param extrudedSpline
	 * @param is
	 */
	public ExtrudedSplineInteractionCalculator(PhotonTrajectory pt, ExtrudedSpline extrudedSpline, InteractionSorter is) {
		
		//first project the photon trajectory to the plane the spline is defined on
		Photon photon = pt.getPhoton();
		boolean fromInside = pt.getContainingObject() == extrudedSpline;
		testSplineSurface(photon, fromInside, extrudedSpline, is);
		testSplineEnds(photon, fromInside, extrudedSpline, is);
	}
	/**
	 * Tests for possible interactions between the most recent photon and the ends of this extruded spline surface
	 * This uses Ampere's law that says the integral around a closed curve will be equal to any contained currents.
	 * If the intersection of the photon path and the plane of each end is inside the piecewise spline then the integral should be non-zero.
	 * Otherwise it will be zero.
	 * 
	 * The intergral is of:
	 * 
	 * B*dL and this should be either u0*I or -u0*I if the point is inside.
	 * 
	 * Assuming the point is the location of an infinite wire then B = u0*I/(2*PI*R)
	 * 
	 * Therefore the integral of (1/R)dL = +/-2*PI
	 * 
	 * L = sqrt(X(s)^2 + Y(s)^2)
	 * 
	 * If I assume that integrating around the curve will be the same even if I offset X and Y then:
	 * 
	 * L = sqrt((X(s) - px)^2 + (Y(s) - py)^2)
	 * 
	 * and
	 * 
	 * dL = (X*dX/ds*ds + Y*dY/ds*ds)/sqrt((X - px)^2 + (Y - py)^2)
	 * 
	 * R = sqrt((X - px)^2 + (Y - py)^2)
	 * 
	 * so (1/R)dL = 
	 * 
	 * (X*dX/ds*ds + Y*dY/ds*ds)/((X - px)^2 + (Y - py)^2)
	 * 
	 * 
	 * Integral of this is 0.5log(X - px)^2 + (Y - py)^2)
	 * 
	 * ***************************************
	 * I think this is wrong.
	 * 
	 * I think that |dL| = sqrt((dX/ds)^2 + (dY/ds)^2) which is different than above
	 * 
	 * dL = (dX/ds), (dY/ds), 0)*ds
	 * 
	 * What about Cauchy's Integral Theorem
	 * That's pretty much the same.
	 * 
	 * If I shift the splines so the test point is always at the origin then:
	 * 
	 * I need to integrate (R x dL)/|R|^2 where R = sqrt(X^2 + Y^2) and 'x' is the cross product
	 * This is the integral of:
	 * (X*Y' - Y*X')*dS/(X^2 + Y^2)
	 * 
	 * if I'm not mistaken the integral is atan(Y/X)
	 * 
	 * This hasn't worked any better.
	 * 
	 *  I'll try Jordan's Curve Theorem
	 * 
	 * @param photon
	 * @param fromInside
	 * @param extrudedSpline
	 * @param is
	 * @param ee 
	 */
	private void testSplineEnds(Photon photon, boolean fromInside, ExtrudedSpline extrudedSpline, InteractionSorter is) {
		Vector3D pDir = photon.getDirection();
		Vector3D pOrg = photon.getOrigin();
		
		Vector3D oTrans = extrudedSpline.getPositioner().getTransDirection();
		Vector3D oOrtho = extrudedSpline.getPositioner().getOrthoDirection();
		Vector3D oDir = extrudedSpline.getPositioner().getDirection();
		Vector3D oOrg = extrudedSpline.getPositioner().getOrigin();
		double thickness = ((EquationProperty)(extrudedSpline.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
		SplineCoefficients[] scs = extrudedSpline.getSplineCoefficients();
		
		
		//first compute the location of the  front and back planes
		Vector3D oFront = oOrg.add(oDir.scale(thickness/2.0));
		Vector3D oBack = oOrg.add(oDir.scale(-thickness/2.0));
		
		//now project photon to each plane
		
		double dFront = distanceAlongLineToPlane(pOrg, pDir, oFront, oDir);
		double dBack = distanceAlongLineToPlane(pOrg, pDir, oBack, oDir);
		
		//don't bother calculating any further if neither front nor back plane are in front of the photon origin
		if(dFront < 0 && dBack < 0){
			return;
		}
		
		Vector3D pFront = pOrg.add(pDir.scale(dFront));
		Vector3D pBack = pOrg.add(pDir.scale(dBack));
		
		
		double pFX = pFront.subtract(oOrg).dot(oOrtho);
		double pFY = pFront.subtract(oOrg).dot(oTrans);
		double pBX = pBack.subtract(oOrg).dot(oOrtho);
		double pBY = pBack.subtract(oOrg).dot(oTrans);
		
		
		
		

		

		if(dFront > 0 /*&& Double.isFinite(dFront)*/ && isPointInside(scs, pFX, pFY)){
			is.add(new PhotonInteraction(photon, extrudedSpline, ExtrusionEnd.FRONT, pFront, oDir, fromInside, "RotatedSpline front surface"));
		}
		if(dBack > 0 /*&& Double.isFinite(dBack) */&& isPointInside(scs, pBX, pBY)){
			is.add(new PhotonInteraction(photon, extrudedSpline, ExtrusionEnd.BACK, pBack, oDir, fromInside, "RotatedSpline back surface"));
		}
//		double frontIntegral = 0;
//		double backIntegral = 0;	
//		for(SplineCoefficients sc : scs){
			//test intersection with line X(s) = pFX
//		}
//			double X0 = sc.calcX(0);
//			double X1 = sc.calcX(1);
//			double Y0 = sc.calcY(0);
//			double Y1 = sc.calcY(1);
//			
//			double df0 = Math.atan2(Y0 - pFY, X0 - pFX);
//			double df1 = Math.atan2(Y1 - pFY, X1 - pFX);
//			double db0 = Math.atan2(Y0 - pBY, X0 - pBX);
//			double db1 = Math.atan2(Y1 - pBY, X1 - pBX);
////			double df0 = Math.atan((Y0 - pFY)/( X0 - pFX));
////			double df1 = Math.atan((Y1 - pFY)/( X1 - pFX));
////			double db0 = Math.atan((Y0 - pBY)/( X0 - pBX));
////			double db1 = Math.atan((Y1 - pBY)/( X1 - pBX));
//			frontIntegral += df1;// - df0;
//			
//			
//			backIntegral += db1 - db0;
//			
//		}
//		System.out.println("ExtrudedSplineInteractionCalculator.testSplineEnds xf,yf: "+pFX+","+pFY+" xb,yb: "+pBX+","+pBY);
//		System.out.println("ExtrudedSplineInteractionCalculator.testSplineEnds results: "+frontIntegral+ ", "+backIntegral);
//		
		
		
	}
	private boolean isPointInside(SplineCoefficients[] scs, double px, double py) {
		int passCount = 0;
		for(SplineCoefficients sc : scs){
			//solve intersection with line X(s) = px
			//then test if Y(s) > py
			Polynomial tp = sc.xSpline.subtract(Polynomial.makeFromCoefficients(new double[] {px}));
			SturmSolver ss = new SturmSolver(tp);
			ss.solve(0, 1);
			List<Polynomial> rs = ss.getRealRoots();
			for(Polynomial r : rs){
				if(r.order() == 1){
					double s = r.linearRoot();
					double y = sc.calcY(s);
					if(y - py > Polynomial.EPSILON){
						++passCount;
					}
				}
				
			}
			
		}
		//if passCount is odd then the point must be inside the curve
		return (passCount - 2*(passCount/2)) > 0;
	}
	/**
	 * Computes the distance along a line to a plane
	 * @param lo the origin of the line
	 * @param ld the direction of the line
	 * @param po a point on the plane
	 * @param pn the normal vector of the plane
	 * @return
	 */
	private double distanceAlongLineToPlane(Vector3D lo, Vector3D ld, Vector3D po, Vector3D pn){
	
		//calc the cos of the angle between the surface normal vector and the photon direction
		double cos = pn.dot(ld);
		
		//calc the normal distance from the surface to the photon origin
		double oDist = po.subtract(lo).dot(pn);
		double dDist = 0;
		if(cos != 0){
		
			dDist = oDist/cos;
		} else {
			dDist = Double.NaN;
		}
		return dDist;
	}
	/**
	 * Tests for possible interactions between the most recent photon and this extruded spline surface
	 * @param photon
	 * @param fromInside
	 * @param extrudedSpline
	 * @param is
	 */
	public void testSplineSurface(Photon photon, boolean fromInside, ExtrudedSpline extrudedSpline, InteractionSorter is){
		Vector3D pDir = photon.getDirection();
		Vector3D pOrg = photon.getOrigin();
		
		Vector3D oTrans = extrudedSpline.getPositioner().getTransDirection();
		Vector3D oOrtho = extrudedSpline.getPositioner().getOrthoDirection();
		Vector3D oDir = extrudedSpline.getPositioner().getDirection();
		Vector3D oOrg = extrudedSpline.getPositioner().getOrigin();
		double thickness = ((EquationProperty)(extrudedSpline.getProperty(PropertyKey.THICKNESS))).getValue().getBaseUnitValue();
		SplineCoefficients[] scs = extrudedSpline.getSplineCoefficients();
		//compute components of photon direction in spline plane
		double pDY = pDir.dot(oTrans);
		double pDX = pDir.dot(oOrtho);
		
		//compute photon origin referenced to spline plane
		Vector3D pOrgObj = pOrg.subtract(oOrg);
		double pOY = pOrgObj.dot(oTrans);
		double pOX = pOrgObj.dot(oOrtho);
		
		//for each spline segment:
		//X(s) = Ax*S^3 + Bx*S^2 + Cx*S + Dx = X(p) = pDX*p + pOX;
		//Y(s) = Ay*S^3 + By*S^2 + Cy*S + Dy = Y(p) = pDY*p + pOY;
		//
		//so scaling one equation by pDX/pDY and subtracting will yield a 3rd order polynomial to solve
		//which equation to scale is decided by which has the smallest component to its photon direction
		double kx;//amount to scale X equations
		double ky;//amount to scale Y equations
		if(Math.abs(pDY) > Math.abs(pDX)){
			if(pDX != 0){
				kx = -pDY/pDX;
				ky = 1.0;
			} else {
				kx = 1.0;
				ky = 0.0;
			}
			
		} else {
			if(pDY != 0){
				kx = 1.0;
				ky = -pDX/pDY;
			} else {
				kx = 0.0;
				ky = 1.0;
			}
			
		}
		
		double bestP = Double.MAX_VALUE;
		SplineCoefficients bestSC = null;
		Vector3D bestLoc = null;
		double bestS = Double.MAX_VALUE;
//		double bestX = 0;
//		double bestY = 0;
		for(SplineCoefficients sc : scs){
			Polynomial polyToSolve = sc.xSpline.multiplyBy(kx).add(sc.ySpline.multiplyBy(ky));
			polyToSolve = polyToSolve.subtract(Polynomial.makeFromCoefficients(new double[] {pOX*kx + pOY*ky}));
			List<Polynomial> roots = SturmSolver.factor(polyToSolve, 0, 1);
			for(Polynomial root : roots){
				if(root.order() == 1){
					double s = root.linearRoot();
					
					
					
					
					double p;

					if((Math.abs(pOX) > Math.abs(pOY)|| pDY == 0) && pDX != 0){
						double x = sc.calcX(s);
						p = (x - pOX)/pDX;
					} else {
						double y = sc.calcY(s);
						p = (y - pOY)/pDY;
					
					}
					if(p > 0){
						Vector3D loc = pDir.scale(p).add(pOrg);
						double t = Math.abs(loc.subtract(oOrg).dot(oDir));
						
						if(t*2 < thickness){
							
							
							double dxds = sc.xSpline.derivative().evaluate(s);
							double dyds = -sc.ySpline.derivative().evaluate(s);
							Vector3D normal = oOrtho.scale(dyds).add(oTrans.scale(dxds)).normalize();
							
							is.add(new PhotonInteraction(photon, extrudedSpline, sc, loc, normal, fromInside, "Extruded spline interaction"));
						}
					}
				}
			}
		}
	}
}
