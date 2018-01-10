package misc.filter;

import java.util.ArrayList;


public abstract class FilterInterfacePairReadAdaptor extends
		FilterInterfaceAdaptor {

	boolean reverseSelection = false;

	public ArrayList[][] alignmentFilter(ArrayList[] readRecordLists) {

		ArrayList[][] retArray = new ArrayList[readRecordLists.length][2];

		if(readRecordLists.length>=2 && readRecordLists[0].size()>0 && readRecordLists[1].size()>0){
			retArray = doFilterInPair(readRecordLists[0], readRecordLists[1]);
		}else{
			for(int i=0;i<readRecordLists.length;i++){
				retArray[i] = doFilterSingle(readRecordLists[i]);
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

		if(!processNonPassed){
			for(int i=0;i<retArray.length;i++){
				retArray[i][1] = null;
			}
		}

		return retArray;
	}
	
	/**
	 * This default filtering function of pair-mate filters filters out all input alignments
	 * @param readRecords input alignments in ArrayList
	 * @return a two-element array of pass and non-pass alignments in ArrayLists
	 */
	protected ArrayList[] doFilterSingle(ArrayList readRecords){
		ArrayList[] retArray = new ArrayList[2];
		
		retArray[0] = new ArrayList();
		retArray[1] = readRecords;
		
		return retArray;
	}
	
	/**
	 * @return a two-element array, where each element is an array that contains 
	 * passed (index 0) and non-passed (index 1) alignments of the input. 
	 * The 1st element in two-element array is the result of readRecordA, 
	 * and the 2nd element is the result of readRecordB.
	 */
	abstract protected ArrayList[][] doFilterInPair(ArrayList readRecordA, ArrayList readRecordB);

	abstract public boolean paraProc(String[] params) throws Exception;

	abstract public String reportSetting();

}
