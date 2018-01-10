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
package net.sourceforge.jocular.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.OpticsProject.NodeUpdateType;

public class OpticsMoveEdit extends OpticsEdit {
	OpticsProject m_project;
	OpticsObjectGroup m_group;
	OpticsObject m_object;
	boolean m_upNotDown;
	int m_oldLoc;
	int m_newLoc;
	
	public OpticsMoveEdit(OpticsProject p, OpticsObjectGroup g, OpticsObject o, boolean upNotDown){
		super(p);
		m_project = p;
		m_group = g;
		m_object = o;
		m_upNotDown = upNotDown;
		m_oldLoc = m_group.getPos(m_object);
		if(upNotDown){
			if(m_oldLoc > 0){
				m_newLoc = m_oldLoc - 1;
			} else {
				m_newLoc = m_oldLoc;
			}
		} else {
			if(m_oldLoc < m_group.getObjects().size() - 1){
				m_newLoc = m_oldLoc + 1;
			} else {
				m_newLoc = m_oldLoc;
			}
		}
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		if(m_oldLoc  != m_newLoc){
			m_group.remove(m_object);
			m_group.add(m_object, m_newLoc);
			getProject().fireNodeUpdate(m_object, m_group, NodeUpdateType.REMOVE, m_oldLoc);
			getProject().fireNodeUpdate(m_object, m_group, NodeUpdateType.ADD, m_newLoc);
			//getProject().FireProjectUpdated(new ProjectUpdatedEvent(UpdateType.ADD, m_object));
		} else {
			System.out.println("OpticsMoveObject.redo new location equals old location.");
		}
		
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		if(m_oldLoc != m_newLoc){
			m_group.remove(m_object);
			m_group.add(m_object, m_oldLoc);
			getProject().fireNodeUpdate(m_object, m_group, NodeUpdateType.REMOVE, m_newLoc);
			getProject().fireNodeUpdate(m_object, m_group, NodeUpdateType.ADD, m_oldLoc);
			//getProject().FireProjectUpdated(new ProjectUpdatedEvent(UpdateType.ADD, m_object));
		} else {
			System.out.println("OpticsMoveObject.undo old location equals new location.");
		}
		
		
		
	}
	
	
}
