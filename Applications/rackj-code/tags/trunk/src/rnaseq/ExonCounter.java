package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.Interval;
import misc.MultiLevelMap;
import misc.Util;

public class ExonCounter implements ReadCounter,ReadCounter2 {

    private static String gffFilename = null;
    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;

    private static String outputPrefix = null;

    private static int joinFactor = 2;
    private static float identityCutoff = 0.95F;
    private static boolean useExonRegion = false;
    private static boolean checkByContaining = false;
    private static int minimumOverlap = 8;
    private static boolean checkAllBlocks = true;
    private static boolean careDirection = false;

    private static boolean intronicCGFF = false;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-M")){
                mappingFilenameMethodMap.put(args[i + 2],args[i + 1]);
                i+=2;
            }
            else if(args[i].equals("-J")){
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-ID")){
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-O")){
                outputPrefix = args[i + 1];
                i++;
            }
            else if(args[i].equals("-exon")){
                useExonRegion = Boolean.valueOf(args[i+1]);
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
            else if(args[i].equals("-direction")){
                careDirection = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-intronic")){
                intronicCGFF = Boolean.valueOf(args[i+1]);
                i++;
            }
        }

        // check for necessary parameters
        if(gffFilename==null){
          System.err.println("canonical GFF filename (-GFF) not assigned");
          System.exit(1);
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",System.getProperty("java.class.path"),"misc");
        if(mappingFilenameMethodMap.size()<=0){
          System.err.println("mapping method/filename (-M) not assigned, available methods:");
          for(Iterator iterator = mappingMethodMap.keySet().iterator();iterator.hasNext();){
              System.out.println(iterator.next());
          }
          System.exit(1);
        }
        for(Iterator methodIterator = mappingFilenameMethodMap.values().iterator();methodIterator.hasNext();){
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if(outputPrefix==null){
            System.err.println("output prefix (-O) not assigned");
            System.exit(1);
        }
        if(minimumOverlap<1){
            System.err.println("minimum block size (-min) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: ExonCounter");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("mapping method/filename (-M):");
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): "+outputPrefix);
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+ identityCutoff);
        System.out.println("use exon region (-exon): "+useExonRegion);
        System.out.println("check by containing (-contain, FALSE for by intersecting): "+checkByContaining);
        System.out.println("minimum overlap (-min): "+ minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): "+checkAllBlocks);
        System.out.println("care mapping direction (-direction): "+careDirection);
        System.out.println("count for intron (-intronic): "+intronicCGFF);
        System.out.println();
    }

    public static void main(String args[]){
        paraProc(args);

        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        if(intronicCGFF){
          cgff = Util.getIntronicCGFF(cgff,true);
        }

        Map geneUniqReadCntMap = new HashMap();
        Map geneMultiReadCntMap = new HashMap();

        // read counters
        final ExonCounter exonCounter = new ExonCounter(checkByContaining,minimumOverlap);

        ReadCounter readCounter = new ReadCounter(){ // wrap ReadCounters
            public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
                exonCounter.countReadUnique(readID,record, cnt, geneID, cgff);
            }

            public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
                exonCounter.countReadMulti(readID,recordCollection, cnt, geneID, cgff);
            }

            public void report(String filename, CanonicalGFF cgff) {
              if(intronicCGFF){
                exonCounter.report(filename + ".intronCount", cgff);
              }else{
                exonCounter.report(filename + ".exonCount", cgff);
              }
            }
        };

        // computing based on unique reads
        int uniqReadCnt = 0;
        Set nonUniqReads = new HashSet();
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            UniqueReadIterator uniqueRI = new UniqueReadIterator(Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
                    identityCutoff,
                    joinFactor,
                    useExonRegion,
                    checkByContaining,
                    minimumOverlap,
                    checkAllBlocks,
                    careDirection,
                    cgff,
                    geneUniqReadCntMap
                    );
            uniqueRI.iterate(readCounter);
            uniqReadCnt += uniqueRI.uniqReadCnt;
            //nonUniqReads.addAll(uniqueRI.restReads);
        }
        System.out.println("#uniq reads: " + uniqReadCnt);

        // computing based on multi reads
        int multiReadCnt = 0;
        int restReadCnt = 0;
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            MultiReadIterator multiRI = new MultiReadIterator(Util.getMRIinstance(mappingFilename, mappingMethodMap,mappingMethod),
                    /*nonUniqReads,*/
                    uniqReadCnt,
                    identityCutoff,
                    joinFactor,
                    useExonRegion,
                    checkByContaining,
                    minimumOverlap,
                    checkAllBlocks,
                    cgff,
                    geneUniqReadCntMap,
                    geneMultiReadCntMap
                    );
            multiRI.iterate(readCounter);
            multiReadCnt += multiRI.mlutiReadCnt;
            restReadCnt += multiRI.restReadCnt;
        }
        System.out.println("#multi reads: " + multiReadCnt);
        System.out.println("#mapped reads: " + (uniqReadCnt + multiReadCnt + restReadCnt));

        // output gene RPKM
        readCounter.report(outputPrefix,cgff);
    }

    private Map geneExonCntMapUniq = new HashMap();
    private Map geneExonCntMapMulti = new HashMap();
    private MultiLevelMap<Integer> hitSetGeneExonSetsCntMap = new MultiLevelMap<Integer>();

    public ExonCounter(boolean checkByContaining,int minimumOverlap){
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;
    }
    
    private Set getHitExons(AlignmentRecord record, String geneID, CanonicalGFF cgff, boolean checkByContaining, int minimumOverlap){

    	HashSet hitExons = new HashSet();

    	Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        Interval[] exonArray = (Interval[]) exonRegions.toArray(new Interval[0]);
        int exonIndex=0;
        int blockIndex=0;
        while(blockIndex<record.numBlocks && exonIndex<exonArray.length){
          Interval blockTarget = new Interval(record.tStarts[blockIndex],record.tStarts[blockIndex] + record.tBlockSizes[blockIndex]-1);
          Interval exonInterval = exonArray[exonIndex];
          boolean hitCheck = Util.blockCheck(exonInterval,blockTarget,exonIndex,exonArray.length,checkByContaining,minimumOverlap);

          if(hitCheck){
            hitExons.add(exonInterval);
          }

          // iteration
          if(blockTarget.getStop()<exonInterval.getStop()){
            blockIndex++;
          }else{
            exonIndex++;
          }
        }
        
        return hitExons;
    }

    private boolean exonCounting(AlignmentRecord record, Map geneExonCntMap, Number count, String geneID, CanonicalGFF cgff, boolean checkByContaining, int minimumOverlap){
        Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        // get hit exons
        Set hitExons = getHitExons(record,geneID,cgff,checkByContaining,minimumOverlap);
        if(hitExons.size()==0) return false;

        // add counts for these hit exons
        Map exonCntMap;
        if(geneExonCntMap.containsKey(geneID)){
            exonCntMap = (Map) geneExonCntMap.get(geneID);
        }else{
            exonCntMap = new HashMap();
            geneExonCntMap.put(geneID,exonCntMap);
        }
        for(Iterator exonIterator = hitExons.iterator();exonIterator.hasNext();){
            Interval exon = (Interval) exonIterator.next();
            if(exonCntMap.containsKey(exon)){
                double oldValue = ((Number)exonCntMap.get(exon)).doubleValue();
                exonCntMap.put(exon,new Double(oldValue+count.doubleValue()));
            }else{
                exonCntMap.put(exon,count);
            }
        }
        return true;
    }

    public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
        exonCounting(record,geneExonCntMapUniq,cnt,geneID,cgff,checkByContaining,minimumOverlap);
    }

    public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
        Set hittingReocrds = new HashSet(recordCollection);
        // adjust count
        Double newCount = new Double(cnt.doubleValue()/hittingReocrds.size());
        for(Iterator recordIterator = hittingReocrds.iterator();recordIterator.hasNext();){
            AlignmentRecord record = (AlignmentRecord) recordIterator.next();
            exonCounting(record,geneExonCntMapMulti,newCount,geneID,cgff,checkByContaining,minimumOverlap);
        }
    }

    public void report(String filename,CanonicalGFF cgff) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            // header
            if(intronicCGFF){
              fw.write("#GeneID" + "\t" +
                       "intronNo" + "\t" +
                       "#reads" + "\t" +
                       "intronLen" + "\t" +
                       "multi/ALL" + "\t" +
                       "format:.intronCount" + "\n"
                      );
            }else{
              fw.write("#GeneID" + "\t" +
                       "exonNo" + "\t" +
                       "#reads" + "\t" +
                       "exonLen" + "\t" +
                       "multi/ALL" + "\t" +
                       "format:.exonCount" + "\n"
                      );
            }
            // contents
            // for each gene
            for(Iterator iterator = cgff.geneLengthMap.keySet().iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                Map uniqReadCntMap = (Map) geneExonCntMapUniq.get(geneID);
                Map multiReadCntMap = (Map) geneExonCntMapMulti.get(geneID);
                // iterate exons for this gene
                Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
                int exonNo = 1;
                for(Iterator exonIterator = exonRegions.iterator();exonIterator.hasNext();){
                    Interval exonInterval = (Interval) exonIterator.next();
                    float uniqCnt = 0;
                    if(uniqReadCntMap!=null && uniqReadCntMap.containsKey(exonInterval)){
                        uniqCnt = ((Number)uniqReadCntMap.get(exonInterval)).floatValue();
                    }
                    float multiCnt = 0;
                    if(multiReadCntMap!=null && multiReadCntMap.containsKey(exonInterval)){
                        multiCnt = ((Number)multiReadCntMap.get(exonInterval)).floatValue();
                    }
                    fw.write(geneID + "\t");
                    fw.write(exonNo + "\t");
                    fw.write((uniqCnt + multiCnt) + "\t");
                    fw.write(exonInterval.length() + "\t");
                    if((uniqCnt+multiCnt)>0){
                        fw.write(new Float(multiCnt/(uniqCnt+multiCnt)).toString());
                    }else{
                        fw.write("0");
                    }
                    fw.write("\n");
                    exonNo++;
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
 
    public void processUnique(String readID,AlignmentRecord record,Number cnt,String geneID,CanonicalGFF cgff){
    	exonCounting(record,geneExonCntMapUniq,cnt,geneID,cgff,checkByContaining,minimumOverlap);
    }

    public void processMulti(String readID,Map hitGeneAlignmentSetMap,Set geneIdSet,CanonicalGFF cgff){

    	for(Iterator geneIterator = hitGeneAlignmentSetMap.keySet().iterator(); geneIterator.hasNext();){
    		String geneID = (String)geneIterator.next();
    		Set alignmentSet = (Set) hitGeneAlignmentSetMap.get(geneID);

    		MultiLevelMap<Integer> intervalCntMap = new MultiLevelMap<Integer>();

    		Set<Set> exonSetSet = new HashSet();
    		for(Iterator alnIterator = alignmentSet.iterator(); alnIterator.hasNext();){
    			AlignmentRecord record = (AlignmentRecord) alnIterator.next();
    			Set hitExons = getHitExons(record,geneID,cgff, checkByContaining,minimumOverlap);
    			Set newHitExons = new HashSet();
    			for(Iterator exonIterator = hitExons.iterator(); exonIterator.hasNext();){
    				Interval hitExon = (Interval) exonIterator.next();
    				intervalCntMap.multiLevelAdd(new Integer(1), hitExon);
    				int intervalIndex = ((Integer)intervalCntMap.multiLevelGet(hitExon)).intValue();
    				Interval newHitExon = new Interval(hitExon.getStart(), hitExon.getStop(), intervalIndex);
    				newHitExons.add(newHitExon);
    			}
    			exonSetSet.add(newHitExons);
    		}

    		hitSetGeneExonSetsCntMap.multiLevelAdd(new Integer(1), geneIdSet,geneID,exonSetSet);
    	}
    }
    
    public void finalize(Map<Set,Map<String,Double>> geneSetIdRatioMap){
    
    	for(Iterator keysIterator = hitSetGeneExonSetsCntMap.iterator(3); keysIterator.hasNext();){
    		Object[] keys1 = (Object[]) keysIterator.next();
    		Set geneIdSet = (Set) keys1[0];
    		String geneId = (String) keys1[1];
    		Set<Set> exonSetSet = (Set<Set>) keys1[2];
    		int readCnt = ((Integer)hitSetGeneExonSetsCntMap.multiLevelGet(keys1)).intValue();
    		
    		double ratio = geneSetIdRatioMap.get(geneIdSet).get(geneId);
    		double share = readCnt*ratio/exonSetSet.size();
    		
   		    for(Iterator<Set> hitExonSetIterator = exonSetSet.iterator();hitExonSetIterator.hasNext();){
   			    Set<Interval> exonSet = hitExonSetIterator.next();
   			    for(Iterator<Interval> exonIterator = exonSet.iterator();exonIterator.hasNext();){
   				    Interval hitExon = exonIterator.next();
				    hitExon = new Interval(hitExon.getStart(),hitExon.getStop());
				    MultiLevelMap.multiLevelAdd(geneExonCntMapMulti, share, geneId, hitExon);
			    }
		    }
    	}

    }
}
