package misc.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;

public class Junction extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "junction";
	private String gffFilename = null;
	private int minBlockSize = 8;
    private boolean careDirection = false;
	
	private CanonicalGFF cgff = null;
	
	// filter parameters
	private int junctionCutoff = 1;
	private boolean top = false;
	private boolean checkALL = false;
	
	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();
		
		TreeMap<Integer,LinkedHashSet> jcntAlnMap = new TreeMap<Integer,LinkedHashSet>();
		
		for (int i = 0; i < readRecordList.size(); i++) {
        	AlignmentRecord record = (AlignmentRecord) readRecordList.get(i);

        	Map<GenomeInterval,Integer> geneJunctCntMap = cgff.getRelatedGenesWithJunctionCounts(record.chr, record.getMappingIntervals(),
        			record.forwardStrand, careDirection, minBlockSize);
        			
        	// checking
    		int topJcnt = 0;
    		if(geneJunctCntMap.size()>0){
    			topJcnt = geneJunctCntMap.get(geneJunctCntMap.keySet().iterator().next());
    		}

        	boolean pass = true;
        	if(topJcnt < junctionCutoff){
        		pass = false;
        	}
        	if(checkALL && topJcnt < 2*(record.numBlocks-1)){
        		pass = false;
        	}
        	
        	if ( pass ) {
            	passedList.add(record);

            	if(top){ // form junction count-alignment map
        			LinkedHashSet set;
        			if(jcntAlnMap.containsKey(topJcnt)){
        				set = jcntAlnMap.get(topJcnt);
        			}else{
        				set = new LinkedHashSet();
        				jcntAlnMap.put(topJcnt, set);
        			}
        			set.add(record);
            	}
            }else if(processNonPassed){
            	nonPassedList.add(record);
            }
        }
		
		if(top){
			passedList.clear();
			
			for(Iterator jcntIt = jcntAlnMap.keySet().iterator();jcntIt.hasNext();){
				int jcnt = (Integer) jcntIt.next();
				if(jcntIt.hasNext()){ // not greatest junction count
					nonPassedList.addAll(jcntAlnMap.get(jcnt));
				}else{ // greatest junction count
					passedList.addAll(jcntAlnMap.get(jcnt));
				}
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
				} else if (params[i].equals("-min")) {
					minBlockSize = Integer.parseInt(params[i + 1]);
					i++;
				} else if(params[i].equals("-direction")){
	                careDirection = Boolean.valueOf(params[i+1]);
	                i++;
				} else if (params[i].equals("-minJ")) {
					junctionCutoff = Integer.parseInt(params[i + 1]);
					i++;
				} else if(params[i].equals("-top")){
	                top = Boolean.valueOf(params[i+1]);
	                i++;
				} else if(params[i].equals("-ALL")){
					checkALL = Boolean.valueOf(params[i+1]);
	                i++;
				} else if(params[i].equals("-KAANP")){
					keepAllasNonpass = Boolean.valueOf(params[i+1]);
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
		str += " -min "+minBlockSize;
		str += " -direction "+careDirection;
		str += " -minJ "+junctionCutoff;
		str += " -top "+top;
		str += " -ALL "+checkALL;
		str += " -KAANP "+keepAllasNonpass;
		
		return str;
	}

}
