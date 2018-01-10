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
package net.sourceforge.jocular.math.equations.functions;

import net.sourceforge.jocular.math.equations.EquationParser.CalcState;
import net.sourceforge.jocular.math.equations.Unit;
import net.sourceforge.jocular.math.equations.UnitedValue;


public class AdditionFunction implements Function {
	@Override
	public UnitedValue perform(UnitedValue... uvs){// throws IncorrectUnitsException, WrongNumberOfArgumentsException{
		if(uvs.length != 2){
			//throw new WrongNumberOfArgumentsException(2,uvs.length);
			return UnitedValue.makeError(CalcState.WRONG_NUMBER_ARGUMENTS);
		}
		//if the input argument is in error then return that argument without finishing calculation
		if(uvs[0].isError()){
			return uvs[0];
		} else if(uvs[1].isError()){
			return uvs[1];
		}
		double v = 0;
		UnitedValue uv1 = uvs[0];
		UnitedValue uv2 = uvs[1];
		Unit u1 = uv1.getUnit();
		Unit u2 = uv2.getUnit();
		Unit u;
		if(!u1.isMatchOrUnitless(u2)){
			//throw new IncorrectUnitsException(uv1.getUnit(), uv2.getUnit());
			return UnitedValue.makeError(CalcState.MISMATCHED_UNITS);
		}
		if(u1.isUnitless()){
			u = u2;
		} else {
			u = u1;
		}
		
		v = u.convertFromBaseUnitValue(uv1.getBaseUnitValue() + uv2.getBaseUnitValue());
		
		return UnitedValue.makeFunctionResult(u,v);
	}
}
