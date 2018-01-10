package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

public class AlignmentRecord {
	
	// constants
	public static boolean ATTACH_QUERY_SEQ_YES = true; 
	public static boolean ATTACH_QUERY_SEQ_NO = true; 
	
	//
    public float identity;
    public int numBlocks;
    public int qStarts[];
    public int qBlockSizes[];
    public String chr;
    public String chrOriginal;
    public int tStarts[];
    public int tBlockSizes[];
    public boolean forwardStrand = true;
    public Object sourceObj = null;
    public ReadInfo readInfo = null;

    // backup for resetting alignment
    private int numBlocksBck;
    private int qStartsBck[];
    private int tStartsBck[];
    private int blockSizesBck[];
    
    //
    private boolean reverseTranslated = false;

    /**
     * @deprecated newer methods require ReadInfo object
     */
    public AlignmentRecord(float identity,int numBlocks,int qStarts[],int qBlockSizes[],String chrOriginal,int tStarts[],int tBlockSizes[],String strand){
      this(identity,numBlocks,qStarts,qBlockSizes,chrOriginal,tStarts,tBlockSizes,strand,null,null);
    }

    public AlignmentRecord(float identity,int numBlocks,int qStarts[],int qBlockSizes[],String chrOriginal,int tStarts[],int tBlockSizes[],String strand,Object srcObj,ReadInfo readInfo){
        this.identity = identity;
        this.numBlocks = numBlocks;
        this.qStarts = qStarts;
        this.qBlockSizes = qBlockSizes;
        this.chr = chrOriginal.toLowerCase().intern();
        this.chrOriginal = chrOriginal.intern();
        this.tStarts = tStarts;
        this.tBlockSizes = tBlockSizes;
        if(strand.equals("-")){
            this.forwardStrand = false;
        }else{
            this.forwardStrand = true;
        }

        // backup
        this.numBlocksBck = numBlocks;
        this.qStartsBck = qStarts.clone();
        this.tStartsBck = tStarts.clone();
        this.blockSizesBck = qBlockSizes.clone();

        this.sourceObj = srcObj;
        
        this.readInfo = readInfo;
    }

    public String getStrand(){
        if(forwardStrand==true){
          return "+";
        }else{
          return "-";
        }
    }

    /**
     * reset the alignment blocks which might have been modified by nearbyJoin().
     */
    public void reset(){
        this.numBlocks = this.numBlocksBck;
        this.qStarts = this.qStartsBck.clone();
        this.qBlockSizes = this.blockSizesBck.clone();
        this.tStarts = this.tStartsBck.clone();
        this.tBlockSizes = this.blockSizesBck.clone();
    }

    /**
     * @return A set of target-side intervals.
     */
    public Set getMappingIntervals(){
        return getMappingIntervals(true);
    }

    /**
     * Get alignment mapping blocks in Intervals.
     * @param targetSide true if lookcing for target-side intervals; otherwise (query-side), false.
     * @return A set of intervals.
     */
    public Set<Interval> getMappingIntervals(boolean targetSide){
        Set<Interval> ansSet = new TreeSet<Interval>();

        if(targetSide){
            for(int i=0;i<numBlocks;i++){
                ansSet.add(new Interval(tStarts[i],tStarts[i]+tBlockSizes[i]-1));
            }
        }else{
            for(int i=0;i<numBlocks;i++){
                ansSet.add(new Interval(qStarts[i],qStarts[i]+qBlockSizes[i]-1));
            }
        }

        return ansSet;
    }
    
    public int getAlignmentStart(boolean targetSide){
    	if(targetSide){
    		return tStarts[0];
    	}else{
    		return qStarts[0];
    	}
    }
    
    public int getAlignmentStop(boolean targetSide){
    	if(targetSide){
    		return tStarts[numBlocks-1] + tBlockSizes[numBlocks-1] -1;
    	}else{
    		return qStarts[numBlocks-1] + qBlockSizes[numBlocks-1] -1;
    	}
    }

