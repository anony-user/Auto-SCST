package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.Interval;
import misc.Util;

public class IntronCounter extends ReadCounterMainAdaptor {
    protected void init1() {
        programName = "IntronCounter";
        useExonRegion = false;
    }

    private CanonicalGFF intronicCGFF = null;

    protected void init2() {
        intronicCGFF = Util.getIntronicCGFF(getCGFF(), true);
    }

    private Map geneIntronCntMapUniq = new HashMap();
    private Map geneIntronCntMapMulti = new HashMap();

    public void countReadUnique(String readID, AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
        intronCounting(record, geneIntronCntMapUniq, cnt, geneID, intronicCGFF, checkByContaining, minimumOverlap);

    }

    public void countReadMulti(String readID, Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
        Set hittingReocrds = new HashSet(recordCollection);
        // adjust count
        Float newCount = new Float(cnt.floatValue() / hittingReocrds.size());
        for (Iterator recordIterator = hittingReocrds.iterator(); recordIterator.hasNext();) {
            AlignmentRecord record = (AlignmentRecord) recordIterator.next();
            intronCounting(record, geneIntronCntMapMulti, newCount, geneID, intronicCGFF, checkByContaining, minimumOverlap);
        }
    }

    public void report(String filename, CanonicalGFF cgff) {
        filename += ".intronCount";
        try {
            FileWriter fw = new FileWriter(new File(filename));
            // header
            fw.write("#GeneID" + "\t" + "intronNo" + "\t" + "#reads" + "\t"
                    + "intronLen" + "\t" + "multi/ALL" + "\t"
                    + "format:.intronCount" + "\n");
            // contents
            // for each gene
            for (Iterator iterator = cgff.geneLengthMap.keySet().iterator(); iterator.hasNext();) {
                String geneID = (String) iterator.next();
                if(intronicCGFF.geneRegionMap.containsKey(geneID)==false){
                  continue;
                }

                Map uniqReadCntMap = (Map) geneIntronCntMapUniq.get(geneID);
                Map multiReadCntMap = (Map) geneIntronCntMapMulti.get(geneID);
                // iterate introns for this gene
                Set intronRegions = (Set) intronicCGFF.geneExonRegionMap.get(geneID);
                int intronNo = 1;
                for (Iterator intronIterator = intronRegions.iterator(); intronIterator.hasNext();) {
                    Interval intronInterval = (Interval) intronIterator.next();

                    float uniqCnt = 0;
                    if (uniqReadCntMap != null && uniqReadCntMap.containsKey(intronInterval)) {
                        uniqCnt = ((Number) uniqReadCntMap.get(intronInterval)).floatValue();
                    }

                    float multiCnt = 0;
                    if (multiReadCntMap != null && multiReadCntMap.containsKey(intronInterval)) {
                        multiCnt = ((Number) multiReadCntMap.get(intronInterval)).floatValue();
                    }
                    fw.write(geneID + "\t");
                    fw.write(intronNo + "\t");
                    fw.write((uniqCnt + multiCnt) + "\t");
                    fw.write(intronInterval.length() + "\t");
                    if ((uniqCnt + multiCnt) > 0) {
                        fw.write(new Float(multiCnt / (uniqCnt + multiCnt)).toString());
                    } else {
                        fw.write("0");
                    }
                    fw.write("\n");
                    intronNo++;
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

    private boolean intronCounting(AlignmentRecord record, Map geneIntronCntMap, Number count, String geneID, CanonicalGFF intronicCGFF,
            boolean checkByContaining, int minimumOverlap) {
        Set intronRegions = (Set) intronicCGFF.geneExonRegionMap.get(geneID);
        // get hit introns
        HashSet hitIntrons = new HashSet();


        for (Iterator intronIterator = intronRegions.iterator(); intronIterator.hasNext();) {
            Interval intronInterval = (Interval) intronIterator.next();

            // check intron contain read splice
            boolean intronSkip = false;
            if (record.numBlocks > 1) {
                int spliceStart = 0;
                for (Iterator blockIterator = record.getMappingIntervals().iterator(); blockIterator.hasNext();) {
                    Interval blockInterval = (Interval) blockIterator.next();
                    if (spliceStart != 0) {  // get splice stop
                        int spliceStop = blockInterval.getStart();
                        if (intronInterval.intersect(spliceStart) || intronInterval.intersect(spliceStop) ) {
                            intronSkip = true;
                            break;
                        }
                    }
                    // get read splice start
                    spliceStart = blockInterval.getStop();
                }
            }
            if (intronSkip) continue;

            for (Iterator blockIterator = record.getMappingIntervals().iterator(); blockIterator.hasNext();) {
                Interval blockInterval = (Interval) blockIterator.next();
                if (checkByContaining) {
                    // check containment
                    if (intronInterval.contain(blockInterval.getStart(), blockInterval.getStop()) == false)
                        continue;
                    // check overlap size
                    if (blockInterval.length() < minimumOverlap)
                        continue;
                } else {
                    // check intersection
                    if (intronInterval.intersect(blockInterval) == false)
                        continue;
                    // check overlap size
                    int intersectStart = (blockInterval.getStart() > intronInterval.getStart()) ? blockInterval.getStart() : intronInterval.getStart();
                    int intersectStop = (blockInterval.getStop() < intronInterval.getStop()) ? blockInterval.getStop() : intronInterval.getStop();
                    if ((intersectStop - intersectStart + 1) < minimumOverlap) continue;
                }
                hitIntrons.add(intronInterval);
                break;
            }

        }
        if (hitIntrons.size() == 0)
            return false;

        // add counts for these hit introns
        Map intronCntMap;
        if (geneIntronCntMap.containsKey(geneID)) {
            intronCntMap = (Map) geneIntronCntMap.get(geneID);
        } else {
            intronCntMap = new HashMap();
            geneIntronCntMap.put(geneID, intronCntMap);
        }
        for (Iterator intronIterator = hitIntrons.iterator(); intronIterator.hasNext();) {
            Interval intron = (Interval) intronIterator.next();
            if (intronCntMap.containsKey(intron)) {
                float oldValue = ((Number) intronCntMap.get(intron)).floatValue();
                intronCntMap.put(intron, new Float(oldValue + count.floatValue()));
            } else {
                intronCntMap.put(intron, count);
            }
        }
        return true;
    }

}
