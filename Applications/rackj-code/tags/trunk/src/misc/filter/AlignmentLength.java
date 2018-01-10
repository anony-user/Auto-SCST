package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;

public class AlignmentLength extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "alnLen";
	private int alignmentLengthCutoff = 0;
	
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
	
        for (int i = 0; i < readRecordList.size(); i++) {
            AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
            if (record.getAlignmentLength() >= alignmentLengthCutoff){
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
			alignmentLengthCutoff=Integer.parseInt(params[0]);
		}
		return true;
	}

	
	public String reportSetting() {
		return (methodName + " : " + Float.toString(alignmentLengthCutoff));
	}


}
