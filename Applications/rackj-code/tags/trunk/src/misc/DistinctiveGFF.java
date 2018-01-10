package misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class parses a given GFF3 file,  
 *  fetch genome data by features, 
 *  then computes the number of distinctive junctions 
 *  as well as the total length of distinctive regions among isoforms.
 * The result will output to a file. 
 * 
 * @author yungilin
 *
 */
public class DistinctiveGFF {
	static private String inFilename = null;
	static private String outFilename = null;
	static private Set<List<String>> geneExonFeatureTriples = new LinkedHashSet<List<String>>();
	static private String idAttr = null;
	static private ArrayList<String> parentAttrs = new ArrayList<String>();

	static private GffTree gff3=null;
	static private HashMap<GffRecord,HashMap<GffRecord,TreeSet<Interval>>> relationMap = new HashMap<GffRecord,HashMap<GffRecord,TreeSet<Interval>>>();
	static private HashMap<String,Object[]> resultMap = new HashMap<String,Object[]>();


	/**
	 * Process command-line arguments
	 * @param args
	 */
	public static void parseArguments(String[] args){
		int i=0;

		// get parameter strings
		while(i<args.length){
			if (args[i].equals("-I")) {
				inFilename = args[i+1];
				i+=2;
			}
			else if(args[i].equals("-O")){
				outFilename = args[i+1];
				i+=2;
			}
			else if(args[i].equals("-GME")){
				List<String> list = new ArrayList<String>();
				list.add(args[i+1]);
				list.add(args[i+2]);
				list.add(args[i+3]);
				geneExonFeatureTriples.add(list);
				i+=4;
			}
			else if(args[i].equals("-idAttr")){
				idAttr = args[i+1];
				i+=2;
			}
			else if(args[i].equals("-parentAttr")){
				String[] attrs = args[i+1].split(System.getProperty("path.separator"));
				parentAttrs = (ArrayList<String>) Arrays.asList(attrs);
				i+=2;
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
		if(geneExonFeatureTriples.size()==0){
			List<String> list = new ArrayList<String>();
			list.add("gene");
			list.add("mRNA");
			list.add("exon");
			geneExonFeatureTriples.add(list);
			System.out.println("gene-rna-exon feature triple (-GME) not assigned, using default: " + geneExonFeatureTriples);
		}

		// list parameters
		System.out.println("program: DistinctiveGFF");
		System.out.println("input GFF file (-I): "+inFilename);
		System.out.println("output filename (-O): "+outFilename);
		System.out.println("gene-rna-exon feature triple (-GME): "+geneExonFeatureTriples);
		System.out.println("ID attribute (-idAttr): "+idAttr);
		System.out.println("parent attribute list (-parentAttr)): "+parentAttrs);
		System.out.println();
	}



	/**
	 * Rearrange the feature triples
	 */
	public static void parseFeaturetriples(){	
		Set<List<String>> removeSet = new HashSet<List<String>>();
		Set<List<String>> addSet = new LinkedHashSet<List<String>>();
		String pathSeparator = System.getProperty("path.separator");
		for(Iterator iterator = geneExonFeatureTriples.iterator();iterator.hasNext();){
			ArrayList<String> triple = (ArrayList<String>) iterator.next();
			String geneFeature = (String) triple.get(0);
			String rnaFeature = (String) triple.get(1);
			String exonFeature = (String) triple.get(2);
			if(geneFeature.indexOf(pathSeparator) > 0 
					|| rnaFeature.indexOf(pathSeparator) > 0
					|| exonFeature.indexOf(pathSeparator) > 0){
				// remove
				removeSet.add(triple);
				// re-generate
				ArrayList<String> newTriple;
				String[] gene = geneFeature.split(pathSeparator);
				String[] rna = rnaFeature.split(pathSeparator);
				String[] exon = exonFeature.split(pathSeparator);
				for(int i=0;i<gene.length;i++){
					for(int j=0;j<rna.length;j++){
						for(int k=0;k<exon.length;k++){
							newTriple = new ArrayList<String>();
							newTriple.add(gene[i]);
							newTriple.add(rna[j]);
							newTriple.add(exon[k]);
							addSet.add(newTriple);
						}
					}
				}

			}
		}
		geneExonFeatureTriples.removeAll(removeSet);
		geneExonFeatureTriples.addAll(addSet);
	}



	/**
	 * Traverses GffTree and extracts the features-binding nodes 
	 *  ,stores into the relationMap
	 */
	public static void treeExtraction(){
		DefaultMutableTreeNode root = gff3.getRoot();
		Iterator featureIterator = geneExonFeatureTriples.iterator();
		while(featureIterator.hasNext()){
			ArrayList<String> features = (ArrayList<String>) featureIterator.next();
			String grandfather = features.get(0);
			String father = features.get(1);
			String son = features.get(2);

			recursiveTraversal(root,null,grandfather,father,son);
		}
	}

	// grandfather - father 
	public static void recursiveTraversal(DefaultMutableTreeNode node, GffRecord obj, String grandpa, String father, String son){
		GffRecord record = (GffRecord) node.getUserObject();
		
		if(record.getFeature().equals(grandpa)){
			obj=record;
			relationMap.put(obj, new HashMap<GffRecord,TreeSet<Interval>>());
		}else if(obj!=null && record.getFeature().equals(father)){
			HashMap<GffRecord,TreeSet<Interval>> map = (HashMap) relationMap.get(obj);	
			TreeSet<Interval> set=null;
			if(node.getChildCount()>0){ // if sons exist
				set = new TreeSet<Interval>();

				recursiveTraversal(node, set, son);
			}
			map.put(record, set);
		}

		for(int i=0;i<node.getChildCount();i++){
			recursiveTraversal((DefaultMutableTreeNode)node.getChildAt(i), obj, grandpa, father, son);
		}
	}
	

	// father - son 
	public static void recursiveTraversal(DefaultMutableTreeNode node, TreeSet<Interval> set, String sonTag){
		GffRecord record = (GffRecord) node.getUserObject();
		if(record.getFeature().equals(sonTag)){
			set.add(record.getInterval());
		}

		for(int i=0;i<node.getChildCount();i++){
			recursiveTraversal((DefaultMutableTreeNode)node.getChildAt(i), set, sonTag);
		}
	}


	/**
	 * Computes the number of uniq junctions and the total length of uniq regions, 
	 * and then stores numbers into the resultMap
	 * @return
	 */
	public static void findDistinctive(){ 
		TreeMap<Interval,ArrayList<GffRecord>> junctionMap;
		TreeMap<Interval,ArrayList<GffRecord>> regionMap;

		Iterator relationMapIterator = relationMap.entrySet().iterator();
		while (relationMapIterator.hasNext()) {
			Map.Entry unit = (Map.Entry)relationMapIterator.next();
			GffRecord grandparent = (GffRecord) unit.getKey();
			String gpid = getValueByAttribute(grandparent,idAttr); // grandparent id
			HashMap<GffRecord,TreeSet<Interval>> parentMap = (HashMap) unit.getValue();
			if(parentMap.size()==0) continue;

			junctionMap = new TreeMap<Interval,ArrayList<GffRecord>>();
			regionMap = new TreeMap<Interval,ArrayList<GffRecord>>();

			Iterator parentMapIterator = parentMap.entrySet().iterator();
			while (parentMapIterator.hasNext()) {
				Map.Entry subUnit = (Map.Entry)parentMapIterator.next();
				GffRecord parent = (GffRecord) subUnit.getKey(); 
				String pid = getValueByAttribute(parent,idAttr); // parent id
				TreeSet<Interval> son = (TreeSet<Interval>) subUnit.getValue(); // son

				ArrayList<GffRecord> list;
				Interval prev=null;
				for(Interval I : son){
					// calculates gap then stores into junction map
					if(prev!=null){
						if(I.compareTo(prev)!=0){
							Interval gap = new Interval(prev.getStop()+1, I.getStart()-1); // gap
							if(junctionMap.containsKey(gap)){
								list = junctionMap.get(gap);
								list.add(parent);
							}else{
								list = new ArrayList<GffRecord>();
								list.add(parent);
								junctionMap.put(gap, list);
							}
						}
					}
					prev = I;


					// stores into region map
					if(regionMap.containsKey(I)){
						list = regionMap.get(I);
						list.add(parent);
					}else{
						list = new ArrayList<GffRecord>();
						list.add(parent);
						regionMap.put(I, list);
					}

				}

				// set default values into resultMap
				Object[] objAry = new Object[]{gpid,pid,new Integer(0),new Integer(0)};
				resultMap.put(pid, objAry);

			}
			
			
			// finding junctions
			if(junctionMap.size()>0){
				Iterator junctionMapIterator = junctionMap.entrySet().iterator();
				while (junctionMapIterator.hasNext()) {
					Map.Entry entry = (Map.Entry)junctionMapIterator.next();
					ArrayList<GffRecord> list = (ArrayList<GffRecord>) entry.getValue();
					if(list.size()==1) {
						GffRecord record = list.get(0);
						String pID = getValueByAttribute(record, idAttr);
						Integer oldJunctionValue = (Integer) resultMap.get(pID)[2];
						resultMap.get(pID)[2] = new Integer(oldJunctionValue.intValue()+1);
					}
				}
			}
			
		
			// finding regions
			if(regionMap.size()>0){
				Iterator regionMapIterator = regionMap.entrySet().iterator();
				while (regionMapIterator.hasNext()) {
					Map.Entry entry = (Map.Entry)regionMapIterator.next();
					Interval region = (Interval) entry.getKey(); 
					ArrayList<GffRecord> list = (ArrayList<GffRecord>) entry.getValue();
					if(list.size()==1) { 
						GffRecord myFather = list.get(0);
						String pID = getValueByAttribute(myFather, idAttr);
						int regionLength = regionCompare(region, myFather, parentMap);
						if(regionLength>0){
							Integer oldRegionValue = (Integer) resultMap.get(pID)[3];
							resultMap.get(pID)[3] = new Integer(oldRegionValue.intValue()+regionLength);
						}
					}
				}
			}


		}


	}


	/**
	 * To search for all intervals in the exon level except my siblings 
	 *  ,then distinguish the unique regions among them
	 * @param me, parent, map
	 * @return
	 */
	private static int regionCompare(Interval me, GffRecord parent, HashMap<GffRecord,TreeSet<Interval>> map){
		int sum=0;
		TreeSet<Interval> competitors = new TreeSet<Interval>(); 

		Iterator mapIterator = map.entrySet().iterator();
		while (mapIterator.hasNext()) {
			Map.Entry entry = (Map.Entry)mapIterator.next();
			GffRecord record = (GffRecord) entry.getKey();
			TreeSet<Interval> values = (TreeSet<Interval>) entry.getValue();

			if(record.compareTo(parent)!=0){ // different parent 
				competitors.addAll(values);	
			}
		}

		sum = exclusiveRegionLength(me, competitors);

		return sum;
	}

	/**
	 * Finds all distinctive regions and gets a summation
	 * @param me, set
	 * @return
	 */
	private static int exclusiveRegionLength(Interval me, TreeSet<Interval> set){
		int length=0;

		boolean isOverlap=false;
		boolean isIntersetAtStart=false;
		boolean isIntersectAtStop=false;
		TreeSet<Interval> overlapee = new TreeSet<Interval>();

		for(Interval other: set){
			// inclusion check 
			if(other.contain(me.getStart(), me.getStop())){
				return 0;
			}

			// overlapping check 
			boolean overlap = other.overlaps(me);
			if(overlap){
				overlapee.add(other);
			}
			isOverlap = (isOverlap || overlap);

			// lowest and highest check
			isIntersetAtStart = ( isIntersetAtStart || other.intersect(me.getStart()) );
			isIntersectAtStop = ( isIntersectAtStop || other.intersect(me.getStop()) );
		}

		// no one overlaps me
		if(isOverlap==false){
			length += me.length(); 
		}

		if(overlapee.size()>0){
			if(isIntersetAtStart==false){
				overlapee.add(new Interval(me.getStart()-2, me.getStart()-1)); // sets lower-bound 
			}
			if(isIntersectAtStop==false){
				overlapee.add(new Interval(me.getStop()+1, me.getStop()+2)); // sets upper-bound 
			}
			
			// merge overlapee
			Interval.combineOverlap(overlapee);
			if(overlapee.size()==1){
				return 0;
			}
			
			int count=0;
			Interval prev=null;
			for(Interval region: overlapee){
				if(prev!=null){
					count = (region.getStart() - prev.getStop() -1);
					length += count;
				}
				prev=region;
			}

		}

		return length;
	}
	
	
	/**
	 * Gets an attribute value 
	 * @param record, attr
	 * @return 
	 */
	private static String getValueByAttribute(GffRecord record, String attr){
		Map attrMap = record.getAttrMap();
		String value = null;
		if(attrMap.containsKey(attr)){
			value = (String) attrMap.get(attr);
		}
		return value;
	}



	/**
	 * Writes all findings to a file
	 *  sorts data by the 1st, then the 2nd element in the Object array
	 * @param outfile, result
	 */
	public static void writeToFile(String outfile){
		BufferedWriter bufferwriter = null;
		try {
			bufferwriter = new BufferedWriter(new FileWriter(outfile));
			StringBuilder sb = new StringBuilder();
			sb.append("#geneID\ttransID\t# of uniq junction\tsum of uniq region\n");

			// sort by values
			Map<String,Object[]> sortedResultMap = sortResultMapComparator(resultMap);
			for (Map.Entry resultMapEntry : sortedResultMap.entrySet()) {
				Object[] values = (Object[]) resultMapEntry.getValue();
				sb.append(join(values,"\t"));
				sb.append("\n");
	        }

			bufferwriter.write(sb.toString());	
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferwriter != null){
				try {
					bufferwriter.flush();
					bufferwriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	
	/**
	 * To sort a Map based on its values
	 */
	private static Map sortResultMapComparator(Map unsortMap){
		ArrayList list = new ArrayList(unsortMap.entrySet());
		
		Collections.sort(list, new Comparator(){
			public int compare(Object obj1, Object obj2) {
				Object[] ary1 = (Object[]) ((Map.Entry)obj1).getValue();
				Object[] ary2 = (Object[]) ((Map.Entry)obj2).getValue();
				String gp1 = (String) ary1[0]; // grand father id
				String gp2 = (String) ary2[0];
				String p1 = (String) ary1[1]; // father id
				String p2 = (String) ary2[1];
				
				int c = gp1.compareTo(gp2);
				if(c!=0){
					return c;
				}else{
					return p1.compareTo(p2);
				}
			}	
		});
		
		Map sortMap = new LinkedHashMap();
		Iterator listIterator = list.iterator();
		while (listIterator.hasNext()) {
			Map.Entry entry = (Map.Entry) listIterator.next();
			sortMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortMap;
	}


	/**
	 * Joins object array with delimiter
	 */
	public static String join (Object[] array, String delimiter) {
		StringBuilder sb=new StringBuilder();
		int i = array.length;
		for(Object o: array){
			i--;
			sb.append(o);
			if(i>0) sb.append(delimiter);
		}
		return sb.toString();
	}



	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		parseArguments(args);
		parseFeaturetriples();
		
		gff3 = new GffTree(inFilename, idAttr, parentAttrs);
		treeExtraction();
		findDistinctive();
		writeToFile(outFilename);
	}


}
