package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Initial_Int;
import be.intimals.freqt.util.Util;

import java.io.FileWriter;
import java.util.*;

import static be.intimals.freqt.util.Util.log;

public abstract class FreqT {

    protected Config config;

    protected ArrayList <ArrayList<NodeFreqT>> transaction = new ArrayList <>();
    protected Map<String, ArrayList <String> > grammar    = new LinkedHashMap<>();
    protected Map<String, String> xmlCharacters  = new LinkedHashMap<>();

    //new variables for Integer
    protected Map<Integer, String> labelIndex = new HashMap<>();
    protected Map<Integer, ArrayList<String> > grammarInt    = new LinkedHashMap<>();
    protected Map<Integer, ArrayList<Integer> > blackLabelsInt = new LinkedHashMap<>();
    protected Map<Integer, ArrayList<Integer> > whiteLabelsInt = new LinkedHashMap<>();

    //store root labels
    public Set<String> rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    public Map<String, FTArray>  rootIDs = new HashMap<>();
    //store file ids of patterns
    public Map<String, String>  fileIDs = new HashMap<>();
    //int nbInputFiles;
    ArrayList <Integer> lineNrs = new ArrayList <>();

    public long timeStart;
    public long timeout;
    public boolean finished;

    //store only pattern, don't store support, rootSupport and size
    public Map<FTArray, String> MFP = new HashMap<>();

    public ArrayList<Integer> transactionClassID = new ArrayList<>();
    protected int sizeClass1;
    protected int sizeClass2;

    ////////////////////////////////////////////////////////////

    public FreqT(Config _config){
        config = _config;
    }

    /**
     * run Freqt with file config.properties on 2-class dataset
     */
    public void run() {
        try{
            //read input ASTs
            readASTData();
            //additional datasets
            initData();
            //set starting time
            setStartingTime();
            //init report file
            FileWriter report = Util.initReport(config, transaction.size());

            System.out.println("Mining frequent subtrees ...");
            //build FP1: all labels are frequent
            Map <FTArray, Projected> FP1 = buildFP1(transaction, rootLabels, transactionClassID);
            //prune FP1 on minimum support
            Constraint.prune(FP1, config.getMinSupport() );
            //expand FP1 to find maximal patterns
            expandFP1(FP1);
            //write patterns to file
            if(config.getTwoStep())
                expandPatternFromRootIDs(rootIDs, report);
            else
                Util.printPatternInTheFirstStep(MFP, config, grammar, labelIndex, xmlCharacters,
                        report, timeStart, finished);
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt_Int");
            e.printStackTrace();
        }
    }

