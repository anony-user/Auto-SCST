package rnaseq;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.IntervalCoverageNode;
import misc.PairedBatchIterator;
import misc.Util;

/**
 * <P> Title: MappingInfoRecover
 * <P> Description: This class helps user to know at where Intron and exron locate
 * <p> Output: Report of Exon and Intron locations
 * @version 1.0
 * @author Chun-Mao Fu
 * @author Wen-Dar Lin
 */
public class MappingInfoRecover {

  // mapping result
  private static Map mappingMethodMap = null;
  private static Map mappingFilenameMethodMap = new LinkedHashMap();
  // output
  private static String outPrefix = null;
  // alignment check
  private static int joinFactor = 2;
  private static float identityCutoff = 0.95F;
  private static int minimumBlockSize = 8;
  // mated pair
  private static int checkMatePair = 0;
  private static int maxDistanceCutoff = 1200;
  private static int minDistanceCutoff = 0;
  //
  private static boolean includeMultiReads = false;

  private static Map icnRelatedMap = new TreeMap();
  private static Set matedPairISet = new TreeSet();

  /****************************************************************************/
  /*                   Parameter processing on command line                   */
  /****************************************************************************/
  static private void paraProc(String[] args){

        // get parameter strings
        for(int i=0;i<args.length;i++){
            if(args[i].equals("-M")){
                mappingFilenameMethodMap.put(args[i + 2],args[i + 1]);
                i+=2;
            }
            else if(args[i].equals("-O")){
                outPrefix = args[i+1];
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
            else if(args[i].equals("-min")){
                minimumBlockSize = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-mate")){
              checkMatePair = Integer.valueOf(args[i + 1]);
              i++;
            }
            else if(args[i].equals("-maxD")){
              maxDistanceCutoff = Integer.parseInt(args[i+1]);
              i++;
            }
            else if(args[i].equals("-minD")){
              minDistanceCutoff = Integer.parseInt(args[i+1]);
              i++;
            }
            else if(args[i].equals("-multi")){
              includeMultiReads = Boolean.parseBoolean(args[i+1]);
              i++;
            }

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
        if(outPrefix==null){
          System.err.println("output filename prefix (-O) not assigned");
          System.exit(1);
        }

        // list parameters
        System.out.println("program: MappingInfoRecover");
        System.out.println("mapping method/filename (-M):");
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Map.Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): "+outPrefix);
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+ identityCutoff);
        System.out.println("minimum block (-min): "+ minimumBlockSize);
        System.out.println("check mate-pair (-mate, 0 for no check): "+checkMatePair);
        if(checkMatePair > 0){
          System.out.println("\tmin mate-pair distance (-minD): " + minDistanceCutoff);
          System.out.println("\tmax mate-pair distance (-maxD): " + maxDistanceCutoff);
        }
        System.out.println("include multi reads (-multi): " + includeMultiReads);
        System.out.println();
    }

