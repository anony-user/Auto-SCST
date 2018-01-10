package misc.filter;

import java.util.ArrayList;


public abstract class FilterInterfaceSingleReadAdaptor extends FilterInterfaceAdaptor {
	
	boolean reverseSelection = false;
	boolean keepAllasNonpass = false;

	public ArrayList[][] alignmentFilter(ArrayList[] readRecordLists) {
		ArrayList[][] retArray = new ArrayList[readRecordLists.length][2];
		
		for(int i=0;i<readRecordLists.length;i++){
			ArrayList readRecords = readRecordLists[i];
			if(readRecords==null){
				retArray[i][0] = new ArrayList();
				retArray[i][1] = null;
			}else{
				retArray[i] = alignmentFilter(readRecords);
			}
		}
		
		if(reverseSelection){
			for(int i=0;i<retArray.length;i++){
				ArrayList ans0 = retArray[i][0];
				ArrayList ans1 = retArray[i][1];
				
				if(ans1==null){
					ans1 = new ArrayList();
				}
				
				retArray[i][0] = ans1;
				retArray[i][1] = ans0;
			}
		}
		
		if(keepAllasNonpass){
			for(int i=0;i<retArray.length;i++){
				ArrayList ans0 = retArray[i][0];
				ArrayList ans1 = retArray[i][1];

				if(ans0.size()==0){
					retArray[i][0] = readRecordLists[i];
					retArray[i][1] = null;
				}
			}
		}
		
		if(!processNonPassed){
			for(int i=0;i<retArray.length;i++){
				retArray[i][1] = null;
			}
		}

		return retArray;
	}

	/**
	 * This function must output exactly one two-element array of ArrayLists of passed and non-pass alignment records.
	 */
	abstract public ArrayList[] alignmentFilter(ArrayList readRecordsList);

	abstract public boolean paraProc(String[] params) throws Exception;

	abstract public String reportSetting();

}
