package misc;

public abstract class MappingResultIteratorAdaptor implements MappingResultIterator {

    public static String methodName="";

    abstract public boolean hasNext();

    abstract public Object next();

    abstract public String getReadID();

    abstract public int getReadLength();

    abstract public float getBestIdentity();

    abstract public int getNumMatch();
}
