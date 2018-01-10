package rnaseq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.IntervalCoverageNode;
import misc.Pair;
import misc.Util;

/**
 * <p> Title: Test04 </p>
 * <P> Description: This class helps user to integrate exon and intron information
 *                  from the output of TestBionUncovering to generate gene region
 *                  Then, use original cgff as reference to adjust generated gene
 *                  region. Finally, output these gene ragion as cgff-like format.
 * @version 1.0
 * @author Chun-Mao Fu
 * @author Wen-Dar Lin
 */

public class TranscriptomeRecover {

	// Map to store exon and intron infomation
	private static Map chromoExonIntervalMap = new TreeMap();
	private static Map chromoRelationMap = new TreeMap();
	private static Map intronInfoMap = new TreeMap();
	private static TreeMap id2icnMap = new TreeMap();
	private static HashMap chrOriginalMap = new HashMap();
	// Input
	private static String gffFilename = null;
	private static String modelFilename = null;
	private static String exonFilename = null;
	private static String intronFilename = null;
	private static String matedPairFilename = null;
	// Output
	private static String outPrefix = null;
	// threshold for coverageArray
	private static int depthCutoff = 2;
	
	private static int intronFilterMode = 1; // 1:byReadSplice, 2:byReadDepth
	private static int minSpliceReadCount = 3;
	private static int minSplicingPos = 2;
	private static int minJunctionLen = 10;
	private static int minSplicingDepth = 0; 
	
	/****************************************************************************/
	/*                   Parameter processing on command line                   */
	/****************************************************************************/
	private static void paraProc(String[] args){
		// get parameter strings
		for(int i=0; i<args.length;i++){
			if(args[i].equals("-GFF")){
				gffFilename = args[i+1];
				i++;
			}else if(args[i].equals("-exon")){
				exonFilename = args[i+1];
				i++;
			}else if(args[i].equals("-intron")){
				intronFilename = args[i+1];
				i++;
			}else if(args[i].equals("-matePair")){
				matedPairFilename = args[i+1];
				i++;
			}else if(args[i].equals("-O")){
				outPrefix = args[i+1];
				i++;
			}else if(args[i].equals("-depth")){
				depthCutoff = Integer.parseInt(args[i+1]);
				i++;
			}else if(args[i].equals("-model")){
				modelFilename = args[i+1];
				i++;
			}else if(args[i].equals("-byReadSplice")){
				intronFilterMode = 1;
				minSpliceReadCount = Integer.parseInt(args[i+1]);
				minSplicingPos = Integer.parseInt(args[i+2]);
				minJunctionLen = Integer.parseInt(args[i+3]);
				i+=3;
			}else if(args[i].equals("-byReadDepth")){
				intronFilterMode = 2;
				minSpliceReadCount = Integer.parseInt(args[i+1]);
				minSplicingDepth = Integer.parseInt(args[i+2]);
				i+=2;
			}
		}
		// check for necessary parameters
		if(gffFilename==null){
			System.out.println("canonical GFF filename (-GFF) not assigned");
			System.exit(1);
		}

		if(exonFilename==null){
			System.out.println("exon filename (-exon) not assigned");
			System.exit(1);
		}

		if(intronFilename==null){
			System.out.println("intron filename (-intron) not assigned");
			System.exit(1);
		}

		if(outPrefix==null){
			System.out.println("outprefix (-O) not assigned");
			System.exit(1);
		}

		if(modelFilename==null){
			System.out.println("model filename (-model) not assigned");
			System.exit(1);
		}

		// list parameters
		System.out.println("program: TranscriptomeRecover");
		System.out.println("canonical GFF filename (-GFF): " + gffFilename);
		System.out.println("exon filename (-exon): " + exonFilename);
		System.out.println("intron filename (-intron): " + intronFilename);
		System.out.println("mated pair filename (-matePair): " + matedPairFilename);
		System.out.println("depth cutoff (-depth): " + depthCutoff);
		System.out.println("outprefix (-O): " + outPrefix);
		System.out.println("model filename (-model): " + modelFilename);
		if(intronFilterMode==1){
			System.out.println("junction cutoff (-byReadSplice): " + minSpliceReadCount + " " + minSplicingPos + " " + minJunctionLen);
		}else if(intronFilterMode==2){
			System.out.println("junction cutoff (-byReadDepth): " + minSpliceReadCount + " " + minSplicingDepth);
		}
		System.out.println();
	}

