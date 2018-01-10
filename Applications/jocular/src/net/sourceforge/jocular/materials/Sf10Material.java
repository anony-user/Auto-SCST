/*******************************************************************************
 * Copyright (c) 2014, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.LookupTable;
import net.sourceforge.jocular.math.SellmeierEquation;


public class Sf10Material extends SimpleOpticalMaterial {
	private static final double[] TX = {100e-9, 101e-9, 365.0E-9, 370.0E-9, 380.0E-9, 390.0E-9, 400.0E-9, 405.0E-9, 420.0E-9, 436.0E-9, 460.0E-9, 500.0E-9, 546.0E-9, 580.0E-9, 620.0E-9, 660.0E-9, 700.0E-9, 1.1E-6, 1.5E-6, 2.0E-6, 2.3E-6, 2.5E-6};
	private static final double[] TY = {0, 0, 0.676243338, 0.77679961, 0.898210546, 0.96102016, 0.985263389, 0.990614342, 0.996660118, 0.998399196, 0.999103453, 0.999578469, 0.999799669, 0.999799669, 0.999709325, 0.999709325, 0.999799669, 0.999889891, 0.999487799, 0.996660118, 0.989080574, 0.985263389};
//	private static final double[] NX = {404.7E-9, 435.8E-9, 480.0E-9, 486.1E-9, 546.1E-9, 587.6E-9, 589.3E-9, 632.8E-9, 643.8E-9, 656.3E-9, 706.5E-9, 852.1E-9, 1.0E-6, 1.1E-6, 1.5E-6, 2.0E-6, 2.3E-6};
//	private static final double[] NY = {1.77579, 1.76198, 1.74805, 1.74648, 1.7343, 1.72825, 1.72803, 1.72309, 1.722, 1.72085, 1.71681, 1.70887, 1.70345, 1.70227, 1.69378, 1.6875, 1.68218};
	public Sf10Material(){
		super(new SellmeierEquation(1.61625977, 0.259229334, 1.07762317, 0.0127534559, 0.0581983954, 116.60768), (FunctionOfX)(new LookupTable(TX, TY, 200, true)), null);
		
	}
	
	@Override
	public Color getColour(){
		// Light Green
		return new Color(144,238,144);
	}
}

