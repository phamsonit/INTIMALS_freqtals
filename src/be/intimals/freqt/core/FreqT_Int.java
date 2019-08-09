package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.input.ReadXML_Int;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Initial_Int;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/*
    extended FREQT: replace string labels by int labels
 */
public class FreqT_Int {
    static char uniChar = '\u00a5';// Japanese Yen symbol
    protected Config config;
    protected Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    protected Map <String,Vector<String> > grammar    = new LinkedHashMap<>();
    //protected Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    //protected Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    protected Map <String,String>          xmlCharacters  = new LinkedHashMap<>();


    //new variables for Integer
    protected Map<Integer,String> labelIndex = new HashMap<>();
    protected Map <Integer,Vector<String> > grammarInt    = new LinkedHashMap<>();
    protected Map <Integer,ArrayList<Integer> > blackLabelsInt = new LinkedHashMap<>();
    protected Map <Integer,ArrayList<Integer> > whiteLabelsInt = new LinkedHashMap<>();

    ///////////
    private Map<ArrayList<Integer>,String> MFP = new HashMap<>();
    //store root labels
    private Set <String>    rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    //private Map<String,String>  rootIDs = new HashMap<>();
    private Map<String,ArrayList<Integer> >  rootIDs = new HashMap<>();
    //store file ids of patterns
    private Map<String,String>  fileIDs = new HashMap<>();
    /////
    //int nbInputFiles;
    private int nbFP;

    Vector<Integer> lineNrs = new Vector<>();

    private long timeStart;
    private long timeout;
    private boolean finished;

