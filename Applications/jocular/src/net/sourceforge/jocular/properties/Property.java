package net.sourceforge.jocular.properties;

public interface Property<T> {
	
	/**
	 * 
	 * @return the string that this value is based on. This can be used for saving or displaying.
	 */
	public String getDefiningString();
	/**
	 * used to all visitors to do stuff with each type of class. Primarily this will be needed for the UI but it can also be used for saving to a file
	 * @param v
	 */
	public void accept(PropertyVisitor v);
	/**
	 * gets a representation of the value of this property
	 * @return
	 */
	public T getValue();
	
	//public Property<T> makeNewProperty(String s, PropertyManager pm)
;	
}
