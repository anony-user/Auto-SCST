package net.sourceforge.jocular.properties;

public interface PropertyVisitor {
	public void visit(AngleProperty v);
	public void visit(BooleanProperty v);
	public void visit(EnumProperty v);
	public void visit(ImageProperty v);
	public void visit(IntegerProperty v);
	public void visit(LengthProperty v);
	public void visit(MaterialProperty v);
	public void visit(StringProperty v);
	public void visit(EquationProperty v);
	public void visit(EquationArrayProperty v);
	public void visit(StringArrayProperty v);
	public void visit(EnumArrayProperty v);
	public void visit(FileProperty v);
}