    ////////////////////////////////////////////////////////////////////////////////
    public FreqT_Int(Config config) {
        this.config = config;
    }

    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}
    public Map <String,Vector<String>> getGrammar(){return this.grammar;}


    /**
     * filter root occurrences: keep the smallest root occurrences
     * @param _rootIDs
     */
    private Map<String, ArrayList<Integer> >  filterRootIDs(Map<String, ArrayList<Integer> > _rootIDs){
        /*for each element of rootIDs check
        if rootOcc is a subset of an element of rootIDs then replace*/
        List<String> ttt = new LinkedList(_rootIDs.keySet());
        Collections.sort(ttt, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (Integer.valueOf(o1.length()).compareTo(o2.length()));
            }
        });

        //System.out.println(ttt);
        for(int i=0; i<ttt.size()-1; ++i) {
            for (int j = i + 1; j < ttt.size(); ++j) {
                if (compareTwoRootOccurrences(ttt.get(i), ttt.get(j)))
                    ttt.remove(j);
            }
        }
        //System.out.println(ttt);
        Map<String, ArrayList<Integer> > _newRootIDs = new LinkedHashMap<>();
         for(int i=0; i<ttt.size(); ++i)
             if(_rootIDs.containsKey(ttt.get(i))) {
                 _newRootIDs.put(ttt.get(i), _rootIDs.get(ttt.get(i)));
             }

         return _newRootIDs;
         //garbage collector
         //System.gc();
    }

    /**
     * compare two sets of root occurrences
     * @param str1
     * @param str2
     * @return
     */
    private boolean compareTwoRootOccurrences(String str1, String str2){
        Collection<String> l1 = Arrays.asList(str1.split(";"));
        Collection<String> l2 = Arrays.asList(str2.split(";"));

        if(l2.containsAll(l1))
            return true;
        else
            return false;
    }

    /**
     * store root occurrences of pattern for the second step
     * @param pat
     * @param projected
     */
    private void addRootIDs(ArrayList<Integer> pat, Projected projected, Map<String,ArrayList<Integer>> _rootIDs){
        try {
            if(Pattern_Int.countLeafNode(pat) > config.getMinLeaf()){
                //find root occurrences (id-pos) of pattern
                String rootOccurrences = "";
                for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                    rootOccurrences = rootOccurrences +
                            Location.getLocationId(projected.getProjectRootLocation(i)) + ("-") +
                            Location.getLocationPos(projected.getProjectRootLocation(i)) + ";";
                }
                //keep only the root occurrences and root label

                ArrayList<Integer> rootLabel_int = new ArrayList<>(pat.subList(0,1));
                _rootIDs.put(rootOccurrences, rootLabel_int);
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
        //check subset of labels before check maximality
        if (checkSubsetLabel(pat1, pat2)) {
            //maximality check
            FreqT_Int_subtree fr = new FreqT_Int_subtree(this.config);
            int pat1Size = Pattern_Int.countNode(pat1);
            int pat2Size = Pattern_Int.countNode(pat2);
            if(pat1Size < pat2Size)
                fr.checkSubtrees(pat1,pat2);
            else
                fr.checkSubtrees(pat2,pat1);
            if (fr.getOutputPattern() == null) {
                return 0; //not related
            } else {
                if (pat1Size <= pat2Size) {
                    return 1; //pat1 is a subtree of pat2
                } else {
                    return 2; //pat2 is a subtree of pat1
                }
            }
        }else {
            return 0;
        }
    }


    //check output patterns by using minLeaf and minNode
    private boolean checkOutput(ArrayList<Integer> pat){
        if(Pattern_Int.countLeafNode(pat) >= config.getMinLeaf() &&
                Pattern_Int.countNode(pat) >= config.getMinNode())
            return true;
        else return false;

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

        //apply minimal number of node constraint
        if(checkOutput(pat)){
            boolean found = false;
            //keep left parts of pattern which have real leaf
            ArrayList<Integer> patTemp = Pattern_Int.getPatternString1(pat);

            if(_MFP.containsKey(patTemp)) return;

            //pair-wise compare the input pattern to every pattern in _MFP
            Iterator < Map.Entry<ArrayList<Integer>,String> > p = _MFP.entrySet().iterator();
            while(p.hasNext() && !found){
                Map.Entry<ArrayList<Integer>, String> entry = p.next();
                switch (checkSubTree(patTemp,entry.getKey())){
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
                int size = Pattern_Int.countNode(patTemp);

                String patternSupport =
                        String.valueOf(support) + "," +
                                String.valueOf(wsupport) + "," +
                                String.valueOf(size);

                _MFP.put(patTemp, patternSupport);
            }

        }
    }

    //add frequent tree to FP
    public void addFP(ArrayList<Integer> pat, Projected projected, Map<ArrayList<Integer>,String> _FP){
        if (checkOutput(pat)) {
            //keep left parts of pattern which have real leaf
            ArrayList<Integer> patTemp = Pattern_Int.getPatternString1(pat);
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport(); //=> root location
            int size = Pattern_Int.countNode(patTemp);

            String patternSupport =
                            String.valueOf(support) + "," +
                            String.valueOf(wsupport) + "," +
                            String.valueOf(size);
            _FP.put(patTemp, patternSupport);
        }
    }

    //find maximal patterns from FP
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
     * choose output
     * @param pat
     * @param projected
     */
    private void chooseOutput(ArrayList<Integer> pat,Projected projected){

        if (config.getTwoStep()) { //store root occurrences for next step
            addRootIDs(pat, projected, rootIDs);
        } else{ //check and store pattern to maximal pattern list
            if(config.getFilter())
                addMFP(pat, projected, MFP);
            else
                addFP(pat, projected, MFP);
        }
    }

    /**
     * calculate the support of a pattern = number of files
     * @param projected
     * @return
     */
    private int support(Projected projected){
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
    private int rootSupport(Projected projected){
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
            int sup = support(entry.getValue());
            int wsup = rootSupport(entry.getValue());
            if(sup < minSup){
                iter.remove();
            }
            else {
                entry.getValue().setProjectedSupport(sup);
                entry.getValue().setProjectedRootSupport(wsup);
            }
        }
    }


    //return true if the label_int is in the set of black lables
    private static boolean checkBlackListLabel(Integer label_int, Map<Integer,ArrayList<Integer>> _blackLabels){
        boolean found = false;
        Iterator<Map.Entry<Integer,ArrayList<Integer>>> iter = _blackLabels.entrySet().iterator();
        while(iter.hasNext() && !found){
            Map.Entry<Integer,ArrayList<Integer>> entry = iter.next();
            if(entry.getValue().contains(label_int)) {
                found = true;
            }
        }
        return found;
    }

    /**
     * prune candidates based on blacklist children
     * blacklist is created in the readWhiteLabel procedure
     * @param candidate
     */
    public void pruneBlackList(ArrayList<Integer> pat,
                               Map <ArrayList<Integer>, Projected > candidate,
                               Map <Integer,ArrayList<Integer>> _blackLabels){

        try{
            Iterator < Map.Entry<ArrayList<Integer>,Projected> > can = candidate.entrySet().iterator();
            while (can.hasNext()) {

                Map.Entry<ArrayList<Integer>, Projected> entry = can.next();
                int candidateLabel_int = entry.getKey().get(entry.getKey().size()-1);
                //check if it is in the blackListLabel
                if(checkBlackListLabel(candidateLabel_int,_blackLabels)){
                    ArrayList<Integer> blackListChildren = Pattern_Int.getChildrenLabels(pat,entry.getKey(),_blackLabels);
                    if(blackListChildren.contains(candidateLabel_int)){
                        can.remove();
                    }
                }
            }

        }catch (Exception e){
            System.out.println("PrungBlackList error "+e);
        }

    }

    //return true if pattern misses obligatory child
    public boolean checkObligatoryChild(ArrayList<Integer> pat,
                                        ArrayList<Integer> candidate,
                                        Map <Integer,Vector<String> > _grammarInt,
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
            Vector<String> childrenG = _grammarInt.get(pat.get(parentPos));
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
                    String[] childGrammarTemp = childrenG.elementAt(j).split(String.valueOf(uniChar));
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


    /**
     * generate candidates by using rightmost extension
     * @param projected
     * @return
     */
    public Map< ArrayList<Integer>, Projected> generateCandidates(Projected projected, Vector <Vector<NodeFreqT> >  _transaction) {
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
                List<Integer> occurrences = Location.getLocationList(projected.getProjectLocation(i));
                //keep lineNr to calculate distance of two nodes
                //List<Integer> lines = projected.getProjectLineNr(i);
                //String prefix = "";
                ArrayList<Integer> prefixInt = new ArrayList<>();
                //ArrayList<Integer> prefixInt = new ArrayList<>();
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.elementAt(id).elementAt(pos).getNodeChild() :
                            _transaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = _transaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        //String item = prefix + uniChar + _transaction.elementAt(id).elementAt(l).getNodeLabel();
                        //System.out.println(item);
                        ArrayList<Integer> itemInt = new ArrayList<>();
                        itemInt.addAll(prefixInt);
                        itemInt.add(_transaction.elementAt(id).elementAt(l).getNode_label_int());
                        //System.out.println(_transaction.elementAt(id).elementAt(l).getNode_label_int());
                        //String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();
                        Projected tmp;
                        if (candidates.containsKey(itemInt)) {
                            candidates.get(itemInt).addProjectLocation(id, l, occurrences);//keeping all locations
                            int rootPos = Location.getLocationList(projected.getProjectLocation(i)).get(0);
                            candidates.get(itemInt).setProjectRootLocation(id, rootPos);//keeping root locations
                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            int rootPos = Location.getLocationList(projected.getProjectLocation(i)).get(0);
                            tmp.setProjectRootLocation(id, rootPos); //keeping root locations
                            candidates.put(itemInt, tmp);
                        }
                    }
                    if (d != -1) {
                        pos = _transaction.elementAt(id).elementAt(pos).getNodeParent();
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

            //constraint 0: minimum support
            prune(candidates, config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());

            //constraint on list of black labels
            pruneBlackList(pattern, candidates, blackLabelsInt);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report the pattern and then stop
            if( candidates.isEmpty() ){
                chooseOutput(pattern,projected);
                //System.out.println("no candidate "+pattern);
                return;
            }
            //for each candidate expand to the current pattern
            Iterator < Map.Entry<ArrayList<Integer>,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = pattern.size();
                Map.Entry<ArrayList<Integer>, Projected> entry = iter.next();
                //System.out.println("potential candidate: "+entry.getKey());
                //add candidate into pattern
                pattern.addAll(entry.getKey());

                //check continuous paragraphs
                //if potential candidate = SectionStatementBlock then check if candidate belongs to black-section or not
                String candidateLabel = labelIndex.get(entry.getKey().get(entry.getKey().size()-1));
                if(candidateLabel.equals("SectionStatementBlock"))
                    checkBlackSection(entry,transaction);
                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock")) {
                    checkContinuousParagraph(pattern, entry, transaction);
                }

                //constraint on maximal number of leafs
                if(Pattern_Int.countLeafNode(pattern) > config.getMaxLeaf()){
                    //System.out.println("max leaf size "+pattern);
                    chooseOutput(pattern,entry.getValue());
                }else{
                    //constraint on real leaf node
                    if(Pattern_Int.checkMissingLeaf(pattern)) {
                        //System.out.println("missing leaf "+ pattern);
                        chooseOutput(pattern,entry.getValue());
                    }else{
                        //constraint on obligatory children
                        if(checkObligatoryChild(pattern,entry.getKey(),grammarInt,blackLabelsInt)){
                            //System.out.println("missing obligatory child "+pattern);
                            chooseOutput(pattern,entry.getValue());
                        }else{
                            project(pattern, entry.getValue());
                        }
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
        //pattern = new Vector<>();
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
    private Map<ArrayList<Integer>, Projected> buildFP1(Vector < Vector<NodeFreqT> > trans) {
        Map<ArrayList<Integer>, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.elementAt(i).size(); ++j) {
                //String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();

                String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                int node_label_id = trans.elementAt(i).elementAt(j).getNode_label_int();
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
                        if (freq1.containsKey(temp)) {
                            freq1.get(temp).setProjectLocation(i, j);
                            //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            freq1.get(temp).setProjectRootLocation(i, j);
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

//            for(Map.Entry<Integer,Vector<String>> entry:grammarInt.entrySet()){
//                System.out.println(entry.getValue());
//            }

            Initial_Int.readWhiteLabel(config.getWhiteLabelFile(), grammarInt, whiteLabelsInt, blackLabelsInt, labelIndex); //read white labels and create black labels
            Initial_Int.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial_Int.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters

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
            /////////

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
                //filter root occurrences
                filterRootIDs(rootIDs);
                //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
                //phase 2: find maximal patterns from rootIDs
                log(report,"- Step 2: Mining maximal patterns WITHOUT max size constraint:");

                FreqT_Int_ext_serial freqT_ext = new FreqT_Int_ext_serial(config, this.grammar, this.grammarInt, this.blackLabelsInt, this.whiteLabelsInt,this.xmlCharacters,this.labelIndex,this.transaction);
                //parallel running
                //FreqT_Int_ext_multi freqT_ext = new FreqT_Int_ext_multi(config, this.grammar, this.grammarInt, this.blackLabelsInt, this.whiteLabelsInt,this.xmlCharacters,this.labelIndex,this.transaction);
                freqT_ext.run(rootIDs,start,report);

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
                Vector<String> pat = Pattern_Int.getPatternStr(entry.getKey(),labelIndex);
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
                                         Vector <Vector<NodeFreqT> >  _transaction){
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
                List<Integer> pos = Location.getLocationList(projected.getProjectLocation(i));
                //System.out.println(pos);

                int firstPos=0;
                for(int j=pos.size()-2; j>0; --j){
                    if(_transaction.elementAt(id).elementAt(pos.get(j)).getNode_label_int() == pat.get(childrenPos.get(childrenPos.size()-2))) {
                        firstPos = pos.get(j);
                        break;
                    }
                }
                int lastPos = pos.get(pos.size()-1);
                //System.out.println(firstPos+" "+lastPost);
                if (_transaction.elementAt(id).elementAt(firstPos).getNodeSibling() != lastPos){
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
    public void checkBlackSection(Map.Entry<ArrayList<Integer>, Projected> entry, Vector <Vector<NodeFreqT> >  _transaction){
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
                    if (blackSectionList.contains(_transaction.elementAt(id).elementAt(currentPos).getNodeLabel())) {
                        //System.out.println("found " + id + " " + searchPos);
                        projected.deleteProjectLocation(i);
                        i--;
                        break;
                    } else {
                        currentPos = _transaction.elementAt(id).elementAt(currentPos).getNodeChild();
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