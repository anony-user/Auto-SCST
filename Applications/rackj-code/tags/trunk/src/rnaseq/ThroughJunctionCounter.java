package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.Interval;
import misc.Util;

public class ThroughJunctionCounter implements ReadCounter {

	private static String gffFilename = null;
	private static Map mappingFilenameMethodMap = new LinkedHashMap();
	private static Map mappingMethodMap = null;

	private static String outputPrefix = null;

	private static int joinFactor = 2;
	private static float identityCutoff = 0.95F;
	private static boolean useExonRegion = false;
	private static boolean checkByContaining = false;
	private static int minimumOverlap = 8;
	private static boolean checkAllBlocks = true;
    private static boolean careDirection = false;

	static private void paraProc(String[] args) {
		int i;

		// get parameter strings
		for (i = 0; i < args.length; i++) {
			if (args[i].equals("-GFF")) {
				gffFilename = args[i + 1];
				i++;
			} else if (args[i].equals("-M")) {
				mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
				i += 2;
			} else if (args[i].equals("-J")) {
				joinFactor = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-ID")) {
				identityCutoff = Float.parseFloat(args[i + 1]);
				i++;
			} else if (args[i].equals("-O")) {
				outputPrefix = args[i + 1];
				i++;
			} else if (args[i].equals("-exon")) {
				useExonRegion = Boolean.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equals("-contain")) {
				checkByContaining = Boolean.valueOf(args[i + 1]);
				i++;
			} else if (args[i].equals("-min")) {
				minimumOverlap = Integer.parseInt(args[i + 1]);
				i++;
			} else if (args[i].equals("-ALL")) {
				checkAllBlocks = Boolean.valueOf(args[i + 1]);
				i++;
			} else if(args[i].equals("-direction")){
                careDirection = Boolean.valueOf(args[i+1]);
                i++;
            }

		}

		// check for necessary parameters
		if (gffFilename == null) {
			System.err.println("canonical GFF filename (-GFF) not assigned");
			System.exit(1);
		}
		mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",
				System.getProperty("java.class.path"), "misc");
		if (mappingFilenameMethodMap.size() <= 0) {
			System.err
					.println("mapping method/filename (-M) not assigned, available methods:");
			for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator
					.hasNext();) {
				System.out.println(iterator.next());
			}
			System.exit(1);
		}
		for (Iterator methodIterator = mappingFilenameMethodMap.values()
				.iterator(); methodIterator.hasNext();) {
			String mappingMethod = (String) methodIterator.next();
			if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
				System.err.println("assigned mapping method (-M) not exists: "
						+ mappingMethod + ", available methods:");
				for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator
						.hasNext();) {
					System.out.println(iterator.next());
				}
				System.exit(1);
			}

		}
		if (outputPrefix == null) {
			System.err.println("output prefix (-O) not assigned");
			System.exit(1);
		}
		if (minimumOverlap < 1) {
			System.err.println("minimum block size (-min) less than 1");
			System.exit(1);
		}
		// post-processing

		// list parameters
		System.out.println("program: ThroughJunctionCounter");
		System.out.println("canonical GFF filename (-GFF): " + gffFilename);
		System.out.println("mapping method/filename (-M):");
		for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Entry) iterator.next();
			System.out.println("  " + entry.getValue() + " : " + entry.getKey());
		}
		System.out.println("output prefix (-O): " + outputPrefix);
		System.out.println("block join factor (-J): " + joinFactor);
		System.out.println("identity cutoff (-ID): " + identityCutoff);
		System.out.println("use exon region (-exon): " + useExonRegion);
		System.out.println("check by containing (-contain, FALSE for by intersecting): " + checkByContaining);
		System.out.println("minimum overlap (-min): " + minimumOverlap);
		System.out.println("check all alignment blocks (-ALL): " + checkAllBlocks);
        System.out.println("care mapping direction (-direction): "+careDirection);
		System.out.println();
	}

	private Map geneExonCntMapUniq = new HashMap();

	public ThroughJunctionCounter(boolean checkByContaining, int minimumOverlap) {
		this.checkByContaining = checkByContaining;
		this.minimumOverlap = minimumOverlap;
	}

	private boolean terminalCounting(AlignmentRecord record,
			Map geneExonCntMap, Number count, String geneID, CanonicalGFF cgff,
			boolean checkByContaining, int minimumOverlap) {

		Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
		// get hit exons
		HashSet hitExonPos = new HashSet();
		for (Iterator exonIterator = exonRegions.iterator(); exonIterator
				.hasNext();) {
			Interval exonInterval = (Interval) exonIterator.next();
			for (Iterator blockIterator = record.getMappingIntervals()
					.iterator(); blockIterator.hasNext();) {
				Interval blockInterval = (Interval) blockIterator.next();
				// check intersection
				if (exonInterval.intersect(blockInterval) == false)
					continue;
				// check terminal intersection
				boolean threeChk = false, fiveChk = false;
				if (blockInterval.intersect(exonInterval.getStart())
                                                && blockInterval.intersect(exonInterval.getStart(),exonInterval.getStop(),minimumOverlap)
						&& blockInterval.getStart() < exonInterval.getStart()
                                              ) {
					hitExonPos.add(exonInterval.getStart());
					fiveChk = true;
				}

				if (blockInterval.intersect(exonInterval.getStop())
						&& blockInterval.intersect(exonInterval.getStart(),exonInterval.getStop(),minimumOverlap)
                                                && blockInterval.getStop() > exonInterval.getStop()
                                              ) {
					hitExonPos.add(exonInterval.getStop());
					threeChk = true;
				}

				// use intron_interval for read count
				if (fiveChk || threeChk)
					hitExonPos.add(exonInterval);
			}
		}

		if (hitExonPos.size() == 0)
			return false;

		// add counts for these hit exon terminals
		Map exonPosCntMap;

		if (geneExonCntMap.containsKey(geneID)) {
			exonPosCntMap = (Map) geneExonCntMap.get(geneID);
		} else {
			exonPosCntMap = new HashMap();
			geneExonCntMap.put(geneID, exonPosCntMap);
		}

		for (Iterator exonIterator = hitExonPos.iterator(); exonIterator
				.hasNext();) {
			Object position = exonIterator.next();
			if (exonPosCntMap.containsKey(position)) {
				int oldValue = (Integer) (exonPosCntMap.get(position));
				exonPosCntMap.put(position, (oldValue + (Integer) count));
			} else {
				exonPosCntMap.put(position, count);
			}
		}
		return true;
	}

	public void countReadUnique(String readID, AlignmentRecord record,
			Number cnt, String geneID, CanonicalGFF cgff) {
		terminalCounting(record, geneExonCntMapUniq, cnt, geneID, cgff,
				checkByContaining, minimumOverlap);
	}

	public void countReadMulti(String readID, Collection recordCollection,
			Number cnt, String geneID, CanonicalGFF cgff) {
	}

	public void report(String filename, CanonicalGFF cgff) {
		try {
			FileWriter fw = new FileWriter(new File(filename));
			// header
			fw.write("#GeneID" + "\t" + "intron" + "\t" + "length" + "\t" + "5' Junction reads"
					+ "\t" + "3' Junction reads" + "\t" + "#reads" + "\t"
					+ "format:.throughJunctionCount" + "\n");

			// contents
			// for each gene
			for (Iterator iterator = cgff.geneLengthMap.keySet().iterator(); iterator
					.hasNext();) {
				String geneID = (String) iterator.next();
				Map uniqReadCntMap = (Map) geneExonCntMapUniq.get(geneID);

				// iterate exons for this gene
				Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
				int exonNo = 1;

				for (Iterator exonIterator = exonRegions.iterator(); exonIterator.hasNext();) {
					Interval exonInterval = (Interval) exonIterator.next();
					int fiveUniqCnt = 0;
					if (uniqReadCntMap != null && uniqReadCntMap.containsKey(exonInterval.getStart()))
						fiveUniqCnt = (Integer) uniqReadCntMap.get(exonInterval.getStart());

					int threeUniqCnt = 0;
					if (uniqReadCntMap != null && uniqReadCntMap.containsKey(exonInterval.getStop()))
						threeUniqCnt = (Integer) uniqReadCntMap.get(exonInterval.getStop());

					int readCount = 0;
					if (uniqReadCntMap != null && uniqReadCntMap.containsKey(exonInterval))
						readCount = (Integer) uniqReadCntMap.get(exonInterval);

					fw.write(geneID + "\t");
					fw.write(exonNo + "\t");
					fw.write(exonInterval.length() + "\t");
					fw.write(fiveUniqCnt + "\t");
					fw.write(threeUniqCnt + "\t");
					fw.write(String.valueOf(readCount));
					fw.write("\n");
					exonNo++;
				}
			}
			fw.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String args[]) {
		paraProc(args);

		CanonicalGFF cgff = new CanonicalGFF(gffFilename);
		cgff = Util.getIntronicCGFF(cgff, true);

		Map geneUniqReadCntMap = new HashMap();

		// read counters
		final ThroughJunctionCounter spliceJunctionCounter = new ThroughJunctionCounter(
				checkByContaining, minimumOverlap);

		ReadCounter readCounter = new ReadCounter() { // wrap ReadCounters
			public void countReadUnique(String readID, AlignmentRecord record,
					Number cnt, String geneID, CanonicalGFF cgff) {
				spliceJunctionCounter.countReadUnique(readID, record, cnt,
						geneID, cgff);
			}

			public void countReadMulti(String readID,
					Collection recordCollection, Number cnt, String geneID,
					CanonicalGFF cgff) {
				spliceJunctionCounter.countReadMulti(readID, recordCollection,
						cnt, geneID, cgff);
			}

			public void report(String filename, CanonicalGFF cgff) {
				spliceJunctionCounter.report(filename + ".throughJunctionCount", cgff);
			}

		};

		// computing based on unique reads
		int uniqReadCnt = 0;
		Set nonUniqReads = new HashSet();
		for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Entry) iterator.next();
			String mappingFilename = (String) entry.getKey();
			String mappingMethod = (String) entry.getValue();
			UniqueReadIterator uniqueRI = new UniqueReadIterator(
					Util.getMRIinstance(mappingFilename,mappingMethodMap,mappingMethod),
					identityCutoff, 
					joinFactor,
					useExonRegion, 
					checkByContaining, 
					minimumOverlap,
					checkAllBlocks,
					careDirection,
					cgff, 
					geneUniqReadCntMap);
			uniqueRI.iterate(readCounter);
			uniqReadCnt += uniqueRI.uniqReadCnt;
		}
		System.out.println("#uniq reads: " + uniqReadCnt);

		// output
		readCounter.report(outputPrefix, cgff);
	}
}

