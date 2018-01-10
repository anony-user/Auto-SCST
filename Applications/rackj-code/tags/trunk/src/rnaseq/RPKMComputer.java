package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.MappingResultIterator;
import misc.MultiLevelMap;
import misc.Util;

public class RPKMComputer {

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
    private static boolean careDirection = false;
    private static boolean geneOnly = false;

    private static String modelFilename = null;

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
            else if(args[i].equals("-geneOnly")){
            	geneOnly = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-model")){
                modelFilename = args[i + 1];
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
        System.out.println("program: RPKMComputer");
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
        System.out.println("RPKM by gene-mapping reads only (-geneOnly): "+geneOnly);
        System.out.println("model filename (-model): "+ modelFilename);
        System.out.println();
    }

    public static double computeRPKM(Map cntMap,int mappedCnt,String geneID,CanonicalGFF cgff,boolean useExonRegion){
        double readCnt;
        if(cntMap.containsKey(geneID)){
            readCnt = ((Number)cntMap.get(geneID)).doubleValue();
        }else{
            readCnt = 0.0;
        }
        double lengthKb;
        if(useExonRegion){
            lengthKb = ((Integer) cgff.geneLengthMap.get(geneID)).doubleValue() / 1000;
        }else{
            GenomeInterval geneRegion =  (GenomeInterval) cgff.geneRegionMap.get(geneID);
            lengthKb = ((double) geneRegion.length()) / 1000;
        }
        double RPKM = (readCnt*1000000)/mappedCnt/lengthKb;

        return RPKM;
    }

    public static void main(String args[]){
        paraProc(args);

        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        Map geneUniqReadCntMap = new HashMap();
        Map<Set, Integer> geneSetMultiReadCntMap = new HashMap();
     
        // gene model for comparison
        CanonicalGFF geneModel = null;
        if(modelFilename!=null){
            geneModel = new CanonicalGFF(modelFilename);
        }
        // read counters
        final RPKM rpkm = new RPKM(useExonRegion,geneOnly);
        final ExonCounter exonCounter = new ExonCounter(checkByContaining,minimumOverlap);
        final SpliceCounter spliceCounter = new SpliceCounter(geneModel,cgff,checkByContaining,minimumOverlap);

        ReadCounter2 readCounter = new ReadCounter2(){ // wrap ReadCounters
            public void processUnique(String readID,AlignmentRecord record,Number cnt,String geneID,CanonicalGFF cgff){
            	rpkm.processUnique(readID, record, cnt, geneID, cgff);
            	exonCounter.processUnique(readID,record, cnt, geneID, cgff);
                spliceCounter.processUnique(readID,record, cnt, geneID, cgff);
            }

            public void processMulti(String readID,Map hitGeneAlignmentSetMap,Set geneIdSet,CanonicalGFF cgff){
            	rpkm.processMulti(readID, hitGeneAlignmentSetMap, geneIdSet, cgff);
                exonCounter.processMulti(readID, hitGeneAlignmentSetMap, geneIdSet, cgff);
                spliceCounter.processMulti(readID, hitGeneAlignmentSetMap, geneIdSet, cgff);
            }
            
            public void finalize(Map<Set,Map<String,Double>> geneSetIdRatioMap){
            	rpkm.finalize(geneSetIdRatioMap);
            	exonCounter.finalize(geneSetIdRatioMap);
            	spliceCounter.finalize(geneSetIdRatioMap);
            }

            public void report(String filename, CanonicalGFF cgff) {
            	rpkm.report(filename+".geneRPKM", cgff);
                exonCounter.report(filename + ".exonCount",cgff);
                spliceCounter.report(filename + ".spliceCount",cgff);
            }
        };

        // computing based on unique reads
        int uniqReadCnt = 0;
        int multiReadCnt = 0;
        int restReadCnt = 0;
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator();iterator.hasNext();){
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            UniqueMultiReadIterator uniqueMultiRI = new UniqueMultiReadIterator(Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
                    identityCutoff,
                    joinFactor,
                    useExonRegion,
                    checkByContaining,
                    minimumOverlap,
                    checkAllBlocks,
                    careDirection,
                    cgff,
                    geneUniqReadCntMap,
                    geneSetMultiReadCntMap
                    );
            uniqueMultiRI.iterate(readCounter);
            uniqReadCnt += uniqueMultiRI.uniqReadCnt;
            multiReadCnt += uniqueMultiRI.multiReadCnt;
            if(!geneOnly){ // not to count rest reads if NOT gene-only for RPKM
            	restReadCnt += uniqueMultiRI.restReadCnt;
            }
        }

