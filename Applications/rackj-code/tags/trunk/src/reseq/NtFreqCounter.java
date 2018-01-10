package reseq;

import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import misc.CanonicalGFF;
import misc.FineAlignmentRecord;
import misc.GenomeInterval;
import misc.Interval;
import misc.MappingResultIterator;
import misc.Pair;
import misc.Util;

public class NtFreqCounter {

	// input file
	private static String gffFilename = null;
	private static String polymorphismFile = null;

	// mapping result
	private static Map<String, String> mappingFilenameMethodMap = new LinkedHashMap<String, String>();
	private static Map<String, String> mappingMethodMap = null;
	// output
	private static String outFilename = null;
	// alignment check
    private static float identityCutoff = 0.80F; // OLD default 0.90F
    private static float mappingIdentityCutoff = 0.85F;
	private static int minimumOverlap = 8;

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
			} else if (args[i].equals("-ID")) {
				identityCutoff = Float.parseFloat(args[i + 1]);
				i++;
            } else if (args[i].equals("-mID")) {
                mappingIdentityCutoff = Float.parseFloat(args[i + 1]);
                i++;
			} else if (args[i].equals("-min")) {
				minimumOverlap = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-GFF")) {
				gffFilename = args[i + 1];
				i++;
			} else if (args[i].equals("-poly")) {
				polymorphismFile = args[i + 1];
				i++;
			}
		}

		// check for necessary parameters
		mappingMethodMap = Util.getMethodMap("misc.FineMappingResultIterator", System.getProperty("java.class.path"),
				"misc");
		if (mappingFilenameMethodMap.size() <= 0) {
			System.err.println("mapping method/filename (-M) not assigned, available methods:");
			for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
				System.out.println(iterator.next());
			}
			System.exit(1);
		}
		for (Iterator<String> methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext();) {
			String mappingMethod = methodIterator.next();
			if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
				System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
				for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
					System.out.println(iterator.next());
				}
				System.exit(1);
			}
		}
		if (outFilename == null) {
			System.err.println("output filename (-O) not assigned");
			System.exit(1);
		}
		// check for necessary parameters
		if (polymorphismFile == null) {
			System.err.println("input polymorphismFile (-poly) not assigned");
			System.exit(1);
		}

		// list parameters
		System.out.println("program: NtFreqCounter");
		System.out.println("mapping method/filename (-M):");
		for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Entry) iterator.next();
			System.out.println("  " + entry.getValue() + " : " + entry.getKey());
		}
		System.out.println("canonical GFF filename (-GFF): " + gffFilename);
		System.out.println("identity cutoff (-ID): " + identityCutoff);
        System.out.println("mapping identity cutoff (-mID): " + mappingIdentityCutoff);
		System.out.println("minimum block size (-min): " + minimumOverlap);
		System.out.println("polymorphism file (-poly): " + polymorphismFile);
		System.out.println("output filename (-O): " + outFilename);
		System.out.println();
	}

	private static void getAnnotatedPolymorphism(String filename, String GeneCgffFile, Map<String, HashSet> snpMap,
			Map<String, String> GeneMap, Map<Pair, HashSet> indelMap,
			Map<String, HashSet> siteMap, Map<Interval, HashSet> infoMap) {

		CanonicalGFF GeneCgff = null;
		if (GeneCgffFile != null)
			GeneCgff = new CanonicalGFF(GeneCgffFile);

		try {
			BufferedReader fr = new BufferedReader(new FileReader(filename));
			while (fr.ready()) {
				String line = fr.readLine();
				String[] token = line.split("\\s");

				// SNP pattern
				Pattern pattern1 = Pattern.compile("(.+):(\\d+):([A|C|G|T|N])->([A|C|G|T|N])");
				Matcher matcher1 = pattern1.matcher(token[0]);
				// INDEL pattern
				Pattern pattern2 = Pattern.compile("(.+)\\((\\d+),(\\d+)\\):(\\d+):(\\d+)");
				Matcher matcher2 = pattern2.matcher(token[0]);

				String chr = null;
				String chrOriginal = null;
				int position = 0;
				int position2 = 0;
				char refBase, mutateBase;
				int refGap, readGap;

				String polymorphismString = null;
				// matching, form 1
				if (matcher1.matches()) {
					chrOriginal = matcher1.group(1);
					chr = chrOriginal.toLowerCase();
					position = Integer.parseInt(matcher1.group(2));
					position2 = position;
					refBase = matcher1.group(3).charAt(0);
					mutateBase = matcher1.group(4).charAt(0);
					polymorphismString = chrOriginal + ":" + position + ":" + refBase + "->" + mutateBase;
					
					Interval snpInterval = new Interval(position, position2, polymorphismString);
					if (snpMap.containsKey(chr)) {
						snpMap.get(chr).add(snpInterval);
					} else {
						HashSet<Interval> intervalSet = new HashSet<Interval>();
						intervalSet.add(snpInterval);
						snpMap.put(chr, intervalSet);
					}
				} else if (matcher2.matches()) {
					chrOriginal = matcher2.group(1);
					chr = chrOriginal.toLowerCase();
					position = Integer.parseInt(matcher2.group(2));
					position2 = Integer.parseInt(matcher2.group(3));
					refGap = Integer.parseInt(matcher2.group(4));
					readGap = Integer.parseInt(matcher2.group(5));
					polymorphismString = chrOriginal + "(" + position + "," + position2 + "):" + refGap + ":" + readGap;
					
					// Pair
					Pair indelPair = new Pair(position, position2);
					if(!indelMap.containsKey(indelPair))
						indelMap.put(indelPair, new HashSet());

					indelMap.get(indelPair).add(polymorphismString);
					
					// start/stop site
					if(!siteMap.containsKey(chr))
						siteMap.put(chr, new HashSet());
					
					siteMap.get(chr).add(new Interval(position, position));
					siteMap.get(chr).add(new Interval(position2, position2));
					
					// annotation that shares the same start/stop site
					Interval startSiteInterval = new GenomeInterval(chr,position, position);
					Interval stopSiteInterval = new GenomeInterval(chr,position2, position2);
					
					if(!infoMap.containsKey(startSiteInterval))
						infoMap.put(startSiteInterval, new HashSet());
					
					infoMap.get(startSiteInterval).add(polymorphismString);
					
					if(!infoMap.containsKey(stopSiteInterval))
						infoMap.put(stopSiteInterval, new HashSet());
					
					infoMap.get(stopSiteInterval).add(polymorphismString);
				} else {
					continue;
				}

				String GeneString = new String();
				if (GeneCgffFile != null) {
					Set containingGeneRegions = GeneCgff.getRelatedGenes(chr, position, position2, true, false, 1);
					if (containingGeneRegions.isEmpty() == false) {
						Iterator genesets = containingGeneRegions.iterator();
						while (genesets.hasNext()) {
							GenomeInterval data = (GenomeInterval) genesets.next();
							GeneString += data.getUserObject().toString() + ",";
						}
						GeneString = GeneString.substring(0, GeneString.length() - 1);
					}
				}
				GeneMap.put(polymorphismString, GeneString);
			}
			fr.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	// Main Code
	public static void main(String[] args) {
		paraProc(args);

		Map<String, String> polymorphismHitGeneMap = new LinkedHashMap<String, String>();
		// SNP record 
		Map<String, HashSet> snpIntervalMap = new LinkedHashMap<String, HashSet>();
		// INDEL record
		Map<Pair, HashSet> indelPairMap = new HashMap<Pair, HashSet>();
		Map<String, HashSet> indelSiteMap = new HashMap<String, HashSet>();
		Map<Interval, HashSet> indelSiteInfoMap = new HashMap<Interval, HashSet>();
		
		getAnnotatedPolymorphism(polymorphismFile, gffFilename, snpIntervalMap, polymorphismHitGeneMap,
				indelPairMap, indelSiteMap, indelSiteInfoMap);
		
		// SNP record to cGFF object
		CanonicalGFF snpCgff = new CanonicalGFF(snpIntervalMap);
		
		// INDEL record to cGFF object
		CanonicalGFF IndelSiteCgff = new CanonicalGFF(indelSiteMap);

		// For SNP base A,T,C,G,N count
		Map<Object, int[]> snpFreqMap = new LinkedHashMap<Object, int[]>();

		// For read count
		Map<Object, Integer> polymorphismCoverageMap = new LinkedHashMap<Object, Integer>();

		// For code A,T,C,G,N mapping to index
		Map<Character, Integer> codeMAP = new HashMap<Character, Integer>();
		codeMAP.put('A', 0);
		codeMAP.put('T', 1);
		codeMAP.put('G', 2);
		codeMAP.put('C', 3);
		codeMAP.put('N', 4);

		// For indel count
		Map<Object, Integer> indelCntMap = new HashMap<Object, Integer>();

		// each Iterator loop for each input BLAT/SAM file
		for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext();) {
			Map.Entry entry = (Entry) mriIterator.next();
			String mappingFilename = (String) entry.getKey();
			String mappingMethod = (String) entry.getValue();
			// iterate alignments in the file
			int mappedReadCnt = 0;
			int processedLines = 0;

			for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap,
					mappingMethod); mappingResultIterator.hasNext();) {
				ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
				processedLines += mappingRecords.size();

                // select best alignments
				ArrayList acceptedRecords = new ArrayList();
				for (int i = 0; i < mappingRecords.size(); i++) {
					FineAlignmentRecord record = (FineAlignmentRecord) mappingRecords.get(i);
					if (record.identity >= mappingResultIterator.getBestIdentity()) {
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
				if (minBlockSize < minimumOverlap)
					continue;

				mappedReadCnt++;

				// SNP
				Set snpSet = snpCgff.getRelatedGenes(record.chr, record.getMappingIntervals(), true, false, 1, false);
				for (Iterator iterator = snpSet.iterator(); iterator.hasNext();) {
					GenomeInterval snpInterval = (GenomeInterval) iterator.next();

					// UserObject-> Object contains SNPString
					Object SNPkey = snpInterval.getUserObject();

					// Retrieve SNP base
					Character x = record.getMappedBaseAt(snpInterval.getStart());

					// IF SNP base exist, save result to codeHitMap &
					// readHitMap
					if (x != null) {
						int idx = codeMAP.get(x);
						if (snpFreqMap.containsKey(SNPkey)) {
							int[] CodeArray = snpFreqMap.get(SNPkey);
							CodeArray[idx]++;
							snpFreqMap.put(SNPkey, CodeArray);
							polymorphismCoverageMap.put(SNPkey, polymorphismCoverageMap.get(SNPkey) + 1);
						} else {
							int[] CodeArray = { 0, 0, 0, 0, 0 };
							CodeArray[idx]++;
							snpFreqMap.put(SNPkey, CodeArray);
							polymorphismCoverageMap.put(SNPkey, 1);
						}
					}
				}
				
				// for reads with exactly the same INDEL
				Set recordIndelSet = record.getIndels();
				for(Iterator rIndelItr = recordIndelSet.iterator(); rIndelItr.hasNext();){
					GenomeInterval recordIndel = (GenomeInterval) rIndelItr.next();
					
					Pair recordIndelPair = new Pair(recordIndel.getStart(), recordIndel.getStop());
					
					if(indelPairMap.containsKey(recordIndelPair)==false)
						continue;
					
					Set dbIndelSet = (HashSet) indelPairMap.get(recordIndelPair);
					String INDELstr = recordIndel.toString();
					
					if(dbIndelSet.contains(INDELstr)){
						if (indelCntMap.containsKey(INDELstr)) {
							indelCntMap.put(INDELstr, indelCntMap.get(INDELstr) + 1);
						} else {
							indelCntMap.put(INDELstr, 1);
						}
					}
				}
				
				// INDEL coverage
				Set indelSiteSet = IndelSiteCgff.getRelatedGenes(record.chr, record.getMappingIntervals(), false, false, 1, false);
				for (Iterator siteItr = indelSiteSet.iterator(); siteItr.hasNext();) {
					GenomeInterval siteInterval = (GenomeInterval) siteItr.next();
					
					if(indelSiteInfoMap.containsKey(siteInterval)==false)
						continue;
					
					Set INDELkeySet = indelSiteInfoMap.get(siteInterval);
					for(Iterator keyItr = INDELkeySet.iterator(); keyItr.hasNext();){
						Object INDELKey = keyItr.next();
						
						if (polymorphismCoverageMap.containsKey(INDELKey)) {
							polymorphismCoverageMap.put(INDELKey, polymorphismCoverageMap.get(INDELKey) + 1);
						} else {
							polymorphismCoverageMap.put(INDELKey, 1);
						}
					}
				}
			}
			System.out.println(mappedReadCnt + " mapped uniq-reads (" + processedLines + " lines) in "
					+ mappingFilename);
		}
		// step 2: write file output according to "polymorphismHitGeneMap"
		try {
			FileWriter fw = new FileWriter(outFilename);
			fw.write("#PolymorphismString" + "\t" + "HitGenes" + "\t" + "Reads" + "\t" + "Freq(A)" + "\t" + "Freq(T)" + "\t"
					+ "Freq(G)" + "\t" + "Freq(C)" + "\t" + "Freq(N)" + "\n");

			for (Iterator mapIterator = polymorphismHitGeneMap.entrySet().iterator(); mapIterator.hasNext();) {
				Map.Entry record = (Entry) mapIterator.next();
				String polymorphismStr = record.getKey().toString();
				String HitGenes = record.getValue().toString();
				int hitReads = 0;
				if (polymorphismCoverageMap.containsKey(record.getKey())) {
					hitReads = polymorphismCoverageMap.get(record.getKey());
				}

				String freqString = "0\t0\t0\t0\t0";
				if (snpFreqMap.containsKey(record.getKey())) {
					int freqs[] = snpFreqMap.get(record.getKey());
					freqString = Integer.toString(freqs[0]) + "\t" + Integer.toString(freqs[1]) + "\t"
							+ Integer.toString(freqs[2]) + "\t" + Integer.toString(freqs[3]) + "\t"
							+ Integer.toString(freqs[4]);
				}

				int indelFreq = 0;
				if(indelCntMap.containsKey(record.getKey())){
					indelFreq = indelCntMap.get(record.getKey());
					freqString = indelFreq+"\t0\t0\t0\t0";
				}

				fw.write(polymorphismStr + "\t" + HitGenes + "\t" + hitReads + "\t" + freqString + "\n");
			}
			fw.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
