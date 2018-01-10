/*******************************************************************************
 * Copyright (c) 2013,Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.xmlParser;

import org.xml.sax.Attributes;

public class JclXmlPropertyElement implements JclXMLElement{
	
	private final String KEY_STRING = "key";
	
	private final String m_tag;
	private String value = "";
	private String key;
	
	public JclXmlPropertyElement(String tag) {	
		
		m_tag = tag;
	}		
	
	public String getTag(){
		return m_tag;
	}
	
	public void setAttributes(Attributes attr){
		key = parseAttribute(attr);
	}
	
	public void parse(String s){
		value = s;
	}
	
	public String getValue(){
		return value;
	}
	
	public String getKey(){
		return key;
	}
	
	public static String getXmlElement(String tag, String value) {
		return "<property key=\"" + tag + "\">" + value + "</property>";
	}	
	
	private String parseAttribute(Attributes attr){
		
		String value = "";
		
		int attrLength = attr.getLength();
		
		for(int i=0; i < attrLength; i++)
		{
			if(attr.getQName(i) == KEY_STRING){
				value = attr.getValue(i);
			}
		}
		
		return value;
	}

	@Override
	public void startElement(String qName, Attributes attributes) {
				
	}

	@Override
	public boolean endElement(String qName) {
		return true; 
	}

	@Override
	public JclXMLElement getChildElement(String qName, Attributes attributes) {
		return null;
	}
}
