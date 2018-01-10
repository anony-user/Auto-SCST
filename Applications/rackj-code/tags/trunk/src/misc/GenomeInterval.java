package misc;

public class GenomeInterval extends Interval {

    private String chr;

    public GenomeInterval(String chr,int start,int stop,boolean assertOrder){
        super(start,stop,assertOrder);
        this.chr = chr;
    }
    
    public GenomeInterval(String chr,int start,int stop){
        super(start,stop);
        this.chr = chr;
    }

    public GenomeInterval(String chr,int start,int stop,Object userObject){
        super(start,stop,userObject);
        this.chr = chr;
    }

    public String getChr(){
        return chr;
    }

    public boolean intersect(String chr,int x){
        if(this.chr.equals(chr)){
            return intersect(x);
        }else{
            return false;
        }
    }

    public boolean intersect(String chr,int x1,int x2){
        if(this.chr.equals(chr)){
            return intersect(x1,x2);
        }else{
            return false;
        }
    }

    public boolean intersect(GenomeInterval otherInterval){
        if(this.chr.equals(otherInterval.getChr())){
            return intersect(otherInterval.getStart(),otherInterval.getStop());
        }else{
            return false;
        }
    }

    public int hashCode(){
      return getStart()+getStop()+chr.hashCode();
    }

    public boolean equals(Object obj){
      GenomeInterval otherIi;

      if(obj==null){
        throw(new NullPointerException());
      }

      if(obj instanceof GenomeInterval){
        otherIi = (GenomeInterval) obj;
        if(chr.equals(otherIi.getChr())){
            return super.equals(obj);
        }else{
            return false;
        }
      }else{
        return false;
      }
    }

    public int compareTo(Object object) {
      GenomeInterval ii;

      if(object==null){
        throw(new NullPointerException());
      }

      ii = (GenomeInterval) object;
      if(chr.equals(ii.getChr())==false){
        return chr.compareTo(ii.getChr());
      }else{
        return super.compareTo(object);
      }
    }

    public String toString(){
        if(getUserObject()==null){
            return chr + "(" + getStart() + "," + getStop() + ")";
        }else{
            return chr + "(" + getStart() + "," + getStop() + "):" +
                    getUserObject();
        }
    }

    public GenomeInterval combine(GenomeInterval interval2){
        GenomeInterval interval1 = this;
        int start = (interval1.getStart()<interval2.getStart())?interval1.getStart():interval2.getStart();
        int stop = (interval1.getStop()>interval2.getStop())?interval1.getStop():interval2.getStop();
        return new GenomeInterval(interval1.getChr(),start,stop);
    }

    public static GenomeInterval combine(GenomeInterval interval1, GenomeInterval interval2){
        int start = (interval1.getStart()<interval2.getStart())?interval1.getStart():interval2.getStart();
        int stop = (interval1.getStop()>interval2.getStop())?interval1.getStop():interval2.getStop();
        return new GenomeInterval(interval1.getChr(),start,stop);
    }
}
