package be.intimals.freqt.core;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;
import be.intimals.freqt.util.*;
import be.intimals.freqt.output.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;



public class FreqT {
    static char uniChar = '\u00a5';// Japanese Yen symbol
    protected Config config;

    private AOutputFormatter outputMaximalPatterns;

    private Vector <String>  pattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();

    protected Map <String,Vector<String> > grammar    = new LinkedHashMap<>();
    protected Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    protected Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    protected Map <String,String>          xmlCharacters  = new LinkedHashMap<>();

    private Set <String>        rootLabels  = new HashSet<>();
    private Map<String,String>  fileIDs = new HashMap<>();
    private Map<String,String>  rootIDs = new HashMap<>();

    private int nbInputFiles;
    private int nbOutputFrequentPatterns;
    private int nbOutputLargestPatterns;
    private int nbOutputMaximalPatterns;

    //store maximal patterns if don't use the second step
    private Map<String,String> outputMaximalPatternsMap = new HashMap<>();


    ////////////////////////////////////////////////////////////////////////////////

    public FreqT(Config config) {
        this.config = config;
    }

    public int getNbInputFiles(){
        return this.nbInputFiles;
    }
    public int getNbOutputFrequentPatterns(){
        return this.nbOutputFrequentPatterns;
    }
    public int getNbOutputMaximalPatterns(){
        return this.nbOutputMaximalPatterns;
    }

