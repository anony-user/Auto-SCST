package net.sourceforge.jocular.actions;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.objects.CylindricalSurface;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.PlanoAsphericLens;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.objects.SimpleAperture;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.objects.SphericalSurface;
import net.sourceforge.jocular.objects.TriangularPrism;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;

public class ContextPopup implements OpticsObjectVisitor{


	

	JPopupMenu m_output;
	OpticsObject m_object;
	private final Jocular m_app;
	
	protected ContextPopup(Jocular app, OpticsObject o){
		m_app = app;
		m_object = o;
	}
	protected JPopupMenu getPopup(){
		m_output = new JPopupMenu();
		m_object.accept(this);
		return m_output; 
	}
	public static JPopupMenu getPopup(Jocular app, OpticsObject o){
		return ((new ContextPopup(app, o)).getPopup());
		
	}

	protected void defaultVisit(OpticsObject o){
		m_output.add(OpticsMenuBar.makeAddSubMenu());
		m_output.add(new JMenuItem(OpticsAction.CUT));
		m_output.add(new JMenuItem(OpticsAction.COPY));
		m_output.add(new JMenuItem(OpticsAction.PASTE));
		m_output.add(new JMenuItem(OpticsAction.DELETE_OBJECT));
		m_output.add(new JMenuItem(OpticsAction.MOVE_UP_IN_TREE));
		m_output.add(new JMenuItem(OpticsAction.MOVE_DOWN_IN_TREE));
		m_output.add(new JMenuItem(OpticsAction.SUPPRESS));
	}
	
	@Override
	public void visit(ProjectRootGroup v) {
		m_output.add(OpticsMenuBar.makeAddSubMenu());
		m_output.add(new JMenuItem(OpticsAction.CUT));
		m_output.add(new JMenuItem(OpticsAction.COPY));
		m_output.add(new JMenuItem(OpticsAction.PASTE));
		
	}
	@Override
	public void visit(OpticsPart v) {
//		addAddMenu();
//		m_output.add(new JMenuItem(OpticsAction.CUT));
//		m_output.add(new JMenuItem(OpticsAction.COPY));
//		m_output.add(new JMenuItem(OpticsAction.PASTE));
		defaultVisit(v);
	}
	@Override
	public void visit(TriangularPrism v) {
		defaultVisit((OpticsObject)v);
		
	}
	@Override
	public void visit(RotatedSpline v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.SPLINE_DIALOG);
		
	}
	@Override
	public void visit(ExtrudedSpline v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.SPLINE_DIALOG);
		
	}
	@Override
	public void visit(PlanoAsphericLens v) {
		defaultVisit((OpticsObject)v);
	}
	@Override
	public void visit(Imager v) {
		defaultVisit((OpticsObject)v);
	}

	@Override
	public void visit(SimpleAperture sa) {
		defaultVisit((OpticsObject)sa);
		
	}
	@Override
	public void visit(SphericalSurface v) {
		defaultVisit((OpticsObject)v);
	}

	@Override
	public void visit(SphericalLens v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.OBJECT_INFO);
		
	}
	@Override
	public void visit(SpectroPhotometer v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.OBJECT_INFO);
	}
	@Override
	public void visit(AutofocusSensor v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.AUTOFOCUS);
	}
	@Override
	public void visit(ImageSource v) {
		defaultVisit((OpticsObject)v);
		m_output.add(OpticsAction.DEFINE_IMAGE);
	}

	@Override
	public void visit(HemiPointSource v) {
		defaultVisit((OpticsObject)v);
	}

	@Override
	public void visit(CylindricalSurface v) {
		defaultVisit((OpticsObject)v);
	}

	@Override
	public void visit(OpticsObjectGroup v) {
		defaultVisit((OpticsObject)v);
	}

}
