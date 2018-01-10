package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.Util;

public class SpliceCounter implements ReadCounter,ReadCounter2 {

    private boolean checkByContaining;
    private int minimumOverlap;

    private Map geneSpliceCntMap = new HashMap();
    private Map geneExonPairSplicingPosMap = new HashMap();
    public Map geneSpliceMapByModel = new HashMap();
    private CanonicalGFF geneModel = null;

    public SpliceCounter(CanonicalGFF geneModel,CanonicalGFF cgff,boolean checkByContaining,int minimumOverlap){
        this.geneModel = geneModel;
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;

        if(geneModel!=null){
            for(Iterator modelIterator = geneModel.geneExonRegionMap.keySet().iterator();modelIterator.hasNext();){
                String modelID = (String) modelIterator.next();
                GenomeInterval modelInterval = (GenomeInterval) geneModel.geneRegionMap.get(modelID);
                Set modelExonSet = (Set) geneModel.geneExonRegionMap.get(modelID);
                Set containingGenes = cgff.getRelatedGenes(modelInterval.getChr(),modelExonSet,true,true,1,true);
                for(Iterator geneIterator = containingGenes.iterator();geneIterator.hasNext();){
                    GenomeInterval geneRegion = (GenomeInterval) geneIterator.next();
                    String geneID = (String) geneRegion.getUserObject();
                    modelSpliceCounting(modelExonSet,geneID,cgff);
                }
            }
        }
    }

    private void modelSpliceCounting(Set intervalSet, String geneID, CanonicalGFF cgff){
        Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        // get hit exons
        Set hitExonInSerial = new TreeSet();
        int exonSn = 1;
        for(Iterator exonIterator = exonRegions.iterator();exonIterator.hasNext();){
            Interval exonInterval = (Interval) exonIterator.next();
            for (Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();) {
                Interval interval = (Interval) intervalIterator.next();
                if(exonInterval.contain(interval.getStart(),interval.getStop())){
                    hitExonInSerial.add(new Integer(exonSn));
                    break;
                }
            }
            exonSn++;
        }
        if(hitExonInSerial.size()<=1) return;

        // add counts for these hit exons
        Set spliceSet;
        if(geneSpliceMapByModel.containsKey(geneID)){
            spliceSet = (Set) geneSpliceMapByModel.get(geneID);
        }else{
            spliceSet = new TreeSet();
            geneSpliceMapByModel.put(geneID,spliceSet);
        }
        Integer exonNo[] = (Integer[]) hitExonInSerial.toArray(new Integer[hitExonInSerial.size()]);
        for(int i=0;i<exonNo.length-1;i++){
            Interval exonPair = new Interval(exonNo[i],exonNo[i+1]);
            spliceSet.add(exonPair);
        }
    }

    private void spliceCounting(AlignmentRecord record, Map geneSpliceCntMap, Number count, String geneID, CanonicalGFF cgff, boolean checkByContaining, int minimumOverlap){
        Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        // get hit exons
        Map exonPairSplicingPosRec = new HashMap();
        Interval[] exonArray = (Interval[]) exonRegions.toArray(new Interval[0]);
        int exonIndex=0;
        int blockIndex=0;
        int lastHitExon=-2;
        int lastHitBlock=-2;
        while(blockIndex<record.numBlocks && exonIndex<exonArray.length){
          Interval blockTarget = new Interval(record.tStarts[blockIndex],record.tStarts[blockIndex] + record.tBlockSizes[blockIndex]-1);
          Interval exonInterval = exonArray[exonIndex];
          boolean hitCheck = Util.blockCheck(exonInterval,blockTarget,exonIndex,exonArray.length,checkByContaining,minimumOverlap);

          if(hitCheck){
            // record the splicing position if an exon was hit previously AND (lastHitBlock and thisHitBlock are consecutive)
            if(blockIndex==lastHitBlock+1 && exonIndex>lastHitExon){
              Interval exonPair = new Interval(lastHitExon+1,exonIndex+1);
              exonPairSplicingPosRec.put(exonPair,record.qStarts[lastHitBlock]+record.qBlockSizes[lastHitBlock]-1);
            }
            lastHitExon=exonIndex;
            lastHitBlock=blockIndex;
          }

          // iteration
          if(blockTarget.getStop()<exonInterval.getStop()){
            blockIndex++;
          }else{
            exonIndex++;
          }
        }
        if(exonPairSplicingPosRec.isEmpty()) return;

        // add counts for these hit exons
        Map spliceCntMap;
        if(geneSpliceCntMap.containsKey(geneID)){
            spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
        }else{
            spliceCntMap = new TreeMap();
            geneSpliceCntMap.put(geneID,spliceCntMap);
        }
        for(Iterator exonPairIterator = exonPairSplicingPosRec.keySet().iterator();exonPairIterator.hasNext();){
            Interval exonPair = (Interval) exonPairIterator.next();
            if(spliceCntMap.containsKey(exonPair)){
                float oldValue = ((Number)spliceCntMap.get(exonPair)).floatValue();
                spliceCntMap.put(exonPair,new Float(oldValue+count.floatValue()));
            }else{
                spliceCntMap.put(exonPair,count);
            }
        }

        // update geneSplicePosMap
        Map exonPairSplicingPosMap;
        if(geneExonPairSplicingPosMap.containsKey(geneID)){
            exonPairSplicingPosMap = (Map) geneExonPairSplicingPosMap.get(geneID);
        }else{
            exonPairSplicingPosMap = new TreeMap();
            geneExonPairSplicingPosMap.put(geneID,exonPairSplicingPosMap);
        }
        for(Iterator iterator=exonPairSplicingPosRec.keySet().iterator();iterator.hasNext();){
            Interval exonPair = (Interval) iterator.next();
            int splicingPos = ((Integer)exonPairSplicingPosRec.get(exonPair)).intValue();
            Map splicingPosMap;
            if(exonPairSplicingPosMap.containsKey(exonPair)){
                splicingPosMap = (Map) exonPairSplicingPosMap.get(exonPair);
            }else{
                splicingPosMap = new TreeMap();
                exonPairSplicingPosMap.put(exonPair,splicingPosMap);
            }
            if(splicingPosMap.containsKey(splicingPos)){
                int val = ((Integer)splicingPosMap.get(splicingPos)).intValue() + 1;
                splicingPosMap.put(splicingPos,val);
            }else{
                splicingPosMap.put(splicingPos,1);
            }
        }
    }

