package misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HashMap2D {

    // key variables
    private HashMap mainMap = new HashMap();
    private HashMap key1Hash = new HashMap();
    private HashMap key2Hash = new HashMap();

    public void put(Comparable key1,Comparable key2,Object value){
        mainMap.put(new Pair(key1,key2),value);

        if(key1Hash.containsKey(key1)==false){
            key1Hash.put(key1,new HashSet());
        }
        ((Set)key1Hash.get(key1)).add(key2);

        if(key2Hash.containsKey(key2)==false){
            key2Hash.put(key2,new HashSet());
        }
        ((Set)key2Hash.get(key2)).add(key1);
    }

    public Object get(Comparable key1,Comparable key2){
        return mainMap.get(new Pair(key1,key2));
    }

    public void remove(Comparable key1,Comparable key2){
        mainMap.remove(new Pair(key1,key2));

        if(key1Hash.containsKey(key1)){
            ((Set)key1Hash.get(key1)).remove(key2);
            if(((Set)key1Hash.get(key1)).size()==0){
                key1Hash.remove(key1);
            }
        }

        if(key2Hash.containsKey(key2)){
            ((Set)key2Hash.get(key2)).remove(key1);
            if(((Set)key2Hash.get(key2)).size()==0){
                key2Hash.remove(key2);
            }
        }
    }

    public Set getColumns(Object key1){
        if(key1Hash.containsKey(key1)){
            return new HashSet((Set)key1Hash.get(key1));
        }else{
            return new HashSet();
        }
    }

    public Set getRows(Object key2){
        if(key2Hash.containsKey(key2)){
            return new HashSet((Set)key2Hash.get(key2));
        }else{
            return new HashSet();
        }
    }
    
    public int size(){
    	return mainMap.size();
    }
}
