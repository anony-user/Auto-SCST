package rnaseq;

import java.util.Map;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;

/**
 * This interface defines necessary functions used in "fast" mapping result
 * iterator, which iterates every mapping result file only once. Class implements
 * this interface should do some necessary preprocessing for multi reads in
 * processingMulti() and then do final computation in finalize().
 * Note that this interface also includes report() which can be the same with
 * that defined in ReadCounter for making report files.
 * @author wdlin
 */
public interface ReadCounter2 {
	/**
	 * Called for every read has exactly one alignment AND hits exactly one gene. 
	 */
    public void processUnique(String readID,AlignmentRecord record,Number cnt,String geneID,CanonicalGFF cgff);

    /**
     * Called for every read has more than one alignment OR hits no gene or more than one gene. 
     */
    public void processMulti(String readID,Map hitGeneAlignmentSetMap,Set geneIdSet,CanonicalGFF cgff);
    
    public void finalize(Map<Set,Map<String,Double>> geneSetIdRatioMap);

    public void report(String filename,CanonicalGFF cgff);
}
