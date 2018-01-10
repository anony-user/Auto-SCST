package misc;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class FineAlignmentRecord extends AlignmentRecord {

    private String qSeqs[];
    private String tSeqs[];

    /**
     * @deprecated newer methods require ReadInfo object
     */
    public FineAlignmentRecord(float identity, int numBlocks, int qStarts[],
                               int qBlockSizes[], String chrOriginal, int tStarts[],
                               int tBlockSizes[], String strand,
                               String qSeqs[], String tSeqs[]) {
      this(identity, numBlocks, qStarts, qBlockSizes, chrOriginal, tStarts,
            tBlockSizes, strand, qSeqs, tSeqs, null, null);
    }

    public FineAlignmentRecord(float identity, int numBlocks, int qStarts[],
                               int qBlockSizes[], String chrOriginal, int tStarts[],
                               int tBlockSizes[], String strand,
                               String qSeqs[], String tSeqs[], Object srcObj, ReadInfo readInfo) {
        super(identity, numBlocks, qStarts, qBlockSizes, chrOriginal, tStarts,
              tBlockSizes, strand, srcObj, readInfo);
        this.qSeqs = qSeqs;
        this.tSeqs = tSeqs;
    }

    public Set getSNPs() {
        TreeSet SNPset = new TreeSet();
        if(tSeqs.length==0) return SNPset;

        for (int i = 0; i <= numBlocks - 1; i++) {
            int start = tStarts[i];
            for (int j = 0; j <= qBlockSizes[i] - 1; j++) {
                Character q = Character.toUpperCase(qSeqs[i].charAt(j));
                Character t = Character.toUpperCase(tSeqs[i].charAt(j));
                if (q.equals(t) == false) {
                    int SNP_position = start + j;
                    GenomeInterval newSNP = new GenomeInterval(chrOriginal, SNP_position, SNP_position, new Pair(t, q));
                    SNPset.add(newSNP);
                }
            }
        }
        return SNPset;
    }

    public Set getIndels(){
        TreeSet INDLset = new TreeSet();
        for (int i = 1; i <= numBlocks - 1; i++) {
            int INDL_start = tStarts[i-1]+tBlockSizes[i-1]-1;
            int INDL_end = tStarts[i];
            int t_gap =  INDL_end-INDL_start-1;
            int q_gap =  qStarts[i]-qStarts[i-1]-qBlockSizes[i-1];
            GenomeInterval newINDL = new GenomeInterval(chrOriginal, INDL_start, INDL_end, new Pair(t_gap, q_gap));
            INDLset.add(newINDL);
        }
        return INDLset;
    }

    /**
     * This method returns the read base mapped to the specified position on reference.
     * @param refPosition reference position
     * @return Character the read base, null if the specified base is not mapped.
     */
    public Character getMappedBaseAt(int refPosition){
    	Character qBase = null;
    	for (int i = 0; i <= numBlocks - 1; i++) {
            int start = tStarts[i];
            int end = tStarts[i]+qBlockSizes[i]-1;
            if (start<=refPosition && refPosition<=end){
            	int basePosition=refPosition-start;
            	qBase = Character.toUpperCase(qSeqs[i].charAt(basePosition));
            	}
            }
    	return qBase;
    }

    protected ArrayList<String> getBLATtokens(String readID,int readLength,boolean attachQuerySeq){

        ArrayList<String> tokens = super.getBLATtokens(readID,readLength,attachQuerySeq);

        //load qSeqs info. from qSeqs[]
        String qSeqsElements = "";
        for (int i = 0; i < numBlocks; i++) {
            qSeqsElements += qSeqs[i] + ",";
        }

        //load tSeqs info. from tSeqs[]
        String tSeqsElements = "";
        if (tSeqs.length!=0) {
        	for (int i = 0; i < numBlocks; i++) {
        		tSeqsElements += tSeqs[i] + ",";
        	}
        }

        // update tokens
        if(tokens.size()<=21){
                tokens.add(21,qSeqsElements);
	}else{
                tokens.set(21,qSeqsElements);
	}
	if(tokens.size()<=22){
		tokens.add(22,tSeqsElements);
	}else{
		tokens.set(22,tSeqsElements);
	}

        return tokens;
    }

}
