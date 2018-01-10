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
package net.sourceforge.jocular.properties;


import net.sourceforge.jocular.math.equations.BaseUnit;
import net.sourceforge.jocular.math.equations.UnitedValue;

public class EquationProperty implements Property<UnitedValue> {
	UnitedValue m_unitedValue;
	String m_definingString;
	public EquationProperty(String s, PropertyOwner po, PropertyKey pk){
		m_definingString = s;
		m_unitedValue = UnitedValue.NAN;
		calcValue(po,pk);
	}
	
	public EquationProperty(double d){
		m_definingString = Double.toString(d);
		m_unitedValue = UnitedValue.makeSimpleValue(BaseUnit.UNITLESS, d);
	}

	public UnitedValue getValue(){
		return m_unitedValue;
	}
	@Override
	public String getDefiningString() {
		return m_definingString;
	}
	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}
	public void calcValue(PropertyOwner po, PropertyKey pk){
		m_unitedValue = PropertyManager.getInstance().parseEquation(m_definingString, po, pk).get(0);
		if(m_unitedValue.isError()){
			System.out.println("EquationProperty.calcValue calculation error \"" + m_definingString + "\""+" for property: "+pk.name());
		}

	}
}
