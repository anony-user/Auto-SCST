package net.sourceforge.jocular.properties;

public class EnumProperty implements Property<Enum<?>> {
	private final Enum<?> m_enum;
	public EnumProperty(Enum<?> e, String s){
		Enum<?> result = null;
		for(Enum<?> e1 : e.getClass().getEnumConstants()){
			if(e1.name().equals(s)){
				result = e1;
				break;
			}
		}
		if(result == null){
			throw new RuntimeException("EnumProperty: \""+s+"\" could not be found in enum");
		}
		m_enum = result;
	}

	@Override
	public String getDefiningString() {
		return m_enum.name();
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}
	@Override
	public Enum<?> getValue() {
		return m_enum;
	}
	

}
