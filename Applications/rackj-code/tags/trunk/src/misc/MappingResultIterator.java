package misc;

public interface MappingResultIterator {

    public boolean hasNext();

    public Object next();

    public String getReadID();

    public int getReadLength();

    public float getBestIdentity();

    public int getNumMatch();
}
