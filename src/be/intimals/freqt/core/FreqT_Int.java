package be.intimals.freqt.core;

import be.intimals.freqt.FTArray;
import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
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
    protected Config config;
    protected ArrayList <ArrayList <NodeFreqT> > transaction = new ArrayList <>();
    protected Map<String, ArrayList <String> > grammar    = new LinkedHashMap<>();
    protected Map<String, String> xmlCharacters  = new LinkedHashMap<>();

    //new variables for Integer
    protected Map<Integer, String> labelIndex = new HashMap<>();
    protected Map<Integer, ArrayList<String> > grammarInt    = new LinkedHashMap<>();
    protected Map<Integer, ArrayList<Integer> > blackLabelsInt = new LinkedHashMap<>();
    protected Map<Integer, ArrayList<Integer> > whiteLabelsInt = new LinkedHashMap<>();

    //store only pattern, don't store support, rootSupport and size
    private ArrayList<FTArray> newMFP = new ArrayList<FTArray>();

    private Map<FTArray, String> MFP = new HashMap<>();
    //store root labels
    private Set<String>    rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    private Map<String, FTArray>  rootIDs = new HashMap<>();
    //store file ids of patterns
    private Map<String, String>  fileIDs = new HashMap<>();
    //int nbInputFiles;

    ArrayList <Integer> lineNrs = new ArrayList <>();

    private long timeStart;
    private long timeout;
    private boolean finished;

    public int hitCount, missCount;

    ////////////////////////////////////////////////////////////////////////////////
    public FreqT_Int(Config config) {
        this.config = config;
    }
    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}
    public Map <String,ArrayList <String>> getGrammar(){return this.grammar;}
    public int getMFPSize(){return  MFP.size();}

    /**
     * run Freqt with file config.properties
     */
    public void run() {
        try{
            //read input ASTs and additional information
            initData();
            //set starting time
            setStartingTime();
            //init report file
            FileWriter report = initReport();
            System.out.println("Mining frequent subtrees ...");
            //build FP1: all labels are frequent
            Map <FTArray, Projected > FP1 = buildFP1(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune FP1 on minimum support
            Constraint.prune(FP1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + FP1.keySet());
            //expand FP1 to find maximal patterns
            expandFP1(FP1);
            //run the second step if getTwoStep = true
            if(config.getTwoStep()){
                expandPatternFromRootIDs(report);
            }else{//output maximal patterns in the first step
                printPatternInTheFirstStep(report, timeStart);
            }
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt_Int");
            e.printStackTrace();
        }
    }

    //read database and additional information
    private void initData() throws Exception {
        transaction = new ReadXML_Int().readDatabase(config.getAbstractLeafs(), new File(config.getInputFiles()), labelIndex);
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
    }

    private FileWriter initReport() throws IOException {
        String reportFile = config.getOutputFile().replaceAll("\"","") +"-report.txt";
        FileWriter report = new FileWriter(reportFile);
        log(report,"INPUT");
        log(report,"===================");
        log(report,"- data sources : " + config.getInputFiles());
        log(report,"- input files : " +  transaction.size());
        log(report,"- minSupport : " + config.getMinSupport());
        report.flush();
        return report;
    }

    //group of procedures to expand pattern
    /**
     * Return all frequent subtrees of size 1
     * @param trans
     * @return
     */
    private Map<FTArray, Projected> buildFP1(ArrayList  < ArrayList <NodeFreqT> > trans) {
        Map<FTArray, Projected> freq1 = new LinkedHashMap<>();
        try {
            for (int i = 0; i < trans.size(); ++i) {
                for (int j = 0; j < trans.get(i).size(); ++j) {
                    //String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                    String node_label = trans.get(i).get(j).getNodeLabel();
                    int node_label_id = trans.get(i).get(j).getNode_label_int();
                    //String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                    //if node_label in rootLabels and lineNr > lineNr threshold
                    if (rootLabels.contains(node_label) || rootLabels.isEmpty()) {
                        //System.out.println(lineNr+"  "+lineNrs.elementAt(i));
                        //if(Integer.valueOf(lineNr) > lineNrs.elementAt(i)){ //using only for Cobol data
                        if (node_label != null) {
                            //update the locations
                            FTArray prefix = new FTArray();
                            Location initLocation = new Location();
                            updateCandidates(freq1, node_label_id, i, j, 0, prefix, initLocation);
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("build FP1 error "+e);
        }
        return freq1;
    }

    /**
     * expand FP1 to find frequent subtrees based on input constraints
     * @param freq1
     */
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

    /**
     * expand the pattern to to find maximal pattern
     * @param pattern
     * @param projected
     */
    private void expandPattern(FTArray pattern, Projected projected) {
        try{
            //if it is timeout then stop expand the pattern;
            if (isTimeout()) return;

            //System.out.print("pattern: ");printFTArray(pattern);System.out.println("Candidates:");

            //find candidates of the current pattern
            Map<FTArray, Projected> candidates = generateCandidates(projected, transaction);

            //printCandidates(candidates);

            //prune the candidates on minimum support and list of black labels
            Constraint.pruneSupportAndBlacklist(candidates, config.getMinSupport(), pattern, blackLabelsInt);

            //if there is no candidate then report the pattern and then stop
            if( candidates.isEmpty()){
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
                        addTree(pattern,entry.getValue());
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
            System.exit(-1);
        }
    }

    /**
     * generate candidates for the pattern by using rightmost extension
     * @param projected
     * @param _transaction
     * @return
     */
    public Map<FTArray, Projected> generateCandidates(Projected projected, ArrayList  <ArrayList <NodeFreqT> >  _transaction) {
        //use LinkedHashMap to keep the order of the candidates
        Map<FTArray, Projected> candidates = new LinkedHashMap<>();
        try{
            //find candidates for each location
            int depth = projected.getProjectedDepth();
            //System.out.println("depth " + depth);
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                //int id = Location.getLocationId(projected.getProjectLocation(i));
                //int pos = Location.getLocationPos(projected.getProjectLocation(i));
                int id = projected.getProjectLocation(i).getLocationId();
                int root = projected.getProjectLocation(i).getRoot();
                int pos = projected.getProjectLocation(i).getLocationPos();
                //store all locations of the labels in the pattern: this uses more memory but need for checking continuous paragraphs
                //Location occurrences = projected.getProjectLocation(i);
                //store only id and root
                Location occurrences = new Location(id,root);

                //keep lineNr to calculate distance of two nodes
                //List<Integer> lines = projected.getProjectLineNr(i);
                FTArray prefixInt = new FTArray();
                //find candidates from left to right
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.get(id).get(pos).getNodeChild() :  _transaction.get(id).get(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1; l = _transaction.get(id).get(l).getNodeSibling()) {
                        int node_label_int = _transaction.get(id).get(l).getNode_label_int();
                        updateCandidates(candidates, node_label_int, id, l, newDepth, prefixInt, occurrences);
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


    /**
     * update locations of the candidate in the list
     * @param freq1
     * @param candidate
     * @param id
     * @param rightmostPos
     */
    private void updateCandidates(Map<FTArray, Projected> freq1, int candidate, int id, int rightmostPos,
                                         int depth, FTArray prefixInt, Location initLocations) {
        try {
            FTArray newTree = new FTArray();
            newTree.addAll(prefixInt);
            newTree.add(candidate);
            Projected value = freq1.get(newTree);
            //if candidate existed in the freq1 then add its location to projected
            if (value != null) {
                value.addProjectLocation(id, rightmostPos, initLocations);
                //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
            } else {
                //add new location
                Projected projected = new Projected();
                projected.setProjectedDepth(depth);
                projected.addProjectLocation(id, rightmostPos, initLocations);
                //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                freq1.put(newTree, projected);
            }
        }catch (Exception e){
            System.out.println("update projected location error "+e);
        }
    }

    /**
     * discover maximal patterns from RootIDs
     * @param report
     * @throws IOException
     */
    private void expandPatternFromRootIDs(FileWriter report) throws IOException {
        System.out.println("Mining maximal frequent subtrees ...");
        log(report,"");
        log(report,"OUTPUT");
        log(report,"===================");

        long diff1 = System.currentTimeMillis( ) - timeStart;
        //report the phase 1
        log(report,"- Step 1: Mining frequent patterns with max size constraints");
        //log(report,"\t + Frequent patterns = "+ nbFP);
        log(report, "\t + running time = "+ diff1/1000 +"s");
        log(report,"\t + root occurrences groups = "+ rootIDs.size());
        //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
        //phase 2: find maximal patterns from rootIDs
        log(report,"- Step 2: Mining maximal patterns WITHOUT max size constraint:");

        FreqT_Int_ext_serial freqT_ext = new FreqT_Int_ext_serial(config, this.grammar, this.grammarInt,
                                                                   this.blackLabelsInt, this.whiteLabelsInt,
                                                                   this.xmlCharacters,this.labelIndex,this.transaction);
        freqT_ext.run(rootIDs, report);
        //for FastTreeCheck
        System.out.println("fastCheckSubTree Hits: "+ freqT_ext.hitCount+" Misses: "+ freqT_ext.missCount);
    }

    /**
     * print maximal pattern found in the first step
     * @param report
     * @param start
     * @throws IOException
     */
    private void printPatternInTheFirstStep(FileWriter report, long start) throws IOException {
        log(report,"OUTPUT");
        log(report,"===================");
        if(finished)
            log(report,"finished search");
        else
            log(report,"timeout");

        String outFile = config.getOutputFile();
        int nbMFP;
        //if filtering maximal pattern in the mining process then print patterns
        if(config.getFilter()) {
            nbMFP = MFP.size();
            outputPatterns(MFP, outFile);
            //nbMFP = newMFP.size();
            //printPatterns(newMFP, outFile);
        }else {//if don't filter maximal pattern in the mining process then filter them and print
            System.out.println("number FP: " + MFP.size());
            long startFilter = System.currentTimeMillis();
            Map<FTArray,String> mfpTemp = filterFP(MFP);
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

    //group of procedures to check maximal subtree
    /*
    //return true if either labels of pat1 contain labels of pat2 or labels of pat2 contain labels of pat1
    private boolean checkSubsetLabel(FTArray pat1, FTArray pat2){

        if( Pattern_Int.countNode(pat1) >= Pattern_Int.countNode(pat2) ){
            return  pat1.containsAll(pat2);
        }
        else {
            return pat2.containsAll(pat1);
        }
    }*/

    /**
     * check if pat1 is a subtree of pat2 ?
     * return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
     * @param pat1
     * @param pat2
     * @return
     */
    private int checkSubTree(FTArray pat1, FTArray pat2) {

        int fastResult = fastCheckSubTree(pat1,pat2);
        if (fastResult != -1) return fastResult;

        //check subset of labels before check maximality
 //       if (checkSubsetLabel(pat1, pat2)) {
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
 /*       }else {
    //        if (fastResult != 0)
    //            fastCheckSubTree(pat1,pat2);  //for debugging: put a breakpoint here
            return 0;
        }*/
    }

    // 0 = no subtree
    // 1 = pat1 is a subtree of pat2
    // 2 = pat2 is a subtree of pat1
    private int fastCheckSubTree(FTArray pat1, FTArray pat2){
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

    protected boolean hasSubtree(FTArray big, FTArray small){
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
    private boolean treeIncludes(FTArray big, FTArray small){
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
    private int skipOver(FTArray tree, int offset){
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

    //group of procedures to check output patterns
    /**
     * store root occurrences of pattern for the second step
     * @param pat
     * @param projected
     */
    private void addRootIDs(FTArray pat, Projected projected){
        try {
            //find root occurrences (id-pos) of pattern
            String rootOccurrences = "";
            for(int i = 0; i<projected.getProjectLocationSize(); ++i){
                rootOccurrences = rootOccurrences +
                        //Location.getLocationId(projected.getProjectLocation(i)) + ("-") +
                        //Location.getRoot(projected.getProjectLocation(i)) + ";";
                        projected.getProjectLocation(i).getLocationId() + ("-") +
                        projected.getProjectLocation(i).getRoot() + ";";
            }

            //check the current root occurrences
            boolean isAdded = true;
            Collection<String> l1 = Arrays.asList(rootOccurrences.split(";"));

            Iterator<Map.Entry<String, FTArray>> iter = rootIDs.entrySet().iterator();
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
                rootIDs.put(rootOccurrences, rootLabel_int);
            }
        }catch (Exception e){System.out.println("Error: adding rootIDs "+e);}
    }

    /**
     * check and add maximal pattern to MFP
     * if pat is a subtree of another pattern in MFP then ignore this pattern
     * else if pat is a super-set of another pattern in the MFP then replace this pattern by pat
     *      else add pat to MFP
     * @param pat
     * @param projected
     * @param _MFP
     */
    public void addMFP(FTArray pat, Projected projected, Map<FTArray,String> _MFP){
        //if pat is already existed in the MFP then return
        if(_MFP.containsKey(pat)) return;
        //compare the input pattern to every pattern in _MFP
        Iterator < Map.Entry<FTArray,String> > p = _MFP.entrySet().iterator();
        while(p.hasNext()){ // && !found){
            Map.Entry<FTArray, String> entry = p.next();
            switch (checkSubTree(pat,entry.getKey())){
                case 1:
                    //found = true; //pat is a subtree of entry.getKey
                    //break;
                    return;
                case 2:
                    p.remove(); //entry.getKey is a subtree of pat
                    break;
            }
        }
        //store other information of the pattern
        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern_Int.countNode(pat);
        String patternSupport = String.valueOf(support) + "," + String.valueOf(wsupport) + "," + String.valueOf(size);
        //add new pattern to the list
        _MFP.put(pat, patternSupport);

    }


    /**
     * add frequent pattern to FP
     * @param pat
     * @param projected
     * @param _FP
     */
    public void addFP(FTArray pat, Projected projected, Map<FTArray,String> _FP){

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern_Int.countNode(pat);

        String patternSupport =
                        String.valueOf(support) + "," +
                        String.valueOf(wsupport) + "," +
                        String.valueOf(size);
        _FP.put(pat, patternSupport);
    }

    /**
     * check output constraints: minLeaf and minNode
     * @param pat
     * @return
     */
    public boolean checkOutput(FTArray pat){
        if(  Constraint.satisfyMinLeaf(pat, config.getMinLeaf()) && Constraint.satisfyMinNode(pat, config.getMinNode()) )
            return true;
        else
            return false;
    }
    /**
     * Add the tree to the root IDs or the MFP
     * @param pat
     * @param projected
     */
    private void addTree(FTArray pat, Projected projected){
        //remove the right part of the pattern that misses leafs
        FTArray patTemp = Pattern_Int.getPatternString1(pat);
        //check minsize constraints and right mandatory children before adding pattern
        if(checkOutput(patTemp) && ! Constraint.checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){
            if (config.getTwoStep()) { //store root occurrences for next step
                addRootIDs(patTemp, projected);
            } else{ //check and store pattern to maximal pattern list
                if(config.getFilter())
                    addMFP(patTemp, projected, MFP);
                    //addMFPTest(patTemp, newMFP);
                else
                    addFP(patTemp, projected, MFP);
            }
        }
    }

    //group of procedures to set running time and print patterns
    private void setStartingTime() {
        timeStart = System.currentTimeMillis();
        timeout = config.getTimeout()*(60*1000);
        finished = true;
    }

    /**
     * check running time of the algorithm
     * return true if the running time is larger than timeout
     * @return
     */
    private boolean isTimeout() {
        long currentTime = System.currentTimeMillis();
        if(!config.getTwoStep())
            if((currentTime-timeStart) > timeout) {
                finished = false;
                return true;
            }
        return false;
    }

    public void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

    //filter maximal patterns from FP
    public Map<FTArray,String> filterFP(Map<FTArray,String> _FP){
        Map<FTArray,String> _MFP = new HashMap<>();
        try{
            Iterator < Map.Entry<FTArray,String> > fp = _FP.entrySet().iterator();
            //for each pattern
            while(fp.hasNext()){
                boolean found = false;
                Map.Entry<FTArray, String> fpEntry = fp.next();

                if(_MFP.isEmpty()){
                    _MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
                else {
                    //check the pattern existing in MFP list ?
                    Iterator<Map.Entry<FTArray, String>> mfp = _MFP.entrySet().iterator();
                    while (mfp.hasNext()) {
                        Map.Entry<FTArray, String> mfpEntry = mfp.next();
                        //check the labels of two subtrees before check maximal subtree
//                        if(checkSubsetLabel(fpEntry.getKey(), mfpEntry.getKey())) {
                        switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                            case 1:
                                found = true;
                                break;
                            case 2:
                                mfp.remove();
                                break;
                        }
//                        }
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
    public Map<FTArray,String> filterFP_multi(Map<FTArray,String> FP){
        Map<FTArray,String> MFP = new ConcurrentHashMap<>();
        FP.entrySet().parallelStream().forEach(fpEntry-> {
            boolean found = false;
            if (MFP.isEmpty()) {
                MFP.put(fpEntry.getKey(), fpEntry.getValue());
            } else {
                //check the maximality in MFP list ?
                Iterator<Map.Entry<FTArray,String>> mfp = MFP.entrySet().iterator();
                while (mfp.hasNext()) {
                    Map.Entry<FTArray,String> mfpEntry = mfp.next();
                    //check labels of pat is subset of labels of mfpEntry
//                    if(checkSubsetLabel(fpEntry.getKey(), mfpEntry.getKey())) {
                    switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                        case 1:
                            found = true;
                            break;
                        case 2:
                            mfp.remove();
                            break;
                    }
//                    }
                }
                if (!found) {
                    MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
            }
        });
        return MFP;
    }

    //print maximal patterns stored in Map to XML file
    public void outputPatterns(Map<FTArray, String> maximalPatterns, String outFile){
        try{
            //create output file to store patterns for mining common patterns
            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");
            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile, config, grammar, xmlCharacters);
            Iterator < Map.Entry<FTArray,String> > iter1 = maximalPatterns.entrySet().iterator();
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

    //print list of candidates: need for debugging
    private void printCandidates(Map<FTArray, Projected> fp){

        for(Map.Entry<FTArray, Projected> entry : fp.entrySet()){

            FTArray pat = entry.getKey();
            Projected projected = entry.getValue();

            System.out.print("\ndepth:" + projected.getProjectedDepth()+", ");

            for(int i=0; i<pat.size(); ++i){
                String label = labelIndex.get(pat.get(i));
                if(label == null){
                    System.out.print(pat.get(i)+" ");
                }else
                    System.out.print(label +" : ");
            }

            System.out.println();
            for(int i = 0 ; i<projected.getProjectLocationSize(); ++i){
                System.out.print(projected.getProjectLocation(i).getLocationId() +" ");
                printFTArray(projected.getProjectLocation(i));
            }

        }
    }

    //print a pattern
    private void printFTArray(FTArray ft){
        for(int i=0; i< ft.size(); ++i)
            System.out.print(ft.get(i)+",");
        System.out.println();
    }

    //testing: store only maximal pattern in the list
    public void addMFPTest(FTArray pat, ArrayList<FTArray> _MFP){
        if(_MFP.size() == 0) {
            _MFP.add(pat);
        }else{
            ArrayList<FTArray> tmp = new ArrayList<>(_MFP);
            for(FTArray lastPattern : tmp) {
                if(! pat.equals(lastPattern)) {
                    int checkMaximality = checkSubTree(pat, lastPattern);
                    switch (checkMaximality) {
                        case 1: //pat is a sub tree of the lastPattern then return
                            return;
                        case 2: //pat is super tree of the lastPattern in the list
                            _MFP.remove(lastPattern);
                            break;
                    }
                }else
                    return;
            }
            //if the pattern is a new maximal pattern then store it
            _MFP.add(pat);
        }
    }

    //print maximal patterns stored in ArrayList
    public void printPatterns(ArrayList<FTArray> patterns, String outFile){
        try{
            //create output file to store patterns for mining common patterns
            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");

            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile, config, grammar, xmlCharacters);
            for(int i=0; i<patterns.size(); ++i){
                //convert pattern from Integer into String
                ArrayList <String> patternStr = Pattern_Int.getPatternStr(patterns.get(i),labelIndex);
                //set the support = 1, wsupport=1 and size = size of pattern
                String supports = "1,1,"+Pattern_Int.countNode(patterns.get(i));
                //print the pattern to file
                ((XMLOutput) outputMaximalPatterns).report_Int(patternStr,supports);

                //convert patternStr into input format of original FREQT and write it to file
                outputCommonPatterns.write(Pattern.getPatternString1(patternStr)+"\n");
            }
            //close output file
            outputMaximalPatterns.close();

            //close common file
            outputCommonPatterns.flush();
            outputCommonPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }

}