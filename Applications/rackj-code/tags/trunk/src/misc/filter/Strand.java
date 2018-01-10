package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;

public class Strand extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "forward";
	private boolean forward = true;

	@Override
	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
	
        for (int i = 0; i < readRecordsList.size(); i++) {
            AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);
            if (record.forwardStrand == forward){
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
			forward=Boolean.valueOf(params[0]);
		}
		return true;
	}

	@Override
	public String reportSetting() {
		return (methodName + " : " + forward);
	}

}
