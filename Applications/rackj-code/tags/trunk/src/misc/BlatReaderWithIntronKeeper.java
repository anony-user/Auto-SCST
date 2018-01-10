package misc;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class BlatReaderWithIntronKeeper extends BlatReaderWithIntronFilter {

    public static String methodName = "intronKeeperBLAT";

    public BlatReaderWithIntronKeeper(String inParameterStr) throws FileNotFoundException {
        super(inParameterStr);
    }

    // override this function to trun a filter into a keeper
    protected void filter(ArrayList list){
        for(int i=list.size()-1;i>=0;i--){
            AlignmentRecord record = (AlignmentRecord) list.get(i);
            boolean match = false;
            for(int j=0;j<record.numBlocks-1;j++){
                int testIntronStart = record.tStarts[j]+record.tBlockSizes[j]-1;
                int testIntronStop  = record.tStarts[j+1];
                GenomeInterval testIntron = new GenomeInterval(record.chr,testIntronStart,testIntronStop);
                if(intronFilterSet.contains(testIntron)){
                    match = true;
                    break;
                }
            }
            if(match==false){
                list.remove(i);
            }
        }
    }
}
