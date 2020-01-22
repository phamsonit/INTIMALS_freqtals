package be.intimals.freqt.constraint;

import be.intimals.freqt.FTArray;
import be.intimals.freqt.structure.Location;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.structure.Pattern_Int;
import be.intimals.freqt.structure.Projected;
import be.intimals.freqt.util.Variables;

import java.util.*;

public class Constraint {

    /**
     * calculate the support of a pattern = number of files
     * @param projected
     * @return
     */
    public static int getSupport(Projected projected){
        //if(weighted) return projected.getProjectLocationSize();
        int old = 0xffffffff;
        int sup = 0;
        for(int i=0; i<projected.getProjectLocationSize(); ++i) {
            if (projected.getProjectLocation(i).getLocationId() != old)
                ++sup;
            old = projected.getProjectLocation(i).getLocationId();
        }
        return sup;
    }

    /**
     * calculate the root support of a pattern = number of root occurrences
     * @param projected
     * @return
     */
    public static int getRootSupport(Projected projected){
        int rootSup = 1;
        for(int i=0; i< projected.getProjectLocationSize()-1;++i) {
            Location location1 = projected.getProjectLocation(i);
            Location location2 = projected.getProjectLocation(i+1);

            if( (location1.getLocationId() == location2.getLocationId() &&
                    location1.getRoot() != location2.getRoot()) ||
                    location1.getLocationId() != location2.getLocationId()
                    )
                ++rootSup;
        }
        return rootSup;
    }

