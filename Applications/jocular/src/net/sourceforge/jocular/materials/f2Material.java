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


public class f2Material extends SimpleOpticalMaterial {
	
	private static final double[] TX = {3.37209000E-007, 3.52713000E-007, 3.68217000E-007, 3.83721000E-007, 4.27649000E-007, 1.53101000E-006, 1.96770000E-006, 2.31137000E-006, 2.50000000E-006};
	private static final double[] TY = {0.939823, 0.990324, 0.996932, 0.99882, 1, 0.999764, 0.995044, 0.985605, 0.979233};

	public f2Material(){
		super(new SellmeierEquation(1.34533359, 0.209073176, 0.937357162, 0.00997743871, 0.0470450767, 111.886764), (FunctionOfX)(new LookupTable(TX, TY, 200, true)), null);
		
	}
	
	@Override
	public Color getColour(){
		
		// Light red
		return new Color(238,144,144);
	}
}

