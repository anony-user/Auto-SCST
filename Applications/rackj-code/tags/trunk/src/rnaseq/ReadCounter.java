package rnaseq;

import java.util.Collection;

import misc.AlignmentRecord;
import misc.CanonicalGFF;


public interface ReadCounter {
    public void countReadUnique(String readID,AlignmentRecord record,Number cnt,String geneID,CanonicalGFF cgff);

    public void countReadMulti(String readID,Collection recordCollection,Number cnt,String geneID,CanonicalGFF cgff);

    public void report(String filename,CanonicalGFF cgff);
}
