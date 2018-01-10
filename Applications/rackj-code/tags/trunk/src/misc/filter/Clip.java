package misc.filter;

import java.util.ArrayList;

import misc.AlignmentRecord;


public class Clip extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "clip";
	private Boolean left, right, outer = null;
	private int minClipLen = 1;
	

	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
			
			int fiveClipped = record.qStarts[0] - 1;
			int threeClipped = record.readInfo.getReadLength()
					- (record.qStarts[record.numBlocks-1] + record.qBlockSizes[record.numBlocks-1] - 1);
			
			boolean result = false;
			
			if(outer!=null){
				boolean outerRes = false;
				if(fiveClipped>=minClipLen && threeClipped>=minClipLen){
					outerRes = true;
				}
				if(!outer){
					outerRes = !outerRes;
				}
								
				result = result || outerRes;
			}
			
			if(left!=null){
				boolean leftRes = false;
				if(fiveClipped>=minClipLen && threeClipped==0){
					leftRes = true;
				}
				if(!left){
					leftRes = !leftRes;
				}

				result = result || leftRes;
			}
			
			if(right!=null){
				boolean rightRes = false;
				if(fiveClipped==0 && threeClipped>=minClipLen){
					rightRes = true;
				}
				if(!right){
					rightRes = !rightRes;
				}

				result = result || rightRes;
			}
			
			//
			if( result ){
				passedList.add(record);
			}else{
				nonPassedList.add(record);
			}
		}
		
		ArrayList[] ansArr = new ArrayList[2];
		ansArr[0]=passedList;
		ansArr[1]=nonPassedList;
		return ansArr;
	}


	public boolean paraProc(String[] params) throws Exception {
		if(params.length<1){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			for(int i=0;i<params.length;i++){
				if(params[i].equals("-left")){
					left = Boolean.valueOf(params[i+1]);
					i++;
				}else if (params[i].equals("-right")){
					right = Boolean.valueOf(params[i+1]);
					i++;
				}else if (params[i].equals("-outer")){
					outer = Boolean.valueOf(params[i+1]);
					i++;
				}else if (params[i].equals("-min")){
					minClipLen = Integer.parseInt(params[i+1]);
					i++;
					if(minClipLen <= 0)
						throw new Exception("invalid minimum clip length");
				}else if (params[i].equals("-reverse")){
					reverseSelection = true;
				}
			}
		}
		
		if(left==null && right==null && outer==null)
			throw new Exception("clipping types -left|-right|-outer must be specified");
		
		return true;
	}


	public String reportSetting() {
		String str = methodName + " :";
		if(left!=null) str += " -left " + left;
		if(right!=null) str += " -right " + right;
		if(outer!=null) str += " -outer " + outer;
		if(minClipLen>0) str += " -min " + minClipLen;
		if(reverseSelection) str += " -reverse";
		
		return str;
	}

}
