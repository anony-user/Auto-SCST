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
package net.sourceforge.jocular.gui.tables;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;

public class OpticsObjectTreeModel implements TreeModel {
	private final Collection<TreeModelListener> m_listeners = new CopyOnWriteArrayList<TreeModelListener>();
	private OpticsObjectGroup m_group = null;
	
	public OpticsObjectTreeModel(){
		throw new RuntimeException("This class should not be used anymore.");
	}
	public void setGroup(OpticsObjectGroup group){
		m_group = group;
		fireUpdate();
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
		boolean result = true;
		if(node instanceof OpticsPart){
			result = true;
		} else if(node instanceof OpticsObjectGroup){
			result = false;
		}
		return result;
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
	public void fireUpdate(){
		Object[] path = new Object[1];
		path[0] = m_group;
		for(TreeModelListener l : m_listeners){
			l.treeStructureChanged(new TreeModelEvent(m_group,path));
		}
	}



}
