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
package net.sourceforge.jocular.math.equations;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.equations.functions.AdditionFunction;
import net.sourceforge.jocular.math.equations.functions.DivisionFunction;
import net.sourceforge.jocular.math.equations.functions.ExponentFunction;
import net.sourceforge.jocular.math.equations.functions.Function;
import net.sourceforge.jocular.math.equations.functions.FunctionList;
import net.sourceforge.jocular.math.equations.functions.IncorrectUnitsException;
import net.sourceforge.jocular.math.equations.functions.MultiplicationFunction;
import net.sourceforge.jocular.math.equations.functions.NegationFunction;
import net.sourceforge.jocular.math.equations.functions.WrongNumberOfArgumentsException;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;
/**
 * Class to parse equations in EquationProperties.
 * handles basic math, units, functions and looking up properties of objects and object positioners
 * @author kenneth
 *
 */
public class EquationParser {
	public enum CalcState {UNKNOWN_STATE, CIRCULAR_REFERENCE, PROPERTY_NOT_NUMBER, SIMPLE_VALUE, COMPUTED_VALUE, OBJECT_NOT_FOUND, PROPERTY_NOT_FOUND, WRONG_NUMBER_ARGUMENTS, MISMATCHED_UNITS, UNDERSPECIFIED_OBJECT, NUMBER_FORMAT_ERROR, COMPUTATION_DEFERRED};
	private FunctionList m_functions = FunctionList.getFunctionList();
	private UnitList m_units = UnitList.getUnitList();
	
