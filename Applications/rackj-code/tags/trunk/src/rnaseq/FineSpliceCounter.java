package rnaseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.AlignmentRecord;
import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.Pair;
import misc.Util;
import misc.intervaltree.IntervalTree;

public class FineSpliceCounter implements ReadCounter {

    private class SpliceLocation implements Comparable{

        private int exon1,position1,exon2,position2;

        SpliceLocation(int exon1,int position1,int exon2,int position2){
            this.exon1 = exon1; this.exon2 = exon2; this.position1 = position1; this.position2 = position2;
        }

        public int rank(){
            int ans = 0;
            if(exon1>0) ans++;
            if(exon2>0) ans++;
            return ans;
        }

        public String toString(){
            if(exon1==exon2){
                if(exon1==0){
                    return position1 + "-" + position2;
                }else{
                    return exon1 + "[" + position1 + "-" + position2 + "]";
                }
            }else{
                String ansStr = "";
                if(exon1>0){
                    ansStr += ( exon1 + "(" );
                    if(position1>0){
                        ansStr += ( "+" + position1 );
                    }else if(position1<0){
                        ansStr += ( "" + position1 );
                    }else{
                        ansStr += ( "0" );
                    }
                    ansStr += ")";
                }else{
                    ansStr += position1;
                }
                ansStr += "-";
                if(exon2>0){
                    ansStr += ( exon2 + "(" );
                    if(position2>0){
                        ansStr += ( "+" + position2 );
                    }else if(position2<0){
                        ansStr += ( "" + position2 );
                    }else{
                        ansStr += ( "0" );
                    }
                    ansStr += ")";
                }else{
                    ansStr += position2;
                }
                return ansStr;
            }
        }

        public int compareTo(Object o) {
            SpliceLocation otherSplice = (SpliceLocation) o;
            int thisRank = this.rank();
            int otherRank = otherSplice.rank();

            if(thisRank==otherRank){
                if(thisRank==0){ // rank 0
                    Interval interval1 = new Interval(this.position1,this.position2);
                    Interval interval2 = new Interval(otherSplice.position1,otherSplice.position2);
                    return interval1.compareTo(interval2);
                }else if(thisRank==1){ // rank 1
                    if( this.exon1!=0 && otherSplice.exon1!=0 ){
                        if(this.exon1==otherSplice.exon1){
                            Interval interval1 = new Interval(this.position1,this.position2);
                            Interval interval2 = new Interval(otherSplice.position1,otherSplice.position2);
                            return interval1.compareTo(interval2);
                        }else{
                            return this.exon1 - otherSplice.exon1;
                        }
                    }else if( this.exon2!=0 && otherSplice.exon2!=0 ){
                        if(this.exon2==otherSplice.exon2){
                            Interval interval1 = new Interval(this.position2,this.position1);
                            Interval interval2 = new Interval(otherSplice.position2,otherSplice.position1);
                            return interval1.compareTo(interval2);
                        }else{
                            return this.exon2 - otherSplice.exon2;
                        }
                    }else{
                        if(this.exon1>0){
                            return -1;
                        }else{
                            return 1;
                        }
                    }
                }else{ // rank 2
                    Interval interval1 = new Interval(this.exon1,this.exon2);
                    Interval interval2 = new Interval(otherSplice.exon1,otherSplice.exon2);
                    if(interval1.compareTo(interval2)==0){
                        interval1 = new Interval(this.position1,this.position2);
                        interval2 = new Interval(otherSplice.position1,otherSplice.position2);
                        return interval1.compareTo(interval2);
                    }else{
                        return interval1.compareTo(interval2);
                    }
                }
            }else{
                return otherRank-thisRank;
            }
        }

        public Pair toGenomicPair( String geneID, CanonicalGFF cgff) {
            int start = 0,stop = 0;
            Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
            // build exonSnMap & interval tree for exons
            Map<Integer, Interval> snExonMap = new HashMap<Integer, Interval>();
            snExonMap.put(0,null);
            int exonSn=1;
            for(Iterator iterator = exonRegions.iterator();iterator.hasNext();){
                Interval exon = (Interval) iterator.next();
                snExonMap.put(exonSn,exon);
                exonSn++;
            }

            //check: for exon1==exon2!=0
            if (exon1==exon2 && exon1>0) {
                start=snExonMap.get(exon1).getStart()+position1;
                stop=snExonMap.get(exon1).getStart()+position2;
            }else{
                if(exon1==0) {
                    start=position1;
                }else{
                    start=snExonMap.get(exon1).getStop();
                    start+=position1;
                }
                if(exon2==0) {
                    stop=position2;
                }else{
                    stop=snExonMap.get(exon2).getStart();
                    stop-=position2;
                }
            }
            return new Pair(start,stop);
        }
    }

