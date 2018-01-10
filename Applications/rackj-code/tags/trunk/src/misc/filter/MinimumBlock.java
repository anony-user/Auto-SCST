package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;

public class MinimumBlock extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "minB";
	private int minimumBlock = 0;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedRecords = new ArrayList();
		ArrayList nonPassedRecords = new ArrayList();
		
		for(int i = 0; i < readRecordList.size(); i++){
			AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
            boolean qualified = true;
            for(int j=0;j<record.numBlocks;j++){
                int minSide = Math.min(record.qBlockSizes[j],record.tBlockSizes[j]);
                if(minSide<minimumBlock){
                    qualified = false;
                    break;
                }
            }
            if(qualified){
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
			minimumBlock=Integer.parseInt(params[0]);
		}
		
		return true;
	}

	
	public String reportSetting() {
		return (methodName + " : " + minimumBlock);
	}


}
