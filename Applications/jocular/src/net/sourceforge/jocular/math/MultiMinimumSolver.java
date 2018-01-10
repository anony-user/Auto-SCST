/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.math;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

/**
 * A multiparameter solver. Each discard cycle it will compute the average location of the
 * half of the population with the lowest error values.
 * the point farthest from this location will be discarded.
 * Cycles that are not discard cycles will just compute a random point in the solution space and compute the error.
 * Discard cycles will occur every few cycles, after a minimum number of points has been computed.
 * @author kenneth
 *
 */
public class MultiMinimumSolver implements SystemSolver {
	private boolean m_running = false;
	//private ArrayList<OneSample> m_history = new ArrayList<OneSample>();
	private ArrayList<OneSample> m_workingSamples = new ArrayList<OneSample>();
	private double[] m_currentValues;
	private double[] m_minValues;
	private double[] m_maxValues;
	private double[] m_bestValues;
	private double m_minError = 0;
	private double m_maxError = 0;
	private static final int CYCLES_BEFORE_DISCARD = 20;
	private static final int MAX_WORKING_POINTS = 50;
	private double m_quality = 0.0;
	private int m_cycleCount = 0;
	private SystemToSolve m_system = null;
	private int m_paramNum = 0;
	private double[] m_bestValueAvs;
	private double[] m_bestValueSqAvs;
	private boolean m_solved = false;
	private List<SolverUpdateListener> m_listeners = new CopyOnWriteArrayList<SolverUpdateListener>();
	//private ArrayList<Double> m_errorValues = new ArrayList<Double>();
	/**
	 * this should be called whey system to solve completes
	 * @return true if sovled
	 * @param e
	 */
	@Override
	public void calcComplete(CalcCompleteEvent e) {
//		String s = "calc pos: ";
//		for(int i = 0; i < m_paramNum; ++i){
//			s += m_currentValues[i] + " ";
//		}
//		System.out.println("MulitMinimumSolver.calcComplete "+s);
		OneSample os = new OneSample(m_system.getErrorValue(), m_currentValues);
		//m_history.add(os);
		m_workingSamples.add(os);
		m_solved = calcBestValues();
		++m_cycleCount;
		if(m_cycleCount > (CYCLES_BEFORE_DISCARD*m_paramNum) && (m_cycleCount % 2 == 0 || m_workingSamples.size() > (MAX_WORKING_POINTS*m_paramNum))){
			discardWorstValue();
		}
//		if(m_workingSamples.size() == (MAX_WORKING_POINTS*m_paramNum)){
//			System.out.println("MultiMinimumSovler.calcComplete reached max point count");
//		}
//		System.out.println("MultiMinimumSovler.calcComplete "+m_workingSamples.size());
		fireSolverUpdate();
		runLater();
				
	}

	

	@Override
	public void solve(SystemToSolve system){
		if(!m_running){
			
			if(system != m_system || system.getParameterCount() != m_paramNum){
				if(m_system != null){
					m_system.removeCalcCompleteListener(this);
				}
				m_system = system;
				
				m_cycleCount = 0;
				
			}
			m_system.addCalcCompleteListener(this);
			m_running = true;
			m_solved = false;
			runOneIteration();
		}
	}
	private void runOneIteration(){
		switch(m_cycleCount){
		case 0://set up all state and get ready to calc at the min values
			m_paramNum = m_system.getParameterCount();
			m_currentValues = new double[m_paramNum];
			m_minValues = new double[m_paramNum];
			m_maxValues = new double[m_paramNum];
			m_bestValues = new double[m_paramNum];
			m_bestValueAvs = new double[m_paramNum];
			m_bestValueSqAvs = new double[m_paramNum];
//			m_parameterValues.clear();
//			m_errorValues.clear();
			//m_history.clear();
			m_workingSamples.clear();
			for(int i = 0; i < m_paramNum; ++i){
				m_bestValueAvs[i] = 0.01;
				m_bestValueSqAvs[i] = 0.01;
				m_currentValues[i] = m_system.getMinLimit(i);
				m_minValues[i] = m_system.getMinLimit(i);
				m_maxValues[i] = m_system.getMaxLimit(i);
				m_system.setParameter(i, m_currentValues[i]);
			}
			
			break;
		case 1://now run all max values
			for(int i = 0; i < m_paramNum; ++i){
				m_currentValues[i] = m_maxValues[i];
				m_system.setParameter(i, m_currentValues[i]);
			}
			
			break;
		default://now generate random values within the defined solution space
			calcRandomValues();
			break;
		}
		m_system.computeError(m_quality);
		
	}
	/**
	 * compute the next values to test. This should probably make sure the values
	 * are within the ellipsoid defined by the min and max values
	 */
	private void calcRandomValues() {
		double mag = Double.MAX_VALUE;

		while(mag > 1){
			mag = 0;
			for(int i = 0; i < m_paramNum; ++i){
				m_currentValues[i] = (m_maxValues[i] - m_minValues[i])*Math.random() + m_minValues[i];
				m_system.setParameter(i, m_currentValues[i]);
			}
		}
		
	}