    private Map geneSpliceCntMap = new HashMap();
    private Map geneSpliceSplicingPosMap = new HashMap();
    public Map geneSpliceMapByModel = new HashMap();
    private CanonicalGFF geneModel = null;

    public FineSpliceCounter(CanonicalGFF geneModel,CanonicalGFF cgff){
        this.geneModel = geneModel;
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;

        if(geneModel!=null){
            for(Iterator modelIterator = geneModel.geneExonRegionMap.keySet().iterator();modelIterator.hasNext();){
                String modelID = (String) modelIterator.next();
                GenomeInterval modelInterval = (GenomeInterval) geneModel.geneRegionMap.get(modelID);
                Set modelExonSet = (Set) geneModel.geneExonRegionMap.get(modelID);
                Set containingGenes = cgff.getRelatedGenes(modelInterval.getChr(),modelExonSet,true,true,1,true);
                for(Iterator geneIterator = containingGenes.iterator();geneIterator.hasNext();){
                    GenomeInterval geneRegion = (GenomeInterval) geneIterator.next();
                    String geneID = (String) geneRegion.getUserObject();
                    modelFineSpliceCounting(modelExonSet,geneID,cgff);
                }
            }
        }
    }

    private void modelFineSpliceCounting(Set intervalSet, String geneID, CanonicalGFF cgff){
        if(intervalSet.size()<2) return;

        Set<Interval> exonRegions = (Set<Interval>) cgff.geneExonRegionMap.get(geneID);
        // build exonSnMap & interval tree for exons
        Map<Interval, Integer> exonSnMap = new HashMap<Interval, Integer>();
        exonSnMap.put(null,0);
        int exonSn=1;
        IntervalTree exonIntervalTree = new IntervalTree();
        for(Iterator<Interval> iterator = exonRegions.iterator();iterator.hasNext();){
            Interval exon = (Interval) iterator.next();
            exonSnMap.put(exon,exonSn);
            exonIntervalTree.insert(exon);
            exonSn++;
        }

        Set spliceSet;
        if(geneSpliceMapByModel.containsKey(geneID)){
            spliceSet = (Set) geneSpliceMapByModel.get(geneID);
        }else{
            spliceSet = new TreeSet();
            geneSpliceMapByModel.put(geneID,spliceSet);
        }

        Interval interval1 = null;
        Interval interval2 = null;
        for(Iterator intervalIterator=intervalSet.iterator();intervalIterator.hasNext();){
            interval1 = interval2;
            interval2 = (Interval) intervalIterator.next();
            if(interval1==null) continue;
            // get exons at splicing junction
            Interval exon1,exon2;
            try{ exon1 = (Interval) (new TreeSet(exonIntervalTree.searchAll(interval1))).last(); }catch(NoSuchElementException ex){ exon1 = null; }
            try{ exon2 = (Interval) (new TreeSet(exonIntervalTree.searchAll(interval2))).first(); }catch(NoSuchElementException ex){ exon2 = null; }
            int exonNo1 = ((Integer)exonSnMap.get(exon1)).intValue();
            int exonNo2 = ((Integer)exonSnMap.get(exon2)).intValue();
            // specify the splice location
            SpliceLocation splice;
            if(exonNo1==0 && exonNo2==0){
                splice = new SpliceLocation(exonNo1,interval1.getStop(),exonNo2,interval2.getStart());
            }else if(exonNo1!=exonNo2){
                if(exonNo1==0){
                    splice = new SpliceLocation(exonNo1,interval1.getStop(),exonNo2,exon2.getStart()-interval2.getStart());
                }else if(exonNo2==0){
                    splice = new SpliceLocation(exonNo1,interval1.getStop()-exon1.getStop(),exonNo2,interval2.getStart());
                }else{
                    splice = new SpliceLocation(exonNo1,interval1.getStop()-exon1.getStop(),exonNo2,exon2.getStart()-interval2.getStart());
                }
            }else{
                splice = new SpliceLocation(exonNo1,interval1.getStop()-exon1.getStart(),exonNo1,interval2.getStart()-exon1.getStart());
            }
            // record
            spliceSet.add(splice);
        }
    }

