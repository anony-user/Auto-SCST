package misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.samtools.SAMRecord;

/**
 * Just a wrapper class of SAMRecord (picard), for storing necessary functions for
 * RackJ.
 */
public class SAMRecordWrapper {

  private SAMRecord samRecord = null;

  private int matchMismatchSum = 0;
  private int qShift = 0;
  private int readLen = 0;
  private int cigarXCnt = 0;

  public SAMRecordWrapper(SAMRecord samRecord) {
    this.samRecord = samRecord;
    runCigerProperties();
  }

  public SAMRecord getSAMRecord(){
    return samRecord;
  }

  /**
   * Compute
   * 1. actual read length
   * 2. qShift
   * 3. mismatches in CIGAR
   */
  private void runCigerProperties() {
    int patternOrder = 0;
    String CigerString = samRecord.getCigarString();
    
    if(CigerString.equals(SAMRecord.NO_ALIGNMENT_CIGAR)) {
  	  readLen = samRecord.getReadLength();
  	  return;
  	}
    
    Pattern pattern = Pattern.compile("\\d+[I|M|S|H|=|X]");
    Matcher matcher = pattern.matcher(CigerString);
    String matchCI = "";
    while (matcher.find()) {
      patternOrder++;
      matchCI = matcher.group();
      Character o = matchCI.charAt(matchCI.length() - 1);
      if (o.equals('H') && patternOrder == 1) qShift = Integer.parseInt(matchCI.substring(0, matchCI.length() - 1));
      if (o.equals('M')||o.equals('=')) {
          matchMismatchSum += Integer.parseInt(matchCI.substring(0, matchCI.length() - 1));
      } else if (o.equals('X')) {
          matchMismatchSum += Integer.parseInt(matchCI.substring(0, matchCI.length() - 1));
          cigarXCnt += Integer.parseInt(matchCI.substring(0, matchCI.length() - 1));
      }
      readLen += Integer.parseInt(matchCI.substring(0, matchCI.length() - 1));
    }
  }

  public int getMatchMismatchSum() {
    return matchMismatchSum;
  }

  public int getReadLen() {
    return readLen;
  }

  public int getMismatchNum() {
      int mismatch = 0;
      if (cigarXCnt > 0) {
          mismatch = cigarXCnt;
      } else if (samRecord.getAttribute("XM") != null) {
          mismatch = samRecord.getIntegerAttribute("XM");
      } else if (samRecord.getAttribute("NM") != null) {
          mismatch = samRecord.getIntegerAttribute("NM");
      }
      return mismatch;
  }

  /**
   * @deprecated
   */
  public int[] getqStarts() {
    int numBlocks = samRecord.getAlignmentBlocks().size();
    int[] qStarts = new int[numBlocks];
    for (int i = 0; i < numBlocks; i++) {
      qStarts[i] = (Integer) samRecord.getAlignmentBlocks().get(i).getReadStart() + qShift;
    }
    return qStarts;
  }

  public static int infoArrayNumBlocks=0;
  public static int infoArrayBlockSizes=1;
  public static int infoArrayQStarts=2;
  public static int infoArrayTStarts=3;

  private ArrayList<Object> thisAlignmentInfo=null;

