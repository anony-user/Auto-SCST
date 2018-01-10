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

public class BaseUnit extends AbstractUnit implements Unit {
	public static final BaseUnit UNITLESS = new BaseUnit("Unitless", "?", UnitType.NONE);
	public static final BaseUnit METRE = new BaseUnit("Metre", "m", UnitType.LENGTH);
	public static final BaseUnit KILOGRAM = new BaseUnit("Kilogram", "kg", UnitType.MASS);
	public static final BaseUnit SECOND = new BaseUnit("Second", "s", UnitType.TIME);
	public static final BaseUnit RADIAN = new BaseUnit("Radian", "rad", UnitType.ANGLE);
	public static final BaseUnit KELVIN = new BaseUnit("Kelvin", "K", UnitType.TEMPERATURE);
	private static final BaseUnit[] m_baseUnits = {UNITLESS, KILOGRAM, METRE, RADIAN, SECOND, KELVIN};
	private UnitType m_type;


	private BaseUnit(String name, String shortName, UnitType type){

		super(name, shortName, null, 1);
		//m_name = name;
		//m_shortName = shortName;
		m_type = type;
	}
	public static BaseUnit[] getAllBaseUnits(){
		
		return m_baseUnits;
	}
	@Override
	public BaseUnit getBaseUnit(int i) {
		return this;
	}

	@Override
	public double getBaseUnitPower(int i) {
		return 1;
	}

	@Override
	public BaseUnit[] getBaseUnits() {
		BaseUnit[] result = {this};
		return result;
	}
	@Override
	public double[] getBaseUnitPowers() {
		double[] result = {1};
		return result;
	}
	@Override
	public int getBaseUnitCount() {
		return 1;
	}

//	@Override
//	public double getFactor() {
//		return 1;
//	}

	@Override
	public double convertToBaseUnitValue(double v) {
		return v;
	}
	@Override
	public double convertFromBaseUnitValue(double v) {
		return v;
	}
	@Override
	public UnitType getType() {
		return m_type;
	}
//	@Override
//	public String getName() {
//		return m_name;
//	}
//
//	@Override
//	public String getPreferredShortName() {
//		return m_shortName;
//	}

//	@Override
//	public boolean isMatch(Unit u) {
//		boolean result = true;
//		if(isUnitless() || u.isUnitless()){
//			result = true;
//		} else {
//			result = equals(u);
//		}
//		return result;
//	}
//	@Override
//	public String toString(){
//		return "Base unit of "+m_type+": "+getName();
//	}
	@Override
	public boolean isUnitless() {
		return equals(BaseUnit.UNITLESS);
	}
	
	
	
	
}