  /****************************************************************************/
  /*                               Main function                              */
  /****************************************************************************/
  public static void main(String args[]){

    paraProc(args);

    Map chromoExonIntervalMap = new TreeMap();
    Map chromoIntronIntervalMap = new TreeMap();
    Map chromoIntronSplicingPosMap = new HashMap();

    int matedPairSerialNumber = 0; // for mated pair interval
    int readCount = 0;

    for(PairedBatchIterator pbiIt = new PairedBatchIterator(mappingFilenameMethodMap,mappingMethodMap,true); pbiIt.hasNextBatch();){
      Set batchSet = (Set)pbiIt.nextBatch();
      readCount += batchSet.size();

      if(checkMatePair > 0){
        // retrieve the alignment with its identity greater than or equal to <paramenter:identityCutoff>
        for(Iterator arrayIt = batchSet.iterator(); arrayIt.hasNext();){
          Object[] array = (Object[])arrayIt.next();
          ArrayList thisMappingRs = (ArrayList)array[0];
          float theBestIdentity = (Float)array[2];
          // skip this iteration if the best identity less than <paramenter:identityCutoff>
          if(theBestIdentity < identityCutoff){
            array[0] = new ArrayList();
            continue;
          }
          // choosing record
          ArrayList passedRs = new ArrayList();
          for(int idx = 0; idx < thisMappingRs.size(); idx++){
            AlignmentRecord record = (AlignmentRecord)thisMappingRs.get(idx);
            // filter by <parameter:identityCutoff>
            if(record.identity < identityCutoff) continue;
            if (joinFactor > 0) record.nearbyJoin(joinFactor);
            // filter by <parameter:minimumOverlap>
            int testMinBlockSize = -1;
            for (int i = 0; i < record.numBlocks; i++) {
              if (testMinBlockSize < 0 || testMinBlockSize > record.tBlockSizes[i]) {
                testMinBlockSize = record.tBlockSizes[i];
              }
            }
            if (testMinBlockSize < minimumBlockSize) continue;
            passedRs.add(record);
          }
          array[0] = passedRs;
        }


        // retrieve mated alignment info.
        if(batchSet.size()==2){
          Iterator arrayIterator = batchSet.iterator();
          Object[] arrayA = (Object[])arrayIterator.next();
          ArrayList passedRsA = (ArrayList)arrayA[0];
          Object[] arrayB = (Object[])arrayIterator.next();
          ArrayList passedRsB = (ArrayList)arrayB[0];

          Set alignmentSetA = new HashSet();
          Set alignmentSetB = new HashSet();
          for(int idx = 0; idx < passedRsA.size(); idx++){
            AlignmentRecord recordA = (AlignmentRecord)passedRsA.get(idx);
            for(int idy = 0; idy < passedRsB.size(); idy++){
              AlignmentRecord recordB = (AlignmentRecord) passedRsB.get(idy);
              if(isPairedAlignment(recordA,recordB,minDistanceCutoff,maxDistanceCutoff)){
                alignmentSetA.add(recordA);
                alignmentSetB.add(recordB);
              }
            }
          }

          // check if there exists mated pair alignment
          if(alignmentSetA.size() == 1 && alignmentSetB.size() == 1){
            arrayA[0] = new ArrayList(alignmentSetA);
            arrayB[0] = new ArrayList(alignmentSetB);
            AlignmentRecord recordA = (AlignmentRecord)alignmentSetA.iterator().next();
            AlignmentRecord recordB = (AlignmentRecord)alignmentSetB.iterator().next();
            int matedPairStart = Math.min(
                             (recordA.tStarts[recordA.numBlocks-1]+recordA.tBlockSizes[recordA.numBlocks-1]-1),
                             (recordB.tStarts[recordB.numBlocks-1]+recordB.tBlockSizes[recordB.numBlocks-1]-1));
            int matedPairStop = Math.max(recordA.tStarts[0],recordB.tStarts[0]);

            GenomeInterval matedPairInterval = new GenomeInterval(recordA.chrOriginal,matedPairStart,matedPairStop,matedPairSerialNumber++);
            matedPairISet.add(matedPairInterval);
          }
        }
      }

      // retrieve intron and exon info.
      for(Iterator arrayIt = batchSet.iterator(); arrayIt.hasNext();){
        Object[] array = (Object[])arrayIt.next();

        ArrayList mappingRecords = (ArrayList)array[0];
        String readId = (String)array[1];

        // fetch the new best identity
        float theNewBestIdentity = -1;
        for(int idx = 0; idx < mappingRecords.size(); idx++) {
          AlignmentRecord record = (AlignmentRecord) mappingRecords.get(idx);
          if(record.identity > theNewBestIdentity) theNewBestIdentity = record.identity;
        }
        // skip this iteration if the best identity less than <parameter:identityCutoff>
        if(theNewBestIdentity < identityCutoff) continue;
        // collect records with the new best identity
        ArrayList acceptedRecords = new ArrayList();
        for (int i = 0; i < mappingRecords.size(); i++) {
          AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
          if (record.identity >= theNewBestIdentity) {
            // join nearby blocks
            if (joinFactor > 0) record.nearbyJoin(joinFactor);
            acceptedRecords.add(record);
          }
        }
        // skip reads with more than one accepted records
        if (acceptedRecords.size() > 1){
          if(includeMultiReads){
            // skip it if some alignments are spliced
            boolean noSplice = true;
            for(int i=0;i<acceptedRecords.size();i++){
                if(((AlignmentRecord)acceptedRecords.get(i)).numBlocks>1){
                    noSplice = false;
                    break;
                }
            }
            if(noSplice==false) continue;

            for(int i=0;i<acceptedRecords.size();i++){
              AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(i);
              int exonStart = record.tStarts[0];
              int exonStop = record.tStarts[0]+record.tBlockSizes[0]-1;
              String chr = record.chrOriginal;
              IntervalCoverageNode thisIcn = new IntervalCoverageNode(exonStart,exonStop,readId);
              addExonInfo(chromoExonIntervalMap,chr,thisIcn);
            }
          }
          continue;
        }
        // skip reads if it minimum blocks size shorter than minimumOverlap threshold(-MIN)
        AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(0);
        int minBlockSize = -1;
        for (int i = 0; i < record.numBlocks; i++) {
          if (minBlockSize < 0 || minBlockSize > record.tBlockSizes[i]) {
            minBlockSize = record.tBlockSizes[i];
          }
        }
        if (minBlockSize < minimumBlockSize) continue;


        // retrieve exon boundary info.
        for(int i=0; i<record.numBlocks;i++){
          int exonStart = record.tStarts[i];
          int exonStop = record.tStarts[i]+record.tBlockSizes[i]-1;
          String chr = record.chrOriginal;
          IntervalCoverageNode thisIcn = new IntervalCoverageNode(exonStart,exonStop,readId);

          addExonInfo(chromoExonIntervalMap,chr,thisIcn);
        }


        // retrieve intron junction info.
        if(record.numBlocks==1) continue;
        for(int i=0; i<record.numBlocks-1;i++){
          int intronStart = record.tStarts[i] + record.tBlockSizes[i] - 1;
          int intronStop = record.tStarts[i + 1];
          int splicingPos = record.qStarts[i]+record.qBlockSizes[i]-1;

          addSpliceInfo(chromoIntronIntervalMap,chromoIntronSplicingPosMap,record.chrOriginal,intronStart,intronStop,splicingPos);
        }
      }

    }

    // assign serail number to each exon interval in <Map:chromoExonIntervalMap>
    assignSerialNumber(chromoExonIntervalMap);

    report(chromoExonIntervalMap, chromoIntronIntervalMap, chromoIntronSplicingPosMap, outPrefix, checkMatePair, matedPairISet);

    // number of reads
    System.out.println("total #read: " + readCount);
  }

