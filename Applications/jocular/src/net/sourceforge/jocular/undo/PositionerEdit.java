package net.sourceforge.jocular.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.positioners.ObjectPositionerKey;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.OpticsProject.NodeUpdateType;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedEvent.UpdateType;
import net.sourceforge.jocular.properties.PropertyKey;

@SuppressWarnings("serial")
public class PositionerEdit extends OpticsEdit {
	private final OpticsObject m_object;
	private final ObjectPositioner m_newPos;
	private final ObjectPositioner m_oldPos;
	public PositionerEdit(OpticsObject o, ObjectPositionerKey newPosKey, OpticsProject p){
		super(p);
		m_object = o;
		
		m_oldPos = o.getPositioner();
		m_newPos = newPosKey.getNewPositioner();
		for(PropertyKey k : m_newPos.getPropertyKeys()){
			m_newPos.copyProperty( m_oldPos, k);
		}
	}
	@Override
	public void redo() throws CannotRedoException {
		m_object.setPositioner(m_newPos);
		super.redo();
		Object[] path = getProject().getPath(m_object);
		Object og = path[path.length - 2];
		OpticsObjectGroup g = (OpticsObjectGroup)og;
		getProject().fireNodeUpdate(m_object, g, NodeUpdateType.CHANGE, g.getPos(m_object));
		getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.POSITION, m_object));
	}
	@Override
	public void undo() throws CannotUndoException {
		m_object.setPositioner(m_oldPos);
		super.undo();
		Object[] path = getProject().getPath(m_object);
		Object og = path[path.length - 2];
		OpticsObjectGroup g = (OpticsObjectGroup)og;
		getProject().fireNodeUpdate(m_object, g, NodeUpdateType.CHANGE, g.getPos(m_object));
		getProject().fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.POSITION, m_object));
		//getProject().fireUpdate();
	}
}
