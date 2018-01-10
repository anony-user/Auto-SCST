package misc;

import java.util.HashMap;
import java.util.Map;

public class GffRecord implements Comparable{
    private String feature;
    private String chr;
    private Interval interval;
    private String strand;
    private String attrStr;

    public GffRecord(String feature,String chr,Interval interval,String strand,String attrStr){
        this.feature = feature.intern();
        this.chr = chr;
        this.interval = interval;
        this.strand = strand;
        this.attrStr = attrStr;
    }

    public String getFeature(){
        return this.feature;
    }

    public String getChr(){
        return this.chr;
    }

    public Interval getInterval(){
        return this.interval;
    }

    public String getStrand(){
        return this.strand;
    }

    public String getAttrStr(){
        return this.attrStr;
    }

    public Map getAttrMap(){
        return getAttrMap('\\','=');
    }

    public Map getAttrMap(char escChar, char delimiterChar){
        Map ansMap = new HashMap();
      /*  EscapeStringTokenizer est = new EscapeStringTokenizer(escChar,delimiterChar);

        StringTokenizer st = new StringTokenizer(attrStr,";");
        while(st.hasMoreTokens()){
            String token = st.nextToken().trim();
            est.parseSeparate(token);
            if(est.getPostStr().indexOf(",")>0){
            	// take only the first token for parent, may cause problem, to be fixed?
                StringTokenizer st1 = new StringTokenizer(est.getPostStr(),",");
                ansMap.put(est.getPreStr(),st1.nextToken());
            }else{
                ansMap.put(est.getPreStr(),est.getPostStr());
            }
        }*/

        return ansMap;
    }

    public String toString(){
        return this.getFeature()+":"+this.getChr()+":"+this.getInterval()+":"+this.getAttrStr();
    }

    public int compareTo(Object obj){
        GffRecord record;

        if(obj==null){
          throw(new NullPointerException());
        }

        record = (GffRecord) obj;
        if(this.getFeature().equals(record.getFeature())==false){
            return this.getFeature().compareTo(record.getFeature());
        }else if(this.getChr().equals(record.getChr())==false){
            return this.getChr().compareTo(record.getChr());
        }else if(this.getInterval().equals(record.getInterval())==false){
            return this.getInterval().compareTo(record.getInterval());
        }else{
            return this.getAttrStr().compareTo(record.getAttrStr());
        }
    }
}
