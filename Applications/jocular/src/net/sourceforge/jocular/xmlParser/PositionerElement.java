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

import java.util.ArrayList;

import org.xml.sax.Attributes;

import net.sourceforge.jocular.objects.OpticsID;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.positioners.ObjectPositionerKey;
import net.sourceforge.jocular.properties.PropertyKey;

public class PositionerElement  extends AbstractJclXMLParentElement{
	
	protected final OpticsID id;
	protected final String type;
	protected ArrayList<JclXmlPropertyElement> m_basicElements;
	
	public PositionerElement(String tag, Attributes attr){
		super(tag, attr);
		
		m_basicElements = new ArrayList<JclXmlPropertyElement>();
		
		id = new OpticsID(attr);
		
		type = attr.getValue(1);
	}	
	
	public OpticsID getID(){
	
		return id;
	}
	
	public String getType(){
		return type;
	}
	
	public JclXMLElement getChildElement(String qName, Attributes attr){		
		 
		if(qName.equalsIgnoreCase("property")){
			
			JclXmlPropertyElement element = new JclXmlPropertyElement(attr.getValue(0));
			m_basicElements.add(element);
			return element;			
		}
				
		return null;		
	}
	
	public JclXmlPropertyElement getElement(String key){
		
		for(JclXmlPropertyElement element : m_basicElements){
			if(element.getTag().equals(key)){
				return element;
			}
		}	
		
		return null;
	}
	
	public ArrayList<JclXmlPropertyElement> getElements(){
		return m_basicElements;
	}
	
	public ObjectPositioner getPositioner() {
		
		ObjectPositionerKey positionerKey = ObjectPositionerKey.getKey(type);
		
		if(positionerKey != null){
			
			ObjectPositioner positioner = positionerKey.getNewPositioner();
			
			positioner.setID(id);
						
			for(JclXmlPropertyElement element : getElements()){
				
				PropertyKey key = PropertyKey.getKey(element.getTag());
				
				if(key != null){
					positioner.setProperty(key, element.getValue());
				}
			}
			
			return positioner;
		}
		
		return null;
	}

}
