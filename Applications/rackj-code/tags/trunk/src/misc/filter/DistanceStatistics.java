package misc.filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import misc.AlignmentRecord;

public class DistanceStatistics extends FilterInterfacePairReadAdaptor {
	public static String methodName = "PairDistStat";
	
	private String outFilename = null;
	private TreeMap<Integer,Integer> distCntMap = new TreeMap<Integer,Integer>();

	protected ArrayList[][] doFilterInPair(ArrayList readRecordA, ArrayList readRecordB) {
		AlignmentRecord alignmentA = (AlignmentRecord) readRecordA.get(0);
		AlignmentRecord alignmentB = (AlignmentRecord) readRecordB.get(0);
		
		if(readRecordA.size()==1 && readRecordB.size()==1 && alignmentA.chr.equals(alignmentB.chr)){
			// from PairInDistance
			// the definition of distance between two alignments MUST be reconsidered
			int distance = (alignmentA.tStarts[0] - alignmentB.tStarts[0] < 0 ? 
					alignmentB.tStarts[0] - (alignmentA.tStarts[alignmentA.numBlocks - 1] + alignmentA.tBlockSizes[alignmentA.numBlocks - 1] - 1) -1
					: alignmentA.tStarts[0] - (alignmentB.tStarts[alignmentB.numBlocks - 1] + alignmentB.tBlockSizes[alignmentB.numBlocks - 1] - 1) -1);

			if(distCntMap.containsKey(distance)){
				int val = distCntMap.get(distance);
				val++;
				distCntMap.put(distance, val);
			}else{
				distCntMap.put(distance, 1);
			}
		}
		
		ArrayList[][] filteredResults = new ArrayList[2][2];
		ArrayList[] resultA = {readRecordA, new ArrayList()};
		ArrayList[] resultB = {readRecordB, new ArrayList()};
		filteredResults[0] = resultA;
		filteredResults[1] = resultB;
		
		return filteredResults;
	}


	public boolean paraProc(String[] params) throws Exception {
		if(params.length<1){
			throw new Exception("filter method/parameters (-filter): "+methodName+", isn't assigned correctly");
		}else{
			outFilename = params[0];
		}

		return true;
	}

	
	public String reportSetting() {
		String str = methodName + " : " + outFilename;
		
		return str;
	}
	
	public void stop(){
        try {
            FileWriter fw = new FileWriter(new File(outFilename));
            // header
            fw.write("#dist" + "\t" +
                     "#pair" + "\n"
            		);
            // contents
            // for each gene
            for(Iterator iterator = distCntMap.keySet().iterator();iterator.hasNext();){
                int distance = (Integer) iterator.next();
                int cnt = distCntMap.get(distance);
                fw.write( distance + "\t" +
                		  cnt + "\n"
                		);
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
	}
}
