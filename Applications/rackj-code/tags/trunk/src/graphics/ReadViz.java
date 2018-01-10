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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;

import misc.Interval;
import misc.Util;
import misc.intervaltree.IntervalTree;

public class ReadViz extends JPanel {

  class ReadMapComparator implements Comparator {

    private Map srcReadMap=null;
    private boolean forward;

    ReadMapComparator(Map srcReadMap, boolean forward){
      this.srcReadMap = srcReadMap;
      this.forward = forward;
    }

    public int compare(Object o1, Object o2) {
      int id1 = ( (Integer) o1).intValue();
      int id2 = ( (Integer) o2).intValue();
      Interval interval1 = (Interval) srcReadMap.get(o1);
      Interval interval2 = (Interval) srcReadMap.get(o2);
      // let splice reads (with Set user objects) former
      if (interval1.getUserObject() instanceof Set &&
          interval2.getUserObject() instanceof Object[]) {
        return -1;
      }
      else if (interval1.getUserObject() instanceof Set &&
               interval2.getUserObject() instanceof Set) {

        //use arraylist to store interval data with specific ordering
        ArrayList al1 = new ArrayList();
        ArrayList al2 = new ArrayList();

        if(forward){
          for (Iterator interval1It = ( (Set) interval1.getUserObject()).iterator();
               interval1It.hasNext(); ) {
            Interval thisInterval1 = (Interval) interval1It.next();
            al1.add(thisInterval1.getStart());
            al1.add(thisInterval1.getStop());
          }
          al1.add(al1.size() - 1, al1.get(0));
          al1.remove(0);
          for (Iterator interval2It = ( (Set) interval2.getUserObject()).iterator();
               interval2It.hasNext(); ) {
            Interval thisInterval2 = (Interval) interval2It.next();
            al2.add(thisInterval2.getStart());
            al2.add(thisInterval2.getStop());
          }
          al2.add(al2.size() - 1, al2.get(0));
          al2.remove(0);
        }else{ // reverse
          for (Iterator interval1It = ( (Set) interval1.getUserObject()).iterator();
               interval1It.hasNext(); ) {
            Interval thisInterval1 = (Interval) interval1It.next();
            al1.add(0,thisInterval1.getStart());
            al1.add(0,thisInterval1.getStop());
          }
          al1.add(al1.size() - 1, al1.get(0));
          al1.remove(0);
          for (Iterator interval2It = ( (Set) interval2.getUserObject()).iterator();
               interval2It.hasNext(); ) {
            Interval thisInterval2 = (Interval) interval2It.next();
            al2.add(0,thisInterval2.getStart());
            al2.add(0,thisInterval2.getStop());
          }
          al2.add(al2.size() - 1, al2.get(0));
          al2.remove(0);
        }

        //compare elements of two array
        int ans=0;
        for (int i = 0; i < al1.size(); i++) {
          try {
            if ( (Integer) al1.get(i) < (Integer) al2.get(i)) {
              ans = -1;
              break;
            }
            else if ( (Integer) al1.get(i) > (Integer) al2.get(i)) {
              ans = 1;
              break;
            }
          }
          catch (IndexOutOfBoundsException ex) {
            if (i >= al1.size()) {
              ans = -1;
              break;
            }
            else {
              ans = 1;
              break;
            }
          }
        }
        if(forward){
          // do nothing
        }else{
          ans *= (-1);
        }
        if(ans==0){
          return id1 - id2;
        }else{
          return ans;
        }
      }
      else if (interval1.getUserObject() instanceof Object[] &&
               interval2.getUserObject() instanceof Set) {
        return 1;
      }
      else {
        // array vs array
        Object[] intervalInfo1 = (Object[]) interval1.getUserObject();
        Object[] intervalInfo2 = (Object[]) interval2.getUserObject();
        String itemName1 = (String) intervalInfo1[0];
        String itemName2 = (String) intervalInfo2[0];
        if (itemName1.equals(itemName2)) {
          // do nothing
        }
        else {
          if (itemName1.equals(Util.uniqString))
            return -1;
          else
            return 1;
        }
      }
      if (interval1.compareTo(interval2) != 0) {
        int ans = interval1.compareTo(interval2);
        if(forward){
          return ans;
        }else{
          return ans*(-1);
        }
      }
      else {
        return id1 - id2;
      }
    }

    public boolean equals(Object obj) {
      return false;
    }
  }

  // parameters
  public int startPos;
  public int stopPos;
  public int length;
  public int sizePt;
  public int scale;
  public int fontSize;
  public int markerHeight;
  public int readHeight;

  // drawing related
  static int border = 10;

  private Color backGroundColor = Color.white;
  private Color bandColor = Color.black;
  private Color uniqReadColor = Color.green;
  private Color spliceReadColor = Color.red;
  private Color spliceBandColor = Color.gray;
  private Color multiReadColor = Color.orange;

  private Font font = null;

  // mode
  private String outputMode=null;

  // direction
  private boolean forwardDirection=true;

  // data
  private Map exonMap = null;
  private Map readMap = null;
  private Map levelReadMap = new TreeMap();

