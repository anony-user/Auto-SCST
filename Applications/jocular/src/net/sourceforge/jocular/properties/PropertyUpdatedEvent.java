package net.sourceforge.jocular.properties;


import java.util.EventObject;

import net.sourceforge.jocular.objects.OpticsObject;

public class PropertyUpdatedEvent extends EventObject {
	PropertyKey m_key;
	public PropertyUpdatedEvent(PropertyOwner source, PropertyKey key){
		super(source);
		m_key = key;
	}
	public OpticsObject getOpticsObjectSource(){
		Object o = getSource();
		if(o instanceof OpticsObject){
			return (OpticsObject)getSource();
		} else {
			return null;
		}
	}
	public PropertyOwner getPropertyOwnerSource(){
		return (PropertyOwner)getSource();
	}
	public PropertyKey getPropertyKey(){
		return m_key;
	}
}
