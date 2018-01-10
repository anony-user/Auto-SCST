package misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;

public class PairedBatchIterator extends Observable {

    private Map mappingFilenameMethodMap;
    private Map mappingMethodMap;

    private Iterator filenameMethodIterator;
    private MappingResultIterator mri;

    private Object[] nextReadArray = null;

    private boolean showMsg=false;

    // current file info
    private int mappedReadCnt=0;
    private int processedLines=0;
    private String mappingFileName=null;

    // recording file info when meet end of ANY file
	public LinkedHashMap fileInfo = new LinkedHashMap();
	
	// read counts in a batch
	private int batchCurFileReadCnt = 0;
	
    public PairedBatchIterator(Map mappingFilenameMethodMap, Map mappingMethodMap){
      this(mappingFilenameMethodMap,mappingMethodMap,false);
    }

    public PairedBatchIterator(Map mappingFilenameMethodMap, Map mappingMethodMap, boolean showMsg) {
        this.mappingFilenameMethodMap = mappingFilenameMethodMap;
        this.mappingMethodMap = mappingMethodMap;
        this.showMsg = showMsg;

        filenameMethodIterator = this.mappingFilenameMethodMap.entrySet().iterator();
        Map.Entry entry = (Entry) filenameMethodIterator.next();
        String mappingFilename = (String) entry.getKey();
        String mappingMethod = (String) entry.getValue();
        mri = Util.getMRIinstance(mappingFilename, mappingMethodMap,
                                  mappingMethod);
        if(showMsg){
          System.out.println("processing "+mappingFilename);
        }
        mappingFileName=mappingFilename;

        nextReadArray = getNextReadArray();
    }

    private Object[] getNextReadArray() {
        if (mri.hasNext()) {
            ArrayList mappingRecords = (ArrayList) mri.next();
            Object[] thisReadArray = new Object[3];
            thisReadArray[0] = mappingRecords;
            thisReadArray[1] = mri.getReadID();
            thisReadArray[2] = mri.getBestIdentity();

            // update info
            mappedReadCnt++;
            processedLines+=mappingRecords.size();
            // update batch read counts
            batchCurFileReadCnt++;

            return thisReadArray;
        } else {

        	// store file info
        	updateFileInfo();

        	while (filenameMethodIterator.hasNext()) {
                Map.Entry entry = (Entry) filenameMethodIterator.next();
                String mappingFilename = (String) entry.getKey();
                String mappingMethod = (String) entry.getValue();
                mri = Util.getMRIinstance(mappingFilename, mappingMethodMap,
                                          mappingMethod);
                if(showMsg){
                  System.out.println("processing "+mappingFilename);
                }

                // start of another file
                mappingFileName=mappingFilename;
            	mappedReadCnt=0;
            	processedLines=0;
            	// add one more batch read count
            	batchCurFileReadCnt = 0;

            	if (mri.hasNext()) {
                    ArrayList mappingRecords = (ArrayList) mri.next();
                    Object[] thisReadArray = new Object[3];
                    thisReadArray[0] = mappingRecords;
                    thisReadArray[1] = mri.getReadID();
                    thisReadArray[2] = mri.getBestIdentity();

                    // update info
                    mappedReadCnt++;
                    processedLines+=mappingRecords.size();
                    // update batch read counts
                    batchCurFileReadCnt++;

                    return thisReadArray;
                }

            	updateFileInfo();
            }
            return null;
        }
    }

    public boolean hasNextBatch() {
        if (nextReadArray == null)
            return false;
        else
            return true;
    }

    public Object nextBatch() {
        Set batch = new HashSet();
    	batchStart();

        Object[] thisReadArray = nextReadArray; // the first read
        nextReadArray = getNextReadArray();

        // this read is the very last read
        if (nextReadArray == null) {
            batch.add(thisReadArray);
        }
        // this read and next read are pair-mate
        else if (Util.isReadIDPaired((String) thisReadArray[1],
                                     (String) nextReadArray[1])) {
            batch.add(thisReadArray);
            batch.add(nextReadArray);
            nextReadArray = getNextReadArray();
        }
        // no pair-mate
        else {
            batch.add(thisReadArray);
        }

        return batch;
    }
    
    public ArrayList nextBatch(IdPairedChecker mateChecker){
    	ArrayList batch = new ArrayList();
    	batchStart();

        Object[] thisReadArray = nextReadArray; // the first read
        nextReadArray = getNextReadArray();
        
        ArrayList thisReadAlignments = (ArrayList) thisReadArray[0];
        String thisReadID = ((AlignmentRecord)thisReadAlignments.get(0)).readInfo.getReadID();
        
        ArrayList nextReadAlignments = null;
        String nextReadID = null;
        if(nextReadArray!=null){
        	nextReadAlignments = (ArrayList) nextReadArray[0];
        	nextReadID = ((AlignmentRecord)nextReadAlignments.get(0)).readInfo.getReadID();
        }

        // this read is the very last read
        if (nextReadArray == null) {
            batch.add(thisReadAlignments);
        }
        // this read and next read are pair-mate
        else if (mateChecker.idPaired(thisReadID,nextReadID)) {
            batch.add(thisReadAlignments);
            batch.add(nextReadAlignments);
            nextReadArray = getNextReadArray();
        }
        // no pair-mate
        else {
            batch.add(thisReadAlignments);
        }

        return batch;
    }
    
    public boolean meetFileEnd(){
    	if(fileInfo.size()>0){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    //
    private void updateFileInfo(){
		int[] infoArr = {mappedReadCnt,processedLines,batchCurFileReadCnt};
		fileInfo.put(mappingFileName, infoArr);
    }
        
    private void batchStart(){
    	// reset batch read count list
    	batchCurFileReadCnt = 1;
    	fileInfo.clear();
    }
}
