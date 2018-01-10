package misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import misc.filter.FilterInterface;
import net.sf.samtools.SAMRecord;

public class AlignmentFilter2 {
	// qualification parameters
	private static List filterParaLists = new LinkedList();
	private static Map filterMethodMap = null;
	private static List reportingFilterList = new LinkedList();
	private static List filterInstanceList = new LinkedList();
	private static Object checkMatePair = null;
	// mapping result
	private static Map mappingFilenameMethodMap = new LinkedHashMap();
	private static Map mappingMethodMap = null;
	// output
	private static String passedFilename = null;
	private static String nonPassedFilename = null;
	
	private static boolean isProcessNonPassed = false;
	private static boolean outAllNonPassed = false;

	// misc
	private static boolean quiet = false;
	
	private static void paraProc(String[] args){
		
		List currentFilterParaList = null;
		boolean filterSection = false;
		
		for(int i=0;i<args.length;i++){

			if(filterSection == false){
				if (args[i].equals("-M")) {
					mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
					i += 2;
				}else if (args[i].equals("-O")) {
					passedFilename=args[i+1];
					i++;
				}else if (args[i].equals("-Un")) {
					nonPassedFilename=args[i+1];
					isProcessNonPassed=true;
					i++;
				} else if (args[i].equals("-UnALL")) {
					outAllNonPassed = Boolean.valueOf(args[i + 1]);
					i++;
				}else if (args[i].equals("-mate")) {
					try{
						checkMatePair = Integer.valueOf(args[i+1]);
					}catch(NumberFormatException ex){
						checkMatePair = args[i+1];
					}
					i++;
				} else if (args[i].equals("-quiet")) {
					quiet = Boolean.valueOf(args[i + 1]);
					i++;
				}else if (args[i].equals("-filter")) {
					filterSection = true;
				}
			}
			
			if(filterSection == true){
				if (args[i].equals("-filter")) {
					currentFilterParaList = new LinkedList();
					filterParaLists.add(currentFilterParaList);
					i++;
				}
				currentFilterParaList.add(args[i]);
			}
		}
		
		//
		mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",System.getProperty("java.class.path"), "misc");
		if(mappingFilenameMethodMap.size()==0){
			System.err.println("mapping method/filename (-M) isn't assigned, available methods:");
			for(Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
				System.out.println(iterator.next());
			}
			System.exit(1);
		}

		for(Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext();){
			String mappingMethod = (String) methodIterator.next();
			if(mappingMethodMap.keySet().contains(mappingMethod) == false){
				System.err.println("assigned mapping method (-M) doesn't exists: "+ mappingMethod + ", available methods:");
				for(Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();){
					System.out.println(iterator.next());
				}
				System.exit(1);
			}
		}
		
		filterMethodMap = Util.getMethodMap("misc.filter.FilterInterface",System.getProperty("java.class.path"),"misc");
		if (filterParaLists.size()==0) {
			System.err.println("filter method/parameters (-filter) isn't assigned, available methods:");
			for(Iterator fItr = filterMethodMap.keySet().iterator(); fItr.hasNext();) {
				System.out.println(fItr.next());
			}
			System.exit(1);
		}
		
		for(Iterator fplIterator = filterParaLists.iterator(); fplIterator.hasNext();){
			String filterMethod = (String) ((LinkedList)fplIterator.next()).getFirst();
			if(filterMethodMap.keySet().contains(filterMethod) == false){
				System.err.println("assigned filter method (-filter) doesn't exists: "+ filterMethod + ", available methods:");
				for(Iterator fItr = filterMethodMap.keySet().iterator(); fItr.hasNext();){
					System.out.println(fItr.next());
				}
				System.exit(1);
			}
		}

		try {
			for(Iterator fplIterator = filterParaLists.iterator(); fplIterator.hasNext();){
				LinkedList filterParameters = (LinkedList) fplIterator.next();
				String filterName = (String) filterParameters.removeFirst();
				String[] filterParas = (String[]) filterParameters.toArray(new String[filterParameters.size()]);
				FilterInterface fi = Util.getFilterInstance(filterName,filterMethodMap);
				boolean enabled = fi.paraProc(filterParas);
				if(enabled){
					fi.setProcessNonPassed(isProcessNonPassed);
					filterInstanceList.add(fi);
				}
				reportingFilterList.add(fi);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		if(passedFilename==null){
			System.err.println("output filename (-O) isn't assigned");
			System.exit(1);
		}
		
		//
	    if(quiet) return;

	    System.out.println("program: AlignmentFilter2");
	    System.out.println("mapping method/filename (-M):");
	    for(Iterator mItr = mappingFilenameMethodMap.entrySet().iterator(); mItr.hasNext();){
			Map.Entry entry = (Map.Entry) mItr.next();
			System.out.println("  " + entry.getValue() + " : " + entry.getKey());
	    }
        System.out.println("check mate-pair (-mate): "+checkMatePair);
	    System.out.println("filter method/parameters (-filter):");
	    for(Iterator fItr=reportingFilterList.iterator(); fItr.hasNext();){
	    	FilterInterface filter = (FilterInterface) fItr.next();
	    	System.out.println(" " + filter.reportSetting());
	    }
	    System.out.println("output passed filename (-O): " + passedFilename);
	    System.out.println("output non-passed filename (-Un): " + nonPassedFilename);
	    System.out.println("output all non-passed records (-UnALL): " + outAllNonPassed);
	    System.out.println();
	}
	
	public static void main(String[] args) {

		paraProc(args);

		try {
			FileWriter passedFw = new FileWriter(new File(passedFilename));
			FileWriter nonPassedFw = null;
			if (isProcessNonPassed)
				nonPassedFw = new FileWriter(new File(nonPassedFilename));
			
			int numTotalRead=0;
			int numPassedRead=0;
			int numNonPassedRead=0;
			int passedReadCnt=0; // counts passed reads of CURRENT file

			IdPairedChecker pairChecker = ReadInfo.getIdPairedChecker(checkMatePair);
			for (PairedBatchIterator pbiIt = new PairedBatchIterator(mappingFilenameMethodMap, mappingMethodMap); pbiIt.hasNextBatch();) {
				ArrayList batchList = pbiIt.nextBatch(pairChecker);
				
				numTotalRead += batchList.size();

				ArrayList[][] tmpFilteredResult = new ArrayList[batchList.size()][2];
				for(int i=0;i<batchList.size();i++){
					tmpFilteredResult[i][0]=(ArrayList) batchList.get(i);
					tmpFilteredResult[i][1]=new ArrayList();
				}
				// filter
				for (Iterator instanceItr = filterInstanceList.iterator(); instanceItr.hasNext();) {
					FilterInterface fi = (FilterInterface) instanceItr.next();

					// prepare input for a filter
					ArrayList[] batchRecords = new ArrayList[batchList.size()];
					for(int i=0;i<batchList.size();i++){
						batchRecords[i] = tmpFilteredResult[i][0];
					}
					
					// filtering
					ArrayList[][] filteredResult = fi.alignmentFilter(batchRecords);
					
					// save filtered results
					for (int i = 0; i < batchList.size(); i++) {
						tmpFilteredResult[i][0] = filteredResult[i][0];
						if(filteredResult[i][1]!=null){
							tmpFilteredResult[i][1].addAll(filteredResult[i][1]);
						}
					}
				}
				// output
				int curBatchPassedReadCnt=0;
				for (int j = 0; j < batchList.size(); j++) {
					ArrayList passedRecords = tmpFilteredResult[j][0];
					// all records not passed, output first only
					if(passedRecords==null || passedRecords.size()==0){
						if(isProcessNonPassed){
							ArrayList nonPassedRecords = tmpFilteredResult[j][1];
							// current MappingResultIterator gives only read with at least one alignment,
							// so there must be at least one alignment in this case
							int outNum = 1;
							if(outAllNonPassed) outNum = nonPassedRecords.size();
							for(int k=0;k<outNum;k++){
								AlignmentRecord record = (AlignmentRecord) nonPassedRecords.get(k);
								record.reset();
								if (record.sourceObj != null && record.sourceObj instanceof SAMRecord) {
									SAMRecord samRecord = (SAMRecord) record.sourceObj;
									nonPassedFw.write(samRecord.getSAMString().trim()+ "\n");
								} else {
									nonPassedFw.write(record.toBLAT(AlignmentRecord.ATTACH_QUERY_SEQ_YES) + "\n");
								}
							}
							
							numNonPassedRead++;
						}
					}else{ // passed, output all
						for (Iterator recordItr = passedRecords.iterator(); recordItr.hasNext();) {
							AlignmentRecord curRecord = (AlignmentRecord) recordItr.next();
							curRecord.reset();
							if (curRecord.sourceObj != null && curRecord.sourceObj instanceof SAMRecord) {
								SAMRecord samRecord = (SAMRecord) curRecord.sourceObj;
								passedFw.write(samRecord.getSAMString().trim()+ "\n");
							} else {
								passedFw.write(curRecord.toBLAT(AlignmentRecord.ATTACH_QUERY_SEQ_YES) + "\n");
							}
						}
						
						numPassedRead++;
						curBatchPassedReadCnt++;
					}
				}

				if(pbiIt.meetFileEnd()){
					int idx = 0; // the index for the batch
					for (Iterator itr = pbiIt.fileInfo.keySet().iterator(); itr.hasNext();) { // iteration of ended files
						String mappingFilename = (String) itr.next();
						int[] inforArr = (int[]) pbiIt.fileInfo.get(mappingFilename);
						
						for(int counter=0;counter<inforArr[2];counter++){ // if the file contributes any reads
							ArrayList passedRecords = (ArrayList) tmpFilteredResult[idx][0];
							if (passedRecords != null && passedRecords.size() > 0) { // update passed read count
								passedReadCnt++;
							}
							idx++; // batch read iteration
						}
						if(!quiet){
							System.out.println(inforArr[0] + " mapped reads (" + inforArr[1] + " lines) in " + mappingFilename + ", passed: " + passedReadCnt);
						}
						passedReadCnt = 0;
					}
					// 
					passedReadCnt = 0;
					curBatchPassedReadCnt = 0;
					for (; idx < batchList.size(); idx++) {
						ArrayList alnRecords = tmpFilteredResult[idx][0];
						if (alnRecords != null && alnRecords.size() > 0) {
							curBatchPassedReadCnt++;
						}
					}
				}
	
				passedReadCnt+=curBatchPassedReadCnt;
				
			}

			passedFw.close();
			if (nonPassedFw != null) nonPassedFw.close();
			if(!quiet){
				System.out.println("#total read: " + numTotalRead);
				System.out.println("#passed read: " + numPassedRead);
				if(isProcessNonPassed) System.out.println("#non-passed read: " + numNonPassedRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// calling filters' stop function
		for (Iterator instanceItr = filterInstanceList.iterator(); instanceItr.hasNext();) {
			FilterInterface fi = (FilterInterface) instanceItr.next();

			fi.stop();
		}
	}
}