	public List<UnitedValue> parse(String s, PropertyOwner po, PropertyManager pm, PropertyKey key){
		
		List<UnitedValue> cuv = parseEquation(s.toLowerCase(), po, pm, key); 

		
		return cuv;
	}
	private List<UnitedValue> parseEquation(String s, PropertyOwner po, PropertyManager pm, PropertyKey key) throws ObjectNotFoundException, PropertyNotFoundException, WrongNumberOfArgumentsException, IncorrectUnitsException, NumberFormatException {
		List<UnitedValue> result = new ArrayList<UnitedValue>();
		
		
		String ts = s.trim();
		
		
		
		//first scan for commas and semis. If any are found the recurse and return the result immediately
		for(int i = 0; i < ts.length(); ++i){
			switch(ts.charAt(i)){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				break;
			case ',':
			case ';':
				result.addAll(parseEquation(ts.substring(0,i),po, pm, key));
				result.addAll(parseEquation(ts.substring(i + 1), po, pm, key));
				break;
			default:
				break;
			}
			if(result.size() > 0){
				return result;
			}
		}
		UnitedValue uv1 = null;
		UnitedValue uv2 = null;
		//now scan for '+' and '-' that are not part of an exponent
		boolean startsWithNeg = false;
		boolean startsWithPos = false;
		for(int i = 0; i < ts.length(); ++i){
			
			char c = ts.charAt(i);
			switch(c){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				
				break;
			case '+':
			case '-':
				if(i == 0){
					if(c == '-'){
						startsWithNeg = true;
					} else {
						startsWithPos = true;
					}
					//uv1 = parseEquation(ts.substring(1), po, pm, key).get(0);
					
				} else if(i < ts.length() - 1){
					//int j = (startsWithNeg || startsWithPos) ? 1 : 0;
					uv1 = parseEquation(ts.substring(0,i), po, pm, key).get(0);
//					if(startsWithNeg){
//						uv1 = (new NegationFunction()).perform(uv1);
//						
//					}
					uv2 = parseEquation(ts.substring(i), po, pm, key).get(0);
					
					result.add((new AdditionFunction()).perform(uv1, uv2));
					
				} else {//there are no addition or subtraction terms except perhaps for the first character
					
					
				}
				break;
			default:
				//if this starts with a number then jump past it
				if(i == 0 || (startsWithNeg && i == 1) || (startsWithPos && i == 1)){
					i = scanThroughNumber(ts, i);
				} 
				break;
				
			}
			 if(i >= ts.length() - 1){
				//if this is the end then check if we started with a neg
				if(startsWithNeg){
					uv1 = parseEquation(ts.substring(1), po, pm, key).get(0);
					uv1 = ((new NegationFunction()).perform(uv1));
					result.add(uv1);
				} else if(startsWithPos){
					uv1 = parseEquation(ts.substring(1), po, pm, key).get(0);
					result.add(uv1);
				}
			}
			if(result.size() > 0){
				return result;
			}
		}
		
		
		//now test for '*'
		for(int i = 0; i < ts.length(); ++i){
			
			char c = ts.charAt(i);
			switch(c){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				break;

			case '*':

				uv1 = parseEquation(ts.substring(0,i), po, pm, key).get(0);
				uv2 = parseEquation(ts.substring(i + 1), po, pm, key).get(0);
				result.add((new MultiplicationFunction()).perform(uv1, uv2));
				
				break;
			default:
				//look for numbers and jump past them if found
				if(i == 0){
					i = scanThroughNumber(ts, i);
				}
				break;
				
			}
			if(result.size() > 0){
				return result;
			}
		}
		String s1;
		String s2;
		//now check for '/'
		for(int i = 0; i < ts.length(); ++i){
			
			char c = ts.charAt(i);
			switch(c){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				break;

			case '/':

				uv1 = parseEquation(ts.substring(0,i), po, pm, key).get(0);
				uv2 = parseEquation(ts.substring(i + 1), po, pm, key).get(0);
				result.add((new DivisionFunction()).perform(uv1, uv2));
				
				break;
						
			default:
				//look for numbers and jump past them if found
				if(i == 0){
					i = scanThroughNumber(ts, i);
				}
				break;
				
			}
			if(result.size() > 0){
				return result;
			}
		}
		
		//now check for '#' '>' and '^'
		for(int i = 0; i < ts.length(); ++i){
					
			char c = ts.charAt(i);
			switch(c){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				break;
			case '^':
				
				uv1 = parseEquation(ts.substring(0,i), po, pm, key).get(0);
				uv2 = parseEquation(ts.substring(i + 1), po, pm, key).get(0);
				result.add((new ExponentFunction()).perform(uv1, uv2));
				break;
			case '#':
				
				s1 = ts.substring(0,i);
				s2 = ts.substring(i + 1);
				result.add(pm.lookupObjectPositionerProperty(s1,s2, po, key));
				break;
			case '>':
				
				s1 = ts.substring(0,i);
				s2 = ts.substring(i + 1);
				result.add(pm.lookupObjectProperty(s1,s2, po, key));
				break;	
			default:
				//look for numbers and jump past them if found
				if(i == 0){
					i = scanThroughNumber(ts, i);
				}
				break;
				
			
			}
			if(result.size() > 0){
				return result;
			}
		}
		//now check for functions
		for(int i = 0; i < ts.length(); ++i){
			
			char c = ts.charAt(i);
			switch(c){
			case '(':
				//skip ahead to closing brace
				int j = findMatchingBracket(ts, i);
				if(j == ts.length() - 1){
					s1 = ts.substring(0, i);
					s2 = ts.substring(i+1,j);
					if(s1.equals("")){
						result.add(parseEquation(s2, po, pm, key).get(0));
					} else {
						result.add(parseFunction(s1, s2, po, pm, key));
					}
				}
				i = j;
				break;
			
			default:
				//look for numbers and jump past them if found
				if(i == 0){
					i = scanThroughNumber(ts, i);
				}
				break;
			}
		}
		//finally parse this as a simple value
	
		if(result.size() == 0){
			result.add(parseSimpleValue(ts));
		}
		return result;
	}
	private enum NumberState {NOTHING, MANTISSA, EXPONENT, EXPONENT_NUM};
	/**
	 * checks the present location for the start of a number. If it is detected then it scans through it to the end
	 * @param ts
	 * @param i
	 * @return
	 */
	private int scanThroughNumber(String ts, int loc) {
		int i = loc;
		int result = -1;
		NumberState state = NumberState.NOTHING;

		
		while(result == -1){
			
			char c = ts.charAt(i);
			
			switch(state){
			case NOTHING:
				if("01234567890".indexOf(c) == -1){
					
					result = i;
				} else {
					state = NumberState.MANTISSA;
				}
				break;
			case MANTISSA:
				if("0123456789.e".indexOf(c) == -1){
					result = i - 1;
				} else {
					if(c == 'e'){
						state = NumberState.EXPONENT;
					}
				}
				break;
			case EXPONENT:
				if("+-0123456789".indexOf(c) == -1){
					result = i - 1;
				} else {
					state = NumberState.EXPONENT_NUM;
				}
				break;
			case EXPONENT_NUM:
				if("0123456789".indexOf(c) == -1){
					result = i - 1;
				}
				break;
			}
			if(result == -1){
			
				++i;
				if(i >= ts.length()){
					result = ts.length() - 1;
				}
			}
				
			
		}
		return result;
	}
	/**
	 * finds matching closing bracket by scanning through a string from location of an open bracket
	 * @param s - the String to scan
	 * @param i - the location of the opening bracket
	 * @return
	 */
	protected static int findMatchingBracket(String s, int i){
		int openingNum = 0;
		int result = -1;
		boolean found = false;
		for(int j = i + 1; j < s.length(); ++j){
			switch(s.charAt(j)){
			case '(':
				++openingNum;
				break;
			case ')':
				if(openingNum > 0){
					--openingNum;
				} else {
					result = j;
					found = true;
				}
				break;
			}
			if(found){
				break;
			}
		}
		if(!found){
			throw new RuntimeException("Matching bracket not found");
		}
		return result;
	}
	/**
	 * parses strings of a function name and a string of comma-separated strings of united values
	 * @param f the name of the function
	 * @param p the parameters of the function
	 * @return
	 */
	protected UnitedValue parseFunction(String f, String p, PropertyOwner po, PropertyManager pm, PropertyKey key){
		//scan string always keeping in top level of brackets
		ArrayList<UnitedValue> uval = new ArrayList<UnitedValue>();
		int lastValueStart = 0;
		for(int i = 0; i < p.length(); ++i){
			switch(p.charAt(i)){
			case '(':
				//skip ahead a bunch because brackets have opened
				i = findMatchingBracket(p,i);
				break;
			case ',':
			case ';':
				uval.addAll(parseEquation(p.substring(lastValueStart,i), po, pm, key));
				lastValueStart = i + 1;
				break;
				
			}
		}
		if(lastValueStart != p.length()){
			uval.addAll(parseEquation(p.substring(lastValueStart), po, pm, key));
		}
		UnitedValue[] uvs = uval.toArray(new UnitedValue[uval.size()]);
		Function func = m_functions.get(f);
		return func.perform(uvs);
	}
	/**
	 * 
	 * @param s - the string to look in
	 * @param chars - a string of chars to look for
	 * @param i - the character index to start
	 * @return - the position of the next char contained in cs or the length of the string if none found
	 */
	protected int scanForFirstOfCharsOrEnd(String s, String chars, int i){
		int result = s.length();
		boolean inNumber = false;
		boolean inExponent = false;
		for(int j = i; j < s.length(); ++j){
			char c = s.charAt(j);
			if(inExponent){
				if("-+".indexOf(c) == -1){
					inExponent = false;
				}
			} else if(inNumber){
				if("0123456789.".indexOf(c) == -1){
					if("eE".indexOf(c) == -1){
						inNumber = false;
					} else {
						inExponent = true;
					}
				}
			} else {
				if("0123456789".indexOf(c) != -1){
					inNumber = true;
				}
			}
			if(c == '('){
				inNumber = false;
				j = findMatchingBracket(s,j);
			} else if(chars.indexOf(c) != -1 && !inNumber){
				result = j;
				break;
			}
		}
		return result;
	}

