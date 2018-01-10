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

package net.sourceforge.jocular.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JOptionPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.JocularFileParsingException;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.photons.PhotonWrangler;
import net.sourceforge.jocular.positioners.ObjectPositionerKey;
import net.sourceforge.jocular.project.ProjectUpdatedEvent.UpdateType;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;
import net.sourceforge.jocular.undo.OpticsEdit;
import net.sourceforge.jocular.undo.OpticsMoveEdit;
import net.sourceforge.jocular.undo.OpticsObjectEdit;
import net.sourceforge.jocular.undo.PositionerEdit;
import net.sourceforge.jocular.undo.PropertyEdit;
import net.sourceforge.jocular.xmlParser.JclXmlWriter;
import net.sourceforge.jocular.xmlParser.OpticsProjectXmlParser;

/**
 * This class represents all the data required to design and optical system, save, load it, undo/redo edits for it, etc
 * @author tandk
 *
 */
public class OpticsProject implements TreeModel {
	private final ProjectRootGroup m_group;
	private final PhotonWrangler m_wrangler;
	private String m_fileName = "";
	private final UndoManager m_undoManager = new UndoManager();
	private final Collection<TreeModelListener> m_listeners = new CopyOnWriteArrayList<TreeModelListener>();
	private final String m_version;
	
	
	private boolean m_dirtyFlag;
	
	private Collection<ProjectUpdatedListener> m_ProjectUpdatedListeners = new ArrayList<ProjectUpdatedListener>();
		
	public OpticsProject(){
		m_group = new ProjectRootGroup();
		m_group.addPropertyUpdatedListener(new PropertyUpdatedListener(){
			@Override
			public void propertyUpdated(PropertyUpdatedEvent e) {
				//I'm not sure why I need to do this
				//m_group.updatePositioner(null);
			}
			
		});
		
		//m_group.setProperty(PropertyKey.OUTSIDE_MATERIAL, MaterialKey.VACUUM.name());
		m_wrangler = new PhotonWrangler();
		
		m_version = Jocular.getFormattedVersion();
		
		m_dirtyFlag = false;
	}
	
//	public OpticsProject(OpticsObjectGroup opticsObjectGroup){
//		m_group = opticsObjectGroup;
//		m_group.setProperty(PropertyKey.OUTSIDE_MATERIAL, MaterialKey.VACUUM.name());
//		m_wrangler = new PhotonWrangler(m_group);
//	}
	public String getVersion() {
		return m_version;
	}
	
	public String getFileName() {
		return m_fileName;
	}	
	public void setFileName(String m_fileName) {
		this.m_fileName = m_fileName;
	}
	public PhotonWrangler getWrangler(){
		return m_wrangler;
	}
	public OpticsObjectGroup getOpticsObject(){
		return m_group;
	}
	
	public boolean isDirty(){
		return m_dirtyFlag;
	}
	
	/**
	 * Returns all objects in the group
	 *   Removes any sub groups  
	 * @param includeSuppressedObjects TODO
	 */
	public Collection<OpticsObject> getFlattenedOpticsObjects(boolean includeSuppressedObjects){
		return m_group.getFlattenedOpticsObjects(includeSuppressedObjects);
	}
	
	public void addOpticsObject(OpticsObject optic){
		m_group.addToEnd(optic);
	}
	
