package net.sourceforge.jocular.math;

public interface SystemToSolve {
	/**
	 * get the present value of the error function of this system. This is what should
	 * be minimized during the solving process.
	 * The noise inherent in this value is assumed to depend on the quality paramter used in computeError(double quality)
	 * @return
	 */
	double getErrorValue();
	/**
	 * Get the present values of the parameters to adjust to minimize the error function
	 * @param i
	 * @return
	 */
	double getParameter(int i);
	/**
	 * Alter a parameter's value hopefully to reduce the value of the error 
	 * @param i
	 * @param v
	 */
	void setParameter(int i, double v);
	/**
	 * Get the lower allowable limit of the specified parameter
	 * @param i
	 * @return
	 */
	double getMinLimit(int i);
	/**
	 * 
	 * @return the number of parameters in this system.
	 */
	int getParameterCount();
	/**
	 * Get the upper allowable limit of the specified parameter
	 * @param i
	 * @return
	 */
	double getMaxLimit(int i);
	/**
	 * Perform a recalculation of the error based on the present parameter values
	 * @param quality - a value from 0.0 to 1.0. 0.0 is the lowest quality result (high noise), 1.0 is the highest quality (low noise).
	 */
	void computeError(double quality);
	void addCalcCompleteListener(CalcCompleteListener listener);
	void removeCalcCompleteListener(CalcCompleteListener listener);
	boolean isCalculating();
//	/**
//	 * Accept the current state as the best values
//	 */
//	void accept(double[] bestValues);
}
