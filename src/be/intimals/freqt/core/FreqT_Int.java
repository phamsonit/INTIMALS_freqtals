package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.input.ReadXML_Int;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Initial_Int;
import be.intimals.freqt.util.Variables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Stream;
import java.util.stream.Collectors;


/*
    extended FREQT: replace string labels by int labels
 */
public class FreqT_Int {
    protected Config config;
    protected ArrayList  <ArrayList <NodeFreqT> >  transaction = new ArrayList <>();
    protected Map<String,ArrayList <String> > grammar    = new LinkedHashMap<>();
    protected Map<String,String>          xmlCharacters  = new LinkedHashMap<>();

    //new variables for Integer
    protected Map<Integer,String> labelIndex = new HashMap<>();
    protected Map<Integer,ArrayList<String> > grammarInt    = new LinkedHashMap<>();
    protected Map<Integer,ArrayList<Integer> > blackLabelsInt = new LinkedHashMap<>();
    protected Map<Integer,ArrayList<Integer> > whiteLabelsInt = new LinkedHashMap<>();

    ///////////
    private Map<ArrayList<Integer>,String> MFP = new HashMap<>();
    //store root labels
    private Set<String>    rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    private Map<String,ArrayList<Integer> >  rootIDs = new HashMap<>();
    //store file ids of patterns
    private Map<String,String>  fileIDs = new HashMap<>();
    /////
    //int nbInputFiles;

    ArrayList <Integer> lineNrs = new ArrayList <>();

    private long timeStart;
    private long timeout;
    private boolean finished;

    private static boolean DEBUG = false;

