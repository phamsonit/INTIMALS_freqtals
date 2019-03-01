package be.intimals.freqt.structure;

import java.util.*;


public class Pattern {
    private  static  char uniChar = '\u00a5';// Japanese Yen symbol

    /**
     * count the number of identifier keywords in a pattern
     * @param pat
     * @return
     */
    public static int countIdentifiers(Vector<String> pat){
        int count=0;

        for(int i=0; i<pat.size(); ++i){
            if(pat.elementAt(i).equals("identifier"))
                ++count;
        }

        return count;
    }

    /**
     * get the string format of a pattern: remove the part missed leaf node
      * @param pat
     * @return
     */
    //all leaf nodes of patterns are also the real leaf nodes in ASTs
    public static String getPatternString1(Vector<String> pat){
        String result="";

        //find the last leaf
        //System.out.println(pat);

        int pos=0;
        for(int i=0; i<pat.size();++i){
            if(pat.elementAt(i).charAt(0)=='*')
                pos = i;
        }
        //System.out.println("post leaf "+pos);
        //output patterns

        int n = 0;
        for(int i = 0; i <= pos; ++i){
            if(pat.elementAt(i).equals(")")) {
                result += pat.elementAt(i);
                --n;
            }
            else{
                ++n;
                result += "(" + pat.elementAt(i);
            }
        }
        for(int i = 0 ; i < n; ++i) {
            result += ")";
        }

        return result;
    }

