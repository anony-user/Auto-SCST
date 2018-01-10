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

public class StringArrayProperty implements ArrayProperty<String[]> {
	private final List<String> m_values = new ArrayList<String>();
	private static final String SEPARATOR = " | ";
	
	public StringArrayProperty(String s){
//		if(s.equals("")){
//			return;
//		}
		String[] ss = split(s);
		for(String ts : ss){
			m_values.add(ts);
		}
	}
	protected String[] split(String s){
		int end = 0;
		int start = 0;
		ArrayList<String> result = new ArrayList<String>();
		while(end != -1) {
			end = s.indexOf(SEPARATOR,start);
			if(end != -1){
				result.add(s.substring(start, end).trim());
				start = end + SEPARATOR.length();
			} else {
				result.add(s.substring(start).trim());
				
			}
			
			
		}
		return result.toArray(new String[result.size()]);
		
		
	}
	public StringArrayProperty(String[] ss){
		if(ss.length == 0){
			return;
		}
		for(String ts : ss){
			m_values.add(ts);
		}

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
		for(String s : m_values){
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
	@Override
	public String[] getValue() {
		return getDefiningStrings();
	}
//	public void add(int i){
//		m_values.add(i,"");
//	}
	@Override
	public int size(){
		return m_values.size();
	}
	
	@Override
	public String[] getDefiningStrings() {
		return m_values.toArray(new String[m_values.size()]);
	}
	@Override
	public String addRowToDefiningString(int i){
		String[] ss = getDefiningStrings();
		ArrayList<String> sss = new ArrayList<String>();
		for(String s : ss){
			sss.add(s);
		}
		sss.add(i,"");
		return (new  StringArrayProperty(sss.toArray(new String[sss.size()]))).getDefiningString();
		
	}
	@Override
	public String removeRowFromDefiningString(int i){
		String[] ss = getValue();
		ArrayList<String> sss = new ArrayList<String>();
		for(String s : ss){
			sss.add(s);
		}
		sss.remove(i);
		return (new  StringArrayProperty(sss.toArray(new String[sss.size()]))).getDefiningString();
		
	}
}
