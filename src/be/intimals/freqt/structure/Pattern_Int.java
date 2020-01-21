package be.intimals.freqt.structure;

import be.intimals.freqt.FTArray;

import java.util.*;

public class Pattern_Int {

    /**
     * return true if candidate is a black label
     * @param pat
     * @param candidate
     * @param ListLabels
     * @param label
     * @return
     */
    public static boolean ChildrenLabelsContains(FTArray pat,
                                                 FTArray candidate,
                                                 Map<Integer,ArrayList<Integer>> ListLabels,
                                                 int label){
        if(pat.size()==1){
            //return true;
            return ListLabels.get(pat.get(0)).contains(label);
        }else{
            FTArray patternTemp = new FTArray(pat);
            patternTemp.addAll(candidate);
            int parentPos = findParentPosition(patternTemp, candidate);
            //find parent's position of potentialCandidate in patternTemp
            int parentLabel_Int = patternTemp.get(parentPos);
            //find children of the parentLabel
            if(ListLabels.containsKey(parentLabel_Int)){
                if (ListLabels.get(parentLabel_Int).contains(label))
                    return true;
            }
        }
        return false;
    }


    //convert pattern of Int into String
    public static ArrayList<String> getPatternStr(FTArray pat, Map<Integer,String> labelIndex){
        ArrayList<String> patStr = new ArrayList<>();
        for(int i=0; i<pat.size(); ++i){
            if(pat.get(i)==-1)
                patStr.add(")");
                else patStr.add(labelIndex.get(pat.get(i)));
        }
        return patStr;
    }

    //remove right-path missed leafs
    public static FTArray getPatternString1(FTArray pat){

        //find the last leaf
        //System.out.println(pat);
        int pos=0;
        for(int i=0; i<pat.size();++i){
            if(pat.get(i) < -1)
                pos = i;
        }

        return pat.subList(0,pos+1);
    }


    public static boolean checkMissingLeaf(FTArray pat){
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


    public static int countLeafNode(FTArray pat){
        int count=0;
        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i) < -1 ) ++count;

        return count;
    }


    public static int countNode(FTArray pat){
        int count=0;
        for(int i=0; i<pat.size(); ++i)
            if(pat.get(i) != -1 ) ++count;

        return count;
    }

    public static int findParentPosition(FTArray pat, FTArray candidate){
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
        ArrayList<Integer> tmp = new ArrayList<Integer>();

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

    public static FTArray findChildrenPosition(FTArray pat, Integer parentPos){
        int top = -1;
        FTArray tmp = new FTArray();

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
