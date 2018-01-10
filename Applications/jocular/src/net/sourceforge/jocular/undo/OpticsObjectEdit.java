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
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.OpticsProject.NodeUpdateType;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedEvent.UpdateType;
import net.sourceforge.jocular.properties.PropertyManager;

/**
 * An edit for adding or removing an optics object
 * @author kmaccallum
 *
 */
@SuppressWarnings("serial")
public class OpticsObjectEdit extends OpticsEdit {
	private final OpticsObjectGroup m_group;
	private final OpticsObject m_newObject;
	private final OpticsObject m_oldObject;
	private final int m_addPos;
	private final int m_removePos;
	public OpticsObjectEdit(OpticsObjectGroup group, OpticsObject newObject, int pos, OpticsObject oldObject, OpticsProject project){
		super(project);
		OpticsObjectGroup g1 = project.getOpticsObject();
		int count = project.getChildCount(g1);
		if(count != 0){
			g1 = group;
		}

		if(g1 == null){
			throw new RuntimeException("Group cannot be null.");
		}
		m_group = g1;
		m_oldObject = oldObject;
		m_newObject = newObject;
		m_addPos = pos;
		if(m_oldObject != null){
			m_removePos = m_group.getPos(m_oldObject);
		} else {
			m_removePos = -1;
		}
	}
	
	
	
	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		if(m_newObject != null){
			m_group.add(m_newObject, m_addPos);
			getProject().fireNodeUpdate(m_newObject, m_group, NodeUpdateType.ADD, m_addPos);
			getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.ADD, m_newObject));
			PropertyManager.getInstance().updateEquations(m_newObject, false);
			//m_newObject.calcEquations();
		}
		if(m_oldObject != null){
			m_group.remove(m_oldObject);
			//getProject().fireStructureUpdate();
			getProject().fireNodeUpdate(m_oldObject, m_group, NodeUpdateType.REMOVE, m_removePos);
			getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.REMOVE, m_oldObject));
		}
	}
	
	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		if(m_newObject != null){
			m_group.remove(m_newObject);
			//getProject().fireStructureUpdate();
			getProject().fireNodeUpdate(m_newObject, m_group, NodeUpdateType.REMOVE, m_addPos);
			getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.REMOVE, m_newObject));
		}
		if(m_oldObject != null){
			m_group.add(m_oldObject, m_removePos);
			getProject().fireNodeUpdate(m_oldObject, m_group, NodeUpdateType.ADD, m_removePos);
			getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.ADD, m_oldObject));
			//m_oldObject.calcEquations();
			PropertyManager.getInstance().updateEquations(m_oldObject, false);
		}
	}	
}
