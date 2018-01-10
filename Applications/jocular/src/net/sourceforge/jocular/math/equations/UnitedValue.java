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

import java.text.DecimalFormat;

import net.sourceforge.jocular.math.equations.EquationParser.CalcState;



public class UnitedValue {
	public static final UnitedValue NAN = new UnitedValue(BaseUnit.UNITLESS, 0.0);
	public static final UnitedValue ZERO = new UnitedValue(BaseUnit.UNITLESS, 0.0);
	public static final UnitedValue DEFERRED = new UnitedValue(BaseUnit.UNITLESS, 0.0, CalcState.COMPUTATION_DEFERRED);
	private Unit m_unit;
	private double m_value;
	private CalcState m_state;
	private UnitedValue(Unit unit, double value){
		m_unit = unit;
		m_value = value;
		m_state = CalcState.SIMPLE_VALUE;
	}
	private UnitedValue(Unit unit, double value, CalcState state){
		m_unit = unit;
		m_value = value;
		m_state = state;
	}
	public static UnitedValue makeError(CalcState sc){
		//throw new RuntimeException("blah");
		return new UnitedValue(BaseUnit.UNITLESS, 0.0, sc);
	}
	public static UnitedValue makeFunctionResult(Unit unit, double value){
		return new UnitedValue(unit, value, CalcState.COMPUTED_VALUE);
	}
	public static UnitedValue makeSimpleValue(Unit unit, double value){
		return new UnitedValue(unit, value, CalcState.SIMPLE_VALUE);
	}
	public double getValue(){
		return m_value;
	}
	public boolean isError(){
		return (m_state != CalcState.COMPUTED_VALUE && m_state != CalcState.SIMPLE_VALUE && m_state != CalcState.COMPUTATION_DEFERRED);
	}
	public boolean isDeferred(){
		return m_state == CalcState.COMPUTATION_DEFERRED;
	}
	public String getErrorText(){
		return m_state.toString();
	}
	public boolean isSimpleValue(){
		return (m_state == CalcState.SIMPLE_VALUE);
	}
	public Unit getUnit(){
		return m_unit;
	}
	public double getBaseUnitValue(){
		return m_unit.convertToBaseUnitValue(m_value);
	}
	public UnitedValue convertToBaseUnits(){
		BaseUnit[] us = m_unit.getBaseUnits();
		double[] ps = m_unit.getBaseUnitPowers();
		double v = m_unit.convertToBaseUnitValue(m_value);
		Unit u = AbstractUnit.makeUnit(us, ps);
		return new UnitedValue(u,v);
		
	}
	public String toString(){
		String s = "";
		if(m_state != CalcState.COMPUTED_VALUE || m_state != CalcState.SIMPLE_VALUE){
			s += " "+m_state.name();
		}
		return ""+m_value+" "+m_unit+s;
	}
	public String getBaseUnitString() {
		String result;
		if(getUnit().isUnitless()){
			DecimalFormat df = new DecimalFormat("##0.00#E0");
			result = df.format(getBaseUnitValue());
		} else {
			result = getEngineeringUnitString();
		}
		return result;
	}
	public String getEngineeringUnitString() {
		String result;
		DecimalFormat df = new DecimalFormat("##0.00#E0");
		String rs = df.format(getBaseUnitValue());
		String s = rs;
		String exp = s.substring(s.indexOf("E"));
		s = s.substring(0,s.indexOf("E")) + " ";
		switch(exp){
		case "E-15":
			s += "f";
			break;
		case "E-12":
			s += "p";
			break;
		case "E-9":
			s += "n";
			break;
		case "E-6":
			s += "µ";
			break;
		
		case "E-3":
			s += "m";
			break;
		
		case "E0":
			break;
		case "E3":
			s += "k";
			break;
		
		case "E6":
			s += "M";
			break;
		case "E9":
			s += "G";
			break;
		case "E12":
			s += "T";
			break;
		case "E15":
			s += "P";
			break;
		default:
			s += exp;
			break;
		}
	
		for(int i = 0; i < getUnit().getBaseUnitCount(); ++i){
			double p = getUnit().getBaseUnitPower(i);
			
			s += getUnit().getBaseUnit(i).getPreferredShortName();
			if(p != 1.0){
				s += "^"+p;
			}
		}
		return s;
	}
}
