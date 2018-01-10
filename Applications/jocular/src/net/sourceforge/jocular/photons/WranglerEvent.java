package net.sourceforge.jocular.photons;

import java.util.EventObject;

public class WranglerEvent extends EventObject {
	public enum Type {STARTED, ONGOING, FINISHED};
	private final Type m_type;
	public WranglerEvent(PhotonWrangler source, Type t){
		super(source);
		m_type = t;
	}
	public PhotonWrangler getWrangler(){
		return (PhotonWrangler)getSource();
	}
	public Type getType(){
		return m_type;
	}

}
