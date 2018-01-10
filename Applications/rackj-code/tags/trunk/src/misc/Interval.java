package misc;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import misc.intervaltree.IntervalInterface;

/**
 * This class defines intervals, the interval will be
 * presented in the form (start,stop) where start and stop are included.
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class Interval implements IntervalInterface{


    private int start,stop;
    private Object userObject = null;

    /**
     * @param start start point
     * @param stop stop point
     */
    public Interval(int start,int stop){
      this.start = start;
      this.stop = stop;
    }

    /**
     * This constructor gives a chance to maintain order of the two ends
     * @param point1 end point 1
     * @param point2 end point 2
     * @param assertOrder true if assert the order of stop <= stop
     */
    public Interval(int point1,int point2,boolean assertOrder){
      if(assertOrder && point1>point2){
        this.start = point2;
        this.stop = point1;
      }else{
        this.start = point1;
        this.stop = point2;
      }
    }

    /**
     * @param start start point
     * @param stop stop point
     * @param obj user object
     */
    public Interval(int start,int stop,Object obj){
      this.start = start;
      this.stop = stop;
      userObject = obj;
    }

    public int getLow() {
        return start;
    }

    public int getHigh() {
        return stop;
    }

    /**
     * @return start point
     */
    public int getStart(){
      return start;
    }

    /**
     * @return stop point
     */
    public int getStop(){
      return stop;
    }

    /**
     * @return user object
     */
    public Object getUserObject(){
      return userObject;
    }

    public void setUserObject(Object obj){
        this.userObject = obj;
    }

    /**
     * @return length
     */
    public int length(){
      return stop - start + 1;
    }

    /**
     * Test if this interval contains the given point.
     * @param x point to be tested
     * @return true or false
     */
    public boolean intersect(int x){
      if(start <= x && x <= stop){
        return true;
      }else{
        return false;
      }
    }

    /**
     * Test if this interval intersect with the given interval for more than (or equal to) minOverlap bps
     * @param x1 input interval start
     * @param x2 input interval stop
     * @param minOverlap
     * @return true or false
     */
    public boolean intersect(int x1,int x2,int minOverlap){
        if(this.intersect(x1,x2)){
            int oStart = (start > x1) ? start : x1;
            int oStop = (stop < x2) ? stop : x2;
            int overlap = oStop-oStart+1;
            if(overlap>=minOverlap) return true;
            else return false;
        }else{
            return false;
        }
    }

    /**
     * Test if this interval intersect with the given interval
     * @param x1 input interval start
     * @param x2 input interval stop
     * @return true or false
     */
    public boolean intersect(int x1,int x2){
      if ( (start <= x1 && x1 <= stop)
          || (start <= x2 && x2 <= stop)
          || (x1 <= start && stop <= x2)) {
        return true;
      } else {
        return false;
      }
    }

    public boolean overlaps(IntervalInterface other) {
        return (this.getLow() <= other.getHigh() &&
                other.getLow() <= this.getHigh());
    }

    /**
     * Test if this interval intersect with the given interval for more than (or equal to) minOverlap bps
     * @param otherInterval the other GenomeInterval
     * @param minOverlap
     * @return true or false
     */
    public boolean intersect(Interval otherInterval,int minOverlap){
        return this.intersect(otherInterval.getStart(),otherInterval.getStop(),minOverlap);
    }

    /**
     * Test if this interval intersect with the given interval
     * @param otherInterval the other GenomeInterval
     * @return true or false
     */
    public boolean intersect(Interval otherInterval){
        return this.intersect(otherInterval.getStart(),otherInterval.getStop());
    }

    public boolean contain(int x1,int x2){
        if(this.getLow()<=x1 && x2<=this.getHigh()) return true;
        else return false;
    }

    /**
     * Get a translated Interval of this Interval, using (fromX,fromY) and (toX,toY)
     * as reference Intervals of this Interval and resulted Interval, respectively.
     * @param fromX reference point of the reference Interval of the this Interval
     * @param toX reference point of the reference Interval of the resulted Interval
     * @param forwardTranslate this translation is direction preserved or not
     * @param assertOrder assert returned Interval with start <= stop
     * @return translated Interval
     */
    public Interval translate(int fromX,int toX, boolean forwardTranslate, boolean assertOrder){
    	int T = (forwardTranslate)? 1 : -1;

    	int newStart = toX + (this.getStart() - fromX)*T;
    	int newStop = toX + (this.getStop()  - fromX)*T;
    	return new Interval(newStart, newStop, assertOrder);
    }

    /**
     * Return a new Interval that is contained by a given constrain Interval
     * @param constrainInterval the constrain Interval
     * @return constrained Interval based on this Interval
     */
    public Interval constrain(Interval constrainInterval){
      if(constrainInterval.contain(this.start,this.stop)){
        return this;
      }else{
        int newStart = (this.start < constrainInterval.getStart()) ? constrainInterval.getStart() : this.start;
        int newStop  = (this.stop  > constrainInterval.getStop())  ? constrainInterval.getStop()  : this.stop;
        return new Interval(newStart,newStop);
      }
    }

    /**
     * @return start + stop
     */
    public int hashCode(){
      return start+stop;
    }

    /**
     * @param obj input object
     * @return true if input object is an instance of IntegerInterval with the same
     * start and stop points, and the same user objects.
     */
    public boolean equals(Object obj){
      Interval otherIi;

      if(obj==null){
        throw(new NullPointerException());
      }

      if(obj instanceof Interval){
        otherIi = (Interval) obj;
        if(start==otherIi.start && stop==otherIi.stop){
          if(userObject==null){
            if(otherIi.getUserObject()==null) return true;
            else return false;
          }else
            return userObject.equals(otherIi.getUserObject());
        }else{
          return false;
        }
      }else{
        return false;
      }
    }

    public int compareTo(Object object) {
      Interval ii;

      if(object==null){
        throw(new NullPointerException());
      }

      ii = (Interval) object;
      if (start != ii.start) {
        return start - ii.start;
      }
      else if (stop != ii.stop) {
        return stop - ii.stop;
      }
      else if(userObject==null){
        if(ii.userObject==null) return 0;
        else return -1;
      }else if(ii.userObject==null){
        return 1;
      }else{
        try{
            return ((Comparable) userObject).compareTo(ii.userObject);
        }catch(ClassCastException ex){ // may got some mistake here
            return userObject.hashCode() - ii.userObject.hashCode();
        }
      }
    }

    public String toString(){
        if(userObject==null){
            return "(" + start + "," + stop + ")";
        }else{
            return "(" + start + "," + stop + "):" + userObject;
        }
    }

    public static void combineOverlap(Set intervalSet){
        while(combineOverlapDoit(intervalSet));
    }

    private static boolean combineOverlapDoit(Set intervalSet){
        Interval interval1,interval2;
        Set removePairs = new HashSet();
        Set removeSet = new HashSet();
        for(Iterator iterator1 = intervalSet.iterator();iterator1.hasNext();){
            interval1 = (Interval) iterator1.next();
            for(Iterator iterator2 = intervalSet.iterator();iterator2.hasNext();){
                interval2 = (Interval) iterator2.next();
                if(interval2.equals(interval1)) break;;
                if(interval1.intersect(interval2)){
                    // we don't remove this pair this round
                    if(removeSet.contains(interval1)==true || removeSet.contains(interval2)==true) continue;

                    // put this pair into collection
                    Set pair = new HashSet();
                    pair.add(interval1);
                    pair.add(interval2);
                    removePairs.add(pair);
                    removeSet.add(interval1);
                    removeSet.add(interval2);
                }
            }
        }
        if(removePairs.size()>0){
            for(Iterator pairIterator = removePairs.iterator();pairIterator.hasNext();){
                Set pair = (Set) pairIterator.next();
                Iterator iterator = pair.iterator();
                interval1 = (Interval) iterator.next();
                interval2 = (Interval) iterator.next();
                intervalSet.remove(interval1);
                intervalSet.remove(interval2);
                intervalSet.add(combine(interval1,interval2));
            }
            return true;
        }else{
            return false;
        }
    }

    public static void combineNearby(TreeSet intervalSet,int distance){
        while(combineNearbyDoit((TreeSet)intervalSet,distance));
    }

    private static boolean combineNearbyDoit(TreeSet intervalSet,int distance){
        if(intervalSet.size()<=1) return false;

        Iterator iterator = intervalSet.iterator();
        Interval lastInterval = (Interval) iterator.next();

        while(iterator.hasNext()){
            Interval thisInterval = (Interval) iterator.next();
            if((thisInterval.getStart()-lastInterval.getStop())<=distance){
                intervalSet.remove(lastInterval);
                intervalSet.remove(thisInterval);
                intervalSet.add(combine(thisInterval,lastInterval));
                return true;
            }
            lastInterval = thisInterval;
        }

        return false;
    }

    public Interval combine(Interval interval2){
        Interval interval1 = this;

        // someday I must find someother way to replace this DIRTY code
        if(interval1 instanceof GenomeInterval){
            return ((GenomeInterval)interval1).combine((GenomeInterval)interval2);
        }

        int start = (interval1.start<interval2.start)?interval1.start:interval2.start;
        int stop = (interval1.stop>interval2.stop)?interval1.stop:interval2.stop;
        return new Interval(start,stop);
    }

    public static Interval combine(Interval interval1, Interval interval2){

        if(interval1.getClass().equals(Interval.class)==false || interval2.getClass().equals(Interval.class)==false){
            try {
                Method combineMethod = interval1.getClass().getMethod("combine", interval1.getClass(), interval2.getClass());
                return (Interval) combineMethod.invoke(null,interval1,interval2);
            }
            catch(Exception ex){
                ex.printStackTrace();
                System.exit(1);
            }
        }else{
            int start = (interval1.start < interval2.start) ? interval1.start : interval2.start;
            int stop = (interval1.stop > interval2.stop) ? interval1.stop : interval2.stop;

            return new Interval(start, stop);
        }
        return null;
    }

    public Interval combine(Set intervalSet){
        if(this.getClass().equals(Interval.class)==false){
            try {
                Method combineMethod = this.getClass().getMethod("combine", Set.class);
                return (Interval) combineMethod.invoke(this,intervalSet);
            }
            catch(Exception ex){
                ex.printStackTrace();
                System.exit(1);
            }
        }else{
            Iterator intervalIterator = intervalSet.iterator();
            Interval ansInterval = (Interval) intervalIterator.next();
            for(;intervalIterator.hasNext();){
                Interval interval = (Interval) intervalIterator.next();
                ansInterval = combine(ansInterval,interval);
            }

            return ansInterval;
        }
        return null;
    }
}
