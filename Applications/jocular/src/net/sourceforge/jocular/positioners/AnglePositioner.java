package net.sourceforge.jocular.positioners;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.project.OpticsPositionerVisitor;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;


public class AnglePositioner extends AbstractObjectPositioner {
	private EquationProperty m_angle = new EquationProperty(0);
	private EquationProperty m_dirOffset = new EquationProperty(0);
	public enum Axis {DIR_AXIS("Dir.", new Vector3D(0,0,1)), 
						TRANS_AXIS("Trans.", new Vector3D(0,1,0)),
						ORTHO_AXIS("Ortho.", new Vector3D(1,0,0))
						;
		private String m_name;
		private Vector3D m_vector;
		private Axis(String name, Vector3D v){
			m_name = name;
			m_vector = v;
			
		}
		public Vector3D getVector(){
			return m_vector;
		}
		public String toString(){
			return m_name;
		}
	}
	private EnumProperty m_axis = new EnumProperty(Axis.DIR_AXIS,Axis.DIR_AXIS.name());
	private Vector3D m_v;
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s) {
		switch(key){
		case DIR_OFFSET:
			return InputVerificationRules.verifyLength(s, this, key);
		case ANGLE:
			return InputVerificationRules.verifyAngle(s, this, key);
		default:
			return super.trySetProperty(key, s);
		}		
	}
	

	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case DIR_OFFSET:
			m_dirOffset = new EquationProperty(s, this, key);
			break;
		case ROT_AXIS:
			m_axis = new EnumProperty(Axis.DIR_AXIS, s);
			calcMembers();
			break;
		case ANGLE:
			m_angle = new EquationProperty(s, this, key);
			break;
		default:
			break;
		}
		firePropertyUpdated(key);

	}
	public AnglePositioner(){
		calcMembers();
	}
	private void calcMembers(){
		m_v = ((Axis)(m_axis.getValue())).getVector();
	}
	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case DIR_OFFSET:
			result = m_dirOffset;
			break;
		case ROT_AXIS:
			result = m_axis;
			break;
		case ANGLE:
			result = m_angle;
			break;
		default:
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.ROT_AXIS, PropertyKey.ANGLE, PropertyKey.DIR_OFFSET));
		return result;
	}
	
	
	@Override
	protected Vector3D calcOrigin(ObjectPositioner parentPositioner) {
		return  parentPositioner.getDirection().scale(m_dirOffset.getValue().getBaseUnitValue()).add(parentPositioner.getOrigin());
	}
	@Override
	protected Vector3D calcDirection(ObjectPositioner parentPositioner) {
		Vector3D result = parentPositioner.getDirection();
		result = result.rotate(m_v, m_angle.getValue().getBaseUnitValue());
		return result;
	}

//	@Override
//	protected Vector3D calcOrthoDirection(ObjectPositioner parentPositioner) {
//		Vector3D result = parentPositioner.getOrthoDirection();
//		result = result.rotate(m_v, m_angle.getValue().getBaseUnitValue());
//		return result;
//	}


	@Override
	protected Vector3D calcTransDirection(ObjectPositioner parentPositioner) {
		Vector3D result = parentPositioner.getTransDirection();
		result = result.rotate(m_v, m_angle.getValue().getBaseUnitValue());
		return result;
	}

	@Override
	public ObjectPositioner makeCopy() {
		AnglePositioner result = new AnglePositioner();
		result.copyProperties(this);
		return result;
	}
	@Override
	public void accept(OpticsPositionerVisitor v) {
		v.visit(this);

	}

}
