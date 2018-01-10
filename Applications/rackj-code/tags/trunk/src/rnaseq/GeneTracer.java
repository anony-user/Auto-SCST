package rnaseq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.StringComparator;
import misc.Util;

public class GeneTracer implements ReadCounter {

    private Set traceList;
    private Map geneFileMap = new HashMap();
    private Map geneBufferMap = new HashMap();
    private int traceID = 1;
    private int bufferLimit = 10240;

    public GeneTracer(Set traceList,String prefix,CanonicalGFF cgff,CanonicalGFF model,int bufferLimit){
      this(traceList,prefix,cgff,model);
      this.bufferLimit = bufferLimit;
    }

    public GeneTracer(Set traceList,String prefix,CanonicalGFF cgff,CanonicalGFF model){
        this.traceList = traceList;

        // prepare gene-FileWriter map
        try {
            // make sure necessary directory exists
            File path = new File(prefix);
            if(prefix.endsWith(System.getProperty("file.separator"))==false) path = path.getParentFile();
            if(path!=null) path.mkdirs();
            // create one file for each gene, and write exon informations
            for(Iterator iterator=traceList.iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                if(cgff.geneExonRegionMap.containsKey(geneID)==false){
                    System.err.println(geneID +" doesn't exist in "+ gffFilename);
                    continue;
                }
                FileWriter fw = new FileWriter(prefix + "." + geneID);
                geneFileMap.put(geneID,new File(prefix + "." + geneID));
                geneBufferMap.put(geneID,new StringBuffer());
                // write exon information for this gene
                Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
                writeExons(fw,geneID,exonRegions,cgff.getStrand(geneID));
                // write exon information for other genes overlapping this gene
                String chr = (String) ((GenomeInterval)cgff.geneRegionMap.get(geneID)).getChr();
                Set geneSet = cgff.getRelatedGenes(chr,exonRegions,false,false,1,false);
                for(Iterator modelIterator = geneSet.iterator();modelIterator.hasNext();){
                    GenomeInterval otherGene = (GenomeInterval) modelIterator.next();
                    String otherGeneID = (String) otherGene.getUserObject();
                    if(otherGeneID.equals(geneID)) continue;
                    writeExons(fw,otherGeneID,(Set)cgff.geneExonRegionMap.get(otherGeneID),cgff.getStrand(otherGeneID));
                }
                // write model information from model (if given)
                if(model!=null){
                    // to list models in the order of name sorting
                    Set modelSet = new TreeSet(new Comparator(){
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
                    modelSet.addAll(model.getRelatedGenes(chr,exonRegions,false,false,1,false));

                    for(Iterator modelIterator = modelSet.iterator();modelIterator.hasNext();){
                        GenomeInterval modelGene = (GenomeInterval) modelIterator.next();
                        String modelGeneID = (String) modelGene.getUserObject();
                        writeExons(fw,modelGeneID,(Set)model.geneExonRegionMap.get(modelGeneID),model.getStrand(modelGeneID));
                    }
                }
                fw.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void writeExons(FileWriter fw,String title,Set intervals,String strand) throws
            IOException {
        for(Iterator exonIterator = intervals.iterator();exonIterator.hasNext();){
            Interval exon = (Interval) exonIterator.next();
            fw.write(title + "\t" +
                     strand + "\t" +
                     exon.getStart() + "\t" +
                     exon.getStop() + "\n");
        }
    }

    public void countReadUnique(String readID,AlignmentRecord record, Number cnt,
                                String geneID, CanonicalGFF cgff) {
        if(traceList.contains(geneID)==false) return;

        StringBuffer buffer = (StringBuffer) geneBufferMap.get(geneID);
        String appendString = "";
        if(record.numBlocks>1){
            for (int i = 0; i < record.numBlocks; i++) {
                appendString += (Util.spliceString + "\t" +
                         record.getStrand() + "\t" +
                         record.tStarts[i] + "\t" +
                         (record.tStarts[i] + record.tBlockSizes[i] - 1) + "\t" +
                         traceID + "\t" +
                         i);
                if(i==0){
                    appendString += ("\t" + readID + "\n");
                }else{
                    appendString += ("\n");
                }
            }
        }else{
            appendString += (Util.uniqString + "\t" +
                     record.getStrand() + "\t" +
                     record.tStarts[0] + "\t" +
                     (record.tStarts[0] + record.tBlockSizes[0] - 1) + "\t" +
                     traceID + "\t" +
                     "0" + "\t" +
                     readID + "\n");
        }
        traceID++;

        buffer.append(appendString);

        // do writing and buffer-cleaning if exceed bufferLimit
        try {
            if(buffer.length() > bufferLimit){
                File file = (File) geneFileMap.get(geneID);
                FileWriter fw = new FileWriter(file,true);
                fw.write(buffer.toString());
                fw.close();
                buffer.setLength(0);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        geneBufferMap.put(geneID,buffer);
    }

    public void countReadMulti(String readID,Collection recordCollection, Number cnt,
                               String geneID, CanonicalGFF cgff) {
        if(traceList.contains(geneID)==false) return;

        StringBuffer buffer = (StringBuffer) geneBufferMap.get(geneID);
        String appendString = "";
        for(Iterator iterator = recordCollection.iterator();iterator.hasNext();){
            AlignmentRecord record = (AlignmentRecord) iterator.next();
            appendString += (Util.multiString + "\t" +
                     record.getStrand() + "\t" +
                     record.tStarts[0] + "\t" +
                     (record.tStarts[0] + record.tBlockSizes[0] - 1) + "\t" +
                     traceID + "\t" +
                     "0" + "\t" +
                     readID + "\n");
            traceID++;
        }
        buffer.append(appendString);

        // do writing and buffer-cleaning if exceed bufferLimit
        try {
            if(buffer.length() > bufferLimit){
                File file = (File) geneFileMap.get(geneID);
                FileWriter fw = new FileWriter(file,true);
                fw.write(buffer.toString());
                fw.close();
                buffer.setLength(0);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        geneBufferMap.put(geneID,buffer);
    }

    public void report(String filename, CanonicalGFF cgff) {
        // write rest buffers
        try {
            for(Iterator iterator = geneFileMap.keySet().iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                File file = (File) geneFileMap.get(geneID);
                StringBuffer buffer = (StringBuffer) geneBufferMap.get(geneID);
                if(buffer.length()>0){
                    FileWriter fw = new FileWriter(file,true);
                    fw.write(buffer.toString());
                    fw.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

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

    private static String modelFilename = null;

    private static Set traceGeneList = new LinkedHashSet();
    private static String traceFilename = null;

    private static int bufferLimitIn = 10240;

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
            else if(args[i].equals("-model")){
                modelFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-trace")){
                traceGeneList.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-traceFile")){
                traceFilename = args[i + 1];
                i++;
            }
            else if (args[i].equals("-buffer")) {
              bufferLimitIn = Integer.parseInt(args[i + 1]);
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
            System.err.println("minimum overlap (-min) less than 1");
            System.exit(1);
        }
        if(traceGeneList.size()==0 && traceFilename==null){
            System.err.println("tracing gene list (-trace) empty AND traceing gene list in file (-traceFile) not assigned");
            System.exit(1);
        }

        // list parameters
        System.out.println("program: GeneTracer");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("mapping method/filename (-M):");
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): "+outputPrefix);
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+identityCutoff);
        System.out.println("use exon region (-exon): "+useExonRegion);
        System.out.println("check by containing (-contain, FALSE for by intersecting): "+checkByContaining);
        System.out.println("minimum overlap (-min): "+ minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): "+checkAllBlocks);
        System.out.println("model filename (-model): "+ modelFilename);
        System.out.println("tracing gene list (-trace): " + traceGeneList);
        System.out.println("traceing gene list in file (-traceFile): " + traceFilename);
        System.out.println("buffer size per gene (-buffer): " + bufferLimitIn);
        System.out.println();

        // post-processing
        if(traceFilename!=null){
            try {
                BufferedReader fr = new BufferedReader(new FileReader(traceFilename));

                while(fr.ready()){
                    String line = fr.readLine();
                    StringTokenizer st = new StringTokenizer(line);
                    if(st.hasMoreTokens()){
                        traceGeneList.add(st.nextToken());
                    }
                }

                fr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }

    public static void main(String args[]){
        paraProc(args);

        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        Map geneUniqReadCntMap = new HashMap();
        Map geneMultiReadCntMap = new HashMap();

        // gene model for comparison
        CanonicalGFF geneModel = null;
        if(modelFilename!=null){
            geneModel = new CanonicalGFF(modelFilename);
        }
        // read counters
        final GeneTracer gt = new GeneTracer(traceGeneList,outputPrefix,cgff,geneModel,bufferLimitIn);

        ReadCounter readCounter = new ReadCounter(){
            public void countReadUnique(String readID,AlignmentRecord record, Number cnt,
                                        String geneID, CanonicalGFF cgff) {
                gt.countReadUnique(readID,record,cnt,geneID,cgff);
            }

            public void countReadMulti(String readID,Collection recordCollection, Number cnt,
                                       String geneID, CanonicalGFF cgff) {
                gt.countReadMulti(readID,recordCollection,cnt,geneID,cgff);
            }

            public void report(String filename, CanonicalGFF cgff) {
                gt.report(filename,cgff);
            }
        };

        // computing based on unique reads
        int uniqReadCnt = 0;
        //Set nonUniqReads = new HashSet();
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
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            MultiReadIterator multiRI = new MultiReadIterator(Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
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
}
