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
import java.security.InvalidParameterException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.sourceforge.jocular.project.OpticsProject;

/**
 *
 * This is the entry point handler for the XML file.  It expects an opticsProject tag
 *  as the main element of the file.
 * 
 * @author brymat
 *
 */
public class OpticsProjectXmlHandler extends DefaultHandler{
			
	OpticsProjectElement handler;
	StringBuilder m_elementData = new StringBuilder();	
	OpticsProject project;

	public void startElement(String uri, String localName,String qName, 
                Attributes attributes) throws SAXException {
 	
		if(handler != null){
			handler.startElement(qName, attributes);
			m_elementData = new StringBuilder();
			return;
		}
		else{
			if (!qName.equalsIgnoreCase("opticsProject")) {
				throw new InvalidParameterException("Top element is not <opticsProject>");
			}
			
			handler = new OpticsProjectElement(attributes);
		}		
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		if(handler != null){
			handler.parse(m_elementData.toString());//parse here instead of in character method
			// If true, we have completed parsing the entire file
			if(handler.endElement(qName)){
				
				project = handler.getProject();
				handler = null;
			}
			
			return;
		}
	}
	/**
	 * This method now only accumulates the data. The data is processed in endElement
	 */
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
 
		String s = new String(ch, start, length);
		m_elementData.append(s);
		
	}
	
	public OpticsProject getProject(){
		
		if(project == null){
			throw new NullPointerException("No file has been parsed!");
		}
		
		return project;
	}
}