  /**
   * @return HashMap for (1) number of blocks, (2) block sizes, (3) qStarts, and (4) tStarts.
   */
	public ArrayList<Object> getAlignmentInfo() {

		if (thisAlignmentInfo != null)
			return thisAlignmentInfo;

		int numRawBlocks = samRecord.getAlignmentBlocks().size();
		List<Integer> qStartList = new ArrayList<Integer>();
		List<Integer> tStartList = new ArrayList<Integer>();
		List<Integer> blocksizeList = new ArrayList<Integer>();
		int blocksize = 0;
		int numBlocks = 0;
		for (int i = 0; i < numRawBlocks; i++) {
			if (i == 0) {
				numBlocks = 1;
				qStartList.add(samRecord.getAlignmentBlocks().get(i).getReadStart()	+ qShift);
				tStartList.add(samRecord.getAlignmentBlocks().get(i).getReferenceStart());
				blocksize = samRecord.getAlignmentBlocks().get(i).getLength();
			} else {
				// collect block information
				int qStart = samRecord.getAlignmentBlocks().get(i).getReadStart();
				int tStart = samRecord.getAlignmentBlocks().get(i).getReferenceStart();
				int prev_qEnd = samRecord.getAlignmentBlocks().get(i - 1).getReadStart()
						+ samRecord.getAlignmentBlocks().get(i - 1).getLength()	- 1;
				int prev_tEnd = samRecord.getAlignmentBlocks().get(i - 1).getReferenceStart()
						+ samRecord.getAlignmentBlocks().get(i - 1).getLength()	- 1;

				// decide alignment block
				if (qStart == prev_qEnd + 1 && tStart == prev_tEnd + 1) {
					blocksize += samRecord.getAlignmentBlocks().get(i).getLength();
				} else {
					numBlocks++;
					qStartList.add(qStart + qShift);
					tStartList.add(tStart);
					blocksizeList.add(blocksize); // add previous block size
					blocksize = samRecord.getAlignmentBlocks().get(i).getLength();
				}
			}
			if (i == numRawBlocks - 1)
				blocksizeList.add(blocksize); // complement last block size
		}
		int[] qStarts = new int[numBlocks];
		int[] tStarts = new int[numBlocks];
		int[] blocksizes = new int[numBlocks];
		for (int i = 0; i < numBlocks; i++) {
			blocksizes[i] = blocksizeList.get(i).intValue();
			qStarts[i] = qStartList.get(i).intValue();
			tStarts[i] = tStartList.get(i).intValue();
		}

		ArrayList<Object> returnVals = new ArrayList<Object>();
		returnVals.add(numBlocks);
		returnVals.add(blocksizes);
		returnVals.add(qStarts);
		returnVals.add(tStarts);
		thisAlignmentInfo = returnVals;

		return returnVals;
	}

  private String[] thisQSeqs=null;

  /**
   * @return String[] an array of query block sequences, null for no supporting information (SEQ is "*").
   */
	public String[] getQSeqs() {
          if(thisQSeqs!=null) return thisQSeqs;

		if (samRecord.getReadString().equals(SAMRecord.NULL_SEQUENCE_STRING)) return null;
		String readSEQ = samRecord.getReadString();
		ArrayList<Object> AlignmentInfo = getAlignmentInfo();
		int numBlocks = (Integer) AlignmentInfo.get(this.infoArrayNumBlocks);
		String qSeqs[] = new String[numBlocks];
		int blockSizes[] = (int[]) AlignmentInfo.get(this.infoArrayBlockSizes);
		int qStarts[] = (int[]) AlignmentInfo.get(this.infoArrayQStarts);

		for (int i = 0; i < numBlocks; i++) {
			// qShift fix
			qSeqs[i] = readSEQ.substring(qStarts[i]-qShift - 1, qStarts[i]-qShift + blockSizes[i] - 1);
		}
                thisQSeqs=qSeqs;

		return qSeqs;
	}

  /**
   * @return String[] an array of target block sequences, null for no supporting information (SEQ is "*" or no MD).
   */
	public String[] getTSeqs() {
		String MDtag = (String) samRecord.getAttribute("MD");
		String qSeqs[] = getQSeqs();
		if (MDtag == null || qSeqs == null) return null;

		ArrayList<Object> AlignmentInfo = getAlignmentInfo();
		int numBlocks = (Integer) AlignmentInfo.get(SAMRecordWrapper.infoArrayNumBlocks);
		int blockSizes[] = (int[]) AlignmentInfo.get(this.infoArrayBlockSizes);
		String SeqsMatchblocks = "";
		for (int i = 0; i < numBlocks; i++) {
			SeqsMatchblocks += qSeqs[i];
		}
		Pattern pattern = Pattern.compile("(\\d+|[A-Z]|\\^[A-Z]+)");
		Matcher matcher = pattern.matcher(MDtag.toUpperCase());
		String MDelement = "";
		String refSEQ = "";
		int qIDX = 0;
		while (matcher.find()) {
			MDelement = matcher.group();
			if (MDelement.matches("\\d+")) {
				refSEQ += SeqsMatchblocks.substring(qIDX,qIDX + Integer.parseInt(MDelement));
				qIDX += Integer.parseInt(MDelement);
			} else if (MDelement.matches("[A-Z]")) {
				refSEQ += MDelement;
				qIDX++;
			}
		}

		String tSeqs[] = new String[numBlocks];
		int tBlockStart = 1;
		for (int i = 0; i < numBlocks; i++) {
			tSeqs[i] = refSEQ.substring(tBlockStart - 1, tBlockStart - 1 + blockSizes[i]);
			tBlockStart += blockSizes[i];
		}
		return tSeqs;
	}
}

