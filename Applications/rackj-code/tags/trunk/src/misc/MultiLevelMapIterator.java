package misc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class MultiLevelMapIterator implements Iterator {

	private Map topLevelMap;
	private int iterateLevel;

	private Map[] mapArray; 
	private Iterator[] iteratorArray;
	private Object[] keyArray;
	
	private boolean hasNextAns;
	
	public MultiLevelMapIterator(Map topLevelMap, int iterateLevel){
		this.iterateLevel = iterateLevel;

		mapArray = new Map[iterateLevel];
		iteratorArray = new Iterator[iterateLevel];
		keyArray = new Object[iterateLevel];
		
		mapArray[0] = topLevelMap;
		iteratorArray[0] = mapArray[0].keySet().iterator();

		// go to first validate iteration
		hasNextAns = gotoNextIterationForward(0);
	}
	
	public boolean hasNext() {
		return hasNextAns;
	}

	public Object next() {
		Object[] retArray = Arrays.copyOf(keyArray, iterateLevel);
		
		// go to next validate iteration, if any
		hasNextAns = gotoNextIteration(iterateLevel-1);
		
		return retArray;
	}
	
	private boolean gotoNextIterationForward(int level){
		while(iteratorArray[level].hasNext()){
			keyArray[level] = iteratorArray[level].next();
			// recursion
			if(level < iterateLevel-1){
				mapArray[level+1] = (Map) mapArray[level].get(keyArray[level]);
				iteratorArray[level+1] = mapArray[level+1].keySet().iterator();
				if(gotoNextIterationForward(level+1)){
					return true;
				}
			}else{
				return true;
			}
		}

		return false;
	}
	
	private boolean gotoNextIteration(int level){

		boolean retVal = gotoNextIterationForward(level);
		if(retVal){
			return true;
		}else if(level>0){
			return gotoNextIteration(level-1);
		}

		return false;
	}

	public void remove() {
		// not implemented
	}
}