  static private void addSpliceInfo(Map chromoIntronIntervalMap,Map chromoIntronSplicingPosMap,String chr,int intronStart,int intronStop,int splicingPos){
    Interval intronInterval = new Interval(intronStart,intronStop);
    if(chromoIntronIntervalMap.containsKey(chr)){
      Map IntronIntervalCntMap = (Map)chromoIntronIntervalMap.get(chr);
      if(IntronIntervalCntMap.containsKey(intronInterval)){
        int intervalCnt = (Integer)IntronIntervalCntMap.get(intronInterval) + 1;
        IntronIntervalCntMap.put(intronInterval,intervalCnt);
        chromoIntronIntervalMap.put(chr,IntronIntervalCntMap);
      }else{
        IntronIntervalCntMap.put(intronInterval,1);
        chromoIntronIntervalMap.put(chr,IntronIntervalCntMap);
      }
    }else{
      Map NewIntronIntervalCntMap = new TreeMap();
      NewIntronIntervalCntMap.put(intronInterval,1);
      chromoIntronIntervalMap.put(chr,NewIntronIntervalCntMap);
    }

    if(chromoIntronSplicingPosMap.containsKey(chr)){
        Map intronSplicingPosMap = (Map) chromoIntronSplicingPosMap.get(chr);
        if(intronSplicingPosMap.containsKey(intronInterval)){
            Map splicingPosMap = (Map) intronSplicingPosMap.get(intronInterval);
            if(splicingPosMap.containsKey(splicingPos)){
                int val = (Integer) splicingPosMap.get(splicingPos);
                splicingPosMap.put(splicingPos,val+1);
            }else{
                splicingPosMap.put(splicingPos,1);
            }
        }else{
            Map splicingPosMap = new TreeMap();
            splicingPosMap.put(splicingPos,1);
            intronSplicingPosMap.put(intronInterval,splicingPosMap);
        }
    }else{
        Map intronSplicingPosMap = new HashMap();
        Map splicingPosMap = new TreeMap();
        splicingPosMap.put(splicingPos,1);
        intronSplicingPosMap.put(intronInterval,splicingPosMap);
        chromoIntronSplicingPosMap.put(chr,intronSplicingPosMap);
    }
  }

