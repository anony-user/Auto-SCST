package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;


public class Splice extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "splice";
	private Integer maximumSplice = null;
	private Integer minimumSplice = null;
	private boolean requireAllBlocks = false;

	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordsList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);
	
	        int totalValid = (record.numBlocks-1);
			for(int j=1; j<record.numBlocks; j++){
				int spliceDistance = record.tStarts[j] - (record.tStarts[j-1] + record.tBlockSizes[j-1] -1) - 1;
				if ((maximumSplice != null && spliceDistance > maximumSplice)
						|| (minimumSplice != null && spliceDistance < minimumSplice)) {
					totalValid--;
				}
			}
			
			boolean qualified = true;
			if( totalValid==0 || (requireAllBlocks && totalValid<(record.numBlocks-1)) )
				qualified = false;
			
			if(qualified){
				passedList.add(record);
			}else if(processNonPassed){
				nonPassedList.add(record);
			}
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
					maximumSplice = Integer.parseInt(params[i + 1]);
					i++;
				}else if (params[i].equals("-min")) {
					minimumSplice = Integer.parseInt(params[i + 1]);
					i++;
				} else if (params[i].equals("-ALL")) {
					requireAllBlocks = Boolean.valueOf(params[i + 1]);
					i++;
				} 
			}
			
			if (maximumSplice==null && minimumSplice==null) {
				return false;
			}
		}
		
		return true;
	}

	public String reportSetting() {
		String str = methodName + " : ";
		if(maximumSplice!=null) str += " -max "+maximumSplice;
		if(minimumSplice!=null) str += " -min "+minimumSplice;
		str += " -ALL "+requireAllBlocks;
		
		return str;
	}

}
