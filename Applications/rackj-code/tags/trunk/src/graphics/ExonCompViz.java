package graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import misc.CanonicalGFF;
import misc.GenomeInterval;
import misc.Interval;

public class ExonCompViz extends JPanel {
    // parameters
    public int startPos;
    public int stopPos;
    public int length;
    public int sizePt;
    public int scale;
    public int fontSize;
    public int markerHeight;
    public boolean heatRG;

    // drawing related
    static int border = 10;

    private Color backGroundColor = Color.white;
    private Color bandColor = Color.black;

    private Font font = null;

    // data
    private Set canonicalExonRegions = new HashSet();
    private Map treatmentExonDeviationMap = new HashMap();
    private Map modelExonSetArrayMap = new HashMap();

    public ExonCompViz(int startPos, int stopPos, int sizePt, int scale,
                       int fontSize, int markerHeight, String fontString,
                       boolean heatRG) {
      super();
      setDimension(startPos,stopPos,sizePt,scale);
      this.markerHeight = markerHeight;
      this.fontSize = fontSize;
      this.heatRG = heatRG;
      font = new Font(fontString, Font.PLAIN, fontSize);
    }

    public void setDimension(int startPos, int stopPos, int sizePt, int scale) {
      this.startPos = startPos;
      this.stopPos = stopPos;
      this.sizePt = sizePt;
      this.scale = scale;
      this.length = stopPos - startPos;
    }

    protected void paintComponent(Graphics g){
      if (isOpaque()) { //paint background
          g.setColor(backGroundColor);
          g.fillRect(0, 0, getWidth(), getHeight());
      }

      if (true) {
          Graphics2D g2d = (Graphics2D)g.create();

          // drawing
          int base;
          base = border;
          drawScaleNumbers(g2d,base);
          base = border+fontSize;
          drawScale(g2d,base);
          base = border+fontSize+markerHeight;
          // draw treatment
          base = drawTreatmentDeviation(g2d,base);
          // draw model
          base = drawModelInfo(g2d,base);

          g2d.dispose(); //clean up
      }
    }

    private int drawTreatmentDeviation(Graphics2D g2d,int base){
      g2d.setFont(font);
      FontMetrics metrics = g2d.getFontMetrics(font);

      Iterator iterator1,iterator2;
      int i;

      if(canonicalExonRegions==null && treatmentExonDeviationMap==null) return base;
      for(iterator1 = treatmentExonDeviationMap.keySet().iterator();iterator1.hasNext();){
          String treatment = (String) iterator1.next();
          // name
          g2d.setColor(bandColor);
          g2d.drawString(treatment,
                         border,
                         base + fontSize);
          base += fontSize;
          // drawing
          drawScale(g2d, base + markerHeight / 2 + 1);
          double[] deviation = (double[]) treatmentExonDeviationMap.get(treatment);
          int deviationIndex = 1;
          for (iterator2 = canonicalExonRegions.iterator(); iterator2.hasNext(); ) {
              // get position
              Interval pos = (Interval) iterator2.next();
              int pos1 = pos.getStart();
              int pos2 = pos.getStop();
              // transfer to relative position
              pos1 -= startPos;
              pos2 -= startPos;
              // transfer to position in Pt
              pos1 = bp2pt(pos1, sizePt, length);
              pos2 = bp2pt(pos2, sizePt, length);
              // draw rect
              g2d.setColor(bandColor);
              g2d.drawRect(border + pos1,
                           base,
                           pos2 - pos1,
                           markerHeight);
              // fill rect
              if(deviation[deviationIndex]!=Double.NaN){
                  float diff = (float) Math.pow(deviation[deviationIndex],2);
                  if(diff>255) diff = 255;
                  diff /= 255.0;
                  if(heatRG){
                      if (deviation[deviationIndex] < 0) {
                          g2d.setColor(new Color(1 - diff, 1f, 0f));
                      } else {
                          g2d.setColor(new Color(1f, 1 - diff, 0f));
                      }
                  }else{
                      if (deviation[deviationIndex] > 0) {
                          g2d.setColor(new Color(1 - diff, 1f, 0f));
                      } else {
                          g2d.setColor(new Color(1f, 1 - diff, 0f));
                      }
                  }
              }else{
                  g2d.setColor(Color.gray);
              }
              g2d.fillRect(border + pos1 + 1,
                           base + 1,
                           pos2 - pos1 - 1,
                           markerHeight - 1);
              // misc
              deviationIndex++;
          }
          base += (markerHeight * 2 + 1);
      }

      return base;
    }

