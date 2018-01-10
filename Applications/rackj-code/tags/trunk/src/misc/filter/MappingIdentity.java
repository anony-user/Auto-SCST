package misc.filter;

import java.util.ArrayList;
import java.util.TreeSet;

import misc.AlignmentRecord;

public class MappingIdentity extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "mID";
	private float mappingIdentityCutoff = 0.0F;
	private boolean top = false;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
	
		TreeSet<Float> identitySet = new TreeSet<Float>();
		if(top){
			for (int i = 0; i < readRecordList.size(); i++) {
	        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
	        	identitySet.add(record.getMappingIdentity());
			}
		}

		for (int i = 0; i < readRecordList.size(); i++) {
            AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
            if (record.getMappingIdentity() >= mappingIdentityCutoff &&
                ((!top) || record.getMappingIdentity() >= identitySet.last() ) ){
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
			mappingIdentityCutoff=Float.parseFloat(params[0]);

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
		str += ""+mappingIdentityCutoff;
		str += " -top "+top;

		return str;
	}


}
