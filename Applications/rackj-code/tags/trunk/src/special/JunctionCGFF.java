package special;

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
import java.util.TreeMap;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.MappingResultIterator;
import misc.Util;


public class JunctionCGFF {

	private static String modelFilename = null;
	private static Map mappingFilenameMethodMap = new LinkedHashMap();
	private static Map mappingMethodMap = null;

	private static int joinFactor = 2;
	private static float identityCutoff = 0.95F;
	private static int minimumBlock = 8;
	private static boolean includeMultiReads = false;
	private static String outputPrefix = null;

	private static int minDepth = 3;
	private static int minSplicingPos = 2;
	private static int minJunctionLen = 10;

	private static int flankingLen = 30;

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
			} else if (args[i].equals("-ID")) {
				identityCutoff = Float.parseFloat(args[i + 1]);
				i++;
			} else if(args[i].equals("-O")){
                          outputPrefix = args[i + 1];
                          i++;
                        } else if (args[i].equals("-minB")) {
				minimumBlock = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-multi")) {
				includeMultiReads = Boolean.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equals("-model")) {
				modelFilename = args[i + 1];
				i++;
			} else if (args[i].equals("-minDepth")) {
				minDepth = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-minSplicingPos")) {
				minSplicingPos = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-minJunctionLen")) {
				minJunctionLen = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-flanking")) {
				flankingLen = Integer.parseInt(args[i + 1]);
				i++;
			}
		}

		// check for necessary parameters
		mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator", System.getProperty("java.class.path"),"misc");
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
				System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
				for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
					System.out.println(iterator.next());
				}
				System.exit(1);
			}
		}
		if(outputPrefix == null){
                  System.err.println("output prefix (-O) not assigned");
                  System.exit(1);
                }

		// list parameters
		System.out.println("program: JunctionCGFF");
		System.out.println("model filename (-model): " + modelFilename);
		System.out.println("mapping method/filename (-M):");
		for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Entry) iterator.next();
			System.out.println("  " + entry.getValue() + " : " + entry.getKey());
		}
		System.out.println("block join factor (-J): " + joinFactor);
		System.out.println("identity cutoff (-ID): " + identityCutoff);
		System.out.println("minimum block (-minB): "+ minimumBlock);
		System.out.println("include multi reads (-multi): " + includeMultiReads);
		System.out.println("minimum depth (-minDepth): " + minDepth);
		System.out.println("minimum splicing entry (-minSplicingPos): " + minSplicingPos);
		System.out.println("minimum junction length (-minJunctionLen): " + minJunctionLen);
		System.out.println("flanking length (-flanking): " + flankingLen);
                System.out.println("output prefix (-O): "+ outputPrefix);
		System.out.println();
	}


	public static void main(String[] args) {
		paraProc(args);

		CanonicalGFF modelCgff = null;
		Set modelSplicingSet = new HashSet();

	    if (modelFilename != null) {
	    	modelCgff = new CanonicalGFF(modelFilename);

	    	for (Iterator iter = modelCgff.geneExonRegionMap.entrySet().iterator(); iter.hasNext();) {
	    		Map.Entry entry = (Map.Entry) iter.next();
				String modelID = (String) entry.getKey();
				TreeSet exonSet = (TreeSet) entry.getValue();
				String chr = modelCgff.getChrOriginal(modelID);

				for (Iterator exonIter = exonSet.iterator(); exonIter.hasNext();) {
					Interval exon = (Interval) exonIter.next();
					Interval nextExon = (Interval) exonSet.higher(exon);
					if (nextExon != null) {
						modelSplicingSet.add(new GenomeInterval(chr, exon.getStop(), nextExon.getStart()));
					}
				}
	    	}
	    }

		Map readSpliceMap = new HashMap();

		ArrayList acceptedRecords;
		for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator();mriIterator.hasNext(); ) {
	        Map.Entry entry = (Map.Entry) mriIterator.next();
	        String mappingFilename = (String) entry.getKey();
	        String mappingMethod = (String) entry.getValue();

	        for(MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod); mappingResultIterator.hasNext();){
	        	ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();

	            // identity
	            if(mappingResultIterator.getBestIdentity()<identityCutoff) continue;

	            // join factor
				acceptedRecords = new ArrayList();
				for(int i=0; i<mappingRecords.size(); i++){
				    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
				    if(record.identity>=mappingResultIterator.getBestIdentity()){
				        // join nearby blocks
				        if(joinFactor>0) record.nearbyJoin(joinFactor);
				        acceptedRecords.add(record);
				    }
				}
				mappingRecords = acceptedRecords;

	            // minimum block
				acceptedRecords = new ArrayList();
	            for(int i=0; i<mappingRecords.size(); i++) {
	                AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
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
	            mappingRecords = acceptedRecords;

	            // uniq reads
	            if (includeMultiReads==false && mappingRecords.size()>1) continue;

	            GenomeInterval readSpliceInterval;
        		Integer readCnt;
    			Map splicingPosFreqMap;
    			ArrayList aList;
	            for(int i=0;i<mappingRecords.size();i++){
	        		AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);

	        		for (int j=0; j<record.numBlocks-1; j++) {
	        			int start = record.tStarts[j]+record.tBlockSizes[j]-1;
	        			int stop = record.tStarts[j+1];

	        			readSpliceInterval = new GenomeInterval(record.chrOriginal, start, stop);

	        			if(modelSplicingSet.contains(readSpliceInterval))
	        				continue;

	        			if(!readSpliceMap.containsKey(readSpliceInterval)){
	        				aList = new ArrayList();
	        				readCnt=0;
	        				splicingPosFreqMap = new HashMap();
	        				aList.add(readCnt);
	        				aList.add(splicingPosFreqMap);
	        				readSpliceMap.put(readSpliceInterval, aList);
	        			}

	        			aList = (ArrayList) readSpliceMap.get(readSpliceInterval);
	        			// read count
	        			readCnt = (Integer) aList.get(0);
	        			aList.set(0, readCnt+1);
	        			// freq
	        			splicingPosFreqMap = (HashMap) aList.get(1);
	        			int splicePos = record.qStarts[j]+record.qBlockSizes[j]-1;
	        			Integer freq = (splicingPosFreqMap.containsKey(splicePos))? (Integer) splicingPosFreqMap.get(splicePos) : 0;
	        			splicingPosFreqMap.put(splicePos, freq+1);
	        		}
	            }


	        }

		}

		// report
		try {
			FileWriter fw = new FileWriter(outputPrefix+".cgff");

			int sn = 1;
                        TreeMap readSpliceMapSorted = new TreeMap(readSpliceMap);
			Iterator iter = readSpliceMapSorted.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				GenomeInterval spliceInterval = (GenomeInterval) entry.getKey();
				ArrayList values = (ArrayList) entry.getValue();

				Integer readCount = (Integer) values.get(0);
				Map freqMap = (HashMap) values.get(1);

				if(readCount>=minDepth && freqMap.size()>=minSplicingPos &&
						(spliceInterval.getStop()-spliceInterval.getStart()-1)>=minJunctionLen){
					fw.write(">splice."+sn+"\t"+
							spliceInterval.getChr()+"\t"+
							(spliceInterval.getStart()-flankingLen+1)+"\t"+
							(spliceInterval.getStop()+flankingLen-1)+"\n"
					);

					fw.write((spliceInterval.getStart()-flankingLen+1)+"\t"+spliceInterval.getStart()+"\n");
					fw.write(spliceInterval.getStop()+"\t"+(spliceInterval.getStop()+flankingLen-1)+"\n");

					sn++;
				}
			}

			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}


	}

}
