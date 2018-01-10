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

public abstract class AbstractJclXMLParentElement implements JclXMLElement{
	
	protected JclXMLElement handler;
	protected final String m_tag;
	
	public AbstractJclXMLParentElement(String tag, Attributes attributes){
		m_tag = tag;
	}
	
	public String getTag(){
		return m_tag;
	}
	
	
	public void parse(String s){
		if(handler != null)
			handler.parse(s);
	}
	
	public void startElement(String qName, Attributes attributes){
 	
		if(handler != null){
			handler.startElement(qName, attributes);
			return;
		}
		
		handler = getChildElement(qName, attributes);		
	}

	public boolean endElement(String qName) {
		
		if(handler != null){
			
			if(handler.endElement(qName))
				handler = null;
			
			return false;
		}
		else
		{
			return true;
		}	 
	}
	
	public abstract JclXMLElement getChildElement(String qName, Attributes attributes);

}
