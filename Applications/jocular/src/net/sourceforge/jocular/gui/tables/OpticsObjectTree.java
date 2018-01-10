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

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.actions.ContextPopup;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.PropertyKey;

@SuppressWarnings("serial")
public class OpticsObjectTree extends JTree {
	private  OpticsProject m_project = new OpticsProject();
	private Jocular m_app;
	public OpticsObjectTree(Jocular app){
		super();
		m_app = app;
		
		disableKeyHandlers();
		
		setCellRenderer(new OpticsObjectTreeCellRenderer());
		//getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setRootVisible(false);
		setShowsRootHandles(true);
		addTreeSelectionListener(new TreeSelectionListener(){

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				//m_project.
				m_app.stopEditing();
				m_project.getOpticsObject().setSelected(false);
				if(e.isAddedPath()){
					OpticsObject oo = (OpticsObject)e.getPath().getLastPathComponent();
					oo.setSelected(!oo.isSelected());
				}
			}
		});
		
		addMouseListener(new MouseListener(){
			private void testForPopup(MouseEvent e){
				if(e.isPopupTrigger()){
					Point p = e.getPoint();
					TreePath tp = getPathForLocation(p.x, p.y);
					
					if(!isPathSelected(tp)){
						setSelectionPath(tp);
					}
					
					Object o;
					if(tp == null){
						o = m_project.getRoot();
					} else {
						o = tp.getLastPathComponent();
					}
					
					if(o instanceof OpticsObject){
						
						JPopupMenu popup = ContextPopup.getPopup(m_app, (OpticsObject)o);
						popup.show(e.getComponent(),
			                       e.getX(), e.getY());
					}
					
					
				}
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				testForPopup(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				testForPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				testForPopup(e);
			}
			
		});
			
	
	}
	
	// These are all handled in the main window
	//  Need to make sure the JTable does not grab the action first.
	private void disableKeyHandlers(){
		
		KeyStroke ks;
		
		ks = KeyStroke.getKeyStroke("control C");
		getInputMap(JComponent.WHEN_FOCUSED ).put(ks, "disableCopy");
		getActionMap().put("disableCopy", null);
		
		ks = KeyStroke.getKeyStroke("control X");
		getInputMap(JComponent.WHEN_FOCUSED ).put(ks, "disableCut");
		getActionMap().put("disableCut", null);
		
		ks = KeyStroke.getKeyStroke("control V");
		getInputMap(JComponent.WHEN_FOCUSED ).put(ks, "disablePaste");
		getActionMap().put("disablePaste", null);
		
		ks = KeyStroke.getKeyStroke("DELETE");
		getInputMap(JComponent.WHEN_FOCUSED ).put(ks, "disableDelete");
		getActionMap().put("disableDelete", null);
		
		ks = KeyStroke.getKeyStroke("F8");
		getInputMap(JComponent.WHEN_FOCUSED ).put(ks, "disableF8");
		getActionMap().put("disableF8", null);
	}
	
	protected class OpticsObjectTreeCellRenderer extends DefaultTreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			String s = "";
			boolean suppressed = false;
			if(value instanceof OpticsObject){
			
				OpticsObject o = (OpticsObject)value;
				s = o.getClass().getSimpleName();
				String n = o.getProperty(PropertyKey.NAME).getDefiningString();
				if(!n.equals("")){
					s = s + " - " + n;
				}
				suppressed = o.isSuppressed();
					
				
			}
			
			Component result = super.getTreeCellRendererComponent(tree, s, selected, expanded, leaf, row, false);
			Font f = result.getFont();
			Map  attributes = f.getAttributes();
			//Map<TextAttribute,?> attributes = f.getAttributes();
			if(suppressed){
				
				attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			} else {
				attributes.put(TextAttribute.STRIKETHROUGH, Boolean.FALSE);
			}
			f = f.deriveFont(attributes);
			result.setFont(f);
			return result;
		}
		
	}

	public void setProject(OpticsProject p){
		m_project = p;
		setModel(p);
		m_project.addTreeModelListener(new TreeModelListener(){

			@Override
			public void treeNodesChanged(TreeModelEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
				//get the path of the added object
				TreePath tp = e.getTreePath().pathByAddingChild(e.getSource());
				setSelectionPath(tp);
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void treeStructureChanged(TreeModelEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
}

