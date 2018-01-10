package statistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>Title: Gene Coverage Array Difference at C.D.F level</p>
 * <p>Description: This program can be used to calculate the biggest gap between
 *                 Control and Treatment smaple at C.D.F level</p>
 * <p>Input: 1. ControlArray which was the GeneCoverageArray outputed by Rackj<rnaseq.GeneCoverageArray>
 *           2. TreatmentArray which is the same as the above.
 * <p>Output: The Biggest gap between two gene coverage array at C.D.F level for each gene
 * @author Chun-Mao Fu
 * @author Wen-Dar Lin
 * @version 1.0
 */
public class GeneCoverageDiff {

  private static Map cGeneInfoMap = new HashMap();
  private static Map tGeneInfoMap = new HashMap();

  /****************************************************************************/
  /*                Declare input & Output filename                  */
  /****************************************************************************/
  private static String controlFilename = null;
  private static String treatmentFilename = null;
  private static String OutputFilename = null;

  /****************************************************************************/
  /*                   Parameter processing on command line                   */
  /****************************************************************************/
  static private void paraProc(String[] args) {

    // get parameter strings
    for (int i = 0; i < args.length; i++){
      if (args[i].equals("-C")) {
        controlFilename = args[i + 1];
        i++;
      }else if (args[i].equals("-T")) {
        treatmentFilename = args[i + 1];
        i++;
      }
      else if(args[i].equals("-O")){
          OutputFilename = args[i + 1];
          i++;
      }
    }

    // start to check if all necessary parameters are to be assigned
    if (controlFilename == null) {
      System.err.println("Control coverage array filename (-C) isn't assigned !");
      System.exit(1);
    }
    if (treatmentFilename == null) {
      System.err.println("Treatment coverage array filename (-T) isn't assigned !");
      System.exit(1);
    }
    if(OutputFilename==null){
        System.err.println("output filename (-O) not assigned");
        System.exit(1);
    }

    // List all of parameters when post-processing
    System.out.println("program: GeneCoverageDiff");
    System.out.println("Control coverage array filename (-C): " + controlFilename);
    System.out.println("Treatment coverage array filename (-T): " + treatmentFilename);
    System.out.println("output filename (-O): " + OutputFilename);
    System.out.println();
  }

  /****************************************************************************/
  /*                  Transform given array into C.D.F array                  */
  /****************************************************************************/
  static private double[] getCDFArray(String CoverageArrayStr){

    double sum=0, preSum = 0;
    String[] tmp = CoverageArrayStr.substring(1,CoverageArrayStr.length()-1).split(",");
    double array[] = new double[tmp.length];
    //translate string[] to double[] and calculate sum of array simultaneously
    for(int i=0; i<tmp.length; i++){
      array[i] = Double.parseDouble(tmp[i].trim());
      sum += array[i];
    }
    //transform the array into C.D.F array
    for(int j=0; j<array.length;j++){
      array[j] = array[j] / sum;
      preSum +=array[j];
      array[j] = preSum;
    }
    return array;
  }

