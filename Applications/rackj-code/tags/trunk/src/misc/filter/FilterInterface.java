package misc.filter;

import java.util.ArrayList;

public interface FilterInterface {
	
	/**
	 * @param readRecordLists an array of lists of alignment records
	 * @return an array of two-element arrays, index 0 for passed alignments, index 1 for non-passed alignments, null for no alignments
	 */
	public ArrayList[][] alignmentFilter(ArrayList[] readRecordLists);
	
	/**
	 * Parameter processing.
	 * @param params upon the filter
	 * @return false if and only if the parameter setting of this filter instance is actually disabling itself.
	 * @throws Exception
	 */
	public boolean paraProc(String[] params) throws Exception;
	
	/**
	 * Setting the filter to output non-passed alignments or not.
	 * @param tof
	 */
	public void setProcessNonPassed(boolean tof);
	
	/**
	 * filter setup information, include parameter setting.
	 */
	public String reportSetting();
	
	/**
	 * The function to be called when the ENTIRE filter processing is going to be stopped.
	 */
	public void stop();
}
