package special;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import misc.CanonicalGFF;
import misc.GenomeInterval;

public class GeneQuery {

  // output
  private static String inFilename = null;
  private static String outFilename = null;
  private static String gffFilename = null;
  private static Boolean useExonDefault = true;
  private static Boolean byContainDefault = true;
  private static int minimumOverlapDefault = 1;

  static private void paraProc(String[] args) {
    int i;

    // get parameter strings
    for (i = 0; i < args.length; i++) {
      if (args[i].equals("-I")) {
        inFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-O")) {
        outFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-GFF")) {
        gffFilename = args[i + 1];
        i++;
      }
      else if (args[i].equals("-exon")) {
        useExonDefault = Boolean.valueOf(args[i + 1]);
        i++;
      }
      else if(args[i].equals("-contain")){
          byContainDefault = Boolean.valueOf(args[i+1]);
          i++;
      }
      else if(args[i].equals("-min")){
          minimumOverlapDefault = Integer.parseInt(args[i + 1]);
          i++;
      }
    }

    // check for necessary parameters
    if (gffFilename == null) {
      System.err.println("canonical GFF filename (-GFF) not assigned");
      System.exit(1);
    }
  }

  static public void main(String args[]) throws IOException {
    paraProc(args);

    CanonicalGFF cgff = new CanonicalGFF(gffFilename);
    BufferedReader fr = null;
    Writer fw = null;

    try {
      if (outFilename==null) {
    	  fw = new BufferedWriter(new OutputStreamWriter(System.out));
      }else{
    	  fw = new FileWriter(outFilename);
      }
      if (inFilename==null) {
    	  fr = new BufferedReader(new InputStreamReader(System.in));
      }else{
    	  fr = new BufferedReader(new FileReader(inFilename));
      }
      String line;
      while ( (line = fr.readLine()) != null) {
        String[] token = line.split("\\s");

        boolean useExon = useExonDefault;
        boolean byContain = byContainDefault;
        int minimumOverlap = minimumOverlapDefault;

        Pattern pattern;
        Matcher matcher;
        Set hitRegions = null;

        // parse query options
        for(int i=1;i<=3 && i<token.length;i++){
          if(token[i].length()==0) continue;
          switch(i){
            case 1: // useExon
              useExon=Boolean.parseBoolean(token[1]);
              break;
            case 2:
              byContain=Boolean.parseBoolean(token[2]);
              break;
            case 3:
              minimumOverlap=Integer.parseInt(token[3]);
              break;
            default:
              break;
          }
        }

        // some priority must be set for these two forms
        // matching, form 1
        pattern = Pattern.compile("^(.+):(\\d+)$");
        matcher = pattern.matcher(token[0]);
        if(matcher.matches()){
            String chr = matcher.group(1).toLowerCase();
            int position = Integer.parseInt(matcher.group(2));
            fw.write(matcher.group(1) + "\t" +
                             matcher.group(2) + "\t");
            hitRegions = cgff.getRelatedGenes(chr,position,position,useExon,byContain,minimumOverlap);
        }
        // matching, form 2
        pattern = Pattern.compile("^(.+)\\((\\d+),(\\d+)\\)$");
        matcher = pattern.matcher(token[0]);
        if(matcher.matches()){
            String chr = matcher.group(1).toLowerCase();
            int position1 = Integer.parseInt(matcher.group(2));
            int position2 = Integer.parseInt(matcher.group(3));
            if(position1==position2){
            	fw.write(matcher.group(1) + "\t" +
                               matcher.group(2) + "\t");
            }else{
            	fw.write(matcher.group(1) + "\t" +
                               matcher.group(2) + "\t" +
                               matcher.group(3) + "\t");
            }
            hitRegions = cgff.getRelatedGenes(chr,position1,position2,useExon,byContain,minimumOverlap);
        }

        // reporting
        if(hitRegions!=null && hitRegions.size()>0){
            for(Iterator iterator=hitRegions.iterator();iterator.hasNext();){
                GenomeInterval gi = (GenomeInterval) iterator.next();
                String strand = cgff.getStrand(gi.getUserObject());
                fw.write(gi.getUserObject() + "(" + strand + ")");
                if(iterator.hasNext()){
                	fw.write(",");
                }
            }
        }else{
        	fw.write("null");
        }
        fw.write("\n");
        fw.flush();
      }
      fw.close();
      fr.close();
    }
    catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }
}
