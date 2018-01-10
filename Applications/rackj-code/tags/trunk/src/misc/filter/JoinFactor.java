package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;


public class JoinFactor extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "J";
	private int joinFactor = 0; // 0 for reset
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordList.size(); i++) {
        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);

        	if(joinFactor>0){
        		record.nearbyJoin(joinFactor);
        	}else{
        		record.reset();
        	}
        	passedList.add(record);
        }
        
        ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=passedList;
        ansArr[1]=nonPassedList;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		if(params.length<1){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else if(params[0].equals("reset")){
			joinFactor = 0;
		}else{
			joinFactor=Integer.parseInt(params[0]);
			if(joinFactor<=0){
				return false;
			}
		}
		
		return true;
	}

	public String reportSetting() {
		return (methodName + " : " + joinFactor);
	}

}
