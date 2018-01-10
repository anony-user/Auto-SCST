package net.sourceforge.jocular.properties;

public class BooleanProperty implements Property<Boolean> {
	
	private final String m_definingString;
	private final boolean m_value;
	
	public BooleanProperty(String s){
		if(s == null){
			throw new RuntimeException("input is null.");
		}
		m_definingString = s;
		
		if(s.equalsIgnoreCase("true")){
			m_value = true;
		}
		else{
			m_value = false;
		}
			
	}

	@Override
	public String getDefiningString() {
		return m_definingString;
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}

	@Override
	public Boolean getValue() {
		return m_value;
	}
}
