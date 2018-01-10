package net.sourceforge.jocular.properties;
/**
 * @Deprecated replaced by net.sourceforge.njocular.math.equations.Unit
 * @author kmaccallum
 *
 */
public interface Unit {
	/** this is the name that will be used for display */
	public String getDisplayString();
	/** this is the multiplier to convert a number in this unit to the base unit*/
	public double getMultiplier();
	/** this is the name that will be used for parsing*/
	public String getParseString();
}