  public ReadViz(int startPos, int stopPos, int sizePt, int scale,
                               int fontSize, int markerHeight, int readHeight, String fontString, String outputMode, boolean forward) {
    super();
    setDimension(startPos,stopPos,sizePt,scale);
    this.markerHeight = markerHeight;
    this.readHeight = readHeight;
    this.fontSize = fontSize;
    font = new Font(fontString, Font.PLAIN, fontSize);
    this.outputMode = outputMode;
    this.forwardDirection = forward;
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

    if((this.outputMode).toLowerCase().equals("mapping")) {
        Graphics2D g2d = (Graphics2D)g.create();
        // drawing
        int base;
        base = border;
        drawScaleNumbers(g2d,base);
        base = border+fontSize;
        drawScale(g2d,base);
        base = border+fontSize+markerHeight;
        base = drawExonInfo(g2d,base);
        base = drawReadInfo(g2d,base);
        //clean up
        g2d.dispose();

    }else{ // if outputMode is Depth or Depthlog
        Graphics2D g2d = (Graphics2D)g.create();
        // drawing
        int base;
        base = border;
        drawScaleNumbers(g2d,base);
        base = border+fontSize;
        drawScale(g2d,base);
        base = border+fontSize+markerHeight;
        base = drawExonInfo(g2d,base);
        drawReadCoverage(g2d,base,this.outputMode,this.readHeight);
        //clean up
        g2d.dispose();
    }
  }

  private void drawReadCoverage(Graphics2D g2d,int base,String outputMode, int readHeight){

    //get depth array
    double[] depthArray = getDepthArray(this.startPos, this.stopPos, readMap, outputMode);
    //fetch maximum depth
    double maxDepth = computeMaxDepth(depthArray);

    //drawing depth of array
    g2d.setColor(Color.red);
    int  drawPos, curDepth=0, preDepth=0;
    if(forwardDirection){
      for (int ind = 0; ind < depthArray.length; ind++) {
        //adjust position
        drawPos = bp2pt(ind, sizePt, length) + border;

        if (outputMode.toLowerCase().equals("depth")) {
          curDepth = (int) depthArray[ind] * readHeight;
        }
        else if (outputMode.toLowerCase().equals("depthlog")) {
          curDepth = (int) (depthArray[ind] * readHeight * 50);
          //if curDepth less than 0, skip
          if (curDepth < 0) {
            preDepth = 0;
            continue;
          }
        }

        g2d.drawLine(drawPos, preDepth + base, drawPos, curDepth + base);
        //store curHeight for next iteration
        preDepth = curDepth;
        //make graph more friendly
        if (ind <= depthArray.length - 2) {
          if (depthArray[ind + 1] < 0)
            g2d.drawLine(drawPos, curDepth + base, drawPos, base);
        }
        else if (ind == depthArray.length - 1) {
          g2d.drawLine(drawPos, base, drawPos, curDepth + base);
        }
      }
    }else{ // reverse
      for (int ind = 0; ind < depthArray.length; ind++) {
        //adjust position
        drawPos = bp2pt(ind, sizePt, length) + border;

        if (outputMode.toLowerCase().equals("depth")) {
          curDepth = (int) depthArray[depthArray.length-1-ind] * readHeight;
        }
        else if (outputMode.toLowerCase().equals("depthlog")) {
          curDepth = (int) (depthArray[depthArray.length-1-ind] * readHeight * 50);
          //if curDepth less than 0, skip
          if (curDepth < 0) {
            preDepth = 0;
            continue;
          }
        }

        g2d.drawLine(drawPos, preDepth + base, drawPos, curDepth + base);
        //store curHeight for next iteration
        preDepth = curDepth;
        //make graph more friendly
        if (ind <= depthArray.length - 2) {
          if (depthArray[depthArray.length-1-ind - 1] < 0)
            g2d.drawLine(drawPos, curDepth + base, drawPos, base);
        }
        else if (ind == depthArray.length - 1) {
          g2d.drawLine(drawPos, base, drawPos, curDepth + base);
        }
      }
    }

    //compute scale's position
    int vScalePos = border + sizePt + border;
    //initialize variables
    int scaleRegion, scaleDepth;
    int lastWordPos = base - 2 * border;
    //set scale's color
    g2d.setColor(Color.BLACK);

    if(outputMode.toLowerCase().equals("depth")){

      //compute how depth should be drawn
      scaleRegion = 10 * readHeight;
      scaleDepth = (int)Math.ceil(maxDepth * readHeight/scaleRegion) * scaleRegion;
      g2d.drawLine(vScalePos, base, vScalePos, base + scaleDepth);
      //drawing
      for (int i = 0;  i * scaleRegion <= scaleDepth; i++) {
        //draw type1 scale
        g2d.drawLine(vScalePos - border / 2, base + (i * scaleRegion),
                     vScalePos + border / 2, base + (i * scaleRegion));
        //draw type2 scale
        if(i * scaleRegion + scaleRegion / 2 < scaleDepth){
          g2d.drawLine(vScalePos - border / 5, base + i * scaleRegion + scaleRegion / 2,
                       vScalePos + border / 5, base + i * scaleRegion + scaleRegion / 2);
        }
        //write type1 scale number
        if (lastWordPos + border > base + (i * scaleRegion) + 5) continue;
        g2d.drawString(Integer.toString(10 * i),
                       vScalePos + border + border / 2,
                       base + (i * scaleRegion) + fontSize);
        lastWordPos = base + (i * scaleRegion) + fontSize;
      }

    }else if(outputMode.toLowerCase().equals("depthlog")){

      scaleRegion = 50 * readHeight;
      scaleDepth = (int)Math.ceil(maxDepth) * scaleRegion;
      g2d.drawLine(vScalePos, base, vScalePos, base + scaleDepth);
      //drawing
      for(int i=0; i * scaleRegion <= scaleDepth ; i++){
        //draw type1 scale
        g2d.drawLine(vScalePos - border / 2, base + (i * scaleRegion),
                       vScalePos + border / 2, base + (i * scaleRegion));
        //draw type2 scale
        if(i * scaleRegion + scaleRegion / 2 < scaleDepth){
          g2d.drawLine(vScalePos - border / 5, base + scaleRegion * i + scaleRegion / 2,
                       vScalePos + border / 5, base + scaleRegion * i + scaleRegion / 2);
        }
        //write type1 scale number
        if (lastWordPos + border > base + (i * scaleRegion)) continue;
        g2d.drawString(Integer.toString( (int) Math.pow(10, i)),
                       vScalePos + border + border / 2,
                       base + (i * scaleRegion) + fontSize);
        lastWordPos = base + (i * scaleRegion) + fontSize;
      }
  }
}