	public void addRootGroup(ProjectRootGroup rGroup){
		m_group.copyProperties(rGroup);
		m_group.setPositioner(rGroup.getPositioner().makeCopy());
		for(OpticsObject o : rGroup.getObjects()){
			m_group.addToEnd(o);
		}
	}
//	public OpticsObject getObjectByName(String name){
//		return m_group.getObjectByName(name);
//	}
	public String getUndoPresentationName(){
		return m_undoManager.getUndoPresentationName();
	}
	public String getRedoPresentationName(){
		return m_undoManager.getRedoPresentationName();
	}
	public void undo(){
		if(m_undoManager.canUndo()){
			m_undoManager.undo();
			
			m_dirtyFlag = true;
		}
	}
	public void redo(){
		if(m_undoManager.canRedo()){
			m_undoManager.redo();
			
			m_dirtyFlag = true;
		}
	}
	public void addPropertyEdit(PropertyOwner owner, PropertyKey key, String value){
		addAndDoEdit(new PropertyEdit(this, owner, key,  value));
	}
	public void addOpticsObjectEdit(OpticsObjectGroup group, OpticsObject toAdd, int pos, OpticsObject toRemove){
		addAndDoEdit(new OpticsObjectEdit(group, toAdd, pos,  toRemove, this));	
	}
	public void addPositionerEdit(OpticsObject object, ObjectPositionerKey key){
		addAndDoEdit(new PositionerEdit(object, key, this));
	}
	public void addObjectMoveEdit(OpticsObjectGroup group, OpticsObject object, boolean upNotDown){
		addAndDoEdit(new OpticsMoveEdit(this, group, object, upNotDown));
	}
	public void addAndDoEdit(OpticsEdit u){
		if(u.getProject() != this){
			throw new RuntimeException("Project in Edit does not match this one.");
		}
		u.redo();

		m_undoManager.addEdit(u);
		
		m_dirtyFlag = true;
		fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.CHANGE));
	}
	public boolean canUndo(){
		return m_undoManager.canUndo();
	}
	public boolean canRedo(){
		return m_undoManager.canRedo();
	}
	
	public void save(){
		JclXmlWriter writer = new JclXmlWriter(m_fileName);		
		writer.visit(this);
		
		m_dirtyFlag = false;
		
		fireProjectUpdated(new ProjectUpdatedEvent(UpdateType.SAVE));
	}
	


	
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		m_listeners.add(l);
	}

	@Override
	public Object getChild(Object parent, int index) {
		OpticsObject result = null;
		if(parent instanceof OpticsObjectGroup && !(parent instanceof OpticsPart)){
			OpticsObjectGroup g = (OpticsObjectGroup)parent;
			OpticsObject[] os = g.getObjects().toArray(new OpticsObject[g.getObjects().size()]);
			result = os[index];
		}
		return result;
	}

	@Override
	public int getChildCount(Object parent) {
		int result = 0;
		if(parent instanceof OpticsObjectGroup && !(parent instanceof OpticsPart)){
			OpticsObjectGroup g = (OpticsObjectGroup)parent;
			result = g.getObjects().size();
		}
		return result;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		int result = -1;
		if(parent instanceof OpticsObjectGroup && !(parent instanceof OpticsPart)){
			OpticsObjectGroup g = (OpticsObjectGroup)parent;
			OpticsObject[] os = g.getObjects().toArray(new OpticsObject[g.getObjects().size()]);
			for(int i = 0; i < os.length; ++i){
				if(os[i] == child){
					result = i;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public Object getRoot() {
		return m_group;
	}

	@Override
	public boolean isLeaf(Object node) {
		return !(node instanceof OpticsObjectGroup) || (node instanceof OpticsPart);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		m_listeners.remove(l);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		for(TreeModelListener l : m_listeners){
			l.treeNodesChanged(new TreeModelEvent(newValue, path));
		}

	}
	public enum NodeUpdateType {ADD, REMOVE, CHANGE};
	public void fireNodeUpdate(OpticsObject o, OpticsObjectGroup g, NodeUpdateType type, int index){
		//TODO: this should use a different TreeModelEvent constructor
		//need path to parent
		//array of int specifying indices of removed children
		//array of object specifying added or removed objects
		int[] childIndices = new int[1];
		childIndices[0] = index;
		Object[] os = new Object[1];
		os[0] = o;
		Object[] path = getPath(g);
		TreeModelEvent e = new TreeModelEvent(o,path, childIndices, os);
		for(TreeModelListener l : m_listeners){
			switch(type){
			case ADD:
//				System.out.print("OpticsProject.fireNodeUpdate = ");
//				for(Object oo : path){
//					System.out.print(""+oo.getClass().getSimpleName()+"/");
//				}
//				System.out.println();
				l.treeNodesInserted(e);
				break;
			case REMOVE:
				l.treeNodesRemoved(e);
				break;
			case CHANGE:
				l.treeNodesChanged(e);
				break;
				
			}
		}
		//TODO: remove this when testing the above code
		//fireStructureUpdate();
	}
	public Object[] getPath(OpticsObject o){
		ArrayList<OpticsObject> result = new ArrayList<OpticsObject>();
		getPath(result, o, m_group);
//		System.out.print("OpticsProject.getPath = ");
//		for(OpticsObject oo : result){
//			System.out.print(""+oo.getClass().getSimpleName()+"/");
//		}
//		System.out.println();
		return result.toArray();
		
	}
	protected boolean getPath(ArrayList<OpticsObject> list, OpticsObject o, OpticsObject path){
		boolean result = false;
		if(o == path){
			list.add(0, path);
			result = true;
		} else if(path instanceof OpticsObjectGroup){
			OpticsObjectGroup g = (OpticsObjectGroup)path;
			for(OpticsObject oTest : g.getObjects()){
				if(getPath(list, o, oTest)){
					list.add(0,g);
					result = true;
				}
			}
		}
		return result;
		
	}
	public void fireStructureUpdate(){
//		Object[] path = new Object[1];
//		path[0] = m_group;
//		TreeModelEvent e = new TreeModelEvent(m_group,path);
//		
//		for(TreeModelListener l : m_listeners){
//			l.treeStructureChanged(e);
//		}
	}
	
	public void stopWrangler(){
		m_wrangler.stop();
	}
	
	//Event Listeners
	public void addProjectUpdatedListener(ProjectUpdatedListener listener){
		
		if(!m_ProjectUpdatedListeners.contains(listener))
			m_ProjectUpdatedListeners.add(listener);
	}
	
	public void fireProjectUpdated(ProjectUpdatedEvent type){
		
		for(ProjectUpdatedListener listener : m_ProjectUpdatedListeners){
			listener.projectUpdated(type);
		}
	}

	public static OpticsProject loadProject(File selectedFile) {
		if(selectedFile == null || !selectedFile.exists()){
			return null;
		}
		OpticsProject result;
		try {
			result = OpticsProjectXmlParser.parseProjectFile(selectedFile.getPath());
			
		} catch (IOException | JocularFileParsingException e) {
			
			JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
			return null;
		}
//		PropertyManager.getInstance().deferParsing(false);
//		PropertyManager.getInstance().updateEquations(result, true);
		//PropertyManager.getInstance().updateDeferred();
		return result;
	}

	public File getFile() {
		return new File(getFileName());
	}
	
	
	
}
