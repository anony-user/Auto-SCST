package misc.filter;

import java.util.ArrayList;

public class Uniq extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "uniq";
	private boolean uniqOnly = false;

	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
	
		if(readRecordList.size()==1){
			passedRecords.add(readRecordList.get(0));
		}
        if(processNonPassed){
        	if(readRecordList.size()>1){
        		nonPassedRecords.addAll(readRecordList);
        	}
        }
		
		ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=passedRecords;
        ansArr[1]=nonPassedRecords;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		if(params.length<1){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			uniqOnly=Boolean.valueOf(params[0]);
		}
		
		return uniqOnly;
	}

	public String reportSetting() {
		return (methodName + " : " + uniqOnly);
	}

}