        processFinal(cgff, uniqReadCnt, readCounter, geneUniqReadCntMap, geneSetMultiReadCntMap);
        
        System.out.println("#uniq reads: " + uniqReadCnt);
        System.out.println("#multi reads: " + multiReadCnt);
        System.out.println("#mapped reads: " + (uniqReadCnt + multiReadCnt + restReadCnt));

        readCounter.report(outputPrefix,cgff);
    }
    
    private static void processFinal(CanonicalGFF cgff,
    						 int uniqReadCnt,
    						 ReadCounter2 rc,
    						 Map geneUniqReadCntMap,
    						 Map geneSetMultiReadCntMap
    						 ){
    	Map<Set,Map<String,Double>> geneSetIdRatioMap = new HashMap<Set,Map<String,Double>>();
    	
    	// get their total RPKM based on unique reads
    	for(Iterator geneSetIterator = geneSetMultiReadCntMap.keySet().iterator();geneSetIterator.hasNext();){
    		double totalUniqRPKM = 0;
    		Set hitGeneIdSet = (Set)geneSetIterator.next();

    		for(Iterator geneIterator = hitGeneIdSet.iterator();geneIterator.hasNext();){
    			String geneID = (String) geneIterator.next();
    			totalUniqRPKM += RPKMComputer.computeRPKM(geneUniqReadCntMap,uniqReadCnt,geneID,cgff,useExonRegion);
    		}

    		// compute their multi read counts
    		if(totalUniqRPKM > (double) 0.0){
    			for (Iterator geneIterator = hitGeneIdSet.iterator();geneIterator.hasNext(); ) {
    				// RPKM counting
    				String geneID = (String) geneIterator.next();
    				double uniqRPKM = RPKMComputer.computeRPKM(geneUniqReadCntMap, uniqReadCnt, geneID, cgff,useExonRegion);
    				// record ratio
    				MultiLevelMap.multiLevelPut(geneSetIdRatioMap, uniqRPKM/totalUniqRPKM, hitGeneIdSet,geneID);
    			}
    		}else{
    			for (Iterator geneIterator = hitGeneIdSet.iterator();geneIterator.hasNext(); ) {
    				// PRKM counting
    				String geneID = (String) geneIterator.next();
    				// record ratio
    				MultiLevelMap.multiLevelPut(geneSetIdRatioMap, (double)1.0/hitGeneIdSet.size(), hitGeneIdSet,geneID);
    			}
    		}
    	}
    	
    	rc.finalize(geneSetIdRatioMap);
    }
}

class RPKM implements ReadCounter2{

	private MultiLevelMap<Integer> geneUniqReadCntMLM = new MultiLevelMap<Integer>();
	private MultiLevelMap<Double> geneMultiReadCntMLM = new MultiLevelMap<Double>();
	private MultiLevelMap<Integer> geneSetMultiReadCntMLM = new MultiLevelMap<Integer>();
	private int mappedReadCnt = 0;
	
	// control options
	private boolean useExonRegion;
	private boolean geneOnly;
		
	public RPKM(boolean useExonRegion, boolean geneOnly){
		this.useExonRegion = useExonRegion;
		this.geneOnly = geneOnly;
	}

	public void processUnique(String readID,AlignmentRecord record,Number cnt,String geneID,CanonicalGFF cgff){
		geneUniqReadCntMLM.multiLevelAdd(Integer.valueOf(1), geneID);
		mappedReadCnt++;
	}

