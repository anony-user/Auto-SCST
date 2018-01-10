package special;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;

public class SpliceJunctionCGFF {

    private static String gffFilename = null;
    private static String modelFilename = null;
    private static int flankingWidth = 0;
    private static String juncionFilename = null;
    private static String outFilename = null;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-O")){
                outFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-W")){
                flankingWidth = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-junction")){
                juncionFilename = args[i + 1];
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
        if(modelFilename==null){
          System.err.println("model filename (-model) not assigned");
          System.exit(1);
        }
        if(outFilename==null){
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        if(flankingWidth < 1){
            System.err.println("flanking width (-W) less than 1");
            System.exit(1);
        }
        if(juncionFilename==null){
            System.err.println("junction filename (-junction) not assigned");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: SpliceJunctionCGFF");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("model filename (-model): "+modelFilename);
        System.out.println("flanking width (-W): "+ flankingWidth);
        System.out.println("junction filename (-junction): "+juncionFilename);
        System.out.println("output filename (-O): "+outFilename);
        System.out.println();
    }

    public static void main(String args[]) throws FileNotFoundException {
        paraProc(args);
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF model = new CanonicalGFF(modelFilename);

        BufferedReader fr = new BufferedReader(new FileReader(juncionFilename));
        try {
            FileWriter fw = new FileWriter(outFilename);
            // for each intron junction
            while (fr.ready()) {
                String line = fr.readLine().trim();
                String tokens[] = line.split("[:.]+");
                int start = Integer.parseInt(tokens[1]);
                int stop = Integer.parseInt(tokens[2]);

                Set hitGenes = cgff.getRelatedGenes(tokens[0],start,stop,false,true,1);

                // NO containing genes
                if(hitGenes.size()==0){
                   System.out.println("no genes for " + line);
                }
                // only ONE containing genes
                else if(hitGenes.size()==1){
                    // get the gene
                    GenomeInterval geneRegion = (GenomeInterval) hitGenes.iterator().next();
                    String geneID = (String) geneRegion.getUserObject();
                    // write
                    fw.write(">"+tokens[0]+"_"+start+"_"+stop+"_"+geneID + "\t" +
                             tokens[0] + "\t" +
                             start + "\t" +
                             stop + "\t" +
                             cgff.getStrand(geneID) + "\n"
                            );
                    fw.write((start-flankingWidth+1) + "\t" +
                             start + "\n");
                    fw.write(stop + "\t" +
                             (stop+flankingWidth-1) + "\n");
                }
                // two or more containing genes
                else{
                    // verify for two points
                    Set verifiedGeneIDs = new HashSet();
                    for(Iterator geneIterator = hitGenes.iterator();geneIterator.hasNext();){
                        GenomeInterval gi = (GenomeInterval) geneIterator.next();
                        String geneID = (String) gi.getUserObject();
                        // collect model IDs, THIS IS UGLY
                        Set modelIDs = new HashSet();
                        for(Iterator modelIterator = model.geneExonRegionMap.keySet().iterator();modelIterator.hasNext();){
                            String modelID = (String) modelIterator.next();
                            if(modelID.startsWith(geneID)){ // specifically for TAIR
                                modelIDs.add(modelID);
                            }
                        }
                        // collect model end points
                        Set modelStartPoints = new HashSet();
                        Set modelStopPoints = new HashSet();
                        for(Iterator modelIterator = modelIDs.iterator();modelIterator.hasNext();){
                            String modelID = (String) modelIterator.next();
                            Set exonRegions = (Set) model.geneExonRegionMap.get(modelID);
                            for(Iterator exonIterator = exonRegions.iterator();exonIterator.hasNext();){
                                Interval exon = (Interval) exonIterator.next();
                                modelStartPoints.add(exon.getStart());
                                modelStopPoints.add(exon.getStop());
                            }
                        }
                        // verify
                        if(modelStopPoints.contains(start) && modelStartPoints.contains(stop)){
                            verifiedGeneIDs.add(geneID);
                        }
                    }

                    // if the two end points fit exactly one gene's models
                    if(verifiedGeneIDs.size()==1){
                        String geneID = (String) verifiedGeneIDs.iterator().next();
                        // write
                        fw.write(">"+tokens[0]+"_"+start+"_"+stop+"_model2_"+geneID + "\t" +
                                 tokens[0] + "\t" +
                                 start + "\t" +
                                 stop + "\t" +
                                 cgff.getStrand(geneID) + "\n"
                                );
                        fw.write((start-flankingWidth+1) + "\t" +
                                 start + "\n");
                        fw.write(stop + "\t" +
                                 (stop+flankingWidth-1) + "\n");
                        continue;
                    }

                    // verify for one points
                    verifiedGeneIDs = new HashSet();
                    for(Iterator geneIterator = hitGenes.iterator();geneIterator.hasNext();){
                        GenomeInterval gi = (GenomeInterval) geneIterator.next();
                        String geneID = (String) gi.getUserObject();
                        // collect model IDs, THIS IS UGLY
                        Set modelIDs = new HashSet();
                        for(Iterator modelIterator = model.geneExonRegionMap.keySet().iterator();modelIterator.hasNext();){
                            String modelID = (String) modelIterator.next();
                            if(modelID.startsWith(geneID)){ // specifically for TAIR
                                modelIDs.add(modelID);
                            }
                        }
                        // collect model end points
                        Set modelStartPoints = new HashSet();
                        Set modelStopPoints = new HashSet();
                        for(Iterator modelIterator = modelIDs.iterator();modelIterator.hasNext();){
                            String modelID = (String) modelIterator.next();
                            Set exonRegions = (Set) model.geneExonRegionMap.get(modelID);
                            for(Iterator exonIterator = exonRegions.iterator();exonIterator.hasNext();){
                                Interval exon = (Interval) exonIterator.next();
                                modelStartPoints.add(exon.getStart());
                                modelStopPoints.add(exon.getStop());
                            }
                        }
                        // verify
                        if(modelStopPoints.contains(start) || modelStartPoints.contains(stop)){
                            verifiedGeneIDs.add(geneID);
                        }
                    }

                    // if one of the two end points fit exactly one gene's models
                    if(verifiedGeneIDs.size()==1){
                        String geneID = (String) verifiedGeneIDs.iterator().next();
                        // write
                        fw.write(">"+tokens[0]+"_"+start+"_"+stop+"_model1_"+geneID + "\t" +
                                 tokens[0] + "\t" +
                                 start + "\t" +
                                 stop + "\t" +
                                 cgff.getStrand(geneID) + "\n"
                                );
                        fw.write((start-flankingWidth+1) + "\t" +
                                 start + "\n");
                        fw.write(stop + "\t" +
                                 (stop+flankingWidth-1) + "\n");
                        continue;
                    }

                    // rest case, the two end points fits no model
                    TreeSet twoEndIntervals = new TreeSet();
                    twoEndIntervals.add(new Interval(start, start));
                    twoEndIntervals.add(new Interval(stop, stop));
                    Set hitGenesEx = cgff.getRelatedGenes(tokens[0], twoEndIntervals, true, true, 1, true);

                    if (hitGenesEx.size() == 1) {
                        // get the gene
                        GenomeInterval geneRegion = (GenomeInterval) hitGenesEx.iterator().next();
                        String geneID = (String) geneRegion.getUserObject();
                        // write
                        fw.write(">" + tokens[0] + "_" + start + "_" + stop + "_exon_" + geneID + "\t" +
                                 tokens[0] + "\t" +
                                 start + "\t" +
                                 stop + "\t" +
                                 cgff.getStrand(geneID) + "\n"
                                );
                        fw.write((start - flankingWidth + 1) + "\t" +
                                 start + "\n");
                        fw.write(stop + "\t" +
                                 (stop + flankingWidth - 1) + "\n");
                    } else {
                        fw.write(">" + tokens[0] + "_" + start + "_" + stop + "_none_" + "\t" +
                                 tokens[0] + "\t" +
                                 start + "\t" +
                                 stop + "\t" +
                                 "+" + "\n"
                                );
                        fw.write((start - flankingWidth + 1) + "\t" +
                                 start + "\n");
                        fw.write(stop + "\t" +
                                 (stop + flankingWidth - 1) + "\n");
                    }
                }
            }
            fr.close();
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
