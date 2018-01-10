package reseq;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import misc.FineAlignmentRecord;
import misc.GenomeInterval;
import misc.MappingResultIterator;
import misc.Pair;
import misc.Util;

/**
 * Output polymorphism frequence of given mapping result files
 *
 * @author Peter Tsai
 * @version 0.5
 */
public class PolymorphismCounter {

    // mapping result
    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;
    // output
    private static String outFilename = null;
    // alignment check
    private static int joinFactor = 0;
    private static float identityCutoff = 0.80F; // OLD default 0.90F
    private static float mappingIdentityCutoff = 0.85F;
    private static int minimumOverlap = 8;

    private static boolean includeSNP = true;
    private static boolean includeINDEL = true;

    private static Pair snpPair = new Pair('A','A');
    private static Pair indelPair = new Pair(1,2);

    // output control
    private static int minimumReads = 2;

    static private void paraProc(String[] args) {
        int i;

        // get parameter strings
        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-M")) {
                mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
                i += 2;
            } else if (args[i].equals("-O")) {
                outFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-J")) {
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-ID")) {
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-mID")) {
                mappingIdentityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-min")) {
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-minRead")) {
                minimumReads = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-SNP")) {
                includeSNP = Boolean.valueOf(args[i+1]);
                i++;
            } else if (args[i].equals("-INDEL")) {
                includeINDEL = Boolean.valueOf(args[i+1]);
                i++;
            }
        }

        // check for necessary parameters
        mappingMethodMap = Util.getMethodMap("misc.FineMappingResultIterator",System.getProperty("java.class.path"), "misc");
        if (mappingFilenameMethodMap.size() <= 0) {
            System.err.println("mapping method/filename (-M) not assigned, available methods:");
            for (Iterator iterator = mappingMethodMap.keySet().iterator();  iterator.hasNext(); ) {
                System.out.println(iterator.next());
            }
            System.exit(1);
        }
        for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext(); ) {
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if (outFilename == null) {
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        if( (includeSNP || includeINDEL)==false){
            System.err.println("include SNP (-SNP) OR include INDEL (-INDEL) should be true");
            System.exit(1);
        }

        // post-processing

        // list parameters
        System.out.println("program: PolymorephismCounter");
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output filename (-O): " + outFilename);
        System.out.println("identity cutoff (-ID): " + identityCutoff);
        System.out.println("mapping identity cutoff (-mID): " + mappingIdentityCutoff);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("minimum block size (-min): " + minimumOverlap);
        System.out.println("include SNP (-SNP): " + includeSNP);
        System.out.println("include INDEL (-INDEL): " + includeINDEL);
        System.out.println("minimum number of reads (-minRead): " + minimumReads);
        System.out.println();
    }

    public static void main(String[] args) {
        paraProc(args);

        // step 1: initialize SNP hash map
        Map<GenomeInterval, Integer> polymorphismMap = new TreeMap<GenomeInterval, Integer>();

        // each Iterator loop for each input BLAT/SAM file
        for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext(); ) {
            Map.Entry entry = (Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();

            // iterate alignments in the file
            int mappedReadCnt = 0;
            int processedLines = 0;

            for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod);
                    mappingResultIterator.hasNext(); ) {

                ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
                processedLines += mappingRecords.size();

                // apply join operation and select best alignments
                ArrayList acceptedRecords = new ArrayList();
                for (int i = 0; i < mappingRecords.size(); i++) {
                    FineAlignmentRecord record = (FineAlignmentRecord) mappingRecords.get(i);
                    if (record.identity >= mappingResultIterator.getBestIdentity()) {
                        if (joinFactor > 0)
                            record.nearbyJoin(joinFactor);
                        acceptedRecords.add(record);
                    }
                }
                mappingRecords = acceptedRecords;

                // other filtering
                acceptedRecords = new ArrayList();
                for(int i=0; i < mappingRecords.size(); i++){
                    FineAlignmentRecord record = (FineAlignmentRecord) mappingRecords.get(i);
                    if(record.identity < identityCutoff) // filtered by identity
                        continue;
                    if(record.getMappingIdentity() < mappingIdentityCutoff) // filtered by mapping identity
                    	continue;
                    acceptedRecords.add(record);
                }
                
                // skip if no qualified alignments
                if(acceptedRecords.size()==0) continue;
                // skip reads with more than one accepted records
                if (acceptedRecords.size() > 1) continue;

                // Retrieve AlignmentRecord
                FineAlignmentRecord record = (FineAlignmentRecord) acceptedRecords.get(0);

                // skip reads with blocks shorter than minimumOverlap
                int minBlockSize = -1;
                for (int i = 0; i < record.numBlocks; i++) {
                    if (minBlockSize < 0 || minBlockSize > record.tBlockSizes[i]) {
                        minBlockSize = record.tBlockSizes[i];
                    }
                }
                if (minBlockSize < minimumOverlap) continue;

                mappedReadCnt++;

                if(includeINDEL){
                    Set INDELSet = record.getIndels();

                    for (Iterator INDELIterator = INDELSet.iterator(); INDELIterator.hasNext(); ) {
                        GenomeInterval INDEL = (GenomeInterval) INDELIterator.next();
                        if (polymorphismMap.containsKey(INDEL)) {
                            int read_count = polymorphismMap.get(INDEL);
                            read_count++;
                            polymorphismMap.put(INDEL, read_count);
                        } else {
                            polymorphismMap.put(INDEL, 1);
                        }
                    }
                }

                if(includeSNP){
                    record.reset();
                    Set snpSet = record.getSNPs();

                    for (Iterator snpIterator = snpSet.iterator(); snpIterator.hasNext(); ) {
                        GenomeInterval snp = (GenomeInterval) snpIterator.next();
                        if (polymorphismMap.containsKey(snp)) {
                            int read_count = polymorphismMap.get(snp);
                            read_count++;
                            polymorphismMap.put(snp, read_count);
                        } else {
                            polymorphismMap.put(snp, 1);
                        }
                    }
                }
            }
        }
        // step 2: write file
        try {
            FileWriter fw = new FileWriter(outFilename);
            for (Iterator SNPMapIterator = polymorphismMap.entrySet().iterator(); SNPMapIterator.hasNext(); ) {
                Map.Entry entry = (Entry) SNPMapIterator.next();
                GenomeInterval gi = (GenomeInterval) entry.getKey();
                int nReads = (Integer) entry.getValue();

                // skip if not enough reads
                if(nReads < minimumReads){
                  continue;
                }

                Pair userObj = (Pair) gi.getUserObject();
                String keyStr;
                if(userObj.key1.getClass().equals(snpPair.key1.getClass()) && userObj.key2.getClass().equals(snpPair.key2.getClass())){
                  keyStr = gi.getChr() + ":" + gi.getStart() + ":" + userObj.key1 + "->" + userObj.key2;
                }else{
                  keyStr = gi.toString();
                }
                fw.write(keyStr + "\t" + nReads + "\n");
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
