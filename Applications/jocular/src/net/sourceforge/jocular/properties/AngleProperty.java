package net.sourceforge.jocular.properties;

/**
 * an angular value of angle measurement. The base unit is radians
 * @Deprecated Use EquationProperty instead
 * @author tandk
 *
 */
public class AngleProperty extends DimensionedProperty {
	public enum LengthUnits implements Unit {DEGREE("°", "deg", Math.PI/180), DEGREE2("°", "°", Math.PI/180), RAD("rad", "rad", 1.0), GRAD("grad", "grad", Math.PI/200);
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
	public AngleProperty(String s){
		super(s, parseDouble(s, LengthUnits.values(), UnitPrefix.values()), LengthUnits.values());
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);

	}

//	@Override
//	public Property<Double> parse(String s) {
//		return new AngleProperty(s);
//	}	
}
