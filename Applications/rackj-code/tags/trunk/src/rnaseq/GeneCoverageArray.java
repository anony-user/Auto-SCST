package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.Util;

/**
 * <p>Title: GeneCoverageArray</p>
 * <p>Description: This class could compute coverage for each gene</p>
 * <P>Output: Gene Coverage Report
 * @author Chun-Mao Fu
 * @author Wen-Dar Lin
 * @version 1.0
 */

public class GeneCoverageArray implements ReadCounter {

    Map geneArrayMap = new TreeMap();
    Map geneUniReadCntMap = new TreeMap();

    /****************************************************************************/
    /*                   Declare inputs, output and factors                     */
    /****************************************************************************/
    private static String gffFilename = null;
    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;
    private static String outputPrefix = null;
    private static int joinFactor = 2;
    private static float identityCutoff = 0.95F;
    private static boolean useExonRegion = true;
    private static boolean checkByContaining = false;
    private static int minimumOverlap = 8;
    private static boolean checkAllBlocks = true;
    private static boolean includeMultiReads = false;
    private static boolean careDirection = false;
    private static boolean fillGaps = false;

    /****************************************************************************/
    /*                  Parameter processing on command line                    */
    /****************************************************************************/
    static private void paraProc(String[] args){

      // get parameter strings
      for(int i=0;i<args.length;i++){
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
            else if(args[i].equals("-multi")){
                includeMultiReads = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-direction")){
                careDirection = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-fill")){
            	fillGaps = Boolean.valueOf(args[i+1]);
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
      System.out.println("program: GeneCoverageArray");
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
      System.out.println("include multi reads (-multi): " + includeMultiReads);
      System.out.println("care mapping direction (-direction): "+careDirection);
      System.out.println("fill gaps between alignment blocks (-fill): "+fillGaps);
      System.out.println();

    }

    /****************************************************************************/
    /*                     Compute coverage for each gene                       */
    /****************************************************************************/
    private void GeneCoveraging(AlignmentRecord record, String geneID, CanonicalGFF cgff, boolean readCounting){

      if(readCounting){
        // update geneUniReadCntMap
        if (geneUniReadCntMap.containsKey(geneID)) {
          int val = ( (Integer) geneUniReadCntMap.get(geneID)).intValue();
          geneUniReadCntMap.put(geneID, new Integer(val + 1));
        }
        else {
          geneUniReadCntMap.put(geneID, new Integer(1));
        }
      }
      // get gene range for a given geneID
      Interval geneRegion = (Interval) cgff.geneRegionMap.get(geneID);

      // check if geneID is contained by geneArrayMap
      // if not, add <geneID, new array> into geneArrayMap
      if(!geneArrayMap.containsKey(geneID)){
        int[] array = new int[geneRegion.length()];
        Arrays.fill(array,0);
        geneArrayMap.put(geneID,array);
      }

      // get array according to given geneID
      int[] array_update =  (int[])geneArrayMap.get(geneID);

      // set mapping intervals
      Set mappingIntervals = record.getMappingIntervals();
      if(fillGaps){
    	  Interval interval = new Interval(record.tStarts[0],
    			  record.tStarts[record.numBlocks-1]+record.tBlockSizes[record.numBlocks-1]-1);
    	  mappingIntervals = new TreeSet();
    	  mappingIntervals.add(interval);
      }
      
      // get records and mark them on given array
      for(Iterator IntervalIterator = mappingIntervals.iterator();IntervalIterator.hasNext();){
        Interval recordInterval = (Interval)IntervalIterator.next();
        // check if recordInterval interacts with geneRegion
        // if yes, update array. Otherwise, check next recordInterval
        if(recordInterval.intersect(geneRegion)){
          int addStart = Math.max(geneRegion.getStart(),recordInterval.getStart()) - geneRegion.getStart();
          int addStop = Math.min(geneRegion.getStop(),recordInterval.getStop()) - geneRegion.getStart();
          //updating array
          for (int ind = addStart; ind <= addStop; ind++)
            array_update[ind] += 1;
        }else continue;
      }
  }

    public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
      GeneCoveraging(record,geneID,cgff,true);
    }

    public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
      Set hittingReocrds = new HashSet(recordCollection);
      boolean readCounting = true;
      // adjust count
      for(Iterator recordIterator = hittingReocrds.iterator();recordIterator.hasNext();){
          AlignmentRecord record = (AlignmentRecord) recordIterator.next();
          GeneCoveraging(record, geneID, cgff, readCounting);
          readCounting = false;
      }
    }

    public void report(String filename, CanonicalGFF cgff) {
      try {
          FileWriter fw = new FileWriter(new File(filename));
          // header
          fw.write("#GeneID" + "\t" +
                   "chr" + "\t" +
                   "start" + "\t" +
                   "readCnt" + "\t" +
                   "CoverageArray" +  "\t" +
                   "format:.geneCoverage" + "\n");
          // contents
          // for each gene
          for(Iterator iterator = geneArrayMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry pointer = (Map.Entry) iterator.next();
            String geneID = (String) pointer.getKey();
            GenomeInterval geneRegion = (GenomeInterval) cgff.geneRegionMap.get(geneID);
            int[] array_report = (int[])pointer.getValue();
            fw.write(pointer.getKey() + "\t" +
                     cgff.chrOriginalMap.get(geneRegion.getChr()) + "\t" +
                     geneRegion.getStart() + "\t" +
                     geneUniReadCntMap.get(pointer.getKey()) + "\t" +
                     Arrays.toString(array_report) + "\n");
          }
          fw.close();
      } catch (IOException ex) {
          ex.printStackTrace();
          System.exit(1);
      }

    }

    public static void main(String args[]){

      paraProc(args);

      CanonicalGFF cgff = new CanonicalGFF(gffFilename);
      Map geneUniqReadCntMap = new HashMap();

      final GeneCoverageArray geneCoverageArray = new GeneCoverageArray();

      ReadCounter readCounter = new ReadCounter(){
        public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
          geneCoverageArray.countReadUnique(readID,record, cnt, geneID, cgff);
        }
        public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
          geneCoverageArray.countReadMulti(readID, recordCollection, cnt, geneID, cgff);
        }
        public void report(String filename, CanonicalGFF cgff) {
          geneCoverageArray.report(filename + ".geneCoverage",cgff);
        }
      };
      // computing based on unique reads
      int uniqReadCnt = 0;

      for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
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
      }
      System.out.println("#uniq reads: " + uniqReadCnt);

      // computing based on multi reads
      if(includeMultiReads){
        int multiReadCnt = 0;
        int restReadCnt = 0;
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();
             iterator.hasNext(); ) {
          Map.Entry entry = (Entry) iterator.next();
          String mappingFilename = (String) entry.getKey();
          String mappingMethod = (String) entry.getValue();
          MultiReadIterator multiRI = new MultiReadIterator(Util.getMRIinstance(
              mappingFilename, mappingMethodMap, mappingMethod),
              uniqReadCnt,
              identityCutoff,
              joinFactor,
              useExonRegion,
              checkByContaining,
              minimumOverlap,
              checkAllBlocks,
              careDirection,
              cgff,
              geneUniqReadCntMap,
              new HashMap()
              );
          multiRI.iterate(readCounter);
          multiReadCnt += multiRI.mlutiReadCnt;
          restReadCnt += multiRI.restReadCnt;
        }
        System.out.println("#multi reads: " + multiReadCnt);
        System.out.println("#mapped reads: " +
                           (uniqReadCnt + multiReadCnt + restReadCnt));
      }

      // output gene coverage report
      readCounter.report(outputPrefix,cgff);
    }

}
