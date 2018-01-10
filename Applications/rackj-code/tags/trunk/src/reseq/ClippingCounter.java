package reseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import misc.AlignmentRecord;
import misc.GenomeInterval;
import misc.MappingResultIterator;
import misc.Pair;
import misc.Util;
import net.sf.samtools.SAMRecord;

public class ClippingCounter {

    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;

    private static String outputPrefix = null;

    private static int joinFactor = 2;
    private static float mappingIdentityCutoff = 0.95F;
    private static int minimumBlock = 25;
    private static int minimumClipping = 20;
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
            } else if (args[i].equals("-minClip")) {
                minimumClipping = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-minRead")) {
                minimumRead = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        // check for necessary parameters
        mappingMethodMap = Util.getMethodMap("misc.SamMappingResultIterator",
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
        System.out.println("program: ClippingCounter");
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): " + outputPrefix);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("mapping identity cutoff (-mID): " + mappingIdentityCutoff);
        System.out.println("minimum block (-minB): " + minimumBlock);
        System.out.println("minimum clipping size (-minClip): " + minimumClipping);
        System.out.println("minimum reads (-minRead): " + minimumRead);
        System.out.println();
    }

    public static void main(String args[]) {
        paraProc(args);

        TreeMap<GenomeInterval, Integer> clippingCountMap = new TreeMap<GenomeInterval, Integer>();

        for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext();) {
            Map.Entry entry = (Map.Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            // iterate alignments
            int processedLines = 0;
            int mappedReadCnt = 0;

            int readLength = 0;
            ArrayList mappingRecords = new ArrayList();

            for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename,
                    mappingMethodMap,mappingMethod); mappingResultIterator.hasNext();) {
                // get one read with mapping
                mappingRecords = (ArrayList) mappingResultIterator.next();
                readLength = mappingResultIterator.getReadLength();

                mappedReadCnt++;
                processedLines += mappingRecords.size();

                //  filtering & count
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

                    record.reset();

                    SAMRecord samREC = (SAMRecord) record.sourceObj;

                    if (samREC.getReadString().equals(SAMRecord.NULL_SEQUENCE_STRING) || samREC.getReadString().length()<readLength) continue;

                    // clipping count
                    // 5' clipping search
                    int headClip = record.qStarts[0] - 1;
                    if (headClip >= minimumClipping) { // filter minimum clipping
                        String hClipSeq = samREC.getReadString().substring(headClip-minimumClipping, headClip);
                        updateClippingCountMap(new GenomeInterval(record.chrOriginal,record.tStarts[0],record.tStarts[0],
                                new Pair("5'",hClipSeq)),clippingCountMap);
                    }


                    // 3' clipping search
                    int tailClip = readLength - (record.qStarts[record.numBlocks-1] + record.qBlockSizes[record.numBlocks-1]-1);
                    if (tailClip >= minimumClipping) { // filter minimum clipping
                        int clippingEnd = record.tStarts[record.numBlocks-1] + record.tBlockSizes[record.numBlocks-1]-1;
                        String tClipSeq = samREC.getReadString().substring(readLength-tailClip,readLength-tailClip+minimumClipping);
                        updateClippingCountMap(new GenomeInterval(record.chrOriginal,clippingEnd,clippingEnd,new Pair("3'",tClipSeq)),clippingCountMap);
                    }
                }
            }
            System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename);
        }

        // report clipping sites
        try {
            FileWriter fw = new FileWriter(new File(outputPrefix+".clippingCount"));
            fw.write("#chr\tPosition\tsite\tSEQ\tcount\n");
            for (Iterator<Entry<GenomeInterval, Integer>> clipCntIR = clippingCountMap.entrySet().iterator(); clipCntIR.hasNext();) {
                GenomeInterval gi = clipCntIR.next().getKey();
                // filter minimum Reads
                if (clippingCountMap.get(gi) < minimumRead ) continue;

                String reportStr = gi.getChr()+"\t"+gi.getStart()+"\t";
                Pair terminalSeqPair = (Pair) gi.getUserObject();
                reportStr += terminalSeqPair.key1+"\t"+terminalSeqPair.key2+"\t" + clippingCountMap.get(gi);
                fw.write(reportStr+"\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static void updateClippingCountMap(GenomeInterval keyGI, Map countMap) {
        if (countMap.containsKey(keyGI)) {
          int val = (Integer) countMap.get(keyGI);
          countMap.put(keyGI, val+1);
        }
        else {
          countMap.put(keyGI, 1);
        }
      }
 }
