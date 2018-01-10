package misc.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import misc.AlignmentRecord;


public class PairInDistance extends FilterInterfacePairReadAdaptor {
	public static String methodName = "PairInDistance";
	private Integer maxDistanceCutoff = null;
	private Integer minDistanceCutoff = null;
	private boolean top = false;
	

	protected ArrayList[][] doFilterInPair(ArrayList readRecordA, ArrayList readRecordB) {
		Set passedSetA = new LinkedHashSet();
		Set passedSetB = new LinkedHashSet();
		TreeMap identityMap = new TreeMap();

		for(ListIterator itrA=readRecordA.listIterator(); itrA.hasNext();){
			AlignmentRecord recordA = (AlignmentRecord) itrA.next();

			for(ListIterator itrB=readRecordB.listIterator(); itrB.hasNext();){
				AlignmentRecord recordB = (AlignmentRecord) itrB.next();
				if(isPairedAlignment(recordA, recordB, minDistanceCutoff, maxDistanceCutoff)){
					passedSetA.add(recordA);
					passedSetB.add(recordB);
					
					if(top){
						int sumMatch = Math.round(recordA.identity * recordA.readInfo.getReadLength())
								+ Math.round(recordB.identity * recordB.readInfo.getReadLength());
						int sumReadLen = recordA.readInfo.getReadLength() + recordB.readInfo.getReadLength();
						float pairedIdentity = ((float) sumMatch) / sumReadLen;
						
						if(identityMap.containsKey(pairedIdentity)==false)
							identityMap.put(pairedIdentity,new HashSet());
						Set pairedRecordSet = (HashSet) identityMap.get(pairedIdentity);
						pairedRecordSet.add(recordA);
						pairedRecordSet.add(recordB);
					}
					
				}
			}

		}

		if(identityMap.size()>0){
			Map.Entry entry = identityMap.lastEntry();
			Set topPairedRecordSet = (Set) entry.getValue();
			passedSetA.retainAll(topPairedRecordSet);
			passedSetB.retainAll(topPairedRecordSet);
		}
		
		// non-passed
		ArrayList nonpassedRecordA;
		ArrayList nonpassedRecordB;
		if(processNonPassed){
			Set tmpA = new LinkedHashSet(readRecordA);
			tmpA.removeAll(passedSetA);
			nonpassedRecordA = new ArrayList(tmpA);
			Set tmpB = new LinkedHashSet(readRecordB);
			tmpB.removeAll(passedSetB);
			nonpassedRecordB = new ArrayList(tmpB);
		}else{
			nonpassedRecordA = new ArrayList();
			nonpassedRecordB = new ArrayList();
		}
		
		ArrayList[][] filteredResults = new ArrayList[2][2];
		ArrayList[] resultA = {new ArrayList(passedSetA), nonpassedRecordA};
		ArrayList[] resultB = {new ArrayList(passedSetB), nonpassedRecordB};
		filteredResults[0] = resultA;
		filteredResults[1] = resultB;
		
		return filteredResults;
	}

	
	public boolean paraProc(String[] params) throws Exception {
		if(params.length<2){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			for(int i=0;i<params.length;i++){
				if(params[i].equals("-max")){
					maxDistanceCutoff = Integer.parseInt(params[i + 1]);
					i++;
				} else if (params[i].equals("-min")) {
					minDistanceCutoff = Integer.parseInt(params[i + 1]);
					i++;
				} else if (params[i].equals("-top")) {
					top = Boolean.valueOf(params[i + 1]);
					i++;
				}
			}

			if (maxDistanceCutoff==null && minDistanceCutoff==null) {
				return false;
			}
		}

		return true;
	}

	
	
	
	public String reportSetting() {
		String str = methodName + " : ";
		str += " -max "+maxDistanceCutoff;
		str += " -min "+minDistanceCutoff;
		str += " -top "+top;
		
		return str;
	}

	
	private boolean isPairedAlignment(AlignmentRecord alignmentA, AlignmentRecord alignmentB, Integer minCutOff, Integer maxCutOff){
		if(alignmentA.chr.equals(alignmentB.chr)==false){ 
			return false;
		}

		// the definition of distance between two alignments MUST be reconsidered
		int distance = (alignmentA.tStarts[0] - alignmentB.tStarts[0] < 0 ? 
				alignmentB.tStarts[0] - (alignmentA.tStarts[alignmentA.numBlocks - 1] + alignmentA.tBlockSizes[alignmentA.numBlocks - 1] - 1) -1
				: alignmentA.tStarts[0] - (alignmentB.tStarts[alignmentB.numBlocks - 1] + alignmentB.tBlockSizes[alignmentB.numBlocks - 1] - 1) -1);
		if( (minCutOff != null && distance < minCutOff) ||
				(maxCutOff != null && distance > maxCutOff) ) {
			return false;
		}

		return true;
	}

}
