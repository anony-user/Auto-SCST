/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.positioners;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.project.OpticsPositionerVisitor;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

public class AxisPositioner extends AbstractObjectPositioner{
	public enum AxisType {X, Y, Z
	}
	
	private EnumProperty m_axisProperty = new EnumProperty(AxisType.Z, AxisType.Z.name());

	
	public AxisPositioner(){
		super();
		update(null);
		
		setProperty(PropertyKey.AXIS,  AxisType.Z.name());
		
		
	}
	
	public AxisPositioner(String s){
		super();
		update(null);
		
		setProperty(PropertyKey.AXIS,  s);
	}
	
	
	
	
	@Override
	public ObjectPositioner makeCopy() {
		ObjectPositioner result = new AxisPositioner();
		result.copyProperties(this);
		return result;
	}

	@Override
	public void setProperty(PropertyKey key, String s) {
		
		switch(key){
		case AXIS:
			m_axisProperty = new EnumProperty(AxisType.X, s);
			break;
		default:
			break;
		}
		firePropertyUpdated(key);
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result;
		switch(key){
		case AXIS:
			result = m_axisProperty;
			break;
		default:
			result = null;
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.AXIS));
		return result;
	}
	

	public void accept(OpticsPositionerVisitor visitor){
		visitor.visit(this);
	}

	@Override
	public Vector3D calcOrigin(ObjectPositioner parentPositioner) {
		Vector3D result;
		switch((AxisType)(m_axisProperty.getValue())){
		case X:
			result = new Vector3D(0,0,0);
			break;
		case Y:
			result = new Vector3D(0,0,0);
			break;
		default:
		case Z:
			result = new Vector3D(0,0,0);
			break;
		
				
		}
		return result;
	}

	@Override
	public Vector3D calcDirection(ObjectPositioner parentPositioner) {
		Vector3D result;
		switch((AxisType)(m_axisProperty.getValue())){
		case X:
			result = new Vector3D(1,0,0);
			break;
		case Y:
			result = new Vector3D(0,1,0);
			break;
		default:
		case Z:
			
			result = new Vector3D(0,0,1);
			break;
		
				
		}
		return result;
	}

	@Override
	public Vector3D calcTransDirection(ObjectPositioner parentPositioner) {
		Vector3D result;
		switch((AxisType)(m_axisProperty.getValue())){
		case X:
			result = new Vector3D(0,0,1);
			break;
		case Y:
			result = new Vector3D(1,0,0);
			break;
		default:
		case Z:
			result = new Vector3D(0,1,0);
			
			break;
		
				
		}
		return result;
	}
	
}