  static private void addExonInfo(Map chromoExonIntervalMap,String chr,IntervalCoverageNode thisIcn){
    //check if the chr exists in ChromoIntronMap
    if(!chromoExonIntervalMap.containsKey(chr)){
      TreeSet NewExonIntervalSet = new TreeSet();
      NewExonIntervalSet.add(thisIcn);
      chromoExonIntervalMap.put(chr,NewExonIntervalSet);
    }else{
      //check if the read have intersection with other reads in ExonIntervalSet
      TreeSet ExonIntervalSet = (TreeSet)chromoExonIntervalMap.get(chr);
      //store IntervalCoverageNode which are ready to be combined
      TreeSet combinedSet = new TreeSet();

      //retrieve the subset of ExonIntervalSet accroding to thisIcn's start and stop points
      IntervalCoverageNode from = new IntervalCoverageNode(thisIcn.getStart(),thisIcn.getStart());
      IntervalCoverageNode to = new IntervalCoverageNode(thisIcn.getStop()+1,thisIcn.getStop()+1);
      TreeSet subset =(TreeSet)ExonIntervalSet.subSet(from,to);
      if(!subset.isEmpty()) combinedSet.addAll(subset);
      //retrieve the last element in tailset of ExonIntervalSet accroding to thisIcn's start point
      TreeSet headSet =(TreeSet)ExonIntervalSet.headSet(thisIcn);
      if(!headSet.isEmpty() && thisIcn.intersect((IntervalCoverageNode)headSet.last())) combinedSet.add(headSet.last());

      //check if there is any IntervalCoverageNode needed to be combined in combinedSet
      if(!combinedSet.isEmpty()){
        combinedSet.add(thisIcn);
        //excute combination and get a new IntervalCoveragenode
        IntervalCoverageNode NewIcn = thisIcn.combine(combinedSet);
        //update ExonIntervalSet and ChromoIntronMap and clear combinedSet
        ExonIntervalSet.removeAll(combinedSet);
        ExonIntervalSet.add(NewIcn);
        chromoExonIntervalMap.put(chr,ExonIntervalSet);
        combinedSet.clear();
      }else{
        ExonIntervalSet.add(thisIcn);
        chromoExonIntervalMap.put(chr,ExonIntervalSet);
      }
    }
  }

  static private void assignSerialNumber(Map chromoExonIntervalMap){
    int serialNumber = 0;
    for(Iterator chrIt = chromoExonIntervalMap.keySet().iterator(); chrIt.hasNext();){
      String chr = (String)chrIt.next();
      Set exonSet = (Set)chromoExonIntervalMap.get(chr);
      for(Iterator exonIt = exonSet.iterator(); exonIt.hasNext();){
        IntervalCoverageNode icn = (IntervalCoverageNode)exonIt.next();
        icn.setUserObject(++serialNumber);
      }
    }
  }

  static private boolean isPairedAlignment(AlignmentRecord alignmentA, AlignmentRecord alignmentB, int minDistanceCutoff, int maxDistanceCutoff){
    //ckeck if two alignments are the different sense
    if(!(alignmentA.forwardStrand ^ alignmentB.forwardStrand)) return false;
    //check two alignment are on the same chromosom
    if(alignmentA.chr != alignmentB.chr) return false;
    //check the mapping position of two alignments
    if(alignmentA.forwardStrand==true){
      if((alignmentA.tStarts[0] - alignmentB.tStarts[0]) >= 0) return false;
    }else{
      if((alignmentB.tStarts[0] - alignmentA.tStarts[0]) >= 0) return false;
    }
    //check if two alignments have the intersection of mapped genes
    int distance = (alignmentA.tStarts[0] - alignmentB.tStarts[0] < 0 ?
        alignmentB.tStarts[0] - (alignmentA.tStarts[alignmentA.numBlocks-1]+alignmentA.tBlockSizes[alignmentA.numBlocks-1]-1) :
        alignmentA.tStarts[0] - (alignmentB.tStarts[alignmentB.numBlocks-1]+alignmentB.tBlockSizes[alignmentB.numBlocks-1]-1));
    if(distance < minDistanceCutoff || distance > maxDistanceCutoff) return false;

    //if two alignment pass above criteria, then we consider they are pair-end alignment
    return true;
  }

