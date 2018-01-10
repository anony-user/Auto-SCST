package misc;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import misc.filter.FilterInterface;

public class Util {

    public static String uniqString = "uniq";
    public static String multiString = "multi";
    public static String spliceString = "splice";

    public static String getIthField(String line,String delimiter,int idx){
        String[] tokens = line.split(delimiter);
        return tokens[idx];
    }

    public static String getIthField(String line,int idx){
        return getIthField(line,"\t",idx);
    }
	
	public static FilterInterface getFilterInstance(String method,Map methodMap){
        FilterInterface filter = null;

        try{
            Class filterClass = (Class) methodMap.get(method);
            Constructor constructor = filterClass.getConstructor();
            filter = (FilterInterface) constructor.newInstance();
        }
        catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }

        return filter;
    }

    public static MappingResultIterator getMRIinstance(String mappingSrc,Map mappingMethodMap,String mappingMethod){
        MappingResultIterator mri = null;

        try{
            Class mriClass = (Class) mappingMethodMap.get(mappingMethod);
            Class[] mriConstructorParameter = {String.class};
            Constructor mriConstructor = mriClass.getConstructor(mriConstructorParameter);
            Object[] mriparameter = {mappingSrc};
            mri = (MappingResultIterator) mriConstructor.newInstance(mriparameter);
        }
        catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }

        return mri;
    }

    public static Map getMethodMap(String templetClassName,String classpath,String packageName){
        int i;
        Map methodMap = new LinkedHashMap();

        // getting classpath list
        ArrayList jarArray = new ArrayList();
        StringTokenizer st = new StringTokenizer(classpath,System.getProperty("path.separator"));
        while(st.hasMoreTokens()){
            File file = new File(st.nextToken());
            if(file.isFile()){
                jarArray.add(file);
            }
        }
        File jarList[] = (File[]) jarArray.toArray(new File[jarArray.size()]);

        URL[] urls = new URL[jarList.length];
        for(i=0;i<jarList.length;i++){
          try {
              urls[i] = jarList[i].toURI().toURL();
          } catch (Exception e) {
              e.printStackTrace();
          }
        }

        ClassLoader classLoader=null;
        try {
          classLoader = URLClassLoader.newInstance(urls);
        }
        catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }

        int enumCnt=0;
        String tmpStr,classPathStr="";
        File classPathFile;
        JarEntry je;

        try {
          Class templetClass = Class.forName(templetClassName);

          for(i=0;i<jarList.length;i++){
            JarFile jf = new JarFile(jarList[i]);
            Enumeration jfEnum = jf.entries();

            while (jfEnum.hasMoreElements()) {
              je = (JarEntry) jfEnum.nextElement();
              if (je.getName().toLowerCase().endsWith(".class")) {
                enumCnt++;
                tmpStr = je.getName();
                if(tmpStr.startsWith(packageName)==false) continue;
                classPathFile = new File(tmpStr.substring(0, tmpStr.length() - 6));
                classPathStr = "";
                while (classPathFile != null) {
                  classPathStr = classPathFile.getName() + "." + classPathStr;
                  classPathFile = classPathFile.getParentFile();
                }
                classPathStr = classPathStr.substring(0, classPathStr.length() - 1);
                // load
                Class methodClass = classLoader.loadClass(classPathStr);

                if(templetClass.isAssignableFrom(methodClass)
                   && Modifier.isAbstract(methodClass.getModifiers())==false
                   && methodClass.isInterface()==false){
                  // here we have method name
                  try{
                      String methodStr = (String) methodClass.getField(
                              "methodName").get(null);
                      methodMap.put(methodStr, methodClass);
                  }catch(NoSuchFieldException ex){
                      System.err.println("class " + classPathStr + " doesn't have field methodName");
                  }
                }
              }
            }
          }
        }
        catch (Exception ex) {
          ex.printStackTrace();
          System.exit(1);
        }

        return methodMap;
    }

    public static boolean isReadIDPaired(String readA, String readB){
      return isReadIDPaired(readA, readB, -1);
    }

    public static boolean isReadIDPaired(String readA, String readB, int tailTrimming){

      if((readA.trim().length()<=0)||(readB.trim().length()<=0)) return false;

      // detect tailing identical part
      if(tailTrimming<0){
        for (tailTrimming = 1; tailTrimming < readA.length() && tailTrimming < readB.length(); tailTrimming++) {
          char tailA = readA.charAt(readA.length()-tailTrimming);
          char tailB = readB.charAt(readB.length()-tailTrimming);
          if(tailA!=tailB){
            break;
          }
        }
      }

      String strA = readA.substring(0,readA.length()-tailTrimming);
      String strB = readB.substring(0,readB.length()-tailTrimming);
      if(strA.equals(strB)){
        return true;
      }else{
        return false;
      }
    }

    public static CanonicalGFF getIntronicCGFF(CanonicalGFF exonCGFF){
      return getIntronicCGFF(exonCGFF,false);
    }

    public static CanonicalGFF getIntronicCGFF(CanonicalGFF exonCGFF, boolean keepGeneRegion){
        // forming chromosome-intronSets Map
        Map chromosomeIntronSetsMap = new TreeMap();
        // for each gene
        for(Iterator geneIterator = exonCGFF.geneRegionMap.keySet().iterator();geneIterator.hasNext();){
            Object geneID = geneIterator.next();
            GenomeInterval gi = (GenomeInterval) exonCGFF.geneRegionMap.get(geneID);
            Set exonRegions = (Set) exonCGFF.geneExonRegionMap.get(geneID);
            if(exonRegions.size()<=1){
              if(keepGeneRegion){
                if(chromosomeIntronSetsMap.containsKey(gi.getChr())){
                    Set intronSets = (Set) chromosomeIntronSetsMap.get(gi.getChr());
                    intronSets.add(geneID);
                }else{
                    Set intronSets = new LinkedHashSet();
                    intronSets.add(geneID);
                    chromosomeIntronSetsMap.put(gi.getChr(),intronSets);
                }
                continue;
              }else{
                // skip gene without any intron
                continue;
              }
            }
            // build intron intervals
            Set intronRegions = new TreeSet();
            Iterator exonIterator = exonRegions.iterator();
            Interval lastExon = (Interval) exonIterator.next();
            for(;exonIterator.hasNext();){
                Interval thisExon = (Interval) exonIterator.next();
                intronRegions.add(new Interval(lastExon.getStop()+1,thisExon.getStart()-1,geneID));
                lastExon = thisExon;
            }
            // insert into chromosome-intronSet Map
            if(chromosomeIntronSetsMap.containsKey(gi.getChr())){
                Set intronSets = (Set) chromosomeIntronSetsMap.get(gi.getChr());
                intronSets.add(intronRegions);
            }else{
                Set intronSets = new LinkedHashSet();
                intronSets.add(intronRegions);
                chromosomeIntronSetsMap.put(gi.getChr(),intronSets);
            }
        }
        // build corresponding CGFF
        CanonicalGFF intronCGFF;
        if(keepGeneRegion){
          intronCGFF = new CanonicalGFF(chromosomeIntronSetsMap,exonCGFF.geneRegionMap);
        }else{
          intronCGFF = new CanonicalGFF(chromosomeIntronSetsMap);
        }
        intronCGFF.geneStrandMap.putAll(exonCGFF.geneStrandMap);
        intronCGFF.chrOriginalMap.putAll(exonCGFF.chrOriginalMap);

        return intronCGFF;
    }

    public static boolean blockCheck(Interval exonInterval, Interval blockTarget,
    		int exonIndex, int exonNum,
    		boolean checkByContaining, int minimumOverlap){
    	return blockCheck(exonInterval,blockTarget,exonIndex,exonNum,checkByContaining,minimumOverlap,true,true);
    }

    public static boolean blockCheck(Interval exonInterval, Interval blockTarget,
    		int exonIndex, int exonNum,
    		boolean checkByContaining, int minimumOverlap,
    		boolean interMinorFix,boolean outerMinorFix){
      if(checkByContaining){
          // check containment
          if(exonInterval.contain(blockTarget.getStart(),blockTarget.getStop())==false) return false;
          // check overlap size

          if( (blockTarget.length() >= minimumOverlap) || // qualified if intersection >= threshold
              ( interMinorFix &&
            	(exonIndex>0 && exonIndex<(exonNum-1)) &&
            	(blockTarget.getStart()==exonInterval.getStart() && blockTarget.getStop()==exonInterval.getStop())
    		  ) || // OR a inter-minor interval fits the block
    		  ( outerMinorFix &&
    			(exonIndex==0 || exonIndex==(exonNum-1))
			  ) // OR a outer-minor interval
            ){
        	  // do nothing
          }else{
        	  return false;
          }
      }else{
          // check intersection
          if(exonInterval.intersect(blockTarget)==false) return false;
          // check overlap size
          int intersectStart = (blockTarget.getStart()>exonInterval.getStart()) ? blockTarget.getStart() : exonInterval.getStart() ;
          int intersectStop = (blockTarget.getStop()<exonInterval.getStop()) ? blockTarget.getStop() : exonInterval.getStop() ;
          int intersectSize = intersectStop-intersectStart+1;

          if( (intersectSize >= minimumOverlap) || // qualified if intersection >= threshold
    		  ( interMinorFix &&
    			(exonIndex>0 && exonIndex<(exonNum-1)) &&
              	blockTarget.contain(exonInterval.getStart(), exonInterval.getStop())
			  ) || // OR a inter-minor interval contained by the block
			  ( outerMinorFix &&
			    (exonIndex==0 || exonIndex==(exonNum-1)) &&
		    	(exonInterval.length() < minimumOverlap)
			  ) // OR a outer-minor interval
        	){
        	  // do nothing
          }else{
        	  return false;
          }
      }
      return true;
    }

    /**
     * For a set of Map.Entry that is sorted by values.
     * From: http://stackoverflow.com/questions/2864840/treemap-sort-by-value
     * @param map The input Map whose entries are going to be sorted.
     * @param increasingOrder true for increasing order; otherwise, decreasing order.
     * @return A SortedSet of entries of the input Map.
     */
    static <K extends Comparable<? super K>,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map, final boolean increasingOrder) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                	if(increasingOrder){
	                    int res = e1.getValue().compareTo(e2.getValue());
	                    return res != 0 ? res : e1.getKey().compareTo(e2.getKey());
                	}else{
	                    int res = e2.getValue().compareTo(e1.getValue());
	                    return res != 0 ? res : e2.getKey().compareTo(e1.getKey());
                	}
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
