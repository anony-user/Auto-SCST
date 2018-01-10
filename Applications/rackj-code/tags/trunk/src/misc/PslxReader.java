package misc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class PslxReader extends BlatReader implements FineMappingResultIterator {

    public static String methodName = "PSLX";

    public PslxReader(String pslxFilename) throws FileNotFoundException {
        super(pslxFilename);
    }

    protected FineAlignmentRecord getAlignmentRecord(String line,ReadInfo readInfo) throws Exception {

        AlignmentRecord record = super.getAlignmentRecord(line,readInfo);

        String[] tokens = line.split("\t");
        String subTokens[];

        int idx;

        // 17: block
        idx = 17;
        int numBlocks = Integer.parseInt(tokens[idx]);

        // 21: qSeq
        idx = 21;
        subTokens = tokens[idx].split(",");
        String qSeqs[] = new String[numBlocks];
        for(int i=0;i<numBlocks;i++){
        	qSeqs[i] = subTokens[i];
        }

        // 22: tSeq
        idx = 22;
        String tSeqs[];
        if(tokens.length>22){
          subTokens = tokens[idx].split(",");
          tSeqs = new String[numBlocks];
          for (int i = 0; i < numBlocks; i++) {
            tSeqs[i] = subTokens[i];
          }
        }else{ // for PSLY format
          tSeqs = new String[0];
        }

        // notice the last parameter blockSizes.clone()
        return new FineAlignmentRecord(record.identity, record.numBlocks,
                                       record.qStarts.clone(),
                                       record.qBlockSizes.clone(),
                                       record.chrOriginal,
                                       record.tStarts.clone(),
                                       record.tBlockSizes.clone(),
                                       record.getStrand(),
                                       qSeqs, tSeqs,
                                       line, readInfo);
    }

    static public void main(String args[]) throws IOException {
        for(PslxReader iterator = new PslxReader(args[0]);iterator.hasNext();){
            ArrayList mappingRecords = (ArrayList) iterator.next();
            for(Iterator recordIterator = mappingRecords.iterator();recordIterator.hasNext();){
                FineAlignmentRecord far = (FineAlignmentRecord) recordIterator.next();
                Set snpSet = far.getSNPs();
                for(Iterator snpIterator = snpSet.iterator();snpIterator.hasNext();){
                    GenomeInterval snp = (GenomeInterval) snpIterator.next();
                    System.out.println(iterator.getReadID() + " : " + snp);
                }
            }
        }
    }
}

