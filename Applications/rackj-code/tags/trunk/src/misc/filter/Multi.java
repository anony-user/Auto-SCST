package misc.filter;

import java.util.ArrayList;

public class Multi extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "multi";
	private Integer maxMultiLimit = null;
	private Integer minMultiLimit = null;
	
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList=new ArrayList();
		ArrayList nonPassedList=new ArrayList();
		
		if ((maxMultiLimit != null && readRecordList.size() > maxMultiLimit)
				|| (minMultiLimit != null && readRecordList.size() < minMultiLimit)) {
			if(processNonPassed) nonPassedList = readRecordList;
		} else {
			passedList = readRecordList;
		}
		
        ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=passedList;
        ansArr[1]=nonPassedList;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		if(params.length<2){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			for(int i=0;i<params.length;i++){
				if(params[i].equals("-max")){
					maxMultiLimit = Integer.parseInt(params[i + 1]);
					i++;
				}else if (params[i].equals("-min")) {
					minMultiLimit = Integer.parseInt(params[i + 1]);
					i++;
				} 
			}
			
			if (maxMultiLimit==null && minMultiLimit==null) {
				return false;
			}
		}
		
		return true;
	}

	public String reportSetting() {
		String str = methodName + " : ";
		if(maxMultiLimit!=null) str += " -max "+maxMultiLimit;
		if(minMultiLimit!=null) str += " -min "+minMultiLimit;
		
		return str;
	}

}