    public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
        spliceCounting(record,geneSpliceCntMap,cnt,geneID,cgff,checkByContaining,minimumOverlap);
    }

    public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
    }

    public void report(String filename, CanonicalGFF cgff) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            // header
            fw.write("#GeneID" + "\t" +
                     "exonPair" + "\t" +
                     "#reads" + "\t" +
                     "jumping"
                    );
            if(geneModel==null){
            }else{
                fw.write("\t" + "novel");
            }
            fw.write("\t" + "splicingPosFreq");
            fw.write( "\t" +
                     "format:.spliceCount" + "\n");
            // contents
            // for each gene
            for(Iterator iterator = cgff.geneLengthMap.keySet().iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                Map spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
                Map exonPairSplicePosMap = (Map) geneExonPairSplicingPosMap.get(geneID);
                if(spliceCntMap==null) continue;
                // from model
                Set modelSpliceSet;
                if(geneModel==null){
                    modelSpliceSet = null;
                }else{
                    modelSpliceSet = (Set) geneSpliceMapByModel.get(geneID);
                }
                // iterate splice for this gene
                for(Iterator exonPairIterator = spliceCntMap.keySet().iterator();exonPairIterator.hasNext();){
                    Interval exonPair = (Interval) exonPairIterator.next();
                    float uniqCnt = ((Number)spliceCntMap.get(exonPair)).floatValue();
                    // geneID
                    fw.write(geneID + "\t");
                    // exon pair
                    fw.write(exonPair.getStart() + "<=>" + exonPair.getStop() + "\t");
                    // # reads
                    fw.write(new Float(uniqCnt).toString() + "\t");
                    // jumping
                    if(exonPair.getStop()-exonPair.getStart()>1){
                        fw.write("V");
                    }else{
                        fw.write(" ");
                    }
                    // check model (if given)
                    if(geneModel==null){
                    }else{
                        if(modelSpliceSet==null){
                            fw.write("\t" + "V");
                        }else if(modelSpliceSet.contains(exonPair)==false){
                            fw.write("\t" + "V");
                        }else{
                            fw.write("\t" + " ");
                        }
                    }
                    // splicingPosFreq
                    if(exonPairSplicePosMap.containsKey(exonPair)){
                        fw.write("\t" + exonPairSplicePosMap.get(exonPair));
                    }else{
                        fw.write("\t" + " ");
                    }
                    // line end
                    fw.write("\n");
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

	public void processUnique(String readID, AlignmentRecord record,
			Number cnt, String geneID, CanonicalGFF cgff) {
		spliceCounting(record,geneSpliceCntMap,cnt,geneID,cgff,checkByContaining,minimumOverlap);
	}

	public void processMulti(String readID, Map hitGeneAlignmentSetMap,
			Set geneIdSet, CanonicalGFF cgff) {
		// do nothing
	}

	public void finalize(Map<Set, Map<String, Double>> geneSetIdRatioMap) {
		// do nothing
	}
}
