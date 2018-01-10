package misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class BlatReader extends MappingResultIteratorAdaptor {

    public static String methodName = "BLAT";

    private BufferedReader dataFile;
    private String nextLine="";
    private ArrayList nextMappedReadRecords=new ArrayList();

    private String readID=null;
    private int bestScore=0;
    private int readLen=0;

    public BlatReader(String blatFilename) throws FileNotFoundException {
        dataFile = new BufferedReader(new FileReader(new File(blatFilename)));
        fetchNextLineStartWithNumber();
    }

    private void fetchNextMappedRead(){
      try{
        // get readID from current line
        readID = Util.getIthField(nextLine, 9);
        bestScore = Integer.parseInt(Util.getIthField(nextLine, 0));
        readLen = Integer.parseInt(Util.getIthField(nextLine, 10));

        // form a ReadInfo object
        ReadInfo readInfo = new ReadInfo(readID,readLen);
        
        nextMappedReadRecords = new ArrayList();
        nextMappedReadRecords.add(getAlignmentRecord(nextLine,readInfo));

        // get rest lines with the same ID
        while (fetchNextLineStartWithNumber() != null) {
          String line = nextLine;
          String nextReadID = Util.getIthField(line, 9);
          if (nextReadID.equals(readID)) {
            nextMappedReadRecords.add(getAlignmentRecord(line,readInfo));
            // update bestScore
            int score = Integer.parseInt(Util.getIthField(nextLine, 0));
            if (score > bestScore)
              bestScore = score;
          }
          else {
            break;
          }
        }
      }
      catch(Exception ex){
        System.err.println("LINE: " + nextLine);
        ex.printStackTrace();
        System.exit(1);
      }
    }

    protected AlignmentRecord getAlignmentRecord(String line, ReadInfo readInfo) throws Exception{
        StringTokenizer st = new StringTokenizer(line);
        String[] tokens = line.split("\t");
        String subTokens[];

        //  0: match
        int idx = 0;
        float identity = Float.parseFloat(tokens[idx])/readLen;

        idx += 7;
/*
        st.nextToken();//  1: mis-match
        st.nextToken();//  2: rep.match
        st.nextToken();//  3: N's
        st.nextToken();//  4: Q gap cnt
        st.nextToken();//  5: Q gap bps
        st.nextToken();//  6: T gap cnt
        st.nextToken();//  7: T gap bps
 */
        //  8: strand
        idx ++;
        String strand = tokens[idx].intern();

        idx += 4;
/*
        st.nextToken();//  9: Q name
        st.nextToken();// 10: Q size
        st.nextToken();// 11: Q start
        st.nextToken();// 12: Q end
*/

        // 13: T name
        idx ++;
        String chr = tokens[idx].intern();

        idx += 3;
/*
        st.nextToken();// 14: T size
        st.nextToken();// 15: T start
        st.nextToken();// 16: T end
*/

        // 17: block
        idx ++;
        int numBlocks = Integer.parseInt(tokens[idx]);
        // 18: blockSizes
        idx ++;
        subTokens = tokens[idx].split(",");
        int blockSizes[] = new int[numBlocks];
        for(int i=0;i<numBlocks;i++){
            blockSizes[i] = Integer.parseInt(subTokens[i]);
        }
        // 19: qStarts
        idx ++;
        subTokens = tokens[idx].split(",");
        int qStarts[] = new int[numBlocks];
        for(int i=0;i<numBlocks;i++){
            qStarts[i] = Integer.parseInt(subTokens[i])+1;
        }
        // 20: tStarts
        idx ++;
        subTokens = tokens[idx].split(",");
        int tStarts[] = new int[numBlocks];
        for(int i=0;i<numBlocks;i++){
            tStarts[i] = Integer.parseInt(subTokens[i])+1;
        }

        idx += 2;
/*
        st.nextToken();// 21: qSeq
        st.nextToken();// 22: tSeq
*/

        // notice the last parameter blockSizes.clone()
        return new AlignmentRecord(identity,numBlocks,qStarts,blockSizes,chr,tStarts,blockSizes.clone(),strand,line,readInfo);
    }

    public String getReadID(){
        return readID;
    }

    public int getReadLength(){
        return readLen;
    }

    public float getBestIdentity(){
        return ((float)bestScore/(float)readLen);
    }

    public int getNumMatch(){
        return nextMappedReadRecords.size();
    }

    private String fetchNextLineStartWithNumber(){
        try{
        	String line;
            while ((line = dataFile.readLine()) != null) {
                if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                    nextLine = line;
                    return nextLine;
                }
            }
        }
        catch(IOException ex){
            System.err.println(ex.getStackTrace());
            System.exit(1);
        }
        nextLine = null;
        return nextLine;
    }

    public boolean hasNext() {
        if(nextLine==null){
            try {
                dataFile.close();
            } catch (IOException ex) {
                System.err.println(ex.getStackTrace());
                System.exit(1);
            }
            return false;
        }else{
            return true;
        }
    }

    public Object next() {
        fetchNextMappedRead();
        return nextMappedReadRecords;
    }

    static public void main(String args[]) throws IOException {
        for(BlatReader iterator = new BlatReader(args[0]);iterator.hasNext();){
            ArrayList mappingRecords = (ArrayList) iterator.next();
            System.out.println(iterator.getReadID() + "\t" + iterator.getNumMatch() + "\t" + iterator.getBestIdentity());
        }
    }
}
