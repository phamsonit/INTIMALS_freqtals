package be.intimals.freqt.structure;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class Pattern_Int {


    //get a list of children from ListLabels, i.e., blackLabels or whiteLabels, ...
    public static ArrayList<Integer> getChildrenLabels(ArrayList<Integer> pat,
                                                ArrayList<Integer> candidate,
                                                Map<Integer,ArrayList<Integer>> ListLabels){
        ArrayList<Integer> childrenTemp = new ArrayList<>();
        try{
            //add current candidate to pattern

            if(pat.size()==1){
                return ListLabels.get(pat.get(0));
            }else{
                ArrayList<Integer> patternTemp = new ArrayList<>(pat);
                patternTemp.addAll(candidate);
                //find parent's position of potentialCandidate in patternTemp
                int parentPos = findParentPosition(patternTemp, candidate);
                int parentLabel_Int = patternTemp.get(parentPos);
                //find children of the parentLabel
                if(ListLabels.containsKey(parentLabel_Int)){
                    childrenTemp.addAll(ListLabels.get(parentLabel_Int));
                }

            }


        }catch (Exception e){
            System.out.println("getChirenLabels error: "+e);
        }

        return childrenTemp;
    }


    //return pattern of string format
    public static Vector<String> getPatternStr(ArrayList<Integer> pat, Map<Integer,String> labelIndex){
        Vector<String> patStr = new Vector<>();
        for(int i=0; i<pat.size(); ++i){
            if(pat.get(i)==-1)
                patStr.add(")");
                else patStr.add(labelIndex.get(pat.get(i)));
        }
        return patStr;
    }

    //remove right-path missed leafs
    public static ArrayList<Integer> getPatternString1(ArrayList<Integer> pat){

        //find the last leaf
        //System.out.println(pat);
        int pos=0;
        for(int i=0; i<pat.size();++i){
            if(pat.get(i) < -1)
                pos = i;
        }

        return new ArrayList<>(pat.subList(0,pos+1));
    }


    public static boolean checkMissingLeaf(ArrayList<Integer> pat){
        boolean result = false;
        ///System.out.println(pat);
        for(int i=0; i<pat.size()-1;++i) {
            if ( (pat.get(i) !=- 1) && (pat.get(i+1) == -1) )
                if (pat.get(i) >= 0 ) {
                    result = true;
                    break;
                }
        }
        //System.out.println(result);
        return result;
    }


    public static int countLeafNode(ArrayList<Integer> pat){
        int count=0;
        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i) < -1 ) ++count;

        return count;
    }


    public static int countNode(ArrayList<Integer> pat){
        int count=0;
        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i) != -1 ) ++count;

        return count;
    }

    public static int findParentPosition(ArrayList<Integer> pat, ArrayList<Integer> candidate){
        int parentPos = 0;
        int nodeLevel = 0;
        int candidateSize = 0;
        try {
            //String[] p = candidate.split(String.valueOf(uniChar));
            for (int i = 0; i < candidate.size(); ++i) {
                if (candidate.get(i) == -1)
                    ++nodeLevel;
                //if (candidate.get(i) != -1)
                ++candidateSize;
            }

            int size = pat.size() - candidateSize;
            if (nodeLevel == 0) {
                parentPos = size - 1;
            } else {
                for (int i = size - 1; i > 0; --i) {
                    if (pat.get(i) == -1)
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

    public static ArrayList<Integer> findChildrenPosition(ArrayList<Integer> pat, Integer parentPos){
        int top = -1;
        ArrayList<Integer> tmp = new ArrayList<>();

        if(parentPos < pat.size()-1){
            int count = parentPos;
            for(int i = parentPos+1; i < pat.size(); ++i){
                if(pat.get(i) == -1)
                    --top;
                else {
                    ++top;
                    ++count;
                }
                if(top == 0 && pat.get(i) != -1) {
                    tmp.add(i);
                }
                if(top == -2) break;
            }
        }
        return tmp;
    }

}
