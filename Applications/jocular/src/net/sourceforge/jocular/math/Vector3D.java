/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
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


public class Vector3D {
	public static final Vector3D INF = new Vector3D();
	public static final Vector3D X_AXIS = new Vector3D(1,0,0);
	public static final Vector3D Y_AXIS = new Vector3D(0,1,0);
	public static final Vector3D Z_AXIS = new Vector3D(0,0,1);
	public static final Vector3D ORIGIN = new Vector3D(0,0,0);
	public final double x;
	public final double y;
	public final double z;
	public Vector3D(double x0, double y0, double z0){
//		if(Double.isInfinite(x0 + y0 + z0)){
//			throw new RuntimeException("Parameters are Inf.");
//		}
		if(Double.isNaN(x0 + y0 + z0)){
			throw new RuntimeException("Parameters are NaN.");
		}
		x = x0;
		y = y0;
		z = z0;
	}
	/**
	 * Compares this vector to the specified one.
	 * @return true if x, y, z components are all equal.
	 */
	public boolean equals(Object o){
		boolean result = false;
		if(o instanceof Vector3D){
			Vector3D v = (Vector3D)o;
			if(x == v.x && y == v.y && z == v.z){
				result = true;
			}
		}
		return result;
	}
	/**
	 * Creates a new vector with elements of the specified array
	 * x is specified by d[0]
	 * y is specified by d[1]
	 * z is specified by d[2]
	 * @param d
	 */
	public Vector3D(double[] d){
		if(d.length != 3){
			throw new RuntimeException("Array must have exactly three elements, not "+d.length);
		}
		x = d[0];
		y = d[1];
		z = d[2];
	}
	/**
	 * Creates a new vector with the specified element values
	 * @param p
	 */
	public Vector3D(Vector3D p){
		this(p.x, p.y, p.z);
	}
	/**
	 * this should only be used to generate INF
	 */
	private Vector3D(){
		x = Double.POSITIVE_INFINITY;
		y = Double.POSITIVE_INFINITY;
		z = Double.POSITIVE_INFINITY;
	}
	/**
	 * creates a random vector. Each element is assigned to the value of a call to a separate call to Math.random()
	 * @return
	 */
	public static Vector3D random(){
		return new Vector3D(Math.random(), Math.random(), Math.random());
	}
	/**
	 * computes the distance between the location indicated by this vector and that indicated by the specified one
	 * @param v
	 * @return
	 */
	public double distanceTo(Vector3D v){
		if(v == null || v == INF || this == INF){
			return Double.POSITIVE_INFINITY;
		}
		return subtract(v).abs();
	}
	/**
	 * computes the angle between this vector and another. the result will always be positive
	 * @param v
	 * @return
	 */
	public double angleBetween(Vector3D v){
		Vector3D nv = v.normalize();
		Vector3D n = normalize();
		double c = dot(nv);
		double s = cross(nv).abs();
		//return Math.acos(c);
		return Math.atan2(s, c);
	}
	/**
	 * returns a vector with each element the difference of the corresponding element of this vector and the specified value
	 * @param v
	 * @return
	 */
	public Vector3D subtract(double v){
		if(Double.isInfinite(v)){
			return INF;
		}
		Vector3D result = new Vector3D(x - v, y - v, z - v);
		return result;
	}
	/**
	 * Computes the difference between this vector and the specified one
	 * @param v
	 * @return
	 */
	public Vector3D subtract(Vector3D v){
		if(v == null || v == INF || this == INF){
			return INF;
		}
		Vector3D result = new Vector3D(x - v.x, y - v.y, z - v.z);
		return result;
	}
	/**
	 * Computest the sum of this vector's elements and the specified one
	 * @param p
	 * @return
	 */
	public Vector3D add(Vector3D p){
		if(p == null || p == INF || this == INF){
			return INF;
		}
		Vector3D result = new Vector3D(p.x + x, p.y + y, p.z + z);
		return result;
	}
	/**
	 * returns a vector with each element the sum of the corresponding element of this vector and the specified value
	 * @param v
	 * @return
	 */
	public Vector3D add(double v){
		if(Double.isInfinite(v)){
			return INF;
		}
		Vector3D result = new Vector3D(x + v, y + v, z + v);
		return result;
	}
	/**
	 * Computes the dot product of this vector and the specifed one
	 * @param p
	 * @return
	 */
	public double dot(Vector3D p){
		double result = 0;
		if(p == null){
			result = 0;
		} else if(p == INF || this == INF){
			result = Double.POSITIVE_INFINITY;
		} else {
			result = (p.x*x + p.y*y + p.z*z);
		}
		return result;
	}
	/**
	 * computes the cross product of this vector and the specified one
	 * @param p
	 * @return
	 */
	public Vector3D cross(Vector3D p){
		if(p == null){
			return null;
		}
		if(p == INF || this == INF){
			return INF;
		}
		return new Vector3D(y*p.z - z*p.y, z*p.x - x*p.z, x*p.y - y*p.x);
	}
	/**
	 * computes the magnitude of this vector
	 * @return
	 */
	public double abs(){
		if(this == INF){
			return Double.POSITIVE_INFINITY;
		}
		double result = Math.pow(x, 2);
		result += Math.pow(y, 2);
		result += Math.pow(z, 2);
		return Math.sqrt(result);
	}
	/**
	 * Computes the square of the magnitude of this vector
	 * @return
	 */
	public double magSquared(){
		if(this == INF){
			return Double.POSITIVE_INFINITY;
		}
		double result = Math.pow(x, 2);
		result += Math.pow(y, 2);
		result += Math.pow(z, 2);
		return result;
	}
	/**
	 * multiplies each element of this vector by the specified scale factor
	 * @param s
	 * @return
	 */
	public Vector3D scale(double s){
		if(this == INF){
			//return INF;
			throw new RuntimeException("Vector to scale is INF.");
			
		}
		if(Double.isInfinite(s)){
			//return INF;
			throw new RuntimeException("Scale factor is infinite.");
		}
		if(Double.isNaN(s)){
			throw new RuntimeException("Scale factor is NaN.");
		}
		return new Vector3D(x*s, y*s, z*s);
	}
	/**
	 * Multiplies each element of this vector by its corresponding element of the specified vector
	 * @param s
	 * @return
	 */
	public Vector3D scale(Vector3D s){
		if(this == INF){
			//return INF;
			throw new RuntimeException("Vector to scale is INF.");
			
		}
		
		
		return new Vector3D(x*s.x, y*s.y, z*s.z);
	}
	/**
	 * element by element divide of this vector by specified vector
	 * @param s
	 * @return
	 */
	public Vector3D divide(Vector3D s){
		if(this == INF){
			//return INF;
			throw new RuntimeException("Vector to scale is INF.");
			
		}
		if(s.x == 0 || s.y == 0 || s.z == 0){
			throw new RuntimeException("Element of dividing Vector is zero: "+s);
		}
		return new Vector3D(x/s.x, y/s.y, z/s.z);
	}
	/**
	 * Computes a vector with the same vector direction as this one but with a magnitude of 1
	 * @return
	 */
	public Vector3D normalize(){
		if(this == INF){
			return INF;
		}
		double m = abs();
		if(m == 0){
			return Vector3D.ORIGIN;
		}
		return new Vector3D(x/m, y/m, z/m);
	}
	public String toString(){
		if(this == INF){
			return "Vector3D.INF";
		}
		NumberFormat nf = new DecimalFormat("0.00E0");
		return "Vector3D x,y,z,abs = "+nf.format(x)+", "+nf.format(y)+", "+nf.format(z)+", "+nf.format(abs());
	}
	/**
	 * checks if this vector's magnitude is equal to one
	 * It uses a tolerance of 1e-12
	 * @return true if the magnitude is equal to one
	 */
	public boolean isNormalized(){
		if(this == INF){
			return false;
		}
		return (Math.abs(abs() - 1) < 0.000000000001)?true:false; 
	}
	/**
	 * computes a vector whose elements are all the negatives of this one
	 * @return
	 */
	public Vector3D neg(){
		if(this == INF){
			return INF;
		}
		return new Vector3D(-x, -y, -z);
	}
	/**
	 * computes the component of this vector along the direction of v
	 * @param v
	 * @return
	 */
	public Vector3D getParallelComponent(Vector3D v){
		if(v == null || v == INF || this == INF){
			return INF;
		}
		Vector3D vNorm = v.normalize();
		double dot = dot(vNorm);
		Vector3D result = vNorm.scale(dot);
		return result;
	}
	/**
	 * computes the component of this vector perpendicular to the direction of v
	 * @param v
	 * @return
	 */
	public Vector3D getPerpendicularComponent(Vector3D v){
		if(v == null || v == INF || this == INF){
			return INF;
		}
		Vector3D result = subtract(getParallelComponent(v));
		return result;
	}
	/**
	 * helper function to compute third vector of 3D basis set: dir, trans, ortho. 
	 * Dir is the main direction vector, like the direction of the photon.
	 * Trans is the main transverse direction with respect to Dir, like the main polarization axis of the photon.
	 * Ortho is the orthogonal direction to Trans, like the secondary polarization axis of the photon.
	 * @param dir
	 * @param ortho
	 * @return
	 */
	public static Vector3D getTrans(Vector3D dir, Vector3D ortho){
		return ortho.cross(dir);
	}
	/**
	  * helper function to compute third vector of 3D basis set: dir, trans, ortho. 
	 * Dir is the main direction vector, like the direction of the photon.
	 * Trans is the main transverse direction with respect to Dir, like the main polarization axis of the photon.
	 * Ortho is the orthogonal direction to Trans, like the secondary polarization axis of the photon.
	 * @param dir
	 * @param trans
	 * @return
	 */
	public static Vector3D getOrtho(Vector3D dir, Vector3D trans){
		return dir.cross(trans);
	}
	/**
	  * helper function to compute third vector of 3D basis set: dir, trans, ortho. 
	 * Dir is the main direction vector, like the direction of the photon.
	 * Trans is the main transverse direction with respect to Dir, like the main polarization axis of the photon.
	 * Ortho is the orthogonal direction to Trans, like the secondary polarization axis of the photon.
	 * @param trans
	 * @param ortho
	 * @return
	 */
	public static Vector3D getDir(Vector3D trans, Vector3D ortho){
		return trans.cross(ortho);
	}
	/**
	 * rotate the present vector around the specified one by a specified angle
	 * @param v
	 * @param rad
	 * @return
	 */
	public Vector3D rotate(Vector3D v, double rad){
		Vector3D n = v.normalize();
//		Vector3D result = n.scale(Math.cos(rad));
//		result.add(n.cross(this).scale(Math.sin(rad)));
//		result.add(n.scale(v.dot(this)*(1 - Math.cos(rad))));
//		return result;
		Vector3D comp = n.scale(dot(n));
		Vector3D perp = subtract(comp);
		Vector3D ortho = getOrtho(n,  perp);
		perp = perp.scale(Math.cos(rad));
		ortho = ortho.scale(Math.sin(rad));
		return (perp.add(ortho).add(comp));
	}
	/**
	 * compute the rotation required to move from this vector to another, centred on the origin
	 * result is a vector whose direction is the axis of rotation and whose magnitude is the amount to rotate
	 * @param from
	 * @param to
	 * @return
	 */
	public Vector3D calcRotation(Vector3D v) {
		Vector3D dir = v.cross(this);//magnitude of v.abs*this.abs*sin
		double s = dir.abs();
		double c = v.dot(this);//magnitude is v.abs*this.abs*cos
		if(Math.abs(s) == 0.0){
			return  Vector3D.ORIGIN;
		}
		dir = dir.scale(1.0/s);
		double angle = -Math.atan2(s, c);
		return dir.scale(angle);
	}
	/**
	 * rotates this vector around an origin by a rotation vector.
	 * The rotation vector is a vector whose direction is the axis of rotation and whose magnitude is the amount to rotate
	 * @param point
	 * @param origin
	 * @param rotation
	 * @return
	 */
	public Vector3D rotate(Vector3D origin, Vector3D rotation){
		double angle = rotation.abs();
		if(angle == 0.0){
			return this;
		}
		Vector3D axis = rotation.scale(1.0/angle);
		Vector3D temp = this.subtract(origin);
		temp = temp.rotate(axis, angle);
		temp = temp.add(origin);
		return temp;
	}
	/**
	 * rotates this vector around the origin by a rotation vector.
	 * The rotation vector is a vector whose direction is the axis of rotation and whose magnitude is the amount to rotate
	 * @param point
	 * @param rotation
	 * @return
	 */
	public Vector3D rotate(Vector3D rotation){
		double angle = rotation.abs();
		if(angle == 0.0){
			return this;
		}
		Vector3D axis = rotation.scale(1.0/angle);
		Vector3D temp = this.rotate(axis, angle);
		return temp;
	}
	/**
	 * Assuming this vector is a rotation vector then this will concatenate this with the specified rotation vector.
	 * This results in a single rotation vector that is equivalent to this rotation followed by the specified rotation.
	 * @param rotation
	 * @return
	 */
	public Vector3D concatenateRotation(Vector3D rotation){
		Vector3D r1 = rotation;
		Vector3D r2 = this;
		//compute quaternion for this vector
		double a1 = r1.abs();
		double w1 = 1.0;
		double x1 = 0.0; 
		double y1 = 0.0; 
		double z1 = 0.0;
		
		if(a1 != 0){
			w1 = Math.cos(a1/2);
			x1 = r1.x/a1*Math.sin(a1/2);
			y1 = r1.y/a1*Math.sin(a1/2);
			z1 = r1.z/a1*Math.sin(a1/2);
		}
		
		//compute quaternion for other vector
		double a2 = r2.abs();
		double w2 = 1.0;
		double x2 = 0.0;
		double y2 = 0.0;
		double z2 = 0.0;
		
		if(a2 != 0){
			w2 = Math.cos(a2/2);
			x2 = r2.x/a2*Math.sin(a2/2);
			y2 = r2.y/a2*Math.sin(a2/2);
			z2 = r2.z/a2*Math.sin(a2/2);
		}
		//concatenate quaternions
		double wr = w1*w2 - x1*x2 - y1*y2 - z1*z2;
		double xr = w1*x2 + x1*w2 + y1*z2 - z1*y2;
		double yr = w1*y2 + y1*w2 - x1*z2 + z1*x2;
		double zr = w1*z2 + z1*w2 + x1*y2 - y1*x2;
		
		//now convert back
		Vector3D result = new Vector3D(xr, yr, zr);
		double sinar2 = result.abs();
		double ar = Math.atan2(sinar2, wr);
		if(sinar2 == 0){
			result = Vector3D.ORIGIN;
		} else {
			result = result.scale(2.0*ar/sinar2);	
		}
		
		
		
		return result;
	}
	public static void main(String[] args){
		Vector3D r1 = Vector3D.X_AXIS.scale(Math.toRadians(30));
		Vector3D r2 = Vector3D.Y_AXIS.scale(Math.toRadians(60));
		Vector3D r3 = Vector3D.Z_AXIS.scale(Math.toRadians(90));
		Vector3D r4 = Vector3D.X_AXIS.scale(Math.toRadians(120));
		Vector3D r5= Vector3D.Y_AXIS.scale(Math.toRadians(150));
		Vector3D r6 = Vector3D.Z_AXIS.scale(Math.toRadians(180));
		
		
		Vector3D rt = r1.concatenateRotation(r2).concatenateRotation(r3).concatenateRotation(r4).concatenateRotation(r5).concatenateRotation(r6);
		
		Vector3D p1 = new Vector3D(-1,1,1);
		
		Vector3D p2 = p1.rotate(r1).rotate(r2).rotate(r3).rotate(r4).rotate(r5).rotate(r6);
		Vector3D p3 = p1.rotate(rt);
		
		System.out.println("Vector3D.main p2: "+p2+", "+p3);
		
	}
}
