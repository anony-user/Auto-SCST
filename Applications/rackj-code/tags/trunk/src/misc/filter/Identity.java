package misc.filter;

import java.util.ArrayList;
import java.util.TreeSet;

import misc.AlignmentRecord;

public class Identity extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "ID";
	private float identityCutoff = 0.0F;
	private boolean top = false;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		TreeSet<Float> identitySet = new TreeSet<Float>();
		if(top){
			for (int i = 0; i < readRecordList.size(); i++) {
	        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
	        	identitySet.add(record.identity);
			}
		}
		
		for (int i = 0; i < readRecordList.size(); i++) {
        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
            if(record.identity >= identityCutoff &&
               ((!top) || record.identity >= identitySet.last() ) ){
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
	
	public boolean paraProc(String[] params) throws Exception{
		if(params.length<1){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			identityCutoff=Float.parseFloat(params[0]);

			for(int i=1;i<params.length;i++){
				if(params[i].equals("-top")){
					top = Boolean.valueOf(params[i+1]);
	                i++;
	            }
			}
		}
		
		return true;
	}
	
	public String reportSetting(){
		String str = "";
		str += methodName + " : ";
		str += ""+identityCutoff;
		str += " -top "+top;

		return str;
	}

}