    /**
     * prune candidates based on minimal support
     * @param candidates
     * @param minSup
     */
    public static void prune (Map<FTArray, Projected > candidates, int minSup){
        Iterator< Map.Entry<FTArray,Projected> > iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<FTArray,Projected> entry = iter.next();
            int sup = getSupport(entry.getValue());
            int wsup = getRootSupport(entry.getValue());
            if(sup < minSup){
                iter.remove();
            }
            else {
                entry.getValue().setProjectedSupport(sup);
                entry.getValue().setProjectedRootSupport(wsup);
            }
        }
    }

    /**
     * prune candidates based on minSup and black labels
     * @param candidates
     * @param minSup
     * @param pat
     * @param _blackLabels
     */
    public static void pruneSupportAndBlacklist(Map <FTArray, Projected > candidates,
                                         int minSup,
                                         FTArray pat,
                                         Map <Integer,ArrayList<Integer>> _blackLabels){

        Iterator < Map.Entry<FTArray,Projected> > can = candidates.entrySet().iterator();
        while (can.hasNext()) {
            Map.Entry<FTArray, Projected> entry = can.next();
            Projected value = entry.getValue();
            int sup = getSupport(value); //5 different *files*
            //int sup = getRootSupport(value); //5 different *things*
            if((sup < minSup) || isBlacklisted(pat,  entry.getKey(), _blackLabels))
                can.remove();
            else {
                value.setProjectedSupport(sup);
                value.setProjectedRootSupport(getRootSupport(value));
            }
        }
    }

    //return true if the label_int is in the set of black labels
    private static boolean checkBlackListLabel(Integer label_int, Collection<ArrayList<Integer>> _blackLabels){
        for(ArrayList<Integer> labels : _blackLabels){
            if(labels.contains(label_int)) return true;
        }
        return false;
    }

    /**
     * prune candidates based on blacklist children
     * blacklist is created in the readWhiteLabel procedure
     * @param candidates
     */
    public static void pruneBlackList(FTArray pat,
                               Map <FTArray, Projected > candidates,
                               Map <Integer,ArrayList<Integer>> _blackLabels){

        Iterator < Map.Entry<FTArray,Projected> > can = candidates.entrySet().iterator();
        while (can.hasNext()) {
            Map.Entry<FTArray, Projected> entry = can.next();
            if (isBlacklisted(pat,  entry.getKey(), _blackLabels)){
                can.remove();
            }
        }
    }

    private static boolean isBlacklisted(FTArray pat, FTArray key, Map <Integer,ArrayList<Integer>> _blackLabels){
        int candidateLabel_int = key.get(key.size()-1);
        return (checkBlackListLabel(candidateLabel_int,_blackLabels.values())) &&
                (Pattern_Int.checkBlackLabels(pat,key,_blackLabels,candidateLabel_int));
    }

    /**
     * return true if the number of nodes is larger or equal to minNode
     * @param pat
     * @return
     */
    public static boolean satisfyMinNode(FTArray pat, int minNode) {
        return Pattern_Int.countNode(pat) >= minNode;
    }

    /**
     * return true if the number of leafs is larger or equal to minLeaf
     * @param pat
     * @return
     */
    public static boolean satisfyMinLeaf(FTArray pat, int minLeaf) {
        return Pattern_Int.countLeafNode(pat) >= minLeaf;
    }

    /**
     * return true if the pattern misses leaf
     * @param pattern
     * @return
     */
    public static boolean satisfyFullLeaf(FTArray pattern) {
        return Pattern_Int.checkMissingLeaf(pattern);
    }

    /**
     * return true if the number of leafs of the pattern is larger than maxLeaf
     * @param pattern
     * @return
     */
    public static boolean satisfyMaxLeaf(FTArray pattern, int maxLeaf) {
        return Pattern_Int.countLeafNode(pattern) > maxLeaf;
    }


    //return true if pattern misses obligatory child at the left side of the current node
    public static boolean checkLeftObligatoryChild(FTArray pat,
                                            FTArray candidate,
                                            Map <Integer,ArrayList <String> > _grammarInt,
                                            Map <Integer,ArrayList<Integer> > _blackLabelsInt){

        boolean missMandatoryChild = false;
        try{
            //1. find all siblings of candidate
            //System.out.println("pattern: "+pat);
            //System.out.println("candidate: "+candidate);

            //parent position of candidate in the patterns
            int parentPos = Pattern_Int.findParentPosition(pat,candidate);
            //String parentLabel = labelIndex.get(pat.get(parentPos));
            //System.out.println("parent pos: "+ parentPos+" label "+parentLabel);

            //find all children of patternLabel in grammar
            ArrayList <String> childrenG = _grammarInt.get(pat.get(parentPos));
            //System.out.println("children in grammar: "+childrenG);

            if(childrenG.get(0).equals("ordered") && !childrenG.get(1).equals("1")){
                //System.out.println("must check obligatory children");
                //get all children of parentPos in pattern
                FTArray childrenP = Pattern_Int.findChildrenPosition(pat,parentPos);

                ArrayList<Integer> blackLabelChildren = new ArrayList<Integer>();
                if(_blackLabelsInt.containsKey(pat.get(parentPos)))
                    blackLabelChildren = _blackLabelsInt.get(pat.get(parentPos));
                //System.out.println("blackLabel "+blackLabelChildren);

                //compare ...
                int i=0;
                int j=2;
                while(i<childrenP.size() && j<childrenG.size() && !missMandatoryChild) {
                    String[] childGrammarTemp = childrenG.get(j).split(Variables.uniChar);
                    int label_int = Integer.valueOf(childGrammarTemp[0]);
                    if(pat.get(childrenP.get(i)) == label_int) {
                        ++i;
                        ++j;
                    }
                    else {
                        //if this child is a mandatory and it is not in the blacklist
                        if ( (childGrammarTemp[1].equals("true") && blackLabelChildren.contains(label_int)) ||
                                (childGrammarTemp[1].equals("false")) )
                            j++;
                        else
                        if( (childGrammarTemp[1].equals("true") && !blackLabelChildren.contains(label_int)) )
                            missMandatoryChild = true;
                        //return true;
                    }
                }
            }
        }catch (Exception e){
            System.out.println("checkObligatoryChildren error "+e);
        }
        return missMandatoryChild;
    }

    // return true if the pattern misses the obligatory child at right side of the current node
    // for each node in the pattern do
    //1. find children of the current node in the pattern
    //2. find children of the current node in the grammar
    //3. compare two set of children to determine the pattern missing mandatory child or not
    public static boolean checkRightObligatoryChild(FTArray pat,
                                             Map <Integer,ArrayList <String> > _grammarInt,
                                             Map <Integer,ArrayList<Integer> > _blackLabelsInt){

        boolean missMandatoryChild = false;
        try{
            //System.out.println(pat);
            for(int pos = 0; pos < pat.size() && !missMandatoryChild; ++pos) {
                int currentLabel = pat.get(pos);
                if(currentLabel >= 0 ){ //consider only internal label
                    //find all children of patternLabel in grammar
                    ArrayList<String> childrenG = _grammarInt.get(currentLabel);
                    if (childrenG.get(0).equals("ordered") && !childrenG.get(1).equals("1")) {
                        //get all children of the current pos in pattern
                        FTArray childrenP = Pattern_Int.findChildrenPosition(pat, pos);
                        if(childrenP.size() > 0){
                            //get black children
                            ArrayList<Integer> blackLabelChildren = new ArrayList<Integer>();
                            if (_blackLabelsInt.containsKey(currentLabel))
                                blackLabelChildren = _blackLabelsInt.get(currentLabel);

                            //compare two sets of children to determine this pattern misses mandatory child or not
                            int i=0;
                            int j=2;
                            while(i<childrenP.size() && j<childrenG.size()) {
                                String[] childGrammarTemp = childrenG.get(j).split(Variables.uniChar);
                                int label_int = Integer.valueOf(childGrammarTemp[0]);

                                if(pat.get(childrenP.get(i)) == label_int) {
                                    ++i;
                                    ++j;
                                }
                                else {
                                    //if this child is a mandatory and it is not in the blacklist
                                    if ( (childGrammarTemp[1].equals("true") && blackLabelChildren.contains(label_int)) ||
                                            (childGrammarTemp[1].equals("false")) )
                                        ++j;
                                    else
                                    if( (childGrammarTemp[1].equals("true") && !blackLabelChildren.contains(label_int)) ) {
                                        missMandatoryChild = true;
                                        break;
                                    }
                                }
                            }
                            if(j < childrenG.size()){
                                while(j < childrenG.size()){
                                    String[] childGrammarTemp = childrenG.get(j).split(Variables.uniChar);
                                    if(childGrammarTemp[1].equals("true") && !blackLabelChildren.contains(Integer.valueOf(childGrammarTemp[0]))) {
                                        missMandatoryChild = true;
                                        break;
                                    }
                                    ++j;
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("checkRightObligatoryChildren error : "+e);
        }
        return missMandatoryChild;
    }


    /////////// specific functions for COBOL source code //////////////////
    public static void checkCobolConstraints(FTArray pattern, Map.Entry<FTArray, Projected> entry, FTArray key,
                                      Map<Integer, String> labelIndex, ArrayList <ArrayList <NodeFreqT> > transaction) {
        //check continuous paragraphs//
        //if potential candidate = SectionStatementBlock then check if candidate belongs to black-section or not
        String candidateLabel = labelIndex.get(key.get(key.size()-1));
        if(candidateLabel.equals("SectionStatementBlock"))
            Constraint.checkBlackSection(entry,transaction);

        //expand the pattern if all paragraphs are continuous
        if(candidateLabel.equals("ParagraphStatementBlock"))
            Constraint.checkContinuousParagraph(pattern, entry, transaction);
    }

    public static void checkContinuousParagraph(FTArray pat,
                                         Map.Entry<FTArray, Projected> entry,
                                         ArrayList <ArrayList <NodeFreqT> >  _transaction){
        try{
            //System.out.println(pat);
            Projected projected = entry.getValue();
            //find parent's location of Paragraph
            int parentPos = Pattern_Int.findParentPosition(pat,entry.getKey());
            //System.out.println("parent "+parentPos);
            //find Paragraph locations
            FTArray childrenPos = Pattern_Int.findChildrenPosition(pat,parentPos);
            //System.out.println("number of paragraphs "+childrenPos.size());
            if (childrenPos.size()==1) return;
            //check continuous paragraphs
            //find the first position in pos --> compare to the last position

            int i=0;
            while(i < projected.getProjectLocationSize()){
                Location pos = projected.getProjectLocation(i);
                int id = pos.getLocationId();
                //System.out.println(pos);

                int firstPos=0;
                for(int j=pos.size()-2; j>0; --j){
                    if(_transaction.get(id).get(pos.get(j)).getNode_label_int() == pat.get(childrenPos.get(childrenPos.size()-2))) {
                        firstPos = pos.get(j);
                        break;
                    }
                }
                int lastPos = pos.get(pos.size()-1);
                //System.out.println(firstPos+" "+lastPost);
                if (_transaction.get(id).get(firstPos).getNodeSibling() != lastPos){
                    //remove paragraph location
                    projected.deleteProjectLocation(i);
                    i--;
                }else
                    i++;
            }
            entry.setValue(projected);
        }catch (Exception e){
            System.out.println("checkContinuousParagraph "+e);
        }
    }
    /**
     * delete locations of a label that belongs to black-section?
     */
    public static void checkBlackSection(Map.Entry<FTArray, Projected> entry, ArrayList  <ArrayList <NodeFreqT> >  _transaction){
        //TODO: read black-section from file
        Set<String> blackSectionList = new HashSet<>();
        blackSectionList.add("*CCVS1");
        blackSectionList.add("*CCVS-EXIT");

        try{
            Projected projected = entry.getValue();
            int i=0;
            while(i < projected.getProjectLocationSize()) {
                //get position of the current label
                int id = projected.getProjectLocation(i).getLocationId();
                //for each location check if it belongs to SectionStatementBlock or not
                int currentPos = projected.getProjectLocation(i).getLocationPos();
                //int searchPos = Location.getLocationPos(projected.getProjectLocation(i));;
                //check if label of section is in black-section or not
                while (currentPos != -1) {
                    //System.out.println("search label " + transaction.elementAt(id).elementAt(searchPos).getNodeLabel());
                    if (blackSectionList.contains(_transaction.get(id).get(currentPos).getNodeLabel())) {
                        //System.out.println("found " + id + " " + searchPos);
                        projected.deleteProjectLocation(i);
                        i--;
                        break;
                    } else {
                        currentPos = _transaction.get(id).get(currentPos).getNodeChild();
                    }
                }
                i++;
            }
            entry.setValue(projected);
        }catch (Exception e){
            System.out.println("Error: Delete SectionStatementBlock "+e);
        }
    }
}