  /****************************************************************************/
  /*       Calculate the biggest gap between C.D.F array for each gene        */
  /****************************************************************************/
  static private Map ksComputation(Map controlInfoMap, Map treatmentInfoMap){

    // get all genes in at least one CDF map
    Set geneSet = new HashSet(controlInfoMap.keySet());
    geneSet.addAll(treatmentInfoMap.keySet());

    Map resultMap = new TreeMap();

    for(Iterator iterator = geneSet.iterator();iterator.hasNext();){
        // for each gene
        String geneID = (String) iterator.next();

        // get info array
        Object[] controlInfo = (Object[])controlInfoMap.get(geneID);
        Object[] treatmentInfo = (Object[])treatmentInfoMap.get(geneID);

        // startPos
        int startPos;
        if(controlInfoMap.containsKey(geneID)){
            startPos = ((Integer)controlInfo[0]).intValue();
        }else{
            startPos = ((Integer)treatmentInfo[0]).intValue();
        }
        // uniCnt
        int cUniCnt = 0;
        int tUniCnt = 0;
        if(controlInfoMap.containsKey(geneID)){
            cUniCnt = ((Integer)controlInfo[1]).intValue();
        }
        if(treatmentInfoMap.containsKey(geneID)){
            tUniCnt = ((Integer)treatmentInfo[1]).intValue();
        }

        // cdf arrays
        double[] cCDFarray = null;
        double[] tCDFarray = null;
        if(controlInfoMap.containsKey(geneID)){
            cCDFarray = (double[])controlInfo[2];
        }
        if(treatmentInfoMap.containsKey(geneID)){
            tCDFarray = (double[])treatmentInfo[2];
        }
        if(cCDFarray==null){
            cCDFarray = new double[tCDFarray.length];
            Arrays.fill(cCDFarray,0);
        }
        if(tCDFarray==null){
            tCDFarray = new double[cCDFarray.length];
            Arrays.fill(tCDFarray,0);
        }


        // KS computation
        double BiggestGap = 0;
        int gapIndex = 0;
        for(int ind=0; ind<cCDFarray.length;ind++){
          if(Math.abs(cCDFarray[ind] - tCDFarray[ind])>BiggestGap){
            BiggestGap = Math.abs(cCDFarray[ind] - tCDFarray[ind]);
            gapIndex = ind;
          }
        }

        // info of KS computation
        Object[] ksInfo = new Object[6];
        ksInfo[0] = startPos;
        ksInfo[1] = cCDFarray.length;
        ksInfo[2] = gapIndex+startPos;
        ksInfo[3] = cUniCnt;
        ksInfo[4] = tUniCnt;
        ksInfo[5] = BiggestGap;

        resultMap.put(geneID,ksInfo);
    }

    return resultMap;
  }

  /****************************************************************************/
  /*                       Report the CDF array results                       */
  /****************************************************************************/
  static private void report(String outFilename,Map ksInfoMap) throws IOException {

    FileWriter Fw = new FileWriter(outFilename);
    //header
    Fw.write("#GeneID" + "\t" +
             "start" + "\t" +
             "length" + "\t" +
             "cnt1" + "\t" +
             "cnt2" + "\t" +
             "maxD pos" + "\t" +
             "D" + "\t" +
             "adjD" + "\n"
            );
    //content
    for(Iterator geneIterator = ksInfoMap.keySet().iterator();geneIterator.hasNext();){
      String geneID = geneIterator.next().toString();
      Object[] ksInfo = (Object[])ksInfoMap.get(geneID);
      int n1 = (Integer) ksInfo[3];
      int n2 = (Integer) ksInfo[4];
      double D = (Double) ksInfo[5];
      Fw.write(geneID + "\t" +
               ksInfo[0] + "\t" +
               ksInfo[1] + "\t" +
               ksInfo[3] + "\t" +
               ksInfo[4] + "\t" +
               ksInfo[2] + "\t" +
               ksInfo[5] + "\t" +
               D*Math.sqrt(((double)n1*n2)/(n1+n2)) + "\n"
              );
    }
    Fw.close();
  }

  private static void readGeneInfoMap(Map geneInfoMap,String filename) throws
            IOException {
      BufferedReader fr = new BufferedReader(new FileReader(filename));
      while(fr.ready()){
        String line = fr.readLine();
        if(line.startsWith("#")) continue;
        else{
          String[] tokens = line.split("\t");
          Object[] infoArray = new Object[3];
          String geneID = tokens[0];
          int startPos = Integer.parseInt(tokens[1]);
          int uniCnt = Integer.parseInt(tokens[2]);
          double[] cdfArray = getCDFArray(tokens[3]);
          infoArray[0] = startPos;
          infoArray[1] = uniCnt;
          infoArray[2] = cdfArray;
          geneInfoMap.put(geneID, infoArray);
        }
      }
      fr.close();
  }

  /****************************************************************************/
  /*                               Main function                              */
  /****************************************************************************/
  public static void main(String args[]) throws IOException {

    paraProc(args);

    //read control and treatment data
    readGeneInfoMap(cGeneInfoMap,controlFilename);
    readGeneInfoMap(tGeneInfoMap,treatmentFilename);

    Map geneKsInfoMap = ksComputation(cGeneInfoMap,tGeneInfoMap);
    report(OutputFilename,geneKsInfoMap);
  }
}