	@Override
	public void stop(){
		if(m_running){
			m_running = false;
			fireSolverUpdate();
			m_system.removeCalcCompleteListener(this);
		}
	}

	@Override
	public double[] getErrorPlotValues() {
		double[] result = new double[m_workingSamples.size()];
		for(int i = 0; i < result.length; ++i){
			result[i] = m_workingSamples.get(i).getError();
		}
		return result;
	}

	@Override
	public double[] getParameterPlotValues(int n) {
		if(n > m_paramNum){
			throw new RuntimeException("Index out of bounds "+n);
		}
		double[] result = new double[m_workingSamples.size()];
		for(int i = 0; i < result.length; ++i){
			result[i] = m_workingSamples.get(i).getParam(n);
			if(m_cycleCount > (CYCLES_BEFORE_DISCARD*m_paramNum)){
				result[i] -= m_bestValues[n];
			} else {
				result[i] -= (m_maxValues[n] + m_minValues[n])/2;
			}
		}
		return result;
	}

	@Override
	public double[] getFitXPlotValues(int i) {
		double[] xs = new double[2];
		xs[0] = 0;
		xs[1] = 0;
		return xs;
	}

	@Override
	public double[] getFitYPlotValues() {
		double[] ys = new double[2];
		ys[0] = m_minError;
		ys[1] = m_maxError;
		return ys;
	}

	@Override
	public boolean isRunning() {
		return m_running;
	}

	@Override
	public void reset() {
		stop();
		//m_history.clear();
		m_workingSamples.clear();
		m_cycleCount = 0;
		if(m_system != null){
			m_system.removeCalcCompleteListener(this);
		}
		m_system = null;
		m_bestValues = null;
		m_minValues = null;
		m_maxValues = null;
		m_bestValueAvs = null;
		m_bestValueSqAvs = null;
	}
	
	@Override
	public int getParameterCount() {
		int result = 0;
		if(m_bestValues != null){
			result = m_bestValues.length;
		}
		return result;
	}



	@Override
	public double getBestParameterValue(int i) {
		double result = 0;
		if(m_bestValues != null){
			result =  m_bestValues[i];
		}
		return result;
	}