    private void fineSpliceCounting(AlignmentRecord record, Map geneSpliceCntMap, Number count, String geneID, CanonicalGFF cgff) {

        if(record.numBlocks<2) return;

        Set<Interval> exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        Map<Integer,Interval> snExonMap = new HashMap<Integer,Interval>();
        int exonSn=1;

        IntervalTree exonIntervalTree = new IntervalTree();
        for(Iterator<Interval> iterator = exonRegions.iterator();iterator.hasNext();){
            Interval exon = iterator.next();
            exonIntervalTree.insert(new Interval(exon.getStart(),exon.getStop(),exonSn));
            snExonMap.put(exonSn,exon);
            exonSn++;
        }

        Set<Interval> spliceIntervalSet= new HashSet<Interval>();
        Map<Integer, Integer> indexPositionMap = new TreeMap<Integer, Integer>();
        Map<Integer, Integer> indexExonMap = new TreeMap<Integer, Integer>();
        int IntervalNum = 0;
        Interval interval1 = null, interval2 = null;
        for(int i=0;i<record.numBlocks;i++){
            interval1 = interval2;
            interval2 = new Interval(record.tStarts[i],record.tStarts[i] + record.tBlockSizes[i]-1);
            if(interval1==null) continue;
            spliceIntervalSet.add(new Interval(interval1.getStop(),interval2.getStart(),i));
            indexPositionMap.put(IntervalNum+1,interval1.getStop());
            indexPositionMap.put(IntervalNum+2,interval2.getStart());
            IntervalNum += 2;
        }

        IntervalTree spliceIntervalTree= new IntervalTree();
        //check inner exon splice
        for (Iterator<Interval> spliceIr=spliceIntervalSet.iterator();spliceIr.hasNext();){
            Interval spliceInterval = spliceIr.next();
            int indexNum = (Integer) spliceInterval.getUserObject()*2;
            List<Interval> innerSet = exonIntervalTree.searchAll(spliceInterval);
            if (innerSet.size()==1) {
                Iterator<Interval> exonIr = innerSet.iterator();
                Interval exonInterval = exonIr.next();
                if (exonInterval.contain(spliceInterval.getStart(), spliceInterval.getStop())) {
                    indexExonMap.put(indexNum-1,(Integer) exonInterval.getUserObject());
                    indexExonMap.put(indexNum,(Integer) exonInterval.getUserObject());
                }else{
                    spliceIntervalTree.insert(spliceInterval);
                }
            }else{
                spliceIntervalTree.insert(spliceInterval);
            }
        }

        // exon left scan & right scan
        int rStart,rStop,lStart,lStop;
        for (int j=1; j<=exonRegions.size(); j++ ) {
            // calculate exon left & right position
            if (j<exonRegions.size()) {
                rStop = snExonMap.get(j+1).getStart();
            }else{ // j = last exon block
                rStop = record.tStarts[record.numBlocks-1]+record.tBlockSizes[record.numBlocks-1]-1;
                if (snExonMap.get(j).getStop()>rStop)  rStop = snExonMap.get(j).getStop();
            }
            rStop -= 1;  // fix for IntervalTree
            rStart = snExonMap.get(j).getStart();

            if (j!=1) {
                lStart = snExonMap.get(j-1).getStop();
            }else{ // j = first exon block
                lStart = snExonMap.get(j).getStart();
                if (record.tStarts[0]<lStart) lStart = record.tStarts[0];
            }
            lStart += 1;  // fix for IntervalTree
            lStop   = snExonMap.get(j).getStop();

            TreeSet<Interval> rScanSet = new TreeSet<Interval>(spliceIntervalTree.searchAll(new Interval(rStart,rStop)));
            TreeSet<Interval> lScanSet = new TreeSet<Interval>(spliceIntervalTree.searchAll(new Interval(lStart,lStop)));

            for (Iterator<Interval> rScanSetIr = rScanSet.iterator(); rScanSetIr.hasNext();) {
                Interval spliceInterval = rScanSetIr.next();
                if (spliceInterval.getStart() >= rStart) {
                    indexExonMap.put((Integer)spliceInterval.getUserObject()*2-1,j);
                    break;
                }
            }

            for (Iterator<Interval> lScanSetIr = lScanSet.descendingIterator(); lScanSetIr.hasNext();) {
                Interval spliceInterval = lScanSetIr.next();
                if (spliceInterval.getStop() <= lStop) {
                    indexExonMap.put((Integer)spliceInterval.getUserObject()*2,j);
                    break;
                }
            }
        }

        for (int i=1; i < record.numBlocks; i++ ) {
            int exonNo1=0,exonNo2=0;
            int startPos = indexPositionMap.get(i*2-1);
            int stopPos  = indexPositionMap.get(i*2);
            if (indexExonMap.containsKey(i*2-1)) exonNo1  = indexExonMap.get(i*2-1);
            if (indexExonMap.containsKey(i*2))   exonNo2  = indexExonMap.get(i*2);

            // specify the splice location
            SpliceLocation splice;
            if(exonNo1==0 && exonNo2==0){
                splice = new SpliceLocation(exonNo1,startPos,exonNo2,stopPos);
            }else if(exonNo1!=exonNo2){
                if(exonNo1==0){
                    splice = new SpliceLocation(exonNo1,startPos,exonNo2,snExonMap.get(exonNo2).getStart()-stopPos);
                }else if(exonNo2==0){
                    splice = new SpliceLocation(exonNo1,startPos-snExonMap.get(exonNo1).getStop(),exonNo2,stopPos);
                }else{
                    splice = new SpliceLocation(exonNo1,startPos-snExonMap.get(exonNo1).getStop(),exonNo2,snExonMap.get(exonNo2).getStart()-stopPos);
                }
            }else{
                splice = new SpliceLocation(exonNo1,startPos-snExonMap.get(exonNo1).getStart(),exonNo1,stopPos-snExonMap.get(exonNo1).getStart());
            }

            Map spliceCntMap;
            if(geneSpliceCntMap.containsKey(geneID)){
                spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
            }else{
                spliceCntMap = new TreeMap();
                geneSpliceCntMap.put(geneID,spliceCntMap);
            }

            // record
            if(spliceCntMap.containsKey(splice)){
                float oldValue = ((Number)spliceCntMap.get(splice)).floatValue();
                spliceCntMap.put(splice,new Float(oldValue+count.floatValue()));
            }else{
                spliceCntMap.put(splice,count);
                }

            // update geneSplicePosMap
            Map spliceSplicingPosMap;
            if(geneSpliceSplicingPosMap.containsKey(geneID)){
                spliceSplicingPosMap = (Map) geneSpliceSplicingPosMap.get(geneID);
            }else{
                spliceSplicingPosMap = new TreeMap();
                geneSpliceSplicingPosMap.put(geneID,spliceSplicingPosMap);
            }

            Map splicingPosMap;
            if (spliceSplicingPosMap.containsKey(splice)) {
                splicingPosMap = (Map) spliceSplicingPosMap.get(splice);
            } else {
                splicingPosMap = new TreeMap();
                spliceSplicingPosMap.put(splice, splicingPosMap);
            }

            int splicingPos = record.qStarts[i-1]+record.qBlockSizes[i-1]-1;
            if (splicingPosMap.containsKey(splicingPos)) {
                int val = ((Integer) splicingPosMap.get(splicingPos)).intValue() + 1;
                splicingPosMap.put(splicingPos, val);
            }else{
                splicingPosMap.put(splicingPos, 1);
            }
        }

    }