    /**
     * get string format of the pattern: keep all node
     * i.e., input  = A B ) C
     *       output = (A(B)(C))
     * @param pat
     * @return
     */
    public static String getPatternString(Vector<String> pat){
        String result="";
        int n = 0;
        for(int i = 0; i<pat.size(); ++i){
            if(pat.elementAt(i).equals(")")) {
                result += pat.elementAt(i);
                --n;
            }
            else{
                ++n;
                result += "(" + pat.elementAt(i);
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
            if( ! pat.elementAt(i).equals(")") ) ++size;
        return size;
    }

    /**
     * count number of leaf nodes in a pattern
     * @param pat
     * @return
     */
    public static int countLeafNode(Vector<String> pat){
        int result=0;

        for(int i=0; i<pat.size(); ++i)
            if(pat.elementAt(i).charAt(0)=='*')
                result++;

        return result;
    }

    /**
     * get label of a potential candidate
     * i.e., input  = )¥)¥)¥label, output = label
     * @param candidate
     * @return
     */
    public static String getPotentialCandidateLabel(String candidate){
        String[] p = candidate.split(String.valueOf(uniChar));
        return p[p.length-1];
    }

    /**
     * find parent's position of a given candidate in a pattern
     * @param pat
     * @param candidate
     * @return
     */
    public static int findParentPosition(Vector<String> pat, String candidate){
        int parentPos = 0;
        int nodeLevel = 0;
        int candidateSize = 0;
        try {
            String[] p = candidate.split(String.valueOf(uniChar));
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
                    if (pat.elementAt(i).equals(")"))
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

    /**
     * find all children of the node at the parentPos
     * @param pat
     * @param parentPos
     * @return
     */
    public static Vector<String > findChildren(Vector<String> pat, Integer parentPos){
        int top = -1;
        Vector<String > children = new Vector<>();
        if(parentPos < pat.size()-1){
            for(int i = parentPos+1; i < pat.size(); ++i){
                if(pat.elementAt(i).equals(")"))
                    --top;
                else
                    ++top;
                if(top == 0 && !pat.elementAt(i).equals(")"))
                    children.addElement(pat.elementAt(i));
                if(top == -2) break;
            }
        }
        return  children;
    }

    /**
     * find the firt mandatory child of a node based on input grammar
     * @param listOfChildrenGrammar
     * @return
     */
    private String findTheFirstMandatoryChild(Vector<String> listOfChildrenGrammar){
        String firstMandatoryChildGrammar = "";
        for (int i = 0; i < listOfChildrenGrammar.size(); ++i) {
            String[] tmpChild = listOfChildrenGrammar.elementAt(i).split(String.valueOf(uniChar));
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
    public static boolean isRepeatedLabel(Vector<String> pat, String candidate, int n){

        String[] Temp = candidate.split( String.valueOf(uniChar) );
        String label = Temp[Temp.length-1];
        int parentPos = findParentPosition(pat,candidate);
        Vector<String> children = findChildren(pat,parentPos);

        int count=0;
        for(int i=0; i<children.size();++i){
            if(children.elementAt(i).equals(label))
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
    public static boolean checkNumberLabel(Vector<String> pat, Integer n){
        boolean result = false;
        for(int i=0;i<pat.size()-1;++i){
            int num=0;
            for(int j=i+1; j<pat.size();++j){
                if(!pat.elementAt(i).equals(")") && pat.elementAt(i).equals(pat.elementAt(j)))
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
    public static boolean isMissedLeafNode(Vector<String> pat){
        boolean result = false;
        for(int i=0; i<pat.size()-1;++i) {
            if (!pat.elementAt(i).equals(")") &&
                    pat.elementAt(i+1).equals(")"))
                if (pat.elementAt(i).charAt(0) != '*') {
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
    public static boolean isMissedMandatoryChild(Vector<String> childrenInPattern,
                                                   Vector<String> childrenInGrammar,
                                                   Set<String> blackChildrenList,
                                                   Set<String> whiteChildrenList){
        int i=0;
        int j=0;
        boolean missMandatoryChild = false;
        while(i<childrenInPattern.size() && j<childrenInGrammar.size()) {
            String[] tmpChild = childrenInGrammar.elementAt(j).split(String.valueOf(uniChar));
            if(childrenInPattern.elementAt(i).equals(tmpChild[0])) {
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
    public static Set<String> getChildrenLabels(Map<String,Vector<String>> ListNode, Vector<String> pat, String candidate){

        //add current candidate to pattern
        Set<String> childrenTemp = new LinkedHashSet<>();
        String[] candidateTemp = candidate.split(String.valueOf(uniChar));
        Vector<String> patternTemp = new Vector<>(pat);
        for (int i = 0; i < candidateTemp.length; ++i) {
            if (!candidateTemp[i].isEmpty())
                patternTemp.addElement(candidateTemp[i]);
        }
        //find parent's position of potentialCandidate in patternTemp
        int parentPos = findParentPosition(patternTemp, candidate);
        String parentLabel = patternTemp.elementAt(parentPos).split(String.valueOf(uniChar))[0];
        //find children of the parentLabel
        if(ListNode.containsKey(parentLabel)){
            childrenTemp.addAll(ListNode.get(parentLabel));
        }
        return childrenTemp;
    }

    //checkLineDistance(pattern, candidate, projected, threshold)
    public boolean checkLineDistance(Vector<String> pat,
                                      String candidate,
                                      Projected projected,
                                      int _minLineDistance,
                                      int _maxLineDistance){

        if(isMissedLeafNode(pat)) return false;

        //System.out.println("check line distance");

        //find LineNr of two repeated labels
        String p = getPotentialCandidateLabel(candidate);
        //System.out.println("pattern "+pattern );

        List<List<Integer>> lines = new LinkedList<>();
        int index = 0;
        for (int i = 0; i < pat.size(); ++i) {
            List<Integer> temp = new LinkedList<>();
            if (pat.elementAt(i).equals(p)) {
                for (int j = 0; j < projected.getProjectLocationSize(); ++j) {
                    //System.out.print(projected.getProjectLineNr(j).get(index)+" ");
                    temp.add(projected.getProjectLineNr(j).get(index));
                }
                ++index;
                lines.add(temp);
                //System.out.println();
            } else if (!pat.elementAt(i).equals(")"))
                ++index;
        }
/*
        System.out.println("occurrences ");
        for(int i=0; i<projected.getProjectLocationSize();i++)
            System.out.println(projected.getProjectLocation(i).getLocationList());

        System.out.println("lines of "+ p);
        for(int i=0; i<lines.size();++i)
            System.out.println(lines.get(i));
*/
        //calculate the largest distance
        int maxDistance = 0;

        if (lines.size() > 1) {
            //System.out.println("number of line occurrences "+lines.get(0).size());
            int pos = 0;
            for (int i = 0; i < lines.get(0).size(); ++i) {
                int distance = lines.get(lines.size() - 1).get(i) - lines.get(lines.size() - 2).get(i);
                if(maxDistance < distance) maxDistance = distance;
                //remove occurrence having distance less than threshold
                if((distance < _minLineDistance) && (distance > _maxLineDistance)) {
                    projected.removeProjectLocation(projected.getProjectLocation(i - pos));
                    ++pos;
                }
            }
            //System.out.println();
        }
        //System.out.println("max line distance " + maxDistance);
        if( ((maxDistance >= _minLineDistance) && (maxDistance <= _maxLineDistance)) || maxDistance == 0) return true;
        else return false;
    }


    /**
     * check combination of constraints
     * @param pat
     * @param maxLeaf
     * @return
     */
    public  boolean checkConstraints(Vector<String> pat, int maxLeaf) {

        if(isMissedLeafNode(pat) ||
                (countLeafNode(pat)  >  maxLeaf) )
            return true;
        else
            return false;


    }


    public static Vector<String> formatPattern(String[] pat){
        Vector<String> _pat = new Vector<>();

        for(int i=0; i<pat.length;++i)
            _pat.add(pat[i].trim());

        return _pat;
    }


}
