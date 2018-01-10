package net.sourceforge.jocular.properties;
/**
 * @Deprecated Use EquationProperty instead
 * @author kmaccallum
 *
 */
public class LengthProperty extends DimensionedProperty {
	public enum LengthUnits implements Unit {METRE("m", "m", 1.0), INCH("in", "in", 25.4e-3), INCH2("in", "\"", 25.4e-3),MIL("mil", "mil", 25.4e-6);
		private final double multiplier;
		private final String displayString;
		private final String parseString;
		LengthUnits(String ds, String ps, double m){
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
	public LengthProperty(String s){
		super(s, parseDouble(s, LengthUnits.values(), UnitPrefix.values()), LengthUnits.values());
	}
	public LengthProperty(double v){
		super(Double.toString(v), v, LengthUnits.values());
	}
	
	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);

	}
}