    /**
     * Get a list of alignment blocks.
     * @return A list of AlignmentBlock objects
     */
    public List<AlignmentBlock> getAlignmentBlockList(){
        List<AlignmentBlock> blockList = new ArrayList<AlignmentBlock>();
        for (int i=0; i < numBlocks; i++ ) {
            Interval qBlockInterval = new Interval(qStarts[i],qStarts[i]+qBlockSizes[i]-1);
            GenomeInterval tBlockInterval = new GenomeInterval(chr,tStarts[i],tStarts[i]+tBlockSizes[i]-1);
            String Strand = getStrand();
            blockList.add(new AlignmentBlock(Strand,qBlockInterval,tBlockInterval,this.chrOriginal));
        }
      return blockList;
    }

    /**
     * Sometimes we may need to join two blocks that were due to very small insertion/deletion
     * @param joinFactor int the distance required to join two nearby blocks
     */
    public void nearbyJoin(int joinFactor){
        if(joinFactor>0){
            while(nearbyJoinDoit(joinFactor));
        }
        return;
    }

    private boolean nearbyJoinDoit(int joinFactor){
        for(int i=0;i<numBlocks-1;i++){
            int thisQStop = qStarts[i]+qBlockSizes[i]-1;
            int thisTStop = tStarts[i]+tBlockSizes[i]-1;
            int nextQStart = qStarts[i+1];
            int nextTStart = tStarts[i+1];
            // distance (unmapped base-pairs) in reference/query side LESS than join factor
            if((nextQStart-thisQStop)<=joinFactor && (nextTStart-thisTStop)<=joinFactor){
                // modify this
                qBlockSizes[i] = qStarts[i+1]+qBlockSizes[i+1]-qStarts[i];
                tBlockSizes[i] = tStarts[i+1]+tBlockSizes[i+1]-tStarts[i];
                // reduce numBlocks by one
                numBlocks--;
                // modify rest
                for(int j=i+1;j<numBlocks;j++){
                    qStarts[j] = qStarts[j+1];
                    qBlockSizes[j] = qBlockSizes[j+1];
                    tStarts[j] = tStarts[j+1];
                    tBlockSizes[j] = tBlockSizes[j+1];
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Compute mapping identity: matches divided by total mapped length
     * @param readLength read length
     * @return mapping identity
     * @deprecated ReadInfo object gives read length, so no parameters are needed
     */
    public float getMappingIdentity(int readLength){
        int originalIdentity = Math.round(identity * readLength);
        int sum_blocksize = 0;
        for (int i = 0; i< numBlocks; i++) {
            sum_blocksize += qBlockSizes[i];
        }
        return ((float)originalIdentity)/sum_blocksize;
    }

    /**
     * Compute mapping identity: matches divided by total mapped length
     * @return mapping identity
     */
    public float getMappingIdentity(){
        int originalIdentity = Math.round(identity * readInfo.getReadLength());
        int sum_blocksize = 0;
        for (int i = 0; i< numBlocks; i++) {
            sum_blocksize += qBlockSizes[i];
        }
        return ((float)originalIdentity)/sum_blocksize;
    }
    
    /**
     * Compute alignment identity
     * @return alignment identity
     */
    public float getAlignmentIdentity(){
        int originalIdentity = Math.round(identity * readInfo.getReadLength());
        return ((float)originalIdentity)/getAlignmentLength();
    }
    
    public int getAlignmentLength(){
    	return getAlignmentLength(true);
    }
    
    public int getAlignmentLength(boolean includeGaps){
        int alignment_size = 0;
        //add block sizes
        for (int i = 0; i< numBlocks; i++) {
        	alignment_size += qBlockSizes[i];
        }
        //add gap sizes
        if(includeGaps){
        	for(int i=0;i<numBlocks-1;i++){
        		alignment_size += qStarts[i+1]-(qStarts[i]+qBlockSizes[i]);
        		alignment_size += tStarts[i+1]-(tStarts[i]+tBlockSizes[i]);
        	}
        }
        return alignment_size;
    }
    
    public String toBLAT(boolean attachQuerySeq){
    	return toBLAT(readInfo.getReadID(),readInfo.getReadLength(),attachQuerySeq);
    }
    
    public String toBLAT(){
    	return toBLAT(readInfo.getReadID(),readInfo.getReadLength(),false);
    }

    public String toBLAT(String readID,int readLength){
    	return toBLAT(readID,readLength,false);
    }
    
    public String toBLAT(String readID,int readLength,boolean attachQuerySeq){
    	ArrayList<String> tokens = getBLATtokens(readID, readLength, attachQuerySeq);
    	
    	String blatLine = "";
    	for(Iterator iterator=tokens.iterator(); iterator.hasNext(); ){
    		String token = (String) iterator.next();
    		blatLine += token;
    		if(iterator.hasNext()){
    			blatLine += "\t";
    		}
    	}
    	
    	return blatLine;
    }

    protected ArrayList<String> getBLATtokens(String readID,int readLength,boolean attachQuerySeq){

      //convert identity from ratio type into original type
       int originalIdentity = Math.round(identity * readLength);

      //convert strand info. into signal
      String strandSignal;
      if(forwardStrand==true){
        strandSignal = "+";
      }else{
        strandSignal = "-";
      }

      int qStartPos = 0;
      int qEndPos = 0;
      int tEndPos = 0;
      if(forwardStrand){
          qStartPos = qStarts[0]-1;
          qEndPos = qStarts[numBlocks-1]+qBlockSizes[numBlocks-1]-1;
          tEndPos = tStarts[numBlocks-1]+tBlockSizes[numBlocks-1]-1;
      }else{
          qStartPos = qStarts[0]-1;
          qEndPos = qStarts[numBlocks-1]+qBlockSizes[numBlocks-1]-1;
          int tmp = qStartPos;
          qStartPos = readLength - qEndPos;
          qEndPos = readLength - tmp;
          tEndPos = tStarts[numBlocks-1]+tBlockSizes[numBlocks-1]-1;
      }

      //load qBlockSize info. from qBlockSizes[]
      String blockSizesElement = "";
      for(int i=0;i<numBlocks;i++){
        blockSizesElement += Integer.toString(tBlockSizes[i])+",";
      }

      //load qStarts info. from qStarts[]
      String qStartsElements = "";
      int qBlockSum = 0;
      for (int i = 0; i < numBlocks; i++) {
          qStartsElements += Integer.toString(qStarts[i] - 1) + ",";
          qBlockSum += this.qBlockSizes[i];
      }

      //load tStarts info. from tStarts[]
      String tStartsElements = "";
      for (int i = 0; i < numBlocks; i++) {
          tStartsElements += Integer.toString(tStarts[i] - 1) + ",";
      }

      //BLAT TOKENS
      ArrayList<String> tokens = new ArrayList<String>();
      
      // 0: matches
      tokens.add(Integer.toString(originalIdentity));
      // 1: misMatches
      tokens.add( Integer.toString( ((readLength - originalIdentity) - (readLength - qBlockSum)) ) );
      // 2: repMatches
      tokens.add("0");
      // 3: nCount
      tokens.add("0");
      // 4: qNumInsert
      tokens.add("0");
      // 5: qBaseInsert
      tokens.add("0");
      // 6: tNumInsert
      tokens.add("0");
      // 7: tBaseInsert
      tokens.add("0");
      // 8: strand
      tokens.add(strandSignal);
      // 9: qName
      tokens.add(readID);
      // 10: qSize
      tokens.add(Integer.toString(readLength));
      // 11: qStart
      tokens.add(Integer.toString(qStartPos));
      // 12: qEnd
      tokens.add(Integer.toString(qEndPos));
      // 13: tName
      tokens.add(chrOriginal);
      // 14: tSize
      tokens.add(Integer.toString(getTargetLength()));
      // 15: tStart
      tokens.add(Integer.toString((tStarts[0] - 1)));
      // 16: tEnd
      tokens.add(Integer.toString(tEndPos));
      // 17: blockCount
      tokens.add(Integer.toString(numBlocks));
      // 18: blockSizes
      tokens.add(blockSizesElement);
      // 19: qStarts
      tokens.add(qStartsElements);
      // 20: tStarts
      tokens.add(tStartsElements);

      if(attachQuerySeq && this.getQuerySeq()!=null){
          tokens.add("");
          tokens.add("");
          tokens.add(this.getQuerySeq());
      }
      
      return tokens;
    }

    public AlignmentRecord translate(CanonicalGFF cgff){
    	return translate(cgff,this.readInfo.getReadLength());
    }

    
    public boolean isReverseTranslated(){
    	return reverseTranslated;
    }

    private void setReverseTranslated(boolean val){
    	reverseTranslated = val;
    }
    
	/**
	 * transcript to genome location translation
	 */
	public AlignmentRecord translate(CanonicalGFF cgff, int readLength) {
          if(cgff.geneLengthMap.containsKey(this.chrOriginal)==false){
            return null;
          }

    	final boolean transcriptForwardStrand = (cgff.getStrand(this.chrOriginal).equals("+"))? true : false;
		TreeSet exonRegion = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				Interval i1 = (Interval) o1;
				Interval i2 = (Interval) o2;

				if (transcriptForwardStrand) {
					return i1.compareTo(i2);
				} else {
					return i2.compareTo(i1);
				}
			}

			public boolean equals(Object obj) {
				return false;
			}
		});
		exonRegion.addAll((TreeSet) cgff.geneExonRegionMap.get(this.chrOriginal));

        int transVeryStart = tStarts[0];
        int transVeryStop  = tStarts[numBlocks-1]+tBlockSizes[numBlocks-1]-1;

		// establish gtbMap
		Map genomeTranscriptBlockMap = new HashMap();
		int pos = 0;
		for (Iterator intervalItr = exonRegion.iterator(); intervalItr.hasNext();) {
			Interval exonBlock = (Interval) intervalItr.next();

			int transStart = pos + 1;
			int transStop = pos + exonBlock.length();

			int refPosition;
			if (transcriptForwardStrand) {
				refPosition = transStart;
			} else {
				refPosition = transStop;
			}

			Interval transBlock = new Interval(transStart, transStop, refPosition);
			pos += exonBlock.length();

                        if(transBlock.intersect(transVeryStart,transVeryStop)){
                          genomeTranscriptBlockMap.put(exonBlock, transBlock);
                        }
		}

		// transform
		Map resultMap = new TreeMap();
		List blockList = getAlignmentBlockList();
		for (int i = 0; i < blockList.size(); i++) {
			AlignmentBlock alnBlock = (AlignmentBlock) blockList.get(i);

			Iterator iter = genomeTranscriptBlockMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				Interval gExonBlock = (Interval) entry.getKey();
				Interval transBlock = (Interval) entry.getValue();

				if (alnBlock.refBlock.intersect(transBlock)) {
					int refPositionTrans = (Integer) transBlock.getUserObject();

					// translate the alignment block on transcript to genome
					Interval alnBlockGenome = alnBlock.refBlock.translate(refPositionTrans,gExonBlock.getStart(),transcriptForwardStrand,true);
					// trim if crossing exon boundary
					alnBlockGenome = alnBlockGenome.constrain(gExonBlock);

					// trim alignment block on transcript if crossing exon boundary
					Interval alnBlockTrans = alnBlock.refBlock.constrain(transBlock);
					// translate the trimmed alignment block to read
					Interval alnBlockRead = alnBlockTrans.translate(alnBlock.refBlock.getStart(), alnBlock.readBlock.getStart(), true, true);

					resultMap.put(alnBlockGenome, alnBlockRead);
				}
			}
		}

		// return a new AlignmentRecord
		String strand = (forwardStrand ^ transcriptForwardStrand) ? "-" : "+";
		int numBlocksNew = resultMap.size();
		int[] qStartsNew = new int[numBlocksNew];
		int[] tStartsNew = new int[numBlocksNew];
		int[] tBlockSizesNew = new int[numBlocksNew];
		int[] qBlockSizesNew = new int[numBlocksNew];

		int i = 0;
		Iterator blockItr = resultMap.entrySet().iterator();
		while (blockItr.hasNext()) {
			Map.Entry entry = (Map.Entry) blockItr.next();
			Interval tBlock = (Interval) entry.getKey();
			Interval qBlock = (Interval) entry.getValue();

			qStartsNew[i] = (transcriptForwardStrand) ? qBlock.getStart() : (readLength-qBlock.getStop()+1);
			qBlockSizesNew[i] = qBlock.length();
			tStartsNew[i] = tBlock.getStart();
			tBlockSizesNew[i] = tBlock.length();

			i++;
		}

		AlignmentRecord newAln = new AlignmentRecord(identity, numBlocksNew, qStartsNew, qBlockSizesNew, cgff.getChrOriginal(this.chrOriginal), tStartsNew,
				tBlockSizesNew, strand, this, readInfo);
		
		if(transcriptForwardStrand==false){
			newAln.setReverseTranslated(true);
		}
		
		return newAln;
	}

