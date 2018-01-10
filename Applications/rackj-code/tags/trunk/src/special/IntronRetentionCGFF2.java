package special;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;
import misc.IntervalCoverageNode;
import misc.Util;

public class IntronRetentionCGFF2 {

    private static boolean debug = false;

    private static String gffFilename = null;

    private static String inFilename = null;
    private static String outFilename = null;

    private static int readCutoff = 3;
    private static int probeLen = 60;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-I")){
                inFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-O")){
                outFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-read")){
                readCutoff = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-P")){
                probeLen = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        // check for necessary parameters
        if(gffFilename==null){
          System.err.println("canonical GFF filename (-GFF) not assigned");
          System.exit(1);
        }
        if(inFilename==null){
            System.err.println("input filename (-I) not assigned");
            System.exit(1);
        }
        if(outFilename==null){
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: IntronRetentionCGFF2");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("input filename (-I): "+inFilename);
        System.out.println("output filename (-O): "+outFilename);
        System.out.println("read cutoff (-read): "+ readCutoff);
        System.out.println("probe length (-P): "+ probeLen);
        System.out.println();
    }

    public static void main(String args[]){
        paraProc(args);

        // get exonic CGFF and then intronic CGFF
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF intronCGFF = Util.getIntronicCGFF(cgff);

        // build ICN sets for all retained intron
        // for each uniquely mapped reads without splicing
        Map geneIntronCntMap = new TreeMap();
        Map geneIntronIcnsMap = new TreeMap();

        BufferedReader fr = null;
        try {
            int icnID = 0;
            fr = new BufferedReader(new FileReader(new File(inFilename)));
            while(fr.ready()){
                String line = fr.readLine();
                if(line.startsWith("#")) continue;

                String tokens[] = line.split("\t");
                String geneID = tokens[0];
                int intronNo = Integer.parseInt(tokens[1]);
                int iStart = Integer.parseInt(tokens[2]);
                int iStop = Integer.parseInt(tokens[3]);
                int readCnt = Integer.parseInt(tokens[4]);
                int rStart = Integer.parseInt(tokens[5]);
                int rStop = Integer.parseInt(tokens[6]);
                String coverageArrayStr = tokens[7];
                String[] tmp = coverageArrayStr.substring(1,coverageArrayStr.length()-1).split(",");
                int coverageArray[] = new int[tmp.length];
                for(int i=0; i<tmp.length; i++){
                  coverageArray[i] = Integer.parseInt(tmp[i].trim());
                }

                // for intron read count
                Map intronCntMap;
                if(geneIntronCntMap.containsKey(geneID)){
                    intronCntMap = (Map) geneIntronCntMap.get(geneID);
                }else{
                    intronCntMap = new TreeMap();
                    geneIntronCntMap.put(geneID,intronCntMap);
                }
                intronCntMap.put(intronNo,readCnt);

                // get corresponding icn set
                Map intronIcnsMap;
                if(geneIntronIcnsMap.containsKey(geneID)){
                    intronIcnsMap = (Map) geneIntronIcnsMap.get(geneID);
                }else{
                    intronIcnsMap = new TreeMap();
                    geneIntronIcnsMap.put(geneID,intronIcnsMap);
                }
                Set icns;
                if(intronIcnsMap.containsKey(intronNo)){
                    icns = (Set) intronIcnsMap.get(intronNo);
                }else{
                    icns = new TreeSet();
                    intronIcnsMap.put(intronNo,icns);
                }
                IntervalCoverageNode newIcn = new IntervalCoverageNode(rStart,rStop,coverageArray);
                newIcn.setUserObject(icnID++);
                icns.add(newIcn);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        try {
            FileWriter fw = new FileWriter(outFilename);

            // for each gene
            for(Iterator geneIterator = geneIntronIcnsMap.keySet().iterator();geneIterator.hasNext();){
                Object geneID = geneIterator.next();
                if(debug) System.out.println("processing gene: " + geneID);
                GenomeInterval geneRegion = (GenomeInterval) cgff.geneRegionMap.get(geneID);
                Set intronRegions = (Set) intronCGFF.geneExonRegionMap.get(geneID);
                Interval[] intronRegionArray = (Interval[]) intronRegions.toArray(new Interval[intronRegions.size()]);
                Map intronIcnsMap = (Map) geneIntronIcnsMap.get(geneID);
                Map intronCntMap = (Map) geneIntronCntMap.get(geneID);
                // for each intron
                for(Iterator intronIterator = intronIcnsMap.keySet().iterator();intronIterator.hasNext();){
                    int intronNo = ((Integer)intronIterator.next()).intValue();
                    if(debug) System.out.println("processing intron: " + intronNo);
                    Set icnSet = (Set) intronIcnsMap.get(intronNo);
                    Interval intronInterval = intronRegionArray[intronNo-1];
                    // get read number of this intron
                    int readNum = ((Integer)intronCntMap.get(intronNo)).intValue();
                    // skip this intron if its read number less than the cutoff
                    if(readNum < readCutoff) continue;

                    // find heaviest probeLen-interval within the intron
                    int maxWeight = 0;
                    int maxStart = 0;
                    for(Iterator icnIterator = icnSet.iterator();icnIterator.hasNext();){
                        IntervalCoverageNode icn = (IntervalCoverageNode) icnIterator.next();
                        for(int i=0;i<icn.getCoverageArray().length;i++){
                            // skip if test interval out of intron range
                            if(icn.getStart()+i < intronInterval.getStart()) continue;
                            if(icn.getStart()+i+probeLen-1 > intronInterval.getStop()) continue;
                            if(i+probeLen-1 > icn.length()-1) continue;

                            int weight = arraySum(icn.getCoverageArray(),i,i+probeLen-1);
                            if(weight > maxWeight){
                                maxWeight = weight;
                                maxStart = icn.getStart()+i;
                            }
                        }
                    }
                    // if any interval within the intron
                    if(maxWeight>0){
                        fw.write(">irProbe_" + geneID + "_" + intronNo + "_" + "inside" + "\t" +
                                 geneRegion.getChr() + "\t" +
                                 maxStart + "\t" +
                                 (maxStart+probeLen-1) + "\t" +
                                 cgff.getStrand(geneID) + "\n");
                        fw.write(maxStart + "\t" +
                                 (maxStart+probeLen-1) + "\n");
                        continue;
                    }

                    // the overlapping between every ICN and the intron shorter than probeLen
                    IntervalCoverageNode icn = null;
                    // see is there any ICN covers the intron
                    for(Iterator icnIterator = icnSet.iterator();icnIterator.hasNext();){
                        icn = (IntervalCoverageNode) icnIterator.next();
                        if(icn.contain(intronInterval.getStart(),intronInterval.getStop())){
                            break;
                        }
                    }
                    // there is some such ICN
                    if(icn.contain(intronInterval.getStart(),intronInterval.getStop())){
                        // restLenRight + restLenLeft + intronLength = probeLen
                        int restLen = probeLen;
                        restLen = restLen - intronInterval.length();
                        int restLenRight = restLen / 2;
                        int restLenLeft = restLen - restLenRight;
                        fw.write(">irProbe_" + geneID + "_" + intronNo + "_" + "contained" + "\t" +
                                 geneRegion.getChr() + "\t" +
                                 (intronInterval.getStart()-1-restLenLeft+1) + "\t" +
                                 (intronInterval.getStop()+1+restLenRight-1) + "\t" +
                                 cgff.getStrand(geneID) + "\n");
                        fw.write((intronInterval.getStart()-1-restLenLeft+1) + "\t" +
                                 (intronInterval.getStop()+1+restLenRight-1) + "\n");
                        continue;
                    }

                    // find the heaviest ICN
                    maxWeight = 0;
                    IntervalCoverageNode bestICN = null;
                    for(Iterator icnIterator = icnSet.iterator();icnIterator.hasNext();){
                        icn = (IntervalCoverageNode) icnIterator.next();
                        if(arraySum(icn.getCoverageArray()) > maxWeight){
                            bestICN = icn;
                        }
                    }
                    // if bestICN intersects no end points of the intron
                    if(bestICN.intersect(intronInterval.getStart())==false &&
                       bestICN.intersect(intronInterval.getStop())==false){
                        int restLen = probeLen;
                        restLen = restLen - bestICN.length();
                        int restLenRight = restLen / 2;
                        int restLenLeft = restLen - restLenRight;
                        fw.write(">irProbe_" + geneID + "_" + intronNo + "_" + "short" + "\t" +
                                 geneRegion.getChr() + "\t" +
                                 (bestICN.getStart()-1-restLenLeft+1) + "\t" +
                                 (bestICN.getStop()+1+restLenRight-1) + "\t" +
                                 cgff.getStrand(geneID) + "\n");
                        fw.write((bestICN.getStart()-1-restLenLeft+1) + "\t" +
                                 (bestICN.getStop()+1+restLenRight-1) + "\n");
                    }
                    // intersects 5'end or 3'end (DNA)
                    else{
                        boolean dna5end;
                        if(bestICN.intersect(intronInterval.getStart())){
                            dna5end = true;
                        }else{
                            dna5end = false;
                        }
                        boolean forward;
                        if(cgff.getStrand(geneID).equals("+")){
                            forward = true;
                        }else{
                            forward = false;
                        }
                        boolean rna5end = (dna5end==forward);
                        String modifier = null;
                        if(rna5end) modifier = "5end";
                        else modifier = "3end";

                        int start = dna5end ? (bestICN.getStop()-probeLen+1) : (bestICN.getStart());
                        int stop = dna5end ? (bestICN.getStop()) : (bestICN.getStart()+probeLen-1);

                        fw.write(">irProbe_" + geneID + "_" + intronNo + "_" + modifier + "\t" +
                                 geneRegion.getChr() + "\t" +
                                 start + "\t" +
                                 stop + "\t" +
                                 cgff.getStrand(geneID) + "\n");
                        fw.write(start + "\t" +
                                 stop + "\n");
                    }
                }
            }

            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static int arraySum(int[] array,int start,int stop){
        int sum = 0;

        for (int i = start; i <= stop; i++) {
            try {
                sum += array[i];
            } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
                System.err.println("start, stop, i: " + start + ", " + stop + ", " + i);
                System.err.println(Arrays.toString(array));
                System.exit(1);
            }
        }

        return sum;
    }

    private static int arraySum(int[] array){
        int sum = 0;

        for(int i=0;i<array.length;i++){
            sum += array[i];
        }

        return sum;
    }
}