	/**
	 * assumes that the string contains only one number and possibly units
	 * @param s
	 * @return
	 */
	protected UnitedValue parseSimpleValue(String s) throws NumberFormatException{
		String unitName = null;
		int index = -1;
		int unitCharLength = 0;
		double v = 0;
		Unit u = BaseUnit.UNITLESS;
		for(String k : m_units.getUnitNames()){
			int i = s.indexOf(k);
			if(i != -1){
				if(k.length() > unitCharLength){//choose the longest name that matches
					unitName = k;
					index = i;
					unitCharLength = k.length();
				}
			}
		}
		if(unitName != null){
			try {
				v = Double.parseDouble(s.substring(0,index));
			} catch(NumberFormatException e){
				throw new RuntimeException("NumberFormatException while parsing: \""+s.substring(0,index)+"\"");
			}
			u = m_units.get(unitName);
		} else {
			try {
				v = Double.parseDouble(s);
			} catch(NumberFormatException e){
				return UnitedValue.makeError(CalcState.NUMBER_FORMAT_ERROR);
			}
		}
		return UnitedValue.makeSimpleValue(u,v);
	}
	/**
	 * Closes any brackets that are left open
	 * @param s
	 * @return
	 */
	protected String completeBrackets(String s){
		String ts = s.trim();
		int openBracketCount = 0;
		for(int i = 0; i < ts.length(); ++i){
			char c = ts.charAt(i);
			if(c == '('){
				++openBracketCount;
			} else if(c == ')'){
				--openBracketCount;
			}
		}
		if(openBracketCount > 0){
			for(int j = openBracketCount; j > 0; --j){
				ts = ts + ")";
			}
		}
		return ts;
	}


	

