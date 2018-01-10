package misc.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import misc.AlignmentRecord;
import misc.Interval;


public class InDel extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "indel";

	private static Set typeSet = new LinkedHashSet(Arrays.asList("insertion","deletion"));
	private String type = null;

	private static Set modeSet = new LinkedHashSet(
			Arrays.asList("minimum","maximum","summation","average","count"));
	private String mode = null;

	private Float maxLimit = null;
	private Float minLimit = null;

	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordsList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);

			// get values according to type
			Set<Interval> intervals = null;
			if(type.toLowerCase().equals("deletion")){
				intervals = record.getMappingIntervals(true);
			}else if(type.toLowerCase().equals("insertion")){
				intervals = record.getMappingIntervals(false);
			}
			
			// get final value according to mode
			int min=-1;
			int max=0;
			int sum=0;
			Interval lastInterval = null;
			for(Interval thisInterval : intervals){
				if(lastInterval != null){
					int gap = thisInterval.getStart() - lastInterval.getStop() -1;
					if(min==-1 || gap<min){
						min = gap;
					}
					if(gap > max){
						max = gap;
					}
					sum += gap;
				}
				lastInterval = thisInterval;
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
				if(processNonPassed) nonPassedList.add(record);
			}else {
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
				}else if (params[i].equals("-mode")) {
					mode = params[i + 1];
					i++;
				}
			}
			
		    if(!typeSet.contains(type.toLowerCase())) {
		    	System.err.println("InDel type (first parameter) not available, available choices:");
		        for(Iterator it = typeSet.iterator();it.hasNext();){
		          System.err.println(it.next());
		        }
				throw new Exception("(-filter): "+methodName+", InDel type (first parameter) not available");
	      	}
			
		    if(mode==null || (!modeSet.contains(mode.toLowerCase()))) {
		    	System.err.println("working mode (-mode) not assigned or not available, available choices:");
		        for(Iterator it = modeSet.iterator();it.hasNext();){
		          System.err.println(it.next());
		        }
				throw new Exception("(-filter): "+methodName+", working mode (-mode) not assigned or not available");
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
		
		return str;
	}

}
