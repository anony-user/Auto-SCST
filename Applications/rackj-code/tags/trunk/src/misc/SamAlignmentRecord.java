package misc;

import net.sf.samtools.SAMRecord;

/**
 * @deprecated
 */
public class SamAlignmentRecord extends FineAlignmentRecord{

    private SAMRecord samRecord = null;

    public SamAlignmentRecord(float identity, int numBlocks, int qStarts[],
                               int qBlockSizes[], String chrOriginal, int tStarts[],
                               int tBlockSizes[], String strand,
                               String qSeqs[], String tSeqs[], SAMRecord samRecord) {
        super(identity, numBlocks, qStarts, qBlockSizes, chrOriginal, tStarts, tBlockSizes,
              strand, qSeqs, tSeqs);
        this.samRecord = samRecord;
    }

    public String toSAM(){
        // output a SAM line exclude newline
    	String SamLine = samRecord.getSAMString().trim();
        return SamLine;
    }
}
