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

import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.SMOOTH_SHADING;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;

import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.jogl.JOGLViewer;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.scene.proxy.scene.ClippingPlane;
import de.jreality.shader.Color;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.tools.ClickWheelCameraZoomTool;
import de.jreality.tools.EncompassTool;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;
import de.jreality.util.RenderTrigger;
import de.jreality.util.SceneGraphUtility;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.PhotonColour;
import net.sourceforge.jocular.gui.panel2d.OpticsObjectPanel.Plane;
import net.sourceforge.jocular.imager.OpticsColour;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.project.ProjectUpdatedEvent;
import net.sourceforge.jocular.project.ProjectUpdatedListener;
import net.sourceforge.jocular.settings.Settings;

@SuppressWarnings("serial")
public class OpticsObjectPanel3d extends JPanel implements ProjectUpdatedListener{
	
	final double ORIGIN_SIZE = 0.01;
	
	final SceneGraphComponent m_rootNode = new SceneGraphComponent("root");
	final SceneGraphComponent m_worldNode = new SceneGraphComponent("world");
	final SceneGraphComponent m_cameraNode = new SceneGraphComponent("camera");	
	final SceneGraphComponent m_geometryNode = new SceneGraphComponent("geometry");
	Viewer m_viewer;
	private double[] m_clipPlaneNormal = null;
	OpticsProject m_project;
	
	Collection<OpticsSceneGraphComponent> m_opticsComponents = new ArrayList<OpticsSceneGraphComponent>();
	//PhotonRenderer3D m_photonRenderer3d;
	SceneGraphComponent m_photonTrajectoriesComponent = null;
	
	public OpticsObjectPanel3d(){
		
		createRootNode();
		
		m_viewer = new JOGLViewer();
		//m_viewer.
		
		m_viewer.setSceneRoot(m_rootNode);
		
		createCamera();
		
		initializeToolSystem(m_viewer);
		
		RenderTrigger rt = new RenderTrigger();
		rt.addSceneGraphComponent(m_rootNode);
		
		rt.addViewer(m_viewer);
		
		setLayout(new java.awt.BorderLayout());
		
		add((Component) m_viewer.getViewingComponent());
	}

	public void zoomIn(){
		Camera c = m_cameraNode.getCamera();
		double fov = c.getFieldOfView();
		fov /= 1.41;
		c.setFieldOfView(fov);
		System.out.println("OpticsObjectPanel3d.zoomOut fov is "+fov);
		
	}
	public void zoomOut(){
		Camera c = m_cameraNode.getCamera();
		double fov = c.getFieldOfView();
		fov *= 1.41;
		c.setFieldOfView(fov);
		System.out.println("OpticsObjectPanel3d.zoomOut fov is "+fov);
	}

	private void createRootNode(){
		
		
		SceneGraphComponent lightNode = new SceneGraphComponent("light");

		m_rootNode.addChild(m_worldNode);
		m_worldNode.addChild(m_geometryNode);
		m_rootNode.addChild(m_cameraNode);
		m_cameraNode.addChild(lightNode);

		lightNode.setLight(createCameraLight());

		initializeTools();
		
		final Appearance rootApp = new Appearance();
		rootApp.setAttribute(LINE_SHADER+"."+POLYGON_SHADER+"."+SMOOTH_SHADING, true);
		rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.WHITE);
		rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(1f, 0f, 0f));
		
		
