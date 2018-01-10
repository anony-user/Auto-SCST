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
package net.sourceforge.jocular.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.OpticsProject.NodeUpdateType;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedEvent.UpdateType;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;

@SuppressWarnings("serial")
public class PropertyEdit extends OpticsEdit {
	private final PropertyKey m_key;
	private final PropertyOwner m_owner;
	private final String m_value;
	private final String m_oldValue;
	public PropertyEdit(OpticsProject p, PropertyOwner o, PropertyKey k, String v){
		super(p);
		m_key = k;
		m_value = v;
		m_owner = o;
		m_oldValue = m_owner.getProperty(m_key).getDefiningString();
		
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		m_owner.setProperty(m_key, m_value);
		if(m_owner instanceof OpticsObject){
			OpticsObject o = (OpticsObject)m_owner;
			Object[] path = getProject().getPath(o);
			Object og = path[path.length - 2];
			OpticsObjectGroup g = (OpticsObjectGroup)og;
			getProject().fireNodeUpdate(o, g, NodeUpdateType.CHANGE, g.getPos(o));
		}
		//getProject().fireNodeInsertedOrDeletedUpdate(o, g, addNotRemove, index);
		m_project.fireProjectUpdated(getUpdateType());
		
		//m_owner.calcEquation(m_key);
		PropertyManager.getInstance().updateEquation(m_owner, m_key, false);
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		m_owner.setProperty(m_key, m_oldValue);
		//m_owner.calcEquation(m_key);
		PropertyManager.getInstance().updateEquation(m_owner, m_key, false);
		//getProject().fireStructureUpdate();
		
		m_project.fireProjectUpdated(getUpdateType());
		//m_owner.calcEquation(m_key);
		PropertyManager.getInstance().updateEquation(m_owner, m_key, false);
	}

	private ProjectUpdatedEvent getUpdateType(){
		if(m_owner instanceof OpticsObject){
			OpticsObject o = (OpticsObject)m_owner;
			return new ProjectUpdatedEvent(UpdateType.CHANGE, o);
		} else if(m_owner instanceof ObjectPositioner){			
			return new ProjectUpdatedEvent(UpdateType.POSITION);
		}else{
			return new ProjectUpdatedEvent(UpdateType.NONE);
		}					
	}
}
