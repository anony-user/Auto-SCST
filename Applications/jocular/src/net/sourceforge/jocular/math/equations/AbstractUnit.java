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

public abstract class AbstractUnit implements Unit {
	private final String m_name;
	private final String m_shortName;
	private final BaseUnit m_baseUnit[];
	private final double m_power[];
	private final double m_factor;
	/**
	 * create a simple unit that is based on the 1st power of only one base unit
	 * @param name
	 * @param shortName
	 * @param baseUnit
	 * @param factor
	 */
	protected AbstractUnit(String name, String shortName, BaseUnit baseUnit, double factor){
		m_name = name;    
		m_shortName = shortName;
		m_baseUnit = new BaseUnit[1];
		m_baseUnit[0] = baseUnit;
		m_power = new double[1];
		m_power[0] = 1;
		m_factor = factor;   
	}
	protected AbstractUnit(BaseUnit[] baseUnits, double[] powers){
		m_name = "";
		m_shortName = "";
		m_baseUnit = new BaseUnit[baseUnits.length];
		m_power = new double[baseUnits.length];
		for(int i = 0; i < baseUnits.length; ++i){
			m_baseUnit[i] = baseUnits[i];
			m_power[i] = powers[i];
		}
		m_factor = 1;
	}
	public static Unit makeUnit(BaseUnit[] baseUnits, double[] powers){
		if(baseUnits.length == 1 && powers[0] == 1){
			return baseUnits[0];
		} else if( baseUnits.length == 0){
			return BaseUnit.UNITLESS;
		} else {
			return new AbstractUnit(baseUnits, powers){};
		}
	}
	@Override
	public BaseUnit getBaseUnit(int i) {
		return m_baseUnit[i];
	}

	@Override
	public double getBaseUnitPower(int i) {
		return m_power[i];
	}

	@Override
	public int getBaseUnitCount() {
		return m_baseUnit.length;
	}

//	@Override
//	public double getFactor() {
//		return m_factor;
//	}

	@Override
	public double convertToBaseUnitValue(double v) {
		return v*m_factor;
	}
	@Override
	public double convertFromBaseUnitValue(double v) {
		// TODO Auto-generated method stub
		return v/m_factor;
	}
	@Override
	public UnitType getType() {
		if(m_baseUnit.length > 1){
			return Unit.UnitType.MULTIPLE;
		} else {
			return m_baseUnit[0].getType();
		}
	}

	@Override
	public BaseUnit[] getBaseUnits() {
		BaseUnit[] result = new BaseUnit[m_baseUnit.length];
		for(int i = 0; i < m_baseUnit.length; ++i){
			result[i] = m_baseUnit[i];
		}
		return result;
	}
	@Override
	public double[] getBaseUnitPowers() {
		double[] result = new double[m_baseUnit.length];
		for(int i = 0; i < m_baseUnit.length; ++i){
			result[i] = m_power[i];
		}
		return result;
	}
	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public String getPreferredShortName() {
		return m_shortName;
	}
	@Override
	public boolean isMatchOrUnitless(Unit u) {
		boolean result = true;
		if(u.isUnitless() || isUnitless()){
			return true;
		}
		if(getBaseUnitCount() != u.getBaseUnitCount()){
			result = false;
		}
		
		
		for(int i = 0; i < getBaseUnitCount(); ++i){
			boolean found = false;
			for(int j = 0; j < getBaseUnitCount(); ++j){
				if(u.getBaseUnit(j).equals(getBaseUnit(i))){
					if(u.getBaseUnitPower(j) == getBaseUnitPower(i)){
						found = true;
						break;
					}
				}
			}
			if(!found){
				result = false;
			}
		}
		return result;
	}
	@Override
	public String toString(){
		String result = "";
		if(getName().equals("")){
			for(int i = 0; i < getBaseUnitCount(); ++i){
				result += getBaseUnit(i).getPreferredShortName();
				double p = getBaseUnitPower(i);
				if(p != 1.0){
					if(Math.floor(p) == p && !Double.isInfinite(p)){
						result += "^"+(int)p;
					} else {
						result += "^"+getBaseUnitPower(i);
					}
				}
				if(i != getBaseUnitCount() - 1){
					result += "·";
				}
				
			}
			
		} else {
			result += getName();
		}
		//result += ")";
		return result;
	}
	public static Unit combineBaseUnits(Unit u1, Unit u2){
		if(u1.isUnitless()){
			return u2;
		} else if(u2.isUnitless()){
			return u1;
		}
		ArrayList<Double> bups = new ArrayList<Double>();
		ArrayList<BaseUnit> bus = new ArrayList<BaseUnit>();
		for(BaseUnit bu : BaseUnit.getAllBaseUnits()){
			double p = getPowerOfBaseUnit(u1,bu) + getPowerOfBaseUnit(u2,bu);
			if(p != 0){
				bups.add(p);
				bus.add(bu);
			}
		}
		double[] powers = new double[bups.size()];
		for(int i = 0; i < bups.size(); ++i){
			powers[i] = bups.get(i);
		}
		BaseUnit[] baseUnits = bus.toArray(new BaseUnit[bus.size()]);
		return makeUnit(baseUnits, powers);
	}
	public static double getPowerOfBaseUnit(Unit u, BaseUnit bu){
		int j = -1;
		double result = 0;
		for(int i = 0; i < u.getBaseUnitCount(); ++i){
			if(u.getBaseUnit(i).equals(bu)){
				j = i;
				break;
			}
		}
		if(j != -1){
			result = u.getBaseUnitPower(j);
		}
		return result;
	}
	public static Unit scaleBaseUnitPowers(Unit u, double s){
		if(u.isUnitless()){
			return u;
		}
		BaseUnit[] bus = u.getBaseUnits();
		double[] ps = u.getBaseUnitPowers();
		for(int i = 0; i < ps.length; ++i){
			ps[i] *= s;
			
		}
		return makeUnit(bus, ps);
	}
	@Override
	public boolean isUnitless() {
		return false;
	}
	
	
	

}
