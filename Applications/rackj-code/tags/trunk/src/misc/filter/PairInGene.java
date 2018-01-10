package misc.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;


public class PairInGene extends FilterInterfacePairReadAdaptor {
	public static String methodName = "PairInGene";
	private String gffFilename = null;
	private boolean useExonRegion = false;
	private boolean checkByContaining = false;
	private int minimumOverlap = 8;
	private boolean checkAllBlocks = false;
	
	private CanonicalGFF cgff = null;
	
	
	protected ArrayList[][] doFilterInPair(ArrayList readRecordA, ArrayList readRecordB) {
		Map readRecordAGeneMap = getHitGenes(readRecordA);
		Map readRecordBGeneMap = getHitGenes(readRecordB);
        
		Set passedTmpA = new HashSet();
		Set passedTmpB = new HashSet();
		// check if any two alignments are paired alignment
		for (Iterator irtA = readRecordAGeneMap.keySet().iterator(); irtA.hasNext();) {
			AlignmentRecord recordA = (AlignmentRecord) irtA.next();
			for (Iterator itrB = readRecordBGeneMap.keySet().iterator(); itrB.hasNext();) {
				AlignmentRecord recordB = (AlignmentRecord) itrB.next();
				if (isPairedAlignment((Set) readRecordAGeneMap.get(recordA),
						(Set) readRecordBGeneMap.get(recordB))) {
					passedTmpA.add(recordA);
					passedTmpB.add(recordB);
				}
			}
		}
		
		// passed 
		Set passedRecordA = new LinkedHashSet(readRecordA); 
		Set passedRecordB = new LinkedHashSet(readRecordB);
		passedRecordA.retainAll(passedTmpA);
		passedRecordB.retainAll(passedTmpB);
		
		// non-passed
		ArrayList nonpassedRecordA;
		ArrayList nonpassedRecordB;
		if(processNonPassed){
			Set tmpA = new LinkedHashSet(readRecordA);
			tmpA.removeAll(passedTmpA);
			nonpassedRecordA = new ArrayList(tmpA);
			Set tmpB = new LinkedHashSet(readRecordB);
			tmpB.removeAll(passedTmpB);
			nonpassedRecordB = new ArrayList(tmpB);
		}else{
			nonpassedRecordA = new ArrayList();
			nonpassedRecordB = new ArrayList();
		}
		
		ArrayList[][] filteredResults = new ArrayList[2][2];
		ArrayList[] resultA = {new ArrayList(passedRecordA), nonpassedRecordA};
		ArrayList[] resultB = {new ArrayList(passedRecordB), nonpassedRecordB};
		filteredResults[0] = resultA;
		filteredResults[1] = resultB;
		
		return filteredResults;
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
		
		return str;
	}
	
	
	private Map getHitGenes(ArrayList recordList){
		LinkedHashMap alignmentGenesMap = new LinkedHashMap();
        for (int i = 0; i < recordList.size(); i++) {
            AlignmentRecord record = (AlignmentRecord) recordList.get(i);
            // qualification
            Set hitGeneRegions = cgff.getRelatedGenes(record.chr,
                    record.getMappingIntervals(), useExonRegion,
                    checkByContaining, minimumOverlap, checkAllBlocks);
            if (hitGeneRegions.size() > 0) {
            	alignmentGenesMap.put(record,hitGeneRegions);
            }
        }
        
        return alignmentGenesMap;
	}
	
	
	private boolean isPairedAlignment(Set hitGeneSetA, Set hitGeneSetB) {
		// check if two alignments have the intersection of mapped genes
		HashSet aGeneSet = new HashSet(hitGeneSetA);
		HashSet bGeneSet = new HashSet(hitGeneSetB);
		aGeneSet.retainAll(bGeneSet);
		if (aGeneSet.size() <= 0)
			return false;

		// if two alignment pass above criteria, then we consider they are
		// pair-end alignment
		return true;
	}

}
