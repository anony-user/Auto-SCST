package rnaseq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import misc.MappingResultIterator;
import misc.StringComparator;
import misc.Util;

/**
 * <P> Title: Region Tracer
 * <P> Description: This class has subtly different from Gene Tracer in mult-read determination.
 *                  That is, Gene Tracer thinks a read is multiple if it has alignment with at least
 *                  two or more genes. However, Region Tracer determines by if the read has
 *                  at least tow or more accepted records.
 * @author Chun-Mao Fu
 * @version 1.0
 */

public class RegionTracer {

  //declare input and output parameters
  private static String gffFilename = null;
  private static String modelFilename = null;
  private static Map mappingMethodMap = null;
  private static Map mappingFilenameMethodMap = new LinkedHashMap();
  private static String outputPrefix = null;

  private static int joinFactor = 0;
  private static float identityCutoff = 0.95F;
  private static String traceFilename = null;

  private static Map geneFileMap = new HashMap();
  private static Map geneBufferMap = new HashMap();
  private static int traceID = 1;
  private static int bufferLimit = 10240;


  /****************************************************************************/
  /*                   Parameter processing on command line                   */
  /****************************************************************************/
  static private void paraProc(String[] args) {

    // get parameter strings
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-GFF")) {
        gffFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-M")) {
        mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
        i += 2;
      }
      else if (args[i].equals("-J")) {
        joinFactor = Integer.parseInt(args[i + 1]);
        i++;
      }
      else if (args[i].equals("-ID")) {
        identityCutoff = Float.parseFloat(args[i + 1]);
        i++;
      }
      else if (args[i].equals("-O")) {
        outputPrefix = args[i + 1];
        i++;
      }
      else if (args[i].equals("-model")) {
        modelFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-traceFile")) {
        traceFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-buffer")) {
        bufferLimit = Integer.parseInt(args[i + 1]);
        i++;
      }
    }

    // check for necessary parameters
    if (gffFilename == null) {
      System.err.println("canonical GFF filename (-GFF) isn't assigned");
      System.exit(1);
    }
    mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",
                                         System.getProperty("java.class.path"),
                                         "misc");
    if (mappingFilenameMethodMap.size() <= 0) {
      System.err.println(
          "mapping method/filename (-M) isn't assigned, available methods:");
      for (Iterator iterator = mappingMethodMap.keySet().iterator();
           iterator.hasNext(); ) {
        System.out.println(iterator.next());
      }
      System.exit(1);
    }
    for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator();
         methodIterator.hasNext(); ) {
      String mappingMethod = (String) methodIterator.next();
      if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
        System.err.println("assigned mapping method (-M) isn't exists: " +
                           mappingMethod + ", available methods:");
        for (Iterator iterator = mappingMethodMap.keySet().iterator();
             iterator.hasNext(); ) {
          System.out.println(iterator.next());
        }
        System.exit(1);
      }
    }
    if (outputPrefix == null) {
      System.err.println("output prefix (-O) isn't assigned");
      System.exit(1);
    }
    if (traceFilename == null) {
      System.out.println("trace filename(-traceFile) isn't assigned");
      System.exit(1);
    }

    // list parameters
    System.out.println("program: RegionTracer");
    System.out.println("canonical GFF filename (-GFF): " + gffFilename);
    System.out.println("mapping method/filename (-M):");
    for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();
         iterator.hasNext(); ) {
      Map.Entry entry = (Entry) iterator.next();
      System.out.println("  " + entry.getValue() + " : " + entry.getKey());
    }
    System.out.println("output prefix (-O): " + outputPrefix);
    System.out.println("block join factor (-J): " + joinFactor);
    System.out.println("identity cutoff (-ID): " + identityCutoff);
    System.out.println("model filename (-model): " + modelFilename);
    System.out.println("traceing gene list in file (-traceFile): " + traceFilename);
    System.out.println("buffer size per region (-buffer): " + bufferLimit);
    System.out.println();

  }

  /****************************************************************************/
  /*                  Transform tracefile into chrIntervalMap                 */
  /****************************************************************************/
  static private TreeMap getChrIntervalMap(String traceFilename, CanonicalGFF cgff) throws IOException {
    TreeMap chrIntervalMap = new TreeMap();
    BufferedReader fr = new BufferedReader(new FileReader(traceFilename));
    while (fr.ready()) {
      String line = fr.readLine();
      //if the line is blank, skip the line
      if(line.trim().length()<=0) continue;

      String[] token = line.split("\t");
      //if the line in format of <chr start stop>
      if (token.length == 3) {
        String chr = token[0];
        String regionStart = token[1];
        String regionStop = token[2];
        String regionID = chr + "_" + regionStart + "_" + regionStop;
        if (chrIntervalMap.containsKey(chr)) {
          Set oldIntervalSet = (Set) chrIntervalMap.get(chr);
          oldIntervalSet.add(new Interval(Integer.parseInt(regionStart),Integer.parseInt(regionStop),regionID));
        }else {
          TreeSet newIntervalSet = new TreeSet();
          newIntervalSet.add(new Interval(Integer.parseInt(regionStart),Integer.parseInt(regionStop),regionID));
          chrIntervalMap.put(chr, newIntervalSet);
        }
      }//else if the line in format of <geneID>
      else{
        String regionID = token[0];
        if(cgff.geneRegionMap.containsKey(regionID)==false){
          System.err.println(regionID +" doesn't exist in "+ gffFilename);
          continue;
        }
        GenomeInterval geneRegion = (GenomeInterval) cgff.geneRegionMap.get(regionID);
        if (chrIntervalMap.containsKey(geneRegion.getChr())) {
          Set oldIntervalSet = (Set) chrIntervalMap.get(geneRegion.getChr());
          oldIntervalSet.add(new Interval(geneRegion.getStart(),geneRegion.getStop(), regionID));
        }else {
          TreeSet newIntervalSet = new TreeSet();
          newIntervalSet.add(new Interval(geneRegion.getStart(),geneRegion.getStop(), regionID));
          chrIntervalMap.put(geneRegion.getChr(), newIntervalSet);
        }
      }
    }
    return chrIntervalMap;
  }

  static private void writeExons(FileWriter fw,String title,Set intervals,String strand) throws IOException {
    for(Iterator exonIterator = intervals.iterator();exonIterator.hasNext();){
      Interval exon = (Interval) exonIterator.next();
      fw.write(title + "\t" + strand + "\t" + exon.getStart() + "\t" + exon.getStop() + "\n");
    }
  }

  static private void countReadMulti(String readID,ArrayList acceptedRecords,CanonicalGFF newcgff){

    for(int i=0; i<acceptedRecords.size();i++){
      AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(i);
      Set regionSet = newcgff.getRelatedGenes(record.chr, record.getMappingIntervals(), true, false, 1, false);

      if (regionSet.size() == 0) continue;
      for(Iterator regionIt = regionSet.iterator();regionIt.hasNext();){
        Interval region = (Interval)regionIt.next();
        String regionID = (String) region.getUserObject();
        StringBuffer buffer = (StringBuffer) geneBufferMap.get(regionID);
        String appendString = "";
        appendString += (Util.multiString + "\t" +
                         record.getStrand() + "\t" +
                         record.tStarts[0] + "\t" +
                         (record.tStarts[0] + record.tBlockSizes[0] - 1) +
                         "\t" +
                         traceID + "\t" +
                         "0" + "\t" +
                         readID + "\n");
        traceID++;
        buffer.append(appendString);

        try {
          if (buffer.length() > bufferLimit) {
            File file = (File) geneFileMap.get(regionID);
            FileWriter fw = new FileWriter(file, true);
            fw.write(buffer.toString());
            fw.close();
            buffer.setLength(0);
          }
        }catch (IOException ex) {
          ex.printStackTrace();
          System.exit(1);
        }

        geneBufferMap.put(regionID, buffer);

      }
    }
  }

  static private void countReadUnique(String readID, AlignmentRecord record,CanonicalGFF newcgff){
    //check if the read has intersaction with query region
    Set regionSet = newcgff.getRelatedGenes(record.chr, record.getMappingIntervals(), true, false, 1,false);

    //check if read has intersaction with region intereval
    if(regionSet.size()==0) return;
    for(Iterator regionIt = regionSet.iterator();regionIt.hasNext();){
      Interval region = (Interval) regionIt.next();
      String regionID = (String) region.getUserObject();
      StringBuffer buffer = (StringBuffer) geneBufferMap.get(regionID);
      String appendString = "";
      //deal with splice read
      if (record.numBlocks > 1) {

        for (int i = 0; i < record.numBlocks; i++) {
          appendString +=  (Util.spliceString + "\t" + record.getStrand() + "\t" + record.tStarts[i] +
                            "\t" + (record.tStarts[i] + record.tBlockSizes[i] - 1) + "\t" +
                            traceID + "\t" + i);
          if (i == 0) {
            appendString += ("\t" + readID + "\n");
          }
          else {
            appendString += ("\n");
          }
        }
      } //deal with unique read
      else {
        appendString +=(Util.uniqString + "\t" + record.getStrand() + "\t" + record.tStarts[0] + "\t" +
                        (record.tStarts[0] + record.tBlockSizes[0] - 1) + "\t" +
                        traceID + "\t" + "0" + "\t" + readID + "\n");
      }
      traceID++;

      buffer.append(appendString);
      // do writing and buffer-cleaning if exceed bufferLimit
      try {
        if (buffer.length() > bufferLimit) {
          File file = (File) geneFileMap.get(regionID);
          FileWriter fw = new FileWriter(file, true);
          fw.write(buffer.toString());
          fw.close();
          buffer.setLength(0);
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
        System.exit(1);
      }
      geneBufferMap.put(regionID, buffer);
    }

  }

  static private void report() {
    // write rest buffers
    try {
      for (Iterator iterator = geneFileMap.keySet().iterator(); iterator.hasNext(); ) {
        String geneID = (String) iterator.next();
        File file = (File) geneFileMap.get(geneID);
        StringBuffer buffer = (StringBuffer) geneBufferMap.get(geneID);
        if (buffer.length() > 0) {
          FileWriter fw = new FileWriter(file, true);
          fw.write(buffer.toString());
          fw.close();
        }
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  /****************************************************************************/
  /*                               Main function                              */
  /****************************************************************************/
  public static void main(String args[]) throws IOException {

    paraProc(args);

    CanonicalGFF cgff = new CanonicalGFF(gffFilename);

    // get trace lists in chromosome-interval Map
    TreeMap traceChrIntervalMap = getChrIntervalMap(traceFilename, cgff);

    // gene model for comparison
    CanonicalGFF geneModel = null;
    if (modelFilename != null) {
      geneModel = new CanonicalGFF(modelFilename);
    }

    //step1
    //construct a new CGFF using chrIntervalMap buit from getIntervalMap function
    CanonicalGFF tracecgff = new CanonicalGFF(traceChrIntervalMap);

    // make sure necessary directory exists
    File path = new File(outputPrefix);
    if(outputPrefix.endsWith(System.getProperty("file.separator"))==false) path = path.getParentFile();
    if(path!=null) path.mkdirs();

    //Step2
    //query cgff and model to pre-write corresponding exons into file
    for (Iterator regionIDIt = tracecgff.geneRegionMap.keySet().iterator();regionIDIt.hasNext();) {
      String regionID = (String) regionIDIt.next();
      FileWriter fw = new FileWriter(outputPrefix + "." + regionID);
      geneFileMap.put(regionID, new File(outputPrefix + "." + regionID));
      geneBufferMap.put(regionID, new StringBuffer());

      GenomeInterval qRegion = (GenomeInterval)tracecgff.geneRegionMap.get(regionID);
      Set geneRanges = cgff.getRelatedGenes(qRegion.getChr(),qRegion.getStart(),qRegion.getStop(),false,false,1);

      //Here, we want to print the information about selected gene first,
      //and then the rest of genes.
      for(Iterator geneIt=geneRanges.iterator();geneIt.hasNext();){
          Interval gene = (Interval)geneIt.next();
          if(regionID.equals(gene.getUserObject())==false) continue;
          Set exonRanges = (Set)cgff.geneExonRegionMap.get(gene.getUserObject());
          writeExons(fw,(String)gene.getUserObject(),exonRanges,cgff.getStrand(gene.getUserObject()));
      }
      for(Iterator geneIt=geneRanges.iterator();geneIt.hasNext();){
          Interval gene = (Interval)geneIt.next();
          if(regionID.equals(gene.getUserObject())==true) continue;
          Set exonRanges = (Set)cgff.geneExonRegionMap.get(gene.getUserObject());
          writeExons(fw,(String)gene.getUserObject(),exonRanges,cgff.getStrand(gene.getUserObject()));
      }
      //if geneModel exists
      if(geneModel!=null){
        // to list models in the order of name sorting
        Set modelgeneRanges = new TreeSet(new Comparator(){
          StringComparator sc = new StringComparator();

          public int compare(Object o1, Object o2) {
            String modelID1 = (String) ((GenomeInterval)o1).getUserObject();
            String modelID2 = (String) ((GenomeInterval)o2).getUserObject();
            return sc.compare(modelID1,modelID2);
          }

          public boolean equals(Object obj) {
            return false;
          }
        });
        modelgeneRanges.addAll(geneModel.getRelatedGenes(qRegion.getChr(),qRegion.getStart(),qRegion.getStop(),false,false,1));

        for(Iterator modelgeneIt=modelgeneRanges.iterator();modelgeneIt.hasNext();){
          Interval modelgene = (Interval)modelgeneIt.next();
          Set modelexonRanges = (TreeSet)geneModel.geneExonRegionMap.get(modelgene.getUserObject());
          writeExons(fw,(String)modelgene.getUserObject(),modelexonRanges,geneModel.getStrand(modelgene.getUserObject()));
        }
      }
      fw.close();
    }

    //step3
    //select read and write information into file
    int uniqReadCnt = 0;
    int multiReadCnt = 0;
    for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator();mriIterator.hasNext(); ){
      Map.Entry entry = (Entry) mriIterator.next();
      String mappingFilename = (String) entry.getKey();
      String mappingMethod = (String) entry.getValue();
      // iterate alignments in the file
      for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod); mappingResultIterator.hasNext(); ){
        ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
        // skip a read if the best identity less than identityCutoff(-ID) threshold
        if (mappingResultIterator.getBestIdentity() < identityCutoff) continue;
        // collect records with best identity
        ArrayList acceptedRecords = new ArrayList();
        for (int i = 0; i < mappingRecords.size(); i++) {
          AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
          if (record.identity >= mappingResultIterator.getBestIdentity()) {
            // join nearby blocks
            if (joinFactor > 0) record.nearbyJoin(joinFactor);
            acceptedRecords.add(record);
          }
        }
        //check if this read is multi-read without splicing
        if (acceptedRecords.size() > 1){
          boolean noSplice = true;
          //check if splice event exists
          for (int j = 0; j < acceptedRecords.size(); j++)
          {
            AlignmentRecord record = (AlignmentRecord)acceptedRecords.get(j);
            if(record.numBlocks>1) noSplice = false;
          }
          if(noSplice) countReadMulti(mappingResultIterator.getReadID(), acceptedRecords,tracecgff);
          multiReadCnt++;

        }else{
          //deal with unique and splice read
          countReadUnique(mappingResultIterator.getReadID(),(AlignmentRecord)acceptedRecords.get(0), tracecgff);
          uniqReadCnt++;
        }
      }
    }
    report();
    System.out.println("#uniq reads: " + uniqReadCnt);
    System.out.println("#multi reads: " + multiReadCnt);
    System.out.println("#mapped reads: " + (uniqReadCnt + multiReadCnt));
  }

}
