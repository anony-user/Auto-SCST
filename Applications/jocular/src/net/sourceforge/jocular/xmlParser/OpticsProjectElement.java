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

import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.project.OpticsProject;

/**
 * This is the main parser for the top level element opticsProject
 * 
 * @author brymat
 *
 */
public class OpticsProjectElement extends AbstractJclXMLParentElement{
	
	private static String FILE_VERSION_STRING = "version";
	
	private String parsedVersion;
	private OpticsProject project;
	
	private OpticsObjectElement m_groupElement;
	
	public OpticsProjectElement(Attributes attr){
		super("opticsProject", attr);
		
		parsedVersion = parseFileVersion(attr);		
	}
		
	public JclXMLElement getChildElement(String qName, Attributes attr){
		
		if(handler != null)
			return handler.getChildElement(qName, attr);
		
		if (qName.equalsIgnoreCase("ROOT_GROUP")) {
			m_groupElement = new OpticsObjectElement(qName, attr);			
			return m_groupElement;
		}
				
		return null;
	}
	
	@Override
	public boolean endElement(String qName){
		
		if(!super.endElement(qName)){
			return false;
		}
		else{		
			
			buildOpticsProjectObject();			
			return true;
		}
	}
	
	public OpticsProject getProject(){
		
		if(project == null){
			throw new NullPointerException("Project was not created yet");
		}
				
		return project;
	}
	
	private void buildOpticsProjectObject(){
			
		project = new OpticsProject();
		checkFileVersion(project);
		ProjectRootGroup g = (ProjectRootGroup)m_groupElement.getOpticsObject();
		project.addRootGroup(g);
	}	

	private String parseFileVersion(Attributes attr){
		
		int attrLength = attr.getLength();
		
		for(int i=0; i < attrLength; i++)
		{
			if(attr.getQName(i) == FILE_VERSION_STRING){
				return(attr.getValue(i));
			}			
		}
		
		return "";
	}
	
	private void checkFileVersion(OpticsProject project){
		
		//TODO: Should probably do something more serious than a print statement here
		if(!parsedVersion.equals(project.getVersion())){
			System.out.println("Parsed project does not match current version!");
			System.out.println("Parsed project version: " + parsedVersion);
			System.out.println("Current project version: " + project.getVersion());
		}
		else{
			System.out.println("Project version matches at: " + parsedVersion);
		}
	}
	
	
}