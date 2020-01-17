package be.intimals.freqt.structure;

import be.intimals.freqt.util.Variables;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;


public class Pattern {

    //subtree representation
    //input subtree
    //    aa
    //    /\
    //   b  c
    //  /    \
    // *1     *2
    //
    //format 1 = a,b,*1,),),c,*2
    //format 2 = (a(b(*1))(c(*2)))

    //TODO: convert format 2 to format 1
  public static String covert(String str){
      ArrayList<String> tmp = new ArrayList<>(); //a list of node labels
      try {
          int len = str.length();
          int size = 0;
          String buff = ""; //store a node label

          int ii = 0;
          while (ii < len) {
              //for(int i = 0; i < len; ++i) //for each char in the str
              //if str.chatAt(i) =='(' open a node
              //if str.chatAt(i) ==')' close a node of a branch
              if (str.charAt(ii) == '(' || str.charAt(ii) == ')') {
                  if (!buff.isEmpty()) {
                      if (buff.charAt(0) == '*') {
                          tmp.add(buff);
                      } else {
                          String[] label = buff.split("_");
                          tmp.add(label[0]);
                      }
                      buff = "";
                      ++size;
                  }
                  if (str.charAt(ii) == ')') tmp.add(")");
              } else
                  if (str.charAt(ii) == '\t' || str.charAt(ii) == ' ') {
                  buff += "_";
                  } else {
                      //adding to find leaf node i.e. *X(120)
                      if (str.charAt(ii) == '*') {
                          int bracket = 0;
                          while (bracket >= 0) {

                              if (str.charAt(ii) == '(')
                                  bracket++;
                              else if (str.charAt(ii) == ')')
                                  bracket--;

                              if (bracket == -1)
                                  break;
                              else {
                                  buff += str.charAt(ii);
                                  ++ii;
                              }
                          }
                          //System.out.println(buff);
                          --ii;
                      } else buff += str.charAt(ii);
                  }
              ++ii;
          }

          for (int i = tmp.size()-1; i >= 0; --i) {
              if (tmp.get(i).equals(")"))
                  tmp.remove(i);
              else
                  break;
          }
      }catch(Exception e){
          System.out.println("Pattern convert " + e);
      }

      return  tmp.toString().substring(1,tmp.toString().length()-1);

  }

    /**
     * filter: remove the parts missed real leafs
     * @param pat
     * @return
     */
    public static ArrayList<String> filter(ArrayList<String> pat){
        ArrayList<String> result = new ArrayList<>();
        //find the last leaf
        //System.out.println(pat);
        int pos=0;
        for(int i=0; i<pat.size();++i){
            if(pat.get(i).charAt(0)=='*')
                pos = i;
        }
        //System.out.println("post leaf "+pos);
        //output patterns
        int n = 0;
        for(int i = 0; i <= pos; ++i){
            result.add(pat.get(i));
        }

        for(int i = pos ; i < pat.size(); ++i) {
            if(pat.get(i).equals(")"))
                result.add(")");
            else
                break;
        }
        return result;
    }


    /**
     * transform format 1 into format 2
     * filter : remove the parts missed real leafs
      * @param pat
     * @return
     */
    //remove part of pattern missing leaf
    public static String getPatternString1(ArrayList<String> pat){
        String result="";
        //find the last leaf
        //System.out.println(pat);
        int pos=0;
        for(int i=0; i<pat.size();++i){
            if(pat.get(i).charAt(0)=='*')
                pos = i;
        }
        //System.out.println("post leaf "+pos);
        //output patterns
        int n = 0;
        for(int i = 0; i <= pos; ++i){
            if(pat.get(i).equals(")")) {
                result += pat.get(i);
                --n;
            }
            else{
                ++n;
                result += "(" + pat.get(i);
            }
        }
        for(int i = 0 ; i < n; ++i) {
            result += ")";
        }
        return result;
    }

    /**
     * transform pattern format 1 into format 2
     * @param pat
     * @return
     */
    public static String getPatternString(ArrayList<String> pat){
        String result="";
        int n = 0;
        for(int i = 0; i<pat.size(); ++i){
            if(pat.get(i).equals(")")) {
                result += pat.get(i);
                --n;
            }
            else{
                ++n;
                result += "(" + pat.get(i);
            }
        }
        for(int i = 0 ; i < n; ++i) {
            result += ")";
        }
        return result;
    }

    /**
     * calculate size (total nodes) of a pattern
     * @param pat
     * @return
     */
    public static int getPatternSize(Vector<String > pat){
        int size = 0;
        for(int i = 0; i < pat.size(); ++i)
            if( ! pat.get(i).equals(")") ) ++size;
        return size;
    }

