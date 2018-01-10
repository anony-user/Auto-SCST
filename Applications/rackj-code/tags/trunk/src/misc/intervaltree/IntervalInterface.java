package misc.intervaltree;

/**
 * Necessary functions for IntervalTree
 */
public interface IntervalInterface extends Comparable {

    public boolean equals(Object other);

    public int hashCode();

    public int compareTo(Object o);

    public String toString();

    public boolean overlaps(IntervalInterface other);

    public int getLow();

    public int getHigh();

}