    public void processMulti(String readID,Map hitGeneAlignmentSetMap,Set geneIdSet,CanonicalGFF cgff){
    	geneSetMultiReadCntMLM.multiLevelAdd(Integer.valueOf(1), geneIdSet);
    	if(geneOnly && geneIdSet.size()==0){
    		// not to count when geneOnly AND no hit genes
    	}else{
        	mappedReadCnt++;
    	}
    }
    
    public void finalize(Map<Set,Map<String,Double>> geneSetIdRatioMap){
    	Map geneSetMultiReadCntMap = geneSetMultiReadCntMLM.getTopLevelMap();
    	
    	for(Set geneIdSet : geneSetIdRatioMap.keySet()){
    		double readCnt = ((Integer) geneSetMultiReadCntMap.get(geneIdSet)).doubleValue();

    		Map<String,Double> geneRatioMap = geneSetIdRatioMap.get(geneIdSet);
    		for( String geneID : geneRatioMap.keySet() ){
    			geneMultiReadCntMLM.multiLevelAdd(Double.valueOf(readCnt*geneRatioMap.get(geneID)), geneID);
    		}
    	}
    }

    public void report(String filename,CanonicalGFF cgff){
    	
    	Map geneUniqReadCntMap = geneUniqReadCntMLM.getTopLevelMap();
    	Map geneMultiReadCntMap = geneMultiReadCntMLM.getTopLevelMap();
    	
        try {
            FileWriter fw = new FileWriter(new File(filename));
            // header
            fw.write("#GeneID" + "\t" +
                     "Length(Kbps)" + "\t" +
                     "#Reads" + "\t" +
                     "RPKM" + "\t" +
                     "multi/ALL"  + "\t" +
                     "format:.geneRPKM" + "\n"
                    );
            // contents
            for(Iterator iterator = cgff.geneLengthMap.keySet().iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                // GeneID
                fw.write(geneID + "\t");
                // Length(Kbps)
                float lengthKb = 0F;
                if(useExonRegion){
                    lengthKb = ((Integer) cgff.geneLengthMap.get(geneID)).floatValue() / 1000;
                }else{
                    GenomeInterval geneRegion =  (GenomeInterval) cgff.geneRegionMap.get(geneID);
                    lengthKb = ((float) geneRegion.length()) / 1000;
                }
                fw.write(lengthKb + "\t");
                // #Reads
                int uniqCnt;
                if(geneUniqReadCntMap.containsKey(geneID)){
                    uniqCnt = ((Integer)geneUniqReadCntMap.get(geneID)).intValue();
                }else{
                    uniqCnt = 0;
                }
                double multiCnt;
                if(geneMultiReadCntMap.containsKey(geneID)){
                    multiCnt = ((Double)geneMultiReadCntMap.get(geneID)).doubleValue();
                }else{
                    multiCnt = 0;
                }
                fw.write((uniqCnt + multiCnt) + "\t");
                // RPKM
                double RPKM = RPKMComputer.computeRPKM(geneUniqReadCntMap,mappedReadCnt,geneID,cgff,useExonRegion)+
                			RPKMComputer.computeRPKM(geneMultiReadCntMap,mappedReadCnt,geneID,cgff,useExonRegion);
                fw.write(RPKM + "\t");
                // multi/ALL
                if(RPKM>0){
                    fw.write(new Float(multiCnt / (uniqCnt + multiCnt)).toString());
                }else{
                    fw.write(new Float(0).toString());
                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

class UniqueMultiReadIterator {
    private MappingResultIterator mri;
    private float identityCutoff;
    private int joinFactor;
    private boolean useExonRegion = true;
    private boolean checkByContaining = true;
    private int minimumOverlap = 6;
    private boolean checkAllBlocks = false;
    private boolean careDirection = false;
    private CanonicalGFF cgff;
    private Map geneUniqReadCntMap;
    private Map<Set, Integer> geneSetMultiReadCntMap;

    public int uniqReadCnt = 0;
    public int restReadCnt = 0;
    public int multiReadCnt = 0;
    
	public UniqueMultiReadIterator(MappingResultIterator mri,
            float identityCutoff,
            int joinFactor,
            boolean useExonRegion,
            boolean checkByContaining,
            int minimumOverlap,
            boolean checkAllBlocks,
            CanonicalGFF cgff,
            Map geneUniqReadCntMap,
            Map geneSetMultiReadCntMap
            ){
        this(mri,
        	 identityCutoff,
        	 joinFactor,
        	 useExonRegion,
        	 checkByContaining,
        	 minimumOverlap,
        	 checkAllBlocks,
        	 false,
        	 cgff,
        	 geneUniqReadCntMap,
        	 geneSetMultiReadCntMap);
    }
    
    public UniqueMultiReadIterator(MappingResultIterator mri,
    		float identityCutoff,
		    int joinFactor,
		    boolean useExonRegion,
		    boolean checkByContaining,
		    int minimumOverlap,
		    boolean checkAllBlocks,
		    boolean careDirection,
		    CanonicalGFF cgff,
		    Map geneUniqReadCntMap,
            Map geneSetMultiReadCntMap
	){
    	this.mri = mri;
		this.identityCutoff = identityCutoff;
		this.joinFactor = joinFactor;
		this.useExonRegion = useExonRegion;
		this.checkByContaining = checkByContaining;
		this.minimumOverlap = minimumOverlap;
		this.checkAllBlocks = checkAllBlocks;
		this.careDirection = careDirection;
		this.cgff = cgff;
		this.geneUniqReadCntMap = geneUniqReadCntMap;
		this.geneSetMultiReadCntMap = geneSetMultiReadCntMap;
	}
    
    public void iterate(ReadCounter2 rc){

    	for(MappingResultIterator iterator = mri;iterator.hasNext();){
    		ArrayList mappingRecords = (ArrayList) iterator.next();
    		// skip a read if best identity less than identityCutoff
    		if(iterator.getBestIdentity()<identityCutoff) continue;
    		// collect records with best identity
    		ArrayList acceptedRecords = new ArrayList();
    		for(int i=0;i<mappingRecords.size();i++){
    			AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
    			if(record.identity>=iterator.getBestIdentity()){
    				// join nearby blocks
    				if(joinFactor>0) record.nearbyJoin(joinFactor);
    				acceptedRecords.add(record);
    			}
    		}
    		// get read counts based only on unique reads
    		// a read will be classified as "unique" if
    		//     1. one best alignment
    		//     2. this best alignment lies in exactly one gene region
            AlignmentRecord theRecord = (AlignmentRecord) acceptedRecords.get(0);
            Set hitGeneRegionSet = cgff.getRelatedGenes(theRecord,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,careDirection);	

            if(acceptedRecords.size()==1 && hitGeneRegionSet.size()==1){
    			GenomeInterval geneRegion = (GenomeInterval) hitGeneRegionSet.iterator().next();
    			String geneID = (String) geneRegion.getUserObject();
    			MultiLevelMap.multiLevelAdd(geneUniqReadCntMap, Integer.valueOf(1), geneID);
    			// call read counter
    			if(rc!=null) rc.processUnique(iterator.getReadID(),theRecord,new Integer(1),geneID,cgff);
    			// mapped count
    			uniqReadCnt++;
    		}else if(acceptedRecords.size()>=1){
    			//multi Read
    			// any other reads with at least one accepted alignment are collected
                boolean noSplice = true;
                for(int i=0;i<acceptedRecords.size();i++){
                    if(((AlignmentRecord)acceptedRecords.get(i)).numBlocks>1){
                        noSplice = false;
                        break;
                    }
                }

                // get related gene set A
                Set hitGeneIdSet = new HashSet();
                Map hitGeneAlignmentSetMap = new HashMap();

                if(noSplice){
	                for(int i=0;i<acceptedRecords.size();i++){
	                    AlignmentRecord alignmentRecord = (AlignmentRecord) acceptedRecords.get(i);
	                    Set hitGenes = cgff.getRelatedGenes(alignmentRecord,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,careDirection);
	
	                    for(Iterator hitGeneIterator = hitGenes.iterator();hitGeneIterator.hasNext();){
	                        String geneID = (String) ((GenomeInterval) hitGeneIterator.next()).getUserObject();
	                        hitGeneIdSet.add(geneID);
	                        if(hitGeneAlignmentSetMap.containsKey(geneID)){
	                            Set set = (Set) hitGeneAlignmentSetMap.get(geneID);
	                            set.add(alignmentRecord);
	                        }else{
	                            Set set = new HashSet();
	                            set.add(alignmentRecord);
	                            hitGeneAlignmentSetMap.put(geneID,set);
	                        }
	                    }
	                }
                }

                // geneSetMultiReadCntMap
                MultiLevelMap.multiLevelAdd(geneSetMultiReadCntMap, Integer.valueOf(1), hitGeneIdSet);
                // read counter
    			if(rc!=null) rc.processMulti(iterator.getReadID(), hitGeneAlignmentSetMap, hitGeneIdSet, cgff);
                // count according to number of mapped gene regions
                if(hitGeneIdSet.size()==0 || noSplice==false){
                    restReadCnt++;
                }else{
                    multiReadCnt++;
                }
    		}
    	}
    }
}

class UniqueReadIterator {

    private MappingResultIterator mri;
    private float identityCutoff;
    private int joinFactor;
    private boolean useExonRegion = true;
    private boolean checkByContaining = true;
    private int minimumOverlap = 6;
    private boolean checkAllBlocks = false;
    private boolean careDirection = false;
    private CanonicalGFF cgff;
    private Map geneUniqReadCntMap;

    //public Set restReads = new HashSet();
    public int uniqReadCnt = 0;

    // old interface
    public UniqueReadIterator(MappingResultIterator mri,
                              float identityCutoff,
                              int joinFactor,
                              boolean useExonRegion,
                              boolean checkByContaining,
                              int minimumOverlap,
                              boolean checkAllBlocks,
                              CanonicalGFF cgff,
                              Map geneUniqReadCntMap
            ){
        this(mri,identityCutoff,joinFactor,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,false,cgff,geneUniqReadCntMap);
    }

    public UniqueReadIterator(MappingResultIterator mri,
                              float identityCutoff,
                              int joinFactor,
                              boolean useExonRegion,
                              boolean checkByContaining,
                              int minimumOverlap,
                              boolean checkAllBlocks,
                              boolean careDirection,
                              CanonicalGFF cgff,
                              Map geneUniqReadCntMap
            ){
        this.mri = mri;
        this.identityCutoff = identityCutoff;
        this.joinFactor = joinFactor;
        this.useExonRegion = useExonRegion;
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;
        this.checkAllBlocks = checkAllBlocks;
        this.careDirection = careDirection;
        this.cgff = cgff;
        this.geneUniqReadCntMap = geneUniqReadCntMap;
    }

    public void iterate(ReadCounter rc){
        for(MappingResultIterator iterator = mri;iterator.hasNext();){
            ArrayList mappingRecords = (ArrayList) iterator.next();
            // skip a read if best identity less than identityCutoff
            if(iterator.getBestIdentity()<identityCutoff) continue;
            // collect records with best identity
            ArrayList acceptedRecords = new ArrayList();
            for(int i=0;i<mappingRecords.size();i++){
                AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                if(record.identity>=iterator.getBestIdentity()){
                    // join nearby blocks
                    if(joinFactor>0) record.nearbyJoin(joinFactor);
                    acceptedRecords.add(record);
                }
            }
            // get read counts based only on unique reads
            // a read will be classified as "unique" if
            //     1. one best alignment
            //     2. this best alignment lies in exactly one gene region
            AlignmentRecord theRecord = (AlignmentRecord) acceptedRecords.get(0);
            Set hitGeneRegions = cgff.getRelatedGenes(theRecord,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,careDirection);
            if(acceptedRecords.size()==1 && hitGeneRegions.size()==1){
                GenomeInterval geneRegion = (GenomeInterval) hitGeneRegions.iterator().next();
                String geneID = (String) geneRegion.getUserObject();
                if (geneUniqReadCntMap.containsKey(geneID)) {
                    int oldValue = ((Integer) geneUniqReadCntMap.get(geneID)).intValue();
                    geneUniqReadCntMap.put(geneID, new Integer(oldValue + 1));
                } else {
                    geneUniqReadCntMap.put(geneID, new Integer(1));
                }
                // call some function here
                if(rc!=null) rc.countReadUnique(iterator.getReadID(),theRecord,new Integer(1),geneID,cgff);
                // mapped count
                uniqReadCnt++;
            }else if(acceptedRecords.size()>=1){
                // any other reads with at least one accepted alignment are collected
                //restReads.add(iterator.getReadID());
            }
        }
    }
}

class MultiReadIterator {

    private MappingResultIterator mri;
    private float identityCutoff;
    private int joinFactor;
    private boolean useExonRegion = true;
    private boolean checkByContaining = true;
    private int minimumOverlap = 6;
    private boolean checkAllBlocks = false;
    private boolean careDirection = false;
    private CanonicalGFF cgff;
    private Map geneUniqReadCntMap;
    private Map geneMultiReadCntMap;
    //private Set multiReads;
    private int uniqReadCnt;

    public int restReadCnt = 0;
    public int mlutiReadCnt = 0;

    // old interface
    public MultiReadIterator(MappingResultIterator mri,
                             int uniqReadCnt,
                             float identityCutoff,
                             int joinFactor,
                             boolean useExonRegion,
                             boolean checkByContaining,
                             int minimumOverlap,
                             boolean checkAllBlocks,
                             CanonicalGFF cgff,
                             Map geneUniqReadCntMap,
                             Map geneMultiReadCntMap
            ){
      this(mri,uniqReadCnt,identityCutoff,joinFactor,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,false,cgff,geneUniqReadCntMap,geneMultiReadCntMap);
    }

    public MultiReadIterator(MappingResultIterator mri,
                             int uniqReadCnt,
                             float identityCutoff,
                             int joinFactor,
                             boolean useExonRegion,
                             boolean checkByContaining,
                             int minimumOverlap,
                             boolean checkAllBlocks,
                             boolean careDirection,
                             CanonicalGFF cgff,
                             Map geneUniqReadCntMap,
                             Map geneMultiReadCntMap
            ){
        this.mri = mri;
        this.uniqReadCnt = uniqReadCnt;
        this.identityCutoff = identityCutoff;
        this.joinFactor = joinFactor;
        this.useExonRegion = useExonRegion;
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;
        this.checkAllBlocks = checkAllBlocks;
        this.careDirection = careDirection;
        this.cgff = cgff;
        this.geneUniqReadCntMap = geneUniqReadCntMap;
        this.geneMultiReadCntMap = geneMultiReadCntMap;
    }

    public void iterate(ReadCounter rc){
        for(MappingResultIterator iterator = mri;iterator.hasNext();){
            ArrayList mappingRecords = (ArrayList) iterator.next();
            // looking only for potential multi-reads
            //if(multiReads.contains(iterator.getReadID())==false) continue;
            if(iterator.getBestIdentity()<identityCutoff) continue;
            // collect records with best identity
            ArrayList acceptedRecords = new ArrayList();
            for(int i=0;i<mappingRecords.size();i++){
                AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                if(record.identity>=iterator.getBestIdentity()){
                    // join nearby blocks
                    // because alignments will be checked for splicing soon
                    if(joinFactor>0) record.nearbyJoin(joinFactor);
                    acceptedRecords.add(record);
                }
            }

            // skip if #alignment!=1 and #hitGeneRegions!=1
            Set hitGenesSet = new HashSet();
            for(int i=0;i<acceptedRecords.size();i++){
                AlignmentRecord alignmentRecord = (AlignmentRecord) acceptedRecords.get(i);
                Set hitGenes = cgff.getRelatedGenes(alignmentRecord,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,careDirection);
                hitGenesSet.addAll(hitGenes);
            }
            if(acceptedRecords.size()==1 && hitGenesSet.size()==1) continue;

            // skip it if some alignments are spliced
            boolean noSplice = true;
            for(int i=0;i<acceptedRecords.size();i++){
                if(((AlignmentRecord)acceptedRecords.get(i)).numBlocks>1){
                    noSplice = false;
                    break;
                }
            }
            if(noSplice==false){
            	restReadCnt++;
            	continue;
            }
            // collect mapped genes
            Set hitGeneRegions = new HashSet();
            Map hitGeneAlignmentMap = new HashMap();
            for(int i=0;i<acceptedRecords.size();i++){
                AlignmentRecord alignmentRecord = (AlignmentRecord) acceptedRecords.get(i);
                Set hitGenes = cgff.getRelatedGenes(alignmentRecord,useExonRegion,checkByContaining,minimumOverlap,checkAllBlocks,careDirection);

                for(Iterator hitGeneIterator = hitGenes.iterator();hitGeneIterator.hasNext();){
                    String geneID = (String) ((GenomeInterval) hitGeneIterator.next()).getUserObject();
                    if(hitGeneAlignmentMap.containsKey(geneID)){
                        Set set = (Set) hitGeneAlignmentMap.get(geneID);
                        set.add(alignmentRecord);
                    }else{
                        Set set = new HashSet();
                        set.add(alignmentRecord);
                        hitGeneAlignmentMap.put(geneID,set);
                    }
                }
                hitGeneRegions.addAll(hitGenes);
            }

            // get their total RPKM based on unique reads
            double totalUniqRPKM = 0;
            for(Iterator geneIterator = hitGeneRegions.iterator();geneIterator.hasNext();){
                String geneID = (String) ((GenomeInterval)geneIterator.next()).getUserObject();
                totalUniqRPKM += RPKMComputer.computeRPKM(geneUniqReadCntMap,uniqReadCnt,geneID,cgff,useExonRegion);
            }
            // compute their multi read counts
            if(totalUniqRPKM > (double) 0.0){
                for (Iterator geneIterator = hitGeneRegions.iterator();geneIterator.hasNext(); ) {
                    // RPKM counting
                    String geneID = (String) ((GenomeInterval) geneIterator.next()).getUserObject();
                    double uniqRPKM = RPKMComputer.computeRPKM(geneUniqReadCntMap, uniqReadCnt, geneID, cgff,useExonRegion);
                    // share this multi-read to this gene
                    double multiCnt = uniqRPKM / totalUniqRPKM;
                    if (geneMultiReadCntMap.containsKey(geneID)) {
                        double oldValue = ((Double) geneMultiReadCntMap.get(geneID)).doubleValue();
                        geneMultiReadCntMap.put(geneID, new Double(oldValue + multiCnt));
                    } else {
                        geneMultiReadCntMap.put(geneID, new Double(multiCnt));
                    }
                    // exon counting
                    if(rc!=null) rc.countReadMulti(iterator.getReadID(),(Set)hitGeneAlignmentMap.get(geneID),new Double(multiCnt),geneID,cgff);
                }
            }else{
                for (Iterator geneIterator = hitGeneRegions.iterator();geneIterator.hasNext(); ) {
                    // PRKM counting
                    String geneID = (String) ((GenomeInterval) geneIterator.next()).getUserObject();
                    // share this multi-read to this gene EVENLY
                    double multiCnt = (double)1.0 / hitGeneRegions.size();
                    if (geneMultiReadCntMap.containsKey(geneID)) {
                        double oldValue = ((Double) geneMultiReadCntMap.get(geneID)).doubleValue();
                        geneMultiReadCntMap.put(geneID, new Double(oldValue + multiCnt));
                    } else {
                        geneMultiReadCntMap.put(geneID, new Double(multiCnt));
                    }
                    // exon counting
                    if(rc!=null) rc.countReadMulti(iterator.getReadID(),(Set)hitGeneAlignmentMap.get(geneID),new Double(multiCnt),geneID,cgff);
                }
            }

            // count according to number of mapped gene regions
            if(hitGeneRegions.size()==0){
                restReadCnt++;
            }else{
                mlutiReadCnt++;
            }
        }
    }
}
