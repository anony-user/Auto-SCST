package misc.filter;

import java.util.ArrayList;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;


public class Gene extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "gene";
	private String gffFilename = null;
	private boolean useExonRegion = false;
	private boolean checkByContaining = false;
	private int minimumOverlap = 8;
	private boolean checkAllBlocks = false;
    private boolean careDirection = false;
	
	private CanonicalGFF cgff = null;

	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		for (int i = 0; i < readRecordList.size(); i++) {
        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);
        	Set hitGeneRegions = cgff.getRelatedGenes(record, useExonRegion,
                    checkByContaining, minimumOverlap, checkAllBlocks,careDirection);
            if (hitGeneRegions.size() > 0) {
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

	
	public boolean paraProc(String[] params) throws Exception {
		if(params.length<2){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			for(int i=0;i<params.length;i++){
				if(params[i].equals("-GFF")){
					gffFilename = params[i + 1];
					i++;
				}else if (params[i].equals("-exon")) {
					useExonRegion = Boolean.valueOf(params[i + 1]);
					i++;
				} else if (params[i].equals("-contain")) {
					checkByContaining = Boolean.valueOf(params[i + 1]);
					i++;
				} else if (params[i].equals("-min")) {
					minimumOverlap = Integer.parseInt(params[i + 1]);
					i++;
				} else if (params[i].equals("-ALL")) {
					checkAllBlocks = Boolean.valueOf(params[i + 1]);
					i++;
				} else if(params[i].equals("-direction")){
	                careDirection = Boolean.valueOf(params[i+1]);
	                i++;
	            }

			}
			
			if(gffFilename==null){
				throw new Exception("(-filter): "+methodName+", canonical GFF filename (-GFF) not assigned");
			}else{
				cgff = new CanonicalGFF(gffFilename);
			}
		}

		return true;
	}
	
	public String reportSetting() {
		String str = "";
		str += methodName + " : ";
		str += " -GFF "+gffFilename;
		str += " -exon "+useExonRegion;
		str += " -contain "+checkByContaining;
		str += " -min "+minimumOverlap;
		str += " -ALL "+checkAllBlocks;
		str += " -direction "+careDirection;
		
		return str;
	}

}
