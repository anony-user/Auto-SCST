package net.sourceforge.jocular.gui.panel3d;

import de.jreality.math.FactoredMatrix;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Camera;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.DoubleArray;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
import net.sourceforge.jocular.math.Vector3D;

/**
 * a jReality tool to allow rotating of the scene with the mouse.
 * @author kenneth
 *
 */
public class RotateViewTool extends AbstractTool {
	private SceneGraphComponent comp;
	private Matrix startMatrix = null;
	private Vector3D startV;
	private Vector3D camStartV;
	private Camera camera = null;
	private Matrix cameraStartMatrix = null;
	private boolean go = false;
	private int iterCount = 0;


	public RotateViewTool(){
		super(InputSlot.LEFT_BUTTON);
		setDescription("Enables panning of the display with right clicking.");
		addCurrentSlot(InputSlot.POINTER_TRANSFORMATION, "drags the view");
	}
	private String daToString(DoubleArray da){
		String s = "";
		for(int n = 0; n < da.getLength(); n++){
			s += da.get(n);
			s += ", ";
		}
		return s;
	}
	private String dsToString(double[] ds){
		String s = "{";
		for(int n = 0; n < ds.length; n++){
			s += ds[n];
			s += ", ";
		}
		s += "}";
		return s;
	}
	@Override
	public void activate(ToolContext tc) {
		super.activate(tc);
		iterCount = 0;
		go = true;
		Viewer v = tc.getViewer();
		SceneGraphPath cp = v.getCameraPath();
		SceneGraphNode sgn = cp.getLastElement();
		comp = cp.getLastComponent();
		cameraStartMatrix = new Matrix(comp.getTransformation());
		startMatrix = new Matrix(tc.getTransformationMatrix(InputSlot.POINTER_TRANSFORMATION));
		camStartV = matToVec(cameraStartMatrix);
		startV = matToVec(startMatrix);
		//System.out.println("OpticsObjectPanel3d.PanTool.activate "+camStartV + startV);
		
	}

	@Override
	public void deactivate(ToolContext tc) {
		super.deactivate(tc);
		System.out.println("RotateViewTool.deactivate. "+iterCount);
		go = false;
	}
	@Override
	public void perform(ToolContext tc) {
		iterCount++;
		SceneGraphPath cp = tc.getViewer().getCameraPath();
		SceneGraphNode sgn = cp.getLastElement();
		comp = cp.getLastComponent();
		FactoredMatrix camM = new FactoredMatrix(comp.getTransformation());
		FactoredMatrix m = new FactoredMatrix(tc.getTransformationMatrix(InputSlot.POINTER_TRANSFORMATION));
		Vector3D camV = matToVec(camM);
		Vector3D v = matToVec(m).subtract(startV);
		Vector3D axis = v.cross(startV).normalize();
		double angle = v.abs()/startV.abs()*-1;
		//v = v.scale(-0.9);
//		Vector3D a = startV.neg();
//		Vector3D b = v.neg();
		Vector3D p1 = Vector3D.ORIGIN;
		Vector3D p2 = p1.add(axis);
		
		System.out.println("OpticsObjectPanel3d.PanTool.perform "+m);
		//comp.getTransformation().setMatrix(MatrixBuilder.euclidean(cameraStartMatrix).rotateFromTo(new double[] {a.x, a.y, a.z, 0.0}, new double[] {b.x, b.y, b.z, 0.0}).getArray());
		if(go){


			MatrixBuilder.euclidean(new Matrix(cameraStartMatrix))

					.translate(new double[] {-camStartV.x, -camStartV.y, -camStartV.z, 0.0})
					.rotate(new double[] {p1.x, p1.y, p1.z, 0.0}, new double[] {p2.x, p2.y, p2.z, 0.0}, angle)


					.translate(new double[] {camStartV.x, camStartV.y, camStartV.z, 0.0})
					.assignTo(comp);


		}
		
	}
	private Vector3D matToVec(Matrix m){
		double[] p = m.getColumn(3);
		return new Vector3D(p[0], p[1], p[2]);
	}

}
