package misc.filter;

import java.util.ArrayList;


public abstract class FilterInterfaceAdaptor implements FilterInterface {
	
	public static String methodName="";
	boolean processNonPassed = false;
	
	abstract public ArrayList[][] alignmentFilter(ArrayList[] readRecordLists);

	abstract public boolean paraProc(String[] params) throws Exception;

	abstract public String reportSetting();
	
	public void setProcessNonPassed(boolean tof){
		processNonPassed=tof;
	}
	
	public void stop(){
		// do nothing in default
	}
}