  private int drawReadInfo(Graphics2D g2d,int base){
    if(forwardDirection){
      for (Iterator levelIterator = levelReadMap.keySet().iterator();
           levelIterator.hasNext(); ) {
        Integer level = (Integer) levelIterator.next();
        Set intervalSet = (Set) levelReadMap.get(level);
        for (Iterator intervalIterator = intervalSet.iterator();
             intervalIterator.hasNext(); ) {
          Interval interval = (Interval) intervalIterator.next();
          if (interval.getUserObject() instanceof Object[]) {
            // an uniq read (or multi read)
            // get position
            int pos1 = interval.getStart();
            int pos2 = interval.getStop();
            // transfer to relative position
            pos1 -= startPos;
            pos2 -= startPos;
            // transfer to position in Pt
            pos1 = bp2pt(pos1, sizePt, length);
            pos2 = bp2pt(pos2, sizePt, length);
            // decide color
            Object[] intervalInfo = (Object[]) interval.getUserObject();
            if (Util.uniqString.equals(intervalInfo[0])) {
              g2d.setColor(uniqReadColor);
            }
            else if (Util.multiString.equals(intervalInfo[0])) {
              g2d.setColor(multiReadColor);
            }
            g2d.drawRect(border + pos1,
                         base,
                         pos2 - pos1,
                         readHeight);
          }
          else {
            // a splice read
            Set blockSet = (Set) interval.getUserObject();
            // for splicing band
            Interval lastBlock = null;
            for (Iterator blockIterator = blockSet.iterator();
                 blockIterator.hasNext(); ) {
              Interval thisBlock = (Interval) blockIterator.next();
              if (lastBlock != null) {
                int pos1 = lastBlock.getStop();
                int pos2 = thisBlock.getStart();
                // transfer to relative position
                pos1 -= startPos;
                pos2 -= startPos;
                // transfer to position in Pt
                pos1 = bp2pt(pos1, sizePt, length);
                pos2 = bp2pt(pos2, sizePt, length);
                // an uniq read
                g2d.setColor(spliceBandColor);
                g2d.drawLine(border + pos1,
                             base + readHeight / 2,
                             border + pos2,
                             base + readHeight / 2);
              }
              lastBlock = thisBlock;
            }
            // for blocks
            // get position
            for (Iterator blockIterator = blockSet.iterator();
                 blockIterator.hasNext(); ) {
              Interval thisBlock = (Interval) blockIterator.next();
              int pos1 = thisBlock.getStart();
              int pos2 = thisBlock.getStop();
              // transfer to relative position
              pos1 -= startPos;
              pos2 -= startPos;
              // transfer to position in Pt
              pos1 = bp2pt(pos1, sizePt, length);
              pos2 = bp2pt(pos2, sizePt, length);
              // an uniq read
              g2d.setColor(spliceReadColor);
              g2d.drawRect(border + pos1,
                           base,
                           pos2 - pos1,
                           readHeight);
            }
          }
        }

        base += (readHeight + 2);
      }
    }else{ // reverse
      for (Iterator levelIterator = levelReadMap.keySet().iterator();
           levelIterator.hasNext(); ) {
        Integer level = (Integer) levelIterator.next();
        Set intervalSet = (Set) levelReadMap.get(level);
        for (Iterator intervalIterator = intervalSet.iterator();
             intervalIterator.hasNext(); ) {
          Interval interval = (Interval) intervalIterator.next();
          if (interval.getUserObject() instanceof Object[]) {
            // an uniq read (or multi read)
            // get position
            int pos2 = interval.getStart();
            int pos1 = interval.getStop();
            // transfer to relative position
            pos1 = stopPos - pos1;
            pos2 = stopPos - pos2;
            // transfer to position in Pt
            pos1 = bp2pt(pos1, sizePt, length);
            pos2 = bp2pt(pos2, sizePt, length);
            // decide color
            Object[] intervalInfo = (Object[]) interval.getUserObject();
            if (Util.uniqString.equals(intervalInfo[0])) {
              g2d.setColor(uniqReadColor);
            }
            else if (Util.multiString.equals(intervalInfo[0])) {
              g2d.setColor(multiReadColor);
            }
            g2d.drawRect(border + pos1,
                         base,
                         pos2 - pos1,
                         readHeight);
          }
          else {
            // a splice read
            Set blockSet = (Set) interval.getUserObject();
            // for splicing band
            Interval lastBlock = null;
            for (Iterator blockIterator = blockSet.iterator();
                 blockIterator.hasNext(); ) {
              Interval thisBlock = (Interval) blockIterator.next();
              if (lastBlock != null) {
                int pos2 = lastBlock.getStop();
                int pos1 = thisBlock.getStart();
                // transfer to relative position
                pos1 = stopPos - pos1;
                pos2 = stopPos - pos2;
                // transfer to position in Pt
                pos1 = bp2pt(pos1, sizePt, length);
                pos2 = bp2pt(pos2, sizePt, length);
                // an uniq read
                g2d.setColor(spliceBandColor);
                g2d.drawLine(border + pos1,
                             base + readHeight / 2,
                             border + pos2,
                             base + readHeight / 2);
              }
              lastBlock = thisBlock;
            }
            // for blocks
            // get position
            for (Iterator blockIterator = blockSet.iterator();
                 blockIterator.hasNext(); ) {
              Interval thisBlock = (Interval) blockIterator.next();
              int pos2 = thisBlock.getStart();
              int pos1 = thisBlock.getStop();
              // transfer to relative position
              pos1 = stopPos - pos1;
              pos2 = stopPos - pos2;
              // transfer to position in Pt
              pos1 = bp2pt(pos1, sizePt, length);
              pos2 = bp2pt(pos2, sizePt, length);
              // an uniq read
              g2d.setColor(spliceReadColor);
              g2d.drawRect(border + pos1,
                           base,
                           pos2 - pos1,
                           readHeight);
            }
          }
        }

        base += (readHeight + 2);
      }
    }
      return base;
  }

