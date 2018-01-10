package misc.filter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;


public class Translation extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "translation";
	private String translationGffFilename = null;
	private CanonicalGFF cgff = null;


	public ArrayList[] alignmentFilter(ArrayList readRecordsList) {
		ArrayList nonPassedList = new ArrayList();
		Set passedSet = new LinkedHashSet();
		
		for (int i = 0; i < readRecordsList.size(); i++) {
			AlignmentRecord record = (AlignmentRecord) readRecordsList.get(i);
			AlignmentRecord newRecord = record.translate(cgff);
			if(newRecord!=null){
				passedSet.add(newRecord);
			}else if(processNonPassed){
				nonPassedList.add(record);
			}
		}
		
		ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=new ArrayList(passedSet);
        ansArr[1]=nonPassedList;
		return ansArr;
	}

	public boolean paraProc(String[] params) throws Exception {
		if(params.length<1){
			throw new Exception("(-filter): "+methodName+", canonical GFF filename not assigned");
		}else{
			translationGffFilename = params[0];
			cgff = new CanonicalGFF(translationGffFilename);
		}

		return true;
	}

	public String reportSetting() {
		return (methodName + " : " + translationGffFilename);
	}

}
