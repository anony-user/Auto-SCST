package misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MultiLevelMap<V extends Number> {
	
	private Map topLevelMap = null;
	private ArrayList defList = new ArrayList();
	
	public MultiLevelMap(){
		topLevelMap = new HashMap();
		defList.add(topLevelMap.getClass());
	}
	
	public MultiLevelMap(Object[] defArray){
		// setup definition list
		for(int i=0;i<defArray.length;i++){
			defList.add(defArray[i]);
		}

		if(defList.get(0) instanceof Class){
			try {
				topLevelMap = (Map) ((Class) defList.get(0)).getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}else if(defList.get(0) instanceof Comparator){
			topLevelMap = new TreeMap((Comparator) defList.get(0));
		}else{
			new Exception("incorrect definition at index " + 0).printStackTrace();
			System.exit(1);
		}
	}
	
	public void setTopLevelMap(Map map){
		this.topLevelMap = map;
		defList.set(0, map.getClass());
	}
	
	public Map getTopLevelMap(){
		return topLevelMap;
	}

	public Object multiLevelPut(Object value, Object... keys){
		Map lastMap = getLastMap(topLevelMap, defList, true, keys);
		
		return lastMap.put(keys[keys.length-1], value);
	}
		
	public Object multiLevelGet(Object... keys){
		return multiLevelGet(topLevelMap,keys);
	}
	
	public V multiLevelAdd(V value, Object... keys){
		return (V) this.multiLevelAdd(topLevelMap, value, keys);
	}
	
	private static Map getLastMap(Map topLevelMap, ArrayList defList, boolean createMaps, Object... keys){
		Map workingMap = topLevelMap;
		
		// go to last-level map
		for(int level=0; level<keys.length-1; level++){
			if(workingMap.containsKey(keys[level])){ // get next level map
				workingMap = (Map) workingMap.get(keys[level]);
			}else if(createMaps){ // create next level map if needed
				Map nextMap = null;
				if( (level+1)>defList.size() ) {
					nextMap = new HashMap();
				}else{
					if(defList.get(level+1) instanceof Class){
						try {
							nextMap = (Map) ((Class) defList.get(level+1)).getConstructor().newInstance();
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(1);
						}
					}else if(defList.get(level+1) instanceof Comparator){
						nextMap = new TreeMap((Comparator) defList.get(level+1));
					}else{
						new Exception("incorrect definition at index " + (level+1)).printStackTrace();
						System.exit(1);
					}
				}
				
				workingMap.put(keys[level], nextMap);
				workingMap = nextMap;
			}else{
				return null;
			}
		}
		
		return workingMap;
	}

	public static Object multiLevelPut(Map topLevelMap, Object value, Object... keys){
		return multiLevelPut(new ArrayList(), topLevelMap, value, keys);
	}
	
	public static Object multiLevelPut(ArrayList defList, Map topLevelMap, Object value, Object... keys){
		Map lastMap = getLastMap(topLevelMap, defList, true, keys);
		
		return lastMap.put(keys[keys.length-1], value);
	}

	public static Number multiLevelAdd(Map topLevelMap, Number value, Object... keys){
		return multiLevelAdd(new ArrayList(), topLevelMap, value, keys);
	}
	
	public static Number multiLevelAdd(ArrayList defList, Map topLevelMap, Number value, Object... keys){
		Map lastMap = getLastMap(topLevelMap, defList, true, keys);
		
		Object oldValObj = lastMap.get(keys[keys.length-1]);
		Number newVal = null;

		// precision preserving add
		if(value instanceof Double || oldValObj instanceof Double){
			double oldVal = 0;
			if(oldValObj!=null) oldVal = ((Number)oldValObj).doubleValue();
			newVal = new Double(oldVal + ((Double)value));
		}else if(value instanceof Float || oldValObj instanceof Float){
			float oldVal = 0;
			if(oldValObj!=null) oldVal = ((Number)oldValObj).floatValue();
			newVal = new Float(oldVal + ((Float)value));
		}else if(value instanceof Integer && (oldValObj==null || oldValObj instanceof Integer)){
			int oldVal = 0;
			if(oldValObj!=null) oldVal = ((Number)oldValObj).intValue();
			newVal = new Integer(oldVal + ((Integer)value));
		}else{
			new Exception("unsupported type: " + value).printStackTrace();
			System.exit(1);
		}

		return (Number) lastMap.put(keys[keys.length-1],newVal);
	}

	public static Object multiLevelGet(Map topLevelMap, Object... keys){
		Map workingMap = topLevelMap;
		
		// go to last-level map
		for(int level=0; level<keys.length-1; level++){
			if(workingMap.containsKey(keys[level])){ // get next level map
				workingMap = (Map) workingMap.get(keys[level]);
			}else{
				return null;
			}
		}

		return workingMap.get(keys[keys.length-1]);
	}
	
	public Iterator iterator(int iterateLevel){
		return new MultiLevelMapIterator(topLevelMap,iterateLevel);
	}
	
	public static Iterator iterator(Map topLevelMap, int iterateLevel){
		return new MultiLevelMapIterator(topLevelMap,iterateLevel);
	}
}
