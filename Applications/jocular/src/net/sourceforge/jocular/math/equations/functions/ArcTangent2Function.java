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

import net.sourceforge.jocular.math.equations.BaseUnit;
import net.sourceforge.jocular.math.equations.EquationParser.CalcState;
import net.sourceforge.jocular.math.equations.Unit;
import net.sourceforge.jocular.math.equations.UnitedValue;


public class ArcTangent2Function implements Function {
	@Override
	public UnitedValue perform(UnitedValue... uvs){// throws IncorrectUnitsException, WrongNumberOfArgumentsException{
		if(uvs.length != 2){
			//throw new WrongNumberOfArgumentsException(1,uvs.length);
			return UnitedValue.makeError(CalcState.WRONG_NUMBER_ARGUMENTS);
		}
		UnitedValue uv1 = uvs[0];
		UnitedValue uv2 = uvs[1];
		//if the input argument is in error then return that argument without finishing calculation
		if(uv1.isError()){
			return uv1;
		}
		if(uv2.isError()){
			return uv2;
		}
		if(!uv1.getUnit().isMatchOrUnitless(uv2.getUnit())){
			return UnitedValue.makeError(CalcState.MISMATCHED_UNITS);
		}

	
		Unit u = BaseUnit.RADIAN;
		double v = Math.atan2(uv1.getBaseUnitValue(),uv2.getBaseUnitValue());
		
		return UnitedValue.makeFunctionResult(u,v);
	}
}
