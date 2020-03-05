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

//TODO: create abstract class for Freqt_Int and Freqt_Int_2_class
public class Freqt_Int_2class{

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
    private Map<FTArray, String> MFP = new HashMap<>();
    //store root labels
    private Set<String> rootLabels  = new HashSet<>();

    private long timeStart;
    private long timeout;
    private boolean finished;

    private ArrayList<Integer> transactionClassID = new ArrayList<>();
    private int sizeClass1;
    private int sizeClass2;

    ///////////////////////////////
    public Freqt_Int_2class(Config _config){
        config = _config;
    }

    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}
    public Map <String,ArrayList <String>> getGrammar(){return this.grammar;}

    /**
     * run Freqt with file config.properties on 2-class dataset
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
            Map <FTArray, Projected> FP1 = buildFP1(transaction);
            //prune FP1 on minimum support
            // Constraint.prune(FP1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + FP1.keySet());
            //expand FP1 to find maximal patterns
            expandFP1(FP1);

            //run the second step if getTwoStep = true
            printPatternInTheFirstStep(report, timeStart);

        }
        catch (Exception e) {
            System.out.println("Error: running Freqt_Int");
            e.printStackTrace();
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

    private FileWriter initReport() throws IOException {
        String reportFile = config.getOutputFile().replaceAll("\"","") +"-report.txt";
        FileWriter report = new FileWriter(reportFile);
        FreqT_Int.log(report,"INPUT");
        FreqT_Int.log(report,"===================");
        FreqT_Int.log(report,"- data sources : " + config.getInputFiles());
        FreqT_Int.log(report,"- input files : " +  transaction.size());
        FreqT_Int.log(report,"- minSupport : " + config.getMinSupport());
        report.flush();
        return report;
    }

    //read database and additional information
    private void initData() throws Exception {
        //transaction = new ReadXML_Int().readDatabase(1, config.getAbstractLeafs(), new File(config.getInputFiles()), labelIndex);

        ReadXML_Int readXML_int = new ReadXML_Int();
        readXML_int.readDatabase(transaction,1, config.getAbstractLeafs(),
                new File(config.getInputFiles1()), labelIndex, transactionClassID);
        readXML_int.readDatabase(transaction,0, config.getAbstractLeafs(),
                new File(config.getInputFiles2()), labelIndex, transactionClassID);

        sizeClass1 = transactionClassID.stream().mapToInt(Integer::intValue).sum();
        sizeClass2 = transactionClassID.size() - sizeClass1;

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

    /**
     * Return all frequent subtrees of size 1
     * @param trans
     * @return
     */
    private Map<FTArray, Projected> buildFP1(ArrayList  < ArrayList <NodeFreqT> > trans) {
        Map<FTArray, Projected> freq1 = new LinkedHashMap<>();
        try {
            for (int i = 0; i < trans.size(); ++i) {
                //get transaction label
                int classID = transactionClassID.get(i);

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
            expand2ClassPattern(pattern, entry.getValue());
            //reset pattern
            pattern = new FTArray();
        }
    }

    //expand pattern in 2-class data
    private void expand2ClassPattern(FTArray pattern, Projected projected) {
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
                add2ClassPattern(pattern, projected);
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
                        add2ClassPattern(pattern, entry.getValue());
                    }else{
                        //continue expanding pattern
                        expand2ClassPattern(pattern, entry.getValue());
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

    /**
     * print patterns found in the first step
     * @param report
     * @param start
     * @throws IOException
     */
    private void printPatternInTheFirstStep(FileWriter report, long start) throws IOException {
        FreqT_Int.log(report,"OUTPUT");
        FreqT_Int.log(report,"===================");
        if(finished)
            FreqT_Int.log(report,"finished search");
        else
            FreqT_Int.log(report,"timeout");

        String outFile = config.getOutputFile();
        int nbMFP;
        //if filtering maximal pattern in the mining process then print patterns
        nbMFP = MFP.size();
        outputPatterns(MFP, outFile);

        long end1 = System.currentTimeMillis( );
        long diff1 = end1 - start;
        FreqT_Int.log(report,"+ Maximal patterns = "+ nbMFP);
        FreqT_Int.log(report,"+ Running times = "+ diff1/1000 +" s");
        report.close();
    }

    //add 2-class pattern into MFP
    private void add2ClassPattern(FTArray pattern, Projected projected){
        FTArray patTemp = Pattern_Int.removeMissingLeaf(pattern);
        //check minsize constraints and right mandatory children before adding pattern
        if(Constraint.checkOutput(patTemp, config.getMinLeaf(), config.getMinNode())
                && ! Constraint.checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){
            //keep patterns having highest score
            addHighScorePattern(patTemp, projected, MFP);
            //keep maximal patterns
            //addMFP(patTemp, projected, MFP);
        }
    }

    //return minimum score in the list of patterns
    private double getMinScore(Map<FTArray,String> _MFP){
        double score = 1000.0;
        for(Map.Entry<FTArray, String> entry : _MFP.entrySet()){
            String[] tmp = entry.getValue().split(",");
            double scoreTmp = Double.valueOf(tmp[1]);
            if(score > scoreTmp)
                score = scoreTmp;
        }
        return score;
    }

    // return a pattern which has minimum score in the list of patterns
    private FTArray getMinScorePattern(Map<FTArray,String> _MFP){
        double score = 1000.0;
        FTArray smallestPattern = new FTArray();

        for(Map.Entry<FTArray, String> entry : _MFP.entrySet()){
            String[] tmp = entry.getValue().split(",");
            double scoreTmp = Double.valueOf(tmp[1]);
            if(score > scoreTmp) {
                score = scoreTmp;
                smallestPattern = new FTArray(entry.getKey());
            }
        }
        return smallestPattern;
    }

    //make a string of support, score, size for a pattern
    private String getPatternSupportString(FTArray pattern, Projected projected, double score){
        String patternSupport = "";
        int[] ac = Constraint.get2ClassSupport(projected);
        String support = String.valueOf(ac[0]) +"-"+String.valueOf(ac[1]);
        int size = Pattern_Int.countNode(pattern);
        patternSupport = support + "," + score + "," + String.valueOf(size);

        return patternSupport;
    }

    //keep only numPattern (for example 100) patterns which have highest score
    private void addHighScorePattern(FTArray pattern, Projected projected, Map<FTArray,String> _MFP){
        if(_MFP.containsKey(pattern)) return;

        double score = Constraint.chiSquare(projected, sizeClass1, sizeClass2);
        //double score = Constraint.oddSup(projected, sizeClass1, sizeClass2);
        int[]ac = Constraint.get2ClassSupport(projected);
        printFTArray(pattern);
        System.out.println(Constraint.getSupport(projected)+" [ "+ac[0]+", "+ac[1]+", DSScore : " + score +" ]");
        if(score >= config.getDSScore() ) {
            if(_MFP.size() >= config.getNumPatterns()) {
                double minScore = getMinScore(_MFP);
                if (score > minScore) {
                    //System.out.println("replace pattern");
                    FTArray minPattern = getMinScorePattern(_MFP);
                    String patternSupport = getPatternSupportString(pattern, projected, score);
                    //remove smallest score pattern
                    _MFP.remove(minPattern);
                    //add new pattern
                    _MFP.put(pattern, patternSupport);
                }
            }else{
                //add new pattern
                String patternSupport = getPatternSupportString(pattern, projected, score);
                _MFP.put(pattern, patternSupport);
            }
        }
    }


    //check and add pattern to maximal pattern list
    private void addMFP(FTArray pattern, Projected projected, Map<FTArray,String> _MFP){
        if(_MFP.containsKey(pattern)) return;

        double score = Constraint.chiSquare(projected, sizeClass1, sizeClass2);
        if(score >= config.getDSScore()){
            Iterator < Map.Entry<FTArray,String> > p = _MFP.entrySet().iterator();
            while(p.hasNext()){
                Map.Entry<FTArray, String> entry = p.next();
                switch (checkSubTree(pattern,entry.getKey())){
                    case 1: //pat is a subtree of entry.getKey
                        return;
                    case 2: //entry.getKey is a subtree of pat
                        p.remove();
                        break;
                }
            }

            String patternSupport = getPatternSupportString(pattern, projected, score);
            //add new pattern to the list
            _MFP.put(pattern, patternSupport);
        }
    }

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


    //print maximal patterns stored in Map to XML file
    private void outputPatterns(Map<FTArray, String> maximalPatterns, String outFile){
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

    //print list of candidates: for debugging
    private void printCandidates(Map<FTArray, Projected> fp, Map<Integer, String> _labelIndex){
        for(Map.Entry<FTArray, Projected> entry : fp.entrySet()){
            FTArray pat = entry.getKey();
            Projected projected = entry.getValue();
            System.out.print("\ndepth:" + projected.getProjectedDepth()+", ");
            for(int i=0; i<pat.size(); ++i){
                String label = _labelIndex.get(pat.get(i));
                if(label == null){
                    System.out.print(pat.get(i)+" ");
                }else
                    System.out.print(label +" : ");
            }
            System.out.println();
            for(int i = 0 ; i<projected.getProjectLocationSize(); ++i){
                System.out.print(projected.getProjectLocation(i).getClassID() +" ");
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

}