	// TODO: got to handle 24th token of extended PSL format
	public String getQuerySeq(){
		Object verySourceObj;
		Object record = this;
		
		int strandVec = 1;
		
		while(record!=null && record instanceof AlignmentRecord){

			AlignmentRecord aln = (AlignmentRecord) record;
			if( aln.isReverseTranslated() ){
				strandVec *= -1;
			}
			
			record = ((AlignmentRecord) record).sourceObj;
		}
		
		// only sourceObj in SAM can give query sequence
		if(record!=null && record instanceof SAMRecord){
			SAMRecord samREC = (SAMRecord) record;
            if (samREC.getReadString().equals(SAMRecord.NULL_SEQUENCE_STRING)){
            	return null;
            }else{
            	if(strandVec>0){
	    			return "+"+samREC.getReadString();
            	}else{
	    			return "-"+samREC.getReadString();
            	}
            }
		}else{
			return null;
		}
	}
	
	public int getTargetLength(){
		
		if(this.sourceObj==null) return 0;
		
		if(this.sourceObj instanceof String){ // PSL or PSLX format
			return Integer.valueOf(Util.getIthField((String) this.sourceObj,14));
		}else if(this.sourceObj instanceof SAMRecord){
			SAMFileHeader header = ((SAMRecord) this.sourceObj).getHeader();
			if(header==null) return 0;
			
			SAMSequenceRecord targetRecord = header.getSequence(chrOriginal);
			if(targetRecord==null) return 0;
			
			return targetRecord.getSequenceLength();
		}
		
		return 0;
	}
	
