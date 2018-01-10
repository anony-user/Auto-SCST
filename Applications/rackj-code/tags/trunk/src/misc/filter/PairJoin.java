package misc.filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import misc.AlignmentRecord;
import misc.Pair;

public class PairJoin extends FilterInterfacePairReadAdaptor {
	public static String methodName = "PairJoin";
	
	private String outFilename = null;
	private int border = 150;

	private Map<Pair,LinkedList> contigPairBorderMap = new TreeMap<Pair,LinkedList>();
	
	protected ArrayList[][] doFilterInPair(ArrayList readRecordA, ArrayList readRecordB) {
		AlignmentRecord alignmentA = (AlignmentRecord) readRecordA.get(0);
		AlignmentRecord alignmentB = (AlignmentRecord) readRecordB.get(0);
		
		Pair borderCheckA = borderCheck(alignmentA,border);
		Pair borderCheckB = borderCheck(alignmentB,border);
		
		if(readRecordA.size()==1 && readRecordB.size()==1 && alignmentA.chr.equals(alignmentB.chr)==false &&
				((Integer)borderCheckA.key2)>=0 && ((Integer)borderCheckB.key2)>=0){
			Pair contigPair,distPair;
			
			if(alignmentA.chrOriginal.compareTo(alignmentB.chrOriginal)<0){
				contigPair = new Pair(new Pair(alignmentA.chrOriginal,alignmentB.chrOriginal),
									  new Pair(borderCheckA.key1,borderCheckB.key1));
				distPair = new Pair(borderCheckA.key2,borderCheckB.key2);
			}else{
				contigPair = new Pair(new Pair(alignmentB.chrOriginal,alignmentA.chrOriginal),
									  new Pair(borderCheckB.key1,borderCheckA.key1));
				distPair = new Pair(borderCheckB.key2,borderCheckA.key2);
			}
			
			if(contigPairBorderMap.containsKey(contigPair)==false){
				contigPairBorderMap.put(contigPair, new LinkedList());
			}
			contigPairBorderMap.get(contigPair).add(distPair);
		}
		
		ArrayList[][] filteredResults = new ArrayList[2][2];
		ArrayList[] resultA = {readRecordA, new ArrayList()};
		ArrayList[] resultB = {readRecordB, new ArrayList()};
		filteredResults[0] = resultA;
		filteredResults[1] = resultB;
		
		return filteredResults;
	}

	private Pair borderCheck(AlignmentRecord alignment,int border){
		// 5'
		if(1<=alignment.getAlignmentStart(true) && 
				alignment.getAlignmentStop(true)<=border &&
				alignment.forwardStrand==false){
			return new Pair("5",alignment.getAlignmentStart(true)-1);
		}
		
		// 3'
		if(alignment.getTargetLength()-border+1<=alignment.getAlignmentStart(true) && 
				alignment.getAlignmentStop(true)<=alignment.getTargetLength() &&
				alignment.forwardStrand==true){
			return new Pair("3",alignment.getTargetLength()-alignment.getAlignmentStop(true));
		}

		return new Pair("0",-1);
	}

	public boolean paraProc(String[] params) throws Exception {

        // get parameter strings
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals("-O")) {
            	outFilename = params[i + 1];
                i++;
            } else if (params[i].equals("-B")) {
            	border = Integer.parseInt(params[i + 1]);
                i++;
            }
        }

        if(outFilename==null){
			throw new Exception("(-filter): "+methodName+", output filename (-O) not assigned");
        }

		return true;
	}

	
	public String reportSetting() {
		String str = "";
		str += methodName + " : ";
		str += " -B "+border;
		str += " -O "+outFilename;
		
		return str;
	}
	
	public void stop(){
        try {
            FileWriter fw = new FileWriter(new File(outFilename));
            // header
            fw.write("#contigPair" + "\t" +
                     "#pair" + "\t" +
                     "distPairList" + "\n"
            		);
            
            for(Iterator iterator = contigPairBorderMap.keySet().iterator(); iterator.hasNext(); ){
            	Pair contigPair = (Pair) iterator.next();
            	List distPairList = contigPairBorderMap.get(contigPair);
            	
            	fw.write(contigPair + "\t" +
            			 distPairList.size() + "\t" +
            			 distPairList + "\n"
            			);
            	
            }
            
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
	}
}
