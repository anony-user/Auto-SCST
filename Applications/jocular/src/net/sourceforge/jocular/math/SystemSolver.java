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

public interface SystemSolver extends CalcCompleteListener {

	/**
	 * runs the solver on the specified system
	 * @param system the system to solve
	 */
	public abstract void solve(SystemToSolve system);
	/**
	 * stops the solver solving, if it is
	 */
	public abstract void stop();
	/**
	 * 
	 * @return the error plot values
	 */
	public abstract double[] getErrorPlotValues();
	
	/**
	 * Add a listener.
	 * Listeners will be informed when data changes 
	 * @param listener
	 */
	public void addSolverUpdateListener(SolverUpdateListener listener);


	/**
	 * remove a listener
	 * @param listener
	 */
	public void removeSolverUpdateListener(SolverUpdateListener listener);



	/**
	 * 
	 * @param n selects which parameter to get the values for.
	 * @return the parameter values corresponding to the error plot values.
	 */
	public abstract double[] getParameterPlotValues(int n);

	/**
	 * Gets x values for  a fit function, if appropriate.
	 * @param i specifies the parameter axis for this fit function
	 * @return x values of the fit function
	 */
	public abstract double[] getFitXPlotValues(int i);

	/**
	 * gets the y axis values of the fit function.
	 * @return
	 */
	public abstract double[] getFitYPlotValues();

	/**
	 * is this solver running
	 * @return true if running, otherwise false
	 */
	public abstract boolean isRunning();

//	/**
//	 * indicates that enough points have been gathered to start fitting
//	 * @deprecated This should be pruned pretty soon.
//	 * @return
//	 */
//	public abstract boolean isEnoughPoints();

	/**
	 * Resets this solver to it's initial state. This does not reset the various object parameters that may have been adjusted.
	 * stop solver if it's running
	 * Maybe it should.
	 */
	public abstract void reset();

	
	/**
	 * Get the value for the specified parameter that best solves the problem
	 * @param i the index of the parameter
	 * @return the best value
	 */
	public double getBestParameterValue(int i);
	/**
	 * 
	 * @return the number of parameters in the system
	 */
	public int getParameterCount();
	/**
	 * get the Standard Deviation of the solution over time.
	 * @param i
	 * @return
	 */
	public double getStandardDeviation(int i);
	/**
	 * returns true if system considers that the problem is solved and has therefore stopped calculating.
	 * @return true if sovled, false otherwise.
	 */
	public boolean isSolved();

}