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
package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.input_verification.InputVerificationRules;
import net.sourceforge.jocular.input_verification.VerificationResult;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;

public class SimpleAperture extends AbstractOpticsObject {
	private EquationProperty m_outerDiameter = new EquationProperty(0.01);
	private EquationProperty m_apertureDiameter = new EquationProperty(0.001);
	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);
	}

	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {
		Photon photon = pt.getPhoton();
		Vector3D origin = photon.getOrigin();
		Vector3D direction = photon.getDirection();
		Vector3D pResult;
		Vector3D p2this = getPositioner().getOrigin().subtract(origin);
		if(p2this.dot(direction) <= 0){
			//photon is not traveling towards this surface
			pResult = null;
		} else {
			double normDist = Math.abs(p2this.dot(getPositioner().getDirection()));
			double dotF = Math.abs(direction.dot(getPositioner().getDirection()));
			double dist = normDist/dotF;
			Vector3D pP;
			//check to see that it's within the diameter of the lens
			pP = origin.add(direction.scale(dist));
			double dia = pP.subtract(getPositioner().getOrigin()).abs()*2;
			if(dia > m_outerDiameter.getValue().getBaseUnitValue() || dia < m_apertureDiameter.getValue().getBaseUnitValue()){
				pResult = null;
			} else {
				pResult = pP;
			}
		}
		if(pResult != null){
			is.add(new PhotonInteraction(photon, this, null, pResult, null, false, ""));
		}
	}


	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		pt.absorb(0.0d, this);

	}
	
	@Override
	public VerificationResult trySetProperty(PropertyKey key, String s){
				
		// Only need to verify user entered values, booleans and enums should be safe
		switch(key){
		
		case DIAMETER:
		case OUTER_DIAMETER:			
			return InputVerificationRules.verifyPositiveLength(s, this, key);
			
		default:
			return super.trySetProperty(key, s);
		}
	}

	@Override
	public void setProperty(PropertyKey key, String s) {
				
		switch(key){
		case DIAMETER:
			m_apertureDiameter = new EquationProperty(s, this, key);
			break;
		case OUTER_DIAMETER:
			m_outerDiameter = new EquationProperty(s, this, key);
			break;
		default:
			super.setProperty(key, s);
			break;
		}
		
		if(getPropertyKeys().contains(key)){
			firePropertyUpdated(key);
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case DIAMETER:
			result = m_apertureDiameter;
			break;
		case OUTER_DIAMETER:
			result = m_outerDiameter;
			break;
		default:
			result = super.getProperty(key);
			break;
			
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.DIAMETER, PropertyKey.OUTER_DIAMETER));
		return result;
	}
	

	
	@Override
	public OpticsObject makeCopy() {
		SimpleAperture result = new SimpleAperture();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	
}
