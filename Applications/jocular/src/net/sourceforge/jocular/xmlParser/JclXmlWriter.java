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

import java.io.IOException;
import java.io.PrintStream;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsObjectKey;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.positioners.ObjectPositionerKey;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.PropertyKey;

public class JclXmlWriter {
	
	private final String XML_STANDARD_HEADING = "<?xml version=\"1.0\"?>";
	
	private int tabLevel = 0;
	
	String fileName;
	private PrintStream m_output;
	
	public JclXmlWriter(){
		m_output = System.out;
	}
	
	public JclXmlWriter(String fileName){
		
		this.fileName = fileName;		
	}

	public void visit(OpticsProject project) {
		
		tabLevel = 0;
		
		try {
			m_output = new PrintStream(fileName);
		} catch (IOException e) {
			System.out.println("Failed to open file " + fileName);
			m_output = null;
		}	

		writeLine(XML_STANDARD_HEADING);		
		writeLine("<opticsProject version=\"" + project.getVersion() + "\">");
		tabLevel++;	
		writeOpticsObject(project.getOpticsObject());
		tabLevel--;
		writeLine("</opticsProject>");
			
		m_output.close();
	}
	


	private void writeOpticsObject(OpticsObject optics){
			
		String tag = OpticsObjectKey.getKeyName(optics);
		
		writeLine("<" + tag + " id=\"" + optics.getID().toString() + "\">");
		tabLevel++;
				
		for(PropertyKey key : optics.getPropertyKeys()){
			
			writeLine(JclXmlPropertyElement.getXmlElement(key.name(), optics.getProperty(key).getDefiningString()));
		}
		
		if(optics instanceof OpticsObjectGroup && !(optics instanceof OpticsPart)){
			for(OpticsObject oo : ((OpticsObjectGroup)optics).getObjects()){
				writeOpticsObject(oo);
			}
		}
		
		writePositioner(optics.getPositioner());
		
		tabLevel--;
		writeLine("</" + tag + ">");
	}
	
	private void writePositioner(ObjectPositioner pos){
		
		String type = ObjectPositionerKey.getKeyName(pos);
		writeLine("<positioner id=\"" + pos.getID().toString() + "\" type=\"" + type + "\">");
		tabLevel++;
				
		for(PropertyKey key : pos.getPropertyKeys()){
			
			writeLine(JclXmlPropertyElement.getXmlElement(key.name(), pos.getProperty(key).getDefiningString()));
		}

		tabLevel--;
		writeLine("</positioner>");
	}
	
	private void writeLine(String s){
		
		insertTabs();				
		m_output.println(s);		
	}
	
	private void insertTabs(){
		
		for(int i=0; i < tabLevel; i++){
			m_output.print("\t");
		}
	}
}
