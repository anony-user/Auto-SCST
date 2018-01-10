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
import misc.Interval;
import misc.Pair;

public class IntersectJoin extends FilterInterfaceSingleReadAdaptor {
	public static String methodName = "IntersectJoin";

	private String outFilename = null;
	private float overlapIdentityCutoff = 0.3F;
	private float minPortion = 0.2F;
	private float minLength = 60;
	
	private Map<Pair,LinkedList> contigPairReadMap = new TreeMap<Pair,LinkedList>();

	public ArrayList[] alignmentFilter(ArrayList readRecordList) {
		ArrayList passedList = new ArrayList();
		ArrayList nonPassedList = new ArrayList();

		Map<Interval,Interval> alignmentCoordinateMap = new TreeMap();
		
		// collect alignments as intervals on the query
		int sn=0;
		for(int i=0;i<readRecordList.size();i++){
			AlignmentRecord alignment = (AlignmentRecord) readRecordList.get(i);
			
			// simple filter on alignment length
			if(alignment.getAlignmentLength()<minLength) continue;

			// canonical start/stop of the alignment (query side)
			Interval queryInterval;
			if(alignment.forwardStrand){
				queryInterval = new Interval(alignment.getAlignmentStart(false),alignment.getAlignmentStop(false));
			}else{
				queryInterval = new Interval(alignment.getQueryLength() - alignment.getAlignmentStop(false) +1,
											alignment.getQueryLength() - alignment.getAlignmentStart(false) +1);
			}
			queryInterval.setUserObject(sn);
			sn++;
			
			// canonical coordinate of target to query
			Interval targetInterval;
			if(alignment.forwardStrand){
				targetInterval = new Interval(queryInterval.getStart() - alignment.getAlignmentStart(true) +1,
						queryInterval.getStart() + alignment.getTargetLength() - alignment.getAlignmentStart(true));
			}else{
				targetInterval = new Interval(queryInterval.getStart() - alignment.getTargetLength() + alignment.getAlignmentStop(true),
						queryInterval.getStart() + alignment.getAlignmentStop(true) -1);
			}
			targetInterval.setUserObject(alignment);
			
			alignmentCoordinateMap.put(queryInterval,targetInterval);
		}

		// for every pair of non-overlapping intervals, check them
		for(Iterator iterator1 = alignmentCoordinateMap.keySet().iterator(); iterator1.hasNext();){
			Interval queryInterval2 = (Interval) iterator1.next();
			for(Iterator iterator2 = alignmentCoordinateMap.keySet().iterator(); iterator2.hasNext();){
				Interval queryInterval1 = (Interval) iterator2.next();
				// queryInterval2 AFTER queryInterval1
				
				if(queryInterval1.equals(queryInterval2)) break;
				
				// check
				Interval targetInterval1 = alignmentCoordinateMap.get(queryInterval1);
				Interval targetInterval2 = alignmentCoordinateMap.get(queryInterval2);

				AlignmentRecord alignment1 = (AlignmentRecord) targetInterval1.getUserObject();
				AlignmentRecord alignment2 = (AlignmentRecord) targetInterval2.getUserObject();

				// filter for same sequences
				if(alignment1.chrOriginal.equals(alignment2.chrOriginal))
					continue;
				
				// filter for overlapping
				if(queryInterval1.intersect(queryInterval2,1))
					continue;
				if(targetInterval1.intersect(targetInterval2,1))
					continue;
				
				// filter portion
				int readLen = alignment1.getQueryLength();
				if(queryInterval1.getStop() < readLen*minPortion) continue;
				if(readLen-queryInterval2.getStart()+1 < readLen*minPortion) continue;
				
				// filter overlap identity
				if(alignment1.getAlignmentIdentity()*alignment1.getAlignmentLength()/queryInterval1.getStop()<overlapIdentityCutoff)
					continue;
				if(alignment2.getAlignmentIdentity()*alignment2.getAlignmentLength()/(readLen-queryInterval2.getStart()+1)<overlapIdentityCutoff)
					continue;

				String end1 = (alignment1.forwardStrand)? "3" : "5";
				String end2 = (alignment2.forwardStrand)? "5" : "3";
				
				Pair contigPair;
				ArrayList dataArr = new ArrayList();
				dataArr.add(alignment1.readInfo.getReadID());
				if(alignment1.chrOriginal.compareTo(alignment2.chrOriginal)<0){
					contigPair = new Pair(new Pair(alignment1.chrOriginal,alignment2.chrOriginal),
							  new Pair(end1,end2));
					dataArr.add(new Interval(alignment1.getAlignmentStart(true),
								alignment1.getAlignmentStop(true),
								alignment1.getStrand()));
					dataArr.add(new Interval(alignment1.getAlignmentStart(false),
								alignment1.getAlignmentStop(false)));
					dataArr.add(new Interval(alignment2.getAlignmentStart(true),
								alignment2.getAlignmentStop(true),
								alignment2.getStrand()));
					dataArr.add(new Interval(alignment2.getAlignmentStart(false),
								alignment2.getAlignmentStop(false)));
				}else{
					contigPair = new Pair(new Pair(alignment2.chrOriginal,alignment1.chrOriginal),
							  new Pair(end2,end1));
					dataArr.add(new Interval(alignment2.getAlignmentStart(true),
								alignment2.getAlignmentStop(true),
								alignment2.getStrand()));
					dataArr.add(new Interval(alignment2.getAlignmentStart(false),
								alignment2.getAlignmentStop(false)));
					dataArr.add(new Interval(alignment1.getAlignmentStart(true),
								alignment1.getAlignmentStop(true),
								alignment1.getStrand()));
					dataArr.add(new Interval(alignment1.getAlignmentStart(false),
								alignment1.getAlignmentStop(false)));
				}
				
				if(contigPairReadMap.containsKey(contigPair)==false){
					contigPairReadMap.put(contigPair, new LinkedList());
				}
				contigPairReadMap.get(contigPair).add(dataArr);
			}
		}
		
        ArrayList[] ansArr = new ArrayList[2];
        ansArr[0]=readRecordList;
        ansArr[1]=new ArrayList();
		return ansArr;
	}
	
	public boolean paraProc(String[] params) throws Exception{

        // get parameter strings
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals("-O")) {
            	outFilename = params[i + 1];
                i++;
            } else if (params[i].equals("-oID")) {
            	overlapIdentityCutoff = Float.parseFloat(params[i + 1]);
                i++;
            } else if (params[i].equals("-minP")) {
            	minPortion = Float.parseFloat(params[i + 1]);
                i++;
            } else if (params[i].equals("-minL")) {
            	minLength = Float.parseFloat(params[i + 1]);
                i++;
            }
        }

        if(outFilename==null){
			throw new Exception("(-filter): "+methodName+", output filename (-O) not assigned");
        }

		return true;
	}
	
	public String reportSetting(){
		String str = "";
		str += methodName + " : ";
		str += " -oID "+overlapIdentityCutoff;
		str += " -minP "+minPortion;
		str += " -minL "+minLength;
		str += " -O "+outFilename;
		
		return str;
	}

	public void stop(){
        try {
            FileWriter fw = new FileWriter(new File(outFilename));
            // header
            fw.write("#contigPair" + "\t" +
                     "dataList" + "\n"
            		);
            
            for(Iterator iterator = contigPairReadMap.keySet().iterator(); iterator.hasNext(); ){
            	Pair contigPair = (Pair) iterator.next();
            	List queryList = contigPairReadMap.get(contigPair);
            	
            	fw.write(contigPair + "\t" +
            			 queryList + "\n"
            			);
            	
            }
            
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
	}
}