    /**
     * calculate size (total nodes) of a pattern
     * @param pat
     * @return
     */
    public static int getPatternSize(ArrayList<String > pat){
        int size = 0;
        for(int i = 0; i < pat.size(); ++i)
            if( ! pat.get(i).equals(")") ) ++size;
        return size;
    }

    /**
     * count number of leaf nodes in a pattern
     * @param pat
     * @return
     */
    public static int countLeafNode(ArrayList<String> pat){
        int result=0;

        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i).charAt(0)=='*')
                result++;

        return result;
    }

    /**
     * count the number of identifier keywords in a pattern
     * @param pat
     * @return
     */
    public static int countIdentifiers(ArrayList<String> pat){
        int count=0;

        for(int i=0; i<pat.size(); ++i){
            if(pat.get(i).equals("identifier"))
                ++count;
        }

        return count;
    }

    /**
     * get label of a potential candidate
     * i.e., input  = )¥)¥)¥label, output = label
     * @param candidate
     * @return
     */
    public static String getPotentialCandidateLabel(String candidate){
        String[] p = candidate.split(Variables.uniChar);
        return p[p.length-1];
    }

    public static void addCandidate(ArrayList<String> pat, String candidate){
        //add a candidate to the current pattern
        String[] p = candidate.split(Variables.uniChar);
        for (int i = 0; i < p.length; ++i) {
            if (!p[i].isEmpty())
                pat.add(p[i]);
        }
    }

    /**
     * find parent's position of a given candidate in a pattern
     * @param pat
     * @param candidate
     * @return
     */
    public static int findParentPosition(ArrayList<String> pat, String candidate){
        int parentPos = 0;
        int nodeLevel = 0;
        int candidateSize = 0;
        try {
            String[] p = candidate.split(Variables.uniChar);
            for (int i = 0; i < p.length; ++i) {
                if (p[i].equals(")"))
                    ++nodeLevel;
                if (!p[i].isEmpty())
                    ++candidateSize;
            }

            int size = pat.size() - candidateSize;
            if (nodeLevel == 0) {
                parentPos = size - 1;
            } else {
                for (int i = size - 1; i > 0; --i) {
                    if (pat.get(i).equals(")"))
                        ++nodeLevel;
                    else --nodeLevel;
                    if (nodeLevel == -1) {
                        parentPos = i;
                        break;
                    }
                }
            }
        }
        catch (Exception e) {System.out.println("find parent position error ");}

        return parentPos;
    }

    public static List<Integer> findChildrenPosition(ArrayList<String> pat, Integer parentPos){
        int top = -1;

        List<Integer> tmp = new ArrayList<>();

        if(parentPos < pat.size()-1){
            int count = parentPos;
            for(int i = parentPos+1; i < pat.size(); ++i){
                if(pat.get(i).equals(")"))
                    --top;
                else {
                    ++top;
                    ++count;
                }
                if(top == 0 && !pat.get(i).equals(")")) {
                    tmp.add(i);//before: add(count)
                }
                if(top == -2) break;
            }
        }
        return tmp;
    }

    /**
     * find all children of the node at the parentPos
     * @param pat
     * @param parentPos
     * @return
     */
    public static ArrayList<String > findChildrenLabels(ArrayList<String> pat, Integer parentPos){
        int top = -1;

        //Set<String> children = new ConcurrentSkipListSet<>();
        ArrayList<String > children1 = new ArrayList<>();

        if(parentPos < pat.size()-1){
            for(int i = parentPos+1; i < pat.size(); ++i){
                if(pat.get(i).equals(")"))
                    --top;
                else
                    ++top;
                if(top == 0 && !pat.get(i).equals(")"))
                    //children.add(pat.get(i));
                    children1.add(pat.get(i));
                if(top == -2) break;
            }
        }
        //children1 = new Vector<>(children);
        return  children1;
    }

    /**
     * find the firt mandatory child of a node based on input grammar
     * @param listOfChildrenGrammar
     * @return
     */
    private String findTheFirstMandatoryChild(ArrayList<String> listOfChildrenGrammar){
        String firstMandatoryChildGrammar = "";
        for (int i = 0; i < listOfChildrenGrammar.size(); ++i) {
            String[] tmpChild = listOfChildrenGrammar.get(i).split(Variables.uniChar);
            if (tmpChild[1].equals("true")) {
                firstMandatoryChildGrammar = tmpChild[0];
                break;
            }
        }
        return firstMandatoryChildGrammar;
    }


    /**
     * check if label occurs more than n times in a node-list ?
     * @param pat
     * @param candidate
     * @param n
     * @return
     */
    public static boolean isRepeatedLabel(ArrayList<String> pat, String candidate, int n){

        String[] Temp = candidate.split(Variables.uniChar);
        String label = Temp[Temp.length-1];
        int parentPos = findParentPosition(pat,candidate);
        ArrayList<String> children = findChildrenLabels(pat,parentPos);

        int count=0;
        for(int i=0; i<children.size();++i){
            if(children.get(i).equals(label))
                ++count;
        }
        if(count <= n)
            return true;
        else
            return false;
    }

    /**
     * check if there is a label occurring more than n times in a pattern ?
     * @param pat
     * @param n
     * @return
     */
    public static boolean checkNumberLabel(ArrayList<String> pat, Integer n){
        boolean result = false;
        for(int i=0;i<pat.size()-1;++i){
            int num=0;
            for(int j=i+1; j<pat.size();++j){
                if(!pat.get(i).equals(")") && pat.get(i).equals(pat.get(j)))
                    num ++;
            }
            if(num >= n) {
                result = true;
                break;
                //return result;
            }
        }
        return result;
    }

    /**
     * check if the left part of this pattern missing leaf node
     * @param pat
     * @return true if the pattern misses a leaf node
     */
    public static boolean isMissedLeafNode(ArrayList<String> pat){
        boolean result = false;
        for(int i=0; i<pat.size()-1;++i) {
            if (!pat.get(i).equals(")") &&
                    pat.get(i+1).equals(")"))
                if (pat.get(i).charAt(0) != '*') {
                    result = true;
                }
        }
        return result;
    }


    /**
     * check if a node misses a mandatory child or not
     * @param childrenInPattern
     * @param childrenInGrammar
     * @param blackChildrenList
     * @param whiteChildrenList
     * @return
     */
    public static boolean isMissedMandatoryChild(ArrayList<String> childrenInPattern,
                                                 ArrayList<String> childrenInGrammar,
                                                   Set<String> blackChildrenList,
                                                   Set<String> whiteChildrenList){
        int i=0;
        int j=0;
        boolean missMandatoryChild = false;
        while(i<childrenInPattern.size() && j<childrenInGrammar.size()) {
            String[] tmpChild = childrenInGrammar.get(j).split(Variables.uniChar);
            if(childrenInPattern.get(i).equals(tmpChild[0])) {
                ++i;
                ++j;
            }
            else {
                //if this child is a mandatory and it is not in the blacklist
                if (tmpChild[1].equals("true")  && !blackChildrenList.contains(tmpChild[0]) ||
                        tmpChild[1].equals("false") &&  whiteChildrenList.contains(tmpChild[0]) ){
                    missMandatoryChild = true;
                    break;
                } else j++;
            }
        }
        return missMandatoryChild;
    }


    /**
     * get children of a node in the black or white list
     * @param ListNode
     * @param pat
     * @param candidate
     * @return
     */
    public static Set<String> getChildrenLabels(Map<String,ArrayList<String>> ListNode, ArrayList<String> pat, String candidate){

        //add current candidate to pattern
        Set<String> childrenTemp = new ConcurrentSkipListSet<>(); //LinkedHashSet<>();

        String[] candidateTemp = candidate.split(Variables.uniChar);
        ArrayList<String> patternTemp = new ArrayList<>(pat);
        for (int i = 0; i < candidateTemp.length; ++i) {
            if (!candidateTemp[i].isEmpty())
                patternTemp.add(candidateTemp[i]);
        }
        //find parent's position of potentialCandidate in patternTemp
        int parentPos = findParentPosition(patternTemp, candidate);
        String parentLabel = patternTemp.get(parentPos).split(Variables.uniChar)[0];
        //find children of the parentLabel
        if(ListNode.containsKey(parentLabel)){
            childrenTemp.addAll(ListNode.get(parentLabel));
        }
        return childrenTemp;
    }


    /**
     * check combination of constraints
     * @param pat
     * @param maxLeaf
     * @return
     */
    public  boolean checkConstraints(ArrayList<String> pat, int maxLeaf) {

        if(isMissedLeafNode(pat) ||
                (countLeafNode(pat)  >  maxLeaf) )
            return true;
        else
            return false;


    }


    public static ArrayList<String> formatPattern(String[] pat){
        ArrayList<String> _pat = new ArrayList<>();

        for(int i=0; i<pat.length;++i)
            _pat.add(pat[i].trim());

        return _pat;
    }


}
