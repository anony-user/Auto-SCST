/*******************************************************************************
 * Copyright (c) 2014, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.autofocus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.math.CalcCompleteEvent;
import net.sourceforge.jocular.math.CalcCompleteListener;
import net.sourceforge.jocular.math.SystemToSolve;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.photons.WranglerEvent;
import net.sourceforge.jocular.photons.WranglerListener;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.sources.ImageSource;

public class AutofocusSystemToSolve implements SystemToSolve, WranglerListener {
	private  int m_numPhotonsPerCalc = 1000;
	private final AutofocusParameterTableModel m_model;
	private List<CalcCompleteListener> m_listeners = new CopyOnWriteArrayList<CalcCompleteListener>();
	
	
	public AutofocusSystemToSolve(AutofocusParameterTableModel m_model, int numPhotonsPerCalc) {
		super();
		this.m_model = m_model;
		m_numPhotonsPerCalc = numPhotonsPerCalc;
	}
	@Override
	public double getErrorValue() {
		return m_model.getSensor().getError();
	}
	
	protected PropertyKey getPropertyKey(PropertyOwner po, int i){
		return PropertyManager.getInstance().getPropertyKey(po, m_model.getPropertyName(i));
	}
	@Override
	public double getParameter(int i) {
		double result;
		PropertyOwner po = m_model.getPropertyOwner(i);
		Property p = po.getProperty(getPropertyKey(po, i));
		if(p instanceof EquationProperty){
			result = ((EquationProperty)p).getValue().getBaseUnitValue();
		} else {
			throw new RuntimeException("Property is not EquationProperty.");
		}
		return result;
	}

	@Override
	public void setParameter(int i, double v) {
		if(Double.isNaN(v)){
			throw new RuntimeException("Value is NaN.");
		}
		PropertyOwner po = m_model.getPropertyOwner(i);
		PropertyKey pk = getPropertyKey(po, i);
		po.setProperty(pk, Double.toString(v));

	}

	@Override
	public double getMinLimit(int i) {
		EquationArrayProperty eap = (EquationArrayProperty)m_model.getValueAt(i, 2);
		return eap.getValue()[i].getBaseUnitValue();
	}

	@Override
	public int getParameterCount() {
		return m_model.getRowCount();
	}

	@Override
	public double getMaxLimit(int i) {
		EquationArrayProperty eap = (EquationArrayProperty)m_model.getValueAt(i, 3);
		return eap.getValue()[i].getBaseUnitValue();
	}

	@Override
	public void computeError(double quality) {
		if(m_model.getSensor() == null){
			return;
		}
		//make a new top level group that only includes the one sensor we're interested in
		//and no other imagers or sources
		OpticsObjectGroup optics = m_model.getProject().getOpticsObject();
		Collection<OpticsObject> os = optics.getFlattenedOpticsObjects(false);
		Collection<OpticsObject> group = new ArrayList<OpticsObject>();
		for(OpticsObject o : os){
			if(o instanceof AutofocusSensor){
			} else if(o instanceof ImageSource){
			} else if(o instanceof Imager){
			} else {
				group.add(o);
			}
			
		}
		group.add(m_model.getSensor());//now add sensor that we want to use
		m_model.getSensor().clear();
		m_model.getProject().getWrangler().addWranglerListener(this);
		//m_model.getProject().getWrangler().wrangle(group, optics,  m_numPhotonsPerCalc);
		m_model.getProject().getWrangler().wrangle(group, optics,  getNumPhotonsPerCalc(quality));

	}
	/**
	 * compute the number of photons to compute per run based on a quality from 0 to 1. 0 is lowest quality; 1 is highest.
	 * 0 will do 1000, 1 will do 100000. It will increase logarithmically between.
	 * @param quality
	 * @return
	 */
	private int getNumPhotonsPerCalc(double quality){
		double q = quality;
		if(q < 0.0){
			q = 0.0;
		} else if(q > 1.0){
			q = 1.0;
		}
		double result = Math.pow(10,q);//for an input of 0 to 1, this will return 1 to 100.
		result *= 2000;
		//System.out.println("AutofocusSystemToSolve.getNumPhotonsPerCalc " + result);
		return (int)Math.round(result);
	}
	@Override
	public void addCalcCompleteListener(CalcCompleteListener listener) {
		if(!m_listeners.contains(listener)){
			m_listeners.add(listener);
		}

	}

	@Override
	public void removeCalcCompleteListener(CalcCompleteListener listener) {
		m_listeners.remove(listener);

	}

	@Override
	public boolean isCalculating() {
		return m_model.getProject().getWrangler().isWrangling();
		
	}
	
	
	
	@Override
	public void wranglingUpdate(WranglerEvent e) {
		switch(e.getType()){
		case FINISHED:
			m_model.getProject().getWrangler().removeWranglerListener(this);
			fireCalcComplete();
			break;
		default:
			break;
		}
		
	}
	private void fireCalcComplete(){
		CalcCompleteEvent e = new CalcCompleteEvent(this);
		for(CalcCompleteListener ccl : m_listeners){
			ccl.calcComplete(e);
		}
	}
	

}
