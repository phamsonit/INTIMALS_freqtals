package be.intimals.freqt.core;

import be.intimals.freqt.Config;

import java.util.Vector;

public class Pattern {

    /**
     * Find all children of a position in a pattern.
     *
     * @param pat
     * @param parentPos
     * @return
     */
    public static Vector<String> findChildren(Vector<String> pat, Integer parentPos) {
        int top = -1;
        Vector<String> children = new Vector<>();
        if (parentPos < pat.size() - 1) {
            for (int i = parentPos + 1; i < pat.size(); ++i) {
                if (pat.elementAt(i).equals(")")) {
                    --top;
                } else {
                    ++top;
                }
                if (top == 0 && !pat.elementAt(i).equals(")")) {
                    children.addElement(pat.elementAt(i));
                }
                if (top == -2) break;
            }
        }
        return children;
    }

    /**
     * Find parent's position of a candidate in a pattern.
     *
     * @param pat
     * @param candidate
     * @return
     */
    public static int findParent(Vector<String> pat, char uniChar, String candidate) {
        int parentPos = 0;
        int nodeLevel = 0;
        int candidateSize = 0;
        try {
            String potentialCandidate = "";
            String[] p = candidate.split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (p[i].equals(")")) ++nodeLevel;
                if (!p[i].equals(")")) potentialCandidate = p[i];
                if (!p[i].isEmpty()) ++candidateSize;
            }

            //System.out.println("pattern :" + pat);
            //System.out.println("potential candidate: " + candidate);
            //System.out.println("length of candidate "+candidateSize);
            int size = pat.size() - candidateSize;
            if (nodeLevel == 0) {
                parentPos = size - 1;
            } else {
                for (int i = size - 1; i > 0; --i) {
                    if (pat.elementAt(i).equals(")")) {
                        ++nodeLevel;
                    } else {
                        --nodeLevel;
                    }

                    if (nodeLevel == -1) {
                        parentPos = i;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("find parent position error ");
        }

        return parentPos;

    }

    /**
     * Check if a pattern has a label repeated n times.
     *
     * @param pat
     * @param candidate
     * @param n
     * @return
     */
    public static Boolean checkDuplicateChildren(Vector<String> pat, char uniChar, String candidate, int n) {
        int parentPos = 0;
        int nodeLevel = 0;
        int candidateSize = 0;
        Boolean duplication = false;

        String potentialCandidate = "";
        String[] p = candidate.split(String.valueOf(uniChar));

        for (int i = 0; i < p.length; ++i) {
            if (p[i].equals(")")) ++nodeLevel;
            if (!p[i].equals(")")) potentialCandidate = p[i];
            if (!p[i].isEmpty()) ++candidateSize;
        }

        int size = pat.size() - candidateSize;

        if (nodeLevel == 0) {
            parentPos = size - 1;
        } else {
            for (int i = size - 1; i > 0; --i) {
                if (pat.elementAt(i).equals(")")) {
                    ++nodeLevel;
                } else {
                    --nodeLevel;
                }

                if (nodeLevel == -1) {
                    parentPos = i;
                    break;
                }
            }
        }

        //find list children
        //System.out.println("parent: "+ pat.elementAt(parentPos));
        //System.out.println("list of existing children : ");
        int nbRepetition = 0;
        if (size >= 1) {
            int top = -1;
            Vector<String> children = new Vector<>();
            for (int i = parentPos + 1; i < size; ++i) {
                if (pat.elementAt(i).equals(")")) {
                    --top;
                } else {
                    ++top;
                }
                if (top == 0 && !pat.elementAt(i).equals(")")) {
                    children.addElement(pat.elementAt(i));
                }
            }

            //if potential candidate is one of children then return true

            for (int i = 0; i < children.size(); ++i) {
                //String[] tmp = children.elementAt(i).split(String.valueOf(uniChar));
                //if (tmp[0].equals(potentialCandidate)) {
                if (children.elementAt(i).equals(potentialCandidate)) {
                    //System.out.print(children.elementAt(i) + " ");
                    //duplication = true;
                    ++nbRepetition;
                    //break;
                }
            }
        }
        //System.out.println(duplication);
        return nbRepetition < n;
    }

    /**
     * Check if there is a label occurring more than n times in a pattern.
     *
     * @param pat
     * @param n
     * @return
     */
    public static boolean checkNumberLabel(Vector<String> pat, Integer n) {
        boolean result = false;
        for (int i = 0; i < pat.size() - 1; ++i) {
            int num = 0;
            for (int j = i + 1; j < pat.size(); ++j) {
                if (!pat.elementAt(i).equals(")") && pat.elementAt(i).equals(pat.elementAt(j))) num++;
            }
            if (num >= n) {
                result = true;
                break;
                //return result;
            }
        }

        return result;
    }

    /**
     * Calculate size of a pattern.
     *
     * @param pat
     * @return
     */
    public static int getPatternSize(Vector<String> pat) {
        int size = 0;
        try {
            for (int i = 0; i < pat.size(); ++i) {
                if (!pat.elementAt(i).equals(")")) ++size;
            }
        } catch (Exception e) {
            //System.out.println("get pattern size error : "+ size+" " + e);
        }
        return size;
    }

    /**
     * Count number of leaf nodes in a pattern.
     *
     * @param pat
     * @return
     */
    public static int countLeafNode(Vector<String> pat) {
        int nbLeaf = 0;
        for (int i = 0; i < pat.size(); ++i) {
            if (pat.elementAt(i).charAt(0) == '*') ++nbLeaf;
        }
        return nbLeaf;
    }

    /**
     * Check if the left part of this subtree misses leaf node.
     *
     * @param pat
     * @return
     */
    public static boolean checkMissedLeafNode(Vector<String> pat) {
        boolean result = false;
        for (int i = 0; i < pat.size() - 1; ++i) {
            if (!pat.elementAt(i).equals(")")
                    && pat.elementAt(i + 1).equals(")")) {
                if (pat.elementAt(i).charAt(0) != '*') {
                    result = true;
                }
            }
            //problem: the right part misses leaf
            //TODO:how to delete right part of this pattern then print it
        }
        return result;
    }

    /**
     * Check a node having all children ?
     * @param pat
     * @param nodeName
     * @return
     */
    // TODO never used
    //static boolean checkMandatoryChild(Vector<String> pat, Map<String,Vector<String>> grammar, String nodeName){
    //    boolean result = false;
    //    for(int i=0; i<pat.size(); ++i)
    //        if(pat.elementAt(i).equals(nodeName))
    //        {
    //            Vector<String> listOfChild = FreqT.findChildren(pat,i);
    //            String degree = grammar.get(nodeName).elementAt(1);
    //            if(!degree.equals(String.valueOf(listOfChild.size())))
    //                return true;//result = true;
    //            else
    //                result = false;
    //        }
    //    return result;
    //}

    /**
     * Returns true if pattern respects constraints.
     *
     * @param pat
     * @return
     */
    public static boolean checkConstraints(Config config, Vector<String> pat) {
        // Check min pattern size
        int size = Pattern.getPatternSize(pat);
        if (size < config.getMinPatternSize()) return false;

        // Check min number of leaves
        if (Pattern.countLeafNode(pat) < config.getMinLeaf()) return false;

        return true;
    }
}
