/*******************************************************************************
 * Copyright (c) 2015,Kenneth MacCallum
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

public class EnumArrayProperty implements ArrayProperty<Enum<?>[]> {
	public static final String SEPARATOR = ",";
	private final Enum<?>[] m_values;
	private final Enum<?> m_enumClass;
	public EnumArrayProperty(Enum<?> e, String[] ss){
		m_enumClass = e;
		if(ss.length == 1 && ss[0] == ""){
			ss = new String[0];
		}
		m_values = new Enum<?>[ss.length];
		for(int i = 0; i < ss.length; ++i){
			Enum<?> result = null;
			for(Enum<?> e1 : e.getClass().getEnumConstants()){
				if(e1.name().equals(ss[i])){
					result = e1;
					break;
				}
			}
			if(result == null){
				throw new RuntimeException("EnumProperty: \""+ss[i]+"\" could not be found in enum");
			}
			
			m_values[i] = result;
		}
		
	}
	public EnumArrayProperty(Enum<?> e, String s){
		this(e,s.split(SEPARATOR));
	}
	

	public Enum<?>[] getValue(){
		return m_values;
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
		for(Enum<?> e : m_values){
			if(!firstTime){
				result += SEPARATOR;
			} else {
				firstTime = false;
			}
			result += e.name();
			
		}
		return result;
	}
	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}
	@Override
	public int size(){
		return m_values.length;
	}
	@Override
	public String[] getDefiningStrings(){
		String[] result = new String[m_values.length];
		for(int i = 0; i < m_values.length; ++i){
			result[i] = m_values[i].name();
		}
		return result;
	}
	@Override
	public String addRowToDefiningString(int i){
		String[] ss = getDefiningStrings();
		ArrayList<String> sss = new ArrayList<String>();
		for(String s : ss){
			sss.add(s);
		}
		sss.add(i,m_enumClass.getClass().getEnumConstants()[0].name());
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

}
