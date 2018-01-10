package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.MappingResultIterator;
import misc.Util;
import net.sf.samtools.SAMRecord;

/**
 * This program is for doing many of alignment filtering. Outputs PSL format files.
 * @version 1.0
 * @author Chun-Mao Fu
 * @author Wen-Dar Lin
 */

public class AlignmentFilter {

  // qualification parameters
  private static float identityCutoff = 0.0F;
  private static float mappingIdentityCutoff = 0.0F;
  private static boolean topOnly = false;
  private static boolean uniqOnly = false;
  private static int minimumBlock = 0;
  private static int maximumSplice = 0;
  private static int multiLimit = 0;
  private static String gffFilename = null;
  private static int joinFactor = 0;
  private static boolean useExonRegion = false;
  private static boolean checkByContaining = false;
  private static int minimumOverlap = 8;
  private static boolean checkAllBlocks = false;
  private static int checkMatePair = 0;
  // mapping result
  private static Map mappingFilenameMethodMap = new LinkedHashMap();
  private static Map mappingMethodMap = null;
  // alignment translation
  private static String translationGffFilename = null;
  // output
  private static String appendStr = "";
  private static String outputFilename = null;
  private static boolean forceSAMout = true;
  // misc
  private static boolean quiet = false;

  /****************************************************************************/
  /*                   Parameter processing on command line                   */
  /****************************************************************************/
  static private void paraProc(String[] args){

    // get parameter strings
    for(int i=0;i<args.length;i++){
      if (args[i].equals("-M")) {
        mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
        i += 2;
      } else if (args[i].equals("-J")) {
          joinFactor = Integer.parseInt(args[i + 1]);
          i++;
      } else if (args[i].equals("-ID")) {
          identityCutoff = Float.parseFloat(args[i + 1]);
          i++;
      } else if (args[i].equals("-mID")) {
          mappingIdentityCutoff = Float.parseFloat(args[i + 1]);
          i++;
      } else if (args[i].equals("-top")) {
          topOnly = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-uniq")) {
          uniqOnly = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-minB")) {
          minimumBlock = Integer.parseInt(args[i + 1]);
          i++;
      } else if (args[i].equals("-splice")) {
          maximumSplice = Integer.parseInt(args[i + 1]);
          i++;
      } else if (args[i].equals("-multi")) {
          multiLimit = Integer.parseInt(args[i + 1]);
          i++;
      } else if(args[i].equals("-GFF")){
        gffFilename = args[i + 1];
        i++;
      } else if(args[i].equals("-translation")){
        translationGffFilename = args[i + 1];
        i++;
      } else if (args[i].equals("-exon")) {
          useExonRegion = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-contain")) {
          checkByContaining = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-min")) {
          minimumOverlap = Integer.parseInt(args[i + 1]);
          i++;
      } else if (args[i].equals("-ALL")) {
          checkAllBlocks = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-mate")) {
          checkMatePair = Integer.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-O")) {
          outputFilename = args[i + 1];
          i++;
      } else if (args[i].equals("-append")) {
          appendStr = args[i + 1];
          i++;
      } else if (args[i].equals("-forceSAM")) {
          forceSAMout = Boolean.valueOf(args[i + 1]);
          i++;
      } else if (args[i].equals("-quiet")) {
          quiet = Boolean.valueOf(args[i + 1]);
          i++;
      }
    }

    mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",System.getProperty("java.class.path"), "misc");
    if (mappingFilenameMethodMap.size() <= 0) {
      System.err.println("mapping method/filename (-M) isn't assigned, available methods:");
      for (Iterator iterator = mappingMethodMap.keySet().iterator();iterator.hasNext(); ) {
        System.out.println(iterator.next());
      }
      System.exit(1);
    }

    for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext(); ) {
      String mappingMethod = (String) methodIterator.next();
      if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
        System.err.println("assigned mapping method (-M) doesn't exists: " +mappingMethod + ", available methods:");
        for (Iterator iterator = mappingMethodMap.keySet().iterator();iterator.hasNext(); ) {
          System.out.println(iterator.next());
        }
        System.exit(1);
      }
    }

    if (outputFilename == null) {
      System.err.println("output filename (-O) isn't assigned");
      System.exit(1);
    }

    // list parameters
    if(quiet) return;

    System.out.println("program: AlignmentFilter");
    System.out.println("mapping method/filename (-M):");
    for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext(); ) {
      Map.Entry entry = (Map.Entry) iterator.next();
      System.out.println("  " + entry.getValue() + " : " + entry.getKey());
    }
    System.out.println("identity cutoff (-ID): "+ identityCutoff);
    System.out.println("mapping identity cutoff (-mID): "+ mappingIdentityCutoff);
    System.out.println("top only (-top): "+ topOnly);
    System.out.println("uniq only (-uniq): "+ uniqOnly);
    System.out.println("translation (-translation): "+ translationGffFilename);
    System.out.println("block join factor (-J): "+joinFactor);
    System.out.println("minimum block (-minB): "+ minimumBlock);
    System.out.println("splice limit (-splice, 0 for none): "+ maximumSplice);
    System.out.println("multiplicity limit (-multi, 0 for none): "+ multiLimit);
    System.out.println("canonical GFF filename (-GFF): "+gffFilename);
    if(gffFilename!=null){
        System.out.println("use exon region (-exon): "+useExonRegion);
        System.out.println("check by containing (-contain, FALSE for by intersecting): "+checkByContaining);
        System.out.println("minimum overlap (-min): "+ minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): "+checkAllBlocks);
        System.out.println("check mate-pair (-mate, 0 for no check): "+checkMatePair);
    }
    System.out.println("output filename (-O): " + outputFilename);
    if(appendStr.length()>0){
        System.out.println("readID appending string (-append): " + appendStr);
    }
    System.out.println("force SAM output for SAM records (-forceSAM): " + forceSAMout);
    System.out.println();
  }

  static private boolean isPairedAlignment(AlignmentRecord alignmentA, AlignmentRecord alignmentB,
          Set hitGeneSetA, Set hitGeneSetB){
    //ckeck if two alignments are the different sense
    if(!(alignmentA.forwardStrand ^ alignmentB.forwardStrand)) return false;
    //check the mapping position of two alignments
    if(alignmentA.forwardStrand==true){
      if((alignmentA.tStarts[0] - alignmentB.tStarts[0]) >= 0) return false;
    }else{
      if((alignmentB.tStarts[0] - alignmentA.tStarts[0]) >= 0) return false;
    }
    //check if two alignments have the intersection of mapped genes
    HashSet aGeneSet = new HashSet(hitGeneSetA);
    HashSet bGeneSet = new HashSet(hitGeneSetB);
    aGeneSet.retainAll(bGeneSet);
    if(aGeneSet.size()<=0) return false;

    //if two alignment pass above criteria, then we consider they are pair-end alignment
    return true;
  }

  /****************************************************************************/
  /*                               Main function                              */
  /****************************************************************************/
  public static void main(String args[]){

    paraProc(args);

    CanonicalGFF cgff = null;
    if(gffFilename!=null){
        cgff = new CanonicalGFF(gffFilename);
    }

    CanonicalGFF translationCgff = null;
    if (translationGffFilename != null) {
    	translationCgff = new CanonicalGFF(translationGffFilename);
    }

    try{
      FileWriter fw = new FileWriter(new File(outputFilename));

      int numTotalRead=0;
      int numPassedRead = 0;

      String preReadID="";
      int preReadLength=0;
      ArrayList preMappingRecords = new ArrayList();
      LinkedHashMap preAlignmentGenesMap = new LinkedHashMap();
      String curReadID = "";
      int curReadLength = 0;
      ArrayList curMappingRecords= new ArrayList();
      LinkedHashMap curAlignmentGenesMap = new LinkedHashMap();

      boolean getMatePair = false;

      for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator();mriIterator.hasNext(); ) {
        Map.Entry entry = (Map.Entry) mriIterator.next();
        String mappingFilename = (String) entry.getKey();
        String mappingMethod = (String) entry.getValue();
        // iterate alignments
        int mappedReadCnt = 0;
        int passedReadCnt = 0;
        int processedLines = 0;
        for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod); mappingResultIterator.hasNext(); ){
          // get one read with mapping
          numTotalRead++;
          curMappingRecords= (ArrayList)mappingResultIterator.next();
          curReadID = mappingResultIterator.getReadID();
          curReadLength = mappingResultIterator.getReadLength();

          mappedReadCnt++;
          processedLines += curMappingRecords.size();

          ArrayList<AlignmentRecord> acceptedRecords;
          // identity filter
          acceptedRecords = new ArrayList<AlignmentRecord>();
          if (mappingResultIterator.getBestIdentity() >= identityCutoff) {
              for (int i = 0; i < curMappingRecords.size(); i++) {
                  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
                  // qualification
                  if (record.identity >= identityCutoff)
                      acceptedRecords.add(record);

              }
          }
          curMappingRecords = acceptedRecords;

          // mapping identity filter
          acceptedRecords = new ArrayList<AlignmentRecord>();
          int readLength = mappingResultIterator.getReadLength();
          for (int i = 0; i < curMappingRecords.size(); i++) {
              AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
              // qualification
              if (record.getMappingIdentity() >= mappingIdentityCutoff)
                  acceptedRecords.add(record);
          }
          curMappingRecords = acceptedRecords;

          // topOnly
          if(topOnly){
              acceptedRecords = new ArrayList<AlignmentRecord>();
              for (int i = 0; i < curMappingRecords.size(); i++) {
                  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
                  // qualification
                  if (record.identity >= mappingResultIterator.getBestIdentity()) {
                      acceptedRecords.add(record);
                  }
              }
              curMappingRecords = acceptedRecords;
          }
          // uniqOnly
          if(uniqOnly){
              if(curMappingRecords.size()>1) curMappingRecords.clear();
          }
          // translation
          if (translationGffFilename != null) {
        	  acceptedRecords = new ArrayList<AlignmentRecord>();

        	  Set set = new HashSet();
        	  for (int i = 0; i < curMappingRecords.size(); i++) {
        		  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
        		  AlignmentRecord newRecord = record.translate(translationCgff, curReadLength);
                          if(newRecord!=null) set.add(newRecord);
        	  }
        	  acceptedRecords.addAll(set);
        	  curMappingRecords = acceptedRecords;
          }
          // join factor
          if(joinFactor>0){
              acceptedRecords = new ArrayList();
              for (int i = 0; i < curMappingRecords.size(); i++) {
                  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
                  record.nearbyJoin(joinFactor);
                  acceptedRecords.add(record);
              }
              curMappingRecords = acceptedRecords;
          }
          // minimum block
          acceptedRecords = new ArrayList();
          for (int i = 0; i < curMappingRecords.size(); i++) {
              AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
              // qualification
              boolean qualified = true;
              for(int j=0;j<record.numBlocks;j++){
                  int minSide = Math.min(record.qBlockSizes[j],record.tBlockSizes[j]);
                  if(minSide<minimumBlock){
                      qualified = false;
                      break;
                  }
              }
              if(qualified){
                  acceptedRecords.add(record);
              }
          }
          curMappingRecords = acceptedRecords;
          // maximum splice
          if(maximumSplice>0){
              acceptedRecords = new ArrayList();
              for (int i = 0; i < curMappingRecords.size(); i++) {
                  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
                  // qualification
                  boolean qualified = true;
                  for(int j=1;j<record.numBlocks;j++){
                      int spliceDistance = record.tStarts[j] - (record.tStarts[j-1] + record.tBlockSizes[j-1] -1) - 1;
                      if(spliceDistance>maximumSplice){
                          qualified = false;
                          break;
                      }
                  }
                  if(qualified){
                      acceptedRecords.add(record);
                  }
              }
              curMappingRecords = acceptedRecords;
          }
          // multiplicity limit
          if(multiLimit>0){
              if (curMappingRecords.size()<=multiLimit) {
                // do nothing
              }else{
                // remove all alignments when exceed multiplicity limit
                curMappingRecords.clear();
              }
          }
          // gene filter if CGFF is given
          if(cgff!=null){
              acceptedRecords = new ArrayList();
              curAlignmentGenesMap = new LinkedHashMap();
              for (int i = 0; i < curMappingRecords.size(); i++) {
                  AlignmentRecord record = (AlignmentRecord) curMappingRecords.get(i);
                  // qualification
                  Set hitGeneRegions = cgff.getRelatedGenes(record.chr,
                          record.getMappingIntervals(), useExonRegion,
                          checkByContaining, minimumOverlap, checkAllBlocks);
                  if (hitGeneRegions.size() > 0) {
                      acceptedRecords.add(record);
                      curAlignmentGenesMap.put(record,hitGeneRegions);
                  }
              }
              curMappingRecords = acceptedRecords;
          }
          // pair filter if CGFF is given AND checkMatePair is true
          if(cgff!=null && checkMatePair > 0){
              if(Util.isReadIDPaired(preReadID,curReadID,checkMatePair)==false){
                  preReadID = curReadID;
                  preReadLength = curReadLength;
                  preMappingRecords = curMappingRecords;
                  preAlignmentGenesMap = curAlignmentGenesMap;
                  getMatePair = false;
              }else{
                  //find intersecetion gene regions of preRecord and curRecord
                  Set preGeneRegions = new HashSet();
                  for(Iterator recIterator = preAlignmentGenesMap.keySet().iterator();recIterator.hasNext();){
                    AlignmentRecord preRecord = (AlignmentRecord)recIterator.next();
                    preGeneRegions.addAll((Set) preAlignmentGenesMap.get(preRecord));
                  }
                  Set curGeneRegions = new HashSet();
                  for(Iterator recIterator = curAlignmentGenesMap.keySet().iterator();recIterator.hasNext();){
                    AlignmentRecord curRecord = (AlignmentRecord)recIterator.next();
                    curGeneRegions.addAll((Set) curAlignmentGenesMap.get(curRecord));
                  }

                  //intersecetion
                  curGeneRegions.retainAll(preGeneRegions);
                  if(curGeneRegions.size()<=0) continue;

                  //check if any two alignments are paired alignment
                  Set preSet = new HashSet();
                  Set curSet = new HashSet();
                  for(Iterator preRecIt = preAlignmentGenesMap.keySet().iterator();preRecIt.hasNext();){
                    AlignmentRecord preRecord = (AlignmentRecord)preRecIt.next();
                    for(Iterator curRecIt = curAlignmentGenesMap.keySet().iterator();curRecIt.hasNext();){
                      AlignmentRecord curRecord = (AlignmentRecord)curRecIt.next();
                      if(isPairedAlignment(preRecord,curRecord,
                                           (Set)preAlignmentGenesMap.get(preRecord),
                                           (Set)curAlignmentGenesMap.get(curRecord))){
                        preSet.add(preRecord);
                        curSet.add(curRecord);
                      }
                    }
                  }
                  // qualification
                  preMappingRecords.retainAll(preSet);
                  curMappingRecords.retainAll(curSet);
                  getMatePair = true;
              }
          }

          // output -- pair filter
          if(cgff!=null && checkMatePair > 0){
              if(getMatePair && preMappingRecords.size()>0 && curMappingRecords.size()>0){
                  for(Iterator preAlnIt = preMappingRecords.iterator();preAlnIt.hasNext();){
                    AlignmentRecord preRecord = (AlignmentRecord)preAlnIt.next();
                    preRecord.reset();
                    if(forceSAMout && preRecord.sourceObj!=null && preRecord.sourceObj instanceof SAMRecord){
                      SAMRecord samRecord = (SAMRecord) preRecord.sourceObj;
                      if(appendStr.length()>0) samRecord.setReadName(samRecord.getReadName()+appendStr);
                      fw.write(samRecord.getSAMString().trim() + "\n");
                    }else{
                      fw.write(preRecord.toBLAT(preReadID+appendStr,preReadLength)+"\n");
                    }
                  }
                  for(Iterator curAlnIt = curMappingRecords.iterator();curAlnIt.hasNext();){
                    AlignmentRecord curRecord = (AlignmentRecord)curAlnIt.next();
                    curRecord.reset();
                    if(forceSAMout && curRecord.sourceObj!=null && curRecord.sourceObj instanceof SAMRecord){
                      SAMRecord samRecord = (SAMRecord) curRecord.sourceObj;
                      if(appendStr.length()>0) samRecord.setReadName(samRecord.getReadName()+appendStr);
                      fw.write(samRecord.getSAMString().trim() + "\n");
                    }else{
                      fw.write(curRecord.toBLAT(curReadID+appendStr,curReadLength)+"\n");
                    }
                  }
                  numPassedRead += 2;
                  passedReadCnt += 2;
              }
          }else{ // output -- identity filter & gene filter
              if(curMappingRecords.size()>0){
                  for(Iterator curAlnIt = curMappingRecords.iterator();curAlnIt.hasNext();){
                    AlignmentRecord curRecord = (AlignmentRecord)curAlnIt.next();
                    curRecord.reset();
                    if(forceSAMout && curRecord.sourceObj!=null && curRecord.sourceObj instanceof SAMRecord){
                      SAMRecord samRecord = (SAMRecord) curRecord.sourceObj;
                      if(appendStr.length()>0) samRecord.setReadName(samRecord.getReadName()+appendStr);
                      fw.write(samRecord.getSAMString().trim() + "\n");
                    }else{
                      fw.write(curRecord.toBLAT(curReadID+appendStr,curReadLength)+"\n");
                    }
                  }
                  numPassedRead ++;
                  passedReadCnt ++;
              }
          }
          // end loop of read iteration
        }
        if(!quiet) System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename + ", passed: " + passedReadCnt);
      }
      fw.close();
      if(!quiet) System.out.println("#total read: " + numTotalRead);
      if(!quiet) System.out.println("#passed read: " + numPassedRead);
    }catch(IOException e){
      e.printStackTrace();
      System.exit(1);
    }
  }
}