    //read requirement data
    private void initData(){
        try{
            //create grammar (labels are strings) which is used to print patterns
            Initial_Int.initGrammar(config.getInputFiles(),grammar, config.buildGrammar());
            //create grammar (labels are integers) which is used in the mining process
            Initial_Int.initGrammar_Int(config.getInputFiles(), grammarInt, labelIndex);
            //read white labels and create black labels
            Initial_Int.readWhiteLabel(config.getWhiteLabelFile(), grammarInt, whiteLabelsInt, blackLabelsInt, labelIndex);
            //read root labels (AST Nodes)
            Initial_Int.readRootLabel(config.getRootLabelFile(), rootLabels);
            //read list of special XML characters
            Initial_Int.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters);

        }catch (Exception e){
            System.out.println("init 2-class dataset error "+e);
        }
    }


    //
    private void expandPatternFromRootIDs(Map<String, FTArray>  _rootIDs, FileWriter report){
        try{
            System.out.println("Mining maximal frequent subtrees ...");
            log(report,"");
            log(report,"OUTPUT");
            log(report,"===================");

            long diff1 = System.currentTimeMillis( ) - timeStart;
            //report the phase 1
            log(report,"- Step 1: Mining frequent patterns with max size constraints");
            //log(report,"\t + Frequent patterns = "+ nbFP);
            log(report, "\t + running time = "+ diff1/1000 +"s");
            log(report,"\t + root occurrences groups = "+ _rootIDs.size());
            //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
            //phase 2: find maximal patterns from rootIDs
            log(report,"- Step 2: Mining maximal patterns WITHOUT max size constraint:");
            //run the second step
            runSecondStep(_rootIDs, report);

            //for FastTreeCheck
            //System.out.println("fastCheckSubTree Hits: "+ freqT_ext.hitCount+" Misses: "+ freqT_ext.missCount);

        }catch (Exception e){
            System.out.println("expand pattern from root IDs error "+e);
        }

    }


    //build subtrees of size 1 based on root labels
    private Map<FTArray, Projected> buildFP1(ArrayList  < ArrayList <NodeFreqT> > trans, Set<String> _rootLabels,
                                             ArrayList<Integer> _transactionClassID) {
        Map<FTArray, Projected> freq1 = new LinkedHashMap<>();
        try {
            for (int i = 0; i < trans.size(); ++i) {
                //get transaction label
                int classID = _transactionClassID.get(i);

                for (int j = 0; j < trans.get(i).size(); ++j) {
                    //String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                    String node_label = trans.get(i).get(j).getNodeLabel();
                    int node_label_id = trans.get(i).get(j).getNode_label_int();
                    //String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                    //if node_label in rootLabels and lineNr > lineNr threshold
                    if (_rootLabels.contains(node_label) || _rootLabels.isEmpty()) {
                        //System.out.println(lineNr+"  "+lineNrs.elementAt(i));
                        //if(Integer.valueOf(lineNr) > lineNrs.elementAt(i)){ //using only for Cobol data
                        if (node_label != null) {
                            //update the locations
                            FTArray prefix = new FTArray();
                            Location initLocation = new Location();
                            updateCandidates(freq1, node_label_id, classID, i, j, 0, prefix, initLocation);
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("build FP1 error "+e);
        }
        return freq1;
    }


    // expand FP1 to find frequent subtrees based on input constraints
    private void expandFP1(Map <FTArray, Projected > freq1){
        //init a pattern
        FTArray pattern = new FTArray();
        //for each label found in FP1, expand it to find maximal patterns
        for(Map.Entry<FTArray, Projected> entry : freq1.entrySet()){
            //entry.getValue().setProjectedDepth(0);
            pattern.addAll(entry.getKey());
            //recursively expand pattern
            expandPattern(pattern, entry.getValue());
            //reset pattern
            pattern = new FTArray();
        }
    }


    //expand pattern in 2-class data
    private void expandPattern(FTArray pattern, Projected projected) {
        try{
            //if it is timeout then stop expand the pattern;
            if (isTimeout()) return;
            //System.out.print("pattern: ");printFTArray(pattern);System.out.println("Candidates:");
            //find candidates of the current pattern
            Map<FTArray, Projected> candidates = generateCandidates(projected, transaction);
            //printCandidates(candidates, labelIndex);
            //prune the candidates on minimum support (total support of the pattern in two classes) and list of black labels
            Constraint.pruneSupportAndBlacklist(candidates, config.getMinSupport(), pattern, blackLabelsInt);
            //if there is no candidate then report the pattern and then stop
            if( candidates.isEmpty()){
                //addTree(pattern, projected);
                addTree(pattern, projected);
                return;
            }
            //expand each candidate to the current pattern
            for(Map.Entry<FTArray, Projected> entry : candidates.entrySet()){
                int oldSize = pattern.size();
                //get the current candidate
                FTArray key = entry.getKey() ;
                //add candidate into pattern
                pattern.addAll(key);
                //check section and paragraphs in COBOL
                //Constraint.checkCobolConstraints(pattern, entry, key, labelIndex, transaction);

                //check obligatory children constraint
                if(Constraint.checkLeftObligatoryChild(pattern, entry.getKey(), grammarInt, blackLabelsInt)){
                    //do nothing = don't store pattern
                }else{
                    //check constraints on maximal number of leafs and real leaf
                    if( Constraint.satisfyMaxLeaf(pattern, config.getMaxLeaf()) || Constraint.satisfyFullLeaf(pattern)){
                        //store the pattern
                        //addTree(pattern,entry.getValue());
                        addTree(pattern, entry.getValue());
                    }else{
                        //continue expanding pattern
                        expandPattern(pattern, entry.getValue());
                    }
                }
                //pattern = pattern.subList(0, oldSize);
                pattern.shrink(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: expandPattern " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }


    //generate candidates for a pattern
    public Map<FTArray, Projected> generateCandidates(Projected projected, ArrayList<ArrayList <NodeFreqT> > _transaction) {
        //use LinkedHashMap to keep the order of the candidates
        Map<FTArray, Projected> candidates = new LinkedHashMap<>();
        try{
            //find candidates for each location
            int depth = projected.getProjectedDepth();
            //System.out.println("depth " + depth);
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                //int id = Location.getLocationId(projected.getProjectLocation(i));
                //int pos = Location.getLocationPos(projected.getProjectLocation(i));
                int classID = projected.getProjectLocation(i).getClassID();
                int id = projected.getProjectLocation(i).getLocationId();
                int root = projected.getProjectLocation(i).getRoot();
                int pos = projected.getProjectLocation(i).getLocationPos();
                //store all locations of the labels in the pattern: this uses more memory but need for checking continuous paragraphs
                Location occurrences = projected.getProjectLocation(i);
                //store only id and root
                //Location occurrences = new Location(id,root);
                //keep lineNr to calculate distance of two nodes
                //List<Integer> lines = projected.getProjectLineNr(i);
                FTArray prefixInt = new FTArray();
                //find candidates from left to right
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.get(id).get(pos).getNodeChild() :  _transaction.get(id).get(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1; l = _transaction.get(id).get(l).getNodeSibling()) {
                        int node_label_int = _transaction.get(id).get(l).getNode_label_int();
                        updateCandidates(candidates, node_label_int, classID, id, l, newDepth, prefixInt, occurrences);
                    }
                    if (d != -1) {
                        pos = _transaction.get(id).get(pos).getNodeParent();
                    }
                    prefixInt.add(-1);
                }
            }
        }
        catch (Exception e){
            System.out.println("Error: generate candidates " + e);
            e.printStackTrace();
            System.exit(-1);
        }
        return candidates;
    }


    //update candidate locations for two-class data
    private void updateCandidates(Map<FTArray, Projected> freq1, int candidate, int classID, int id, int rightmostPos,
                                  int depth, FTArray prefixInt, Location initLocations) {
        try {
            FTArray newTree = new FTArray();
            newTree.addAll(prefixInt);
            newTree.add(candidate);
            Projected value = freq1.get(newTree);
            //if candidate existed in the freq1 then add its location to projected
            if (value != null) {
                value.addProjectLocation(classID, id, rightmostPos, initLocations);
                //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
            } else {
                //add new location
                Projected projected = new Projected();
                projected.setProjectedDepth(depth);
                projected.addProjectLocation(classID, id, rightmostPos, initLocations);
                //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                freq1.put(newTree, projected);
            }
        }catch (Exception e){
            System.out.println("update projected location error "+e);
        }
    }


    //add the tree to the root IDs or the MFP
    private void addTree(FTArray pat, Projected projected){
        //remove the right part of the pattern that misses leafs
        FTArray patTemp = Pattern_Int.removeMissingLeaf(pat);
        //check minsize constraints and right mandatory children before adding pattern
        if(Constraint.checkOutput(patTemp,config.getMinLeaf(),config.getMinNode())
                && ! Constraint.checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){
            if (config.getTwoStep()) {
                //store root occurrences for next step
                addRootIDs(patTemp, projected, rootIDs);
            } else{
                //check and store pattern to maximal pattern list
                addMFP(patTemp, projected, MFP, config);
            }
        }
    }

    //add root occurrences of pattern to rootIDs
    private void addRootIDs(FTArray pat, Projected projected, Map<String, FTArray> _rootIDs){
        try {
            //find root occurrences (id-pos) of pattern
            String rootOccurrences = getStringRootOccurrence(projected);

            //check the current root occurrences
            boolean isAdded = true;
            Collection<String> l1 = Arrays.asList(rootOccurrences.split(";"));

            Iterator<Map.Entry<String, FTArray>> iter = _rootIDs.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry<String, FTArray> entry = iter.next();
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
                FTArray rootLabel_int = pat.subList(0,1);
                _rootIDs.put(rootOccurrences, rootLabel_int);
            }
        }catch (Exception e){System.out.println("Error: adding rootIDs "+e);}
    }


    private String getStringRootOccurrence(Projected projected) {
        String rootOccurrences = "";
        for(int i = 0; i<projected.getProjectLocationSize(); ++i){
            rootOccurrences = rootOccurrences +
                    projected.getProjectLocation(i).getClassID() + ("-") +
                    projected.getProjectLocation(i).getLocationId() + ("-") +
                    projected.getProjectLocation(i).getRoot() + ";";
        }
        return rootOccurrences;
    }


    //set time to begin a run
    private void setStartingTime() {
        timeStart = System.currentTimeMillis();
        timeout = config.getTimeout()*(60*1000);
        finished = true;
    }


    //check running time of the algorithm
    private boolean isTimeout() {
        long currentTime = System.currentTimeMillis();
        if(!config.getTwoStep())
            if((currentTime-timeStart) > timeout) {
                finished = false;
                return true;
            }
        return false;
    }


    //return input xml characters
    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}


    //return input grammar
    public Map <String,ArrayList <String>> getGrammar(){return this.grammar;}


    //read input AST
    public abstract void readASTData();

    //run the second step to find maximal pattern
    public abstract void runSecondStep(Map<String, FTArray> _rootIDs, FileWriter report);

    //add a pattern to a list
    public abstract void addMFP(FTArray pattern, Projected projected, Map<FTArray,String> _MFP, Config config);

}
