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


import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.equations.EquationParser;
import net.sourceforge.jocular.math.equations.UnitedValue;

public class EquationArrayProperty implements ArrayProperty<UnitedValue[]> {
	public static final String SEPARATOR = ",";
	List<UnitedValue> m_unitedValues = new ArrayList<UnitedValue>();
	String[] m_definingStrings;
	public EquationArrayProperty(String s, PropertyOwner po, PropertyKey pk){
		this(EquationParser.splitString(s), po, pk);
		
		
	}
	public EquationArrayProperty(String[] ss, PropertyOwner po, PropertyKey pk){
		m_definingStrings = ss;
		calcValue(po,pk);
	}

	public UnitedValue[] getValue(){
		return m_unitedValues.toArray(new UnitedValue[m_unitedValues.size()]);
	}
	@Override
	public String makeDefiningString(String[] ss) {
		String result = "";
		boolean firstTime = true;
		for(String s : ss){
			if(!firstTime){
				result += SEPARATOR;
			} else {
				firstTime = false;
			}
			result += s;
			
		}
		return result;
	}
	@Override
	public String getDefiningString() {
		String result = "";
		boolean firstTime = true;
		for(String s : m_definingStrings){
			if(!firstTime){
				result += SEPARATOR;
			} else {
				firstTime = false;
			}
			result += s;
			
		}
		return result;
	}
	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}
	public void calcValue(PropertyOwner po, PropertyKey pk){
		
		List<UnitedValue> uvs = PropertyManager.getInstance().parseEquation(getDefiningString(), po, pk);
		m_unitedValues = uvs;
	}
	@Override
	public int size(){
		//return m_definingStrings.length;
		return getValue().length;
	}
	public String[] getDefiningStrings(){
		return m_definingStrings;
	}
	@Override
	public String addRowToDefiningString(int i){
		String[] ss = getDefiningStrings();
		ArrayList<String> sss = new ArrayList<String>();
		for(String s : ss){
			sss.add(s);
		}
		sss.add(i,"0.0");
		String result = "";
		boolean firstTime = true;
		for(String s : sss){
			if(!firstTime){
				result += SEPARATOR;
			} else {
				firstTime = false;
			}
			result += s;
			
		}
		return result;
		
	}
	@Override
	public String removeRowFromDefiningString(int i){
		String[] ss = getDefiningStrings();
		ArrayList<String> sss = new ArrayList<String>();
		for(String s : ss){
			sss.add(s);
		}
		sss.remove(i);
		String result = "";
		boolean firstTime = true;
		for(String s : sss){
			if(!firstTime){
				result += SEPARATOR;
			} else {
				firstTime = false;
			}
			result += s;
			
		}
		return result;
		
	}
	/**
	 * indicates whether the calculation of this property has been deferred
	 * @return
	 */
	public boolean isDeferred(){
		boolean result = false;
		for(UnitedValue v : getValue()){
			if(v.isDeferred()){
				result = true;
			}
		}
		return result;
	}
	public boolean isError(){
		boolean result = false;
		for(UnitedValue v : getValue()){
			if(v.isError()){
				result = true;
			}
		}
		return result;
	}
//	@Override
//	public String getSeparator() {
//		return SEPARATOR;
//	}
//	
}
