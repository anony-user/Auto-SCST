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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

public class SimpleMinimumSolver implements SystemSolver{
	private SystemToSolve m_system;
	/** history of previous values where x is parameter value and y is error*/
	private ArrayList<Point2D.Double> m_history = new ArrayList<Point2D.Double>();
	private ArrayList<Point2D.Double> m_lowestPoints = new ArrayList<Point2D.Double>();
	//private QuadraticFit m_fit = new QuadraticFit();
	private CubicFit m_fit = new CubicFit();
	private enum State {FIRST_TIME, SECOND_TIME, SUBSEQUENT_TIMES};
	private State m_firstTime = State.FIRST_TIME;
	private double[] m_fitYs = new double[FIT_POINTS];
	private double[] m_fitXs = new double[FIT_POINTS];
	private double m_bestValueAv = 0;
	private double m_bestValueSq = 0;
	private List<SolverUpdateListener> m_listeners = new CopyOnWriteArrayList<SolverUpdateListener>();

	public SimpleMinimumSolver(){
		m_system = null;
	}
	int m_runCounter = 0;
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#run(boolean)
	 */
	@Override
	public void solve(SystemToSolve system){
		if(m_system != null){
			m_system.removeCalcCompleteListener(this);
		}
		m_system = system;
		m_system.addCalcCompleteListener(this);
		if(!m_running){
			m_runCounter = 0;
			m_running = true;
			runOneIteration();
		}
	}
	@Override
	public void stop(){
		if( m_running){
			m_running = false;
		}
	}
	double m_min = 0;
	double m_max = 0;
	double m_bestValue = 0;
	private boolean m_running;
	private void runOneIteration(){
		if(!m_running || m_system == null){
			return;
		}
		++m_runCounter;

		if(m_runCounter < 1000){
			//System.out.println("SimpleMinimumSolver.runOneIteration");
			double v = m_system.getParameter(0);
			switch(m_firstTime){
			case FIRST_TIME:
				m_min = m_system.getMinLimit(0);
				m_max = m_system.getMaxLimit(0);
				v = m_min;
				//System.out.println("SimpleMinimumSolver first time.");
				m_firstTime = State.SECOND_TIME;
				break;
			case SECOND_TIME:
				m_firstTime = State.SUBSEQUENT_TIMES;
				v = m_max;
				break;
			default:
				v = Math.random()*(m_max - m_min)+m_min;
				
			}
			m_system.setParameter(0, v);
			m_system.computeError(1.0);
		}
	}
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#getErrorPlotValues()
	 */
	@Override
	public double[] getErrorPlotValues(){
		double[] result = new double[m_lowestPoints.size()];
		for(int i = 0; i < m_lowestPoints.size(); ++i){
			result[i] = m_lowestPoints.get(i).y;
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#getParameterPlotValues(int)
	 */
	@Override
	public double[] getParameterPlotValues(int n){
		double[] result = new double[m_lowestPoints.size()];
		for(int i = 0; i < m_lowestPoints.size(); ++i){
			result[i] = m_lowestPoints.get(i).x;
		}
		return result;
	}
	private static final int FIT_POINTS = 100;
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#getFitXPlotValues(int)
	 */
	@Override
	public double[] getFitXPlotValues(int i){
		return m_fitXs;
	}
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#getFitYPlotValues()
	 */
	@Override
	public double[] getFitYPlotValues(){
		double[] result = new double[m_lowestPoints.size()];
		for(int i = 0; i < m_lowestPoints.size(); ++i){
			result[i] = m_lowestPoints.get(i).x;
		}
		return m_fitYs;
	}
	@Override
	public void calcComplete(CalcCompleteEvent e) {
		if(m_system == null){
			return;
		}
		boolean keepRunning = true;
		Point2D.Double p = new Point2D.Double(m_system.getParameter(0), m_system.getErrorValue());
		m_history.add(p);
		if(m_lowestPoints.size() == 0){
			m_lowestPoints.add(p);
		} else {
			int j = m_lowestPoints.size();
			for(int i = 0; i < m_lowestPoints.size(); ++i){
				if(m_lowestPoints.get(i).x > p.x){
					j = i;
					break;
				}
			}
			m_lowestPoints.add(j,p);
			
			
			
		}
		if(m_lowestPoints.size() > 31){
			int maxI = m_lowestPoints.size() - 1;
			if(m_history.size() % 3 == 1){
				if(m_bestValue - m_lowestPoints.get(0).x > m_lowestPoints.get(maxI).x - m_bestValue){
					m_lowestPoints.remove(0);
					
				} else {
					m_lowestPoints.remove(maxI);
				}
			}
		}
		if(m_lowestPoints.size() > 3){
			m_min = m_lowestPoints.get(0).x;
			m_max = m_lowestPoints.get(m_lowestPoints.size() - 1).x;
			double[] xs = new double[m_lowestPoints.size()];
			double[] ys = new double[m_lowestPoints.size()];
			for(int i = 0; i < xs.length; ++i){
				xs[i] = m_lowestPoints.get(i).x;
				ys[i] = m_lowestPoints.get(i).y;
			}
			m_fit.fit(xs, ys);
			keepRunning = !accumulateBestValue(m_fit.getMin(m_min, m_max));
			
			for(int i = 0; i < FIT_POINTS; ++i){
				m_fitXs[i] = (m_max-m_min)/((double)FIT_POINTS)*((double)i) + m_min;
				m_fitYs[i] = m_fit.getValue(m_fitXs[i]);
			}
		}
		//System.out.println("SimpleMinimumSolver.calcComplete "+m_history.size()+", "+m_system.getParameter(0)+","+m_system.getErrorValue());
		if(keepRunning){
			runLater();
		}
		
	}
	/**
	 * 
	 * @param bv
	 * @return true if standard deviation is less than 1 micron
	 */
	private boolean accumulateBestValue(double bv) {
		m_bestValue = bv;
		m_bestValueAv *= 0.95;
		m_bestValueAv += 0.05*bv;
		m_bestValueSq *= 0.95;
		m_bestValueSq += 0.05*bv*bv;
		
		
		double sd = Math.sqrt(m_bestValueSq - Math.pow(m_bestValueAv, 2));
		System.out.println("SimpleMinimumSolver.accululateBestValue st dev "+sd);
		return (sd < 1e-6);
		
	}
	
	@Override
	public boolean isRunning(){
		return m_running;
	}
	
	private void runLater(){
		if(!m_running || m_system == null){
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
	
	@Override
	public double getBestParameterValue(int i){
		if(m_system == null){
			throw new RuntimeException("System is null");
		}
		double result = m_system.getParameter(i);
		if(i == 0){
			result = m_bestValue;
		}
		return result;
	}
//	/* (non-Javadoc)
//	 * @see net.sourceforge.jocular.math.SystemSolver#isEnoughPoints()
//	 */
//	@Override
//	public boolean isEnoughPoints(){
//		return m_lowestPoints.size() > 10;
//	}
	/* (non-Javadoc)
	 * @see net.sourceforge.jocular.math.SystemSolver#reset()
	 */
	@Override
	public void reset() {
		m_running = false;
		m_history.clear();

		m_lowestPoints.clear();
		m_firstTime = State.FIRST_TIME;
		m_runCounter = 0;
		//m_fitXs = new double[FIT_POINTS];
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
	@Override
	public double getStandardDeviation(int i) {
		return Math.sqrt(m_bestValueSq - Math.pow(m_bestValueAv, 2));
	}
	@Override
	public boolean isSolved() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public int getParameterCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	
}
