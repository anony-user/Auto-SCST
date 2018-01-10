/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
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

import java.io.File;

import net.sourceforge.jocular.project.OpticsProject;
/**
 * This will hopefully replace the use of StringProperty for the filename of OpticsParts
 * This is a work in progress
 * @author kenneth
 *
 */
public class FileProperty implements Property<File> {
	private File m_file = null;
	private String m_filename;
	private PropertyOwner m_owner;
	
	public FileProperty(PropertyOwner owner, String s){
		m_owner = owner;
		OpticsProject p = PropertyManager.getInstance().getProject(m_owner);
		if(p == null){
			m_filename = s;
			m_file = null;
		} else {
			m_file = PropertyManager.loadRelative(p.getFile(), s);
			if(m_file != null && m_file.exists()){
				m_filename = null;
			}
		
		}
		
	}
	public FileProperty(PropertyOwner owner, File file){
		m_owner = owner;
		m_file = file;
		m_filename = null;
	}
	public PropertyOwner getOwner(){
		return m_owner;
	}
	@Override
	public String getDefiningString() {
		String result = "";
	
		
		
		result = PropertyManager.getRelativePath(m_owner, getValue());
		
		return result;
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);

	}
	@Override
	public File getValue() {
		if(m_file == null){
			OpticsProject p = PropertyManager.getInstance().getProject(m_owner);
			if(p != null){
				m_file = PropertyManager.loadRelative(p.getFile(), m_filename);
				if(m_file != null && m_file.exists()){
					m_filename = null;
				}
				
			}
		}
		return m_file;
	}
	
}
