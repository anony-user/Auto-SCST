package net.sourceforge.jocular.gui.panel3d;

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
 * a jReality tool to allow panning of the scene.
 * @author kenneth
 *
 */
public class PanTool extends AbstractTool {
	private SceneGraphComponent comp;
	private Matrix startMatrix = null;
	private Vector3D startV;
	private Vector3D camStartV;
	private Camera camera = null;
	private Matrix cameraStartMatrix = null;

	private final InputSlot pointerSlot = InputSlot.getDevice("PointerTransformation");

	public PanTool(){
		super(InputSlot.RIGHT_BUTTON);
		setDescription("Enables panning of the display with right clicking.");
		addCurrentSlot(pointerSlot, "drags the view");
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
		Viewer v = tc.getViewer();
		SceneGraphPath cp = v.getCameraPath();
		SceneGraphNode sgn = cp.getLastElement();
		comp = cp.getLastComponent();
		cameraStartMatrix = new Matrix(comp.getTransformation());
		startMatrix = new Matrix(tc.getTransformationMatrix(pointerSlot));
		camStartV = matToVec(cameraStartMatrix);
		startV = matToVec(startMatrix);
		//System.out.println("OpticsObjectPanel3d.PanTool.activate "+camStartV + startV);
		
	}

	@Override
	public void perform(ToolContext tc) {
		SceneGraphPath cp = tc.getViewer().getCameraPath();
		SceneGraphNode sgn = cp.getLastElement();
		comp = cp.getLastComponent();
		Matrix camM = new Matrix(comp.getTransformation());
		Matrix m = new Matrix(tc.getTransformationMatrix(pointerSlot));
		Vector3D camV = matToVec(camM);
		Vector3D v = matToVec(m).subtract(startV).neg();
		//System.out.println("OpticsObjectPanel3d.PanTool.perform "+camV+v);
		MatrixBuilder.euclidean(cameraStartMatrix)
			.translate(v.x, v.y, v.z)
			.assignTo(comp);
		//comp.getTransformation().setMatrix(m.getArray());
		//comp.getTransformation().setMatrix((MatrixBuilder.euclidean(new Matrix(da)).translate(0.005, 0, 0).getArray()));
	}
	private Vector3D matToVec(Matrix m){
		double[] p = m.getColumn(3);
		return new Vector3D(p[0], p[1], p[2]);
	}

}