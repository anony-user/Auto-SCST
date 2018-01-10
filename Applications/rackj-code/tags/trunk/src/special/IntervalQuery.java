package special;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import misc.Interval;
import misc.intervaltree.IntervalTree;



public class IntervalQuery {

	public static Map seqIntervalTreeMap = new LinkedHashMap();
	
	// insert
	public static void insert(String seqID,int start,int end){
		
		Interval seqInterval = new Interval(start,end);

		if(seqIntervalTreeMap.containsKey(seqID)){
		    IntervalTree tree = (IntervalTree) seqIntervalTreeMap.get(seqID);
		    tree.insert(seqInterval);
		}else{
		    IntervalTree tree = new IntervalTree();
		    tree.insert(seqInterval);
		    seqIntervalTreeMap.put(seqID,tree);
		}
	}
	
	// query
	public static Set getRelatedRegions(String seqId, int start, int stop) {
		Set ansSet = new LinkedHashSet();
		
		if(seqIntervalTreeMap.containsKey(seqId)){
			IntervalTree tree = (IntervalTree) seqIntervalTreeMap.get(seqId);
			ansSet.addAll(tree.searchAll(new Interval(start,stop)));
		}

		return ansSet;
	}
	
	// report
	public static void report(Set hitSet, Writer fw) {
		try{
			if(hitSet.size()>0){
				for(Iterator iterator=hitSet.iterator();iterator.hasNext();){
					Interval interval = (Interval) iterator.next();
					fw.write(interval.getStart() + "\t" + interval.getStop());
					if(iterator.hasNext()){
						fw.write(",");
					}
				}
			}
			fw.write("\n");
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		Writer fw = new BufferedWriter(new OutputStreamWriter(System.out));
		BufferedReader fr = new BufferedReader(new InputStreamReader(System.in));
		try {
			String line;
			while ( (line = fr.readLine()) != null) {
				String[] tokens = line.split("\\s");
				String command = tokens[0];
				String seqID = tokens[1];
				int start = Integer.parseInt(tokens[2]);
				int end = Integer.parseInt(tokens[3]);
				
				if(command.equals("insert")){
					// do insert
					insert(seqID, start, end);
				}else if(command.equals("query")){
					// do query & report
					Set hitSet = getRelatedRegions(seqID, start, end);
					report(hitSet, fw);
				}
			}
			fw.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