	private void runLater(){
		if(!m_running || m_system == null || m_solved){
			return;
		}
		if(!m_system.isCalculating()){
			runOneIteration();
		} else {
		
			Timer t = new Timer(100, new ActionListener(){
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					runLater();
				}
				
			});
			t.setRepeats(false);
			t.start();
		}
	}
	private class OneSample {
		private final double m_errorValue;
		private final double[] m_paramValues;
		OneSample(double error, double[] params){
			m_errorValue = error;
			m_paramValues = new double[params.length];
			for(int i = 0; i < m_paramValues.length; ++i){
				m_paramValues[i] = params[i];
			}
		}
		public double getError(){
			return m_errorValue;
		}
		public int getParamNum(){
			return m_paramValues.length;
		}
		public double getParam(int i){
			return m_paramValues[i];
		}
		public double distance(double[] p){
			double result = 0;
			for(int i = 0; i < m_paramValues.length; ++i){
				result += Math.pow(m_paramValues[i] - p[i], 2);
			}
			result = Math.sqrt(result);
			return result;
		}
	}
	/**
	 * assume this is called at the instant upon completing the last error calc
	 * @return true if considered solved
	 */
	private boolean calcBestValues(){
		//compute the mean error. Use this for now instead of median value
		double mean = 0;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double[] minPs = new double[m_paramNum];
		double[] maxPs = new double[m_paramNum];
		double[] minBestHalf = new double[m_paramNum];
		double[] maxBestHalf = new double[m_paramNum];
		boolean firstTime = true;
		
		for(OneSample os : m_workingSamples){
			mean += os.getError();
			if(os.getError() > max){
				max = os.getError();
			}
			if(os.getError() < min){
				min = os.getError();
			}
			if(m_cycleCount > (CYCLES_BEFORE_DISCARD*m_paramNum)){
				for(int i = 0; i < m_paramNum; ++i){
					if(firstTime){	
						minPs[i] = Double.MAX_VALUE;
						maxPs[i] = Double.MIN_VALUE;
						minBestHalf[i] = Double.MAX_VALUE;
						maxBestHalf[i] = Double.MIN_VALUE;
					}
					if(minPs[i] > os.getParam(i)){
						minPs[i] = os.getParam(i);
					}
					if(maxPs[i] < os.getParam(i)){
						maxPs[i] = os.getParam(i);
					}
				}
				firstTime = false;
				
			}
				
		}
		if(m_cycleCount > (CYCLES_BEFORE_DISCARD*m_paramNum)){
			m_maxValues = maxPs;
			m_minValues = minPs;
		}
		mean /= m_workingSamples.size();
		//compute the average position in the parameter space. This is the best value at this moment
		double[] bvs = new double[m_paramNum];
		firstTime = true;
		int n = 0;
		for(OneSample os : m_workingSamples){
			if(os.getError() < mean){
				++n;
				for(int i = 0; i < m_paramNum; ++i){
					
					if(firstTime){
						bvs[i] = os.getParam(i);
						
					} else {
						bvs[i] += os.getParam(i);
					}
					
					if(os.getParam(i) > maxBestHalf[i]){
						maxBestHalf[i] = os.getParam(i);
					}
					if(os.getParam(i) < minBestHalf[i]){
						minBestHalf[i] = os.getParam(i);
					}
					
				}
				firstTime = false;
			}
		}
		
		for(int i = 0; i < m_paramNum; ++i){
			if(n == 0){
				bvs[i] = 0;
			} else {
				bvs[i] /= n;
			}
			
		}
		
		//compute a metric of the noisiness of the data. This will be used to set the quality of the next run
		double noise = 0;
		for(int i = 0; i < m_paramNum; ++i){
			double pNoise = (maxBestHalf[i] - minBestHalf[i])/(m_maxValues[i] - m_minValues[i]);
			//System.out.println("MultiminimumSolver.calcBestValues i: "+ i+", n:"+pNoise);
			noise += Math.pow(pNoise,2);
		}
		noise = Math.sqrt(noise/m_paramNum);
		noise -= 0.75;
		//noise = (noise > 0.0) ? noise : 0.0;
		//if noise > 0 then probably the quality should be increased. If noise < 0 then the quality can be decreased.
		//System.out.println("MultiMinimumSolver.calcBestValues noise = "+noise);
		//compute a new value for quality
		m_quality += noise/100;
		if(m_quality > 1.0){
			m_quality = 1.0;
		} else if(m_quality < 0.0){
			m_quality = 0.0;
		}
		
		
		m_bestValues = bvs;
		m_minError = min;
		m_maxError = max;
		return accumulateBestValue(bvs);
		
	}
	/**
	 * assumes that calcBestValues has been run just before this
	 */
	private void discardWorstValue() {
		OneSample worstSample = null;
		double d = 0;
		double worstD = 0;
		for(OneSample os : m_workingSamples){
			d = os.distance(m_bestValues);
			if(worstSample == null || d > worstD){
				worstD = d;
				worstSample = os;
			}
		}
		if(worstSample != null){
			m_workingSamples.remove(worstSample);
		}
	}
	/**
	 * 
	 * @param bv
	 * @return true if standard deviation is less than 1 micron
	 */
	private boolean accumulateBestValue(double[] bvs) {
		double maxSd = 0;
		for(int i = 0; i < bvs.length; ++i){
			m_bestValueAvs[i] *= 0.95;
			m_bestValueAvs[i] += 0.05*bvs[i];
			m_bestValueSqAvs[i] *= 0.95;
			m_bestValueSqAvs[i] += 0.05*bvs[i]*bvs[i];
			double sd = Math.sqrt(m_bestValueSqAvs[i] - Math.pow(m_bestValueAvs[i], 2));
			if(Double.isNaN(sd)){
				sd = Double.MAX_VALUE;
			}
			if(sd > maxSd){
				maxSd = sd;
			}
			if(Double.isNaN(m_bestValueAvs[i]) || Double.isNaN(m_bestValueSqAvs[i])){
				m_bestValueAvs[i] = 0.01;
				m_bestValueSqAvs[i] = 0.01;
			}
			
		}

		return (maxSd < 1e-6);
		
	}
	@Override
	public void addSolverUpdateListener(SolverUpdateListener listener) {
		if(!m_listeners.contains(listener)){
			m_listeners.add(listener);
		}

	}

	@Override
	public void removeSolverUpdateListener(SolverUpdateListener listener) {
		m_listeners.remove(listener);

	}
	private void fireSolverUpdate(){
		SolverUpdateEvent e = new SolverUpdateEvent(this);
		for(SolverUpdateListener ccl : m_listeners){
			ccl.solverUpdated(e);
		}
	}



	@Override
	public double getStandardDeviation(int i) {
		if(m_bestValueSqAvs == null || m_bestValueAvs == null){
			return 0;
		}
		return Math.sqrt(m_bestValueSqAvs[i] - Math.pow(m_bestValueAvs[i], 2));
	}



	@Override
	public boolean isSolved() {
		return m_solved;
	}
	
	
}
