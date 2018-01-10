package statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import misc.StringComparator;

public class CountComparator {

    private static ArrayList sourceFilenameList = new ArrayList();
    private static String baseFilename = null;
    private static int minimumCount = 2;
    private static int lineLimit = 60000;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-S")) {
                sourceFilenameList.add(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-B")){
                baseFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-min")){
                minimumCount = Integer.parseInt(args[i + 1]);
                i++;
            }
            else if(args[i].equals("-limit")){
                lineLimit = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        // check for necessary parameters
        if(sourceFilenameList.size()<=0){
          System.err.println("source table(s) (-S) not assigned");
          System.exit(1);
        }
        if(baseFilename==null){
            System.err.println("base table (-B) not assigned");
            System.exit(1);
        }
        if(lineLimit<0){
            System.err.println("line limit (-limit) less than 0");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: CountComparator");
        System.out.println("source table(s) (-S): "+sourceFilenameList);
        System.out.println("base table (-B): "+baseFilename);
        System.out.println("minimum count (-min): "+minimumCount);
        System.out.println("line limit (-limit) (0: no limit): "+lineLimit);
        System.out.println();
    }

    public static void main(String args[]){
        paraProc(args);

        Map baseMap = readGroupCountMap(baseFilename);
        for(int i=0;i<sourceFilenameList.size();i++){
            String sourceFilename = (String) sourceFilenameList.get(i);
            Map sourceMap = readGroupCountMap(sourceFilename);
            Map canonicalListMap = getCanonicalListMap(baseMap,sourceMap);

            Map reportingMap = new HashMap();
            final Map groupPvalueMapTmp = new HashMap();
            int n11,n12,n21,n22;

            // do computation according to cannicalListMap
            for(Iterator groupIterator = canonicalListMap.keySet().iterator();groupIterator.hasNext();){
                String group = (String) groupIterator.next();
                Set itemSet = (Set) canonicalListMap.get(group);
                Map itemValueMapBase = (Map) baseMap.get(group);
                if(itemValueMapBase==null) itemValueMapBase = new HashMap();
                Map itemValueMapSource = (Map) sourceMap.get(group);
                if(itemValueMapSource==null) itemValueMapSource = new HashMap();
                Map itemValueMapReporting = new LinkedHashMap();
                reportingMap.put(group,itemValueMapReporting);
                Double pValueMSgroup = new Double(1.0);
                for(Iterator itemIterator1 = itemSet.iterator();itemIterator1.hasNext();){
                    String item1 = (String) itemIterator1.next();
                    // get n11 & n12
                    Float baseValue1 = (Float) itemValueMapBase.get(item1);
                    if(baseValue1!=null) n11 = Math.round(baseValue1.floatValue()); else n11 = 0;
                    Float sourceValue1 = (Float) itemValueMapSource.get(item1);
                    if(sourceValue1!=null) n12 = Math.round(sourceValue1.floatValue()); else n12 = 0;
                    // MS for most significant
                    Double pValueMSitem1 = new Double(1.0);
                    String item2MS = (String) itemSet.iterator().next();
                    for(Iterator itemIterator2 = itemSet.iterator();itemIterator2.hasNext();){
                        String item2 = (String) itemIterator2.next();
                        if(item2.equals(item1)) continue;
                        // get n21 & n22
                        Float baseValue2 = (Float) itemValueMapBase.get(item2);
                        if(baseValue2!=null) n21 = Math.round(baseValue2.floatValue()); else n21 = 0;
                        Float sourceValue2 = (Float) itemValueMapSource.get(item2);
                        if(sourceValue2!=null) n22 = Math.round(sourceValue2.floatValue()); else n22 = 0;
                        // Fisher's exact test
                        Double pValue = new FisherExactTest(n11,n12,n21,n22).getTwoTail();
                        if (pValue.doubleValue() < pValueMSitem1.doubleValue()) {
                            pValueMSitem1 = pValue;
                            item2MS = item2;
                        }
                    }
                    // for item1, now we have most significant P-value (pValueMSitem1) and corresponding item2 (item2MS)
                    Object[] reportArray = {item2MS,pValueMSitem1};
                    itemValueMapReporting.put(item1,reportArray);
                    if(pValueMSitem1.doubleValue() < pValueMSgroup.doubleValue()){
                        pValueMSgroup = pValueMSitem1;
                    }
                }
                groupPvalueMapTmp.put(group,pValueMSgroup);
            }

            // sort groups by P-values
            Map groupPvalueMap = new TreeMap(new Comparator(){
                public int compare(Object o1, Object o2) {
                    Double v1 = (Double) groupPvalueMapTmp.get(o1);
                    Double v2 = (Double) groupPvalueMapTmp.get(o2);
                    if(v1.compareTo(v2)!=0){
                        return v1.compareTo(v2);
                    }else{
                        return ((Comparable)o1).compareTo(o2);
                    }
                }

                public boolean equals(Object obj) {
                    return false;
                }
            });
            groupPvalueMap.putAll(groupPvalueMapTmp);

            // reporting
            File srcFile = new File((String) sourceFilenameList.get(i));
            report(baseFilename+"-"+srcFile.getName(),lineLimit,reportingMap,groupPvalueMap,baseMap,sourceMap,minimumCount);
        }
    }

    private static void report(String outFilename,int lineLimit,Map reportingMap,Map groupPvalueMap,Map baseMap,Map sourceMap,int minimumCount){
        try {
            FileWriter fw = new FileWriter(outFilename);

            int lineCnt = 0;
            for(Iterator groupIterator = groupPvalueMap.keySet().iterator();groupIterator.hasNext();){
                String group = (String) groupIterator.next();
                fw.write("Group" + "\t" + group + "\t" + groupPvalueMap.get(group) + "\n");
                lineCnt++;
                Map itemValueMapReporting = (Map) reportingMap.get(group);
                Map itemValueMapBase = (Map) baseMap.get(group);
                if(itemValueMapBase==null) itemValueMapBase = new HashMap();
                Map itemValueMapSource = (Map) sourceMap.get(group);
                if(itemValueMapSource==null) itemValueMapSource = new HashMap();
                for(Iterator itemIterator = itemValueMapReporting.keySet().iterator();itemIterator.hasNext();){
                    String item1 = (String) itemIterator.next();
                    Object[] reportArray = (Object[]) itemValueMapReporting.get(item1);
                    String item2 = (String) reportArray[0];
                    Double pValue = (Double) reportArray[1];
                    int baseCnt,sourceCnt;
                    Float baseValue1 = (Float) itemValueMapBase.get(item1);
                    if(baseValue1!=null) baseCnt = Math.round(baseValue1.floatValue()); else baseCnt = 0;
                    Float sourceValue1 = (Float) itemValueMapSource.get(item1);
                    if(sourceValue1!=null) sourceCnt = Math.round(sourceValue1.floatValue()); else sourceCnt = 0;
                    if(baseCnt<minimumCount && sourceCnt<minimumCount){
                        lineCnt--;
                        continue;
                    }
                    fw.write(item1 + "\t");
                    fw.write(baseCnt + "\t" + sourceCnt + "\t");
                    fw.write(item2 + "\t" + pValue + "\n");
                }
                lineCnt += (itemValueMapReporting.size());
                if(lineLimit>0 && lineCnt>lineLimit) break;
            }

            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static Map getCanonicalListMap(Map map1,Map map2){
        Map canonicalListMap = new HashMap();

        Set groupSet = new HashSet();
        groupSet.addAll(map1.keySet());
        groupSet.addAll(map2.keySet());

        for(Iterator groupIterator = groupSet.iterator();groupIterator.hasNext();){
            String group = (String) groupIterator.next();
            Map itemValueMap1 = (Map) map1.get(group);
            Map itemValueMap2 = (Map) map2.get(group);
            Set itemSet = new TreeSet(new StringComparator());
            if(itemValueMap1!=null) itemSet.addAll(itemValueMap1.keySet());
            if(itemValueMap2!=null) itemSet.addAll(itemValueMap2.keySet());
            canonicalListMap.put(group,itemSet);
        }

        return canonicalListMap;
    }

    public static Map readGroupCountMap(String filename){
        Map groupItemValueMap = new HashMap();

        try{
            BufferedReader fr = new BufferedReader(new FileReader(new File(filename)));

            while(fr.ready()){
                String tokens[] = fr.readLine().split("\t");
                // group
                String group = tokens[0];
                // item
                String item = tokens[1];
                // value
                Float value = null;
                try{
                    value = new Float(tokens[2]);
                }
                catch(NumberFormatException ex){
                    continue;
                }
                // group-item-value
                Map itemValueMap;
                if(groupItemValueMap.containsKey(group)){
                    itemValueMap = (Map) groupItemValueMap.get(group);
                }else{
                    itemValueMap = new LinkedHashMap();
                    groupItemValueMap.put(group,itemValueMap);
                }
                itemValueMap.put(item,value);
            }

            fr.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
            System.exit(1);
        }

        return groupItemValueMap;
    }
}
