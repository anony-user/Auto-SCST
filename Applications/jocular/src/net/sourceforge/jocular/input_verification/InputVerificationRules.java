/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.input_verification;

import net.sourceforge.jocular.math.equations.BaseUnit;
import net.sourceforge.jocular.math.equations.ObjectNotFoundException;
import net.sourceforge.jocular.math.equations.ObjectUnderspecifiedException;
import net.sourceforge.jocular.math.equations.PropertyNotFoundException;
import net.sourceforge.jocular.math.equations.functions.IncorrectUnitsException;
import net.sourceforge.jocular.math.equations.functions.WrongNumberOfArgumentsException;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;

public class InputVerificationRules {

	public static VerificationResult verifyPositiveLength(String s, PropertyOwner po, PropertyKey key){
		
		boolean isValid = true;
		String message = "";
		//LengthProperty test;
		EquationProperty test;
		
		try{
			//test = new LengthProperty(s);
			
			test = new EquationProperty(s, po, key);
			
			// TODO: Remove hard-coded value
			if (test.getValue().getBaseUnitValue() < 0.0){
				isValid = false;
				message = "Value must be greater than 0.0";
			}
		} catch(WrongNumberOfArgumentsException e){
			isValid = false;
			message = "Wrong number of arguments. "+e.getMessage();
		} catch(IncorrectUnitsException e){
			isValid = false;
			message = "Mismatch of units. "+e.getMessage();
		} catch(PropertyNotFoundException e){
			isValid = false;
			message = "Property not found. "+e.getMessage();
		} catch(ObjectNotFoundException e){
			isValid = false;
			message = "Object not found. "+e.getMessage();
		} catch(ObjectUnderspecifiedException e){
			isValid = false;
			message = "Multiple object with name. "+e.getMessage();
		} catch(NumberFormatException e){			
			isValid = false;
			message = "Number format error. "+e.getMessage();
		}
		
		return new VerificationResult(isValid, message);
	}
	
	public static VerificationResult verifyLength(String s, PropertyOwner po, PropertyKey key){
		
		boolean isValid = true;
		String message = "";
		EquationProperty test;
				
		try{
			test = new EquationProperty(s, po, key);
			if(!test.getValue().getUnit().isMatchOrUnitless(BaseUnit.METRE)){
				isValid = false;
			}
			
		} catch(WrongNumberOfArgumentsException e){
			isValid = false;
			message = "Wrong number of arguments. "+e.getMessage();
		} catch(IncorrectUnitsException e){
			isValid = false;
			message = "Mismatch of units. "+e.getMessage();
		} catch(PropertyNotFoundException e){
			isValid = false;
			message = "Property not found. "+e.getMessage();
		} catch(ObjectNotFoundException e){
			isValid = false;
			message = "Object not found. "+e.getMessage();
		} catch(ObjectUnderspecifiedException e){
			isValid = false;
			message = "Multiple object with name. "+e.getMessage();
		} catch(NumberFormatException e){			
			isValid = false;
			message = "Number format error. "+e.getMessage();
		}
		
		return new VerificationResult(isValid, message);
	}
	
	public static VerificationResult verifyAngle(String s, PropertyOwner po, PropertyKey key){
		
		boolean isValid = true;
		String message = "";
		EquationProperty test;
		
		try{
			test = new EquationProperty(s, po, key);
			
			if(!test.getValue().getUnit().isMatchOrUnitless(BaseUnit.RADIAN)){
				isValid = false;
			}
			
			// TODO: Remove hard-coded value
			if (test.getValue().getBaseUnitValue() < -2*Math.PI){
				isValid = false;
				message = "Value must be greater than -360.0";
			} else if (test.getValue().getBaseUnitValue() > 2*Math.PI){
				isValid = false;
				message = "Value must be less than 360.0";
			}
			
			
		} catch(WrongNumberOfArgumentsException e){
			isValid = false;
			message = "Wrong number of arguments. "+e.getMessage();
		} catch(IncorrectUnitsException e){
			isValid = false;
			message = "Mismatch of units. "+e.getMessage();
		} catch(PropertyNotFoundException e){
			isValid = false;
			message = "Property not found. "+e.getMessage();
		} catch(ObjectNotFoundException e){
			isValid = false;
			message = "Object not found. "+e.getMessage();
		} catch(ObjectUnderspecifiedException e){
			isValid = false;
			message = "Multiple object with name. "+e.getMessage();
		} catch(NumberFormatException e){			
			isValid = false;
			message = "Number format error. "+e.getMessage();
		}
		
		return new VerificationResult(isValid, message);
	}
	
	public static VerificationResult verifyPositiveInteger(String s){
		
		boolean isValid = true;
		String message = "";
		IntegerProperty test;
		
		try{
			test = new IntegerProperty(s);	
			
			// TODO: Remove hard-coded value
			if (test.getValue() < 1){
				isValid = false;
				message = "Value must be greater than 1";
			}
		} catch(NumberFormatException e){			
			isValid = false;
			message = "Value is not a valid integer";
		}
		
		return new VerificationResult(isValid, message);
	}
}
