package misc.intervaltree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;




/**
 *
 * Utility class to find objects that flank a certain position.
 *
 *
 */

public class FlankingFinder {
    private RbTree lows;
    private RbTree highs;

    private Map lowRbNodeToObj;
    private Map highRbNodeToObj;

    public FlankingFinder() {
	this.lows = new RbTree();
	this.highs = new RbTree();
	this.lowRbNodeToObj = new WeakHashMap();
	this.highRbNodeToObj = new WeakHashMap();
    }


    public void add(Object obj, int low, int high) {
	RbNode lowNode = new RbNode(low);
	RbNode highNode = new RbNode(high);
	this.lows.insert(lowNode);
	this.highs.insert(highNode);

	this.lowRbNodeToObj.put(lowNode, obj);
	this.highRbNodeToObj.put(highNode, obj);
    }



    public List flankingLeft(int pos, int n) {
	if (this.highs.root.isNull())
	    return new ArrayList();

	RbNode node = this.highs.root;
	RbNode lastNode = node;
	while (node != RbNode.NIL) {
	    if (pos <= node.key)  {
		lastNode = node;
		node = node.left;
	    } else {
		lastNode = node;
		node = node.right;
	    }
	}

	while (lastNode != RbNode.NIL && lastNode.key >= pos) {
	    lastNode = this.highs.predecessor(lastNode);
	}

	List results = new ArrayList();
	for (int i = 0; i < n && lastNode != RbNode.NIL; i++) {
	    results.add(highRbNodeToObj.get(lastNode));
	    lastNode = this.highs.predecessor(lastNode);
	}
	return results;
    }




    public List flankingRight(int pos, int n) {
	if (this.lows.root.isNull())
	    return new ArrayList();

	RbNode node = this.lows.root;
	RbNode lastNode = node;
	while (node != RbNode.NIL) {
	    if (pos <= node.key)  {
		lastNode = node;
		node = node.left;
	    } else {
		lastNode = node;
		node = node.right;
	    }
	}

	while (lastNode != RbNode.NIL && lastNode.key <= pos) {
	    lastNode = this.lows.successor(lastNode);
	}

	List results = new ArrayList();
	for (int i = 0; i < n && lastNode != RbNode.NIL; i++) {
	    results.add(lowRbNodeToObj.get(lastNode));
	    lastNode = this.lows.successor(lastNode);
	}
	return results;
    }
}
