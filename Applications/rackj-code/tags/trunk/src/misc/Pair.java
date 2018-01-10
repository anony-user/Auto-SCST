package misc;

public class Pair implements Comparable {

    public Comparable key1,key2;

    public Pair(Comparable key1, Comparable key2){
        this.key1 = key1;
        this.key2 = key2;
    }

    public int hashCode(){
        return key1.hashCode()+key2.hashCode();
    }

    public boolean equals(Object obj){
        if(obj instanceof Pair){
            Pair otherPair = (Pair) obj;
            if(this.key1.equals(otherPair.key1) && this.key2.equals(otherPair.key2)){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    public int compareTo(Object object) {
        if(object==null){
          throw(new NullPointerException());
        }

        Pair other = (Pair) object;
        if(this.key1.compareTo(other.key1)==0){
            return this.key2.compareTo(other.key2);
        }else{
            return this.key1.compareTo(other.key1);
        }
    }

    public String toString(){
        return key1+":"+key2;
    }

}
