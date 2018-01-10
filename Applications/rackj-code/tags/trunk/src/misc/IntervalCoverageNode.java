package misc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * This class mimics Interval so that it maintains start and stop points,
 * and an array of coverage depth.
 * @author Chun-Mao Fu
 * @version 1.0
 */
public class IntervalCoverageNode  extends Interval{

  private int[] CoverageArray;

  //constructor
  public IntervalCoverageNode(int start,int stop) {
    super(start,stop);
    this.CoverageArray = new int[stop-start+1];
    Arrays.fill(this.CoverageArray,1);
  }

  public IntervalCoverageNode(int start,int stop, int[] array) {
    super(start,stop);
    this.CoverageArray = array;
  }

  public IntervalCoverageNode(int start,int stop, Object userObject) {
    super(start,stop,userObject);
    this.CoverageArray = new int[stop-start+1];
    Arrays.fill(this.CoverageArray,1);
  }

  public IntervalCoverageNode(IntervalCoverageNode baseICN,int start,int stop){
    super(start,stop);
    this.CoverageArray = new int[stop-start+1];
    System.arraycopy(baseICN.CoverageArray,start-baseICN.getStart(),this.CoverageArray,0,this.CoverageArray.length);
  }

  public int[] getCoverageArray(){
    return CoverageArray;
  }

  public void setCoverageArray(int[] array){
      this.CoverageArray = array;
  }

  //combine method
  public IntervalCoverageNode combine(Set IntervalSet){

    //find the final start and stop point for constructing a new array
    Iterator intervalIterator = IntervalSet.iterator();
    IntervalCoverageNode firstIcn = (IntervalCoverageNode)intervalIterator.next();
    int start = firstIcn.getStart();
    int stop = firstIcn.getStop();
    while(intervalIterator.hasNext()){
      IntervalCoverageNode restIcn = (IntervalCoverageNode)intervalIterator.next();
      start = (restIcn.getStart()>start ? start : restIcn.getStart());
      stop = (restIcn.getStop()>stop ? restIcn.getStop() : stop);
    }

    //construct a new array
    int[] array = new int[stop-start+1];
    Arrays.fill(array,0);

    //update new array
    for(Iterator coverageNodeIterator = IntervalSet.iterator();coverageNodeIterator.hasNext();){
      IntervalCoverageNode intervalCoverageNode = (IntervalCoverageNode)coverageNodeIterator.next();
      int from = intervalCoverageNode.getStart() - start;
      int to = intervalCoverageNode.getStop() - start + 1;
      for(int i=from,j=0; i<to && j<intervalCoverageNode.CoverageArray.length; i++,j++)
        array[i] += intervalCoverageNode.CoverageArray[j];
    }

    return new IntervalCoverageNode(start,stop,array);
  }

  public boolean equals(Object obj){

      if(obj==null) throw(new NullPointerException());

      if(obj instanceof IntervalCoverageNode){
        if(super.equals(obj)){
            IntervalCoverageNode otherIcn = (IntervalCoverageNode) obj;
            return Arrays.equals(this.CoverageArray,otherIcn.CoverageArray);
        }else{
            return false;
        }
      }else return false;
  }

  public int compareTo(Object obj) {

    if (obj == null) throw (new NullPointerException());

    if (equals(obj)) {
      return 0;
    }else {

      if(super.compareTo(obj)!=0) return super.compareTo(obj);

      IntervalCoverageNode otherIcn = (IntervalCoverageNode) obj;

      if (CoverageArray.length != otherIcn.CoverageArray.length) {
        return CoverageArray.length - otherIcn.CoverageArray.length;
      }else {
        int result = 0;
        for (int ind = 0; ind < CoverageArray.length; ind++) {
          if (CoverageArray[ind] != otherIcn.CoverageArray[ind]) {
            result = CoverageArray[ind] - otherIcn.CoverageArray[ind];
            break;
          }else continue;
        }
        return result;
      }
    }
  }

}
