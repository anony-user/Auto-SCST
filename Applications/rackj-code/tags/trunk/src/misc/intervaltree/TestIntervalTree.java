package misc.intervaltree;

import java.util.ArrayList;
import java.util.List;

import misc.Interval;


public class TestIntervalTree {
    private static IntervalTree tree;

    public static void main(String args[]){
        tree = new IntervalTree();

        // test empty
        System.out.println("test empty: " + tree.search(new Interval(0,42)));

        // test overlap (equal)
        tree.insert(new Interval(1, 5));
        System.out.println("test overlap (equal): " + tree.search(new Interval(1, 5)));

        // test overlap (left)
        System.out.println("test overlap (left): " + tree.search(new Interval(0, 3)));

        // test overlap (right)
        System.out.println("test overlap (right): " + tree.search(new Interval(3, 6)));

        // test overlap (enclosed)
        System.out.println("test overlap (enclosed): " + tree.search(new Interval(2, 3)));

        // test overlap (surrounding)
        System.out.println("test overlap (surrounding): " + tree.search(new Interval(0, 6)));

        // test unsuccessful match (right)
        System.out.println("test unsuccessful match (right): " + tree.search(new Interval(6, 10)));

        // test unsuccessful match (left)
        tree = new IntervalTree();
        tree.insert(new Interval(2, 5));
        System.out.println("test unsuccessful match (left): " + tree.search(new Interval(0, 1)));

        // check to see that duplicate intervals work out ok structurally.
        tree = new IntervalTree();
        prepareTestCaseTree();
        int n = tree.size();
        int someNumber = 20;
        for (int i = 0; i < someNumber; i++) {
            prepareTestCaseTree();
        }
        System.out.println("test duplicate");
        assertEquals(n * (1 + someNumber),
                     tree.size());

        // misc tests
        tree = new IntervalTree();
        prepareTestCaseTree();
        System.out.println("misc");
        assertEquals(new Interval(0, 3), searchInterval(-1, 4));
        assertEquals(new Interval(15, 23), searchInterval(15, 15));
        assertEquals(null, searchInterval(11, 14));
        assertEquals(new Interval(25, 30), searchInterval(24, 25));

        // multiple search
        tree = new IntervalTree();
        System.out.println("multiple search");
        prepareTestCaseTree();
        List expected = new ArrayList();
        expected.add(new Interval(0, 3));
        expected.add(new Interval(5, 8));
        expected.add(new Interval(6, 10));
        assertEquals(expected, searchAllIntervals(1, 7));

        // unsuccessful multiple search
        tree = new IntervalTree();
        System.out.println("unsuccessful multiple search");
        prepareTestCaseTree();
        expected = new ArrayList();
        assertEquals(expected, searchAllIntervals(11, 14));

        // testSingleMatchWithMultipleSearch
        tree = new IntervalTree();
        System.out.println("testSingleMatchWithMultipleSearch");
        prepareTestCaseTree();
        expected = new ArrayList();
        expected.add(new Interval(25, 30));
        assertEquals(expected, searchAllIntervals(24, 25));

        // testSingleMatchWithMultipleSearchAndDuplicates
        tree = new IntervalTree();
        System.out.println("testSingleMatchWithMultipleSearchAndDuplicates");
        prepareTestCaseTree();
        prepareTestCaseTree();
        prepareTestCaseTree();
        expected = new ArrayList();
        expected.add(new Interval(25, 30));
        expected.add(new Interval(25, 30));
        expected.add(new Interval(25, 30));
        assertEquals(expected, searchAllIntervals(24, 25));

        // testSearchAllOnWholeResults
        tree = new IntervalTree();
        System.out.println("testSearchAllOnWholeResults");
        expected = new ArrayList();
        int bignumber = 30000;
        for(int i = 0; i < bignumber; i++) {
            tree.insert(new Interval(i, i));
            expected.add(new Interval(i, i));
        }
        assertTrue(tree.isValid());
        assertEquals(expected, searchAllIntervals(0, bignumber-1));
        assertEquals(expected, searchAllIntervals(0, 2*bignumber));
        assertEquals(expected, searchAllIntervals(0, 3*bignumber));
        assertEquals(new ArrayList(), searchAllIntervals(-1, -1));

        // testWithLotsOfOverlapping
        tree = new IntervalTree();
        System.out.println("testWithLotsOfOverlapping");
        bignumber = 1000;
        for(int i = 0; i < bignumber; i++) {
            tree.insert(new Interval(0, i));
        }
        assertTrue(tree.isValid());

        /* At this point, the tree has something that looks like
         -
         --
         ---
         ----
         ...
         */

        for(int i = 0; i < bignumber; i++) {
            assertEquals(i,
                         searchAllIntervals(bignumber - i, bignumber).size());
        }
    }

    private static void assertTrue(boolean val){
        System.out.println(val);
    }

    private static void assertEquals(int val1,int val2){
        System.out.println(val1==val2);
    }

    private static void assertEquals(Object obj1,Object obj2){
        if(obj1!=null){
            System.out.println(obj1.equals(obj2));
        }else if(obj2!=null){
            System.out.println(obj2.equals(obj1));
        }else{
            System.out.println(true);
        }
    }


    private static Interval searchInterval(int low, int high) {
        return (Interval) tree.search(new Interval(low, high));
    }

    private static List searchAllIntervals(int low, int high) {
        List intervals = tree.searchAll(new Interval(low, high));
        java.util.Collections.sort(intervals);
        return intervals;
    }

    // The following test data comes from CLR's "Introduction to
    // Algorithms", on the chapter of Red-Black trees.
    private static void prepareTestCaseTree() {
        tree.insert(new Interval(0, 3));
        tree.insert(new Interval(5, 8));
        tree.insert(new Interval(6, 10));
        tree.insert(new Interval(8, 9));
        tree.insert(new Interval(15, 23));
        tree.insert(new Interval(16, 21));
        tree.insert(new Interval(17, 19));
        tree.insert(new Interval(19, 20));
        tree.insert(new Interval(25, 30));
        tree.insert(new Interval(26, 26));
    }
}
