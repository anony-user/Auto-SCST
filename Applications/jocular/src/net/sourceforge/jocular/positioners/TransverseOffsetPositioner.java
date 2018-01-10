/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
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

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.project.OpticsPositionerVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
/**
 * A positionr that allows offsets from the reference positoner  along its direction, transversely along its orientation and in the direction orthogonol to its orientation
 * @author tandk
 *
 */
public class TransverseOffsetPositioner extends AbstractObjectPositioner {

	EquationProperty m_dirOffsetValue = new EquationProperty("0", this, PropertyKey.DIR_OFFSET);
	EquationProperty m_transOffsetValue = new EquationProperty("0", this, PropertyKey.TRANS_OFFSET);
	EquationProperty m_orthoOffsetValue = new EquationProperty("0", this, PropertyKey.ORTHO_OFFSET);
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s) {
		switch(key){
		case DIR_OFFSET:
		case TRANS_OFFSET:
		case ORTHO_OFFSET:
			return InputVerificationRules.verifyLength(s, this, key);
		default:
			return super.trySetProperty(key, s);
		}		
	}
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case DIR_OFFSET:
			m_dirOffsetValue = new EquationProperty(s, this, key);
			break;
		case TRANS_OFFSET:
			m_transOffsetValue = new EquationProperty(s, this, key);
			break;
		case ORTHO_OFFSET:
			m_orthoOffsetValue = new EquationProperty(s, this, key);
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
		case DIR_OFFSET:
			result = m_dirOffsetValue;
			break;
		case TRANS_OFFSET:
			result = m_transOffsetValue;
			break;
		case ORTHO_OFFSET:
			result = m_orthoOffsetValue;
			break;
		default:
			result = null;
			break;
		}
		return result;
	}
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.DIR_OFFSET, PropertyKey.TRANS_OFFSET, PropertyKey.ORTHO_OFFSET));
		return result;
	}

	public TransverseOffsetPositioner(){
		m_dirOffsetValue = new EquationProperty(0);
		m_transOffsetValue = new EquationProperty(0);
		m_orthoOffsetValue = new EquationProperty(0);
		//setPositioner(new AxisPositioner());
	}
	@Override
	public ObjectPositioner makeCopy() {
		ObjectPositioner result = new TransverseOffsetPositioner();
		result.copyProperties(this);
		return result;
		
	}
	
	@Override
	public Vector3D calcOrigin(ObjectPositioner parentPositioner) {
		Vector3D dir = parentPositioner.getDirection().scale(m_dirOffsetValue.getValue().getBaseUnitValue());
		Vector3D trans = parentPositioner.getTransDirection().scale(m_transOffsetValue.getValue().getBaseUnitValue());
		Vector3D ortho = parentPositioner.getOrthoDirection().scale(m_orthoOffsetValue.getValue().getBaseUnitValue());
		Vector3D origin = parentPositioner.getOrigin();
		origin = origin.add(dir).add(trans).add(ortho);
		return origin;
	}

	
	

	
	public void accept(OpticsPositionerVisitor visitor){
		visitor.visit(this);
	}
}