    private int drawModelInfo(Graphics2D g2d,int base){
      g2d.setColor(bandColor);
      g2d.setFont(font);
      FontMetrics metrics = g2d.getFontMetrics(font);

      Iterator iterator1,iterator2;
      int i;

      if(modelExonSetArrayMap==null) return base;
      for(iterator1 = modelExonSetArrayMap.keySet().iterator();iterator1.hasNext();){
        String name = (String) iterator1.next();
        // name
        g2d.drawString(name,
                       border,
                       base + fontSize);
        base += fontSize;
        // drawing
        drawScale(g2d,base+markerHeight/2+1);
        Set locationSetArray[] = (Set[]) modelExonSetArrayMap.get(name);
        for(i=0;i<2;i++){
          if(locationSetArray[i]==null) continue;
          for(iterator2 = locationSetArray[i].iterator();iterator2.hasNext();){
            // get position
            Interval pos = (Interval) iterator2.next();
            int pos1 = pos.getStart();
            int pos2 = pos.getStop();
            // transfer to relative position
            pos1 -= startPos;
            pos2 -= startPos;
            // transfer to position in Pt
            pos1 = bp2pt(pos1,sizePt,length);
            pos2 = bp2pt(pos2,sizePt,length);
            // draw
            g2d.drawRect(border+pos1,
                         base + i*(markerHeight+2),
                         pos2-pos1,
                         markerHeight);
          }
        }
        base += (markerHeight * 2 + 1);
      }

      return base;
    }

    private void drawScale(Graphics2D g2d,int base) {
      g2d.setColor(bandColor);
      g2d.setFont(font);
      FontMetrics metrics = g2d.getFontMetrics(font);

      g2d.drawLine(border,
                   base + markerHeight/2,
                   border + sizePt,
                   base + markerHeight/2);

      int i;
      int lastWordPos = 0 - border*2;
      for (i = 0; scale * i <= length; i++) {
        g2d.drawLine(border + bp2pt(scale*i,sizePt,length),
                     base,
                     border + bp2pt(scale*i,sizePt,length),
                     base + markerHeight);
      }
    }

