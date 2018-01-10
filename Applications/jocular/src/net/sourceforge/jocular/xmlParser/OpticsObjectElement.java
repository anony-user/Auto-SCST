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
import java.util.Collection;

import org.xml.sax.Attributes;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsObjectKey;
import net.sourceforge.jocular.properties.PropertyKey;

public class OpticsObjectElement extends AbstractJclXmlObjectElement {
	
	private Collection<OpticsObjectElement> m_objectElements;

	public OpticsObjectElement(String tag, Attributes attr) {
		super(tag, attr);
		
		m_objectElements = new ArrayList<OpticsObjectElement>();
	}
	
	@Override
	public JclXMLElement getChildElement(String qName, Attributes attr){
		
		JclXMLElement child = super.getChildElement(qName, attr); 
		
		if(child == null){
			
			OpticsObjectElement element = new OpticsObjectElement(qName, attr);
			m_objectElements.add(element);
			return element;
		}
		else
			return child;
		
	}

	@Override
	public OpticsObject getOpticsObject() {
		
		OpticsObjectKey objectKey = OpticsObjectKey.getKey(m_tag);
		
		
		if(objectKey != null){
			
			OpticsObject object = objectKey.getNewObject();
			
			object.setID(id);
			object.setPositioner(m_positionerElement.getPositioner());
			
			//TODO: This is a hack for now!  Only groups have optics objects
			for(OpticsObjectElement objectElement : m_objectElements){				
				((OpticsObjectGroup)object).addToEnd(objectElement.getOpticsObject());
			}	
			
			for(JclXmlPropertyElement element : getElements()){
				
				PropertyKey key = PropertyKey.getKey(element.getTag());
				
				if(key != null){
					object.setProperty(key, element.getValue());
				}
			}
			
			return object;
		}
		
		return null;
	}

}
