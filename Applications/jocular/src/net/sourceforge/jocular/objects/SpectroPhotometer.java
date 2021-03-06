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

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.settings.Settings;

/**
 * @author kmaccallum
 *
 */
public class SpectroPhotometer extends AbstractOpticsObject implements OutputObject{

	private EquationProperty m_diameter = new EquationProperty("10cm", this, PropertyKey.DIAMETER);
	private EquationProperty m_minWavelength = new EquationProperty("200nm", this, PropertyKey.WAVELENGTH_MIN);
	private EquationProperty m_maxWavelength = new EquationProperty("1000nm", this, PropertyKey.WAVELENGTH_MAX);
	private IntegerProperty m_numBins = new IntegerProperty("100");
	private double[] m_bins;
	/** a geometric progression of wavelengths that define the geometric centres of the m_bins such that m_minWavelength is the bottom of the first bin and m_maxWavelength is the top of the last one*/
	private double[] m_wavelengths;
	private double m_totalEnergy = 0;
	/** geometric constant for the bins*/
	private double m_lnR;
	/** starting value of geometric progression*/
	private double m_lnA0;
	
	public SpectroPhotometer() {
		super();
		clear();
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
			if(dia > m_diameter.getValue().getBaseUnitValue()){
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
		addPhoton(pt.getPhoton());
		//make a new photon just like the incoming one
		synchronized(this){
			pt.addPhoton(new Photon(pt.getPhoton()), pi);
		}
	}

	protected void addPhoton(Photon p){
		//calc what bin it's in
		double w = p.getWavelength();
		
		double deltaE = p.getIntensity()*Settings.HC_CONSTANT/w;
		int i = getBinIndex(w);
		if(i != -1){
			m_bins[i] += deltaE;
		}
		m_totalEnergy += deltaE;
	}
	public void clear(){
		m_totalEnergy = 0;
		m_bins = new double[m_numBins.getValue()];
		m_wavelengths = new double[m_numBins.getValue()];
		m_lnR = Math.log(m_maxWavelength.getValue().getBaseUnitValue()/m_minWavelength.getValue().getBaseUnitValue())/(m_numBins.getValue() - 1);
		m_lnA0 = Math.log(m_minWavelength.getValue().getBaseUnitValue()) - m_lnR/2;
		
		
		for(int i = 0; i < m_bins.length; ++i){
			m_bins[i] = 0;
			m_wavelengths[i] = Math.exp(m_lnA0+m_lnR*(i+0.5));
			
		}
		
	}
	public double[] getBinValues(){
		return m_bins;
	}
	public double[] getBinCentres(){
		return m_wavelengths;
	}
	/**
	 * get index based on wavelength. 
	 * @param w
	 * @return bin index. -1 if outside of range
	 */
	protected int getBinIndex(double w){
		double lnw = Math.log(w);
		double i = Math.floor((lnw - m_lnA0)/m_lnR);
		int result = (int)i;
		if(result >= m_numBins.getValue() || result < 0){
			result = -1;
		}
		return result;
	}
//	public double[][] getHistogram(){
//		
//	}

	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);

	}
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case DIAMETER:
			m_diameter = new EquationProperty(s, this, key);
			break;
		case WAVELENGTH_MIN:
			m_minWavelength = new EquationProperty(s, this, key);
			clear();
			break;
		case WAVELENGTH_MAX:
			m_maxWavelength = new EquationProperty(s, this, key);
			clear();
			break;
		case NUM_BINS:
			m_numBins = new IntegerProperty(s);
			clear();
			break;
		default:
		super.setProperty(key, s);
			break;
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case DIAMETER:
			result = m_diameter;
			break;
		case WAVELENGTH_MIN:
			result = m_minWavelength;
			break;
		case WAVELENGTH_MAX:
			result = m_maxWavelength;
			break;
		case NUM_BINS:
			result = m_numBins;
			break;	
		default:
			result = super.getProperty(key);
			break;
			
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.DIAMETER, PropertyKey.WAVELENGTH_MIN, PropertyKey.WAVELENGTH_MAX, PropertyKey.NUM_BINS));
		return result;
	}
	@Override
	public OpticsObject makeCopy() {
		SpectroPhotometer result = new SpectroPhotometer();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
	@Override
	public void doInternalCalcs(){
		
	}
}
