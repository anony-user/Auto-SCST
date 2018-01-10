package misc;

import java.util.ArrayList;
import java.util.Comparator;

public class StringComparator implements Comparator {

  public boolean equals(Object object) {
    return false;
  }

  private ArrayList decomposeString(String str){
    ArrayList list = new ArrayList();
    int i,lastIdx=0;

    boolean inDigit = Character.isDigit(str.charAt(0));

    for(i=0;i<str.length();i++){
      if(Character.isDigit(str.charAt(i)) != inDigit){
        if(inDigit){
          // a number section
          try{
            list.add(new Integer(str.substring(lastIdx, i)));
          }catch(NumberFormatException e){
            list.add(new java.math.BigInteger(str.substring(lastIdx, i)));
          }
        }else{
          // a string section
          list.add(str.substring(lastIdx,i));
        }
        lastIdx=i;
        inDigit = Character.isDigit(str.charAt(i));
      }
    }
    if(inDigit){
      // a number section
      try{
        list.add(new Integer(str.substring(lastIdx)));
      }catch(NumberFormatException e){
        list.add(new java.math.BigInteger(str.substring(lastIdx)));
      }
    }else{
      // a string section
      list.add(str.substring(lastIdx));
    }
    return list;
  }

  private int compareString(String str1,String str2){
    ArrayList list1,list2;
    int i,returnVal;

    list1 = decomposeString(str1);
    list2 = decomposeString(str2);

    for(i=0;i<list1.size() && i<list2.size();i++){
      if((list1.get(i) instanceof Number) && (list2.get(i) instanceof Number)){
        returnVal = ((Comparable) list1.get(i)).compareTo(list2.get(i));
      }else if(list1.get(i) instanceof Number){
        returnVal = -1;
      }else if(list2.get(i) instanceof Number){
        returnVal = 1;
      }else{
        returnVal = ((Comparable) list1.get(i)).compareTo(list2.get(i));
      }
      if(returnVal!=0) return returnVal;
    }

    if((list1.size()-list2.size())!=0){
      return (list1.size()-list2.size());
    }else{
      return str1.compareTo(str1);
    }
  }

  public int compare(Object object1, Object object2) {
    if(!(object1 instanceof String) || !(object2 instanceof String)){
      // it is supposed that object1 and object2 are of String type, but we
      // use Object.equals and assign -1 for non-equal for safty
      if(object1.equals(object2)){
        return 0;
      }else
        return -1;
    }

    String str1 = (String) object1;
    String str2 = (String) object2;

    return compareString(str1,str2);
  }
}
