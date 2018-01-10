/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
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

import java.util.ArrayList;

import org.xml.sax.Attributes;

import net.sourceforge.jocular.objects.OpticsID;
import net.sourceforge.jocular.objects.OpticsObject;

public abstract class AbstractJclXmlObjectElement  extends AbstractJclXMLParentElement {
	
	protected final OpticsID id;
	protected PositionerElement m_positionerElement;
	protected ArrayList<JclXmlPropertyElement> m_elements;
	
	public AbstractJclXmlObjectElement(String tag, Attributes attr){
		super(tag, attr);
		
		id = new OpticsID(attr);
			
		m_elements = new ArrayList<JclXmlPropertyElement>();
	}
	
	public OpticsID getID(){
	
		return id;
	}	
	
	public abstract OpticsObject getOpticsObject();

	public JclXMLElement getChildElement(String qName, Attributes attributes){
			
		if(qName.equalsIgnoreCase("property")){
			
			JclXmlPropertyElement sElement = new JclXmlPropertyElement(attributes.getValue(0));
			m_elements.add(sElement);
			return sElement;
		}
		
		if(qName.equalsIgnoreCase("positioner")){
			
			m_positionerElement = new PositionerElement("positioner", attributes);
			return m_positionerElement;
		}
			
		return null;		
	}
	
	public JclXmlPropertyElement getElement(String key){
		
		for(JclXMLElement element : m_elements){			
	
			if(element.getTag().equals(key)){
				return (JclXmlPropertyElement)element;
			}
		}	
		
		return null;
	}
	
	public ArrayList<JclXmlPropertyElement> getElements(){
		return m_elements;
	}
}