    public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
        fineSpliceCounting(record,geneSpliceCntMap,cnt,geneID,cgff);
    }

    public void countReadMulti(String readID,Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
    }

    public void report(String filename, CanonicalGFF cgff) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            // header
            fw.write("#GeneID" + "\t" +
                     "splice" + "\t" +
                     "#reads"
                    );
            if(geneModel==null){
            }else{
                fw.write("\t" + "novel");
            }
            fw.write("\t" + "splicingPosFreq");
            fw.write( "\t" + "format:.fineSplice" + "\n");
            // contents for each gene
            for(Iterator iterator = cgff.geneLengthMap.keySet().iterator();iterator.hasNext();){
                String geneID = (String) iterator.next();
                Map spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
                Map spliceSplicingPosMap = (Map) geneSpliceSplicingPosMap.get(geneID);

                if(spliceCntMap==null) continue;
                // from model
                Set modelSpliceSet;
                if(geneModel==null){
                    modelSpliceSet = null;
                }else{
                    modelSpliceSet = (Set) geneSpliceMapByModel.get(geneID);
                }

                // iterate splice for this gene
                for(Iterator spliceIterator = spliceCntMap.keySet().iterator();spliceIterator.hasNext();){
                    SpliceLocation splice = (SpliceLocation) spliceIterator.next();
                    float uniqCnt = ((Number)spliceCntMap.get(splice)).floatValue();
                    // geneID
                    fw.write(geneID + "\t");
                    // splice
                    fw.write(splice.toString() + "\t");
                    // # reads
                    fw.write(new Float(uniqCnt).toString() + "\t");
                    // check model (if given)
                    if(geneModel==null){
                    }else{
                        if(modelSpliceSet==null){
                            fw.write("V" + "\t");
                        }else if(modelSpliceSet.contains(splice)==false){
                            fw.write("V" + "\t");
                        }else{
                            fw.write(" " + "\t");
                        }
                    }
                    // splicingPosFreq
                    if(spliceSplicingPosMap.containsKey(splice)){
                        fw.write(spliceSplicingPosMap.get(splice).toString());
                    }else{
                        fw.write(" ");
                    }
                    fw.write("\n");
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static String gffFilename = null;
    private static Map mappingFilenameMethodMap = new LinkedHashMap();
    private static Map mappingMethodMap = null;

    private static String outputPrefix = null;

    private static int joinFactor = 2;
    private static float identityCutoff = 0.95F;
    private static boolean useExonRegion = false;
    private static boolean checkByContaining = false;
    private static int minimumOverlap = 8;
    private static boolean checkAllBlocks = false;
    private static boolean careDirection = false;

    private static String modelFilename = null;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-M")){
                mappingFilenameMethodMap.put(args[i + 2],args[i + 1]);
                i+=2;
            }
            else if(args[i].equals("-J")){
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-ID")){
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-O")){
                outputPrefix = args[i + 1];
                i++;
            }
            else if(args[i].equals("-exon")){
                useExonRegion = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-contain")){
                checkByContaining = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-min")){
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-ALL")){
                checkAllBlocks = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-direction")){
                careDirection = Boolean.valueOf(args[i+1]);
                i++;
            }
            else if(args[i].equals("-model")){
                modelFilename = args[i + 1];
                i++;
            }
        }

        // check for necessary parameters
        if(gffFilename==null){
          System.err.println("canonical GFF filename (-GFF) not assigned");
          System.exit(1);
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator",System.getProperty("java.class.path"),"misc");
        if(mappingFilenameMethodMap.size()<=0){
          System.err.println("mapping method/filename (-M) not assigned, available methods:");
          for(Iterator iterator = mappingMethodMap.keySet().iterator();iterator.hasNext();){
              System.out.println(iterator.next());
          }
          System.exit(1);
        }
        for(Iterator methodIterator = mappingFilenameMethodMap.values().iterator();methodIterator.hasNext();){
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if(outputPrefix==null){
            System.err.println("output prefix (-O) not assigned");
            System.exit(1);
        }
        if(minimumOverlap<1){
            System.err.println("minimum block size (-min) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: FineSpliceCounter");
        System.out.println("canonical GFF filename (-GFF): " + gffFilename);
        System.out.println("mapping method/filename (-M):");
        for(Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): "+outputPrefix);
        System.out.println("block join factor (-J): "+joinFactor);
        System.out.println("identity cutoff (-ID): "+ identityCutoff);
        System.out.println("use exon region (-exon): "+useExonRegion);
        System.out.println("check by containing (-contain, FALSE for by intersecting): "+checkByContaining);
        System.out.println("minimum overlap (-min): "+ minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): "+checkAllBlocks);
        System.out.println("care mapping direction (-direction): "+careDirection);
        System.out.println("model filename (-model): "+ modelFilename);
        System.out.println();
    }

    //  Main section
    public static void main(String args[]){
        paraProc(args);

        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        Map geneUniqReadCntMap = new HashMap();
        Map geneMultiReadCntMap = new HashMap();

        // gene model for comparison
        CanonicalGFF geneModel = null;
        if(modelFilename!=null){
            geneModel = new CanonicalGFF(modelFilename);
        }
        // read counters
        final FineSpliceCounter fineSpliceCounter = new FineSpliceCounter(geneModel,cgff);

        ReadCounter readCounter = new ReadCounter(){ // wrap ReadCounters
            public void countReadUnique(String readID,AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
                fineSpliceCounter.countReadUnique(readID,record, cnt, geneID, cgff);
            }

            public void countReadMulti(String readID, Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
                fineSpliceCounter.countReadMulti(readID,recordCollection, cnt, geneID, cgff);
            }

            public void report(String filename, CanonicalGFF cgff) {
                fineSpliceCounter.report(filename + ".fineSplice",cgff);
            }
        };

        // computing based on unique reads
        int uniqReadCnt = 0;
        //Set nonUniqReads = new HashSet();
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            UniqueReadIterator uniqueRI = new UniqueReadIterator(Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
                    identityCutoff,
                    joinFactor,
                    useExonRegion,
                    checkByContaining,
                    minimumOverlap,
                    checkAllBlocks,
                    careDirection,
                    cgff,
                    geneUniqReadCntMap
                    );
            uniqueRI.iterate(readCounter);
            uniqReadCnt += uniqueRI.uniqReadCnt;
            //nonUniqReads.addAll(uniqueRI.restReads);
        }
        System.out.println("#uniq reads: " + uniqReadCnt);

        // output gene RPKM
        readCounter.report(outputPrefix,cgff);
    }
}
