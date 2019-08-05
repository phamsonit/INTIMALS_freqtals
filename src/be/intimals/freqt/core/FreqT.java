package be.intimals.freqt.core;

import be.intimals.freqt.input.CreateSingleTree;
import be.intimals.freqt.input.ReadXML;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;
import be.intimals.freqt.util.*;
import be.intimals.freqt.output.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/*
    extended FREQT
 */
public class FreqT {
    static char uniChar = '\u00a5';// Japanese Yen symbol
    protected Config config;
    protected Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    protected Map <String,Vector<String> > grammar    = new LinkedHashMap<>();
    protected Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    protected Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    protected Map <String,String>          xmlCharacters  = new LinkedHashMap<>();


    //store the indexes of all labels
    protected Map<Integer,String> labelIndex = new HashMap<>();

    ///////////
    //store root labels
    private Set <String>    rootLabels  = new HashSet<>();
    //store root occurrences of patterns
    private Map<String,String>  rootIDs = new HashMap<>();
    //store file ids of patterns
    private Map<String,String>  fileIDs = new HashMap<>();
    /////
    private int nbInputFiles;
    private int nbOutputFrequentPatterns;

    Vector<Integer> lineNrs = new Vector<>();

    ////////////////////////////////////////////////////////////////////////////////
    public FreqT(Config config) {
        this.config = config;
    }

    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}
    public Map <String,Vector<String>> getGrammar(){return this.grammar;}


     /**
     * store root occurrences of pattern for the second step
     * @param pat
     * @param projected
     */
    private void addRootIDs(Vector<String> pat, Projected projected, Map<String,String> _rootIDs){
        try {
            //find root occurrences (id-pos) of pattern
            String rootOccurrences = "";
            for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                rootOccurrences = rootOccurrences +
                        Location.getLocationId(projected.getProjectRootLocation(i)) + ("-") +
                        Location.getLocationPos(projected.getProjectRootLocation(i)) + ";";
            }
            //keep only the root occurrences and root label
            String rootLabel = pat.subList(0,1).toString();
            _rootIDs.put(rootOccurrences, rootLabel);
        }catch (Exception e){System.out.println("Error: adding rootIDs "+e);}
    }

    /**
     * filter root occurrences: keep the smallest root occurrences
     * @param _rootIDs
     */
    private Map<String, String>  filterRootOccurrences(Map<String, String> _rootIDs){
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
        Map<String, String> _newRootIDs = new LinkedHashMap<>();
         for(int i=0;i<ttt.size();++i)
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


    //convert pattern of String format, i.e., (A(B)(B)(C)) to Vector<String> format [A,B,B,C]
    private ArrayList<String> convert(String pat){
        Vector<String> temp = new Vector<>();
        Set<String> temp1 = new HashSet<>();
        ArrayList<String> temp2 = new ArrayList<>();

        int i=0;
        String buff="";
        while(i<pat.length()){
            if( (pat.charAt(i)=='(') || (pat.charAt(i)==')') ) {
                if(!buff.isEmpty()) {
                    temp2.add(buff);
                    buff = "";
                }
            }
            else{
                buff += pat.charAt(i);
            }
            ++i;
        }
        return  temp2;
    }
    //return true if either labels of pat1 contain labels of pat2 or labels of pat2 contain labels of pat1
    public boolean checkSubsetLabels(String pat1, String pat2){

        ArrayList<String> p1 = convert(pat1);
        ArrayList<String> p2 = convert(pat2);
        //System.out.println(p1);
        //System.out.println(p2);
        if(p1.size() >= p2.size()){
            //System.out.println(p1.containsAll(p2));
            return  p1.containsAll(p2);
        }
        else {
            //System.out.println(p2.containsAll(p1));
            return p2.containsAll(p1);
        }
    }

    /**
     * check if pat1 is a subtree of pat2 ?
     * return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
     * @param pat1
     * @param pat2
     * @return
     */
    public int checkSubTree(String pat1, String pat2) {
        if (checkSubsetLabels(pat1, pat2)) {

            FreqT_subtree post = new FreqT_subtree(this.config);
            post.checkSubtrees(pat1, pat2);
            if (post.getOutputPattern() == null) {
                return 0; //not related
            } else {
                if (pat1.length() <= pat2.length()) {
                    return 1; //pat1 is a subtree of pat2
                } else {
                    return 2; //pat2 is a subtree of pat1
                }
            }
        }else return 0;
    }

    /**
     * pair-wise check maximal subtree
     * adding a pattern to maximal pattern list (patSet)
     * if pat is a subtree of another pattern in the patSet then ignore this pattern
     * else if pat is a super-set of another pattern in the patSet then replace this pattern by pat
     *      else add pat to patSet
     * @param pat
     * @param projected
     * @param patSet
     */

    public void addMaximality(Vector<String> pat, Projected projected, Map<String,String> patSet){
        boolean found = false;

        if (Pattern.getPatternSize(pat) < config.getMinNode()) return;

        Iterator < Map.Entry<String,String> > p = patSet.entrySet().iterator();
        while(p.hasNext()){
            Map.Entry<String, String> entry = p.next();
            switch (checkSubTree(Pattern.getPatternString1(pat),entry.getKey())){
                case 1:
                    found = true;
                    break;
                case 2:
                    p.remove();
                    break;
            }
        }
        if(! found) {
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport(); //=> root location
            int size = Pattern.getPatternSize(pat);

            //replace "," in the leafs by uniChar
            String patString = pat.elementAt(0);
            for(int i=1; i< pat.size(); ++i)
                patString = patString+"," + pat.elementAt(i);
                //patString = patString + uniChar + pat.elementAt(i);

            String patternSupport =
                    "rootOccurrences" + "," +
                            String.valueOf(support) + "," +
                            String.valueOf(wsupport) + "," +
                            String.valueOf(size)+"\t"+
                            patString;//pat.toString(); //keeping for XML output

            String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes
            patSet.put(patternString, patternSupport);
        }
    }


    /**
     * check output conditions
     * @param pat
     * @return
     */
    public boolean checkOutput(Vector<String> pat){
        if(Pattern.countLeafNode(pat) < config.getMinLeaf())// || Pattern.countIdentifiers(pat) <= config.getNbIdentifiers())
            return true;
        else
            return false;
    }
    /**
     * choose output
     * @param pat
     * @param projected
     */
    public void chooseOutput(Vector<String> pat, Projected projected, Map<String,String> _outputMaximalPatternsMap, boolean post){
        if (! checkOutput(pat)) {
            nbOutputFrequentPatterns++;
            if (post) { //store root occurrences for next step
                addRootIDs(pat, projected, rootIDs);
            } else //check and store pattern to maximal pattern list
                //if using pair-wise subtree check
                addMaximality(pat, projected, _outputMaximalPatternsMap);
                //if using original FREQT
                //_outputMaximalPatternsMap.put(...,...)
        }
    }

    /**
     * calculate the support of a pattern = number of files
     * @param projected
     * @return
     */
    public int support(Projected projected){
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
    public int rootSupport(Projected projected){
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
    public void prune (Map <String, Projected > candidates, int minSup){

        Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
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

    /**
     * prune candidates based on blacklist children
     * blacklist is created in the readWhiteLabel procedure
     * @param candidate
     */
    public void pruneBlackList(Vector<String> pat, Map <String, Projected > candidate,
                               Map <String,Vector<String> > _blackLabels){
        Iterator < Map.Entry<String,Projected> > iterTemp = candidate.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, Projected> entry = iterTemp.next();
            Set<String> blackListChildren = Pattern.getChildrenLabels(_blackLabels,pat,entry.getKey());
            //System.out.println("blackListChildren "+ blackListChildren);

            String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
            //System.out.println("candidateLabel "+ candidateLabel);
            if(     blackListChildren.contains(candidateLabel) ||
                    candidateLabel.equals("*get") ||
                    candidateLabel.equals("*set") ||
                    candidateLabel.equals("*RETURN-CODE") ||
                    candidateLabel.equals("*SORT-RETURN") ||
                    candidateLabel.equals("*SORT-CORE-SIZE") ||
                    candidateLabel.equals("*TALLY") ||
                    candidateLabel.equals("*XML-CODE") ){
                //System.out.println(candidateLabel+" in black list");
                iterTemp.remove();
            }
        }
    }

    /**
     * generate candidates by using rightmost extension
     * @param projected
     * @return
     */
    public Map<String, Projected> generateCandidates(Projected projected,
                                                     Vector <Vector<NodeFreqT> >  _transaction) {
        Map<String, Projected> candidates = new LinkedHashMap<>();
        //Map<String, Projected> candidates = new ConcurrentHashMap<>();
        //keep the order of elements
        try{
            // Find all candidates of the current subtree
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
                String prefix = "";
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.elementAt(id).elementAt(pos).getNodeChild() :
                            _transaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = _transaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + _transaction.elementAt(id).elementAt(l).getNodeLabel();
                        //String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();

                        Projected tmp;
                        if (candidates.containsKey(item)) {
                            candidates.get(item).addProjectLocation(id, l, occurrences);//keeping all locations
                            int rootPos = Location.getLocationList(projected.getProjectLocation(i)).get(0);
                            candidates.get(item).setProjectRootLocation(id, rootPos);//keeping root locations
                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            int rootPos = Location.getLocationList(projected.getProjectLocation(i)).get(0);
                            tmp.setProjectRootLocation(id, rootPos); //keeping root locations
                            candidates.put(item, tmp);
                        }
                    }
                    if (d != -1) {
                        pos = _transaction.elementAt(id).elementAt(pos).getNodeParent();
                    }
                    prefix += uniChar + ")";
                }
            }
        }
        catch (Exception e){System.out.println("Error: generate candidates " + e);}
        return candidates;
    }

    /**
     * expand a pattern based on obligatory children, children repeated label
     * @param entry
     */
    private void grammarExpand(Vector<String> pattern,
                               Map.Entry<String, Projected> entry,
                               Map<String,String> outputMaximalPatternsMap){
        //grammar, blacklist, whitelist

        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project(pattern,entry.getValue(),outputMaximalPatternsMap);
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(pattern, entry.getKey());
            String parentLabel = pattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(pattern,entry.getValue(),outputMaximalPatternsMap);
                        break;
                    case "1..*"://node-list
                        //project(entry.getValue());
                        if(parentOrdered.equals("unordered")) {
                            //don't allow N children of an unordered node to have the same label
                            if (Pattern.isRepeatedLabel(pattern, entry.getKey(), config.getMaxRepeatLabel()))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                project(pattern,entry.getValue(),outputMaximalPatternsMap);
                            else{//output the current pattern
                                chooseOutput(pattern,entry.getValue(),outputMaximalPatternsMap,config.postProcess());
                                return;
                            }
                        }else
                            if(parentOrdered.equals("ordered")){
                                project(pattern,entry.getValue(),outputMaximalPatternsMap);
                            }
                        break;

                    default: //AST node has fixed N children
                        //project(entry.getValue());
                        //find all children of parentPos in the grammar
                        Vector<String> listOfChildrenGrammar = new Vector<>();
                        listOfChildrenGrammar.addAll(grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));
                        //find all children of parentPos in the pattern
                        Vector<String> listOfChildrenPattern = Pattern.findChildrenLabels(pattern, parentPos);
                        //find white labels and black labels
                        Set<String> blackLabelChildren = new LinkedHashSet<>();
                        Set<String> whiteLabelChildren = new LinkedHashSet<>();
                        if(whiteLabels.containsKey(parentLabel))
                            whiteLabelChildren.addAll(whiteLabels.get(parentLabel));
                        if(blackLabels.containsKey(parentLabel))
                            blackLabelChildren.addAll(blackLabels.get(parentLabel));
                        //expand this candidate if it doesn't miss the previous mandatory sibling
                        if (! Pattern.isMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(pattern,entry.getValue(),outputMaximalPatternsMap);
                        }else {
                            chooseOutput(pattern, entry.getValue(),outputMaximalPatternsMap,config.postProcess());
                            return;
                        }

                        break;
                }
            }
        }
    }

    /**
     * expand a pattern with size constraint
     * @param entry
     */
    private void expandCandidate(Vector<String> pattern, Map.Entry<String, Projected> entry,
                                 Map<String,String> outputMaximalPatternsMap) {
        try{
            //add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    pattern.addElement(p[i]);
            }

            if(Pattern.countLeafNode(pattern) <= config.getMaxLeaf())
            {
                if (Pattern.isMissedLeafNode(pattern)){
                    chooseOutput(pattern,entry.getValue(),outputMaximalPatternsMap,config.postProcess());
                    return;
                }else
                    grammarExpand(pattern,entry,outputMaximalPatternsMap);
                    //project(pattern,entry.getValue(),outputMaximalPatternsMap);
            }

        }catch (Exception e){System.out.println("Error: expand candidate " + e);}
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Vector<String> pattern, Projected projected, Map<String,String> outputMaximalPatternsMap) {
        try{
            //System.out.println("pattern: "+ pattern);
            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(pattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report pattern and then stop
            if( candidates.isEmpty() ){
                chooseOutput(pattern,projected,outputMaximalPatternsMap,config.postProcess());
                return;
            }
            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = pattern.size();
                Map.Entry<String, Projected> entry = iter.next();

                //if potential candidate = SectionStatementBlock then check if candidate belongs to black-section or not
                String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
                if(candidateLabel.equals("SectionStatementBlock"))
                    checkBlackSection(entry,transaction);
                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock")) {
                    checkContinuousParagraph(pattern, entry, transaction);
                }

                //System.out.println("potential candidate: "+ entry.getKey());
                expandCandidate(pattern, entry, outputMaximalPatternsMap);

                pattern.setSize(oldSize);
            }
        }catch (Exception e){System.out.println("Error: Freqt - projected " + e);}
    }

    /**
     * expand single-subtrees to find frequent subtrees based on input parameters
     * @param freq1
     */
    private void findFP(Vector<String> pattern, Map < String , Projected > freq1,
                        Map<String,String> outputMaximalPatternsMap){
        //pattern = new Vector<>();
        Iterator < Map.Entry<String,Projected> > iter = freq1.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            if(entry.getKey() != null && entry.getKey().charAt(0) != '*'){
                entry.getValue().setProjectedDepth(0);
                pattern.addElement(entry.getKey());
                project(pattern, entry.getValue(),outputMaximalPatternsMap);
                pattern.setSize(pattern.size() - 1);
            }
        }
    }

    /**
     * Return all frequent subtrees of size 1
     * @return
     */
    public Map<String, Projected> buildFP1Set(Vector < Vector<NodeFreqT> > trans) {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.elementAt(i).size(); ++j) {
                String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                //if node_label in rootLabels and lineNr > lineNr threshold
                if(rootLabels.contains(node_label) || rootLabels.isEmpty()){
                    //System.out.println(lineNr+"  "+lineNrs.elementAt(i));
                    //if(Integer.valueOf(lineNr) > lineNrs.elementAt(i)){ //using only for Cobol data
                    //find a list of locations then add it to freq1[node_label].locations
                    if (node_label != null) {
                        //System.out.println("Node "+ node_label+" "+lineNr);
                        //if node_label already exists
                        if (freq1.containsKey(node_label)) {
                            freq1.get(node_label).setProjectLocation(i, j);
                            //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            freq1.get(node_label).setProjectRootLocation(i, j);
                        } else {
                            Projected projected = new Projected();
                            projected.setProjectLocation(i, j);
                            //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                            projected.setProjectRootLocation(i, j);
                            freq1.put(node_label, projected);
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
            if(config.buildGrammar())
                Initial.initGrammar(config.getInputFiles(),grammar,config.buildGrammar());
            else
                Initial.initGrammar(config.getGrammarFile(),grammar,config.buildGrammar());

//            Iterator < Map.Entry<String,Vector<String>> > it = grammar.entrySet().iterator();
//            while(it.hasNext()){
//                Map.Entry<String,Vector<String>> entry = it.next();
//                System.out.println(entry.getKey()+" "+entry.getValue());
//            }

            Initial.readWhiteLabel(config.getWhiteLabelFile(), grammar, whiteLabels, blackLabels); //read white labels and create black labels
            Initial.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters
            Initial.readDatabase(config.getAbstractLeafs(),config.getInputFiles(),grammar,transaction,labelIndex);
            //ReadXML.printTransaction(transaction);


//            Vector<NodeFreqT> sTree = new Vector<>();
//            CreateSingleTree tree = new CreateSingleTree();
//            tree.createTree(false,new File(config.getInputFiles()),grammar,sTree);
//
//            for(int i=0; i<sTree.size(); ++i){
//                System.out.println(sTree.elementAt(i).getNodeLevel()+" "+sTree.elementAt(i).getNodeLabel());
//            }

            //lineNrs = Initial.getLineNrs();
            //System.out.println(lineNrs);

            //set timeout for the current task
//            TimeOut timeOut = new TimeOut();
//            timeOut.setTimes(config.getTimeout() * 60 * 1000);
//            timeOut.setReport(report);
//            Thread timeOutThread = new Thread(timeOut);
//            timeOutThread.start();

            /////mining maximal patterns
            //create report file
            String reportFile = config.getOutputFile().replaceAll("\"","") +"-report.txt";
            FileWriter report = new FileWriter(reportFile);

            long start = System.currentTimeMillis( );
            nbInputFiles = transaction.size();

            log(report,"INPUT");
            log(report,"===================");
            log(report,"- data sources : " + config.getInputFiles());
            log(report,"- input files : " + nbInputFiles);
            log(report,"- minSupport : " + config.getMinSupport());

            System.out.println("Mining frequent subtrees ...");
            Vector<String> pattern = new Vector<>();
            Map<String,String> outputMaximalPatternsMap = new HashMap<>();
            //find 1-subtrees
            Map < String , Projected > FP1 = buildFP1Set(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune 1-subtrees
            prune(FP1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + freq1.keySet());
            //expand 1-subtrees to find frequent subtrees with size constraints
            findFP(pattern, FP1, outputMaximalPatternsMap);
            /////////

            System.out.println("Mining maximal frequent subtrees ...");
            if(config.postProcess()){//for each group of root occurrences expand to find largest patterns
                log(report,"");
                log(report,"OUTPUT");
                log(report,"===================");
                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                //report the phase 1
                log(report,"- Step 1: Mining frequent patterns with max size constraints");
                log(report,"\t + Frequent patterns = "+ nbOutputFrequentPatterns);
                log(report, "\t + running time = "+ Float.valueOf(diff1)/1000 +"s");
                //log(report,"#root occurrences groups = "+ rootIDs.size());
                //filter root occurrences
                filterRootOccurrences(rootIDs);
                //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
                //phase 2: find maximal patterns from rootIDs
                log(report,"- Step 2: Mining maximal patterns WITHOUT max size constraint:");
                //serial running
                //FreqT_ext_serial freqT_ext = new FreqT_ext_serial(config, this.grammar, this.blackLabels,this.whiteLabels,this.xmlCharacters,this.transaction);
                //parallel running
                FreqT_ext_multi freqT_ext = new FreqT_ext_multi(config, this.grammar, this.blackLabels, this.whiteLabels,this.xmlCharacters,this.transaction);
                freqT_ext.run(rootIDs,start,report);

            }else{//output maximal patterns in the first step
                log(report,"OUTPUT");
                log(report,"===================");
                log(report,"- Mining maximal patterns with max size constraint");

                String outFile = config.getOutputFile();
                outputMFP(outputMaximalPatternsMap,outFile);

                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                log(report,"\t + Frequent patterns = "+ nbOutputFrequentPatterns);
                log(report,"\t + Maximal patterns = "+ outputMaximalPatternsMap.size());
                log(report,"\t + Running times = "+ diff1 +" ms");
                report.close();
            }
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt");
            e.printStackTrace();
        }
    }

    public void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

    //filter and print maximal patterns
    public void outputMFP(Map<String,String> maximalPatterns,String outFile){
        try{

            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");
            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile,config, grammar, xmlCharacters);
            Iterator < Map.Entry<String,String> > iter1 = maximalPatterns.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                outputMaximalPatterns.printPattern(entry.getValue());
                //System.out.println(entry.getKey());
                outputCommonPatterns.write(entry.getKey()+"\n");
            }
            outputMaximalPatterns.close();

            outputCommonPatterns.flush();
            outputCommonPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }

    /////////// specific functions for COBOL source code //////////////////
    public void checkContinuousParagraph(Vector<String> pat,
                                         Map.Entry<String, Projected> entry,
                                         Vector <Vector<NodeFreqT> >  _transaction){
        //boolean continuous = true;
        try{
            //System.out.println(entry.getKey());
            Vector<String> tmp = new Vector<>(pat);
            Pattern.addCandidate(tmp,entry.getKey());
            Projected projected = entry.getValue();

//            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
//                System.out.println(Location.getLocationList(projected.getProjectLocation(i)));
//            }
            //find parent's location of Paragraph
            int parentPos = Pattern.findParentPosition(tmp,entry.getKey());
            //System.out.println("parent "+parentPos);
            //find Paragraph locations
            List<Integer> childrenPos = Pattern.findChildrenPosition(tmp,parentPos);
            //System.out.println("number of paragraphs "+childrenPos.size());
            if (childrenPos.size()==1) return;
            //check continuous paragraphs
            int i=0;
            while(i < projected.getProjectLocationSize()){
                int id = Location.getLocationId(projected.getProjectLocation(i));
                List<Integer> pos = Location.getLocationList(projected.getProjectLocation(i));
                //System.out.println(pos);
                if(childrenPos.size()>1){
                    int pos1 = pos.get(childrenPos.get(childrenPos.size()-2));
                    int pos2 = pos.get(childrenPos.get(childrenPos.size()-1));
                    //System.out.println(pos1+" "+pos2);
                    if (_transaction.elementAt(id).elementAt(pos1).getNodeSibling() != pos2){
                        //remove paragraph location
                        projected.deleteProjectLocation(i);
                        i--;
                    }else
                        i++;
                }
            }
            entry.setValue(projected);
            ///
//            for (int k = 0; k < projected.getProjectLocationSize(); ++k) {
//                System.out.println(Location.getLocationList(projected.getProjectLocation(k)));
//            }
        }catch (Exception e){
            System.out.println("checkContinuousParagraph "+e);
        }
    }
    /**
     * delete locations of a label that belongs to black-section?
     */
    public void checkBlackSection(Map.Entry<String, Projected> entry, Vector <Vector<NodeFreqT> >  _transaction){

        //TODO: read black-section from file
        Set<String> blackSectionList = new HashSet<>();
        blackSectionList.add("*CCVS1");
        blackSectionList.add("*CCVS-EXIT");

        try{
            Projected projected = entry.getValue();
//            System.out.println("pos of sections");
//            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
//                System.out.println(Location.getLocationList(projected.getProjectLocation(i)));
//            }
            //////
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
            ///////
//            System.out.println("pos of sections after removed");
//            for (int j = 0; j < projected.getProjectLocationSize(); ++j) {
//                System.out.println(Location.getLocationList(projected.getProjectLocation(j)));
//            }

            entry.setValue(projected);

        }catch (Exception e){
            System.out.println("Error: Delete SectionStatementBlock "+e);
        }
    }

    public boolean checkBlackSectionNew(Map.Entry<String, Projected> entry, Vector <Vector<NodeFreqT> >  _transaction){

        Set<String> blackSectionList = new HashSet<>();
        blackSectionList.add("*CCVS1");
        blackSectionList.add("*CCVS-EXIT");
        boolean found = false;
        try{
            int i=0;
            Projected projected = entry.getValue();
//            int id = Location.getLocationId(projected.getProjectLocation(i));
//            //for each location check if it belongs to SectionStatementBlock or not
//            int currentPos = Location.getLocationPos(projected.getProjectLocation(i));
//
//            int searchPos = currentPos;
//            //check if label of section is in black-section or not
//            while (searchPos != -1) {
//                //System.out.println("search label " + transaction.elementAt(id).elementAt(searchPos).getNodeLabel());
//                if (blackSectionList.contains(_transaction.elementAt(id).elementAt(searchPos).getNodeLabel())) {
//                    System.out.println("found " + id + " " + searchPos);
//                    //projected.deleteProjectLocation(i);
//                    //i--;
//                    //projected.deleteProjectLocation(currentPos);
//                    found = true;
//                    break;
//                } else {
//                    searchPos = _transaction.elementAt(id).elementAt(searchPos).getNodeChild();
//                }
//            }


//            //System.out.println("pos of sections");
//            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
//                System.out.println(Location.getLocationList(projected.getProjectLocation(i)));
//            }

            while(i < projected.getProjectLocationSize() && !found) {
                //get position of the current label
                int id = Location.getLocationId(projected.getProjectLocation(i));
                //for each location check if it belongs to SectionStatementBlock or not
                int currentPos = Location.getLocationPos(projected.getProjectLocation(i));

                int searchPos = currentPos;
                //check if label of section is in black-section or not
                while (searchPos != -1) {
                    //System.out.println("search label " + transaction.elementAt(id).elementAt(searchPos).getNodeLabel());
                    if (blackSectionList.contains(_transaction.elementAt(id).elementAt(searchPos).getNodeLabel())) {
                        System.out.println("found " + id + " " + searchPos);
                        //projected.deleteProjectLocation(i);
                        //i--;
                        //projected.deleteProjectLocation(currentPos);
                        found = true;
                        break;
                    } else {
                        searchPos = _transaction.elementAt(id).elementAt(searchPos).getNodeChild();
                    }
                }
                i++;
            }

        }catch (Exception e){
            System.out.println("Error: Delete locations of SectionStatementBlock "+e);
        }
        return found;
    }

}