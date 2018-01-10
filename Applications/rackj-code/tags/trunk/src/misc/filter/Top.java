package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;

/**
 * @deprecated
 */
public class Top extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "top";
	private boolean topOnly = false;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();

		// best identity
		float bestIdentity=0.0F;
		for(int i = 0; i < readRecordList.size(); i++){
			AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
			if(record.identity > bestIdentity) {
				bestIdentity=record.identity;
			}
		}
		
		for (int i = 0; i < readRecordList.size(); i++) {
        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
        	if (record.identity >= bestIdentity) {
        		passedRecords.add(record);
            }else if(processNonPassed){
        		nonPassedRecords.add(record);
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
			topOnly=Boolean.valueOf(params[0]);
		}

		return topOnly;
	}

	public String reportSetting() {
		return (methodName + " : " + topOnly);
	}

}