    public Map <String,Vector<String> > getGrammar(){ return this.grammar;}
    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}


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
     * store root occurrences of pattern for the second step
     * @param pat
     * @param projected
     */
    public void addRootIDs(Vector<String> pat, Projected projected, Map<String,String> _rootIDs){
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
    private void filterRootOccurrences(Map<String, String> _rootIDs){
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

         rootIDs = _newRootIDs;
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
     * check if pat1 is a subtree of pat2 ?
     * return 1 : pat1 is subset of 2; 2 : pat2 is subset of pat1; otherwise return 0
     * @param pat1
     * @param pat2
     * @return
     */
    private int checkSubTree(String pat1, String pat2){

        FreqT_max1 post = new FreqT_max1(this.config);
        post.checkSubtrees(pat1, pat2);

        if (post.getOutputPattern() == null){
            return 0;
        }else{
            if( pat1.length() <= pat2.length() ) {
                return 1;
            }
            else {
                return 2;
            }
        }
    }

    /**
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
     * choose output
     * @param pat
     * @param projected
     */
    private void chooseOutput(Vector<String> pat, Projected projected){

        if (checkOutput(pat)) return;

        nbOutputFrequentPatterns++;

        if (config.postProcess()) { //store root occurrences for next step
            addRootIDs(pat, projected,rootIDs);
        }else //check and store pattern to maximal pattern list
            addMaximality(pat,projected,outputMaximalPatternsMap);

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
        //keep the order of elements
        try{
            // Find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = Location.getLocationId(projected.getProjectLocation(i));
                int pos = Location.getLocationPos(projected.getProjectLocation(i));
                //TODO: how to keep only root and rightmost positions
                List<Integer> occurrences = Location.getLocationList(projected.getProjectLocation(i)).subList(0,1);
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

    //expand candidate based on grammar

    /**
     * expand a pattern based on obligatory children, children repeated label
     * @param entry
     */
    private void grammarExpand(Map.Entry<String, Projected> entry){

        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project( entry.getValue() );
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(pattern, entry.getKey());
            String parentLabel = pattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(entry.getValue());
                        break;
                    case "1..*"://node-list
                        //project(entry.getValue());
                        if(parentOrdered.equals("unordered")) {
                            //don't allow N children of an unordered node to have the same label
                            if (Pattern.isRepeatedLabel(pattern, entry.getKey(), config.getMaxRepeatLabel()))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                project(entry.getValue());
                            else{//output the current pattern
                                chooseOutput(pattern,entry.getValue());
                                return;
                            }
                        }else
                            if(parentOrdered.equals("ordered")){
                                project(entry.getValue());
                            }
                        break;

                    default: //AST node has fixed N children
                        //project(entry.getValue());
                        //find all children of parentPos in the grammar
                        Vector<String> listOfChildrenGrammar = new Vector<>();
                        listOfChildrenGrammar.addAll(grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));
                        //find all children of parentPos in the pattern
                        Vector<String> listOfChildrenPattern = Pattern.findChildren(pattern, parentPos);
                        //find white labels and black labels
                        Set<String> blackLabelChildren = new LinkedHashSet<>();
                        Set<String> whiteLabelChildren = new LinkedHashSet<>();
                        if(whiteLabels.containsKey(parentLabel))
                            whiteLabelChildren.addAll(whiteLabels.get(parentLabel));
                        if(blackLabels.containsKey(parentLabel))
                            blackLabelChildren.addAll(blackLabels.get(parentLabel));
                        //expand this candidate if it doesn't miss the previous mandatory sibling
                        if (! Pattern.isMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(entry.getValue());
                        }else {
                            chooseOutput(pattern, entry.getValue());
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
    private void expandCandidate(Map.Entry<String, Projected> entry) {
        try{
            //add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    pattern.addElement(p[i]);
            }

            if(Pattern.countLeafNode(pattern) <= config.getMaxLeaf()){
                if (Pattern.isMissedLeafNode(pattern)){
                    chooseOutput(pattern,entry.getValue());
                    return;
                }else
                    grammarExpand(entry);
            }
        }catch (Exception e){System.out.println("Error: expand candidate " + e);}
    }


    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{
            //System.out.println(pattern);
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
                chooseOutput(pattern,projected);
                return;
            }
            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = pattern.size();
                Map.Entry<String, Projected> entry = iter.next();
                expandCandidate(entry);
                pattern.setSize(oldSize);
            }
        }catch (Exception e){System.out.println("Error: projected " + e);}
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

            Initial.readWhiteLabel(config.getWhiteLabelFile(), grammar, whiteLabels, blackLabels); //read white labels and create black labels
            Initial.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters
            Initial.initDatabase(config.getInputFiles(),grammar,transaction);

            long start = System.currentTimeMillis( );

            nbInputFiles = transaction.size();

            //create report file
            String reportFile = config.getOutputFile().replaceAll("\"","") +"-report.txt";
            FileWriter report = new FileWriter(reportFile);

            //set timeout for the current task
            TimeOut timeOut = new TimeOut();
            timeOut.setTimes(config.getTimeout() * 60 * 1000);
            timeOut.setReport(report);
            Thread timeOutThread = new Thread(timeOut);
            timeOutThread.start();
            log(report,"INPUT");
            log(report,"===================");

            log(report,"- data sources : " + config.getInputFiles());
            log(report,"- input files : " + nbInputFiles);
            log(report,"- minSupport : " + config.getMinSupport());

            //find 1-subtrees
            Map < String , Projected > freq1 = buildFreq1Set(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune 1-subtrees
            prune(freq1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + freq1.keySet());
            //expand 1-subtrees to find frequent subtrees with size constraints
            findFrequentSubtrees(freq1);

            if(config.postProcess()){ //expanding every group of root occurrences to find largest patterns
                log(report,"");
                log(report,"OUTPUT");
                log(report,"===================");
                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                //report phase 1
                log(report,"- Step 1: Find frequent patterns with max size constraints");
                log(report,"\t + Frequent patterns = "+ nbOutputFrequentPatterns + ", time = "+ diff1 +"ms");
                //log(report,"#root occurrences groups = "+ rootIDs.size());

                filterRootOccurrences(rootIDs);

                //log(report,"#filtered root occurrences groups = "+ rootIDs.size());
                //phase 2: find maximal patterns from rootID
                log(report,"- Step 2: Find maximal patterns WITHOUT max size constraint:");
                FreqT_ext freqT_ext = new FreqT_ext(config, this.grammar, this.blackLabels,this.whiteLabels,this.xmlCharacters);
                freqT_ext.run(rootIDs,transaction,diff1,report);
                nbOutputLargestPatterns = freqT_ext.getNbOutputMaximalPatterns();
                long end2 = System.currentTimeMillis( );
                long diff2 = end2 - end1;
                //report phase 2
                //log(report,"===================");
                log(report,"\t + Maximal patterns = "+nbOutputLargestPatterns+", time = "+ diff2 +"ms");
                //total running time
                log(report,"- Total running times = "+(end2-start)+" ms");
                report.close();

            }else{//output maximal patterns in the first step
                log(report,"OUTPUT");
                log(report,"===================");
                log(report,"- Find maximal patterns with max size constraint");
                outputMaximalPatterns =  new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters);
                Iterator < Map.Entry<String,String> > iter1 = outputMaximalPatternsMap.entrySet().iterator();
                while(iter1.hasNext()){
                    Map.Entry<String,String> entry = iter1.next();
                    outputMaximalPatterns.printPattern(entry.getValue());
                }
                outputMaximalPatterns.close();

                long end1 = System.currentTimeMillis( );
                long diff1 = end1 - start;
                log(report,"\t + Frequent patterns = "+ nbOutputFrequentPatterns);
                log(report,"\t + Maximal patterns = "+outputMaximalPatterns.getNbPattern());
                log(report,"\t + Running times = "+ diff1 +" ms");
                report.close();

            }
        }
        catch (Exception e) {
            System.out.println("Error: running Freqt");
            e.printStackTrace();
        }
    }

    /**
     * expand single-subtrees to find frequent subtrees based on input parameters
     * @param freq1
     */
    private void findFrequentSubtrees(Map < String , Projected > freq1){
        pattern = new Vector<>();
        Iterator < Map.Entry<String,Projected> > iter = freq1.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            if(entry.getKey() != null && entry.getKey().charAt(0) != '*'){
                if(rootLabels.contains(entry.getKey()) || rootLabels.isEmpty())
                {
                    if (grammar.containsKey(entry.getKey())) {
                        entry.getValue().setProjectedDepth(0);
                        pattern.addElement(entry.getKey());

                        project(entry.getValue());

                        pattern.setSize(pattern.size() - 1);

                    } else {
                        System.out.println(entry.getKey() + " doesn't exist in grammar ");
                    }
                }
            }
        }
    }

    /**
     * Return all frequent subtrees of size 1
     * @return
     */
    public Map<String, Projected> buildFreq1Set(Vector < Vector<NodeFreqT> > trans) {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.elementAt(i).size(); ++j) {
                String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                //find a list of locations then add it to freq1[node_label].locations
                if(node_label != null){
                    //System.out.println("Node "+ node_label+" "+lineNr);
                    //if node_label already exists
                    if(freq1.containsKey(node_label)) {
                        freq1.get(node_label).setProjectLocation(i,j);
                        //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        freq1.get(node_label).setProjectRootLocation(i,j);
                    }
                    else {
                        Projected projected = new Projected();
                        projected.setProjectLocation(i,j);
                        //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        projected.setProjectRootLocation(i,j);
                        freq1.put(node_label, projected);
                    }
                }
            }
        }
        return freq1;
    }

    public void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

}