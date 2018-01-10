package misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import misc.intervaltree.IntervalTree;

/**
 * This class reads a GFF file, doing "canonicalization" of its exons
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class CanonicalGFF {

    static private String inFilename = null;
    static private String outFilename = null;
    static private Set geneExonFeaturePairs = new LinkedHashSet();
    static private String idAttr = null;
    static private ArrayList parentAttrs = new ArrayList();
    
    // controls
    static private boolean intronPreserving = false;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-I")) {
                inFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-O")){
                outFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-GE")){
                List list = new ArrayList();
                list.add(args[i+1]);
                list.add(args[i+2]);
                geneExonFeaturePairs.add(list);
                i+=2;
            }
            else if(args[i].equals("-idAttr")){
                idAttr = args[i + 1];
                i++;
            }
            else if(args[i].equals("-parentAttr")){
                parentAttrs.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-IP")){
            	intronPreserving = Boolean.valueOf(args[i+1]);
                i++;
            }
        }

        // check for necessary parameters
        if(inFilename==null){
          System.err.println("GFF filename (-I) not assigned");
          System.exit(1);
        }
        if(outFilename==null){
          System.err.println("output filename (-O) not assigned");
          System.exit(1);
        }

        // post-processing
        if(idAttr==null){
            idAttr = "ID";
            System.out.println("ID attribute (-idAttr) not assigned, using default: " + idAttr);
        }
        if(parentAttrs.size()==0){
            parentAttrs.add("Parent");
            parentAttrs.add("Derives_from");
            System.out.println("parent attribute list (-parentAttr) not assigned, using default: " + parentAttrs);
        }
        if(geneExonFeaturePairs.size()==0){
            List list = new ArrayList();
            list.add("gene");
            list.add("exon");
            geneExonFeaturePairs.add(list);
            System.out.println("gene-exon feature pairs (-GE) not assigned, using default: " + geneExonFeaturePairs);
        }

        // list parameters
        System.out.println("program: CanonicalGFF");
        System.out.println("input GFF file (-I): "+inFilename);
        System.out.println("output filename (-O): "+outFilename);
        System.out.println("gene-exon feature pairs (-GE): "+geneExonFeaturePairs);
        System.out.println("ID attribute (-idAttr): "+idAttr);
        System.out.println("parent attribute list (-parentAttr)): "+parentAttrs);
        System.out.println("intron preserving (-IP)): "+intronPreserving);
        System.out.println();
    }

    private static void buildGeneExonRelationships(DefaultMutableTreeNode node, Map map, GffRecord geneRecord, String geneFeature, String exonFeature) {
        GffRecord record = (GffRecord) node.getUserObject();

        if (record.getFeature().equals(geneFeature)) {
            geneRecord = record;
        }

        if (record.getFeature().equals(exonFeature) && geneRecord != null) {
            if (map.containsKey(geneRecord)) {
                Set exonSet = (Set) map.get(geneRecord);
                exonSet.add(record.getInterval());
            } else {
                Set set = new TreeSet();
                set.add(record.getInterval());
                map.put(geneRecord, set);
            }
        }

        int i;
        for (i = 0; i < node.getChildCount(); i++) {
            buildGeneExonRelationships((DefaultMutableTreeNode) node.getChildAt(i),
                                       map, geneRecord, geneFeature, exonFeature);
        }
        return;
    }

    /**
     * This main function reads a GFF3-format file then output a <i>canonicalized</i> GFF file.
     * The canonicalized GFF file could be read and utilized by creating a CanonicalGFF object.
     */
    public static void main(String[] args) {
        paraProc(args);

        // read input GFF
        GffTree gffTree = new GffTree(inFilename,idAttr,parentAttrs);

        // parse geneExonFeaturePairs
        Set removeSet = new HashSet();
        Set addSet = new LinkedHashSet();
        String pathSeparator = System.getProperty("path.separator");
        for(Iterator iterator = geneExonFeaturePairs.iterator();iterator.hasNext();){
            ArrayList pair = (ArrayList) iterator.next();
            String geneFeature = (String) pair.get(0);
            String exonFeature = (String) pair.get(1);
            if(geneFeature.indexOf(pathSeparator) < 0 && exonFeature.indexOf(pathSeparator) < 0){
                // do nothing
            }else{
                // remove
                removeSet.add(pair);
                // re-generate
                StringTokenizer st1 = new StringTokenizer(geneFeature,pathSeparator);
                while(st1.hasMoreTokens()){
                    String token1 = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(exonFeature,pathSeparator);
                    while(st2.hasMoreTokens()){
                        String token2 = st2.nextToken();
                        ArrayList newPair = new ArrayList();
                        newPair.add(token1);
                        newPair.add(token2);
                        addSet.add(newPair);
                    }
                }
            }
        }
        geneExonFeaturePairs.removeAll(removeSet);
        geneExonFeaturePairs.addAll(addSet);

        // build gene-exons relationships, where genes are GffRecords and exons are intervals
        Map geneExonsMap = new TreeMap();
        for(Iterator iterator = geneExonFeaturePairs.iterator();iterator.hasNext();){
            ArrayList pair = (ArrayList) iterator.next();
            String geneFeature = (String) pair.get(0);
            String exonFeature = (String) pair.get(1);
            buildGeneExonRelationships(gffTree.getRoot(),geneExonsMap,null,geneFeature,exonFeature);
        }

        // canonicalize exons -- intron preserving
        if(intronPreserving){
	        for(Iterator iterator = geneExonsMap.keySet().iterator();iterator.hasNext();){
	            GffRecord geneRecord = (GffRecord) iterator.next();
	            Set exonSet = (Set) geneExonsMap.get(geneRecord);
	            // form position-d/a list
	            Set daSet = new TreeSet();
	            for(Iterator exonIterator = exonSet.iterator(); exonIterator.hasNext();){
	            	Interval exon = (Interval) exonIterator.next();
	            	daSet.add(new Pair(exon.getStop(),0)); // 0 for stops
	            	daSet.add(new Pair(exon.getStart(),1)); // 1 for starts
	            }
	            // remove near-by stop-start pairs
	            Pair[] daList = (Pair[]) daSet.toArray(new Pair[0]);
	            removeSet = new HashSet();
	            for(int i=0;i+1<daList.length;i++){
	            	// a near-by stop-start pair
	            	if(((Integer)daList[i].key1)==((Integer)daList[i+1].key1)-1 && daList[i].key2.compareTo(daList[i+1].key2) < 0){
	            		// stop at i, there must be an item at i-1
	            		if(((Integer)daList[i-1].key2)==0){
	            			removeSet.add(daList[i]);
	            		}
	            		// start at i+1, there must be an item at i+2
	            		if(((Integer)daList[i+2].key2)==1){
	            			removeSet.add(daList[i+1]);
	            		}
	            	}
	            }
	            daSet.removeAll(removeSet);
	            // output new exons: the very first start, closest stop-start pairs, and the very last stop
	            exonSet.clear();
	            daList = (Pair[]) daSet.toArray(new Pair[0]);
	            Pair lastStart = daList[0]; // must be a start
	            for(int i=1;i<daList.length-1;i++){
	            	if(daList[i].key2.compareTo(daList[i+1].key2) < 0){ // a consecutive stop-start pair
	            		exonSet.add(new Interval((Integer)lastStart.key1,(Integer)daList[i].key1));
	            		lastStart = daList[i+1];
	            	}
	            }
	            exonSet.add(new Interval((Integer)lastStart.key1,(Integer)daList[daList.length-1].key1)); // the very last stop
	        }
        }else{ // combine overlapping intervals
	        for(Iterator iterator = geneExonsMap.keySet().iterator();iterator.hasNext();){
	            GffRecord geneRecord = (GffRecord) iterator.next();
	            Set exonSet = (Set) geneExonsMap.get(geneRecord);
	            Interval.combineOverlap(exonSet);
	        }
        }
        

        // canonicalize exons -- combine nearby intervals
        for(Iterator iterator = geneExonsMap.keySet().iterator();iterator.hasNext();){
            GffRecord geneRecord = (GffRecord) iterator.next();
            TreeSet exonSet = (TreeSet) geneExonsMap.get(geneRecord);
            Interval.combineNearby(exonSet,1);
        }

        // write canonicalized gene-exons relationships
        try {
            FileWriter fw = new FileWriter(outFilename);

            for(Iterator iterator1 = geneExonsMap.keySet().iterator();iterator1.hasNext();){
                GffRecord geneRecord = (GffRecord) iterator1.next();
                fw.write(">"+
                         geneRecord.getAttrMap().get(idAttr)+"\t"+
                         geneRecord.getChr().intern()+"\t"+
                         geneRecord.getInterval().getStart()+"\t"+
                         geneRecord.getInterval().getStop()+"\t"+
                         geneRecord.getStrand()+"\n"
                        );
                Set exonSet = (Set) geneExonsMap.get(geneRecord);
                for(Iterator iterator2 = exonSet.iterator();iterator2.hasNext();){
                    Interval interval = (Interval) iterator2.next();
                    fw.write(interval.getStart()+"\t"+interval.getStop()+"\n");
                }
            }

            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public Map geneRegionMap = new LinkedHashMap();
    public Map<Object,TreeSet<Interval>> geneExonRegionMap = new LinkedHashMap<Object,TreeSet<Interval>>();
    public Map geneLengthMap = new LinkedHashMap();
    public Map geneStrandMap = new LinkedHashMap();
    public Map chrIntervalTreeMap = new LinkedHashMap();

    //
    public Map chrOriginalMap = new HashMap();

    /**
     * @deprecated
     */
    public Set getContainingGenes(AlignmentRecord alignment,boolean usingExonRange){
        return getContainingGenes(alignment,usingExonRange,0);
    }

    /**
     * @deprecated
     */
    public Set getContainingGenes(AlignmentRecord alignment,boolean useExonRange,int minimumBlockSize){
        Set ansSet = new LinkedHashSet();

        if(chrIntervalTreeMap.containsKey(alignment.chr)){
            IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(alignment.chr);

            for (int i = 0; i < alignment.numBlocks; i++) {
                ansSet.addAll(tree.searchAll(new Interval(alignment.tStarts[i],
                                                          alignment.tStarts[i] +
                                                          alignment.tBlockSizes[i] - 1)));
            }
        }

        if(useExonRange){
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean allContained = true;
                Set exonSet = (Set) geneExonRegionMap.get(geneRegion.getUserObject());
                for (int i = 0; i < alignment.numBlocks; i++) {
                    boolean contained = false;
                    for (Iterator exonIterator = exonSet.iterator();exonIterator.hasNext(); ) {
                        Interval exonRegion = (Interval) exonIterator.next();
                        if (exonRegion.contain(alignment.tStarts[i],alignment.tStarts[i] + alignment.tBlockSizes[i] - 1) &&
                            alignment.tBlockSizes[i] >= minimumBlockSize) {
                            contained = true;
                            break;
                        }
                    }
                    if(contained==false){
                        allContained=false;
                        break;
                    }
                }
                if(allContained==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }else{
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean allContained = true;
                for (int i = 0; i < alignment.numBlocks; i++) {
                    if (geneRegion.contain(alignment.tStarts[i],alignment.tStarts[i] + alignment.tBlockSizes[i] - 1) &&
                        alignment.tBlockSizes[i] >= minimumBlockSize) {
                        // donothing
                    }else{
                        allContained = false;
                        break;
                    }
                }
                if(allContained==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }

        return ansSet;
    }

    /**
     * @deprecated
     */
    public Set getContainingGenes(String chr,Set intervalSet,boolean useExonRange){
        Set ansSet = new LinkedHashSet();

        if(chrIntervalTreeMap.containsKey(chr)){
            IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(chr);

            for (Iterator iterator = intervalSet.iterator();iterator.hasNext();) {
                Interval interval = (Interval) iterator.next();
                ansSet.addAll(tree.searchAll(interval));
            }
        }

        if(useExonRange){
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean allContained = true;
                Set exonSet = (Set) geneExonRegionMap.get(geneRegion.getUserObject());
                for (Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();) {
                    Interval interval = (Interval) intervalIterator.next();
                    boolean contained = false;
                    for (Iterator exonIterator = exonSet.iterator();exonIterator.hasNext(); ) {
                        Interval exonRegion = (Interval) exonIterator.next();
                        if (exonRegion.contain(interval.getStart(),interval.getStop())) {
                            contained = true;
                            break;
                        }
                    }
                    if(contained==false){
                        allContained=false;
                        break;
                    }
                }
                if(allContained==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }else{
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean allContained = true;
                for (Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();) {
                    Interval interval = (Interval) intervalIterator.next();
                    if (geneRegion.contain(interval.getStart(),interval.getStop())) {
                        // donothing
                    }else{
                        allContained = false;
                        break;
                    }
                }
                if(allContained==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }

        return ansSet;
    }

    /**
     * @deprecated
     */
    public Set getIntersectingGenes(String chr,Set intervalSet,boolean useExonRange){
        Set ansSet = new LinkedHashSet();

        if(chrIntervalTreeMap.containsKey(chr)){
            IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(chr);

            for (Iterator iterator = intervalSet.iterator();iterator.hasNext();) {
                Interval interval = (Interval) iterator.next();
                ansSet.addAll(tree.searchAll(interval));
            }
        }

        if(useExonRange){
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean intersected = false;
                Set exonSet = (Set) geneExonRegionMap.get(geneRegion.getUserObject());
                intervalFor:
                for (Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();) {
                    Interval interval = (Interval) intervalIterator.next();
                    for (Iterator exonIterator = exonSet.iterator();exonIterator.hasNext(); ) {
                        Interval exonRegion = (Interval) exonIterator.next();
                        if (exonRegion.intersect(interval.getStart(),interval.getStop())) {
                            intersected = true;
                            break intervalFor;
                        }
                    }
                }
                if(intersected==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }else{
            Set removeSet = new HashSet();

            for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
                GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();
                boolean intersected = false;
                for (Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();) {
                    Interval interval = (Interval) intervalIterator.next();
                    if (geneRegion.intersect(interval.getStart(),interval.getStop())) {
                        intersected = true;
                        break;
                    }else{
                        // do nothing
                    }
                }
                if(intersected==false){
                    removeSet.add(geneRegion);
                }
            }

            ansSet.removeAll(removeSet);
        }

        return ansSet;
    }

    /**
     * Get genes with fitting junctions.
     * @param chr				chromosome
     * @param intervalSet		exon interval set
     * @param forwardAlignment	direction of the alignment
     * @param careDirection		consider alignment direction
     * @param minBlockSize		size filter to remove unqualified exons first
     * @return
     */
    public Set getRelatedGenesByJunction(String chr,Set intervalSet,boolean forwardAlignment,boolean careDirection,
    		int minBlockSize){
    	
    	TreeSet querySet = new TreeSet();
    	
    	// remove boundary small blocks
    	boolean first = true;
    	for(Iterator iterator = intervalSet.iterator();iterator.hasNext();){
    		Interval interval = (Interval) iterator.next();
    		if(interval.length()<minBlockSize && (first || iterator.hasNext()==false)){
    			// do nothing
    		}else{
    			querySet.add(interval);
    		}
    		first = false;
    	}
    	
    	Set ansSet = new LinkedHashSet();
    	if(querySet.size()<2){ // no need to do junction verification
    		return ansSet;
    	}
    	
    	// get potential genes
    	ansSet = getRelatedGenes(chr,querySet,forwardAlignment,useEXON,byINTERSECT,1,checkALL,careDirection,true,true);
    	
    	// check junction for each gene
    	HashSet delSet = new HashSet();
    	for(Iterator iterator=ansSet.iterator();iterator.hasNext();){
    		GenomeInterval gi = (GenomeInterval) iterator.next();
    		String geneID = (String) gi.getUserObject();
    		Set exonSet = (Set) geneExonRegionMap.get(geneID);
    		
    		// prepare exon / block
    		Iterator exonIterator = exonSet.iterator();
    		Iterator blockIterator = querySet.iterator();
    		Interval exon = (Interval) exonIterator.next();
    		Interval block = (Interval) blockIterator.next();

    		boolean pass = false;
    		boolean firstExon = true;
    		// go to first exon that intersecting first block
    		while(exon.intersect(block) == false){
            	if(exonIterator.hasNext()){
                    exon = (Interval) exonIterator.next();
                    firstExon = false;
            	}else{
            		exon = null;
            		break;
            	}
    		}
    		if(exon!=null && exon.intersect(block)){ // maybe PASS if there is an intersection
    			pass = true;
    		}
    		
    		// iterate exon & block at the same time for checking
    		boolean firstBlock = true;
    		while( exon!=null && block!=null ){
    			
    			if(firstBlock){ // first block
    				if(block.getStop()!=exon.getStop()){ // check its stop
    					pass = false;
    					break;
    				}
    				if(!firstExon && block.getStart()<exon.getStart()){ // no covering inner exon's start
    					pass = false;
    					break;
    				}
    			}else if(blockIterator.hasNext() == false){ // last block
    				if(block.getStart()!=exon.getStart()){ // check its start
    					pass = false;
    					break;
    				}
    				if(exonIterator.hasNext() && block.getStop()>exon.getStop()){ // no covering inner exon's stop
    					pass = false;
    					break;
    				}
    			}else{ // mid block, exactly fit
    				if(block.getStart()!=exon.getStart() || block.getStop()!=exon.getStop()){
    					pass = false;
    					break;
    				}
    			}
    			
        		// iteration
    			if(blockIterator.hasNext()){
    				block = (Interval) blockIterator.next();
    				firstBlock = false;
    			}else{
    				block = null;
    			}
    			if(exonIterator.hasNext()){
    				exon = (Interval) exonIterator.next();
                    firstExon = false;
    			}else{
    				exon = null;
    			}
    		}
    		
    		if(pass == false){
    			delSet.add(gi);
    		}
    	}
    	ansSet.removeAll(delSet);
    	
    	return ansSet;
    }

    /**
     * Get genes with fitting junction counts.
     * @param chr				chromosome
     * @param intervalSet		exon interval set
     * @param forwardAlignment	direction of the alignment
     * @param careDirection		consider alignment direction
     * @param minBlockSize		size filter to remove unqualified exons first
     * @return A LinkedHashMap of GenomeInterval-Integer entries in decreasing order.
     */
    public Map<GenomeInterval, Integer> getRelatedGenesWithJunctionCounts(String chr,Set intervalSet,boolean forwardAlignment,boolean careDirection,
    		int minBlockSize){
    	
    	TreeSet querySet = new TreeSet();
    	
    	// remove boundary small blocks
    	boolean first = true;
    	for(Iterator iterator = intervalSet.iterator();iterator.hasNext();){
    		Interval interval = (Interval) iterator.next();
    		if(interval.length()<minBlockSize && (first || iterator.hasNext()==false)){
    			// do nothing
    		}else{
    			querySet.add(interval);
    		}
    		first = false;
    	}
    	
    	Map<GenomeInterval,Integer> ansMap = new HashMap<GenomeInterval,Integer>();
    	
    	// get potential genes
    	Set relatedGenes = getRelatedGenes(chr,querySet,forwardAlignment,useEXON,byINTERSECT,1,checkSOME,careDirection,true,true);
    	
    	// check junction for each gene
    	for(Iterator iterator=relatedGenes.iterator();iterator.hasNext();){
    		GenomeInterval gi = (GenomeInterval) iterator.next();
    		String geneID = (String) gi.getUserObject();
    		Set exonSet = (Set) geneExonRegionMap.get(geneID);
    		int hitJunctionCnt = 0;
    		
    		// prepare exon / block
    		Iterator exonIt = exonSet.iterator();
    		Iterator blockIt = querySet.iterator();
    		Interval exon = (Interval) exonIt.next();
    		Interval block = (Interval) blockIt.next();

    		// iterate exon & block at the same time for checking
    		boolean firstBlock = true;
        	while( exon!=null && block!=null ){
        		
        		// if block intersects exon
        		if( block.intersect(exon) ){
        			if(blockIt.hasNext()){ // has next block
        				if(block.getStop()==exon.getStop()){ // check its stop
        					hitJunctionCnt++;
        				}
        			}
        			if(!firstBlock){ // has a previous block
        				if(block.getStart()==exon.getStart()){ // check its start
        					hitJunctionCnt++;
        				}
        			}
        		}

        		// iteration
                if( block.getStop() < exon.getStop() ){
                	if(blockIt.hasNext()){
                        block = (Interval) blockIt.next();
                        firstBlock = false;
                	}else{
                		block = null;
                	}
                }else{
                	if(exonIt.hasNext()){
                        exon = (Interval) exonIt.next();
                	}else{
                		exon = null;
                	}
                }
        	}
        	
        	ansMap.put(gi, hitJunctionCnt);
    	}
    	
    	Set<Map.Entry<GenomeInterval, Integer>> sortedEntries = Util.entriesSortedByValues(ansMap,false);
    	LinkedHashMap<GenomeInterval, Integer> sortedAnsMap = new LinkedHashMap<GenomeInterval, Integer>(); 
    	
    	for(Map.Entry<GenomeInterval, Integer> entry : sortedEntries){
    		sortedAnsMap.put(entry.getKey(), entry.getValue());
    	}
    	
    	return sortedAnsMap;
    }
    
    static public final boolean useEXON = true;
    static public final boolean useGENOME = false;
    static public final boolean byCONTAIN = true;
    static public final boolean byINTERSECT = false;
    static public final boolean checkALL = true;
    static public final boolean checkSOME = false;
    static public final boolean directionCARE = true;
    static public final boolean directionIGNORE = false;

    public Set getRelatedGenes(String chr,int start,int stop,boolean useExonRange,boolean byContaining,int minimumOverlap){
        Set querySet = new HashSet();
        querySet.add(new Interval(start,stop));
        return getRelatedGenes(chr,querySet,useExonRange,byContaining,minimumOverlap,false);
    }

    // old getRelatedGenes interface
    public Set getRelatedGenes(String chr,Set intervalSet,
                               boolean useExonRange,boolean byContaining,int minimumOverlap,boolean requireAllBlocks){
      return getRelatedGenes(chr,intervalSet,true,useExonRange,byContaining,minimumOverlap,requireAllBlocks,false,true,true);
    }

    // getRelatedGenes interface with alignment
    public Set getRelatedGenes(AlignmentRecord alignment,
                               boolean useExonRange,boolean byContaining,int minimumOverlap,boolean requireAllBlocks,boolean careDirection){
      return getRelatedGenes(alignment.chr,alignment.getMappingIntervals(),alignment.forwardStrand,
                             useExonRange,byContaining,minimumOverlap,requireAllBlocks,careDirection,true,true);
    }

    /**
     * The key function of gene query.
     * @param chr				chromosome of the alignment.
     * @param intervalSet		alignment blocks as a Set of Intervals.
     * @param forwardAlignment	direction of the alignment.
     * @param useExonRange		true to use exon regions to check alignment; otherwise, use gene regions.
     * @param byContaining		true qualify alignmnet blocks by containment; otherwise, by intersection.
     * @param minimumOverlap	minimum overlap size to qualify containment / intersection.
     * @param requireAllBlocks	require all alignment blocks to be qualified.
     * @param careDirection		consider alignment direction.
     * @param interMinorFix		true to fix the problem of small overlap size with minor & inter blocks
     * @param outerMinorFix		true to fix the problem of small overlap size with minor & outer blocks
     * @return					A Set of GenomeIntervals with user object of gene IDs in String.
     */
    public Set getRelatedGenes(String chr,Set intervalSet,boolean forwardAlignment,
                               boolean useExonRange,boolean byContaining,int minimumOverlap,boolean requireAllBlocks,
                               boolean careDirection,boolean interMinorFix,boolean outerMinorFix){
        Set ansSet = new LinkedHashSet();

        // get potential hit genes
        if(chrIntervalTreeMap.containsKey(chr)){
            IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(chr);

            for (Iterator iterator = intervalSet.iterator();iterator.hasNext();) {
                Interval interval = (Interval) iterator.next();
                ansSet.addAll(tree.searchAll(interval));
            }
        }

        // for each potential hit gene
        Set removeSet = new HashSet();
        for(Iterator geneRegionIterator = ansSet.iterator();geneRegionIterator.hasNext();){
            GenomeInterval geneRegion = (GenomeInterval) geneRegionIterator.next();

            // prepare qualifying interval(s)
            Set qualifyingIntervalSet;
            if(useExonRange){
                qualifyingIntervalSet = (Set) geneExonRegionMap.get(geneRegion.getUserObject());
            }else{
                qualifyingIntervalSet = new TreeSet();
                qualifyingIntervalSet.add(geneRegion);
            }

            // qualify each input interval
            boolean allBlockQualified = true;
            boolean someBlockQualified = false;
            for(Iterator iiIterator = intervalSet.iterator();iiIterator.hasNext();){
                Interval inInterval = (Interval) iiIterator.next();
                boolean thisBlockQualified = false;
                int intervalCnt = 0;
                for (Iterator qiIterator = qualifyingIntervalSet.iterator();qiIterator.hasNext(); ) {
                    Interval qInterval = (Interval) qiIterator.next();
                    intervalCnt++;
                    if(byContaining){
                        // check containment
                        if(qInterval.contain(inInterval.getStart(),inInterval.getStop())==false) continue;
                        // check overlap size
                        if( (inInterval.length() >= minimumOverlap) || // qualified if intersection >= threshold
                        	( interMinorFix &&
                        	  (intervalCnt>1 && qiIterator.hasNext()) &&
                        	  (inInterval.getStart()==qInterval.getStart() && inInterval.getStop()==qInterval.getStop())
                        	) || // OR a inter-minor interval fits the block
                        	( outerMinorFix &&
                              (intervalCnt==1 || qiIterator.hasNext()==false)
                           	) // OR a outer-minor interval
                          ){
                        	// do nothing
                        }else{
                        	continue;
                        }
                    }else{ // by intersection
                        // check intersection
                        if(qInterval.intersect(inInterval)==false) continue;
                        // check overlap size
                        int intersectStart = (inInterval.getStart()>qInterval.getStart()) ? inInterval.getStart() : qInterval.getStart() ;
                        int intersectStop = (inInterval.getStop()<qInterval.getStop()) ? inInterval.getStop() : qInterval.getStop() ;
                        int intersectSize = intersectStop-intersectStart+1;

                        if( (intersectSize >= minimumOverlap) || // qualified if intersection >= threshold
                        	( interMinorFix &&
                        	  (intervalCnt>1 && qiIterator.hasNext()) &&
                        	  inInterval.contain(qInterval.getStart(), qInterval.getStop())
                        	) || // OR a inter-minor interval contained by the block
                        	( outerMinorFix &&
                   			  (intervalCnt==1 || qiIterator.hasNext()==false) &&
                   			  (qInterval.length() < minimumOverlap)
                        	) // OR a outer-minor interval
                          ){
                        	// do nothing
                        }else{
                        	continue;
                        }
                    }
                    thisBlockQualified = true;
                    break;
                }
                // update
                if(thisBlockQualified){
                    someBlockQualified = true;
                    if(requireAllBlocks==false) break; // for minor speed up
                }else{
                    allBlockQualified = false;
                    if(requireAllBlocks) break; // for minor speed up
                }
            }
            // make decision
            if(requireAllBlocks){
                if(allBlockQualified==false){
                    removeSet.add(geneRegion);
                }
            }else{
                if(someBlockQualified==false){
                    removeSet.add(geneRegion);
                }
            }
        }
        ansSet.removeAll(removeSet);

        if(careDirection){
          removeSet = new HashSet();
          for (Iterator giIterator = ansSet.iterator(); giIterator.hasNext(); ) {
            GenomeInterval gi = (GenomeInterval) giIterator.next();
            Object geneID = gi.getUserObject();
            if( !((getStrand(geneID).equals("+") && forwardAlignment) || (getStrand(geneID).equals("+")==false && forwardAlignment==false)) ){
              removeSet.add(gi);
            }
          }
          ansSet.removeAll(removeSet);
        }

        return ansSet;
    }


    private static void buildGeneRnaExonRelationships(DefaultMutableTreeNode node,
    		Map rnaExonMap, Map rnaGeneMap,
    		GffRecord geneRecord, GffRecord rnaRecord,
    		String geneFeature, String rnaFeature, String exonFeature, String idAttr) {
        GffRecord record = (GffRecord) node.getUserObject();

        // if meet a gene record
        if (record.getFeature().equals(geneFeature)) {
            geneRecord = record;
        }
        // if meet a rna record
        if (record.getFeature().equals(rnaFeature)) {
        	rnaRecord = record;
        	
        	// save rna-gene relationship into rnaGeneMap
        	if(geneRecord !=null){
        		rnaGeneMap.put(rnaRecord.getAttrMap().get(idAttr), geneRecord.getAttrMap().get(idAttr));
        	}
        }

        // if meet an exon record and a ready rna record
        if (record.getFeature().equals(exonFeature) && rnaRecord != null && geneRecord != null) {
            if (rnaExonMap.containsKey(rnaRecord)) {
                Set exonSet = (Set) rnaExonMap.get(rnaRecord);
                exonSet.add(record.getInterval());
            } else {
                Set set = new TreeSet();
                set.add(record.getInterval());
                rnaExonMap.put(rnaRecord, set);
            }
        }

        // recursive
        int i;
        for (i = 0; i < node.getChildCount(); i++) {
        	buildGeneRnaExonRelationships((DefaultMutableTreeNode) node.getChildAt(i),
        			rnaExonMap, rnaGeneMap,
            		geneRecord, rnaRecord,
            		geneFeature, rnaFeature, exonFeature, idAttr);
        }
        return;
    }
    
    
    private static Map readRepFile(String filename){
    	Map map = new HashMap();
    	
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            while(fr.ready()){
                String line = fr.readLine().trim();
                if(line.startsWith("#")) continue;
                if(line.length()==0) continue;

                StringTokenizer st = new StringTokenizer(line,"\t");
                String gene = st.nextToken();
                String repModel = st.nextToken();
                map.put(gene, repModel);
            }
            fr.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        return map;
    }

    /**
     * A constructor that generates a CanonicalGFF based on representative models
     * and merged exons from other models. Exon Intervals are assigned IDs as
     * their user objects.
     * @param mergedCGFF	a CGFF of merged models
     * @param modelCGFF		a CGFF of all models
     * @param repFilename	a file stores gene-representative mapping
     */
    public CanonicalGFF(CanonicalGFF mergedCGFF, CanonicalGFF modelCGFF, String repFilename){
    	
    	Map geneRepMap = readRepFile(repFilename);
    	
    	for(Iterator geneIterator = geneRepMap.keySet().iterator(); geneIterator.hasNext();){
    		String geneID = (String) geneIterator.next();
    		String repID = (String) geneRepMap.get(geneID);

    		// basic info
    		GenomeInterval gi = (GenomeInterval) mergedCGFF.geneRegionMap.get(geneID);
            String chrOriginal = (String) mergedCGFF.chrOriginalMap.get(gi.getChr());
            String chr = chrOriginal.toLowerCase().intern();
            chrOriginalMap.put(chr,chrOriginal);
            GenomeInterval geneRegion = new GenomeInterval(chr,
            		gi.getStart(),
            		gi.getStop(),
            		repID);
            // strand
            geneStrandMap.put(repID,mergedCGFF.geneStrandMap.get(geneID));
            // gene region
            geneRegionMap.put(repID,geneRegion);
            // insert into corresponding interval tree
            if(chrIntervalTreeMap.containsKey(geneRegion.getChr().toLowerCase())){
                IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr().toLowerCase());
                tree.insert(geneRegion);
            }else{
                IntervalTree tree = new IntervalTree();
                tree.insert(geneRegion);
                chrIntervalTreeMap.put(geneRegion.getChr().toLowerCase(),tree);
            }

            // exon region
            TreeSet repExons = (TreeSet) modelCGFF.geneExonRegionMap.get(repID);
            TreeSet cgffExons = (TreeSet) mergedCGFF.geneExonRegionMap.get(geneID);
            TreeSet exonRanges = new TreeSet();
            geneExonRegionMap.put(repID,exonRanges);
            // exon region, insert exons from representative
            if(modelCGFF.geneStrandMap.get(repID).equals("+")){
                int idx=1;
            	for(Iterator it=repExons.iterator();it.hasNext();){
            		Interval ii = (Interval) it.next();
            		exonRanges.add(new Interval(ii.getStart(),ii.getStop(),idx));
            		idx++;
            	}
            }else{
                int idx=1;
            	for(Iterator it=repExons.descendingIterator();it.hasNext();){
            		Interval ii = (Interval) it.next();
            		exonRanges.add(new Interval(ii.getStart(),ii.getStop(),idx));
            		idx++;
            	}
            }
            // exon region, insert exons from merged CGFF
            for(Iterator it1=cgffExons.iterator();it1.hasNext();){
            	Interval ii1 = (Interval) it1.next();
            	boolean contain = false;
            	for(Iterator it2=repExons.iterator();it2.hasNext();){
            		Interval ii2 = (Interval) it2.next();
            		if(ii1.contain(ii2.getStart(), ii2.getStop())){
            			contain = true;
            		}
            	}
            	if(!contain){
            		exonRanges.add(new Interval(ii1.getStart(),ii1.getStop()));
            	}
            }
            // exon region, numbering exons
            Iterator it;
            if(modelCGFF.geneStrandMap.get(repID).equals("+")){
            	it=exonRanges.iterator();
            }else{
            	it=exonRanges.descendingIterator();
            }
            int idx1=0;
            int idx2=1;
            int length = 0;
            for(;it.hasNext();){
            	Interval ii = (Interval) it.next();
            	if(ii.getUserObject() == null){
            		ii.setUserObject( idx1 + "^" + idx2 );
            		idx2++;
            	}else{
            		idx1 = (Integer) ii.getUserObject();
            		idx2 = 1;
            		ii.setUserObject( "" + idx1 );
            	}
            	length += ii.length();
            }

            // length
            geneLengthMap.put(repID, length);
    	}
    }
    
    /**
     * A constructor that generates a CanonialGFF by merging RNAs from another CanonicalGFF
     * of models, where models are merged into genes based on a model-gene map
     * @param modelCGFF		source model CGFF
     * @param modelGeneMap	the model-gene map
     */
    public CanonicalGFF(CanonicalGFF modelCGFF, Map modelGeneMap){
    	// build a gene-exonSet map
    	Map geneExonSetMap = new HashMap();
    	Map geneModelMap = new HashMap();
    	for(Iterator rnaIterator = modelCGFF.geneExonRegionMap.keySet().iterator(); rnaIterator.hasNext();){
    		String rnaID = (String) rnaIterator.next();
    		String geneID = (String) modelGeneMap.get(rnaID);
    		
    		geneModelMap.put(geneID, rnaID);
    		
    		if(geneExonSetMap.containsKey(geneID)){
    			Set exonSet = (Set) geneExonSetMap.get(geneID);
    			exonSet.addAll((Set)modelCGFF.geneExonRegionMap.get(rnaID));
    		}else{
    			Set exonSet = new TreeSet();
    			geneExonSetMap.put(geneID, exonSet);
    			exonSet.addAll((Set)modelCGFF.geneExonRegionMap.get(rnaID));
    		}
    	}

    	// combine overlapping intervals
        for(Iterator iterator = geneExonSetMap.keySet().iterator();iterator.hasNext();){
            String geneID = (String) iterator.next();
            Set exonSet = (Set) geneExonSetMap.get(geneID);
            Interval.combineOverlap(exonSet);
        }
    

        // canonicalize exons -- combine nearby intervals
        for(Iterator iterator = geneExonSetMap.keySet().iterator();iterator.hasNext();){
        	String geneID = (String) iterator.next();
        	TreeSet exonSet = (TreeSet) geneExonSetMap.get(geneID);
        	Interval.combineNearby(exonSet,1);
        }

        // build
        for(Iterator geneIterator = geneExonSetMap.keySet().iterator(); geneIterator.hasNext();){
        	String geneID = (String) geneIterator.next();
        	TreeSet exonSet = (TreeSet) geneExonSetMap.get(geneID);
        	
            // get gene information
        	GenomeInterval rnaGI = (GenomeInterval) modelCGFF.geneRegionMap.get(geneModelMap.get(geneID));
            String chrOriginal = (String) modelCGFF.chrOriginalMap.get(rnaGI.getChr());
            String chr = chrOriginal.toLowerCase().intern();
            chrOriginalMap.put(chr,chrOriginal);
            GenomeInterval geneRegion = new GenomeInterval(chr,
            		((Interval)exonSet.first()).getStart(),
            		((Interval)exonSet.last()).getStop(),
            		geneID);
            // strand
            geneStrandMap.put(geneID,modelCGFF.geneStrandMap.get(geneModelMap.get(geneID)));
            // gene region
            geneRegionMap.put(geneID,geneRegion);
            // insert into corresponding interval tree
            if(chrIntervalTreeMap.containsKey(geneRegion.getChr().toLowerCase())){
                IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr().toLowerCase());
                tree.insert(geneRegion);
            }else{
                IntervalTree tree = new IntervalTree();
                tree.insert(geneRegion);
                chrIntervalTreeMap.put(geneRegion.getChr().toLowerCase(),tree);
            }
            // exon region
            TreeSet exonRanges = new TreeSet();
            geneExonRegionMap.put(geneID,exonRanges);
            int length = 0;
            for(Iterator exonIterator=exonSet.iterator();exonIterator.hasNext();){
            	Interval exon = (Interval) exonIterator.next();
            	length += exon.length();
            	exonRanges.add(exon);
            }
            // length
            geneLengthMap.put(geneID, length);
        }
    }
    
    /**
     * A constructor that generates a CanonicalGFF directly from GFF3 but not CGFF.
     * This constructor works by traversing the GFF tree according to triples in
     * gene-rna-exon level, it build the CanonicalGFF that represents rna-exon relationships,
     * and reports rna-gene relationships. 
     * @param GFF3filename			the GFF3 file
     * @param geneRnaExonTriples	specified gene-rna-exon triples
     * @param idAttr				the attribute of ID
     * @param parentAttrs			the attributes of parent
     * @param rnaGeneMap			the map to store rna-gene relationships
     */
    public CanonicalGFF(String GFF3filename,
    		Set geneRnaExonTriples,
    		String idAttr, List parentAttrs,
    		Map rnaGeneMap){

    	// read input GFF
        GffTree gffTree = new GffTree(GFF3filename,idAttr,parentAttrs);
        
        // parse geneRnaExonTriples
        Set removeSet = new HashSet();
        Set addSet = new LinkedHashSet();
        String pathSeparator = System.getProperty("path.separator");
        for(Iterator iterator = geneRnaExonTriples.iterator();iterator.hasNext();){
            ArrayList triple = (ArrayList) iterator.next();
            String geneFeature = (String) triple.get(0);
            String rnaFeature = (String) triple.get(1);
            String exonFeature = (String) triple.get(2);
            if(geneFeature.indexOf(pathSeparator) < 0
            		&& rnaFeature.indexOf(pathSeparator) < 0
            		&& exonFeature.indexOf(pathSeparator) < 0){
                // do nothing
            }else{
                // remove
                removeSet.add(triple);
                // re-generate
                StringTokenizer st1 = new StringTokenizer(geneFeature,pathSeparator);
                while(st1.hasMoreTokens()){
                    String token1 = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(rnaFeature,pathSeparator);
                    while(st2.hasMoreTokens()){
                        String token2 = st2.nextToken();
                        StringTokenizer st3 = new StringTokenizer(exonFeature,pathSeparator);
                        while(st3.hasMoreTokens()){
                        	String token3 = st3.nextToken();
                        	ArrayList newTriple = new ArrayList();
                        	newTriple.add(token1);
                        	newTriple.add(token2);
                        	newTriple.add(token3);
                        	addSet.add(newTriple);
                        }
                    }
                }
            }
        }
        geneRnaExonTriples.removeAll(removeSet);
        geneRnaExonTriples.addAll(addSet);

        // build gene-rna-exon relationships
        Map rnaExonMap = new TreeMap();
        for(Iterator iterator = geneRnaExonTriples.iterator();iterator.hasNext();){
            ArrayList triple = (ArrayList) iterator.next();
            String geneFeature = (String) triple.get(0);
            String rnaFeature = (String) triple.get(1);
            String exonFeature = (String) triple.get(2);
            buildGeneRnaExonRelationships(gffTree.getRoot(),
            		rnaExonMap,rnaGeneMap,
            		null,null,
            		geneFeature,rnaFeature,exonFeature, idAttr);
        }

    	// combine overlapping intervals
        for(Iterator iterator = rnaExonMap.keySet().iterator();iterator.hasNext();){
            GffRecord rnaRecord = (GffRecord) iterator.next();
            Set exonSet = (Set) rnaExonMap.get(rnaRecord);
            Interval.combineOverlap(exonSet);
        }
    

        // canonicalize exons -- combine nearby intervals
        for(Iterator iterator = rnaExonMap.keySet().iterator();iterator.hasNext();){
        	GffRecord rnaRecord = (GffRecord) iterator.next();
        	TreeSet exonSet = (TreeSet) rnaExonMap.get(rnaRecord);
        	Interval.combineNearby(exonSet,1);
        }

        // build
        for(Iterator rnaRecIterator = rnaExonMap.keySet().iterator(); rnaRecIterator.hasNext();){
        	GffRecord rnaRecord = (GffRecord) rnaRecIterator.next();
        	
            // get gene information
            String geneID = (String) rnaRecord.getAttrMap().get(idAttr);
            String chrOriginal = rnaRecord.getChr();
            String chr = chrOriginal.toLowerCase().intern();
            chrOriginalMap.put(chr,chrOriginal);
            GenomeInterval geneRegion = new GenomeInterval(chr,
            		rnaRecord.getInterval().getStart(),
            		rnaRecord.getInterval().getStop(),
            		geneID);
            // strand
            geneStrandMap.put(geneID,rnaRecord.getStrand());
            // gene region
            geneRegionMap.put(geneID,geneRegion);
            // insert into corresponding interval tree
            if(chrIntervalTreeMap.containsKey(geneRegion.getChr().toLowerCase())){
                IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr().toLowerCase());
                tree.insert(geneRegion);
            }else{
                IntervalTree tree = new IntervalTree();
                tree.insert(geneRegion);
                chrIntervalTreeMap.put(geneRegion.getChr().toLowerCase(),tree);
            }
            // exon region
            TreeSet exonRanges = new TreeSet();
            geneExonRegionMap.put(geneID,exonRanges);
            int length = 0;
            Set exonSet = (Set) rnaExonMap.get(rnaRecord);
            for(Iterator exonIterator=exonSet.iterator();exonIterator.hasNext();){
            	Interval exon = (Interval) exonIterator.next();
            	length += exon.length();
            	exonRanges.add(exon);
            }
            // length
            geneLengthMap.put(geneID, length);
        }
    }

    /**
     * A constructor based on a given chromosome-interval(set) map and a supporting
     * map. In the chromosome-interval(set) map, every chromosome corresponds to a 
     * set of gene entries, where a gene entry may be represented by a String, an Interval,
     * or a set of Intervals. When it is a String, it means a gene ID and corresponding
     * gene regions were extracted from the supporting map. For every other gene, its gene
     * ID is from first Interval's userObject.
     * @param chrIntervalsMap	the chromosome-interval(set) map.
     * @param srcGeneRegionMap	the supporting map.
     */
    public CanonicalGFF(Map chrIntervalsMap, Map srcGeneRegionMap){

    	TreeSet exonRanges = null;//appended by chun-mao
        for(Iterator chrIterator = chrIntervalsMap.keySet().iterator();chrIterator.hasNext();){
            String chrOriginal = (String) chrIterator.next();
            String chr = chrOriginal.toLowerCase().intern();
            chrOriginalMap.put(chr,chrOriginal);
            Set geneSet = (Set) chrIntervalsMap.get(chrOriginal);
            for(Iterator objIterator = geneSet.iterator();objIterator.hasNext();){
                Object obj = objIterator.next();
                if(obj instanceof String){
                  Object geneID = obj;

                  GenomeInterval geneRegion = (GenomeInterval) srcGeneRegionMap.get(geneID);
                  geneRegionMap.put(geneID, geneRegion);
                  geneExonRegionMap.put(geneID, new TreeSet()); //appended by chun-mao
                  geneLengthMap.put(geneID, 0); // appended by wdlin

                  // insert into corresponding interval tree
                  if (chrIntervalTreeMap.containsKey(geneRegion.getChr())) {
                      IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr());
                      tree.insert(geneRegion);
                  } else {
                      IntervalTree tree = new IntervalTree();
                      tree.insert(geneRegion);
                      chrIntervalTreeMap.put(geneRegion.getChr(), tree);
                  }
                }else if(obj instanceof Interval){
                    Interval interval = (Interval) obj;
                    // preparation
                    Object geneID = interval.getUserObject();
                    GenomeInterval geneRegion = (GenomeInterval) srcGeneRegionMap.get(geneID);
                    geneRegionMap.put(geneID, geneRegion);

                    exonRanges = new TreeSet();
                    exonRanges.add(obj); //appended by chun-mao
                    geneExonRegionMap.put(geneID, exonRanges); //appended by chun-mao
                    geneLengthMap.put(geneID, interval.length()); // appended by wdlin

                    // insert into corresponding interval tree
                    if (chrIntervalTreeMap.containsKey(geneRegion.getChr())) {
                        IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr());
                        tree.insert(geneRegion);
                    } else {
                        IntervalTree tree = new IntervalTree();
                        tree.insert(geneRegion);
                        chrIntervalTreeMap.put(geneRegion.getChr(), tree);
                    }
                }else if(obj instanceof TreeSet){
                    TreeSet intervalSet = (TreeSet) obj;
                    // preparation
                    Interval firstInterval = (Interval) intervalSet.first();
                    Object geneID = firstInterval.getUserObject();

                    GenomeInterval geneRegion = (GenomeInterval) srcGeneRegionMap.get(geneID);
                    geneRegionMap.put(geneID, geneRegion);

                    exonRanges = new TreeSet();
                    exonRanges.addAll((Set)obj);
                    geneExonRegionMap.put(geneID, exonRanges); //appended by chun-mao
                    // for gene length, appended by wdlin
                    int length=0;
                    for(Iterator exonIterator=exonRanges.iterator();exonIterator.hasNext();){
                      Interval exonInterval = (Interval) exonIterator.next();
                      length += exonInterval.length();
                    }
                    geneLengthMap.put(geneID, length); // appended by wdlin

                    // insert into corresponding interval tree
                    if (chrIntervalTreeMap.containsKey(geneRegion.getChr())) {
                        IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr());
                        tree.insert(geneRegion);
                    } else {
                        IntervalTree tree = new IntervalTree();
                        tree.insert(geneRegion);
                        chrIntervalTreeMap.put(geneRegion.getChr(), tree);
                    }
                }
            }
        }
    }

    /**
     * A constructor based on a given chromosome-interval(set) map. In this map,
     * every chromosome corresponds to a set of gene entries, where a gene entry
     * may be represented by an Interval or a set of Intervals. For every gene,
     * its gene ID is from first Interval's userObject.
     * @param chrIntervalsMap	the chromosome-interval(set) map.
     */
    public CanonicalGFF(Map chrIntervalsMap){

    	TreeSet exonRanges = null;//appended by chun-mao
        for(Iterator chrIterator = chrIntervalsMap.keySet().iterator();chrIterator.hasNext();){
            String chrOriginal = (String) chrIterator.next();
            String chr = chrOriginal.toLowerCase().intern();
            chrOriginalMap.put(chr,chrOriginal);
            Set geneSet = (Set) chrIntervalsMap.get(chrOriginal);
            for(Iterator objIterator = geneSet.iterator();objIterator.hasNext();){
                Object obj = objIterator.next();
                if(obj instanceof Interval){
                    Interval interval = (Interval) obj;
                    // preparation
                    Object geneID = interval.getUserObject();
                    GenomeInterval geneRegion = new GenomeInterval(chr,
                            interval.getStart(), interval.getStop(), geneID);
                    geneRegionMap.put(geneID, geneRegion);

                    exonRanges = new TreeSet();
                    exonRanges.add(obj); //appended by chun-mao
                    geneExonRegionMap.put(geneID, exonRanges); //appended by chun-mao
                    geneLengthMap.put(geneID, interval.length()); // appended by wdlin

                    // insert into corresponding interval tree
                    if (chrIntervalTreeMap.containsKey(geneRegion.getChr())) {
                        IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr());
                        tree.insert(geneRegion);
                    } else {
                        IntervalTree tree = new IntervalTree();
                        tree.insert(geneRegion);
                        chrIntervalTreeMap.put(geneRegion.getChr(), tree);
                    }
                }else if(obj instanceof TreeSet){
                    TreeSet intervalSet = (TreeSet) obj;
                    // preparation
                    Interval firstInterval = (Interval) intervalSet.first();
                    Interval lastInterval = (Interval) intervalSet.last();
                    Object geneID = firstInterval.getUserObject();

                    GenomeInterval geneRegion = new GenomeInterval(chr,
                            firstInterval.getStart(), lastInterval.getStop(), geneID);
                    geneRegionMap.put(geneID, geneRegion);

                    exonRanges = new TreeSet();
                    exonRanges.addAll((Set)obj);
                    geneExonRegionMap.put(geneID, exonRanges); //appended by chun-mao
                    // for gene length, appended by wdlin
                    int length=0;
                    for(Iterator exonIterator=exonRanges.iterator();exonIterator.hasNext();){
                      Interval exonInterval = (Interval) exonIterator.next();
                      length += exonInterval.length();
                    }
                    geneLengthMap.put(geneID, length); // appended by wdlin

                    // insert into corresponding interval tree
                    if (chrIntervalTreeMap.containsKey(geneRegion.getChr())) {
                        IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr());
                        tree.insert(geneRegion);
                    } else {
                        IntervalTree tree = new IntervalTree();
                        tree.insert(geneRegion);
                        chrIntervalTreeMap.put(geneRegion.getChr(), tree);
                    }
                }
            }
        }
    }

    /**
     * The constructor that based on a given CGFF file
     * @param filename		The CGFF file.
     */
    public CanonicalGFF(String filename){
        try {
            BufferedReader fr = new BufferedReader(new FileReader(new File(
                    filename)));

            String geneID = null;
            String chr = null;
            String chrOriginal = null;
            GenomeInterval geneRegion = null;
            TreeSet exonRanges = null;
            int length = 0; // for total length of exons

            while(fr.ready()){
                String line = fr.readLine();

                if(line.startsWith("#")) continue;
                
                if(line.startsWith(">")==true){
                    // update geneLengthMap
                    if(geneID!=null){
                        geneLengthMap.put(geneID,new Integer(length));
                        length=0;
                    }
                    // get gene information
                    StringTokenizer st = new StringTokenizer(line.substring(1));
                    geneID = st.nextToken();
                    chrOriginal = st.nextToken().intern();
                    chr = chrOriginal.toLowerCase().intern();
                    chrOriginalMap.put(chr,chrOriginal);
                    geneRegion = new GenomeInterval(chr,Integer.parseInt(st.nextToken()),Integer.parseInt(st.nextToken()),geneID);
                    // if strand is given
                    if(st.hasMoreTokens()){
                        geneStrandMap.put(geneID,st.nextToken());
                    }
                    // insert to map
                    exonRanges = new TreeSet();
                    geneRegionMap.put(geneID,geneRegion);
                    geneExonRegionMap.put(geneID,exonRanges);
                    // insert into corresponding interval tree
                    if(chrIntervalTreeMap.containsKey(geneRegion.getChr().toLowerCase())){
                        IntervalTree tree = (IntervalTree) chrIntervalTreeMap.get(geneRegion.getChr().toLowerCase());
                        tree.insert(geneRegion);
                    }else{
                        IntervalTree tree = new IntervalTree();
                        tree.insert(geneRegion);
                        chrIntervalTreeMap.put(geneRegion.getChr().toLowerCase(),tree);
                    }
                }else{
                    StringTokenizer st = new StringTokenizer(line);
                    int start = Integer.parseInt(st.nextToken());
                    int stop = Integer.parseInt(st.nextToken());
                    exonRanges.add(new Interval(start,stop));
                    length += (stop-start+1);
                }
            }
            // insert last gene length, if any
            if(geneID!=null){
              geneLengthMap.put(geneID, new Integer(length));
            }
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public String getStrand(Object geneID){
        if(geneStrandMap.containsKey(geneID)){
            return (String) geneStrandMap.get(geneID);
        }else{
            return "+";
        }
    }

    public String getChr(Object geneID){
      if(geneRegionMap.containsKey(geneID)){
        GenomeInterval gi = (GenomeInterval) geneRegionMap.get(geneID);
        return gi.getChr();
      }else{
        return null;
      }
    }

    public String getChrOriginal(Object geneID){
      if(geneRegionMap.containsKey(geneID)){
        GenomeInterval gi = (GenomeInterval) geneRegionMap.get(geneID);
        return (String) chrOriginalMap.get(gi.getChr());
      }else{
        return null;
      }
    }
}