  static private void report(Map exonMap, Map intronMap, Map intronSplicingPosMap, String filename, int checkMatePair, Set matedPairISet){
    try{
      //step1:print exon interval information
      FileWriter efw = new FileWriter(filename+".exonInfo");
      // title
      efw.write("#Chr\tSerialNumber\tStart\tStop\tCoverageArray\n");
      // exon content
      for(Iterator exonMapIt = exonMap.keySet().iterator();exonMapIt.hasNext();){
        String chr = (String) exonMapIt.next();
        TreeSet ExonIntervalSet = (TreeSet)exonMap.get(chr);
        for(Iterator exonIntervalIt = ExonIntervalSet.iterator();exonIntervalIt.hasNext();){
          IntervalCoverageNode intervalCoverageNode = (IntervalCoverageNode)exonIntervalIt.next();
          efw.write(chr + "\t" +
                    intervalCoverageNode.getUserObject() + "\t" +
                    intervalCoverageNode.getStart() + "\t" +
                    intervalCoverageNode.getStop() + "\t" +
                    Arrays.toString(intervalCoverageNode.getCoverageArray()) + "\n");
        }
      }
      efw.close();

      //step2:print exon interval information
      FileWriter ifw = new FileWriter(filename+".intronInfo");
      // title
      ifw.write("#Chr\tStart\tStop\t#Count\n");
      // exon content
      for(Iterator intronMapIt = intronMap.keySet().iterator();intronMapIt.hasNext();){
        String chr = (String)intronMapIt.next();
        Map intervalCntMap = (Map)intronMap.get(chr);
        Map intervalSplicingPosMap = (Map) intronSplicingPosMap.get(chr);
        for(Iterator intervalCntMapIt = intervalCntMap.keySet().iterator();intervalCntMapIt.hasNext();){
          Interval intronInterval = (Interval)intervalCntMapIt.next();
          int support = (Integer)intervalCntMap.get(intronInterval);
          Map splicingPosMap = (Map) intervalSplicingPosMap.get(intronInterval);
          ifw.write(chr + "\t" + intronInterval.getStart() + "\t" + intronInterval.getStop() + "\t" + support + "\t" + splicingPosMap + "\n");
        }
      }
      ifw.close();
    }catch(IOException e){
      e.printStackTrace();
      System.exit(1);
    }

    //step2:print mated Pair information
    if(checkMatePair>0 && matedPairISet.size()!=0){
      CanonicalGFF icnCgff = new CanonicalGFF(exonMap);
      for(Iterator matedPairIIt = matedPairISet.iterator(); matedPairIIt.hasNext();){
        GenomeInterval matedPairI = (GenomeInterval)matedPairIIt.next();

        // THIS filter was moved to TranscriptomeRecover
        // skip this matePairInterval if it satisfies:
        // a. it crosses two genes.
        // b. the first genomeInterval has no intersection with the last genomeInterval
        // TreeSet checkSet = new TreeSet(cgff.getRelatedGenes(matedPairI.getChr(),matedPairI.getStart(),matedPairI.getStop(),false,false,1));
        // if(checkSet.size()>=2){
        //   GenomeInterval gi1 = (GenomeInterval)checkSet.first();
        //   GenomeInterval gi2 = (GenomeInterval)checkSet.last();
        //   if(gi1.intersect(gi2)==false) continue;
        // }

        // find relationship between exon intervals
        TreeSet rltExonSet = new TreeSet(icnCgff.getRelatedGenes(matedPairI.getChr().toLowerCase().intern(),matedPairI.getStart(),matedPairI.getStop(),false,false,1));
        if(rltExonSet.size()==1) continue;

        GenomeInterval firstICN = (GenomeInterval)rltExonSet.first();
        GenomeInterval lastICN = (GenomeInterval)rltExonSet.last();

        Interval i = new Interval((Integer)firstICN.getUserObject(),(Integer)lastICN.getUserObject());

        if(icnRelatedMap.containsKey(i)){
          int cnt = (Integer)icnRelatedMap.get(i);
          icnRelatedMap.put(i,cnt+1);
        }else{
          icnRelatedMap.put(i,1);
        }
      }

      try {
        FileWriter fw = new FileWriter(outPrefix + ".matePairInfo");
        // header
        fw.write("#exonRelation\t#count\n");
        // content
        for (Iterator It = icnRelatedMap.keySet().iterator(); It.hasNext(); ) {
          Interval i = (Interval) It.next();
          int cnt = (Integer) icnRelatedMap.get(i);
          fw.write(i.getStart()+":"+i.getStop() + "\t" + cnt + "\n");
        }
        fw.close();
      }
      catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
