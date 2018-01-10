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
package net.sourceforge.jocular.gui.panel3d;

import de.jreality.math.MatrixBuilder;
import de.jreality.scene.SceneGraphComponent;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.properties.PropertyUpdatedEvent;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;


/**
 * This is a wrapper class around the jReality {@link SceneGraphComponent} class that contains
 * an OpticsObject that is used to specify the 3D geometry.  
 * 
 * @author Bryan Matthews
 *
 */
public class OpticsSceneGraphComponent extends SceneGraphComponent implements PropertyUpdatedListener{

	private final OpticsObject m_opticsObject;
	
	private SceneGraphComponent m_geometry;
	private boolean m_isTransparent;
			
	public OpticsSceneGraphComponent(OpticsObject obj){
		super();

		m_opticsObject = obj;	
		updateGeometry();
		m_opticsObject.addPropertyUpdatedListener(this);
		
		
		m_isTransparent = false;
		
	}
	
	public void updateGeometry(){
		
		OpticsObject3DFactory factory = new OpticsObject3DFactory();
		
		removeChild(m_geometry);
		if(m_opticsObject.isSuppressed()){
			return;
		}
		m_geometry = factory.render(m_opticsObject);
		
		// This can happen in the case of opticsGroups or unimplemented objects
		if(m_geometry == null){
			System.out.println("OpticsSceneGraphComponent.updateGeometry geometry is null for "+m_opticsObject.getClass());
			return;
		}
		
		AppearanceFactory appFactory = new AppearanceFactory(m_geometry);
		m_isTransparent = appFactory.setAppearance(m_opticsObject);
		
		applyPositioner();
				
		addChild(m_geometry);
	}

	public OpticsObject getOpticsObject() {
		
		return m_opticsObject;
	}

	@Override
	public void propertyUpdated(PropertyUpdatedEvent e) {
		updateGeometry();
	}
	
	public void updatePosition(){
		applyPositioner();
	}
	
	public void updateAppearance(){
		
		// An imager is the only object that can change regularly.  Other objects
		// change based on positioner events or property updated events.
		if(m_opticsObject instanceof Imager){
			if(!m_opticsObject.isSuppressed()){
				AppearanceFactory appFactory = new AppearanceFactory(m_geometry);
				appFactory.setAppearance(m_opticsObject);
			}
		}
		
	}
	
	
	/**
	 * This method is used as a work-around for transparent objects not rendering
	 *  properly using JOGL.  Transparent objects should be added last to the viewer.
	 * 
	 * @return true if 3D object uses transparency else false
	 */
	public boolean isTransparent(){
		
		return m_isTransparent;
	}
	
	private void applyPositioner(){
		
		// This can happen in the case of opticsGroups or unimplemented objects
		if(m_geometry == null) return;
		
		ObjectPositioner positioner = m_opticsObject.getPositioner();
		
		Vector3D orig = positioner.getOrigin();
		Vector3D trans = positioner.getTransDirection();
		Vector3D ortho = positioner.getOrthoDirection();
		
		double xAngle;
		double yAngle;
		double zAngle;
						
		// TODO: This assumes the main axis is Z
		if(trans.y != 0){
			xAngle = Math.atan2(trans.z,trans.y);
		}else{
			xAngle = 0.0;
		}
		
		if(ortho.x != 0){
			yAngle = Math.atan2(ortho.z,-ortho.x);
		}else{
			yAngle = 0.0;
		}
		
		if(trans.x != 0){
			zAngle = Math.atan2(trans.y,-trans.x);
		}else{
			zAngle = 0.0;
		}
		
		MatrixBuilder.euclidean().translate(orig.x, orig.y, orig.z)
				.rotate(xAngle, 1.0, 0.0, 0.0)
				.rotate(yAngle, 0.0, 1.0, 0.0)
				.rotate(zAngle, 0.0, 0.0, 1.0)
				.assignTo(m_geometry);
	}
}
