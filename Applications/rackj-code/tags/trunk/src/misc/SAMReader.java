package misc;

/**
 * @author Chao-Yu Pan
 * @author Peter Tsai 20111115
 * @version 0.5
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;


public class SAMReader extends MappingResultIteratorAdaptor implements SamMappingResultIterator {
        public static String methodName = "SAM";
        protected SAMFileReader ndataFile;
        protected SAMRecordWrapper nextRec = null;
        protected ArrayList<AlignmentRecord> nextMappedReadRecords = new ArrayList<AlignmentRecord>();
        protected String readID = null;
        protected int bestScore = 0;
        protected int readLen = 0;
        protected Iterator iteratorN = null;

        public SAMReader(){}
        
        public SAMReader(String samFilename) throws FileNotFoundException {
                File inF = new File(samFilename);
                ndataFile = new SAMFileReader(inF);
                // set ValidationStringency SILENT to avoid program stop due to
                // Reference sequence NAME empty or other file format error
                ndataFile.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
                iteratorN = ndataFile.iterator();
                fetchNextSamRecordWithAlignment();
        }

        private void fetchNextMappedRead() {
                // get readID from current record
                readID = nextRec.getSAMRecord().getReadName(); // get read ID from nextRec
                readLen= nextRec.getReadLen();
                
                ReadInfo readInfo = new ReadInfo(readID,readLen);
                
                bestScore=0;

                int matchMismatchSum=nextRec.getMatchMismatchSum();
                int score = matchMismatchSum - nextRec.getMismatchNum();
                if (score > bestScore) bestScore = score;
                nextMappedReadRecords = new ArrayList<AlignmentRecord>();
                nextMappedReadRecords.add(getAlignmentRecord(nextRec,score,readInfo));

                // get rest lines with the same ID
                while (fetchNextSamRecordWithAlignment() != null) {
                        String nextReadID = nextRec.getSAMRecord().getReadName();
                        if (nextReadID.equals(readID)) {

                        	// update bestScore
                        	matchMismatchSum=nextRec.getMatchMismatchSum();
                        	score = matchMismatchSum - nextRec.getMismatchNum();
                        	if (score > bestScore) bestScore = score;
                        	nextMappedReadRecords.add(getAlignmentRecord(nextRec,score,readInfo));
                        } else {
                            break;
                        }
                }
        }

        private SAMRecordWrapper fetchNextSamRecordWithAlignment() {
                boolean parseAble = false;

                while (parseAble == false && iteratorN.hasNext()) {
                        parseAble = true;
                        try {
                                nextRec = new SAMRecordWrapper((SAMRecord)iteratorN.next());
                                parseAble =! nextRec.getSAMRecord().getReadUnmappedFlag();
                                // set parseAble FALSE if this SAMRecord got no alignment

                        } catch (Exception parseEx) {
                                System.out.println(parseEx.toString());
                                parseAble = false;
                        }
                }

                if (parseAble) {
                        return nextRec;
                } else { // iteratorN.hasNext()==false
                        nextRec = null;
                        return nextRec;
                }
        }

        protected AlignmentRecord getAlignmentRecord(SAMRecordWrapper record,int score,ReadInfo readInfo) {
                if (!(record.getSAMRecord().getCigarString().equals(SAMRecord.NO_ALIGNMENT_CIGAR))) {
                        // 0: match
                        float identity = (float) score /readLen;

                        // 8: strand
                        String strand = "+";
                        if (record.getSAMRecord().getReadNegativeStrandFlag())	strand = "-";

                        // 13: T name
                        String chr = record.getSAMRecord().getReferenceName().intern();

                        // get alignment information
                        ArrayList<Object> alignmentInfo = record.getAlignmentInfo();

                        // 17: block
                        int numBlocks = (Integer) alignmentInfo.get(record.infoArrayNumBlocks);

                        // 18: blockSizes
                        int blockSizes[] = (int[]) alignmentInfo.get(record.infoArrayBlockSizes);

                        // 19: qStarts
                        int qStarts[] = (int[]) alignmentInfo.get(record.infoArrayQStarts);

                        // 20: tStarts
                        int tStarts[] = (int[]) alignmentInfo.get(record.infoArrayTStarts);

                        // 21: qSeqs
                    	String qSeqs[] = record.getQSeqs();

                    	// 22: tSeqs
                    	String tSeqs[] = record.getTSeqs();

                    	// notice the last parameter blockSizes.clone()
                        if(qSeqs == null) {
                        	return new AlignmentRecord(identity, numBlocks,qStarts,blockSizes,chr,tStarts,blockSizes.clone(),strand,record.getSAMRecord(),readInfo);
                        } else if (tSeqs == null && qSeqs != null) {
                        	return new FineAlignmentRecord(identity, numBlocks,qStarts,blockSizes,chr,tStarts,blockSizes.clone(),strand,qSeqs,new String[0],record.getSAMRecord(),readInfo);
                        } else {
                        	return new FineAlignmentRecord(identity, numBlocks,qStarts,blockSizes,chr,tStarts,blockSizes.clone(),strand,qSeqs,tSeqs,record.getSAMRecord(),readInfo);
                        }
                } else {
                        return null;
                }

        }

        public String getReadID() {
                return readID;
        }

        public int getReadLength() {
                return readLen;
        }

        public float getBestIdentity() {
                return ( (float) bestScore / (float) readLen);
        }

        public int getNumMatch() {
                return nextMappedReadRecords.size();
        }

        public boolean hasNext() {
                if (nextRec == null) {
                        return false;
                } else {
                        return true;
                }
        }

        public Object next() {
                fetchNextMappedRead();
                return nextMappedReadRecords;
        }

        static public void main(String args[]) throws IOException {
                for (SAMReader iterator = new SAMReader(args[0]); iterator.hasNext();) {
                        ArrayList mappingRecords = (ArrayList) iterator.next();
                        System.out.println(iterator.getReadID() + "\t" + iterator.getNumMatch() + "\t"	+ iterator.getBestIdentity());
                }
        }
}
