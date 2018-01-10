package misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The constructor of this class reads a GFF file, then buildup a tree
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class GffTree {

    private static String gffRootFeature = "GffRoot";
    private DefaultMutableTreeNode gffRoot;

    public GffTree(String gffFilename,String idAttr,List parentAttrs){
        // initial root
        gffRoot = new DefaultMutableTreeNode();
        gffRoot.setUserObject(new GffRecord(gffRootFeature,"",null,"",""));
        // parse the GFF file
        parseGFF(gffFilename,idAttr,parentAttrs);
    }

    public DefaultMutableTreeNode getRoot(){
        return gffRoot;
    }

    /**
     * parse and insert GFF content into this GFF tree.
     * @param gffFilename String
     */
    public void parseGFF(String gffFilename,String idAttr,List parentAttrs){

        // tree-structure related
        Map idNodeMap = new HashMap();
        String line="";

        // to-be-assigned collection
        Set tbaCollection = new HashSet();

        // read from the file
        try {
            BufferedReader fr = new BufferedReader(new FileReader(gffFilename));
            while(fr.ready()){
                line = fr.readLine().trim();
                if(line.startsWith("#")) continue;
                if(line.length()==0) continue;

                StringTokenizer st = new StringTokenizer(line,"\t");
                // 1. seqID
                String chr = st.nextToken().intern();
                // 2. source
                st.nextToken();
                // 3. feature
                String feature = st.nextToken().intern();
                // 4. start
                int start = Integer.parseInt(st.nextToken());
                // 5. stop
                int stop = Integer.parseInt(st.nextToken());
                // 6. score
                st.nextToken();
                // 7. strand
                String strand = st.nextToken();
                // 8. frame
                st.nextToken();
                // 9. attributes
                String attrStr = st.nextToken();

                // build node
                GffRecord record = new GffRecord(feature,chr,new Interval(start,stop),strand,attrStr);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(record);
                Map attrMap = record.getAttrMap();

                // insert id-node relation (if any)
                String id = null;
                if(attrMap.containsKey(idAttr)){
                    id = (String) attrMap.get(idAttr);
                    idNodeMap.put(id,node);
                }

                // search all parent ID from attributes, insert into the first found parent
                boolean withSomeParentAttr = false;
                boolean parentFound = false;
                for(Iterator iterator=parentAttrs.iterator();iterator.hasNext();){
                    String parentAttr = (String) iterator.next();

                    // next if not in attr map
                    if(attrMap.containsKey(parentAttr)==false){
                      continue;
                    }
                    withSomeParentAttr = true;

                    // insert this record to a found parent
                    String parentId = (String) attrMap.get(parentAttr);
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) idNodeMap.get(parentId);
                    if(parent!=null){
                      parent.add(node);
                      parentFound = true;
                      break;
                    }
                }
                // insert to the root if without any parent attribute
                if(parentFound==false && withSomeParentAttr==false){
                  gffRoot.add(node);
                }
                // collect if with some parent attribute but not present yet
                if(parentFound==false && withSomeParentAttr==true){
                  tbaCollection.add(node);
                }
            }
            fr.close();
        } catch (Exception ex) {
            System.err.println("error line: " + line);
            ex.printStackTrace();
            System.exit(1);
        }

        int errMsgCnt=0;

        // for records(nodes) that found no parent when reading
        for(Iterator nodeIterator = tbaCollection.iterator();nodeIterator.hasNext();){
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeIterator.next();

          GffRecord record = (GffRecord) node.getUserObject();
          Map attrMap = record.getAttrMap();

          boolean parentFound = false;
          for(Iterator iterator=parentAttrs.iterator();iterator.hasNext();){
              String parentAttr = (String) iterator.next();

              // next if not in attr map
              if(attrMap.containsKey(parentAttr)==false){
                continue;
              }

              // insert this record to a found parent
              String parentId = (String) attrMap.get(parentAttr);
              DefaultMutableTreeNode parent = (DefaultMutableTreeNode) idNodeMap.get(parentId);
              if(parent!=null){
                parent.add(node);
                parentFound = true;
                break;
              }
          }
          if(parentFound==false){
            System.err.print("parent not found for record: " +
                               record.getChr() + "\t" +
                               record.getFeature() + "\t" +
                               record.getInterval().getStart() + "\t" +
                               record.getInterval().getStop() + "\t" +
                               record.getStrand() + "\t" +
                               record.getAttrStr() + "\n"
                );
            errMsgCnt++;
            if(errMsgCnt>=errMsgCntLimit){
                System.err.print("Reach error message limit. Exit.\n");
                System.exit(1);
            }
          }
        }
    }

    private static DefaultMutableTreeNode featureReport(GffTree gffTree){
        DefaultMutableTreeNode featureRoot = new DefaultMutableTreeNode(gffRootFeature);
        featureReportDoit(gffTree.getRoot(),featureRoot);
        return featureRoot;
    }

    private static void featureReportDoit(DefaultMutableTreeNode node,DefaultMutableTreeNode featureNode){
        // for recording child features
        Map featureNodeMap = new LinkedHashMap();

        // recall featureNodeMap (if any)
        for(int i=0;i<featureNode.getChildCount();i++){
            DefaultMutableTreeNode featureChildNode = (DefaultMutableTreeNode) featureNode.getChildAt(i);
            featureNodeMap.put(featureChildNode.getUserObject(),featureChildNode);
        }

        // adding child feature
        for(int i=0;i<node.getChildCount();i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            String childFeature = ((GffRecord)child.getUserObject()).getFeature();
            if(featureNodeMap.containsKey(childFeature)){
                // do nothing
            }else{
                // add one feature child and put into map
                DefaultMutableTreeNode featureChildNode = new DefaultMutableTreeNode(childFeature);
                featureNode.add(featureChildNode);
                featureNodeMap.put(childFeature,featureChildNode);
            }
        }

        // recursion
        for(int i=0;i<node.getChildCount();i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            String childFeature = ((GffRecord)child.getUserObject()).getFeature();
            if(featureNodeMap.containsKey(childFeature)==false){
                // must be something wrong
                System.err.println(((GffRecord)child.getUserObject()).getAttrStr());
                System.exit(1);
            }else{
                // add one feature child and put into map
                DefaultMutableTreeNode featureChildNode = (DefaultMutableTreeNode) featureNodeMap.get(childFeature);
                featureReportDoit(child,featureChildNode);
            }
        }
    }

    private static void saveSimpleTree(DefaultMutableTreeNode node,Map idCheckMap,String outFilename){
        try {
            FileWriter fw = new FileWriter(outFilename);

            saveSimpleTreeDoit(node,idCheckMap,fw);

            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void saveSimpleTreeDoit(DefaultMutableTreeNode node,Map idCheckMap,FileWriter fw) throws IOException {
        // write depth
        for(int i=0;i<node.getLevel();i++) fw.write("  ");
        // get idCheck
        boolean idChk = ((Boolean)idCheckMap.get(node.getUserObject())).booleanValue();
        // write
        fw.write(node.getUserObject().toString());
        if(idChk) fw.write("*");
        fw.write("\n");

        for(int i=0;i<node.getChildCount();i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            saveSimpleTreeDoit(child,idCheckMap,fw);
        }
    }

    private static Map getFeatureWithIdMap(DefaultMutableTreeNode node){
        Map map = new HashMap();
        getFeatureWithIdMap(node,map);
        return map;
    }

    private static void getFeatureWithIdMap(DefaultMutableTreeNode node,Map map){

        GffRecord record = (GffRecord) node.getUserObject();
        boolean idAttrExists = record.getAttrMap().containsKey(idAttrIn);

        if(idAttrExists){
            if(map.containsKey(record.getFeature())==false){
                map.put(record.getFeature(),new Boolean(idAttrExists));
            }
        }else{
            map.put(record.getFeature(),new Boolean(idAttrExists));
        }

        for(int i=0;i<node.getChildCount();i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            getFeatureWithIdMap(child,map);
        }
    }

    static private String inFilename = null;
    static private String idAttrIn = null;
    static private ArrayList parentAttrsIn = new ArrayList();
    
    static private int errMsgCntLimit = 10;

    static private void paraProc(String[] args){
        int i;

        // get parameter strings
        for(i=0;i<args.length;i++){
            if (args[i].equals("-I")) {
                inFilename = args[i + 1];
                i++;
            }
            else if(args[i].equals("-idAttr")){
                idAttrIn = args[i + 1];
                i++;
            }
            else if(args[i].equals("-parentAttr")){
                parentAttrsIn.add(args[i + 1]);
                i++;
            }
        }

        // check for necessary parameters
        if(inFilename==null){
          System.err.println("GFF filename (-I) not assigned");
          System.exit(1);
        }

        // post-processing
        if(idAttrIn==null){
            idAttrIn = "ID";
            System.out.println("ID attribute (-idAttr) not assigned, using default: " + idAttrIn);
        }
        if(parentAttrsIn.size()==0){
            parentAttrsIn.add("Parent");
            parentAttrsIn.add("Derives_from");
            System.out.println("parent attribute list (-parentAttr) not assigned, using default: " + parentAttrsIn);
        }

        // list parameters
        System.out.println("program: GffTree");
        System.out.println("input GFF file (-I): "+inFilename);
        System.out.println("ID attribute (-idAttr): "+idAttrIn);
        System.out.println("parent attribute list (-parentAttr): "+parentAttrsIn);
        System.out.println();
    }

    public static void main(String[] args){
        paraProc(args);

        GffTree gffTree = new GffTree(inFilename,idAttrIn,parentAttrsIn);

        DefaultMutableTreeNode featureRoot = featureReport(gffTree);
        saveSimpleTree(featureRoot,getFeatureWithIdMap(gffTree.getRoot()),inFilename+".features");
    }
}