    ////////////////////////////////////////////////////////////////////////////////
    public FreqT_Int(Config config) {
        this.config = config;
    }

    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}
    public Map <String,ArrayList <String>> getGrammar(){return this.grammar;}

     /**
     * store root occurrences of pattern for the second step
     * @param pat
     * @param projected
     */
    private void addRootIDs(ArrayList<Integer> pat, Projected projected){
        try {
            //find root occurrences (id-pos) of pattern
            String rootOccurrences = "";
            for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                rootOccurrences = rootOccurrences +
                        Location.getLocationId(projected.getProjectRootLocation(i)) + ("-") +
                        Location.getLocationPos(projected.getProjectRootLocation(i)) + ";";
            }
            //check the current root occurrences
            boolean isAdded = true;
            Collection<String> l1 = Arrays.asList(rootOccurrences.split(";"));

            Iterator<Map.Entry<String, ArrayList<Integer>>> iter = rootIDs.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry<String, ArrayList<Integer>> entry = iter.next();
                Collection<String> l2 = Arrays.asList(entry.getKey().split(";"));
                //if l1 is super set of l2 then we don't need to add l1 to rootIDs
                if(l1.containsAll(l2)){
                    isAdded = false;
                    break;
                }else {
                    //if l2 is a super set of l1 then remove l2 from rootIDs
                    if (l2.containsAll(l1)) {
                        iter.remove();
                    }
                }
            }

            if(isAdded){
                //keep only the root occurrences and root label
                ArrayList<Integer> rootLabel_int = new ArrayList<>(pat.subList(0,1));
                rootIDs.put(rootOccurrences, rootLabel_int);
            }
        }catch (Exception e){System.out.println("Error: adding rootIDs "+e);}
    }

    //return true if either labels of pat1 contain labels of pat2 or labels of pat2 contain labels of pat1
    private boolean checkSubsetLabel(ArrayList<Integer> pat1, ArrayList<Integer> pat2){

        if( Pattern_Int.countNode(pat1) >= Pattern_Int.countNode(pat2) ){
            return  pat1.containsAll(pat2);
        }
        else {
            return pat2.containsAll(pat1);
        }
    }

    /**
     * check if pat1 is a subtree of pat2 ?
     * return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
     * @param pat1
     * @param pat2
     * @return
     */
    private int checkSubTree(ArrayList<Integer> pat1, ArrayList<Integer> pat2) {

        int fastResult = fastCheckSubTree(pat1,pat2);
        if (fastResult != -1) return fastResult;

        //check subset of labels before check maximality
        if (checkSubsetLabel(pat1, pat2)) {
            //maximality check
            FreqT_Int_subtree fr = new FreqT_Int_subtree(this.config);
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
        }else {
    //        if (fastResult != 0)
    //            fastCheckSubTree(pat1,pat2);  //for debugging: put a breakpoint here
            return 0;
        }
    }

    public int hitCount,missCount;

    // 0 = no subtree
    // 1 = pat1 is a subtree of pat2
    // 2 = pat2 is a subtree of pat1
    private int fastCheckSubTree(ArrayList<Integer> pat1, ArrayList<Integer> pat2){
        hitCount++;
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
            hitCount--;
            missCount++;
            return -1;
        }
    }

    protected boolean hasSubtree(List<Integer> big, List<Integer> small){
        int root = small.get(0); //the root of small
        int smallSize = small.size();
        int bigSize = big.size();
        int startIdx = 0;

        List<Integer> bigPart = big;
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
    private boolean treeIncludes(List<Integer> big, List<Integer> small){
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
    private int skipOver(List<Integer> tree, int offset){
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

    //check output patterns by using minLeaf and minNode
    public boolean checkOutput(ArrayList<Integer> pat){
        if(  Pattern_Int.countLeafNode(pat) >= config.getMinLeaf() &&
             Pattern_Int.countNode(pat)     >= config.getMinNode())
            return true;
        else
            return false;
    }
    /**
     * add a pattern to MFP
     * if pat is a subtree of another pattern in MFP then ignore this pattern
     * else if pat is a super-set of another pattern in the MFP then replace this pattern by pat
     *      else add pat to MFP
     * @param pat
     * @param projected
     * @param _MFP
     */

    public void addMFP(ArrayList<Integer> pat, Projected projected, Map<ArrayList<Integer>,String> _MFP){
        boolean found = false;
        //if pat is already existed in the MFP then return
        if(_MFP.containsKey(pat)) return;
        //pair-wise compare the input pattern to every pattern in _MFP
        Iterator < Map.Entry<ArrayList<Integer>,String> > p = _MFP.entrySet().iterator();
        while(p.hasNext() && !found){
            Map.Entry<ArrayList<Integer>, String> entry = p.next();
            switch (checkSubTree(pat,entry.getKey())){
                case 1:
                    found = true; //patTemp is a subtree of entry.getKey
                    break;
                case 2:
                    p.remove(); //entry.getKey is a subtree of patTemp
                    break;
            }
        }
        if(! found) {
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport(); //=> root location
            int size = Pattern_Int.countNode(pat);

            String patternSupport =
                    String.valueOf(support) + "," +
                            String.valueOf(wsupport) + "," +
                            String.valueOf(size);

            _MFP.put(pat, patternSupport);
        }
    }

    //add frequent tree to FP
    public void addFP(ArrayList<Integer> pat, Projected projected, Map<ArrayList<Integer>,String> _FP){

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern_Int.countNode(pat);

        String patternSupport =
                        String.valueOf(support) + "," +
                        String.valueOf(wsupport) + "," +
                        String.valueOf(size);
        _FP.put(pat, patternSupport);
    }

    //filter maximal patterns from FP
    public Map<ArrayList<Integer>,String> filterFP(Map<ArrayList<Integer>,String> _FP){
        Map<ArrayList<Integer>,String> _MFP = new HashMap<>();
        try{
            Iterator < Map.Entry<ArrayList<Integer>,String> > fp = _FP.entrySet().iterator();
            //for each pattern
            while(fp.hasNext()){
                boolean found = false;
                Map.Entry<ArrayList<Integer>, String> fpEntry = fp.next();

                if(_MFP.isEmpty()){
                    _MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
                else {
                    //check the pattern existing in MFP list ?
                    Iterator<Map.Entry<ArrayList<Integer>, String>> mfp = _MFP.entrySet().iterator();
                    while (mfp.hasNext()) {
                        Map.Entry<ArrayList<Integer>, String> mfpEntry = mfp.next();
                        //check the labels of two subtrees before check maximal subtree
                        if(checkSubsetLabel(fpEntry.getKey(), mfpEntry.getKey())) {
                            switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                                case 1:
                                    found = true;
                                    break;
                                case 2:
                                    mfp.remove();
                                    break;
                            }
                        }
                    }
                    if (!found) {
                        _MFP.put(fpEntry.getKey(), fpEntry.getValue());
                    }
                }
            }
        }catch (Exception e){System.out.println("Error: Filter maximal pattern");}
        return _MFP;
    }

    //parallel filter maximal patterns by using pair-wise checking
    public Map<ArrayList<Integer>,String> filterFP_multi(Map<ArrayList<Integer>,String> FP){
        Map<ArrayList<Integer>,String> MFP = new ConcurrentHashMap<>();
        FP.entrySet().parallelStream().forEach(fpEntry-> {
            boolean found = false;
            if (MFP.isEmpty()) {
                MFP.put(fpEntry.getKey(), fpEntry.getValue());
            } else {
                //check the maximality in MFP list ?
                Iterator<Map.Entry<ArrayList<Integer>,String>> mfp = MFP.entrySet().iterator();
                while (mfp.hasNext()) {
                    Map.Entry<ArrayList<Integer>,String> mfpEntry = mfp.next();
                    //check labels of pat is subset of labels of mfpEntry
                    if(checkSubsetLabel(fpEntry.getKey(), mfpEntry.getKey())) {
                        switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                            case 1:
                                found = true;
                                break;
                            case 2:
                                mfp.remove();
                                break;
                        }
                    }
                }
                if (!found) {
                    MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
            }
        });
        return MFP;
    }

    /**
     * Add the tree to the root IDs or the MFP
     * @param pat
     * @param projected
     */
    private void addTree(ArrayList<Integer> pat, Projected projected){
        //remove the right part of the pattern that misses leafs
        ArrayList<Integer> patTemp = Pattern_Int.getPatternString1(pat);

        //check minsize constraints and right mandatory children before adding pattern
        if(checkOutput(patTemp) && !checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){
            if (config.getTwoStep()) { //store root occurrences for next step
                addRootIDs(patTemp, projected);
            } else{ //check and store pattern to maximal pattern list
                if(config.getFilter())
                    addMFP(patTemp, projected, MFP);
                else
                    addFP(patTemp, projected, MFP);
            }
        }
    }

    /**
     * calculate the support of a pattern = number of files
     * @param projected
     * @return
     */
    private int getSupport(Projected projected){
        //if(weighted) return projected.getProjectLocationSize();
        int old = 0xffffffff;
        int sup = 0;
        for(int i=0; i<projected.getProjectLocationSize(); ++i) {
            if (Location.getLocationId(projected.getProjectLocation(i)) != old)
                ++sup;
            old = Location.getLocationId(projected.getProjectLocation(i));
        }
        return sup;
    }

    /**
     * calculate the root support of a pattern = number of root occurrences
     * @param projected
     * @return
     */
    private int getRootSupport(Projected projected){
        int rootSup = 1;
        for(int i=0; i< projected.getProjectRootLocationSize()-1;++i) {
            int[] location1 = projected.getProjectRootLocation(i);
            int[] location2 = projected.getProjectRootLocation(i+1);

            if( (Location.getLocationId(location1) == Location.getLocationId(location2) &&
                    Location.getLocationPos(location1) != Location.getLocationPos(location2)) ||
                    Location.getLocationId(location1) != Location.getLocationId(location2)
                    )
                ++rootSup;
        }
        return rootSup;
    }
    /**
     * prune candidates based on minimal support
     * @param candidates
     */
    public void prune (Map <ArrayList<Integer>, Projected > candidates, int minSup){
        Iterator < Map.Entry<ArrayList<Integer>,Projected> > iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ArrayList<Integer>,Projected> entry = iter.next();
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

    public void pruneSupportAndBlacklist(Map <ArrayList<Integer>, Projected > candidates,
                                          int minSup,
                                          ArrayList<Integer> pat,
                                          Map <Integer,ArrayList<Integer>> _blackLabels){

        Map<ArrayList<Integer>,Integer> rootSupports = new HashMap<>();

        Iterator < Map.Entry<ArrayList<Integer>,Projected> > can = candidates.entrySet().iterator();
        while (can.hasNext()) {
            Map.Entry<ArrayList<Integer>, Projected> entry = can.next();
            Projected value = entry.getValue();
            int sup = getSupport(value);
            if((sup < minSup) || isBlacklisted(pat,  entry.getKey(), _blackLabels))
                can.remove();
            else {
                value.setProjectedSupport(sup);
                value.setProjectedRootSupport(getRootSupport(value));
                rootSupports.put(entry.getKey(),getRootSupport(value));
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
    public void pruneBlackList(ArrayList<Integer> pat,
                               Map <ArrayList<Integer>, Projected > candidates,
                               Map <Integer,ArrayList<Integer>> _blackLabels){
        Iterator < Map.Entry<ArrayList<Integer>,Projected> > can = candidates.entrySet().iterator();
        while (can.hasNext()) {
            Map.Entry<ArrayList<Integer>, Projected> entry = can.next();
            if (isBlacklisted(pat,  entry.getKey(), _blackLabels)){
                 can.remove();
            }
        }
    }

    public boolean isBlacklisted(ArrayList<Integer> pat, ArrayList<Integer> key, Map <Integer,ArrayList<Integer>> _blackLabels){
        int candidateLabel_int = key.get(key.size()-1);
        return (checkBlackListLabel(candidateLabel_int,_blackLabels.values())) &&
            (Pattern_Int.ChildrenLabelsContains(pat,key,_blackLabels,candidateLabel_int));
}

    //return true if pattern misses obligatory child
    public boolean checkLeftObligatoryChild(ArrayList<Integer> pat,
                                        ArrayList<Integer> candidate,
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
                ArrayList<Integer> childrenP = Pattern_Int.findChildrenPosition(pat,parentPos);

                ArrayList<Integer> blackLabelChildren = new ArrayList<>();
                if(_blackLabelsInt.containsKey(pat.get(parentPos)))
                    blackLabelChildren = _blackLabelsInt.get(pat.get(parentPos));
                //System.out.println("blackLabel "+blackLabelChildren);

                //compare ...
                int i=0;
                int j=2;
                while(i<childrenP.size() && j<childrenG.size() && !missMandatoryChild) {
                    String[] childGrammarTemp = childrenG.get(j).split(Variables.uniChar);
                    int label_int = Integer.valueOf(childGrammarTemp[0]);
                    if(pat.get(childrenP.get(i)).equals(label_int)) {
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

    // for each node in the pattern do
    //1. find children of the current node in the pattern
    //2. find children of the current node in the grammar
    //3. compare two set of children to determine the pattern missing mandatory child or not
    public boolean checkRightObligatoryChild(ArrayList<Integer> pat,
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
                        ArrayList<Integer> childrenP = Pattern_Int.findChildrenPosition(pat, pos);
                        if(childrenP.size() > 0){
                            //get black children
                            ArrayList<Integer> blackLabelChildren = new ArrayList<>();
                            if (_blackLabelsInt.containsKey(currentLabel))
                                blackLabelChildren = _blackLabelsInt.get(currentLabel);

                            //compare two sets of children to determine this pattern misses mandatory child or not
                            int i=0;
                            int j=2;
                            while(i<childrenP.size() && j<childrenG.size()) {
                                String[] childGrammarTemp = childrenG.get(j).split(Variables.uniChar);
                                int label_int = Integer.valueOf(childGrammarTemp[0]);

                                if(pat.get(childrenP.get(i)).equals(label_int)) {
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
    /**
     * generate candidates by using rightmost extension
     * @param projected
     * @return
     */
    public Map< ArrayList<Integer>, Projected> generateCandidates(Projected projected, ArrayList  <ArrayList <NodeFreqT> >  _transaction) {
        Map<ArrayList<Integer>, Projected> candidates = new LinkedHashMap<>();
        //Map<ArrayList<Integer>, Projected> candidates = new ConcurrentHashMap<>();
        //keep the order of elements
        try{
            // For each location, find all candidates
            int depth = projected.getProjectedDepth();
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = Location.getLocationId(projected.getProjectLocation(i));
                int pos = Location.getLocationPos(projected.getProjectLocation(i));
                //keep only the root id and rightmost locations
                //List<Integer> occurrences = Location.getLocationList(projected.getProjectLocation(i)).subList(0,1);
                //keep all locations of pattern
                int[] occurrences = Location.getLocationArr(projected.getProjectLocation(i));
                //keep lineNr to calculate distance of two nodes
                //List<Integer> lines = projected.getProjectLineNr(i);
                //String prefix = "";
                ArrayList<Integer> prefixInt = new ArrayList<>();
                //ArrayList<Integer> prefixInt = new ArrayList<>();
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.get(id).get(pos).getNodeChild() :
                            _transaction.get(id).get(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = _transaction.get(id).get(l).getNodeSibling()) {
                        //String item = prefix + uniChar + _transaction.elementAt(id).elementAt(l).getNodeLabel();
                        //System.out.println(item);
                        ArrayList<Integer> itemInt = new ArrayList<>();
                        itemInt.addAll(prefixInt);
                        itemInt.add(_transaction.get(id).get(l).getNode_label_int());
                        //System.out.println(_transaction.elementAt(id).elementAt(l).getNode_label_int());
                        //String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();
                        Projected value = candidates.get(itemInt);
                        if (value != null) {
                            value.addProjectLocation(id, l, occurrences);//keeping all locations
                            int rootPos = Location.getRoot(projected.getProjectLocation(i));
                            value.setProjectRootLocation(id, rootPos);//keeping root locations
                        } else {
                            Projected tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            int rootPos = Location.getRoot(projected.getProjectLocation(i));
                            tmp.setProjectRootLocation(id, rootPos); //keeping root locations
                            candidates.put(itemInt, tmp);
                        }
                    }
                    if (d != -1) {
                        pos = _transaction.get(id).get(pos).getNodeParent();
                    }
                    //prefix += uniChar + ")";
                    prefixInt.add(-1);
                }
            }
        }
        catch (Exception e){System.out.println("Error: generate candidates " + e);}
        return candidates;
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(ArrayList<Integer> pattern, Projected projected) {
        try{

            //check time out: if current time - timeStar > timeout the return;
            long currentTime = System.currentTimeMillis();
            if(!config.getTwoStep())
                if((currentTime-timeStart) > timeout) {
                    finished = false;
                    return;
                }

            //System.out.println("pattern "+pattern);
            //find candidates
            Map<ArrayList<Integer>, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());

            //prune on minimum support and list of black labels
            pruneSupportAndBlacklist(candidates,config.getMinSupport(),pattern,blackLabelsInt);
            //System.out.println("after minsup + blacklist pruning " + candidates.keySet());

            //if there is no candidate then report the pattern and then stop
            if( candidates.isEmpty()){
                addTree(pattern,projected);
                return;
            }

            //for each candidate expand to the current pattern
            for(Map.Entry<ArrayList<Integer>, Projected> entry : candidates.entrySet()){
                int oldSize = pattern.size();
                ArrayList<Integer> key = entry.getKey() ;
                //add candidate into pattern
                pattern.addAll(key);

                //check continuous paragraphs
                //if potential candidate = SectionStatementBlock then check if candidate belongs to black-section or not
                String candidateLabel = labelIndex.get(key.get(key.size()-1));
                if(candidateLabel.equals("SectionStatementBlock"))
                    checkBlackSection(entry,transaction);
                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock"))
                    checkContinuousParagraph(pattern, entry, transaction);

                if(checkLeftObligatoryChild(pattern, entry.getKey(), grammarInt,blackLabelsInt)){ //constraint on obligatory children
                    //do nothing = don't add pattern to MFP
                }else{
                    if(  (Pattern_Int.countLeafNode(pattern) > config.getMaxLeaf())             //constraint on maximal number of leafs
                       || Pattern_Int.checkMissingLeaf(pattern)                                 //constraint on real leaf node
                        ){
                        addTree(pattern,entry.getValue());
                    }else{
                        project(pattern, entry.getValue());
                    }
                }

                pattern = new ArrayList<>(pattern.subList(0,oldSize));
            }
        }catch (Exception e){System.out.println("Error: Freqt_Int - projected " + e);}
    }

    /**
     * expand single-subtrees to find frequent subtrees based on input parameters
     * @param freq1
     */
    private void findFP(Map < ArrayList<Integer> , Projected > freq1){
        //pattern = new ArrayList <>();
        ArrayList<Integer> pattern = new ArrayList<>();
        Iterator < Map.Entry<ArrayList<Integer>,Projected> > iter = freq1.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry< ArrayList<Integer>,Projected> entry = iter.next();
            entry.getValue().setProjectedDepth(0);
            pattern.addAll(entry.getKey());
            project(pattern, entry.getValue());
            pattern = new ArrayList<>();
        }
    }

    /**
     * Return all frequent subtrees of size 1
     * @return
     */
    private Map<ArrayList<Integer>, Projected> buildFP1(ArrayList  < ArrayList <NodeFreqT> > trans) {
        Map<ArrayList<Integer>, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.get(i).size(); ++j) {
                //String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();

                String node_label = trans.get(i).get(j).getNodeLabel();
                int node_label_id = trans.get(i).get(j).getNode_label_int();
                //String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                //if node_label in rootLabels and lineNr > lineNr threshold
                if(rootLabels.contains(node_label) || rootLabels.isEmpty()){
                    //System.out.println(lineNr+"  "+lineNrs.elementAt(i));
                    //if(Integer.valueOf(lineNr) > lineNrs.elementAt(i)){ //using only for Cobol data
                    //find a list of locations then add it to freq1[node_label].locations
                    if (node_label != null) {
                        //System.out.println("Node "+ node_label+" "+lineNr);
                        //if node_label already exists
                        ArrayList<Integer> temp = new ArrayList<>();
                        temp.add(node_label_id);
                        Projected value = freq1.get(temp);
                        if (value != null) {
                            value.setProjectLocation(i, j);
                            //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            value.setProjectRootLocation(i, j);
                        } else {
                            Projected projected = new Projected();
                            projected.setProjectLocation(i, j);
                            //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            projected.setProjectRootLocation(i, j);
                            freq1.put(temp, projected);
                        }
                    }
                    //}
                }
            }
        }
        return freq1;
    }

    /**
     * run Freqt with file config.properties
     */
    public void run() {
        try{
            //System.out.println("=========running FreqT==========");
            transaction = new ReadXML_Int().readDatabase(config.getAbstractLeafs(), new File(config.getInputFiles()), labelIndex);

            //normal grammar (labels are strings) is used to print patterns
            Initial_Int.initGrammar(config.getInputFiles(),grammar, config.buildGrammar());
            //new grammar (labels are integers) is used to calculate patterns
            Initial_Int.initGrammar_Int(config.getInputFiles(),grammarInt,labelIndex);

            Initial_Int.readWhiteLabel(config.getWhiteLabelFile(), grammarInt, whiteLabelsInt, blackLabelsInt, labelIndex); //read white labels and create black labels
            Initial_Int.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial_Int.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters

            if(DEBUG) {
                //print grammar
                System.out.println("extracted grammar");
                for (Map.Entry<String, ArrayList<String>> g : grammar.entrySet()) {
                    System.out.println(g.getKey() + " " + g.getValue());
                }

                //print grammar
                System.out.println("extracted grammar");
                for (Map.Entry<Integer, ArrayList<String> > g : grammarInt.entrySet()) {
                    System.out.println(g.getKey() + " " + g.getValue());
                }

                //print black list label
                System.out.println("Black list labels");
                for(Map.Entry<Integer,ArrayList<Integer> > entry: blackLabelsInt.entrySet()){
                    System.out.println(entry.getKey()+" "+entry.getValue());
                }

                //print list of labels
                System.out.println("Index labels");
                for(Map.Entry<Integer,String> entry : labelIndex.entrySet()){
                    System.out.println(entry.getKey()+" "+entry.getValue());
                }
            }

            //create report file
            String reportFile = config.getOutputFile().replaceAll("\"","") +"-report.txt";
            FileWriter report = new FileWriter(reportFile);

            log(report,"INPUT");
            log(report,"===================");
            log(report,"- data sources : " + config.getInputFiles());
            log(report,"- input files : " +  transaction.size());
            log(report,"- minSupport : " + config.getMinSupport());
            report.flush();

            timeStart = System.currentTimeMillis();
            timeout = config.getTimeout()*(60*1000);
            finished = true;

            long start = System.currentTimeMillis( );

            System.out.println("Mining frequent subtrees ...");
            //find 1-subtrees
            Map < ArrayList<Integer> , Projected > FP1 = buildFP1(transaction);
            //System.out.println("all candidates " + freq1.keySet());

            //prune 1-subtrees
            prune(FP1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + FP1.keySet());

            //expand 1-subtrees to find frequent subtrees with size constraints
            findFP(FP1);

            System.out.println("Mining maximal frequent subtrees ...");
            if(config.getTwoStep()){//for each group of root occurrences expand to find largest patterns
                log(report,"");
                log(report,"OUTPUT");
                log(report,"===================");
                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                //report the phase 1
                log(report,"- Step 1: Mining frequent patterns with max size constraints");
                //log(report,"\t + Frequent patterns = "+ nbFP);
                log(report, "\t + running time = "+ diff1/1000 +"s");
                log(report,"\t + root occurrences groups = "+ rootIDs.size());
                //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
                //phase 2: find maximal patterns from rootIDs
                log(report,"- Step 2: Mining maximal patterns WITHOUT max size constraint:");

                FreqT_Int_ext_serial freqT_ext = new FreqT_Int_ext_serial(config, this.grammar, this.grammarInt, this.blackLabelsInt, this.whiteLabelsInt,this.xmlCharacters,this.labelIndex,this.transaction);
                //parallel running
                //FreqT_Int_ext_multi freqT_ext = new FreqT_Int_ext_multi(config, this.grammar, this.grammarInt, this.blackLabelsInt, this.whiteLabelsInt,this.xmlCharacters,this.labelIndex,this.transaction);
                freqT_ext.run(rootIDs, start, report);
                //for FastTreeCheck
                System.out.println("fastCheckSubTree Hits: "+ freqT_ext.hitCount+" Misses: "+ freqT_ext.missCount);

            }else{//output maximal patterns in the first step
                log(report,"OUTPUT");
                log(report,"===================");
                if(finished)
                    log(report,"finished search");
                else
                    log(report,"timeout");

                //log(report,"+ Frequent patterns = "+ nbFP);

                String outFile = config.getOutputFile();
                int nbMFP;
                if(config.getFilter()) {
                    nbMFP = MFP.size();
                    outputPatterns(MFP, outFile);
                }else {
                    System.out.println("filter FP: "+MFP.size());
                    long startFilter = System.currentTimeMillis();
                    Map<ArrayList<Integer>,String> mfpTemp = filterFP_multi(MFP);
                    log(report,"filtering time: "+(System.currentTimeMillis()-startFilter)/1000+"s");
                    nbMFP = mfpTemp.size();
                    outputPatterns(mfpTemp,outFile);
                }
                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                log(report,"+ Maximal patterns = "+ nbMFP);
                log(report,"+ Running times = "+ diff1/1000 +" s");
                report.close();
            }
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt_Int");
            e.printStackTrace();
        }
    }


    //print maximal patterns
    public void outputPatterns(Map< ArrayList<Integer>, String> maximalPatterns, String outFile){
        try{
            //create output file to store patterns for mining common patterns
            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");
            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile, config, grammar, xmlCharacters);
            Iterator < Map.Entry<ArrayList<Integer>,String> > iter1 = maximalPatterns.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<ArrayList<Integer>,String> entry = iter1.next();
                ArrayList <String> pat = Pattern_Int.getPatternStr(entry.getKey(),labelIndex);
                String supports = entry.getValue();
                ((XMLOutput) outputMaximalPatterns).report_Int(pat,supports);
                //System.out.println(pat);
                outputCommonPatterns.write(Pattern.getPatternString1(pat)+"\n");
            }
            outputMaximalPatterns.close();

            outputCommonPatterns.flush();
            outputCommonPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }

    public void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

    /////////// specific functions for COBOL source code //////////////////
    public void checkContinuousParagraph(ArrayList<Integer> pat,
                                         Map.Entry<ArrayList<Integer>, Projected> entry,
                                         ArrayList <ArrayList <NodeFreqT> >  _transaction){
        try{
            //System.out.println(pat);
            Projected projected = entry.getValue();
            //find parent's location of Paragraph
            int parentPos = Pattern_Int.findParentPosition(pat,entry.getKey());
            //System.out.println("parent "+parentPos);
            //find Paragraph locations
            List<Integer> childrenPos = Pattern_Int.findChildrenPosition(pat,parentPos);
            //System.out.println("number of paragraphs "+childrenPos.size());
            if (childrenPos.size()==1) return;
            //check continuous paragraphs
            //find the first position in pos --> compare to the last position

            int i=0;
            while(i < projected.getProjectLocationSize()){
                int id = Location.getLocationId(projected.getProjectLocation(i));
               int[] pos = Location.getLocationArr(projected.getProjectLocation(i));
                //System.out.println(pos);

                int firstPos=0;
                for(int j=pos.length-2; j>0; --j){
                    if(_transaction.get(id).get(pos[j]).getNode_label_int() == pat.get(childrenPos.get(childrenPos.size()-2))) {
                        firstPos = pos[j];
                        break;
                    }
                }
                int lastPos = pos[pos.length-1];
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
    public void checkBlackSection(Map.Entry<ArrayList<Integer>, Projected> entry, ArrayList  <ArrayList <NodeFreqT> >  _transaction){
        //TODO: read black-section from file
        Set<String> blackSectionList = new HashSet<>();
        blackSectionList.add("*CCVS1");
        blackSectionList.add("*CCVS-EXIT");

        try{
            Projected projected = entry.getValue();
            int i=0;
            while(i < projected.getProjectLocationSize()) {
                //get position of the current label
                int id = Location.getLocationId(projected.getProjectLocation(i));
                //for each location check if it belongs to SectionStatementBlock or not
                int currentPos = Location.getLocationPos(projected.getProjectLocation(i));
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