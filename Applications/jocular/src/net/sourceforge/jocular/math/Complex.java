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

public class Complex {
	private static final double EPSILON = 1e-25;
	private final double m_real;
	private final double m_imag;
	public Complex(double rp){
		m_real = rp;
		m_imag = 0.0;
	}
	public Complex(double rp, double ip){
		m_real = rp;
		m_imag = ip;
	}
	public Complex add(Complex c){
		return new Complex(m_real + c.m_real, m_imag + c.m_imag);
	}
	public Complex add(double d){
		return new Complex(m_real + d, m_imag);
	}
	public Complex sqr(){
		return multiply(this);
	}
	public Complex subtract(Complex c){
		return new Complex(m_real - c.m_real, m_imag - c.m_imag);
	}
	
	public Complex subtract(double d){
		return new Complex(m_real - d, m_imag);
	}
	public Complex multiply(Complex c){
		return new Complex(m_real*c.m_real - m_imag*c.m_imag, m_imag*c.m_real + m_real*c.m_imag);
	}
	public Complex multiply(double d){
		return new Complex(m_real*d, m_imag*d);
	}
	public Complex neg(){
		return new Complex(-m_real, -m_imag);
	}
	public Complex divide(Complex c){
		double r = m_real*c.m_real + m_imag*c.m_imag;
		double i = m_imag*c.m_real - m_real*c.m_imag;
		double d = c.m_real*c.m_real + c.m_imag*c.m_imag;
		return new Complex(r/d, i/d);
	}
	public Complex divide(double d){
		double r = m_real/d;
		double i = m_imag/d;
		return new Complex(r, i);
	}
	public Complex rootn(double n){
		double abs = abs();
		double arg = arg();
		return complexFromPolar(Math.pow(abs,1.0/n), arg/n);
	}
	public Complex sqrt(){
		if(m_imag == 0 && m_real > 0){
			return new Complex(Math.sqrt(m_real),0);
		} else {
//			double r = Math.sqrt((m_real+Math.sqrt(m_real*m_real + m_imag*m_imag))/2);
//			double i = Math.signum(m_imag)*Math.sqrt((-m_real+Math.sqrt(m_real*m_real + m_imag*m_imag))/2);
			double abs = Math.sqrt(abs());
			double arg = arg()/2;
			return complexFromPolar(abs, arg);
		}
	}
	public double abs(){
//		if(m_real == 0.0){
//			return m_imag;
//		} else if(m_imag == 0.0){
//			return m_real;
//		}
		return Math.sqrt(m_real*m_real + m_imag*m_imag);
	}
	
	public double arg(){
		return Math.atan2(m_imag, m_real);
	}
	public static Complex complexFromPolar(double r, double a){
		return new Complex(r*Math.cos(a), r*Math.sin(a));
	}
	public double absSq(){
		return m_real*m_real + m_imag*m_imag;
	}
	public double real(){
		return m_real;
	}
	public double imag(){
		return m_imag;
	}
	public Complex onlyReal(){
		return new Complex(m_real, 0.0);
	}
	public Complex onlyImag(){
		return new Complex(0.0, m_imag);
	}
	public boolean isNaN(){
		return Double.isNaN(m_real + m_imag);
	}
	public Complex conjugate(){
		return new Complex(m_real, -m_imag);
	}
	public String toString(){
		DecimalFormat df = new DecimalFormat("0.####E0");
		if(isNaN()){
			return "(NaN)";
		}
		String ip = df.format(m_imag);
		if(ip.equals("0") || ip.equals("-0")){
			ip = "";
		} else {
			ip += "*i";
			if(ip.substring(0, 1).equals("-")){
				ip = " " + ip;
			} else if(!ip.equals("")){
				ip = " +" + ip;
			}
		}

		return "(" + df.format(m_real) + ip + ")";
	}
	public boolean isReal(){
		if(m_real == 0){
			if(m_imag == 0){
				return true;
			} else {
				return false;
			}
		}
		return Math.abs(m_imag/m_real) < EPSILON;
	}
}

