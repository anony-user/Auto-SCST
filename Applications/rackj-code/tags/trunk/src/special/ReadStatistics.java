package special;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.MappingResultIterator;
import misc.Util;

public class ReadStatistics {

    private static String gffFilename = null;
    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;

    private static int joinFactor = 2;
    private static float identityCutoff = 0.95F;
    private static int minimumBlock = 8;

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
            else if(args[i].equals("-min")){
                minimumBlock = Integer.parseInt(args[i + 1]);
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
        if(minimumBlock<1){
            System.err.println("minimum block size (-min) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: ReadStatistics");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("mapping method/filename (-M):");
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+ identityCutoff);
        System.out.println("minimum block size (-min): "+ minimumBlock);
        System.out.println();
    }

    public static void main(String args[]){
        paraProc(args);

        // get exonic CGFF and then intronic CGFF
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF intronCGFF = Util.getIntronicCGFF(cgff);

        // for each uniquely mapped reads
        int numExonic = 0;
        int numIntronic = 0;
        int numBridge = 0;
        int numJunction = 0;
        int numIntergenic = 0;
        int numUniqueRead = 0;
        int numMultiGene = 0;
        for(Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator();mriIterator.hasNext();){
            Map.Entry entry = (Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            // iterate alignments in the file
            int mappedReadCnt = 0;
            int processedLines = 0;
            for(MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod);
                    mappingResultIterator.hasNext();){
                mappedReadCnt++;
                ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
                processedLines += mappingRecords.size();
                // skip a read if best identity less than identityCutoff
                if(mappingResultIterator.getBestIdentity()<identityCutoff) continue;
                // collect records with best identity
                ArrayList acceptedRecords = new ArrayList();
                for(int i=0;i<mappingRecords.size();i++){
                    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                    if(record.identity>=mappingResultIterator.getBestIdentity()){
                        // join nearby blocks
                        if(joinFactor>0) record.nearbyJoin(joinFactor);
                        acceptedRecords.add(record);
                    }
                }
                // skip reads with more than one accepted records
                if(acceptedRecords.size()>1) continue;

                AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(0);
                numUniqueRead++;

                // a read will be processed if it is contained by some gene
                // skip this read if it hits two or more genes
                Set hitGenesEntire = cgff.getRelatedGenes(record.chr,record.getMappingIntervals(),false,true,minimumBlock,true);
                if (hitGenesEntire.size()>1){
                	numMultiGene++;
                	continue;
                }

                if(hitGenesEntire.size()==0){ // hits NO genes
                  numIntergenic++;
                }else if(record.numBlocks>1){ // splice reads
                    numJunction++;
                }else{ // non-splice reads
                    Set hitGenesExon = cgff.getRelatedGenes(record.chr,
                                                            record.getMappingIntervals(), true, true,
                                                            minimumBlock, true);
                    Set hitGenesIntron = intronCGFF.getRelatedGenes(record.chr,
                                                            record.getMappingIntervals(), true, true,
                                                            minimumBlock, true);
                    if(hitGenesExon.size()==1){ // contained by some exon
                        numExonic++;
                    }else if(hitGenesIntron.size()==1){
                        numIntronic++;
                    }else{
                        numBridge++;
                    }
                }
            }
            System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename);
        }

        System.out.println("#unique reads: " + numUniqueRead);
        System.out.println("#unique-gene-mapping reads: " + (numJunction+numExonic+numIntronic+numBridge));
        System.out.println("#exonic reads: " + numExonic);
        System.out.println("#intronic reads: " + numIntronic);
        System.out.println("#exon-intron bridge reads: " + numBridge);
        System.out.println("#exon-exon junction reads: " + numJunction);
        System.out.println("#intergenic reads: " + numIntergenic);
        System.out.println("#multiple-gene-mapping reads: " + numMultiGene);
    }
}
