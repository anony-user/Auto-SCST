/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
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
/**
 * A class to compute the parametric equation of the distance from one line to another
 * This equation will always be of the form d = sqrt(as^2 + bs + c), where s is a parameter of the first line, such that the line can be described as: o1 + s*d1
 * Note that this class computes the coefficients for the parametric equation along line1.
 * This means it is more efficient to compute the distance along line1, not line2.
 * Choose which line you assign to line1 and line2 with this in mind.
 * The intent of this class is as a helper to calculate intermediate results towards intersections between photons (lines) and surfaces.
 * In particular, this should be useful for computing intersections with revolved surfaces.
 * If this turns out to be a heavily used class then it might be good to implement lazy computation of results and cache them.
 * @author kmaccallum
 *
 */
public class LineToLineDistance {
	private Vector3D m_line1Origin;
	private Vector3D m_line1Dir;
	private Vector3D m_line2Origin;
	private Vector3D m_line2Dir;
	private Polynomial m_curve;
//	private double m_a;
//	private double m_b;
//	private double m_c;
	/**
	 * makes a new object based on the specified two lines
	 * @param o1 - origin of 1st line
	 * @param d1 - direction of 1st line
	 * @param o2 - origin of 2nd line
	 * @param d2 - direction of 2nd line
	 */
	public LineToLineDistance(Vector3D o1, Vector3D d1, Vector3D o2, Vector3D d2){
		m_line1Origin = o1;
		m_line1Dir = d1;
		m_line2Origin = o2;
		m_line2Dir = d2;
		calcCoeffs();
	}
	private void calcCoeffs(){
		Vector3D c1 = m_line2Dir.scale(m_line2Origin.dot(m_line2Dir) - m_line1Origin.dot(m_line2Dir)).add(m_line1Origin).subtract(m_line2Origin);
		Vector3D c2 = m_line1Dir.subtract(m_line2Dir.scale(m_line1Dir.dot(m_line2Dir)));
		m_curve = Polynomial.makeFromCoefficients(new double[] { c2.magSquared(), 2.0*c1.dot(c2), c1.magSquared() });
//		m_a = c1.magSquared();
//		m_b = 2.0*c1.dot(c2);
//		m_c = c2.magSquared();
	}
	
	public double distanceAlongLine2(double s1){
		return (s1 - m_line1Dir.dot(m_line2Origin) + m_line1Dir.dot(m_line1Origin))/m_line1Dir.dot(m_line2Dir);
	}
	public double distanceAlongLine1(double s2){
		return m_line2Dir.scale(s2).add(m_line2Origin).subtract(m_line1Origin).dot(m_line1Dir);
	}
	public double distanceFromLine1(double s1){
		return Math.sqrt(m_curve.evaluate(s1));
	}
	public double distanceFromLine2(double s2){
		return distanceFromLine1(distanceAlongLine1(s2));
	}
	public Polynomial getCurve(){
		return m_curve;
	}
	
	
}
