package misc.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import misc.AlignmentRecord;
import misc.Interval;


public class BlockSizes extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "block";

	private static Set typeSet = new LinkedHashSet(Arrays.asList("query","target"));
	private String type = null;

	private static Set modeSet = new LinkedHashSet(
			Arrays.asList("minimum","maximum","summation","average","count","window"));
	private String mode = null;

	private Float maxLimit = null;
	private Float minLimit = null;
	
	private Integer windowSize = null;

	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordsList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);

			// get values according to type
			Set<Interval> intervals = null;
			if(type.toLowerCase().equals("query")){
				intervals = record.getMappingIntervals(false);
			}else if(type.toLowerCase().equals("target")){
				intervals = record.getMappingIntervals(true);
			}
			
			
			// for window mode
			if(mode.toLowerCase().equals("window")){
				int windowCnt = 0;
				for(Interval thisInterval : intervals){
					int size = thisInterval.length();
					// if in window mode and count not reached, TRY
					if(windowCnt<windowSize){
						if((maxLimit != null && size > maxLimit)
								|| (minLimit != null && size < minLimit)){ // unqualified
							windowCnt++;
						}else{
							windowCnt=0; // reset if qualified
						}
					}
				}
				// qualification
				if(windowCnt >= windowSize){
					if(processNonPassed) nonPassedList.add(record);
				}else{
					passedList.add(record);
				}
				continue;
			}
			
			// get final value according to mode
			int min=-1;
			int max=0;
			int sum=0;
			for(Interval thisInterval : intervals){
				int size = thisInterval.length();
				if(min==-1 || size<min){
					min = size;
				}
				if(size > max){
					max = size;
				}
				sum += size;
			}

			Number testVal = 0;
			if(mode.toLowerCase().equals("minimum")){
				testVal = min;
			}else if(mode.toLowerCase().equals("maximum")){
				testVal = max;
			}else if(mode.toLowerCase().equals("summation")){
				testVal = sum;
			}else if(mode.toLowerCase().equals("average")){
				if(sum==0){
					testVal = 0;
				}else{
					testVal = (float) sum / (intervals.size()-1);
				}
			}else if(mode.toLowerCase().equals("count")){
				testVal = intervals.size()-1;
			}
			
			// qualification
			if((maxLimit != null && testVal.floatValue() > maxLimit)
					|| (minLimit != null && testVal.floatValue() < minLimit)){
				nonPassedList.add(record);
			}else if(processNonPassed){
				passedList.add(record);
			}

		}
		
		ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=passedList;
        ansArr[1]=nonPassedList;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		if(params.length<2){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			type=params[0];

			for(int i=1;i<params.length;i++){
				if(params[i].equals("-max")){
					maxLimit = Float.parseFloat(params[i + 1]);
					i++;
				}else if (params[i].equals("-min")) {
					minLimit = Float.parseFloat(params[i + 1]);
					i++;
				}else if (params[i].equals("-window")) {
					windowSize = Integer.parseInt(params[i + 1]);
					i++;
				}else if (params[i].equals("-mode")) {
					mode = params[i + 1];
					i++;
				}
			}
			
		    if(!typeSet.contains(type.toLowerCase())) {
		    	System.err.println("block type (first parameter) not available, available choices:");
		        for(Iterator it = typeSet.iterator();it.hasNext();){
		          System.err.println(it.next());
		        }
				throw new Exception("(-filter): "+methodName+", block type (first parameter) not available");
	      	}
			
		    if(mode==null || (!modeSet.contains(mode.toLowerCase()))) {
		    	System.err.println("working mode (-mode) not assigned or not available, available choices:");
		        for(Iterator it = modeSet.iterator();it.hasNext();){
		          System.err.println(it.next());
		        }
				throw new Exception("(-filter): "+methodName+", working mode (-mode) not assigned or not available");
	      	}
		    
		    if(mode.equals("window") && (windowSize==null || windowSize<=0)){
				throw new Exception("(-filter): "+methodName+", window size (-window) not assigned or not available");
		    }
			
			if (maxLimit==null && minLimit==null) {
				return false;
			}
		}
		
		return true;
	}

	public String reportSetting() {
		String str = methodName + " : ";
		str += " "+type;
		str += " -mode "+mode;
		if(maxLimit!=null) str += " -max "+maxLimit;
		if(minLimit!=null) str += " -min "+minLimit;
		if(windowSize!=null) str += " -window "+windowSize;
		
		return str;
	}

}