	public int getQueryLength(){
		return readInfo.getReadLength();
	}

    public int hashCode(){
		int result = 0;

		result = result + Arrays.hashCode(Arrays.copyOf(tStarts, numBlocks));
		result = result + Arrays.hashCode(Arrays.copyOf(tBlockSizes, numBlocks));
		result = result + Arrays.hashCode(Arrays.copyOf(qStarts, numBlocks));
		result = result + chr.hashCode();
		result = result + readInfo.hashCode();

		return result;
	}

	public boolean equals(Object obj){
		if(obj==null){
			throw(new NullPointerException());
		}

		if (obj instanceof AlignmentRecord) {
			AlignmentRecord other = (AlignmentRecord) obj;
			if (chr.equals(other.chr) &&
			    readInfo.equals(other.readInfo) &&
			    forwardStrand==other.forwardStrand &&
                Arrays.equals(Arrays.copyOf(tStarts, numBlocks), Arrays.copyOf(other.tStarts, numBlocks)) &&
                Arrays.equals(Arrays.copyOf(qStarts, numBlocks), Arrays.copyOf(other.qStarts, numBlocks)) &&
                Arrays.equals(Arrays.copyOf(tBlockSizes, numBlocks), Arrays.copyOf(other.tBlockSizes, numBlocks)) &&
                Arrays.equals(Arrays.copyOf(qBlockSizes, numBlocks), Arrays.copyOf(other.qBlockSizes, numBlocks))) {
                          return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
