package special;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.GffRecord;
import misc.GffTree;
import misc.Interval;
import misc.MappingResultIterator;
import misc.Util;
import misc.intervaltree.IntervalTree;

/**
 * Computation of read locations relative to TSS (transcription start site)
 * and TTS (transcription termination site) of closest genes.
 *
 * See main() for the algorithm.
 *
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class ReadLoc2TssTts {

    // CGFF
    private static String gffFilename = null;
    // mapping result
    private static String mappingMethod = null;
    private static String mappingFilename = null;
    private static Map mappingMethodMap = null;
    // GFF
    private static String inFilename = null;
    private static String idAttrIn = null;
    private static ArrayList parentAttrsIn = new ArrayList();
    // max chr length
    private static int maxLen = 0;
    // output
    private static String outFilename = null;
    // alignment check
    private static int joinFactor = 0;
    private static float identityCutoff = 0.95F;
    private static boolean checkByContaining = true;
    private static int minimumOverlap = 6;
    private static boolean checkAllBlocks = true;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-M")){
                mappingMethod = args[i + 1];
                mappingFilename = args[i + 2];
                i+=2;
            }
            else if (args[i].equals("-I")) {
                inFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-idAttr")){
                idAttrIn = args[i + 1];
                i++;
            }
            else if(args[i].equals("-parentAttr")){
                parentAttrsIn.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-MAX")){
                maxLen = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-O")){
                outFilename = args[i+1];
                i++;
            }
            else if(args[i].equals("-J")){
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-ID")){
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-contain")){
                checkByContaining = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-min")){
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-ALL")){
                checkAllBlocks = Boolean.valueOf(args[i+1]);
                i++;
            }
        }

        // check for necessary parameters
        if(gffFilename==null){
          System.err.println("canonical GFF filename (-GFF) not assigned");
          System.exit(1);
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",System.getProperty("java.class.path"),"misc");
        if(mappingMethod==null){
          System.err.println("mapping method/filename (-M) not assigned, available methods:");
          for(Iterator iterator = mappingMethodMap.keySet().iterator();iterator.hasNext();){
              System.out.println(iterator.next());
          }
          System.exit(1);
        }
        if(inFilename==null){
          System.err.println("GFF filename (-I) not assigned");
          System.exit(1);
        }
        if(outFilename==null){
          System.err.println("output filename (-O) not assigned");
          System.exit(1);
        }
        if(maxLen<=0){
            System.err.println("maximum chromosome length (-MAX) not assigned");
            System.exit(1);
        }

        // post-processing
        if(idAttrIn==null){
            idAttrIn = "ID";
            System.out.println("ID attribute (-idAttr) not assigned, using default: " + idAttrIn);
        }
        if(parentAttrsIn.size()==0){
            parentAttrsIn.add("Parent");
            parentAttrsIn.add("Derives_from");
            System.out.println("parent attribute list (-parentAttr) not assigned, using default: " + parentAttrsIn);
        }

        // list parameters
        System.out.println("program: CanonicalGFF");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("mapping method (-M): "+ mappingMethod + ", Source: " + mappingFilename);
        System.out.println("input GFF file (-I): "+inFilename);
        System.out.println("ID attribute (-idAttr): "+idAttrIn);
        System.out.println("parent attribute list (-parentAttr): "+parentAttrsIn);
        System.out.println("maximum chromosome length (-MAX): " + maxLen);
        System.out.println("output filename (-O): "+outFilename);
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+ identityCutoff);
        System.out.println("check by containing (-contain, FALSE for by intersecting): "+checkByContaining);
        System.out.println("minimum overlap (-min): "+ minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): "+checkAllBlocks);
        System.out.println();
    }

    public static void main(String[] args) {
        paraProc(args);

        // STEP 0: for any necessary preparation
        // read CGFF
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        // read GffTree
        GffTree gffTree = new GffTree(inFilename,idAttrIn,parentAttrsIn);

        // STEP 1: compute intergenic genome intervals
        // read genome intervals of genes for each chromosome (done in STEP 0)
        // combine[nearby|overlap] genome intervals of genes for each chromosome
        Map chrGeneIntervalsMap = new LinkedHashMap();
        for(Iterator chrIterator = cgff.chrIntervalTreeMap.keySet().iterator();chrIterator.hasNext();){
            Object chr = chrIterator.next();
            IntervalTree intervalTree = (IntervalTree) cgff.chrIntervalTreeMap.get(chr);
            Set tmpIntervalSet = new HashSet(intervalTree.getAll());
            tmpIntervalSet.remove(null);
            TreeSet geneIntervals = new TreeSet(tmpIntervalSet);
            Interval.combineOverlap(geneIntervals);
            Interval.combineNearby(geneIntervals,1);
            chrGeneIntervalsMap.put(chr,geneIntervals);
        }
        // generate intergenic genome intervals for each chromosome
        Map chrIntergenicIntervalsMap = new LinkedHashMap();
        int tmpId = 1;
        for(Iterator chrIterator = chrGeneIntervalsMap.keySet().iterator();chrIterator.hasNext();){
            Object chr = chrIterator.next();
            TreeSet intergenicIntervals = new TreeSet();
            chrIntergenicIntervalsMap.put(chr,intergenicIntervals);
            TreeSet geneIntervals = (TreeSet) chrGeneIntervalsMap.get(chr);
            int lastPos = 1;
            for(Iterator geneIntervalIterator = geneIntervals.iterator();geneIntervalIterator.hasNext();){
                Interval geneInterval = (Interval) geneIntervalIterator.next();
                Interval intergenicInterval = new Interval(lastPos,geneInterval.getStart()-1,"IntergenicRegion"+tmpId);
                tmpId++;
                intergenicIntervals.add(intergenicInterval);
                lastPos = geneInterval.getStop()+1;
            }
            // add the most last "intergenic" interval
            intergenicIntervals.add(new Interval(lastPos,maxLen,"IntergenicRegion"+tmpId));
        }
        // translate chrIntergenicIntervalsMap into a CGFF so that we can use CGFF.getRelatedGenes()
        CanonicalGFF intergenicCGFF = new CanonicalGFF(chrIntergenicIntervalsMap);

        // STEP 2: get nearby genic genome intervals (and their strands) for each intergenic genome interval
        // build gene-strand map
        Map geneStrandMap = new HashMap();
        getGeneStrandMap(gffTree.getRoot(),cgff.geneLengthMap.keySet(),geneStrandMap);
        // for each intergenic genome interval, find genes at the both side
        Map nearbyGeneIDsMap = new HashMap();
        for(Iterator chrIterator = chrIntergenicIntervalsMap.keySet().iterator();chrIterator.hasNext();){
            String chr = (String) chrIterator.next();
            Set intergenicIntervalSet = (Set) chrIntergenicIntervalsMap.get(chr);
            for(Iterator intervalIterator = intergenicIntervalSet.iterator();intervalIterator.hasNext();){
                Interval interval = (Interval) intervalIterator.next();
                String intergenicID = (String) interval.getUserObject();
                Set queryIntervalSet = new HashSet();
                queryIntervalSet.add(new Interval(interval.getStart()-1,interval.getStop()+1));
                Set genicIntervals = cgff.getRelatedGenes(chr,queryIntervalSet,false,false,1,false);
                Set geneIDs = new HashSet();
                for(Iterator geneRegionIterator = genicIntervals.iterator();geneRegionIterator.hasNext();){
                    GenomeInterval genicInterval = (GenomeInterval) geneRegionIterator.next();
                    geneIDs.add(genicInterval.getUserObject());
                }
                nearbyGeneIDsMap.put(intergenicID,geneIDs);
            }
        }

        // STEP 3: iterate all uniq reads, and determine their relative positions
        // for each uniq read belonging to a intergenic genome interval, find its relative position for each related genes
        // (there are four cases: relative position of the read vs a gene & strand of the gene)
        int uniqReadCnt = 0;
        try {
            FileWriter fw = new FileWriter(outFilename);
            for (MappingResultIterator iterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod);
                                                  iterator.hasNext(); ) {
                ArrayList mappingRecords = (ArrayList) iterator.next();
                // skip a read if best identity less than identityCutoff
                if (iterator.getBestIdentity() < identityCutoff)
                    continue;
                // collect records with best identity
                ArrayList acceptedRecords = new ArrayList();
                for (int i = 0; i < mappingRecords.size(); i++) {
                    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                    if (record.identity >= iterator.getBestIdentity()) {
                        // join nearby blocks
                        if (joinFactor > 0)
                            record.nearbyJoin(joinFactor);
                        acceptedRecords.add(record);
                    }
                }
                // skip multi reads
                if (acceptedRecords.size() > 1)
                    continue;
                // get corresponding intergenic regions
                AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(0);
                int alignmentStart = record.tStarts[0];
                int alignmentStop = record.tStarts[record.numBlocks - 1] +
                                    record.tBlockSizes[record.numBlocks - 1] - 1;
                Set hitIntergenicRegions = intergenicCGFF.getRelatedGenes(record.chr, record.getMappingIntervals(),
                        false, checkByContaining, minimumOverlap, checkAllBlocks);
                // skip if no intergenic region hit
                if(hitIntergenicRegions.size()==0) continue;
                uniqReadCnt++;

                GenomeInterval intergenicRegion = (GenomeInterval) hitIntergenicRegions.iterator().next(); // we got only one hit intergenic interval
                String intergenicID = (String) intergenicRegion.getUserObject();
                Set nearbyGeneIDs = (Set) nearbyGeneIDsMap.get(intergenicID);
                for (Iterator geneIdIterator = nearbyGeneIDs.iterator(); geneIdIterator.hasNext(); ) {
                    String geneID = (String) geneIdIterator.next();
                    String strand = (String) geneStrandMap.get(geneID);
                    GenomeInterval geneRegion = (GenomeInterval) cgff.geneRegionMap.get(geneID);
                    // decide relative position
                    boolean fiveEnd=true;
                    if (alignmentStart < geneRegion.getStart() && alignmentStop < geneRegion.getStop()) {
                        fiveEnd = true;
                    } else if (alignmentStart > geneRegion.getStart() && alignmentStop > geneRegion.getStop()) {
                        fiveEnd = false;
                    } else {
                        System.err.println("intersecting: " + iterator.getReadID() + " AND " + geneID);
                        // we don't process this kind of reads
                        continue;
                    }
                    // decide strand
                    boolean positiveStrand=true;
                    if(strand.equals("+")){
                        positiveStrand = true;
                    }else if(strand.equals("-")){
                        positiveStrand = false;
                    } else {
                        System.err.println("no strand: " + geneID);
                    }
                    // OUTPUT: readID
                    fw.write(iterator.getReadID());
                    // OUTPUT: TSS or TTS, be aware of the logical operation
                    if(fiveEnd==positiveStrand){
                        fw.write("\t" + "TSS");
                    }else{
                        fw.write("\t" + "TTS");
                    }
                    // OUTPUT: geneID
                    fw.write("\t" + geneID);
                    // OUTPUT: relative interval
                    if(fiveEnd){
                        fw.write("\t" + (geneRegion.getStart()-alignmentStart));
                        fw.write("\t" + (geneRegion.getStart()-alignmentStop));
                    }else{
                        fw.write("\t" + (alignmentStop-geneRegion.getStop()));
                        fw.write("\t" + (alignmentStart-geneRegion.getStop()));
                    }

                    fw.write("\n");
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.out.println("processed # uniq-reads: " + uniqReadCnt);
    }

    private static void getGeneStrandMap(DefaultMutableTreeNode node,Set geneIDs,Map ansMap){
        GffRecord record = (GffRecord) node.getUserObject();
        Map recordAttrMap = record.getAttrMap();
        if(recordAttrMap.containsKey(idAttrIn) && geneIDs.contains(recordAttrMap.get(idAttrIn))){
            ansMap.put(recordAttrMap.get(idAttrIn),record.getStrand());
        }

        // recursive
        for(int i=0;i<node.getChildCount();i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            getGeneStrandMap(child,geneIDs,ansMap);
        }
    }
}