	private static void readExonInfo(String exonFilename, Map chromoExonIntervalMap){

		try{
			BufferedReader br = new BufferedReader(new FileReader(exonFilename));
			String[] array;
			while(br.ready()){
				String line = br.readLine();
				
				if(line.startsWith("#"))
					continue;
				
				array = line.split("\t");
				// array[0], chromosome name
				String chrOrigin = array[0].intern();
				String chr = chrOrigin.toLowerCase().intern();
				// array[1], serial number
				int serialNumber = Integer.parseInt(array[1]);
				// array[2], start position of exon interval
				int start = Integer.parseInt(array[2]);
				// array[3], stop position of exon interval
				int stop = Integer.parseInt(array[3]);
				// array[4], CoverageArray of exon interval
				StringTokenizer token = new StringTokenizer(array[4],"[, ]");
				int[] coverageArray = new int[token.countTokens()];
				for(int idx=0; idx<coverageArray.length && token.hasMoreTokens(); idx++){
					coverageArray[idx] = Integer.parseInt(token.nextToken());
				}

				// construct an IntervalCoverageNode
				IntervalCoverageNode icn = new IntervalCoverageNode(start,stop,coverageArray);
				icn.setUserObject(serialNumber);

				// build the <id2icnMap> for further use
				ArrayList ali = new ArrayList();
				ali.add(0,icn);
				ali.add(1,chrOrigin);
				id2icnMap.put(serialNumber,ali);

				// chrOriginMap
				chrOriginalMap.put(chr, chrOrigin);

				// store exon info. into <chromoExonIntervalMap>
				if(chromoExonIntervalMap.containsKey(chrOrigin)){
					Set oldExonicSet= (Set)chromoExonIntervalMap.get(chrOrigin);
					oldExonicSet.add(icn);
				}else{
					TreeSet newExonicSet = new TreeSet();
					newExonicSet.add(icn);
					chromoExonIntervalMap.put(chrOrigin,newExonicSet);
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void readIntronInfo(String intronFilename, Map chromoRelationMap, Map intronInfoMap, 
			CanonicalGFF cgff, CanonicalGFF exonCgff) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(intronFilename));
			String[] array;
			int sn=0;
			while(br.ready()){
				String line = br.readLine();
				
				if(line.startsWith("#"))
					continue;
				
				array = line.split("\t");
				// array[0], chromosome name
				String chrOrigin = array[0].intern();
				String chr = chrOrigin.toLowerCase().intern();
				// array[1], start position of intron interval
				int start = Integer.parseInt(array[1]);
				// array[2], stop position of intron interval
				int stop = Integer.parseInt(array[2]);
				// array[3], count of intron interval
				int cnt = Integer.parseInt(array[3]);
				StringTokenizer token = new StringTokenizer(array[4],"{, }");
				Map posMap = new HashMap();
				while (token.hasMoreTokens()) {
					String[] arr = token.nextToken().split("=");
					posMap.put(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
				}

				// filtering by cgff & exonCff, for skipping intron intervals spanning two genes
				// get extended end points through exonCgff
				TreeSet checkSet = new TreeSet(exonCgff.getRelatedGenes(chr,start,stop,false,false,1));
				int checkStart = ((GenomeInterval)checkSet.first()).getStart();
				int checkStop  = ((GenomeInterval)checkSet.last()).getStop();
				// find overlapping genes
				checkSet = new TreeSet(cgff.getRelatedGenes(chr,checkStart,checkStop,false,false,1));
				if(checkSet.size()>=2){
					GenomeInterval gi1 = (GenomeInterval)checkSet.first();
					GenomeInterval gi2 = (GenomeInterval)checkSet.last();
					if(gi1.intersect(gi2)==false) continue;
				}

				// Construct an Interval
				Interval intronInterval = new Interval(start,stop);

				//store intron info. into <chromoIntronIntervalMap>
				if(chromoRelationMap.containsKey(chrOrigin)){
					Map intervalCntMap = (Map)chromoRelationMap.get(chrOrigin);
					if(intervalCntMap.containsKey(intronInterval)){
						int intervalCnt = (Integer)intervalCntMap.get(intronInterval) + cnt;
						intervalCntMap.put(intronInterval,intervalCnt);
					}else{
						intervalCntMap.put(intronInterval, cnt);
					}
				}else{
					Map newIntervalCntMap = new TreeMap();
					newIntervalCntMap.put(intronInterval,cnt);
					chromoRelationMap.put(chrOrigin,newIntervalCntMap);
				}

				// intronInfoMap	
				GenomeInterval junction = new GenomeInterval(chr, start+1, stop-1, ++sn);
				
				ArrayList aList = new ArrayList();
				aList.add(cnt);
				aList.add(posMap);
				
				intronInfoMap.put(junction, aList);
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void readGeneRegionInfo(String gffFilename, Map chromoRelationMap){
		CanonicalGFF cgff = new CanonicalGFF(gffFilename);

		// add gene region in cgff into <chromoRelationMap>
		for(Iterator geneIt=cgff.geneRegionMap.keySet().iterator();geneIt.hasNext();){
			String gene = (String)geneIt.next();
			GenomeInterval genomeI = (GenomeInterval)cgff.geneRegionMap.get(gene);
			Interval geneInterval = new Interval(genomeI.getStart(),genomeI.getStop());
			String chr = cgff.getChrOriginal(gene);

			if(chromoRelationMap.containsKey(chr)){
				Map IntervalCntMap = (Map)chromoRelationMap.get(chr);
				if(IntervalCntMap.containsKey(geneInterval)){
					int intervalCnt = (Integer)IntervalCntMap.get(geneInterval) + 1;
					IntervalCntMap.put(geneInterval,intervalCnt);
				}else{
					IntervalCntMap.put(geneInterval,1);
				}
			}else{
				Map newIntervalCntMap = new TreeMap();
				newIntervalCntMap.put(geneInterval,1);
				chromoRelationMap.put(chr,newIntervalCntMap);
			}
		}

	}

	private static void readMatedPairInfo(String matedPairFilename, Map chromoRelationMap, Map id2icnMap,
			CanonicalGFF cgff){
		try{
			BufferedReader br = new BufferedReader(new FileReader(matedPairFilename));
			String[] array;
			while(br.ready()){
				String line = br.readLine();
				
				if(line.startsWith("#"))
					continue;
				
				array = line.split("\t");

				// array[1], count or support
				int count = Integer.parseInt(array[1]);
				// array[0], intervalCoverageNodes' serial number
				String[] exonPair = ((String)array[0]).split(":");
				int firstID = Integer.parseInt(exonPair[0]);
				int lastID = Integer.parseInt(exonPair[1]);

				ArrayList firstAli = (ArrayList)id2icnMap.get(firstID);
				String chr = (String)firstAli.get(1);
				IntervalCoverageNode firstICN = (IntervalCoverageNode)firstAli.get(0);
				ArrayList lastAli = (ArrayList)id2icnMap.get(lastID);
				IntervalCoverageNode lastICN = (IntervalCoverageNode)lastAli.get(0);

				Interval matedPairInterval = new Interval(firstICN.getStart(),lastICN.getStop());

				// filtering by cgff & exonCff, for skipping mate-pair intervals spanning two genes
				// find overlapping genes
				TreeSet checkSet = new TreeSet(cgff.getRelatedGenes(chr,firstICN.getStart(),lastICN.getStop(),false,false,1));
				if(checkSet.size()>=2){
					GenomeInterval gi1 = (GenomeInterval)checkSet.first();
					GenomeInterval gi2 = (GenomeInterval)checkSet.last();
					if(gi1.intersect(gi2)==false) continue;
				}

				if(chromoRelationMap.containsKey(chr)){
					Map intervalCntMap = (Map)chromoRelationMap.get(chr);
					if(intervalCntMap.containsKey(matedPairInterval)){
						int intervalCnt = (Integer)intervalCntMap.get(matedPairInterval) + count;
						intervalCntMap.put(matedPairInterval,intervalCnt);
					}else{
						intervalCntMap.put(matedPairInterval,count);
					}
				}else{
					Map newIntervalCntMap = new TreeMap();
					newIntervalCntMap.put(matedPairInterval,count);
					chromoRelationMap.put(chr,newIntervalCntMap);
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	/****************************************************************************/
	/*                               Main function                              */
	/****************************************************************************/
	public static void main(String args[]){

		paraProc(args);

		CanonicalGFF cgff = new CanonicalGFF(gffFilename);

		readExonInfo(exonFilename, chromoExonIntervalMap);
		CanonicalGFF exonCgff = new CanonicalGFF(chromoExonIntervalMap);
		readIntronInfo(intronFilename, chromoRelationMap, intronInfoMap, cgff, exonCgff);
		readGeneRegionInfo(gffFilename, chromoRelationMap);

		if(matedPairFilename != null){
			readMatedPairInfo(matedPairFilename,chromoRelationMap,id2icnMap,cgff);
		}

		// get <bionRelatedMap>
		TreeMap bionRelatedMap = getBionRelatedMap(chromoExonIntervalMap,chromoRelationMap,id2icnMap);
		// construct a new cgff
		CanonicalGFF extdCgff = new CanonicalGFF(bionRelatedMap);

		// GER = gene exon range
		// GR  = gene range
		TreeMap destGERMap = new TreeMap();
		TreeMap destGRMap = new TreeMap();
		Map srcGERMap = new HashMap(extdCgff.geneExonRegionMap);
		Map srcGRMap = new HashMap(extdCgff.geneRegionMap);

		// process simplest cases: mapping to none or exactly one known gene
		for(Iterator geneIt=extdCgff.geneRegionMap.keySet().iterator();geneIt.hasNext();){
			int geneID = (Integer)geneIt.next(); // here, geneID is a number
			GenomeInterval genomeI = (GenomeInterval)extdCgff.geneRegionMap.get(geneID);
			TreeSet relatedSet = new TreeSet(cgff.getRelatedGenes(genomeI.getChr(),genomeI.getStart(),genomeI.getStop(),false,false,1));

			if(relatedSet.size()<=1){
				// update <srcGERMap> and <destGERMap>
				TreeSet exonRegions = (TreeSet)srcGERMap.get(geneID);
				destGERMap.put(geneID,exonRegions);
				srcGERMap.remove(geneID);
				// update <srcGRMap> and <destGRMap>
				GenomeInterval geneRegion = (GenomeInterval)srcGRMap.get(geneID);
				destGRMap.put(geneID,geneRegion);
				srcGRMap.remove(geneID);
			}
		}

		// docomposition: mapping to more than one known genes
		System.out.println("Decomposition process");
		while(srcGERMap.size()>0 && srcGRMap.size()>0){
			decomposition(destGERMap, srcGERMap, destGRMap, srcGRMap, cgff);
		}

		// qualification
		System.out.println("Qualification process");
		qualification(destGERMap, destGRMap, depthCutoff);

		// clone destGERMap so that intervals are saved
		Map icnClonedMap = cloneMap(destGRMap, destGERMap);

		// adjustment
		System.out.println("Adjustment process");
		adjustment(destGERMap, destGRMap, cgff); // known genes are included

		// exon-level adjustment
		System.out.println("Intron boundry adjustment process");
		Map chromoIntronIntervalMap = new TreeMap();
		for(Iterator itr=intronInfoMap.keySet().iterator(); itr.hasNext();){
			GenomeInterval gi = (GenomeInterval) itr.next();
			String chr = gi.getChr();
			
			if(!chromoIntronIntervalMap.containsKey(chr))
				chromoIntronIntervalMap.put(chr, new TreeSet());
			
			((Set) chromoIntronIntervalMap.get(chr)).add(gi);
		}
		CanonicalGFF intronInfoCgff = new CanonicalGFF(chromoIntronIntervalMap);
		CanonicalGFF icnExonCgff = new CanonicalGFF(icnClonedMap);
		CanonicalGFF model = new CanonicalGFF(modelFilename);
		boundaryAdjustment(destGERMap, destGRMap, intronInfoMap, intronInfoCgff, model, icnExonCgff);

		// report
		System.out.println("Reporting");
		report(destGERMap, destGRMap, outPrefix, gffFilename);
	}

	private static TreeMap getBionRelatedMap(Map exonMap, Map intronMap, TreeMap id2icnMap){

		TreeMap chromoBionRelationMap = new TreeMap();
		Set giSet = new HashSet();
		//Step1: find exon relationships by given a intron interval, then store them into <chromoBionRelationMap>
		CanonicalGFF exonicCGFF = new CanonicalGFF(exonMap);
		for(Iterator chrIt=intronMap.keySet().iterator();chrIt.hasNext();){
			String chrOrigin = (String)chrIt.next();
			String chr = chrOrigin.toLowerCase().intern();
			Map IntronIntervalCntMap = (Map)intronMap.get(chrOrigin);
			Set intronISet = IntronIntervalCntMap.keySet();

			Set tmpSet = new HashSet();
			Set bionRelatedSet = new HashSet();

			for(Iterator intronIIt = intronISet.iterator();intronIIt.hasNext();){
				Interval intronI = (Interval)intronIIt.next();
				TreeSet relatedExonSet = new TreeSet(exonicCGFF.getRelatedGenes(chr,intronI.getStart(),intronI.getStop(),true,false,1));
				if(relatedExonSet.isEmpty()) continue;

				//for futher use
				TreeSet transSet = new TreeSet();
				for(Iterator giIt=relatedExonSet.iterator();giIt.hasNext();){
					GenomeInterval gi = (GenomeInterval)giIt.next();
					// reocrd mapped geneID
					int exonID = (Integer)gi.getUserObject();
					giSet.add(exonID);
					// transform <GenomeInterval> into <IntervalCoverageNode>
					ArrayList ali = (ArrayList)id2icnMap.get(exonID);
					transSet.add(ali.get(0));
				}
				relatedExonSet = transSet;

				if (tmpSet.isEmpty()) {
					tmpSet.addAll(relatedExonSet);
				}else {
					//check intersection
					boolean intersection = false;
					for (Iterator relatedExonIt = relatedExonSet.iterator();relatedExonIt.hasNext(); ) {
						if (tmpSet.contains(relatedExonIt.next())) {
							intersection = true;
							break;
						}
					}
					//if has intersection
					if (intersection) {
						tmpSet.addAll(relatedExonSet);
					}else {
						TreeSet result = new TreeSet(tmpSet);
						bionRelatedSet.add(result);
						tmpSet = new TreeSet(relatedExonSet);
					}
				}
			}
			// check if <tmpSet> contains something
			if(tmpSet.isEmpty()==false){
				TreeSet res = new TreeSet(tmpSet);
				bionRelatedSet.add(res);
			}

			chromoBionRelationMap.put(chrOrigin,bionRelatedSet);
		}


		//Step2: find exon that unmapped by any intron interval
		//get difference set
		for(Iterator giIt=giSet.iterator();giIt.hasNext();){
			int removeID = (Integer)giIt.next();
			if(id2icnMap.containsKey(removeID)){
				id2icnMap.remove(removeID);
			}
		}
		//store the remaining infor. into <chromoBionRelationMap>
		for(Iterator It=id2icnMap.entrySet().iterator();It.hasNext();){
			Map.Entry pointer = (Map.Entry)It.next();
			ArrayList ali = (ArrayList)pointer.getValue();
			IntervalCoverageNode icn = (IntervalCoverageNode)ali.get(0);
			String chrOrigin = (String)ali.get(1);
			if(chromoBionRelationMap.containsKey(chrOrigin)){
				Set set = (Set)chromoBionRelationMap.get(chrOrigin);
				set.add(icn);
			}else{
				Set set = new HashSet();
				set.add(icn);
				chromoBionRelationMap.put(chrOrigin,set);
			}
		}

		//Step3: return map
		return chromoBionRelationMap;
	}

	private static void decomposition(Map destGERMap, Map srcGERMap, Map destGRMap, Map srcGRMap, CanonicalGFF cgff){

		Set referenceSet = new HashSet(srcGRMap.keySet());

		// iterate each predicted gene in geneRegionMap
		for(Iterator geneIt=referenceSet.iterator();geneIt.hasNext();){
			int geneID = (Integer)geneIt.next();
			GenomeInterval genomeI = (GenomeInterval)srcGRMap.get(geneID);
			TreeSet exonSet = new TreeSet((Set)srcGERMap.get(geneID));
			//Step1: for each gene, explore ranges that not overlap with any gene region in original cgff
			TreeSet rSet = new TreeSet(); // storing gap regions
			boolean inGap = false;
			int bPosition = -1, ePosition = -1;
			for(int idx=genomeI.getStart(); idx<=genomeI.getStop(); idx++){
				Set rltGene= cgff.getRelatedGenes(genomeI.getChr(),idx,idx,false,false,1);
				if(inGap){
					if(rltGene.size()!=0){
						inGap = false;
						ePosition = idx;
						rSet.add(new Interval(bPosition,ePosition));
					}
				}else{
					if(rltGene.size()==0){
						inGap = true;
						bPosition = ((idx-1) >= genomeI.getStart()) ? idx-1 : genomeI.getStart();
					}
				}
			}
			// for the possible ending gap
			if(inGap){
				rSet.add(new Interval(bPosition,genomeI.getStop()));
			}

			//Step2:adjust <rSet>
			Set removeSet1 = new HashSet();
			for(Iterator rIt=rSet.iterator();rIt.hasNext();){
				Interval gapRegion = (Interval)rIt.next();
				if(gapRegion.intersect(genomeI.getStart()) || gapRegion.intersect(genomeI.getStop())){
					removeSet1.add(gapRegion);
				}
			}
			if(removeSet1.isEmpty()==false) rSet.removeAll(removeSet1);

			if(rSet.size()==0){
				destGRMap.put(geneID,genomeI);
				destGERMap.put(geneID,exonSet);
				srcGRMap.remove(geneID);
				srcGERMap.remove(geneID);
			}else{
				Interval gapRegion = (Interval)rSet.first();
				// prepare information for futher use
				TreeSet idSet = new TreeSet(srcGRMap.keySet());
				idSet.addAll(destGRMap.keySet());
				int max_id = (Integer)idSet.last();

				// check if gapRegion is contained by any exon
				boolean contain = false;
				IntervalCoverageNode containingExon = null;
				for(Iterator exonIt=exonSet.iterator();exonIt.hasNext();){
					IntervalCoverageNode exon = (IntervalCoverageNode)exonIt.next();
					if(exon.contain(gapRegion.getStart(),gapRegion.getStop())){
						contain = true;
						containingExon = exon;
						break;
					}
				}

				if(contain){
					int cutPoint = (gapRegion.getStart() + gapRegion.getStop()) / 2;

					//set1 of decomposition
					TreeSet dSet1 = new TreeSet(exonSet.headSet(containingExon,false));
					dSet1.add(new IntervalCoverageNode((IntervalCoverageNode)containingExon,containingExon.getStart(),cutPoint));
					srcGERMap.put(max_id+1,dSet1);
					srcGRMap.put(max_id+1,new GenomeInterval(genomeI.getChr(),
							((IntervalCoverageNode)dSet1.first()).getStart(),
							((IntervalCoverageNode)dSet1.last()).getStop()));
					//set2 of decomposition
					TreeSet dSet2 = new TreeSet(exonSet.tailSet(containingExon,false));
					dSet2.add(new IntervalCoverageNode((IntervalCoverageNode)containingExon,cutPoint+1,containingExon.getStop()));
					srcGERMap.put(max_id+2,dSet2);
					srcGRMap.put(max_id+2,new GenomeInterval(genomeI.getChr(),
							((IntervalCoverageNode)dSet2.first()).getStart(),
							((IntervalCoverageNode)dSet2.last()).getStop()));
					srcGRMap.remove(geneID);
					srcGERMap.remove(geneID);
				}else{
					TreeSet dSet1 = new TreeSet();
					TreeSet dSet2 = new TreeSet();
					for(Iterator exonIt=exonSet.iterator();exonIt.hasNext();){
						IntervalCoverageNode exon = (IntervalCoverageNode)exonIt.next();
						if(exon.getStop() < gapRegion.getStop()){
							dSet1.add(exon);
						}
						if(exon.getStart() > gapRegion.getStart()){
							dSet2.add(exon);
						}
					}

					// update
					srcGERMap.put(max_id + 1,dSet1);
					srcGRMap.put(max_id + 1,new GenomeInterval(genomeI.getChr(),
							((IntervalCoverageNode)dSet1.first()).getStart(),
							((IntervalCoverageNode)dSet1.last()).getStop()));
					srcGERMap.put(max_id + 2,dSet2);
					srcGRMap.put(max_id + 2,new GenomeInterval(genomeI.getChr(),
							((IntervalCoverageNode)dSet2.first()).getStart(),
							((IntervalCoverageNode)dSet2.last()).getStop()));
					srcGRMap.remove(geneID);
					srcGERMap.remove(geneID);
				}
			}
		}
	}

	private static void qualification(Map destGERMap, Map destGRMap, int depthCutoff){
		// remove the novel gene, of which any exon boundary has depth less than <parameter:depthCutoff>
		Set geneIDSet = new HashSet(destGERMap.keySet());
		for(Iterator geneIDIt = geneIDSet.iterator(); geneIDIt.hasNext();){
			int geneID = (Integer)geneIDIt.next(); // here, geneID is just a serial number
			Set exonSet = (Set)destGERMap.get(geneID);
			// start to qualify
			boolean qualified = false;
			for(Iterator exonIt = exonSet.iterator(); exonIt.hasNext();){
				IntervalCoverageNode icn = (IntervalCoverageNode)exonIt.next();
				int[] coverageArray = icn.getCoverageArray();
				for(int idx=0; idx < coverageArray.length; idx++){
					if(coverageArray[idx] > depthCutoff) {
						qualified = true;
						break;
					}
				}
			}
			// if not qualified, add the geneID into <removeSet>
			if(qualified==false){
				destGRMap.remove(geneID);
				destGERMap.remove(geneID);
			}
		}
	}

	private static void adjustment(Map destGERMap, final Map destGRMap, CanonicalGFF cgff){

		TreeMap tmpGeneRegionMap = new TreeMap();
		TreeMap tmpGeneExonRegionMap = new TreeMap();

		//step1: make our predicted region adjusting
		int pred_id_num=1;
		Set referenceSet = new TreeSet(new Comparator(){
			public int compare(Object o1, Object o2) {
				GenomeInterval gr1 = (GenomeInterval) destGRMap.get(o1);
				GenomeInterval gr2 = (GenomeInterval) destGRMap.get(o2);
				return gr1.compareTo(gr2);
			}

			public boolean equals(Object obj) {
				return false;
			}
		});
		referenceSet.addAll(destGRMap.keySet());
		for(Iterator geneIt=referenceSet.iterator();geneIt.hasNext();){
			int geneID = (Integer) geneIt.next(); //-here, geneID is just a number
			GenomeInterval genomeI = (GenomeInterval) destGRMap.get(geneID);
			TreeSet exonSet = (TreeSet)destGERMap.get(geneID);

			TreeSet rltGeneSet = new TreeSet(cgff.getRelatedGenes(genomeI.getChr(),genomeI.getStart(),genomeI.getStop(),false,false,1));

			// adjust <rltGeneSet> if there exists containing event
			if(rltGeneSet.size()>=2){
				Set removeSet = new HashSet();
				ArrayList geneList = new ArrayList(rltGeneSet);
				for(int idx=0; idx<geneList.size()-1; idx++){
					GenomeInterval rGene1 = (GenomeInterval)geneList.get(idx);
					if(removeSet.contains(rGene1)) continue;
					for(int jdx=idx+1; jdx<geneList.size(); jdx++){
						GenomeInterval rGene2 = (GenomeInterval)geneList.get(jdx);
						if(rGene1.contain(rGene2.getStart(),rGene2.getStop())){
							removeSet.add(rGene2);
						}else if(rGene2.contain(rGene1.getStart(),rGene1.getStop())){
							removeSet.add(rGene1);
							break;
						}
					}
				}
				// remove the gene that contained by another
				if(removeSet.size()!=0) rltGeneSet.removeAll(removeSet);
			}

			if(rltGeneSet.size()==0){
				String gName = "NOVEL_" + (pred_id_num++);
				tmpGeneRegionMap.put(gName, genomeI);
				tmpGeneExonRegionMap.put(gName, exonSet);

			}else if(rltGeneSet.size()==1){
				GenomeInterval rltGene = (GenomeInterval)rltGeneSet.iterator().next();
				String rltGeneName = (String)rltGene.getUserObject();
				TreeSet rltGeneExonSet = (TreeSet)cgff.geneExonRegionMap.get(rltGeneName);
				//adjust the front
				Interval pivI1 = (Interval)rltGeneExonSet.first();
				for(Iterator exonIt=exonSet.iterator();exonIt.hasNext();){
					Interval exon = (Interval) exonIt.next();
					if (exon.getStart() < pivI1.getStart()) {
						if (exon.intersect(pivI1) == false) {
							rltGeneExonSet.add(exon);
						}else{
							Interval newI1 = new Interval(exon.getStart(), pivI1.getStop());
							rltGeneExonSet.remove(pivI1);
							rltGeneExonSet.add(newI1);
							break;
						}
					}
				}
				//adjust the rear
				Interval pivI2 = (Interval)rltGeneExonSet.last();
				for(Iterator exonIt=exonSet.descendingIterator();exonIt.hasNext();){
					Interval exon = (Interval) exonIt.next();
					if (exon.getStop() > pivI2.getStop()) {
						if (exon.intersect(pivI2) == false) {
							rltGeneExonSet.add(exon);
						}else {
							Interval newI2 = new Interval(pivI2.getStart(), exon.getStop());
							rltGeneExonSet.remove(pivI2);
							rltGeneExonSet.add(newI2);
							break;
						}
					}
				}
				tmpGeneExonRegionMap.put(rltGeneName, rltGeneExonSet);
				tmpGeneRegionMap.put(rltGeneName, new GenomeInterval(rltGene.getChr(),
						((Interval)rltGeneExonSet.first()).getStart(),
						((Interval)rltGeneExonSet.last()).getStop()));

			}else{
				// pick up the very left gene of <rltGeneSet>
				GenomeInterval vlGene = (GenomeInterval)rltGeneSet.first();
				String vlGeneName = (String)vlGene.getUserObject();
				TreeSet vlGeneExonSet = (TreeSet)cgff.geneExonRegionMap.get(vlGeneName);
				// adjust left
				Interval pivI1 = (Interval)vlGeneExonSet.first();
				for(Iterator exonIt=exonSet.iterator();exonIt.hasNext();){
					Interval exon = (Interval) exonIt.next();
					if (exon.getStart() < pivI1.getStart()) {
						if (exon.intersect(pivI1) == false) {
							vlGeneExonSet.add(exon);
						}else{
							Interval newI1 = new Interval(exon.getStart(), pivI1.getStop());
							vlGeneExonSet.remove(pivI1);
							vlGeneExonSet.add(newI1);
							break;
						}
					}
				}
				tmpGeneExonRegionMap.put(vlGeneName, vlGeneExonSet);
				tmpGeneRegionMap.put(vlGeneName, new GenomeInterval(vlGene.getChr(),
						((Interval)vlGeneExonSet.first()).getStart(),
						((Interval)vlGeneExonSet.last()).getStop()));

				// pick the very right gene of <rltGeneSet>
				GenomeInterval vrGene = (GenomeInterval)rltGeneSet.last();
				String vrGeneName = (String)vrGene.getUserObject();
				TreeSet vrGeneExonSet = (TreeSet)cgff.geneExonRegionMap.get(vrGeneName);
				// adjust right
				Interval pivI2 = (Interval)vrGeneExonSet.last();
				for(Iterator exonIt=exonSet.descendingIterator();exonIt.hasNext();){
					Interval exon = (Interval) exonIt.next();
					if (exon.getStop() > pivI2.getStop()) {
						if (exon.intersect(pivI2) == false) {
							vrGeneExonSet.add(exon);
						}else {
							Interval newI2 = new Interval(pivI2.getStart(), exon.getStop());
							vrGeneExonSet.remove(pivI2);
							vrGeneExonSet.add(newI2);
							break;
						}
					}
				}
				tmpGeneExonRegionMap.put(vrGeneName, vrGeneExonSet);
				tmpGeneRegionMap.put(vrGeneName, new GenomeInterval(vrGene.getChr(),
						((Interval)vrGeneExonSet.first()).getStart(),
						((Interval)vrGeneExonSet.last()).getStop()));
			}
		}
		// update
		destGRMap.clear();
		destGRMap.putAll(tmpGeneRegionMap);
		destGERMap.clear();
		destGERMap.putAll(tmpGeneExonRegionMap);

		//step2: add gene information which isn't included in our extdCgff
		for(Iterator geneIt =cgff.geneRegionMap.keySet().iterator();geneIt.hasNext();){
			String geneID = (String)geneIt.next(); //-here, geneID is a real geneID
			if(destGRMap.containsKey(geneID)==false){
				GenomeInterval addGenomeI = (GenomeInterval)cgff.geneRegionMap.get(geneID);
				Set addExonSet = (Set)cgff.geneExonRegionMap.get(geneID);
				destGRMap.put(geneID,addGenomeI);
				destGERMap.put(geneID,addExonSet);
			}
		}

	}

	private static Map cloneMap(Map srcGRMap, Map srcGERMap){
		Map targetMap = new TreeMap();
		
		for(Iterator itr=srcGRMap.keySet().iterator(); itr.hasNext();){
			int geneID = (Integer) itr.next();
			GenomeInterval gi = (GenomeInterval) srcGRMap.get(geneID);
			String chr = gi.getChr();
			
			if(!targetMap.containsKey(chr))
				targetMap.put(chr, new TreeSet());
			
			Set exonSet = (TreeSet) srcGERMap.get(geneID); 
			((Set) targetMap.get(chr)).addAll(exonSet);
		}
		
		return targetMap;
	}
	
	private static void boundaryAdjustment(Map destGERMap, final Map destGRMap, Map intronInfoMap, 
			CanonicalGFF intronInfoCgff, CanonicalGFF model, CanonicalGFF icnExonCgff) {
		
		Map modelJuncMap = new HashMap();
		Map validJuncMap = new HashMap();

		CanonicalGFF modelIntronCgff = Util.getIntronicCGFF(model);
		for(Iterator geneItr=destGRMap.keySet().iterator(); geneItr.hasNext();){
			String geneName = (String) geneItr.next();
			GenomeInterval gi = (GenomeInterval) destGRMap.get(geneName);
			
			Set rltModelIntrons = modelIntronCgff.getRelatedGenes(gi.getChr(), gi.getStart(), gi.getStop(), true, false, 1);
			
			for(Iterator modelIt=rltModelIntrons.iterator(); modelIt.hasNext();){
				GenomeInterval intronGi = (GenomeInterval) modelIt.next();
				Set intronSet = (TreeSet) modelIntronCgff.geneExonRegionMap.get(intronGi.getUserObject());
				
				if(modelJuncMap.containsKey(geneName)==false)
					modelJuncMap.put(geneName, new TreeSet());
				
				((Set) modelJuncMap.get(geneName)).addAll(intronSet);
			}
		}

		for(Iterator geneIt=destGRMap.keySet().iterator(); geneIt.hasNext();){
			String geneName = (String) geneIt.next();
			GenomeInterval gi = (GenomeInterval) destGRMap.get(geneName);
			
			Set rltJuncSet = intronInfoCgff.getRelatedGenes(gi.getChr(), gi.getStart(), gi.getStop(), true, false, 1);
			if(rltJuncSet.size()==0)
				continue;
			
			Set validJuncSet = validateReadCount(minSpliceReadCount, rltJuncSet, intronInfoMap);
			if(intronFilterMode==1){
				validJuncSet = validateSplicingPos(minSplicingPos, minJunctionLen, validJuncSet, intronInfoMap);
			}else if(intronFilterMode==2){
				validJuncSet = validateDepth(minSplicingDepth, validJuncSet, icnExonCgff);
			}
			
			validJuncMap.put(geneName, validJuncSet);
		}
		
		// gather all valid junctions and those model junctions that are not overlapped with valid 
		for(Iterator itr=modelJuncMap.keySet().iterator(); itr.hasNext();){
			String geneName = (String) itr.next();
			Set modelIntronSet = (Set) modelJuncMap.get(geneName);
			
			if(validJuncMap.containsKey(geneName)==false){
				validJuncMap.put(geneName, modelIntronSet);
				continue;
			}
			
			TreeSet validJuncSet = (TreeSet) validJuncMap.get(geneName);
			
			Set xSet = new HashSet();
			for(Iterator jIt=validJuncSet.iterator(); jIt.hasNext();){
				Interval junction = (Interval) jIt.next();
				
				for(Iterator iIt=modelIntronSet.iterator(); iIt.hasNext();){
					Interval intron = (Interval) iIt.next();
					
					if(junction.intersect(intron))
						xSet.add(intron);
				}
			}
			modelIntronSet.removeAll(xSet);
			
			validJuncSet.addAll(modelIntronSet);
		}
		
		// intron preserving - splicing boundary adjustment
		for(Iterator geneIt=destGRMap.keySet().iterator(); geneIt.hasNext();){
			String geneName = (String) geneIt.next();
			
			if(validJuncMap.containsKey(geneName)==false)
				continue;
			
			GenomeInterval gi = (GenomeInterval) destGRMap.get(geneName);
			TreeSet exonSet = (TreeSet) destGERMap.get(geneName);
			
			Set daSet = new TreeSet();

			daSet.add(new Pair(gi.getStart(), 1)); // 1 for starts
			daSet.add(new Pair(gi.getStop(), 0)); // 0 for stops
			
			TreeSet validJunctions = (TreeSet) validJuncMap.get(geneName);
			for(Iterator vIt=validJunctions.iterator(); vIt.hasNext();){
		    	Interval validJunction = (Interval) vIt.next();
		    	int donorSite = validJunction.getStart()-1;
		    	int acceptorSite = validJunction.getStop()+1;
		    	
		    	if(gi.contain(donorSite, acceptorSite)==false)
		    		continue;
		    	
		    	daSet.add(new Pair(donorSite, 0)); 
            	daSet.add(new Pair(acceptorSite, 1)); 
		    }
			
			// remove near-by stop-start pairs
		    Pair[] daList = (Pair[]) daSet.toArray(new Pair[0]);
		    
		    Set removeSet = new HashSet();
		    for(int i=0; i+1<daList.length; i++){
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

	}
	
	private static Set validateReadCount(int minReadCount, Set srcIntronSet, Map infoMap){
		Set validSet = new TreeSet();

		for(Iterator intervalIt=srcIntronSet.iterator(); intervalIt.hasNext();){
			Interval intronInterval = (Interval) intervalIt.next();
			
			if(infoMap.containsKey(intronInterval)){
				ArrayList aList = (ArrayList) infoMap.get(intronInterval);
				int readCount = (Integer) aList.get(0);
				
				if(readCount < minReadCount)
					continue;
				
				validSet.add(intronInterval);
			}
		}
		
		return validSet;
	}
	
	private static Set validateSplicingPos(int minSplicingPos, int minJunctionLen, Set srcIntronSet, Map infoMap){
		Set validSet = new TreeSet();
		
		for(Iterator intervalIt=srcIntronSet.iterator(); intervalIt.hasNext();){
			Interval intronInterval = (Interval) intervalIt.next();
			
			int junctionLen = intronInterval.getStop() - intronInterval.getStart() + 1;
			if(junctionLen < minJunctionLen)
				continue;
			
			if(infoMap.containsKey(intronInterval)){
				ArrayList aList = (ArrayList) infoMap.get(intronInterval);
				Map posMap = (Map) aList.get(1);
				
				if(posMap.size() < minSplicingPos)
					continue;
				
				validSet.add(intronInterval);
			}
		}
		
		return validSet;
	}
	
	private static Set validateDepth(int minSplicingDepth, Set srcIntronSet, CanonicalGFF icnCgff){
		Set validSet = new TreeSet();
		
		for(Iterator intervalIt=srcIntronSet.iterator(); intervalIt.hasNext();){
			GenomeInterval intronInterval = (GenomeInterval) intervalIt.next();
			int dSite = intronInterval.getStart()-1;
			int aSite = intronInterval.getStop()+1;
			
			TreeSet rltGenes = new TreeSet(icnCgff.getRelatedGenes(
					intronInterval.getChr(), dSite, aSite, true, false, 1));
			
			for(Iterator geneIt=rltGenes.iterator(); geneIt.hasNext();){
				GenomeInterval geneRegion = (GenomeInterval) geneIt.next();
				Set icnExonSet = (Set) icnCgff.geneExonRegionMap.get(geneRegion.getUserObject());			
				List icnExonList = new ArrayList(icnExonSet);
				
				boolean isValid = false;
				for(int i=0; i<icnExonList.size()-1; ++i){
					IntervalCoverageNode icn = (IntervalCoverageNode) icnExonList.get(i);
					IntervalCoverageNode nextIcn = (IntervalCoverageNode) icnExonList.get(i+1);
					if(icn.getStop()!=dSite || nextIcn.getStart()!=aSite)
						continue;
					
					int depth = icn.getCoverageArray()[icn.getCoverageArray().length-1];
					depth += nextIcn.getCoverageArray()[0];
					if((depth/2) >= minSplicingDepth){
						isValid = true;
						break;
					}
				}
				
				if(isValid){
					validSet.add(intronInterval);
					break;
				}
			}

		}
		
		return validSet;
	}
	
	
	private static void report(Map destGERMap, final Map destGRMap, String outPrefix, String gffFilename){

		CanonicalGFF cgff = new CanonicalGFF(gffFilename);
		cgff.chrOriginalMap.putAll(chrOriginalMap);

		// for ordering
		Set referenceSet = new TreeSet(new Comparator(){
			public int compare(Object o1, Object o2) {
				GenomeInterval gr1 = (GenomeInterval) destGRMap.get(o1);
				GenomeInterval gr2 = (GenomeInterval) destGRMap.get(o2);
				return gr1.compareTo(gr2);
			}

			public boolean equals(Object obj) {
				return false;
			}
		});
		referenceSet.addAll(destGRMap.keySet());

		//step1:report extend cgff
		try{
			FileWriter fw = new FileWriter(outPrefix+".extdCGFF");
			// first round original genes only
			for(Iterator geneIt=referenceSet.iterator();geneIt.hasNext();){
				String geneID = (String)geneIt.next();
				if(cgff.geneRegionMap.containsKey(geneID)==false){
					continue;
				}
				GenomeInterval genomeI = (GenomeInterval)destGRMap.get(geneID);
				Set ExonSet = (Set)destGERMap.get(geneID);
				fw.write(">" + geneID + "\t" +
						cgff.chrOriginalMap.get(genomeI.getChr()) + "\t" +
						genomeI.getStart() + "\t" +
						genomeI.getStop() + "\t" +
						cgff.getStrand(geneID) + "\n");
				for(Iterator exonIt=ExonSet.iterator();exonIt.hasNext();){
					Interval exon = (Interval)exonIt.next();
					fw.write(exon.getStart()+"\t"+exon.getStop()+"\n");
				}
			}
			// second round novel genes only
			for(Iterator geneIt=referenceSet.iterator();geneIt.hasNext();){
				String geneID = (String)geneIt.next();
				if(cgff.geneRegionMap.containsKey(geneID)){
					continue;
				}
				GenomeInterval genomeI = (GenomeInterval)destGRMap.get(geneID);
				Set ExonSet = (Set)destGERMap.get(geneID);
				fw.write(">" + geneID + "\t" +
						cgff.chrOriginalMap.get(genomeI.getChr()) + "\t" +
						genomeI.getStart() + "\t" +
						genomeI.getStop() + "\t" +
						cgff.getStrand(geneID) + "\n");
				for(Iterator exonIt=ExonSet.iterator();exonIt.hasNext();){
					Interval exon = (Interval)exonIt.next();
					fw.write(exon.getStart()+"\t"+exon.getStop()+"\n");
				}
			}
			fw.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		//step2:report infomation of extended gene
		try{
			FileWriter fw = new FileWriter(outPrefix+".extdReport");
			//header
			fw.write("GeneID\tSide\tExtended_region\n");
			for(Iterator geneIt=referenceSet.iterator();geneIt.hasNext();){
				String geneID = (String)geneIt.next();
				if(cgff.geneExonRegionMap.containsKey(geneID)){
					TreeSet referenceExonSet = (TreeSet)cgff.geneExonRegionMap.get(geneID);
					TreeSet exonSet = (TreeSet)destGERMap.get(geneID);

					exonSet.removeAll(referenceExonSet);
					if(exonSet.size()<=0) continue;

					Interval reference_firstExon = (Interval)referenceExonSet.first();
					Interval reference_lastExon = (Interval)referenceExonSet.last();

					TreeSet left_extdExonSet = new TreeSet();
					TreeSet right_extdExonSet = new TreeSet();

					for(Iterator exonIt=exonSet.iterator();exonIt.hasNext();){
						Interval exon = (Interval)exonIt.next();
						if(exon.getStart() < reference_firstExon.getStart()){ //left-hand side change
							if(exon.intersect(reference_firstExon)){
								left_extdExonSet.add(new Interval(exon.getStart(),reference_firstExon.getStart()));
							}else{
								left_extdExonSet.add(exon);
							}
						}
						if(exon.getStop() > reference_lastExon.getStop()){ //right-hand side change
							if(exon.intersect(reference_lastExon)){
								right_extdExonSet.add(new Interval(reference_lastExon.getStop(),exon.getStop()));
							}else{
								right_extdExonSet.add(exon);
							}
						}
					}
					//content
					if(left_extdExonSet.size()>0){
						fw.write(geneID+"\tLeft\t");
						fw.write("[");
						for(Iterator exonIt = left_extdExonSet.iterator(); exonIt.hasNext();){
							Interval interval = (Interval)exonIt.next();
							if(exonIt.hasNext()){
								fw.write("(" + interval.getStart() + "," + interval.getStop() + "), ");
							}else{
								fw.write("(" + interval.getStart() + "," + interval.getStop() + ")");
							}
						}
						fw.write("]\n");
					}
					if(right_extdExonSet.size()>0){
						fw.write(geneID+"\tRight\t");
						fw.write("[");
						for(Iterator exonIt = right_extdExonSet.iterator(); exonIt.hasNext();){
							Interval interval = (Interval)exonIt.next();
							if(exonIt.hasNext()){
								fw.write("(" + interval.getStart() + "," + interval.getStop() + "), ");
							}else{
								fw.write("(" + interval.getStart() + "," + interval.getStop() + ")");
							}
						}
						fw.write("]\n");
					}
				}
			}
			fw.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

		//step3:report infomation of novel gene
		try{
			FileWriter fw = new FileWriter(outPrefix+".novelReport");
			//header
			fw.write("GeneID\tChr\tNovel_Region\n");
			for(Iterator geneIdIt=referenceSet.iterator();geneIdIt.hasNext();){
				String geneID = (String)geneIdIt.next();
				if(cgff.geneRegionMap.containsKey(geneID)) continue; // novel genes only

				TreeSet exonSet = (TreeSet)destGERMap.get(geneID);
				GenomeInterval genomeI = (GenomeInterval)destGRMap.get(geneID);
				//content
				fw.write(geneID+"\t"+cgff.chrOriginalMap.get(genomeI.getChr())+"\t");
				fw.write("[");
				for(Iterator exonIt = exonSet.iterator(); exonIt.hasNext();){
					Interval interval = (Interval)exonIt.next();
					if(exonIt.hasNext()){
						fw.write("(" + interval.getStart() + "," + interval.getStop() + "), ");
					}else{
						fw.write("(" + interval.getStart() + "," + interval.getStop() + ")");
					}
				}
				fw.write("]\n");
			}
			fw.close();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

}