  private int drawExonInfo(Graphics2D g2d,int base){
    g2d.setColor(bandColor);
    g2d.setFont(font);
    FontMetrics metrics = g2d.getFontMetrics(font);

    Iterator iterator1,iterator2;
    int i,j;

    if(exonMap==null) return base;
    if(forwardDirection){
      for (iterator1 = exonMap.keySet().iterator(); iterator1.hasNext(); ) {
        String name = (String) iterator1.next();
        // name
        g2d.drawString(name,
                       border,
                       base + fontSize);
        base += fontSize;
        // drawing
        drawScale(g2d, base + markerHeight / 2 + 1);
        Set locationSetArray[] = (Set[]) exonMap.get(name);
        for (i = 0; i < 2; i++) {
          for (iterator2 = locationSetArray[i].iterator(); iterator2.hasNext(); ) {
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
            // draw
            g2d.drawRect(border + pos1,
                         base + i * (markerHeight + 2),
                         pos2 - pos1,
                         markerHeight);
          }
        }
        base += (markerHeight * 2 + 1);
      }
    }else{ // reverse
      for (iterator1 = exonMap.keySet().iterator(); iterator1.hasNext(); ) {
        String name = (String) iterator1.next();
        // name
        g2d.drawString(name,
                       border,
                       base + fontSize);
        base += fontSize;
        // drawing
        drawScale(g2d, base + markerHeight / 2 + 1);
        Set locationSetArray[] = (Set[]) exonMap.get(name);
        for (j = 0; j < 2; j++) {
          i = 1-j; // this is going to be stupid
          for (iterator2 = locationSetArray[i].iterator(); iterator2.hasNext(); ) {
            // get position
            Interval pos = (Interval) iterator2.next();
            int pos2 = pos.getStart();
            int pos1 = pos.getStop();
            // transfer to relative position
            pos1 = stopPos - pos1;
            pos2 = stopPos - pos2;
            // transfer to position in Pt
            pos1 = bp2pt(pos1, sizePt, length);
            pos2 = bp2pt(pos2, sizePt, length);
            // draw
            g2d.drawRect(border + pos1,
                         base + j * (markerHeight + 2),
                         pos2 - pos1,
                         markerHeight);
          }
        }
        base += (markerHeight * 2 + 1);
      }
    }
    base += (GraphicsMisc.minorVisibleGap*2);

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

    if(forwardDirection){
      for (i = 0; scale * i <= length; i++) {
        if (border + bp2pt(scale * i, sizePt, length) < lastWordPos)
          continue;
        if ( ( (startPos + scale * i) % GraphicsMisc.oneM) == 0 &&
            (startPos + scale * i) != 0) {
          g2d.drawString( (startPos + scale * i) / GraphicsMisc.oneM + "M",
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth( (startPos + scale * i) / GraphicsMisc.oneM + "M") +
              border;
        }
        else if ( ( (startPos + scale * i) % GraphicsMisc.oneK) == 0 &&
                 (startPos + scale * i) != 0) {
          g2d.drawString( (startPos + scale * i) / GraphicsMisc.oneK + "K",
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth( (startPos + scale * i) / GraphicsMisc.oneK + "K") +
              border;
        }
        else {
          g2d.drawString("" + (startPos + scale * i),
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth("" + (startPos + scale * i)) + border;
        }
      }
    }else{ // reverse
      for (i = 0; scale * i <= length; i++) {
        if (border + bp2pt(scale * i, sizePt, length) < lastWordPos)
          continue;
        if ( ( (stopPos - scale * i) % GraphicsMisc.oneM) == 0 &&
            (stopPos - scale * i) != 0) {
          g2d.drawString( (stopPos - scale * i) / GraphicsMisc.oneM + "M",
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth( (stopPos - scale * i) / GraphicsMisc.oneM + "M") +
              border;
        }
        else if ( ( (stopPos - scale * i) % GraphicsMisc.oneK) == 0 &&
                 (stopPos - scale * i) != 0) {
          g2d.drawString( (stopPos - scale * i) / GraphicsMisc.oneK + "K",
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth( (stopPos - scale * i) / GraphicsMisc.oneK + "K") +
              border;
        }
        else {
          g2d.drawString("" + (stopPos - scale * i),
                         border + bp2pt(scale * i, sizePt, length),
                         base + fontSize);
          lastWordPos = border + bp2pt(scale * i, sizePt, length) +
              metrics.stringWidth("" + (stopPos - scale * i)) + border;
        }
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

  public void setData(Map exonMap,final Map srcReadMap){
    this.exonMap = exonMap;

    final ReadMapComparator rmc = new ReadMapComparator(srcReadMap,forwardDirection);

    this.readMap = new TreeMap(rmc);
    readMap.clear();
    readMap.putAll(srcReadMap);
  }

  private void computeReadLevels(){
      int expandSizeBp = border*length/sizePt;

      for(Iterator readIterator = readMap.keySet().iterator();readIterator.hasNext();){
          Object readID = readIterator.next();
          Interval readInterval = (Interval) readMap.get(readID);
          if(readInterval.intersect(startPos,stopPos)==false) continue;
          Interval expandedInterval = new Interval(readInterval.getStart()-expandSizeBp,readInterval.getStop()+expandSizeBp);
          // use expandedInterval to find appropriate level
          Object level=null;
          boolean notIntersecting=false;
          for(Iterator levelIterator = levelReadMap.keySet().iterator();levelIterator.hasNext();){
              level = levelIterator.next();
              Set intervalSet = (Set) levelReadMap.get(level);
              notIntersecting = true;
              for(Iterator intervalIterator = intervalSet.iterator();intervalIterator.hasNext();){
                  Interval interval = (Interval) intervalIterator.next();
                  if(interval.intersect(expandedInterval)){
                      notIntersecting = false;
                      break;
                  }
              }
              if(notIntersecting) break;
          }
          if(notIntersecting){
              Set intervalSet = (Set) levelReadMap.get(level);
              intervalSet.add(readInterval);
          }else{
              Set intervalSet = new HashSet();
              intervalSet.add(readInterval);
              levelReadMap.put(new Integer(levelReadMap.size()),intervalSet);
          }
      }
  }
  
  public void outDepthArrayText(String outFilename){
	  
	  double[] depthArray = getDepthArray(startPos,stopPos,readMap,outputMode);
	  
	  // form interval trees for each set of exons
	  Map<String,IntervalTree> nameIntervalTreeMap = new HashMap<String,IntervalTree>();
      for (Iterator iterator1 = exonMap.keySet().iterator(); iterator1.hasNext(); ) {
    	  String name = (String) iterator1.next();
    	  IntervalTree tree = new IntervalTree();
    	  nameIntervalTreeMap.put(name, tree);
          // drawing
          Set locationSetArray[] = (Set[]) exonMap.get(name);
          for (int j = 0; j < 2; j++) {
        	  int i = 1-j; // this is going to be stupid
        	  for (Iterator iterator2 = locationSetArray[i].iterator(); iterator2.hasNext(); ) {
        		  // get position
        		  Interval pos = (Interval) iterator2.next();
        		  tree.insert(pos);
        	  }
          }
      }
	  
      try {
          FileWriter fw = new FileWriter(outFilename);

          ArrayList<String> nameList = new ArrayList<String>(); // insurance
          // header
          fw.write( "pos" + "\t" +
        		  	"depth" );
          for (Iterator iterator1 = exonMap.keySet().iterator(); iterator1.hasNext(); ) {
        	  String name = (String) iterator1.next();
        	  fw.write( "\t" + name );
        	  nameList.add(name);
          }
          fw.write("\n");
          
          for(int i=0; i<(stopPos-startPos+1); i++){
        	  fw.write( (startPos+i) + "\t" );
              // depth
        	  fw.write( depthArray[i] + "" );
              // exons
              for(String name : nameList){
            	  IntervalTree tree = nameIntervalTreeMap.get(name);
            	  List ans = tree.searchAll(new Interval(startPos+i,startPos+i));
            	  fw.write( "\t" + (ans.size()>0 ? 1 : 0) );
              }
              fw.write("\n");
          }
          

          fw.close();
      } catch (IOException ex) {
          ex.printStackTrace();
          System.exit(1);
      }
  }

  private double[] getDepthArray(int startPos, int stopPos, Map readMap, String outputMode){

    //declare an array to store depth info.
    double[] array = new double[stopPos-startPos+1];
    Arrays.fill(array,0.0);

    //compute read coverage
    for(Iterator readMapIt = readMap.keySet().iterator();readMapIt.hasNext();){
      Integer traceID = (Integer) readMapIt.next();
      Interval interval = (Interval) readMap.get(traceID);

      if (interval.getUserObject() instanceof Object[]) {
        // an uniq read (or multi read)
        int pos1 = interval.getStart();
        int pos2 = interval.getStop();
        // transfer to relative position
        pos1 -= startPos;
        pos2 -= startPos;
        // four mapping conditions needed to be calibrated
        if (pos1 < 0 & pos2 < 0)
          continue;
        if (pos1 > array.length - 1 & pos2 > array.length - 1)
          continue;
        if (pos1 < 0 & pos2 >= 0)
          pos1 = 0;
        if (pos1 <= array.length - 1 & pos2 > array.length - 1)
          pos2 = array.length - 1;
        //update array
        for (int i = pos1; i <= pos2; i++) {
          array[i] += 1;
        }

      }
      else {
        // a splice read
        Set blockSet = (Set) interval.getUserObject();
        // for blocks
        for (Iterator blockIterator = blockSet.iterator(); blockIterator.hasNext(); ) {
          Interval thisBlock = (Interval) blockIterator.next();
          int pos1 = thisBlock.getStart();
          int pos2 = thisBlock.getStop();
          // transfer to relative position
          pos1 -= startPos;
          pos2 -= startPos;
          // four mapping conditions needed to be calibrated
          if (pos1 < 0 & pos2 < 0)
            continue;
          if (pos1 > array.length - 1 & pos2 > array.length - 1)
            continue;
          if (pos1 < 0 & pos2 >= 0)
            pos1 = 0;
          if (pos1 <= array.length - 1 & pos2 > array.length - 1)
            pos2 = array.length - 1;
          //update array
          for (int j = pos1; j <= pos2; j++) {
            array[j] += 1;
          }
        }
      }
    }

    //replace 0 by 0.1 if outputMode is Depthlog
    if (outputMode.toLowerCase().equals("depthlog")) {
      for (int ind = 0; ind < array.length; ind++) {
        if (array[ind] <= 0) {
          array[ind] = -1; // -1 = log10(0.1)
        }else {
          array[ind] = Math.log10(array[ind]);
        }
      }
    }

    return array;
  }

  private double computeMaxDepth(double array[]){

    //find maximum depth
    double maxDepth = 0.0;
    for(int ind=0; ind<array.length; ind++){
      if(array[ind]>maxDepth){
        maxDepth = array[ind];
      }
    }

    return maxDepth;
  }

  public void adjustSize(){
    FontMetrics metrics = this.getFontMetrics(font);

    // height
    int height = 0;
    double maxDepth = 0;
    double[] depthArray = {};

    //int scaleRegion, maxDepthAdjust=0;
    if (this.outputMode.toLowerCase().equals("mapping")) {
      // border
      height += (2 * border);
      // default scale
      height += (fontSize + markerHeight);
      if (exonMap != null) {
        // gene/model names
        height += (exonMap.size() * fontSize);
        // drawings
        height += ( (markerHeight * 2 + 1) * exonMap.size());
      }
      // for reads
      computeReadLevels();
      height += ( (readHeight + 2) * levelReadMap.size());

    }else if (this.outputMode.toLowerCase().equals("depth")) {
      height += (2 * border);
      height += (fontSize + markerHeight);
      if (exonMap != null) {
        height += (exonMap.size() * fontSize);
        height += ( (markerHeight * 2 + 1) * exonMap.size());
      }
      depthArray = getDepthArray(this.startPos, this.stopPos, readMap, outputMode);
      maxDepth = computeMaxDepth(depthArray);
      int scaleRegion = 10 * readHeight;
      int scaleDepth = (int)Math.ceil(maxDepth/10) * scaleRegion;
      height += scaleDepth + fontSize + border; // the last border should be for the gap between the last horizontal scale and the vertical scale

    }else if (this.outputMode.toLowerCase().equals("depthlog")) {
      height += (2 * border);
      height += (fontSize + markerHeight);
      if (exonMap != null) {
        height += (exonMap.size() * fontSize);
        height += ( (markerHeight * 2 + 1) * exonMap.size());
      }
      depthArray = getDepthArray(this.startPos, this.stopPos, readMap, outputMode);
      maxDepth = computeMaxDepth(depthArray);
      int scaleRegion = 50 * readHeight;
      int scaleDepth = (int)Math.ceil(maxDepth) * scaleRegion;
      height += scaleDepth + fontSize + border; // the last border should be for the gap between the last horizontal scale and the vertical scale
    }



    // width
    int width = 0;
    int vlastWordPos = 0;
    int lastWordPos = 0 - border * 2;
    // forward direction
    if(forwardDirection){
      for (int i = 0; scale * i <= length; i++) {
        if (border + sizePt * scale * i / length < lastWordPos)
          continue;
        if ( ( (startPos + scale * i) % GraphicsMisc.oneM) == 0 &&
            (startPos + scale * i) != 0) {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth( (startPos + scale * i) / GraphicsMisc.oneM + "M") +
              border;
        }
        else if ( ( (startPos + scale * i) % GraphicsMisc.oneK) == 0 &&
                 (startPos + scale * i) != 0) {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth( (startPos + scale * i) / GraphicsMisc.oneK + "K") +
              border;
        }
        else {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth("" + (startPos + scale * i)) + border;
        }
      }
    }else{ // reverse direction
      for (int i = 0; scale * i <= length; i++) {
        if (border + sizePt * scale * i / length < lastWordPos)
          continue;
        if ( ( (stopPos - scale * i) % GraphicsMisc.oneM) == 0 &&
            (stopPos - scale * i) != 0) {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth( (stopPos - scale * i) / GraphicsMisc.oneM + "M") +
              border;
        }
        else if ( ( (stopPos - scale * i) % GraphicsMisc.oneK) == 0 &&
                 (stopPos - scale * i) != 0) {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth( (stopPos - scale * i) / GraphicsMisc.oneK + "K") +
              border;
        }
        else {
          lastWordPos = border + sizePt * scale * i / length +
              metrics.stringWidth("" + (stopPos - scale * i)) + border;
        }
      }
    }

    lastWordPos -= border;
    width = lastWordPos + border;

    if (this.outputMode.toLowerCase().equals("depth")) {
      vlastWordPos = border + sizePt + border + border + border/2 + metrics.stringWidth(""+maxDepth);
      if(vlastWordPos > lastWordPos){
        width = vlastWordPos + border;
      }
    }else if (this.outputMode.toLowerCase().equals("depthlog")) {
      int digits = (int)Math.ceil(maxDepth);
      String tmpStr = "1";
      for(int i=0;i<digits;i++) tmpStr += "0";
      vlastWordPos = border + sizePt + border + border + border/2 + metrics.stringWidth(tmpStr);
      if(vlastWordPos > lastWordPos){
        width = vlastWordPos + border;
      }
    }

    // setup size
    this.setSize(width, height);
}


  private static String inFilename = null;
  private static String outFilename = null;

  private static int inReadHeight = 2;
  private static boolean includeSplice = true;
  private static boolean includeUniq = true;
  private static boolean includeMulti = true;

  private static String inDirectionStr = "forward";
  private static Set directionSet = new LinkedHashSet(Arrays.asList("forward","reverse","first"));

  private static boolean useFirstModel = true; //appended by chun-mao
  private static Set outputModeSet = new LinkedHashSet(Arrays.asList("mapping","depth","depthlog","depthdata"));
  private static String outputModeIn = (String) outputModeSet.iterator().next();
  private static boolean inAuto = true;
  private static int inStart = 0;
  private static int inStop = 0;
  private static int inScale = 0;
  private static int inSizePt = 1000;
  private static String inFontString = "Courier";


  private static void paraProc(String[] args){
    int i;

    // get parameter strings
    for(i=0;i<args.length;i++){
      if(args[i].equals("-I")){
        inFilename = args[i+1];
        i++;
      }else if(args[i].equals("-O")){
        outFilename = args[i+1];
        i++;
      }else if(args[i].equals("-start")){
        inStart = Integer.parseInt(args[i+1]);
        i++;
      }else if(args[i].equals("-stop")){
        inStop = Integer.parseInt(args[i+1]);
        i++;
      }else if(args[i].equals("-scale")){
        inScale = Integer.parseInt(args[i+1]);
        i++;
      }else if(args[i].equals("-size")){
        inSizePt = Integer.parseInt(args[i+1]);
        i++;
      }else if(args[i].equals("-font")){
        inFontString = args[i+1];
        i++;
      }else if(args[i].equals("-auto")){
        inAuto = Boolean.valueOf(args[i + 1]).booleanValue();
        i++;
      }else if(args[i].equals("-splice")){
        includeSplice = Boolean.valueOf(args[i + 1]).booleanValue();
        i++;
      }else if (args[i].equals("-uniq")) {
        includeUniq = Boolean.valueOf(args[i + 1]).booleanValue();
        i++;
      } else if (args[i].equals("-multi")) {
        includeMulti = Boolean.valueOf(args[i + 1]).booleanValue();
        i++;
      }else if (args[i].equals("-h")) {
        inReadHeight = Integer.parseInt(args[i + 1]);
        i++;
      }else if (args[i].equals("-F")) {
        useFirstModel = Boolean.valueOf(args[i + 1]);
        i++;
      }else if (args[i].equals("-mode")) {
        outputModeIn = args[i + 1];
        i++;
      }else if (args[i].equals("-direction")) {
        inDirectionStr = args[i + 1];
        i++;
      }
    }

    // check for necessary parameters
    if(inFilename==null){
      System.err.println("input filename (-I) not assigned");
      System.exit(1);
    }
    if(outFilename==null){
      System.err.println("output filename (-O) not assigned");
      System.exit(1);
    }
    if(inAuto==false){ // then check input plot settings
      if(inStart>=inStop){
        System.err.println("start (-start) greater than or equal to stop (-stop)");
        System.exit(1);
      }
      if(inScale<=0){
        System.err.println("scale (-scale) less than or equal to 0");
        System.exit(1);
      }
    }
    if(outputModeIn==null || (!outputModeSet.contains(outputModeIn.toLowerCase()))) {
      System.err.println("Output mode (-mode) not assigned or not available, available choices:");
      for(Iterator outputModeIt = outputModeSet.iterator();outputModeIt.hasNext();){
        System.out.println(outputModeIt.next());
      }
      System.exit(1);
    }
    if(!directionSet.contains(inDirectionStr.toLowerCase())) {
      System.err.println("direction (-direction) not assigned or not available, available choices:");
      for(Iterator directionIt = directionSet.iterator();directionIt.hasNext();){
        System.out.println(directionIt.next());
      }
      System.exit(1);
    }


    // post-processing

    // list parameters
    System.out.println("program: ReadViz");
    System.out.println("input filename (-I): "+inFilename);
    System.out.println("output filename (-O): "+outFilename);
    System.out.println("output mode (-mode): "+outputModeIn);
    System.out.println("read height (-h): "+inReadHeight);
    System.out.println("include splice reads (-splice): "+includeSplice);
    System.out.println("include uniq reads (-uniq): "+includeUniq);
    System.out.println("include multi reads (-multi): "+includeMulti);
    System.out.println("direction (-direction): "+inDirectionStr);
    System.out.println("auto setting (-auto): "+inAuto);
    if(inAuto){
        System.out.println("\tuse first model (-F): "+useFirstModel);
    }else{
        System.out.println("\tplot start (-start): " + inStart);
        System.out.println("\tplot stop (-stop): " + inStop);
        System.out.println("\tplot scale (-scale): " + inScale);
    }
    System.out.println("plot size (-size): " + inSizePt);
    System.out.println("font (-font): " + inFontString);
    System.out.println();
  }

  public static void main(String[] args) throws IOException {
    System.setProperty("java.awt.headless", "true");
    paraProc(args);

    // read data
    Map exonLocationMap = new LinkedHashMap();
    Map readLocationMap = new HashMap();
    readData(inFilename,exonLocationMap,readLocationMap);

    // for auto setting
    if(inAuto){
        int[] settingArray = autoSetting(exonLocationMap,readLocationMap,useFirstModel);
        inStart = settingArray[0];
        inStop = settingArray[1];
        inScale = settingArray[2];
        System.out.println("auto start (-start): " + inStart);
        System.out.println("auto stop (-stop): " + inStop);
        System.out.println("auto plot scale (-scale): " + inScale);
    }

    // for direction
    boolean forward=true;
    if(inDirectionStr.toLowerCase().equals("first")){
      if(exonLocationMap.size()>0){
        Object firstModel = exonLocationMap.keySet().iterator().next();
        Set[] locationSetArray = (Set[]) exonLocationMap.get(firstModel);
        if(locationSetArray[0].size()>0){
          forward = true;
        }else{
          forward = false;
        }
      }
    }else{
      if(inDirectionStr.toLowerCase().equals("forwrad")){
        forward = true;
      }else if(inDirectionStr.toLowerCase().equals("reverse")){
        forward = false;
      }
    }

    // draw
    ReadViz cop = new ReadViz(inStart,inStop,inSizePt,inScale,12,10,inReadHeight,inFontString,outputModeIn.toLowerCase(),forward);
    cop.setData(exonLocationMap,readLocationMap);
    
    if(outputModeIn.toLowerCase().equals("depthdata")){
    	cop.outDepthArrayText(outFilename);
    	System.exit(0);
    }
    
    cop.adjustSize();
    BufferedImage image = new BufferedImage(cop.getWidth(),
                                            cop.getHeight(),
                                            BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();
    cop.paintComponent(g2);
    javax.imageio.ImageIO.write(image, "jpeg",new File(outFilename));
  }

  public static int[] autoSetting(Map exonLocationMap,Map readLocationMap,boolean useFirstModel){
      int veryStart = 0;
      int veryStop = 0;
      boolean firstIteration = true;

      if(exonLocationMap!=null){
          for (Iterator iterator = exonLocationMap.values().iterator(); iterator.hasNext(); ) {
              Set[] locationSetArray = (Set[]) iterator.next();
              for (int i = 0; i < locationSetArray.length; i++) {
                  if(locationSetArray[i]==null) continue;
                  for (Iterator intervalIterator = locationSetArray[i].iterator(); intervalIterator.hasNext(); ) {
                      Interval interval = (Interval) intervalIterator.next();
                      int itemStart = interval.getStart();
                      int itemStop = interval.getStop();
                      if (firstIteration) {
                          veryStart = itemStart;
                          veryStop = itemStop;
                          firstIteration = false;
                      } else {
                          veryStart = ((veryStart < itemStart) ? veryStart : itemStart);
                          veryStop = ((veryStop > itemStop) ? veryStop : itemStop);
                      }
                  }
              }
              if(useFirstModel==true) break; //appended by chun-mao
          }
      }
      if(readLocationMap!=null && useFirstModel!=true ){ //appended by chun-mao
          for (Iterator iterator = readLocationMap.values().iterator(); iterator.hasNext(); ) {
              Interval interval = (Interval) iterator.next();
              int itemStart = interval.getStart();
              int itemStop = interval.getStop();
              if (firstIteration) {
                  veryStart = itemStart;
                  veryStop = itemStop;
                  firstIteration = false;
              } else {
                  veryStart = ((veryStart < itemStart) ? veryStart : itemStart);
                  veryStop = ((veryStop > itemStop) ? veryStop : itemStop);
              }
          }
      }
      int locationRange = veryStop-veryStart;
      int start,stop,scale;

      // find plot range
      int testLen = 10;
      while(testLen<locationRange){
        testLen *= 2;
        if(testLen>locationRange) break;
        testLen /= 2;
        testLen *= 5;
        if(testLen>locationRange) break;
        testLen *= 2;
        if(testLen>locationRange) break;
      }
      scale = testLen/10;

      // find plot start
      int startZero = 10;
      int testStart = (veryStart/startZero)*startZero;
      start = veryStart;
      while(testStart+testLen >= veryStop && testStart>0){
        start = testStart;
        startZero *= 10;
        testStart = (veryStart/startZero)*startZero;
      }
      stop = start+testLen;

      // adjust start & stop
      while(start + scale < veryStart){
        start += scale;
      }
      while(stop - scale > veryStop){
        stop -= scale;
      }

      int[] retArray = {start,stop,scale};
      return retArray;
  }

  public static void readData(String filename,Map exonLocationMap,Map readLocationMap){
      try{
          Set[] locationSetArray;
          BufferedReader fr = new BufferedReader(new FileReader(new File(filename)));
          while (fr.ready()) {
              String line = fr.readLine();
              StringTokenizer st = new StringTokenizer(line);
              String itemName = st.nextToken().intern();
              int itemStart, itemStop;

              if (itemName.equals(Util.uniqString) ||
                  itemName.equals(Util.spliceString) ||
                  itemName.equals(Util.multiString)) {

                  if(itemName.equals(Util.uniqString) && includeUniq==false) continue;
                  if(itemName.equals(Util.spliceString) && includeSplice==false) continue;
                  if(itemName.equals(Util.multiString) && includeMulti==false) continue;

                  String strain = st.nextToken();
                  itemStart = Integer.parseInt(st.nextToken());
                  itemStop = Integer.parseInt(st.nextToken());
                  int traceID = Integer.parseInt(st.nextToken());
                  int blockID = Integer.parseInt(st.nextToken());

                  // put above into the data map
                  Boolean forwardStrain = strain.equals("+") ? Boolean.TRUE : Boolean.FALSE;
                  Object[] intervalInfo = {itemName,traceID,forwardStrain};
                  if (readLocationMap.containsKey(traceID)) {
                      Interval oldInterval = (Interval) readLocationMap.get(traceID);
                      Set intervalSet;
                      if (oldInterval.getUserObject() instanceof Object[]) {
                          // second block
                          intervalSet = new TreeSet();
                          intervalSet.add(oldInterval);
                      } else {
                          // third or later blocks
                          intervalSet = (Set) oldInterval.getUserObject();
                      }
                      intervalSet.add(new Interval(itemStart, itemStop,intervalInfo));
                      readLocationMap.put(traceID,new Interval((itemStart < oldInterval.getStart()) ? itemStart : oldInterval.getStart(),
                                                               (itemStop > oldInterval.getStop()) ? itemStop : oldInterval.getStop(),
                                                               intervalSet));
                  } else {
                      readLocationMap.put(traceID,new Interval(itemStart, itemStop,intervalInfo));
                  }
              } else {
                  String strain = st.nextToken();
                  itemStart = Integer.parseInt(st.nextToken());
                  itemStop = Integer.parseInt(st.nextToken());

                  // put above into the data map
                  if (exonLocationMap.containsKey(itemName)) {
                      locationSetArray = (Set[]) exonLocationMap.get(itemName);
                  } else {
                      locationSetArray = new Set[2];
                      locationSetArray[0] = new HashSet();
                      locationSetArray[1] = new HashSet();
                      exonLocationMap.put(itemName, locationSetArray);
                  }
                  // add location
                  if (strain.equals("+")) {
                      locationSetArray[0].add(new Interval(itemStart, itemStop));
                  } else {
                      locationSetArray[1].add(new Interval(itemStart, itemStop));
                  }
              }
          }
      }
      catch(IOException ex){
          ex.printStackTrace();
          System.exit(1);
      }
  }
}
