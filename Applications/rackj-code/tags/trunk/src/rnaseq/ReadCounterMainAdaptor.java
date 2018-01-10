package rnaseq;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import misc.CanonicalGFF;
import misc.Util;

public abstract class ReadCounterMainAdaptor implements ReadCounter {

    protected String gffFilename = null;
    private Map mappingFilenameMethodMap = new LinkedHashMap();
    private Map mappingMethodMap = null;
    private String outputPrefix = null;
    protected int joinFactor = 2;
    protected float identityCutoff = 0.95F;
    protected boolean useExonRegion = true;
    protected boolean checkByContaining = false;
    protected int minimumOverlap = 8;
    protected boolean checkAllBlocks = true;
    private boolean includeMultiReads = false;
    protected String modelFilename = null;

    // for subclasses
    protected String programName = null;
    private CanonicalGFF cgff = null;

    public CanonicalGFF getCGFF(){
      return cgff;
    }

    protected void subParaProc(String[] args) {

        // get parameter strings
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-M")) {
                mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
                i += 2;
            } else if (args[i].equals("-J")) {
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-ID")) {
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-O")) {
                outputPrefix = args[i + 1];
                i++;
            } else if (args[i].equals("-exon")) {
                useExonRegion = Boolean.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equals("-contain")) {
                checkByContaining = Boolean.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equals("-min")) {
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-ALL")) {
                checkAllBlocks = Boolean.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equals("-multi")) {
                includeMultiReads = Boolean.valueOf(args[i + 1]);
                i++;
            } else if (args[i].equals("-model")) {
                modelFilename = args[i + 1];
                i++;
            }
        }

        // check for necessary parameters
        if (gffFilename == null) {
            System.err.println("canonical GFF filename (-GFF) not assigned");
            System.exit(1);
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator", System.getProperty("java.class.path"), "misc");
        if (mappingFilenameMethodMap.size() <= 0) {
            System.err.println("mapping method/filename (-M) not assigned, available methods:");
            for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
                System.out.println(iterator.next());
            }
            System.exit(1);
        }
        for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext();) {
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext();) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if (outputPrefix == null) {
            System.err.println("output prefix (-O) not assigned");
            System.exit(1);
        }
        if (minimumOverlap < 1) {
            System.err.println("minimum block size (-min) less than 1");
            System.exit(1);
        }
        // post-processing

        // list parameters
        System.out.println("program: " + programName);
        System.out.println("canonical GFF filename (-GFF): " + gffFilename);
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output prefix (-O): " + outputPrefix);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("identity cutoff (-ID): " + identityCutoff);
        System.out.println("use exon region (-exon): " + useExonRegion);
        System.out.println("check by containing (-contain, FALSE for by intersecting): " + checkByContaining);
        System.out.println("minimum overlap (-min): " + minimumOverlap);
        System.out.println("check all alignment blocks (-ALL): " + checkAllBlocks);
        System.out.println("include multi reads (-multi): " + includeMultiReads);
        System.out.println("model filename (-model): " + modelFilename);
        System.out.println();

    }

    protected void subMain(String args[]) {

        subParaProc(args);

        cgff = new CanonicalGFF(gffFilename);

        init2();

        Map geneUniqReadCntMap = new HashMap();

        // computing based on unique reads
        int uniqReadCnt = 0;

        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Entry) iterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            UniqueReadIterator uniqueRI = new UniqueReadIterator( Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
                    identityCutoff, joinFactor, useExonRegion, checkByContaining, minimumOverlap, checkAllBlocks, cgff, geneUniqReadCntMap);
            uniqueRI.iterate(this);
            uniqReadCnt += uniqueRI.uniqReadCnt;
        }
        System.out.println("#uniq reads: " + uniqReadCnt);

        // computing based on multi reads
        if (includeMultiReads) {
            int multiReadCnt = 0;
            int restReadCnt = 0;
            for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Entry) iterator.next();
                String mappingFilename = (String) entry.getKey();
                String mappingMethod = (String) entry.getValue();
                MultiReadIterator multiRI = new MultiReadIterator( Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod),
                        uniqReadCnt, identityCutoff, joinFactor, useExonRegion, checkByContaining, minimumOverlap, checkAllBlocks,
                        cgff, geneUniqReadCntMap, new HashMap());
                multiRI.iterate(this);
                multiReadCnt += multiRI.mlutiReadCnt;
                restReadCnt += multiRI.restReadCnt;
            }
            System.out.println("#multi reads: " + multiReadCnt);
            System.out.println("#mapped reads: " + (uniqReadCnt + multiReadCnt + restReadCnt));
        }

        // output gene coverage report
        this.report(outputPrefix, cgff);
    }

    /**
     * This function is for (1) initializing <i>programName</i>, and (2) adjust
     * all other default parameters. This function should return the
     * <i>ReadCounterMainAdaptor</i> object.
     */
    protected abstract void init1();

    /**
     * Do necessary data preparation after getting parameters.
     */
    protected abstract void init2();

    public static void main(String args[]) {
        // class loader stuffs
        // get a ReadCounterAdaptor instance
        // getting classpath list
        String classpath = System.getProperty("java.class.path");
        ArrayList<File> jarArray = new ArrayList<File>();
        StringTokenizer st = new StringTokenizer(classpath, System.getProperty("path.separator"));
        while (st.hasMoreTokens()) {
            File file = new File(st.nextToken());
            if (file.isFile()) jarArray.add(file);
        }

        File jarList[] = (File[]) jarArray.toArray(new File[jarArray.size()]);
        URL[] urls = new URL[jarList.length];

        int i;
        for (i = 0; i < jarList.length; i++) {
            try {
                urls[i] = jarList[i].toURI().toURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ClassLoader classLoader = null;
        try {
            classLoader = URLClassLoader.newInstance(urls);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        String templetClassName = "rnaseq.ReadCounterMainAdaptor";
        ReadCounterMainAdaptor rcma = null;
        try {
          if(args.length<=0){
            System.out.println("first parameter of ReadCounterMainAdaptor should be given");
            System.exit(1);
          }
            String className = args[0];
            Class templetClass = Class.forName(templetClassName);
            Class subClass = classLoader.loadClass(className);

            if (!templetClass.isAssignableFrom(subClass) || Modifier.isAbstract(subClass.getModifiers()) == true
                    || subClass.isInterface() == true) {
                //
                System.out.println("classname error: first parameter should be a ReadCounterMainAdaptor subclass");
                System.exit(1);
            }
            rcma = (ReadCounterMainAdaptor) subClass.newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        rcma.init1();
        rcma.subMain(args);
    }
}
