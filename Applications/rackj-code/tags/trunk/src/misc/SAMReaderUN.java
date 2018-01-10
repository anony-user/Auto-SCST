package misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public class SAMReaderUN extends SAMReader {
	public static String methodName = "SAMun";
	private List<ArrayList> nextReadRecordLists = new ArrayList();
	
	public SAMReaderUN(String samFilename) throws FileNotFoundException {
		File inF = new File(samFilename);
		ndataFile = new SAMFileReader(inF);
		// set ValidationStringency SILENT to avoid program stop due to
		// Reference sequence NAME empty or other file format error
		ndataFile.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
		iteratorN = ndataFile.iterator();
		fetchNextRecord();
	}
	
	private void fetchNextQName() {
		boolean isPaired = nextRec.getSAMRecord().getReadPairedFlag();
		ReadInfo[] readInfos = {null,null};
		
		nextReadRecordLists = new ArrayList<ArrayList>();
		nextReadRecordLists.add(new ArrayList<AlignmentRecord>());
		if(isPaired){
			nextReadRecordLists.add(new ArrayList<AlignmentRecord>());
		}
		
		readID = nextRec.getSAMRecord().getReadName();
        readLen= nextRec.getReadLen();

        // decide index
		int idx = 0;
		if(isPaired && nextRec.getSAMRecord().getSecondOfPairFlag())
			idx = 1;

		if(readInfos[idx]==null){
			readInfos[idx] = new ReadInfo(readID,readLen,idx);
		}
		
		int score = nextRec.getMatchMismatchSum() - nextRec.getMismatchNum();

		nextReadRecordLists.get(idx).add(getAlignmentRecord(nextRec,score,readInfos[idx]));

		// keep getting records of the same QNAME
		while (fetchNextRecord() != null) {
			String nextReadID = nextRec.getSAMRecord().getReadName();
			if(!nextReadID.equals(readID))
				break;

	        readLen= nextRec.getReadLen();
			
	        // decide index
			idx = 0;
			if(isPaired && nextRec.getSAMRecord().getSecondOfPairFlag())
				idx = 1;

			if(readInfos[idx]==null){
				readInfos[idx] = new ReadInfo(readID,readLen,idx);
			}
			
			score = nextRec.getMatchMismatchSum() - nextRec.getMismatchNum();

			nextReadRecordLists.get(idx).add(getAlignmentRecord(nextRec,score,readInfos[idx]));
		}
		
		// remove empty list
		for(int i=nextReadRecordLists.size()-1; i >=0 ; i--){
			if(nextReadRecordLists.get(i).size()==0){
				nextReadRecordLists.remove(i);
			}
		}
	}
	
	private int numMatch = 0;
	private float bestIdentity = 0;
	
	/**
	 * Set readID, readLen, bestScore, and numMatch for this iteration.
	 * Set readInfo to records in the list
	 * @param recList
	 */
	private void setMisc(ArrayList<AlignmentRecord> recList){
		AlignmentRecord firstRec = recList.get(0);
		this.readID= firstRec.readInfo.getReadID();
		this.readLen = firstRec.readInfo.getReadLength();
		
		numMatch = 0;
		bestIdentity = 0;
		for(int i=0;i<recList.size();i++){
			if(recList.get(i).identity>0){
				numMatch++;
			}
			if(recList.get(i).identity>bestIdentity){
				bestIdentity = recList.get(i).identity;
			}
		}
	}
	
    public float getBestIdentity() {
        return bestIdentity;
    }

    public int getNumMatch() {
        return numMatch;
    }
	
	private SAMRecordWrapper fetchNextRecord() {
		nextRec = null;
		if (iteratorN.hasNext()) {
			try {
				nextRec = new SAMRecordWrapper((SAMRecord)iteratorN.next());
			} catch (Exception parseEx) {
				System.out.println(parseEx.toString());
			}
		}
		return nextRec;
	}
	
	protected AlignmentRecord getAlignmentRecord(SAMRecordWrapper record,int score,ReadInfo readInfo) {
		AlignmentRecord rec = super.getAlignmentRecord(record, score, readInfo);

		// null means an alignment record without alignment
		if(rec == null){
			rec = new AlignmentRecord(0.0f,0,new int[0],new int[0],"",new int[0],new int[0],"",record.getSAMRecord(),readInfo);
		}

		return rec;
	}
	
	public boolean hasNext() {
		if(nextRec==null && nextReadRecordLists.size()==0){
			return false;
		}
		return true;
	}
	
	public Object next() {
		if(nextReadRecordLists.size()==0){
			fetchNextQName();
		}
		ArrayList nextReadRecords = (ArrayList) nextReadRecordLists.remove(0);
		setMisc(nextReadRecords);
		
		return nextReadRecords;
	}

    static public void main(String args[]) throws IOException {
            for (SAMReaderUN iterator = new SAMReaderUN(args[0]); iterator.hasNext();) {
                    ArrayList mappingRecords = (ArrayList) iterator.next();
                    System.out.println(iterator.getReadID() + "\t" +
                    					iterator.getNumMatch() + "\t" +
                    					mappingRecords.size() + "\t" +
                    					iterator.getBestIdentity());
            }
    }
}
