package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.input.ReadXML_Int;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Initial_Int;
import be.intimals.freqt.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static be.intimals.freqt.util.Util.*;
import static java.lang.StrictMath.round;

public class FreqT {

    protected Config config;

    protected ArrayList <ArrayList<NodeFreqT>> transaction = new ArrayList <>();
    protected Map<String, ArrayList <String> > grammar    = new HashMap<>();
    protected Map<String, String> xmlCharacters  = new HashMap<>();

    //new variables for Integer
    protected Map<Integer, String> labelIndex = new HashMap<>();
    protected Map<Integer, ArrayList<String> > grammarInt    = new HashMap<>();
    protected Map<Integer, ArrayList<Integer> > blackLabelsInt = new HashMap<>();
    protected Map<Integer, ArrayList<Integer> > whiteLabelsInt = new HashMap<>();

    //store root labels
    public Set<String> rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    public Map<Projected, FTArray>  rootIDs = new HashMap<>();
    //store file ids of patterns
    public Map<String, String>  fileIDs = new HashMap<>();
    //int nbInputFiles;
    ArrayList <Integer> lineNrs = new ArrayList <>();

    public long timeStart;
    public long timeout;
    public boolean finished;

    //store maximal patterns
    public Map<FTArray, String> MFP = new HashMap<>();

    //store k-highest chi-square score patterns
    private Map<FTArray, Projected> HSP = new HashMap<>();

    //store transaction ids and their correspond class ids
    public ArrayList<Integer> transactionClassID = new ArrayList<>();
    protected int sizeClass1;
    protected int sizeClass2;


    FTArray leafPattern = new FTArray();
    Projected leafProjected = new Projected();

    ////////////////////////////////////////////////////////////

    public FreqT(Config _config){
        config = _config;
    }

    //run Freqt with file config.properties
    public void run() {
        try{
            //read input data
            initData();
            //set starting time
            setStartingTime();
            //init report file
            FileWriter report = initReport(config, transaction.size());
            System.out.println("Mining frequent subtrees ...");
            //build FP1: all labels are frequent
            Map <FTArray, Projected> FP1 = buildFP1(transaction, rootLabels, transactionClassID);
            //prune FP1 on minimum support
            Constraint.prune(FP1, config.getMinSupport() );
            //expand FP1 to find maximal patterns
            expandFP1(FP1);
            if(config.getTwoStep()) {
                if(config.get2Class()) {
                    //group root occurrences from 1000-highest chi-square score patterns
                    rootIDs = groupRootOcc(HSP);
                }
                //find pattern from root occurrences
                expandPatternFromRootIDs(rootIDs, report);
            }else {
                outputPatternInTheFirstStep(MFP, config, grammar, labelIndex, xmlCharacters, report);
            }
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt_Int");
            e.printStackTrace();
        }
    }