//		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(rootApp, false);
//		dgs.setShowLines(true);
//		DefaultLineShader dls = (DefaultLineShader)dgs.createLineShader("default");
//		dls.setTubeDraw(false);
//		dls.setLineWidth(10.0);
//		dls.setDiffuseColor(Color.BLACK);
		
		//tt.activate(m_viewer);
		m_rootNode.setAppearance(rootApp);

	}
	
	private void createCamera(){
		
		Camera camera = new Camera();
		camera.setPerspective(false);
		camera.setNear(.001);
		camera.setFar(100.0);
		camera.setFieldOfView(2);
		
		// This looks down  Z Axis towards the origin
		MatrixBuilder.euclidean().translate(0, 0, 0.5)
			.assignTo(m_cameraNode);
		
		m_cameraNode.setCamera(camera);
		SceneGraphPath camPath = SceneGraphUtility.getPathsBetween(m_rootNode, m_cameraNode).get(0);
		camPath.push(camera);
		
		m_viewer.setCameraPath(camPath);
	}
	
	private void initializeTools(){
			
		RotateTool rotateTool = new RotateTool();
		// This seems to fix an issue when rotating with objects rendered off screen
		//  For example, photons that have missed hitting any objects
		rotateTool.setFixOrigin(true);
		// This disables the automatic spinning after releasing the mouse button
		rotateTool.setAnimTimeMax(0);   
		m_worldNode.addTool(rotateTool);
		
//		DraggingTool draggingTool = new DraggingTool();
//		m_worldNode.addTool(draggingTool);
//		m_rootNode.addTool(new RotateViewTool());
		
		ClickWheelCameraZoomTool zoomTool = new ClickWheelCameraZoomTool();
		m_rootNode.addTool(zoomTool);
		
		//m_worldNode.addTool(new PanTool());
		m_rootNode.addTool(new PanTool());

		EncompassTool encompassTool = new EncompassTool();
		encompassTool.setAutomaticClippingPlanes(false);
		m_geometryNode.addTool(encompassTool);
	}
	
	private void initializeToolSystem(Viewer viewer){
		
		ToolSystem toolSystem = ToolSystem.toolSystemForViewer(viewer);
		toolSystem.initializeSceneTools();		
	}
		
	private Light createCameraLight(){
		DirectionalLight light = new DirectionalLight();
		light.setColor(Color.WHITE);
		light.setIntensity(0.5);
		
		return light;
	}
	
	public void setProject(OpticsProject project){
		
		m_project = project;
		
		project.addProjectUpdatedListener(this);
		
		updateGeometry();
	}
	
	private void updateGeometry(){
		System.out.println("OpticsObjectPanel3d.updateGeometry.");
		Collection<OpticsObject> optics = m_project.getFlattenedOpticsObjects(true);
		//m_photonRenderer3d = new PhotonRenderer3D(m_project.getWrangler());
		
		removeComponents();
		SceneGraphComponent sgc = new SceneGraphComponent("Optics");
		renderOptics(sgc, optics);
		
		if(m_clipPlaneNormal != null){
			ClippingPlane cp = new ClippingPlane();
			cp.setPlane(m_clipPlaneNormal);
			cp.setLocal(true);
			
			sgc.setGeometry(cp);
		} else {
			sgc.setGeometry(null);
		}
		m_geometryNode.addChild(sgc);
		
		//m_geometryNode.addChild(m_photonRenderer3d);
		m_geometryNode.addChild(OpticsObjects3D.createOrigin(ORIGIN_SIZE));
		
	}
	
	private void removeComponents(){
		
		
		m_geometryNode.removeAllChildren();
		m_opticsComponents.clear();
//		for(SceneGraphComponent sgc : m_geometryNode.getChildComponents()){
//			m_geometryNode.removeChild(sgc);
//		}
			
	}
	
	private void renderOptics(SceneGraphComponent sgc, Collection<OpticsObject> optics){
				
		for(OpticsObject obj : optics){
			
			OpticsSceneGraphComponent result = new OpticsSceneGraphComponent(obj); 
			result.updateGeometry();
			
			if(result != null){				
				m_opticsComponents.add(result);
			}
		}
		
		// Need a dual pass adding here.  This ensures that all transparent objects
		//  are at the end of the geometry node's list.  This is a work-around
		//  for the JOGL backend.		
		for(OpticsSceneGraphComponent osgc : m_opticsComponents){
			
			if(!osgc.isTransparent()){
				sgc.addChild(osgc);
			}
		}
		
		for(OpticsSceneGraphComponent osgc : m_opticsComponents){
			
			if(osgc.isTransparent()){
				sgc.addChild(osgc);
			}
		}
			
	}
	public void updatePhotons(){
	
		if(m_photonTrajectoriesComponent != null){
			m_geometryNode.removeChild(m_photonTrajectoriesComponent);
		}
		m_photonTrajectoriesComponent = new SceneGraphComponent();
		
		Collection<PhotonTrajectory> pts = m_project.getWrangler().getTrajectories();
		PhotonColour pc = Settings.SETTINGS.getPhotonColourScheme();
		for(PhotonTrajectory pt : pts){
			IndexedLineSetFactory ilsf = new IndexedLineSetFactory();
			int n = pt.getNumberOfPhotons();
			double[] vertices = new double[3 * (n + 1)];
			double[] edgeColor = new double[n*3];
			int[] edgeIndices = new int[2 * n];
			//add photons
			for(int i = 0; i < n; i++){
				Photon p = pt.getPhoton(i);
				Vector3D v = p.getOrigin();
				vertices[i*3 + 0] = v.x;
				vertices[i*3 + 1] = v.y;
				vertices[i*3 + 2] = v.z;
				edgeIndices[i*2 + 0] = i;
				edgeIndices[i*2 + 1] = i + 1;
				OpticsColour c = OpticsColour.getColour(pt, i, pc);
				edgeColor[i*3 + 0] = c.getRed();
				edgeColor[i*3 + 1] = c.getGreen();
				edgeColor[i*3 + 2] = c.getBlue();
			}
			Photon p = pt.getPhoton();
			Vector3D v = p.getOrigin().add(p.getDirection().scale(0));
			vertices[n*3 + 0] = v.x;
			vertices[n*3 + 1] = v.y;
			vertices[n*3 + 2] = v.z;
			
			ilsf.setVertexCount(n + 1);
			ilsf.setVertexCoordinates(vertices);
			ilsf.setEdgeCount(n);
			ilsf.setEdgeIndices(edgeIndices);
			ilsf.setEdgeColors(edgeColor);
			ilsf.update();
			
			//m_photonTrajectoriesComponent.setGeometry(ilsf.getIndexedLineSet());
			
			
			
			SceneGraphComponent sgc = new SceneGraphComponent();
			sgc.setGeometry(ilsf.getIndexedLineSet());
			
			Appearance a = new Appearance();
			DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(a, true);
			DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
			dls.setLineLighting(false);
			dls.setLineWidth(2.0);
			dls.setTubeDraw(false);
			DefaultPointShader dps = (DefaultPointShader)dgs.createPointShader("default");
			dps.setSpheresDraw(false);
			dps.setPointRadius(1e-3);
			a.setAttribute(CommonAttributes.VERTEX_DRAW, false);
			sgc.setAppearance(a);
			m_photonTrajectoriesComponent.addChild(sgc);
		}
//		Appearance a = m_photonTrajectoriesComponent.getAppearance();
//		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(a, true);
//		//dgs.setShowLines(true);
//		DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
//		dls.setLineLighting(false);
//		dls.setLineWidth(2.0);
//		dls.setTubeDraw(false);
		m_geometryNode.addChild(m_photonTrajectoriesComponent);
	}
	public void updateDisplay(){
		
		
		for(OpticsSceneGraphComponent osgc : m_opticsComponents){
			
			osgc.updateAppearance();
		}
		
		updatePhotons();

		//System.out.println("OpticsObjectPanel3d.updateDisplay: Trajectory count = "+m_photonRenderer3d.getChildComponentCount());
	}
	public void updateEverything(){
		updateGeometry();
		updateDisplay();
	}
	
	private void updateOpticsPosition(){
		
		for(OpticsSceneGraphComponent osgc : m_opticsComponents){
		
			osgc.updatePosition();
		}
	}

	@Override
	public void projectUpdated(ProjectUpdatedEvent e) {
		
		switch(e.getType()){
		
		case ADD:
			OpticsSceneGraphComponent result = new OpticsSceneGraphComponent(e.getOpticsObject()); 
			result.updateGeometry();
			
			if(result != null){
				m_geometryNode.addChild(result);
				m_opticsComponents.add(result);
			}
			break;
			
		case REMOVE:
			
			OpticsSceneGraphComponent match = null;
			
			for(OpticsSceneGraphComponent osgc : m_opticsComponents){
				
				if(osgc.getOpticsObject() == e.getOpticsObject()){
					match = osgc;
				}
			}
			
			if(match != null){
				m_geometryNode.removeChild(match);
				m_opticsComponents.remove(match);
			}
			
			break;
				
		case POSITION:
			// Position change could affect all objects, so need to update them
			updateOpticsPosition();
			break;
			
		// We can safely ignore any other changes
		default:
			
			break;
		}
	}
	
	
	// Camera is set to look down the Z-Axis
	//  Need to rotate world component to display selected plane
	public void setViewPlane(OpticsObjectPanel.Plane plane){

		Matrix m = MatrixBuilder.euclidean().translate(0, 0, 0).getMatrix();
		
		switch(plane){
		
		case XY_PLANE:
			m = MatrixBuilder.euclidean().translate(0, 0, 0)
					//.rotateZ(Math.PI/2)
					//.rotateX(Math.PI/2)
					.getMatrix();
			break;
		
		case ZY_PLANE:
			m = MatrixBuilder.euclidean().translate(0, 0, 0)
					.rotateY(Math.PI/2)
					.getMatrix();
			break;

		case ZX_PLANE:
			m = MatrixBuilder.euclidean().translate(0, 0, 0)
					.rotateY(Math.PI/2)
					.rotateZ(Math.PI/2)
					.getMatrix();
			break;
		
		default:
			break;
		}
		
		//TODO: This should only set the rotation not reset the translation
		//TODO: I believe we could get the translation, remove it with the inverse, 
		//TODO: apply the rotation, then reapply the translation
		m_worldNode.setTransformation(new Transformation(m.getArray()));
	}
	public void setClipPLane(Plane plane) {
		double[] normal;
		
		switch(plane){
		
		case XY_PLANE:
			normal = new double[] {0 ,0 ,1};
			break;
		
		case ZY_PLANE:
			normal = new double[] {1 ,0 ,0};
			break;

		case ZX_PLANE:
			normal = new double[] {0 ,-1 ,0};
			break;
		
		default:
		case NONE:
			normal = null;
			break;
		}
		m_clipPlaneNormal = normal;
		updateGeometry();
		
	}
	/**
	 * method to test stuff if required
	 */
	public void test(){
		double[] da = m_cameraNode.getTransformation().getMatrix();
		
		m_cameraNode.getTransformation().setMatrix((MatrixBuilder.euclidean(new Matrix(da)).translate(0.005, 0, 0).getArray()));
		
	}

	
}