	private void test(String s, double shouldBeValue, PropertyOwner po, PropertyManager pm, PropertyKey key){
		UnitedValue uv = null;
		try {
			uv = parse(completeBrackets(s), po, pm, key).get(0);
		} catch(RuntimeException e){
			System.out.println("EquationParser.test: Exception: "+e);
		}
		
		if(uv != null){
			
			String result = "Testing: "+s+" = "+uv+" which is "+uv.convertToBaseUnits();
			if(uv.getBaseUnitValue() == shouldBeValue){
				result += " CORRECT";
			} else {
				result += " WRONG!!!!!!!!!!!!!!";
			}
			System.out.println(result);
		}
	}
	static public String[] splitString(String s){
		ArrayList<String> result = new ArrayList<String>();
		String ts = s.trim();
		//first scan for commas and semis. If any are found the recurse and return the result immediately
		int j = 0;
		for(int i = 0; i < ts.length(); ++i){
			switch(ts.charAt(i)){
			case '(':
				//skip ahead to closing brace
				i = findMatchingBracket(ts, i);
				break;
			case ',':
			case ';':
				//result.addAll(parseEquation(ts.substring(0,i),po, pm, key));
				result.add(ts.substring(j,i));
				j = i+1;
				break;
			default:
				break;
			}
			
		}
		result.add(ts.substring(j));
		return result.toArray(new String[result.size()]);
	}
	public static void main(String[] args) {
		EquationParser ep = new EquationParser();
		PropertyManager pm = PropertyManager.getInstance();
		OpticsObject po = new SphericalLens();
		
		OpticsProject proj = new OpticsProject();
		pm.addProject(proj);
		proj.getOpticsObject().add(po, 0);
		po.setProperty(PropertyKey.NAME, "lens1");
		PropertyKey key = PropertyKey.LENGTH;
		po.setProperty(PropertyKey.THICKNESS, "10mm");
//		ep.test("1.0e-3", po, pm, key);
//		ep.test("1.0e-3mm", po, pm, key);
//		ep.test("sqrt(10mm/sin(0.5rad*pi()))", po, pm, key);
//		ep.test("k()", po, pm, key);
//		ep.test("cos(pi()/2)", po, pm, key);
//		ep.test("4*8+6/2", po, pm, key);
//		ep.test("4*(8+6/2)", po, pm, key);
//		ep.test("4*(8 + 6)/2", po, pm, key);
//		ep.test("(4*( (8 + (((6m)))))/(2))", po, pm, key);
//		ep.test("4mm+10ft*sin(30deg + pi()*1rad)", po, pm, key);
//		ep.test("4*( -8 + +6 m)/2", po, pm, key);
//		ep.test("(4*( (-8 + (((+6 m)))))/(2", po, pm, key);
//		ep.test("4\"/8 + 5m/6", po, pm, key);
//		ep.test("3.234in", po, pm, key);
//		ep.test("pi()^2", po, pm, key);
//		ep.test("1mm^2", po, pm, key);
//		ep.test("2^0.5", po, pm, key);
//		ep.test("2^(1+2)", po, pm, key);
		ep.test("4*lens1>thick", 40e-3,  po, pm, key);
		ep.test("4*lens1#ortho", Double.NaN, po, pm, key);
		ep.test("100mm", 100e-3, po, pm, key);
		ep.test("-8.3E-01", -8.3e-1, po, pm, key);
		ep.test(" (-(4+6)/2 + 4)+1/2", -0.5, po, pm, key);
		ep.test(" - 5 + 8 + 7", 10.0, po, pm, key);
		ep.test("-(4+6) - 5 + 8 + 7", 0.0, po, pm, key);
		ep.test("-pi() + 2", 2.0 - Math.PI, po, pm, key);
		ep.test("-6 + 2", -4.0, po, pm, key);
		ep.test("-2.646768761769558E-4", -2.646768761769558E-4, po, pm, key);
		ep.test("tan(30deg)", Math.tan(Math.toRadians(30)),po, pm, key);
	}
}
