package misc.filter;

import java.util.ArrayList;
import java.util.TreeSet;

import misc.AlignmentRecord;

public class AlignmentIdentity extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "alnID";
	private float alignmentIdentityCutoff = 0.0F;
	private boolean top = false;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
	
		TreeSet<Float> identitySet = new TreeSet<Float>();
		if(top){
			for (int i = 0; i < readRecordList.size(); i++) {
	        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
	        	identitySet.add(record.getAlignmentIdentity());
			}
		}
		
        for (int i = 0; i < readRecordList.size(); i++) {
            AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
            if (record.getAlignmentIdentity() >= alignmentIdentityCutoff &&
            	((!top) || record.getAlignmentIdentity() >= identitySet.last() ) ){
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
			alignmentIdentityCutoff=Float.parseFloat(params[0]);

			for(int i=1;i<params.length;i++){
				if(params[i].equals("-top")){
					top = Boolean.valueOf(params[i+1]);
	                i++;
	            }
			}
		}
		
		return true;
	}

	
	public String reportSetting() {
		String str = "";
		str += methodName + " : ";
		str += ""+alignmentIdentityCutoff;
		str += " -top "+top;

		return str;
	}


}
