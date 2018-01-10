package misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Chao-Yu Pan
 * @author Wen-Dar Lin
 * @version 0.9
 */
public class SeqGeneMd
{
    //inFilename: input file name
    static private String inFilename = null;
    //outFilename: output file prefix name
    static private String outPrefix = null;
    //groupLabelType: group_label in seq_gene.md
    static private String groupLabelType = null;

    static private String chrPrefix = "chr";

    //processing input parameters
    static private void paraProc(String[] args)
    {
        int i;
        // get parameter strings
        for (i = 0; i < args.length; i++)
        {
            if (args[i].equals("-I")){
                inFilename = args[i + 1];
                i++;
            }else if (args[i].equals("-O")){
            	outPrefix = args[i + 1];
                i++;
            }else if (args[i].equals("-G")){
                groupLabelType = args[i + 1];
                i++;
            }else if (args[i].equals("-chr")){
            	chrPrefix = args[i + 1];
                i++;
            }
        }

        // check for necessary parameters
        if (inFilename == null)
        {
            System.err.println("seq_gene.md filename (-I) not assigned\n");
            System.exit(1);
        }
        if (outPrefix == null)
        {
            System.err.println("output prefix (-O) not assigned\n");
            System.exit(1);
        }
        if (groupLabelType == null)
        {
            Set groupLabelSets=new TreeSet<String>();
            System.out.println("Ggroup label type (-G) not assigned, choose one of the following Group Label:");
            //fetch group label types
            try {
                BufferedReader fr = new BufferedReader(new FileReader(inFilename));
                while (fr.ready()) {
                    String line = fr.readLine();

                    if (Character.isDigit(line.charAt(0))) {
                        String temp[]=line.split("\t");
                        groupLabelSets.add(temp[12]);
                    }
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                System.exit(1);
            }

            //print out group label types
            String groupLabels[]=(String[]) groupLabelSets.toArray(new String[groupLabelSets.size()]);
            for(int j=0; j<groupLabels.length; j++)
            {
                System.out.println((j+1)+":"+groupLabels[j]);
            }

            String gInput ="0";
            int intGinput=0;
            int flag=0;
            int intFlag=0;

            //check input group label code
            while(intGinput<1 || intGinput>groupLabels.length || intFlag==0)
            {
                if(flag==0)
                {
                    System.out.print("Choose group label:");
                }
                else
                {
                    System.out.println("Group label number incorrect!!");
                    System.out.print("Please choose group label again:");
                }
                flag=1;
                BufferedReader stdin = new BufferedReader(new InputStreamReader(
                        System.in));
                try {
                    gInput = stdin.readLine();
                    intFlag=1;
                    try
                    {
                        intGinput=Integer.parseInt(gInput);
                    }
                    catch(NumberFormatException ex2)
                    {
                        intFlag=0;
                    }
                } catch (IOException ex1) {
                }
            }
            groupLabelType=groupLabels[(intGinput-1)];
            System.out.println(groupLabels[(intGinput-1)]);
            System.out.println();
        }

        // list parameters
        System.out.println("program: SeqGeneMd");
        System.out.println("input seq_gene.md (-I): " + inFilename);
        System.out.println("output filename (-O): " + outPrefix);
        System.out.println("group label type (-G): " + groupLabelType);
        System.out.println("chromosome prefix string (-chr): " + chrPrefix);
        System.out.println();
    }

    public static void main(String[] args)
    {
        //processing input parameters
        paraProc(args);

        //declare Maps about location
        //geneLocMap records geneID and its chromosome and gene intervals
        Map geneLocMap=new HashMap();
        //geneExonMap records exon intervals of given geneID
        Map geneExonMap=new HashMap();
        //transcriptLocMap records transcript and its chromosome and transcript intervals
        Map transcriptLocMap=new HashMap();
        //transcriptExonMap records exon intervals of given transcript
        Map transcriptExonMap=new HashMap();
        //cgff error flag
        boolean cgffErr=false;
        //model error flag
        boolean modelErr=false;

        //parse seq_gene.md to get information we needed
        parseSeqGeneMd(inFilename, groupLabelType,geneLocMap,geneExonMap,transcriptLocMap,transcriptExonMap);

        //write cgff file
         try{
           FileWriter fw = new FileWriter(outPrefix+".cgff");
           Iterator geneKeysIterator=geneLocMap.keySet().iterator();
           while(geneKeysIterator.hasNext()){
             Object currentKey = geneKeysIterator.next();
             Set geneHashSet = (HashSet) geneLocMap.get(currentKey);

             // processing exon
             boolean geneHaveExon=false;
             TreeSet<Interval> exonSet = (TreeSet) geneExonMap.get(currentKey);
             if(exonSet!=null){
               Interval.combineOverlap(exonSet);
               Interval.combineNearby(exonSet, 1);
               geneHaveExon = true;
             }

             // skip if a geneID has more than one record
             if(geneHashSet.size()>1){
               cgffErr=true;
               continue;
             }

             // header
             fw.write(">" + currentKey + "\t");
             GenomeInterval geneInterval = (GenomeInterval) geneHashSet.iterator().next();
             fw.write(chrPrefix + geneInterval.getChr() + "\t");
             if (geneHaveExon) {
               fw.write(exonSet.first().getStart() + "\t" +
                        exonSet.last().getStop() + "\t" +
                        geneInterval.getUserObject() + "\n");
             } else {
               fw.write(geneInterval.getStart() + "\t" +
                        geneInterval.getStop() + "\t" +
                        geneInterval.getUserObject() + "\n");
             }

             // exons
             if (geneHaveExon) {
               Iterator exonIterator = exonSet.iterator();
               while (exonIterator.hasNext()) {
                 Interval exonInterval = (Interval) exonIterator.next();
                 fw.write(exonInterval.getStart() + "\t" +
                          exonInterval.getStop() + "\n");
               }
             } else {
               fw.write(geneInterval.getStart() + "\t" +
                        geneInterval.getStop() + "\n");
             }
           }
           fw.close();
         }
         catch (IOException ex){
             ex.printStackTrace();
             System.exit(1);
         }
         // write cgff file end

         //get geneIDs that have multi-interval start
         Set tempGeneHashSet=new HashSet();
         if(cgffErr){
           Iterator geneKeyIterator = geneLocMap.keySet().iterator();
           while (geneKeyIterator.hasNext()) {
             Object currentKey = geneKeyIterator.next();
             Set geneHashSet = (HashSet) geneLocMap.get(currentKey);
             if (geneHashSet.size() == 1) continue;
             tempGeneHashSet.add(currentKey);
           }
         }
          //get geneIDs that have multi-interval end

         // write model file start
         try{
           FileWriter fwTranscript = new FileWriter(outPrefix+".model");
           Iterator transcriptKeyIterator=transcriptLocMap.keySet().iterator();
           while (transcriptKeyIterator.hasNext()){
             Object currentKey = transcriptKeyIterator.next();
             Set transcriptHashSet = (HashSet) transcriptLocMap.get(currentKey);

             // processing exon
             boolean transHaveExon=false;
             TreeSet<Interval> exonSet = (TreeSet) transcriptExonMap.get(currentKey);
             if(exonSet!=null){
               Interval.combineOverlap(exonSet);
               Interval.combineNearby(exonSet, 1);
               transHaveExon = true;
             }

             // skip a transcript if
             if(transcriptHashSet.size()>1){
               modelErr=true;
               continue;
             }

             // header
             fwTranscript.write(">" + currentKey + "\t");
             GenomeInterval transcriptInterval = (GenomeInterval) transcriptHashSet.iterator().next();
             fwTranscript.write(chrPrefix + transcriptInterval.getChr() + "\t");
             if (transHaveExon) {
               fwTranscript.write(exonSet.first().getStart() + "\t" +
                                  exonSet.last().getStop() + "\t" +
                                  transcriptInterval.getUserObject() + "\n");
             } else {
               fwTranscript.write(transcriptInterval.getStart() + "\t" +
                                  transcriptInterval.getStop() + "\t" +
                                  transcriptInterval.getUserObject() + "\n");
             }

             // exon
             if (transHaveExon) {
               Iterator ittranscriptExonIterator = exonSet.iterator();
               while (ittranscriptExonIterator.hasNext()) {
                 Interval transcriptExonInterval = (Interval) ittranscriptExonIterator.next();
                 fwTranscript.write(transcriptExonInterval.getStart() + "\t" +
                                    transcriptExonInterval.getStop() + "\n");
               }
             }else{
               fwTranscript.write(transcriptInterval.getStart() + "\t" +
                                  transcriptInterval.getStop() + "\n");
             }
           }
           fwTranscript.close();
         }
         catch (IOException ex){
           ex.printStackTrace();
         }
         // write model file end

         //get transcripts that have multi-interval start
         Set tempTransHashSet=new HashSet();
         if(modelErr){
             Iterator transcriptIterator = transcriptLocMap.keySet().iterator();
             while (transcriptIterator.hasNext()) {
                 Object currentKey = transcriptIterator.next();
                 Set transcriptHashSet = (HashSet) transcriptLocMap.get(currentKey);
                 if(transcriptHashSet.size() == 1) continue;
                 tempTransHashSet.add(currentKey);
             }
         }
         //get transcripts that have multi-interval end

         // write skipped rows start
         if(cgffErr || modelErr){
           try {
             FileWriter fwSolved = new FileWriter(outPrefix + ".solved");
             String headLine = "";
             BufferedReader fr = new BufferedReader(new FileReader(inFilename));
             Map geneIdLinesMap = new LinkedHashMap();

             while(fr.ready()){
               String line = fr.readLine();
               if(Character.isDigit(line.charAt(0))==false && headLine.length()==0){
                 headLine = line;
                 fwSolved.write(line + "\n");
                 continue;
               }
               String[] tokens = line.split("\t");
               String geneID = tokens[10].split(":")[1];
               if(tokens[12].equals(groupLabelType) &&
                  (tempTransHashSet.contains(tokens[13]) || tempGeneHashSet.contains(geneID))
                   ){
                 if(geneIdLinesMap.containsKey(geneID)){
                   String lines = ((String)geneIdLinesMap.get(geneID)) + line + "\n";
                   geneIdLinesMap.put(geneID,lines);
                 }else{
                   geneIdLinesMap.put(geneID,line + "\n");
                 }
               }else{
                 fwSolved.write(line + "\n");
               }
             }
             fr.close();
             fwSolved.close();

             FileWriter fwErr = new FileWriter(outPrefix + ".skipped");
             fwErr.write(headLine + "\n");
             for(Iterator iterator = geneIdLinesMap.keySet().iterator();iterator.hasNext();){
               Object key = iterator.next();
               String lines = (String) geneIdLinesMap.get(key);
               fwErr.write("\n" + lines);
             }
             fwErr.close();
           }
           catch (IOException ex){
             ex.printStackTrace();
             System.exit(1);
           }
         }
         // write skipped rows end
    }

    //parse seq_gene.md to get information we needed
    public static void parseSeqGeneMd(String seqMdFilename,String groupType,Map geneLocMap,Map geneExonMap,Map transcriptLocMap,Map transcriptExonMap)
   {
       HashSet geneSets = null;
       HashSet transcriptSets = null;
       Set<Interval> exonSets=null;
       Set<Interval> transcriptExonSets=null;
       Interval seqInterval=null;
       GenomeInterval geneInterval=null;
       GenomeInterval transInterval=null;

       try {
           BufferedReader fr = new BufferedReader(new FileReader(seqMdFilename));
           while (fr.ready()) {
               String line = fr.readLine();

               if(!(Character.isDigit(line.charAt(0))))  continue;

               String temp[]=line.split("\t");

               String chr=temp[1];
               int start = Integer.parseInt(temp[2]);
               int stop = Integer.parseInt(temp[3]);
               String strand = temp[4].intern();
               String featureID = temp[10];
               String featureType = temp[11];
               String groupLabel = temp[12];
               String transcript = temp[13];

               String[] splitGID=featureID.split(":");
               String newFeatureID=splitGID[1];

               if(!(groupLabel.equals(groupType))) continue;

               //gene processing start
               if (featureType.equals("GENE")){
            	   if (geneLocMap.containsKey(newFeatureID)){
            		   geneSets = (HashSet) geneLocMap.get(newFeatureID);
            	   }else{
            		   geneSets=new HashSet();
            	   }

            	   geneInterval=new GenomeInterval(chr,start,stop,true);
            	   geneInterval.setUserObject(strand);
            	   geneSets.add(geneInterval);
            	   geneLocMap.put(newFeatureID, geneSets);
               }else if(!featureType.equals("RNA") && !featureType.equals("PSEUDO")){
            	   if (geneExonMap.containsKey(newFeatureID)){
            		   exonSets = (TreeSet) geneExonMap.get(newFeatureID);
            	   }else{
            		   exonSets=new TreeSet<Interval>();
            	   }
                   seqInterval=new Interval(start,stop,true);
            	   exonSets.add(seqInterval);
            	   geneExonMap.put(newFeatureID, exonSets);
               }
               	//gene processing end

               //transcript processing start
               if(featureType.equals("RNA") && !transcript.equals("-")){
            	   if (transcriptLocMap.containsKey(transcript)){
            		   transcriptSets = (HashSet) transcriptLocMap.get(transcript);
            	   }else{
            		   transcriptSets=new HashSet();
            	   }

            	   transInterval=new GenomeInterval(chr,start,stop,true);
            	   transInterval.setUserObject(strand);
            	   transcriptSets.add(transInterval);
            	   transcriptLocMap.put(transcript, transcriptSets);
               }else if(!featureType.equals("RNA") && !transcript.equals("-")){
            	   if (transcriptExonMap.containsKey(transcript)){
            		   transcriptExonSets = (TreeSet) transcriptExonMap.get(transcript);
            	   }else{
            		   transcriptExonSets=new TreeSet<Interval>();
            	   }
                   seqInterval=new Interval(start,stop,true);
            	   transcriptExonSets.add(seqInterval);
            	   transcriptExonMap.put(transcript, transcriptExonSets);
               }
               //transcript processing end
           }
           fr.close();
       }
       catch (IOException ex)
       {
           ex.printStackTrace();
           System.exit(1);
       }
   }
}