    private void drawScaleNumbers(Graphics2D g2d,int base) {
      g2d.setColor(bandColor);
      g2d.setFont(font);
      FontMetrics metrics = g2d.getFontMetrics(font);

      int i;
      int lastWordPos = 0 - border*2;
      for (i = 0; scale * i <= length; i++) {
        if(border + bp2pt(scale*i,sizePt,length) < lastWordPos) continue;
        if ( ( (startPos + scale * i) % GraphicsMisc.oneM) == 0 && (startPos + scale * i)!=0) {
          g2d.drawString( (startPos + scale * i) / GraphicsMisc.oneM + "M",
                         border + bp2pt(scale*i,sizePt,length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale*i,sizePt,length) + metrics.stringWidth((startPos + scale * i) / GraphicsMisc.oneM + "M") + border;
        }else if ( ( (startPos + scale * i) % GraphicsMisc.oneK) == 0 && (startPos + scale * i)!=0){
          g2d.drawString( (startPos + scale * i) / GraphicsMisc.oneK + "K",
                         border + bp2pt(scale*i,sizePt,length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale*i,sizePt,length) + metrics.stringWidth((startPos + scale * i) / GraphicsMisc.oneK + "K") + border;
        }else{
          g2d.drawString(""+(startPos + scale * i),
                         border + bp2pt(scale*i,sizePt,length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale*i,sizePt,length) + metrics.stringWidth(""+(startPos + scale * i)) + border;
        }
      }
    }

    private int bp2pt(int bp,int sizePt,int lengthBp){
      if(Integer.MAX_VALUE/sizePt > bp){
        return bp * sizePt / lengthBp;
      }else{
        return (int) Math.round(((double)bp / (double)lengthBp) * sizePt);
      }
    }

    public void setData(Set canonicalExonRegions,Map treatmentExonDeviationMap,Map modelExonSetArrayMap){
      this.canonicalExonRegions = new TreeSet();
      this.canonicalExonRegions.addAll(canonicalExonRegions);
      this.treatmentExonDeviationMap = treatmentExonDeviationMap;
      this.modelExonSetArrayMap = modelExonSetArrayMap;
    }

    public void adjustSize(){
      // width
      FontMetrics metrics = this.getFontMetrics(font);

      int i;
      int lastWordPos = 0 - border*2;
      for (i = 0; scale * i <= length; i++) {
        if(border + sizePt*scale*i/length < lastWordPos) continue;
        if ( ( (startPos + scale * i) % GraphicsMisc.oneM) == 0 && (startPos + scale * i)!=0) {
          lastWordPos = border + sizePt*scale*i/length + metrics.stringWidth((startPos + scale * i) / GraphicsMisc.oneM + "M") + border;
        }else if ( ( (startPos + scale * i) % GraphicsMisc.oneK) == 0 && (startPos + scale * i)!=0){
          lastWordPos = border + sizePt*scale*i/length + metrics.stringWidth((startPos + scale * i) / GraphicsMisc.oneK + "K") + border;
        }else{
          lastWordPos = border + sizePt*scale*i/length + metrics.stringWidth(""+(startPos + scale * i)) + border;
        }
      }
      lastWordPos -= border;

      // height
      int height = 0;
      // border
      height += (2*border);
      // default scale
      height += (fontSize + markerHeight);
      // treatment
      if(treatmentExonDeviationMap!=null){
          // treatment names
          height += (treatmentExonDeviationMap.size() * fontSize);
          // drawings
          height += ( (markerHeight * 2 + 1) * treatmentExonDeviationMap.size());
      }
      // model
      if(modelExonSetArrayMap!=null){
          // gene/model names
          height += (modelExonSetArrayMap.size() * fontSize);
          // drawings
          height += ((markerHeight * 2 + 1) * modelExonSetArrayMap.size());
      }

      this.setSize(lastWordPos+border,height);
    }

    private static ArrayList sourceFilenameList = new ArrayList();
    private static String baseFilename = null;
    private static int minimumCount = 2;

    private static String gffFilename = null;
    private static String modelFilename = null;

    private static String outputPrefix = null;

    private static Set traceGeneList = new LinkedHashSet();
    private static String traceFilename = null;

    private static boolean inHeatRG = true;
    private static int inSizePt = 1000;
    private static String inFontString = "Courier";

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-model")){
                modelFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-O")){
                outputPrefix = args[i + 1];
                i++;
            }
            else if (args[i].equals("-S")) {
                sourceFilenameList.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-B")){
                baseFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-trace")){
                traceGeneList.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-traceFile")){
                traceFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-size")){
              inSizePt = Integer.parseInt(args[i+1]);
              i++;
            }
            else if(args[i].equals("-font")){
              inFontString = args[i+1];
              i++;
            }
            else if(args[i].equals("-RG")){
              inHeatRG = Boolean.valueOf(args[i+1]);
              i++;
            }
        }

        // check for necessary parameters
        if(gffFilename==null){
          System.err.println("canonical GFF filename (-GFF) not assigned");
          System.exit(1);
        }
        if(outputPrefix==null){
            System.err.println("output prefix (-O) not assigned");
            System.exit(1);
        }
        if(sourceFilenameList.size()<=0){
          System.err.println("source table(s) (-S) not assigned");
          System.exit(1);
        }
        if(baseFilename==null){
            System.err.println("base table (-B) not assigned");
            System.exit(1);
        }
        if(traceGeneList.size()==0 && traceFilename==null){
            System.err.println("tracing gene list (-trace) empty AND traceing gene list in file (-traceFile) not assigned");
            System.exit(1);
        }

        // list parameters
        System.out.println("program: ExonCompViz");
        System.out.println("canonical GFF filename (-GFF): "+gffFilename);
        System.out.println("model filename (-model): "+ modelFilename);
        System.out.println("output prefix (-O): "+outputPrefix);
        System.out.println("source table(s) (-S): "+sourceFilenameList);
        System.out.println("base table (-B): "+baseFilename);
        System.out.println("tracing gene list (-trace): " + traceGeneList);
        System.out.println("traceing gene list in file (-traceFile): " + traceFilename);
        System.out.println("plot size (-size): " + inSizePt);
        System.out.println("font (-font): " + inFontString);
        System.out.println("red for over-expression, green for under-expression (-RG): " + inHeatRG);
        System.out.println();

        // post-processing
        if(traceFilename!=null){
            try {
                BufferedReader fr = new BufferedReader(new FileReader(traceFilename));

                while(fr.ready()){
                    String line = fr.readLine();
                    traceGeneList.add(line.trim());
                }

                fr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }

    public static void main(String args[]) throws IOException {
        System.setProperty("java.awt.headless", "true");
        paraProc(args);

        // STEP 0: preparation
        // ensure the output prefix
        File path = new File(outputPrefix);
        if(outputPrefix.endsWith(System.getProperty("file.separator"))==false) path = path.getParentFile();
        if(path!=null) path.mkdirs();

        // STEP 1: read files: CGFF, model, exonCount files
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF geneModel = null;
        if(modelFilename!=null){
            geneModel = new CanonicalGFF(modelFilename);
        }

        // STEP 2: compute deviation for each input gene
        // read  geneExonCountMap for control and treatments
        Map geneExonCountMapControl = statistics.CountComparator.readGroupCountMap(baseFilename);
        Map treatmentGeneExonCountMap = new LinkedHashMap();
        for(Iterator iterator = sourceFilenameList.iterator();iterator.hasNext();){
            String filename = (String) iterator.next();
            Map map = statistics.CountComparator.readGroupCountMap(filename);
            File file = new File(filename);
            filename = file.getName();
            int lastDotIndex = filename.lastIndexOf(".");
            String treatmentName = filename.substring(0,lastDotIndex);
            treatmentGeneExonCountMap.put(treatmentName,map);
        }
        // compute deviation for each exon for each input gene
        Map geneTreatmentExonDeviationMap = new HashMap();
        for(Iterator geneIterator = traceGeneList.iterator();geneIterator.hasNext();){
            String geneID = (String) geneIterator.next();
            // get control exonCountMap
            Map controlExonCountMap = (Map) geneExonCountMapControl.get(geneID);
            // for each treatment
            for(Iterator treatmentIterator = treatmentGeneExonCountMap.keySet().iterator();treatmentIterator.hasNext();){
                String treatment = (String) treatmentIterator.next();
                // now we have controlExonCountMap and treatmentExonCountMap
                Map treatmentExonCountMap = (Map) ((Map) treatmentGeneExonCountMap.get(treatment)).get(geneID);
                // get number of canonical exons
                int exonCnt = ((Set)cgff.geneExonRegionMap.get(geneID)).size();
                // compute deviation
                double[] deviation = computeDeviation(controlExonCountMap,treatmentExonCountMap,exonCnt);
                // PUT
                Map treatmentExonDeviationMap;
                if(geneTreatmentExonDeviationMap.containsKey(geneID)){
                    treatmentExonDeviationMap = (Map) geneTreatmentExonDeviationMap.get(geneID);
                }else{
                    treatmentExonDeviationMap = new LinkedHashMap();
                    geneTreatmentExonDeviationMap.put(geneID,treatmentExonDeviationMap);
                }
                treatmentExonDeviationMap.put(treatment,deviation);
            }
        }

        // STEP 3: make pictures
        // for each gene
        for(Iterator geneIterator = traceGeneList.iterator();geneIterator.hasNext();){
            String geneID = (String) geneIterator.next();
            Map treatmentExonDeviationMap = (Map) geneTreatmentExonDeviationMap.get(geneID);
            // get canonical exon regions
            Set[] exonSetArray = new Set[2];
            Map canonicalExonSetArrayMap = new LinkedHashMap();
            Set canonicalExonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
            exonSetArray[0] = canonicalExonRegions; exonSetArray[1] = null;
            canonicalExonSetArrayMap.put(geneID,exonSetArray);
            // get corresponding gene models
            Map modelExonSetArrayMap = new LinkedHashMap();
            if(geneModel!=null){
                String chr = (String) ((GenomeInterval)cgff.geneRegionMap.get(geneID)).getChr();
                Set modelSet = geneModel.getRelatedGenes(chr,canonicalExonRegions,false,false,1,false);
                for(Iterator modelIterator = modelSet.iterator();modelIterator.hasNext();){
                    GenomeInterval modelGene = (GenomeInterval) modelIterator.next();
                    String modelGeneID = (String) modelGene.getUserObject();
                    Set modelExonSet = (Set) geneModel.geneExonRegionMap.get(modelGeneID);
                    exonSetArray = new Set[2];
                    exonSetArray[0] = modelExonSet; exonSetArray[1] = null;
                    modelExonSetArrayMap.put(modelGeneID,exonSetArray);
                }
            }
            // decide its plot start (in bp), stop (in bp), and scale
            int[] settingArray = ReadViz.autoSetting(canonicalExonSetArrayMap,null,false);
            int plotStart = settingArray[0];
            int plotStop = settingArray[1];
            int plotScale = settingArray[2];
            // draw
            ExonCompViz cop = new ExonCompViz(plotStart,plotStop,inSizePt,plotScale,12,10,inFontString,inHeatRG);
            cop.setData(canonicalExonRegions,treatmentExonDeviationMap,modelExonSetArrayMap);
            cop.adjustSize();
            BufferedImage image = new BufferedImage(cop.getWidth(),
                                                    cop.getHeight(),
                                                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            cop.paintComponent(g2);
            javax.imageio.ImageIO.write(image, "jpeg",new File(outputPrefix + geneID + ".jpg"));
        }
    }

    private static double[] computeDeviation(Map controlCountMap,Map treatmentCountMap,int exonCnt){
        double[] deviation = new double[exonCnt+1];

        // prepare arrays
        // determine number of effective datapoints
        Set exonIndexSet = new HashSet();
        exonIndexSet.addAll(controlCountMap.keySet());
        exonIndexSet.addAll(treatmentCountMap.keySet());
        // fill arrays and find max for both arrays
        float[] controlCounts = new float[exonIndexSet.size()];
        float[] treatmentCounts = new float[exonIndexSet.size()];
        float controlMax = 0 , treatmentMax = 0;
        Map indexMap = new HashMap(); // a exonIndex-fillingIndex map
        int fillingIndex = 0;
        for(Iterator iterator = exonIndexSet.iterator();iterator.hasNext();){
            String exonString = (String) iterator.next();
            Integer exonIndex = new Integer(exonString);
            if(controlCountMap.containsKey(exonString)){
                controlCounts[fillingIndex] = (Float) controlCountMap.get(exonString);
                if(controlCounts[fillingIndex] > controlMax) controlMax = controlCounts[fillingIndex];
            }else{
                controlCounts[fillingIndex] = 0;
            }
            if(treatmentCountMap.containsKey(exonString)){
                treatmentCounts[fillingIndex] = (Float) treatmentCountMap.get(exonString);
                if(treatmentCounts[fillingIndex] > treatmentMax) treatmentMax = treatmentCounts[fillingIndex];
            }else{
                treatmentCounts[fillingIndex] = 0;
            }
            indexMap.put(exonIndex,fillingIndex);
            fillingIndex++;
        }
        // do 100-normalization
        for(int i=0;i<controlCounts.length;i++){
            controlCounts[i] = controlCounts[i]/controlMax*100;
            treatmentCounts[i] = treatmentCounts[i]/treatmentMax*100;
        }
        // linear regression, comptue M and B (y = Mx + B)
        double sigmaXY = 0 , sigmaX = 0 , sigmaY = 0 , sigmaXX = 0;
        for(int i=0;i<controlCounts.length;i++){
            double X = controlCounts[i];
            double Y = treatmentCounts[i];
            sigmaXY += (X*Y);
            sigmaX += X;
            sigmaY += Y;
            sigmaXX += (X*X);
        }
        double linestM = (controlCounts.length * sigmaXY - sigmaX * sigmaY) / (controlCounts.length * sigmaXX - sigmaX * sigmaX);
        double linestB = sigmaY/controlCounts.length - linestM * sigmaX / controlCounts.length;
        // compute projection to the regression line, and the distance (i.e. the deviation)
        double orthogonalM = -1/linestM;
        double[] distance = new double[controlCounts.length];
        for(int i=0;i<controlCounts.length;i++){
            double orthogonalB = treatmentCounts[i] - orthogonalM * controlCounts[i];
            double projectionX = (orthogonalB - linestB) / (linestM - orthogonalM);
            double projectionY = linestM * projectionX + linestB;
            distance[i] = Math.sqrt(Math.pow(controlCounts[i]-projectionX,2)+Math.pow(treatmentCounts[i]-projectionY,2));
            if(treatmentCounts[i] > (linestM * controlCounts[i] + linestB)){
                // do nothing
            }else{
                distance[i] *= -1;
            }
        }
        for(int exonIndex=1;exonIndex<=exonCnt;exonIndex++){
            if(indexMap.containsKey(exonIndex)){
                deviation[exonIndex] = distance[(Integer)indexMap.get(exonIndex)];
            }else{
                deviation[exonIndex] = Double.NaN;
            }
        }

        return deviation;
    }
}
