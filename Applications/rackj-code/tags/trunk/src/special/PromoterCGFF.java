package special;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import misc.CanonicalGFF;
import misc.GenomeInterval;

public class PromoterCGFF {

    private static String gffFilename = null;
    private static int lengthFront = 0;
    private static int lengthRear = 0;
    private static boolean intergenic = false;
    private static int minimumLen = 0;
    private static boolean fromTSS = true;

    private static String inFilename = null;
    private static String outFilename = null;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }else if(args[i].equals("-I")){
                inFilename = args[i + 1];
                i++;
            }else if(args[i].equals("-O")){
                outFilename = args[i + 1];
                i++;
            }else if(args[i].equals("-L1")){
                lengthFront = Integer.parseInt(args[i + 1]);
                i++;
            }else if(args[i].equals("-L2")){
                lengthRear = Integer.parseInt(args[i + 1]);
                i++;
            }else if(args[i].equals("-intergenic")){
                intergenic = Boolean.valueOf(args[i + 1]);
                i++;
            }else if(args[i].equals("-min")){
                minimumLen = Integer.parseInt(args[i + 1]);
                i++;
            }else if(args[i].equals("-TSS")){
            	fromTSS = Boolean.valueOf(args[i + 1]);
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
        if((lengthFront+lengthRear) < 1){
            System.err.println("total length -- front plus rear (-L1 & -L2) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: PromoterCGFF");
        System.out.println("canonical GFF filename (-GFF): "+ gffFilename);
        System.out.println("input filename (-I): "+inFilename);
        System.out.println("retrieve region around TSS (-TSS, false for stop site): "+ fromTSS);
        System.out.println("region length -- front (-L1): "+ lengthFront);
        System.out.println("region length -- rear (-L2): "+ lengthRear);
        System.out.println("intergenic region (-intergenic): "+ intergenic);
        System.out.println("minimum length (-min): "+ minimumLen);
        System.out.println("output filename (-O): "+ outFilename);
        System.out.println();
    }

    public static void main(String args[]) throws FileNotFoundException {
        paraProc(args);
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);

        BufferedReader fr = new BufferedReader(new FileReader(inFilename));
        try {
            FileWriter fw = new FileWriter(outFilename);
            // for each input gene
            while (fr.ready()) {
                String geneID = fr.readLine().trim();
                
                // error handling
                if(cgff.geneRegionMap.containsKey(geneID)==false){
                	System.err.println("canonical GFF does not contain " + geneID);
                	continue;
                }
                
                GenomeInterval geneInterval = (GenomeInterval) cgff.geneRegionMap.get(geneID);
                boolean forward;
                int start,stop,tssPos;
                if(cgff.getStrand(geneID).equals("+")){
                    forward = true;
                    if (!fromTSS) {
                    	start = geneInterval.getStop()+1-lengthFront;
                        stop = geneInterval.getStop()+lengthRear;
                        tssPos = geneInterval.getStop();
                    }else{
                    	start = geneInterval.getStart()-lengthFront;
                    	stop = geneInterval.getStart()-1+lengthRear;
                        tssPos = geneInterval.getStart();
                    }
                }else{
                    forward = false;
                    if (!fromTSS) {
                    	start = geneInterval.getStart()-lengthRear;
                        stop = geneInterval.getStart()-1+lengthFront;
                        tssPos = geneInterval.getStart();
                    }else{
                    	start = geneInterval.getStop()+1-lengthRear;
                        stop = geneInterval.getStop()+lengthFront;
                        tssPos = geneInterval.getStop();
                    }
                }

                if(intergenic){
                    Set hitGeneSet = cgff.getRelatedGenes(geneInterval.getChr(),start,stop,false,false,1);
                    Set sameDirectionGenes = new HashSet();
                    if(hitGeneSet.size()>0){
                        for(Iterator iterator = hitGeneSet.iterator();iterator.hasNext();){
                            GenomeInterval hitGeneInterval = (GenomeInterval) iterator.next();
                            String hitGeneID = (String) hitGeneInterval.getUserObject();
                            if(cgff.getStrand(hitGeneID).equals(cgff.getStrand(geneID))){
                                sameDirectionGenes.add(hitGeneInterval);
                            }
                        }
                    }
                    int lastCoveredPoint;
                    if(forward){
                        lastCoveredPoint = start;
                    }else{
                        lastCoveredPoint = stop;
                    }
                    for(Iterator iterator = sameDirectionGenes.iterator();iterator.hasNext();){
                        GenomeInterval hitGeneInterval = (GenomeInterval) iterator.next();
                        int testPoint;
                        if(forward){
                            testPoint = hitGeneInterval.getStop();
                        }else{
                            testPoint = hitGeneInterval.getStart();
                        }
                        if((forward && testPoint>lastCoveredPoint && testPoint<tssPos) || (!forward && testPoint<lastCoveredPoint && testPoint>tssPos)){
                            lastCoveredPoint = testPoint;
                        }
                    }
                    if(forward){
                        start = lastCoveredPoint;
                    }else{
                        stop = lastCoveredPoint;
                    }
                }

                if((stop-start+1) < minimumLen) continue;
                fw.write(">" + geneID + "\t" +
                         cgff.getChrOriginal(geneID) + "\t" +
                         start + "\t" +
                         stop + "\t" +
                         cgff.getStrand(geneID) + "\n");
                fw.write(start + "\t" +
                         stop + "\n");
            }
            fr.close();
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
