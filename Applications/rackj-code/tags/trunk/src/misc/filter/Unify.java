package misc.filter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import misc.AlignmentRecord;


public class Unify extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "unify";

	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList nonPassedList = new ArrayList();
		Set passedSet = new LinkedHashSet();
		
		for (int i = 0; i < readRecordsList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);
			passedSet.add(record);
		}
		
		ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=new ArrayList(passedSet);
        ansArr[1]=nonPassedList;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		return true;
	}

	public String reportSetting() {
		return methodName;
	}

}
