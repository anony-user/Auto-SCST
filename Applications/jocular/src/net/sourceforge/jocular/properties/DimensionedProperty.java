package net.sourceforge.jocular.properties;
/**
 * @Deprecated Use EquationProperty instead
 * @author kmaccallum
 *
 */
public abstract class DimensionedProperty implements Property<Double> {
	public enum UnitPrefix implements Unit {GIGA("G", "G", 1.0e9), MEGA("M", "M", 1.0e6), KILO("k", "k", 1.0e3), CENTI("c", "c", 1.0e-2), MILLI("m", "m", 1.0e-3), MICRO("μ", "u", 1.0e-6), MICRO2("μ", "μ", 1.0e-6), NANO("n", "n", 1.0e-9), PICO("p", "p", 1.0e-12);
		private final double multiplier;
		private final String displayString;
		private final String parseString;
		UnitPrefix(String ds, String ps, double m){
			multiplier = m;
			displayString = ds;
			parseString = ps;
		}
		
		public String getDisplayString(){
			return displayString;
		}
		public String getParseString(){
			return parseString;
		}
		public double getMultiplier(){
			return multiplier;
		}
		
	}
	
	/** 
	 * the initial value as entered by the users or loaded from the file
	 */
	protected final String m_valueString;
	protected final Unit[] m_units;
	protected final double m_value;
	/** create a new dimensioned value based on an array of Units that will be used for parsing
	 *  this class will be the basis of linear and angular units and maybe more in the future.
	 * @param s
	 */
	protected DimensionedProperty(String s, double v,  Unit[] us){
		if(s != null){
			m_valueString = s.trim();
		} else {
			m_valueString = null;
		}
		m_units = us;
		m_value = v;
	}
	
	protected static double parseDouble(String s, Unit[] us, Unit[] ps) throws NumberFormatException{
		double m = 1.0;
		String temp = s;
		for(Unit u : us){
			if(temp.endsWith(u.getParseString())){
				m *= u.getMultiplier();
				temp = temp.substring(0, temp.length() - u.getParseString().length());
				break;
			}
		}
		

		for(Unit u : UnitPrefix.values()){
			if(temp.endsWith(u.getParseString())){
				m *= u.getMultiplier();
				temp = temp.substring(0, temp.length() - u.getParseString().length());
				break;
			}
		}
		
		double v;
		v = Double.parseDouble(temp);

		return v*m;
	}

	@Override
	public String getDefiningString() {
		// TODO this might want to be reformated but don't bother for now
		return m_valueString;
	}

	@Override
	public Double getValue(){
		return m_value;
	}
}