    //read input data
    private void initData(){
        try{
            ReadXML_Int readXML_int = new ReadXML_Int();
            //remove black labels when reading ASTs
            if(config.get2Class()){
                readXML_int.readDatabase(transaction,1,
                        new File(config.getInputFiles1()), labelIndex, transactionClassID, config.getWhiteLabelFile());
                readXML_int.readDatabase(transaction,0,
                        new File(config.getInputFiles2()), labelIndex, transactionClassID, config.getWhiteLabelFile());
                sizeClass1 = transactionClassID.stream().mapToInt(Integer::intValue).sum();
                sizeClass2 = transactionClassID.size() - sizeClass1;

                Initial_Int.initGrammar(config.getInputFiles1(),grammar, config.buildGrammar());
                Initial_Int.initGrammar(config.getInputFiles2(),grammar, config.buildGrammar());

                Initial_Int.initGrammar_Int(config.getInputFiles1(), grammarInt, labelIndex);
                Initial_Int.initGrammar_Int(config.getInputFiles2(), grammarInt, labelIndex);
            }else{
                readXML_int.readDatabase(transaction,1,
                        new File(config.getInputFiles()), labelIndex, transactionClassID, config.getWhiteLabelFile());

                //create grammar (labels are strings) which is used to print patterns
                Initial_Int.initGrammar(config.getInputFiles(),grammar, config.buildGrammar());

                //create grammar (labels are integers) which is used in the mining process
                Initial_Int.initGrammar_Int(config.getInputFiles(), grammarInt, labelIndex);
            }

            //read white labels and create black labels
            Initial_Int.readWhiteLabel(config.getWhiteLabelFile(), grammarInt, whiteLabelsInt, blackLabelsInt, labelIndex);
            //read root labels (AST Nodes)
            Initial_Int.readRootLabel(config.getRootLabelFile(), rootLabels);
            //read list of special XML characters
            Initial_Int.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters);

        }catch (Exception e){
            System.out.println("read data set error "+e);
        }
    }

    //run the 2nd step to find maximal patterns from groups of root occurrences
    private void expandPatternFromRootIDs(Map<Projected, FTArray>  _rootIDs, FileWriter report){
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
            FreqT_ext freqT_ext = new FreqT_ext(config, this.grammar, this.grammarInt,
                                                        this.blackLabelsInt, this.whiteLabelsInt,
                                                        this.xmlCharacters,this.labelIndex,this.transaction,
                                                        this.sizeClass1, this.sizeClass2);
            freqT_ext.run(_rootIDs, report);

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

    //expand pattern
    private void expandPattern(FTArray pattern, Projected projected) {
        try {
            //if it is timeout then stop expand the pattern;
            if (isTimeout()) return;
            //System.out.print("pattern: ");printFTArray(pattern);System.out.println("Candidates:");
            //find candidates of the current pattern
            Map<FTArray, Projected> candidates = generateCandidates(projected, transaction);
            //printCandidates(candidates, labelIndex);
            //prune the candidates on minimum support (total support of the pattern in two classes) and list of black labels
            //Constraint.pruneSupportAndBlacklist(candidates, config.getMinSupport(), pattern, blackLabelsInt);
            Constraint.prune(candidates, config.getMinSupport());
            //if there is no candidate then report the pattern and then stop
            if( candidates.isEmpty()){
                if(leafPattern.size() > 0)
//                    //addTree(pattern, projected);
//                    System.out.println("output at empty");
//                    Util.printFTArray(leafPattern, labelIndex);
//                    System.out.println(getSupportString(leafPattern, leafProjected));
//                    Util.printFTArray(pattern, labelIndex);
//                    System.out.println(getSupportString(pattern, projected));

                    addTree(leafPattern, leafProjected);
                return;
            }
            //expand each candidate to the current pattern
            for(Map.Entry<FTArray, Projected> entry : candidates.entrySet()){
                int oldSize = pattern.size();

                //get the current candidate
                FTArray key = entry.getKey() ;
                //add candidate into pattern
                pattern.addAll(key);

                //if the right most node of the pattern is a leaf then keep track this pattern
                if(pattern.getLast() < -1 )
                    keepLeafPattern(pattern, entry.getValue());

                FTArray oldLeafPattern = leafPattern;
                Projected oldLeafProjected = leafProjected;

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
                        if(leafPattern.size() > 0)
                            //add leafPattern to MFP
                            addTree(leafPattern, leafProjected);

                    }else{
                        //continue expanding pattern
                        expandPattern(pattern, entry.getValue());
                    }
                }
                pattern = pattern.subList(0, oldSize);
                keepLeafPattern(oldLeafPattern, oldLeafProjected);
                //pattern.shrink(oldSize);
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
                //Location occurrences = new Location(classID,id,root);
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

    //keep track the pattern which has the right-most node is a leaf
    public void keepLeafPattern(FTArray pat, Projected projected){
        leafPattern = new FTArray(pat);
        leafProjected = projected;
    }

    //add the tree to the root IDs or the MFP
    private void addTree(FTArray pat, Projected projected){
        //check minsize constraints and right mandatory children
        if(Constraint.checkOutput(pat, config.getMinLeaf(), config.getMinNode())
                && ! Constraint.checkRightObligatoryChild(pat, grammarInt, blackLabelsInt)){
            if(config.get2Class()){
                //check chi-square score
                if(Constraint.satisfyChiSquare(projected, sizeClass1, sizeClass2, config.getDSScore())){
                    if(config.getTwoStep()){
                        //add pattern to the list of 1000-highest chi-square score patterns
                        addHighScorePattern(pat, projected, HSP);
                    }else{
                        //add pattern to maximal pattern list
                        addMaximalPattern(pat, projected, MFP);
                    }
                }
            }else{
                if(config.getTwoStep()){
                    //add root occurrences of the current pattern to rootIDs
                    addRootIDs(pat, projected, rootIDs);
                }else{
                    //add pattern to maximal pattern list
                    addMaximalPattern(pat, projected, MFP);
                }
            }
        }
    }

    //add root occurrences of pattern to rootIDs
    private void addRootIDs(FTArray pat, Projected projected, Map<Projected, FTArray> _rootIDs){
        try {
            //find root occurrences of current pattern
            String rootOccurrences = getStringRootOccurrence(projected);

            //check the current root occurrences existing in the rootID or not
            boolean isAdded = true;
            Collection<String> l1 = Arrays.asList(rootOccurrences.split(";"));

            Iterator<Map.Entry<Projected, FTArray>> iter = _rootIDs.entrySet().iterator();
            while (iter.hasNext()){
                Map.Entry<Projected, FTArray> entry = iter.next();
                String rootOccurrence1 = getStringRootOccurrence(entry.getKey());
                Collection<String> l2 = Arrays.asList(rootOccurrence1.split(";"));
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
                //store root occurrences and root label
                FTArray rootLabel_int = pat.subList(0,1);
                _rootIDs.put(projected, rootLabel_int);
            }
        }catch (Exception e){System.out.println("Error: adding rootIDs "+e);}
    }

    //add maximal patterns
    public void addMaximalPattern(FTArray pat, Projected projected, Map<FTArray, String> _MFP) {
        if(! _MFP.isEmpty()){
            if(_MFP.containsKey(pat)){
                return;
            }
            //check maximal pattern
            Iterator< Map.Entry<FTArray,String> > p = _MFP.entrySet().iterator();
            while(p.hasNext()){
                Map.Entry<FTArray, String> entry = p.next();
                switch (CheckSubtree.checkSubTree(pat, entry.getKey(), config)){
                    case 1: //pat is a subtree of entry.getKey
                        return;
                    case 2: //entry.getKey is a subtree of pat
                        p.remove();
                        //keep the old patternSupport
                        break;
                }
            }
            //add new maximal pattern to the list
            String patternSupport = getSupportString(pat, projected);
            _MFP.put(pat, patternSupport);
        }else{
            //add new maximal pattern to the list
            String patternSupport = getSupportString(pat, projected);
            _MFP.put(pat, patternSupport);
        }
    }

    //get a string of support, score, size for a pattern
    public String getSupportString(FTArray pat, Projected projected){
        String result;
        if(config.get2Class()){
            double score = Constraint.chiSquare(projected, sizeClass1, sizeClass2);
            int[] ac = Constraint.get2ClassSupport(projected);
            String support = String.valueOf(ac[0]) +"-"+String.valueOf(ac[1]);
            int size = Pattern_Int.countNode(pat);
            result = support + "," + score + "," + String.valueOf(size);
        }else{
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport();
            int size = Pattern_Int.countNode(pat);
            result = String.valueOf(support) + "," + String.valueOf(wsupport) + "," + String.valueOf(size);
        }
        return result;
    }

    //group root occurrences from 1000 patterns in HSP
    private Map<Projected, FTArray> groupRootOcc(Map<FTArray, Projected> _HSP){
        Map<Projected, FTArray> _rootIDs = new HashMap<>();

        for(Map.Entry<FTArray, Projected> entry : _HSP.entrySet() ){
            addRootIDs(entry.getKey(), entry.getValue(), _rootIDs);
        }

        return _rootIDs;
    }

    //add pattern to the list of 1000-highest chi-square score patterns
    private void addHighScorePattern(FTArray pat, Projected projected, Map<FTArray,Projected> _HSP){
        double score = Constraint.chiSquare(projected, sizeClass1, sizeClass2);
        if(_HSP.size() >= config.getNumPatterns()) {
            double minScore = getMinScore(_HSP);
            if (score > minScore) {
                //get pattern which has minScore
                FTArray minPattern = getMinScorePattern(_HSP);
                //remove minScore pattern
                _HSP.remove(minPattern);
                //add new pattern
                _HSP.put(pat, projected);
            }
        }else{
            //add new pattern
            _HSP.put(pat, projected);
        }
    }

    //get minimum score of pattern in HSP
    private double getMinScore(Map<FTArray,Projected> _HSP){
        double score = 1000.0;
        for(Map.Entry<FTArray, Projected> entry : _HSP.entrySet()){
            double scoreTmp = Constraint.chiSquare(entry.getValue(), sizeClass1, sizeClass2);
            if(score > scoreTmp)
                score = scoreTmp;
        }
        return score;
    }

    //get a pattern which has minimum score in the list of patterns
    private FTArray getMinScorePattern(Map<FTArray,Projected> _HSP){
        double score = 1000.0;
        FTArray minScorerPattern = new FTArray();
        for(Map.Entry<FTArray, Projected> entry : _HSP.entrySet()){
            double scoreTmp = Constraint.chiSquare(entry.getValue(), sizeClass1, sizeClass2);
            if(score > scoreTmp) {
                score = scoreTmp;
                minScorerPattern = new FTArray(entry.getKey());
            }
        }
        return minScorerPattern;
    }

    //print patterns found in the first step
    private void outputPatternInTheFirstStep(Map<FTArray, String> MFP,
                                                  Config config,
                                                  Map<String, ArrayList<String>> grammar,
                                                  Map<Integer, String> labelIndex,
                                                  Map<String, String> xmlCharacters,
                                                  FileWriter report) throws IOException {
        log(report,"OUTPUT");
        log(report,"===================");
        if(finished)
            log(report,"finished search");
        else
            log(report,"timeout");

        //filter maximality ?
        //Map<FTArray, String> filterMFP = filterFP(MFP, config);

        //print pattern to xml file
        outputPatterns(MFP, config, grammar, labelIndex, xmlCharacters);

        long end1 = System.currentTimeMillis( );
        long diff1 = end1 - timeStart;
        log(report,"+ Maximal patterns = "+ MFP.size());
        log(report,"+ Running times = "+ diff1/1000 +" s");
        report.close();
    }

    //print maximal patterns to XML file
    public void outputPatterns(Map<FTArray, String> MFP,
                                       Config config, Map<String, ArrayList <String> > grammar,
                                       Map<Integer, String> labelIndex,
                                       Map<String, String> xmlCharacters){
        try{
            String outFile = config.getOutputFile();
            //create output file to store patterns for mining common patterns
            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");

            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile, config, grammar, xmlCharacters);
            Iterator< Map.Entry<FTArray,String> > iter1 = MFP.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<FTArray,String> entry = iter1.next();
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

    //create a report
    private FileWriter initReport(Config _config, int dataSize) throws IOException {
        String reportFile = _config.getOutputFile().replaceAll("\"","") +"_report.txt";
        FileWriter report = new FileWriter(reportFile);
        log(report,"INPUT");
        log(report,"===================");
        log(report,"- data sources : " + _config.getInputFiles());
        log(report,"- input files : " +  dataSize);
        log(report,"- minSupport : " + _config.getMinSupport());
        report.flush();
        return report;
    }

    //return input xml characters
    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}

    //return input grammar
    public Map <String,ArrayList <String>> getGrammar(){return this.grammar;}

    //write a string to report
    public void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

}
