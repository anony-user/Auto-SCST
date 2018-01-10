package net.sourceforge.jocular.properties;

import java.util.List;

import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.objects.OpticsObject;

public interface PropertyOwner {
	
	/**
	 * Checks if string s would result in a valid property 
	 * @param key
	 * @param s
	 * @return the result of trying to set selected property
	 */
	public @Deprecated VerificationResult trySetProperty(PropertyKey key, String s);
	
	/**
	 * sets a property of this object. If the key is for a property that this object doesn't know about then it is expected to ignore it quietly
	 * @param key
	 * @param s
	 */
	public void setProperty(PropertyKey key, String s);
	/**
	 * Gets the property of this object according to the supplied key. If this object does not know about the key then is should return null
	 * @param key
	 * @return
	 */
	public Property<?> getProperty(PropertyKey key);
	/**
	 * 
	 * @return a list of all the possible keys that this object knows about
	 */
	public List<PropertyKey> getPropertyKeys();
	/**
	 * 
	 * @return a list of all the possible properties that this object knows about
	 */
	@Deprecated public List<Property<?>> getProperties();
	/**copys a property from the specified owner
	 * 
	 */
	public void copyProperty(PropertyOwner owner, PropertyKey k);
	/**
	 * copy all properties from the specified PropertyOwner.
	 * @param o
	 */
	public void copyProperties(PropertyOwner o);
	/**
	 * add a listener that will be notified if any of this owners properties change
	 * @param listener
	 */
	public void addPropertyUpdatedListener(PropertyUpdatedListener listener);
	public void removePropertyUpdatedListener(PropertyUpdatedListener listener);
//	/**
//	 * cause this owner to recalculate all of it's properties that are EquationProperties
//	 * @returns true if all EquationProperties were able to be calculated and did not have a Value of NaN.
//	 */
//	public boolean calcEquations();
//	/**
//	 * cause this owner to recalc a specific equation. Should just quietly ignore if key does not refer to an equation property
//	 * @param key
//	 * @returns true if EquationProperties was able to be calculated and did not have a Value of NaN.
//	 */
//	public boolean calcEquation(PropertyKey key);
	/**
	 * Inform all property changed listeners that a property has changed
	 * @param key TODO
	 */
	public void firePropertyUpdated(PropertyKey key);
	/**
	 * Checks if this owner is either equal to the specified project or the positioner of the specified object
	 * @param o
	 * @return
	 */
	public boolean isSame(OpticsObject o);
	/**
	 * force property owner to update its internal state. This is inteneded for those objects that generate their state from properties
	 * but may not have noticed that their equation properties have updated their values
	 */
	public void doInternalCalcs();
	
}
