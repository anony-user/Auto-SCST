package reseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import misc.AlignmentBlock;
import misc.AlignmentRecord;
import misc.GenomeInterval;
import misc.MappingResultIterator;
import misc.Util;

public class RearrangeCounter {

    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;

    private static String outputPrefix = null;

    private static int joinFactor = 2;
    private static float mappingIdentityCutoff = 0.95F;
    private static int minimumBlock = 25;
    private static int blockErrThreshold = 10;
    private static int minimumRead = 3;

    static private void paraProc(String[] args) {
        int i;

        // get parameter strings
        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-M")) {
                mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
                i += 2;
            } else if (args[i].equals("-J")) {
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-mID")) {
                mappingIdentityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-O")) {
                outputPrefix = args[i + 1];
                i++;
            } else if (args[i].equals("-minB")) {
                minimumBlock = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-blockErr")) {
                blockErrThreshold = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-minRead")) {
                minimumRead = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        // check for necessary parameters
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",
                System.getProperty("java.class.path"), "misc");
        if (mappingFilenameMethodMap.size() <= 0) {
            System.err.println("mapping method/filename (-M) not assigned, available methods:");
            for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
                System.out.println(iterator.next());
            }
            System.exit(1);
        }
        for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext();) {
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: "
                        + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if (outputPrefix == null) {
            System.err.println("output prefix (-O) not assigned");
            System.exit(1);
        }
        if (minimumBlock < 1) {
            System.err.println("minimum block size (-minB) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: RerrangeCounter");
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): " + outputPrefix);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("mapping identity cutoff (-mID): " + mappingIdentityCutoff);
        System.out.println("minimum block (-minB): " + minimumBlock);
        System.out.println("block error threshold (-blockErr): " + blockErrThreshold);
        System.out.println("minimum reads (-minRead): " + minimumRead);
        System.out.println();
    }

    public static void main(String args[]) {
        paraProc(args);

        TreeMap rearrangeCountMap = new TreeMap(new Comparator(){
          public int compare(Object o1, Object o2) {
            ArrayList array1 = (ArrayList) o1;
            ArrayList array2 = (ArrayList) o2;

            if(((GenomeInterval)array1.get(0)).compareTo(array2.get(0))!=0){
              return ((GenomeInterval)array1.get(0)).compareTo(array2.get(0));
            }else if(((GenomeInterval)array1.get(1)).compareTo(array2.get(1))!=0){
              return ((GenomeInterval)array1.get(1)).compareTo(array2.get(1));
            }else{
              int gap1 = (Integer) array1.get(2);
              int gap2 = (Integer) array2.get(2);
              return gap1-gap2;
            }
          }

          public boolean equals(Object obj) {
            return false;
          }
        });

        for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext();) {
            Map.Entry entry = (Map.Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            // iterate alignments
            int processedLines = 0;
            int mappedReadCnt = 0;

            int readLength = 0;
            ArrayList mappingRecords = new ArrayList();

            for (MappingResultIterator mappingResultIterator =
                    Util.getMRIinstance(mappingFilename, mappingMethodMap,mappingMethod);
                    mappingResultIterator.hasNext();) {
                // get one read with mapping
                mappingRecords = (ArrayList) mappingResultIterator.next();
                readLength = mappingResultIterator.getReadLength();

                mappedReadCnt++;
                processedLines += mappingRecords.size();

                ArrayList acceptedRecords = new ArrayList();
                for (int i = 0; i < mappingRecords.size(); i++) {
                    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                    // filter mapping Identity
                    if (record.getMappingIdentity() < mappingIdentityCutoff)
                        continue;

                    // join nearby blocks
                    if (joinFactor > 0)
                        record.nearbyJoin(joinFactor);

                    // filter minimum block
                    boolean skip_record = false;
                    for (int j = 0; j < record.numBlocks; j++) {
                        if (record.qBlockSizes[j] < minimumBlock) {
                            skip_record = true;
                            break;
                        }
                    }
                    if (skip_record) continue;

                    acceptedRecords.add(record);
                }

                // get unique mapping blocks
                HashSet<AlignmentBlock> blockSet = new HashSet<AlignmentBlock>();
                for (int i = 0; i < acceptedRecords.size(); i++) {
                    AlignmentRecord alignmentRecord = (AlignmentRecord) acceptedRecords.get(i);
                    blockSet.addAll(alignmentRecord.getAlignmentBlockList());
                }

                // block hashSet to array
                ArrayList<AlignmentBlock> blockList = new ArrayList<AlignmentBlock>(blockSet);

                // do block pair
                for (int x = 0; x < blockList.size(); x++) {
                    for (int y = 0 ; y < blockList.size(); y++) {
                        if (x==y) continue;

                        AlignmentBlock blockX = (AlignmentBlock) blockList.get(x);
                        AlignmentBlock blockY = (AlignmentBlock) blockList.get(y);

                        // check if two block in error distance
                        if (Math.abs(  blockY.getCanonicalReadBlock(readLength).getStart() - blockX.getCanonicalReadBlock(readLength).getStop() -1 ) >  blockErrThreshold )
                            continue;

                        GenomeInterval comparandX = (blockX.getStrand().equals("+")) ?
                            new GenomeInterval(blockX.chrOriginal,blockX.refBlock.getStop(),blockX.refBlock.getStop(),blockX.getStrand()) :
                            new GenomeInterval(blockX.chrOriginal,blockX.refBlock.getStart(),blockX.refBlock.getStart(),blockX.getStrand()) ;
                        GenomeInterval comparandY = (blockY.getStrand().equals("+")) ?
                            new GenomeInterval(blockY.chrOriginal,blockY.refBlock.getStart(),blockY.refBlock.getStart(),blockY.getStrand()) :
                            new GenomeInterval(blockY.chrOriginal,blockY.refBlock.getStop(),blockY.refBlock.getStop(),blockY.getStrand()) ;

                        // switch for canonicalization
                        if ( (blockX.strand.equals("-") && blockY.strand.equals("-")) ||
                             (blockX.strand.equals(blockY.strand)==false && comparandX.compareTo(comparandY)>0)) {

                            if (comparandX.getUserObject().equals("+")) {
                                comparandX.setUserObject("-");
                            }else{
                                comparandX.setUserObject("+");
                            }

                            if (comparandY.getUserObject().equals("+")) {
                                comparandY.setUserObject("-");
                            }else{
                                comparandY.setUserObject("+");
                            }
                          GenomeInterval tmpGi = comparandX;
                          comparandX = comparandY;
                          comparandY = tmpGi;
                        }

                        updateRearrangeCountMap(comparandX,comparandY,
                                                blockY.getCanonicalReadBlock(readLength).getStart()-blockX.getCanonicalReadBlock(readLength).getStop()-1,
                                                rearrangeCountMap);
                    }
                }
            }

            System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename);
        }

        // report block pairs
        try {
            FileWriter fw = new FileWriter(new File(outputPrefix+".rerrangeCount"));
            fw.write("#"+"chr1\tpos1\tdir1"+"\t"+"chr2\tpos2\tdir2" +
            "\t"+ "blockGap" + "\t" + "#read" +"\n");

            for (Iterator <Map.Entry> reCntIR = rearrangeCountMap.entrySet().iterator(); reCntIR.hasNext();) {
                ArrayList entryKey = (ArrayList) reCntIR.next().getKey();
                GenomeInterval gi1 = (GenomeInterval) entryKey.get(0);
                GenomeInterval gi2 = (GenomeInterval) entryKey.get(1);
                int gap = (Integer) entryKey.get(2);
                int readCnt = (Integer)rearrangeCountMap.get(entryKey);

                if(readCnt < minimumRead) continue;

                String reportStr = gi1.getChr()+"\t"+gi1.getStart()+"\t"+gi1.getUserObject();
                reportStr += "\t" + gi2.getChr()+"\t"+gi2.getStart()+"\t"+gi2.getUserObject();
                reportStr += "\t" + gap + "\t" + readCnt;
                fw.write(reportStr+"\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void updateRearrangeCountMap(GenomeInterval gi1, GenomeInterval gi2, int gapInRead, Map countMap) {
      ArrayList key = new ArrayList();
      key.add(gi1);
      key.add(gi2);
      key.add(gapInRead);

      if (countMap.containsKey(key)) {
        int val = (Integer) countMap.get(key);
        countMap.put(key, val+1);
      }
      else {
        countMap.put(key, 1);
      }
    }
}
