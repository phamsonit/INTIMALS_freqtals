package be.intimals.freqt.core;

import be.intimals.freqt.structure.FTArray;
import be.intimals.freqt.config.Config;

public class CheckSubtree {

    public CheckSubtree(){}

    /**
     * check if pat1 is a subtree of pat2 ?
     * return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
     * @param pat1
     * @param pat2
     * @return
     */
    public static int checkSubTree(FTArray pat1, FTArray pat2, Config config) {

        int fastResult = CheckSubtree.fastCheckSubTree(pat1,pat2);
        if (fastResult != -1) return fastResult;

        //check subset of labels before check maximality
        //       if (checkSubsetLabel(pat1, pat2)) {
        //maximality check
        FreqT_Int_subtree fr = new FreqT_Int_subtree(config);
        int pat1Size = pat1.size();
        int pat2Size = pat2.size();
        if(pat1Size < pat2Size)
            fr.checkSubtrees(pat1,pat2);
        else
            fr.checkSubtrees(pat2,pat1);
        if (fr.getOutputPattern() == null) {
            //                if (fastResult != 0 && fastResult != -1)
            //                    fastCheckSubTree(pat1,pat2); //for debugging: put a breakpoint here
            return 0; //not related
        } else {
            if (pat1Size <= pat2Size) {
                //                    if (fastResult != 1 && fastResult != -1)
                //                        fastCheckSubTree(pat1,pat2); //for debugging: put a breakpoint here
                return 1; //pat1 is a subtree of pat2
            } else {
                //                    if (fastResult != 2 && fastResult != -1)
                //                        fastCheckSubTree(pat1,pat2); //for debugging: put a breakpoint here
                return 2; //pat2 is a subtree of pat1
            }
        }
 /*       }else {
    //        if (fastResult != 0)
    //            fastCheckSubTree(pat1,pat2);  //for debugging: put a breakpoint here
            return 0;
        }*/
    }

    // 0 = no subtree
    // 1 = pat1 is a subtree of pat2
    // 2 = pat2 is a subtree of pat1
    private static int fastCheckSubTree(FTArray pat1, FTArray pat2){
        //hitCount++;
        if(pat1.size() == pat2.size())
        {
            if (pat1.equals(pat2)) return 1; else return 0;
        }
        try {
            if (pat1.size() > pat2.size()) {
                if (hasSubtree(pat1, pat2))
                    return 2;
                else
                    return 0;
            }

            //pat2.size() > pat1.size();
            if (hasSubtree(pat2, pat1))
                return 1;
            else
                return 0;
        }
        catch (IndexOutOfBoundsException ex){
            //hitCount--;
            //missCount++;
            return -1;
        }
    }

    public static boolean hasSubtree(FTArray big, FTArray small){
        int root = small.get(0); //the root of small
        int smallSize = small.size();
        int bigSize = big.size();
        int startIdx = 0;

        FTArray bigPart = big;
        while(true) //loop over big, searching for the root
        {
            int rootIdx = bigPart.indexOf(root);
            if (rootIdx == -1)
                return false;
            int bigPartSize =  bigPart.size();
            if (rootIdx + smallSize > bigPartSize)
                return false;
            if(treeIncludes(bigPart.subList(rootIdx, bigPartSize),small))
                return true;
            startIdx += rootIdx+1;
            bigPart = big.subList(startIdx,bigSize); //continue with the rest of the array
        }
    }

    //both big and small have the same root
    //inclusion check ignores sub-trees that are in big but not in small
    private static boolean treeIncludes(FTArray big, FTArray small){
        if (big.size() == small.size()) return big.equals(small);

        int smallSize = small.size();
        int bigSize = big.size();
        int smallIndex = 1;
        int bigIndex = 1;

        while( smallIndex < smallSize) { //loop until the end of the small tree
            if(bigIndex >= bigSize)
                return false; //there is more in small that is not in big
            int bigNode = big.get(bigIndex);
            int smallNode = small.get(smallIndex);

            while (bigNode != smallNode) {
                if (bigNode < -1) {
                    bigIndex += 2; //skip over leaves in big but not in small
                    if (bigIndex >= bigSize)
                        return false; //there is more in small that is not in big
                }
                //in a branch in big that has the same prefix but continues differently in small
                //we need to go back and skip over it -- complex case
                else if (bigNode == -1) {
                    throw new IndexOutOfBoundsException();
                }
                //in big we have a branch that is not in small, skip over it
                else {
                    bigIndex = skipOver(big, bigIndex + 1);
                    if (bigIndex >= bigSize)
                        return false; //there is more in small that is not in big
                }
                bigNode = big.get(bigIndex);
            }

            bigIndex++;smallIndex++;
        }

        return true;
    }

    // in the tree at offset-1 there is the start of a subtree that we should skip over
    // return the offset in the tree after that subtree
    private static int skipOver(FTArray tree, int offset){
        offset++;
        int treeSize = tree.size();
        int recursion = 1; //how deep are we recursing in the subtree
        while(recursion >= 0){
            if(offset >= treeSize)
                return offset; //end of the big tree, break out
            int node = tree.get(offset);
            if (node == -1 )
                recursion--;
            else
                recursion++;

            offset++;
        }
        return offset;
    }



}
