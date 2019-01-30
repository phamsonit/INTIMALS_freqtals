/*
find subtrees:
 */


package be.intimals.freqt.core;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;
import be.intimals.freqt.util.*;


import java.io.*;
import java.util.*;

import be.intimals.freqt.output.*;

public class FreqT {
    private  static  char uniChar = '\u00a5';// Japanese Yen symbol
    private static Config config;
    private AOutputFormatter output;
    public  Vector <String> pattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    private Map <String,Vector<String> > grammar     = new LinkedHashMap<>();
    private Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    private Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    private Set <String>                 rootLabels  = new LinkedHashSet<>();
    private Map <String,String>          xmlCharacters  = new LinkedHashMap<>();
    private Map<String,String>           outputFrequentPatternsMap = new LinkedHashMap<>(); //store patterns for post-processing

    private int oldRootSupport;

    private  int maxSize = 0;
    private String maxPattern = "";

    private int nbInputFiles;
    private int nbOutputFrequentPatterns;
    private int nbOutputMaximalPatterns;

    private Map<String,String> fileIDs = new LinkedHashMap<>();
    private Map<String,String> rootIDs = new LinkedHashMap<>();

    private boolean threeSteps = true;
    private int nbIdentifiers = 2;

    ///////Implementations//////

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


    public boolean checkOutput(Vector<String> pat){

        if(Pattern.countLeafNode(pat) < config.getMinLeaf() ||
                Pattern.countIdentifiers(pat) <= nbIdentifiers)
            return true;
        else
            return false;

    }

    /**
     * store frequent subtrees for post-processing
     * @param pat
     * @param projected
     */
    public void addPattern(Vector<String> pat, Projected projected,
                            Map<String,String> _outputFrequentPatternsMap){

        if(checkOutput(pat)) return;

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern.getPatternSize(pat);
        //keep fileIds for itemset mining algorithm
        String fileIds = String.valueOf(projected.getProjectLocation(0).getLocationId());
        int oldId = projected.getProjectLocation(0).getLocationId();
        for(int i=1;i<projected.getProjectLocationSize(); ++i)
            if(oldId != projected.getProjectLocation(i).getLocationId() ) {
                fileIds = fileIds+","+ String.valueOf(projected.getProjectLocation(i).getLocationId());
                oldId = projected.getProjectLocation(i).getLocationId();
            }

        String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes

        String patStr =
                fileIds + " " +
                        String.valueOf(support)+" "+
                        String.valueOf(wsupport)+" "+
                        String.valueOf(size);

        _outputFrequentPatternsMap.put(patternString,patStr);
    }

    /**
     * store fileIDs of pattern for grouping patterns by fileIDs
     * @param pat
     * @param projected
     */
    private void addFileIDs(Vector<String> pat, Projected projected){

        //if(Pattern.checkMissedLeafNode(pat) || (Pattern.countLeafNode(pat) < config.getMinLeaf()) ) return;
        if((Pattern.countLeafNode(pat) < config.getMinLeaf()) ) return;

        String fileIds = String.valueOf(projected.getProjectLocation(0).getLocationId());
        int oldId = projected.getProjectLocation(0).getLocationId();
        for(int i=1;i<projected.getProjectLocationSize(); ++i)
            if(oldId != projected.getProjectLocation(i).getLocationId() ) {
                fileIds = fileIds+","+ String.valueOf(projected.getProjectLocation(i).getLocationId());
                oldId = projected.getProjectLocation(i).getLocationId();
            }

        String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes

        if(fileIDs.containsKey(fileIds)){
            if(fileIDs.get(fileIds).length() < patternString.length())
                fileIDs.replace(fileIds,patternString);
        }else
            fileIDs.put(fileIds,patternString);
    }

    /**
     * store root occurrences of patterns for grouping patterns by root occurrences
     * @param pat
     * @param projected
     */
    private void addRootIDs(Vector<String> pat, Projected projected){
        try {
            if (checkOutput(pat)) return;
            //find rootID of pattern
            String rootOccurrences = "";
            for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                rootOccurrences = rootOccurrences +
                        projected.getProjectRootLocation(i).getLocationId() + (",") +
                        projected.getProjectRootLocation(i).getLocationPos() + ";";
            }
            //find root label of this pattern
            //String rootLabel = Pattern.getPatternString1(pat);
            String rootLabel = pat.elementAt(0);

            rootIDs.put(rootOccurrences, rootLabel);
        }catch (Exception e){System.out.println("Error: adding root IDs "+e);}
    }

    private void filterRootOccurrences(Map<String, String> _rootIDs){
        //for each element of rootIDs check
        //if rootOcc is a subset of an element of rootIDs then replace
        Iterator<Map.Entry<String, String>> iter1 = _rootIDs.entrySet().iterator();
        Iterator<Map.Entry<String, String>> iter2 = _rootIDs.entrySet().iterator();

        List<String> ttt = new LinkedList(_rootIDs.keySet());

        Collections.sort(ttt, new Comparator<>() {
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
                 //System.out.println(ttt.get(i));
                 //System.out.println("hhhh");
             }

         rootIDs = _newRootIDs;

    }

    private boolean compareTwoRootOccurrences(String str1, String str2){
        Collection<String> l1 = Arrays.asList(str1.split(";"));
        Collection<String> l2 = Arrays.asList(str2.split(";"));

        if(l2.containsAll(l1))
            return true;
        else
            return false;
    }

    private void chooseOutput(Vector<String> pat, Projected projected){
        if(threeSteps){
            addRootIDs(pat, projected);
        }else{
            if (config.postProcess())
                addPattern(pat,projected,outputFrequentPatternsMap);
            else output.report(pat, projected);
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
     * calculate the support of a pattern
     * @param projected
     * @return
     */
    public int support(Projected projected){
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
     *
     * @param projected
     * @return
     */
    public int rootSupport(Projected projected){
        int rootSup = 1;
        for(int i=0; i< projected.getProjectRootLocationSize()-1;++i) {
            Location location1 = projected.getProjectRootLocation(i);
            Location location2 = projected.getProjectRootLocation(i+1);

            if( (location1.getLocationId() == location2.getLocationId() &&
                    location1.getLocationPos() != location2.getLocationPos()) ||
                    location1.getLocationId() != location2.getLocationId()
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

    ////Procedures to expand pattern ///////

    /**
     *right most extension
     * @param projected
     * @return
     */
    public Map<String, Projected> generateCandidates(Projected projected,
                                                     Vector <Vector<NodeFreqT> >  _transaction) {
        Map<String, Projected> candidates = new LinkedHashMap<>(); //keep the order of elements
        try{
            // Find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                // Add to keep all occurrences --> problem: memory consumption
                // keep root location and right-most location
                List<Integer> occurrences = projected.getProjectLocation(i).getLocationList().subList(0,1);

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
                            //candidate.get(item).setProjectLocation(id, l); //keep right most position
                            candidates.get(item).addProjectLocation(id, l, occurrences);//keeping all locations
                            //candidate.get(item).addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            //rootId = id, rootPos = ?
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
                            //if (id, rootPos) exists in root ???
                            candidates.get(item).setProjectRootLocation(id, rootPos);//keeping root locations
                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            //tmp.setProjectLocation(id, l); //keep right most position
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            //tmp.addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
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
        catch (Exception e){System.out.println("Error: generate candidates" + e);}
        return candidates;
    }

    //TODO: expanding all locations for unordered children
    public Map<String, Projected> generateUnorderedCandidates(Projected projected) {
        int depth = projected.getProjectedDepth();
        Map<String, Projected> candidate = new LinkedHashMap<>(); //keep the order of elements
        /**
         * if X has unordered children
         * find candidates of the right most position
         * and candidates of the X in both directions: backward, forward
         * how to extend these two set of candidates ???
         */
        return  candidate;
    }

    //expand candidate based on grammar
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
                        if(parentOrdered.equals("unordered")) {
                            //grammar constraint: don't allow N children of an unordered node to have the same label
                            if (Pattern.checkRepeatedLabel(pattern, entry.getKey(), config.getMaxRepeatLabel()))
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
                        if (! Pattern.checkMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(entry.getValue());
                        }

                        break;
                }
            }
        }

    }

    /**
     * expand a pattern
     * @param entry
     */
    private void expandCandidate(Map.Entry<String, Projected> entry) {
        try{

            // add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    pattern.addElement(p[i]);
            }

            //if number of leaf > max leaf then consider rootOccurrences
            if(Pattern.countLeafNode(pattern) <= config.getMaxLeaf()){
                if (Pattern.checkMissedLeafNode(pattern)){
                    chooseOutput(pattern,entry.getValue());
                    return;
                }else grammarExpand(entry);
            }
            else{ //if don't use the second step then expand patterns by root occurrences
                if(!threeSteps){
                    int newRootSupport = rootSupport(entry.getValue());
                    if (oldRootSupport == newRootSupport){
                        if (Pattern.checkMissedLeafNode(pattern)){
                            chooseOutput(pattern,entry.getValue());
                            return;
                        }else grammarExpand(entry);
                    }
                    else{
                        chooseOutput(pattern,entry.getValue());
                        return;
                    }
                }
            }

        }catch (Exception e){System.out.println("Error: expand candidate " + e);}
    }

    private void updateMaximalPattern(Vector<String> pat){
    if(maxSize < Pattern.getPatternSize(pat)){
        maxSize = Pattern.getPatternSize(pat);
        maxPattern = Pattern.getPatternString1(pat);
    }

}

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            oldRootSupport = rootSupport(projected);

            if(Pattern.checkMissedLeafNode(pattern)){
                chooseOutput(pattern,projected);
            }

            //System.out.println(pattern);
            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(pattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report pattern --> stop
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

                oldRootSupport = rootSupport(entry.getValue());

                pattern.setSize(oldSize);
            }
        }catch (Exception e){System.out.println("Error: projected " + e);}
    }


    /**
     * run Freqt with file config.properties
     * @param _config
     */
    public void run(Config _config) {
        try{
            config = _config;
            //os = new FileWriter(config.getOutputFile());
            /*  ==============================  */
            //System.out.println("==============================");
            //System.out.println("running FreqT");
            //System.out.println("==============================");
            if(config.buildGrammar()) Initial.initGrammar(config.getInputFiles(),grammar,config.buildGrammar());
            else Initial.initGrammar(config.getGrammarFile(),grammar,config.buildGrammar()) ;

            //ReadGrammar.printGrammar(grammar);
            Initial.readWhiteLabel(config.getWhiteLabelFile(), grammar, whiteLabels, blackLabels); //read white labels and create black labels
            Initial.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters
            Initial.initDatabase(config.getInputFiles(),grammar,transaction);

            nbInputFiles = transaction.size();

            //don't output pattern in this step

            if(!config.postProcess() && !threeSteps)
                output = config.outputAsXML() ? new XMLOutput(config, grammar, xmlCharacters) :
                                           new LineOutput(config, grammar, xmlCharacters, uniChar);

            long start = System.currentTimeMillis( );

            //find 1-subtree
            Map < String , Projected > freq1 = buildFreq1Set(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune 1-subtree
            prune( freq1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + freq1.keySet());
            //expand 1-subtree to find frequent subtrees
            expandFreq1(freq1);
            long end = System.currentTimeMillis( );
            long diff = end - start;

            long end2,diff2,end3,diff3;

            if(threeSteps){
                //expand the largest patterns according to root occurrences
                filterRootOccurrences(rootIDs);
                System.out.println("FREQT: frequent patterns = "+ "..." + ", groups = "+rootIDs.size()+", time = "+ diff);
                FreqT_ext freqT_ext = new FreqT_ext();
                freqT_ext.run(rootIDs,config,transaction,grammar,blackLabels,whiteLabels,xmlCharacters);
                end2 = System.currentTimeMillis( );
                diff2 = end2 - end;
                System.out.println("FREQT_EXT: largest patterns "+freqT_ext.getOutputLargestPatterns().size()+", time "+ diff2);

                //maximality check
                nbOutputFrequentPatterns = freqT_ext.getNbOutputLargestPatterns();
                FreqT_max post = new FreqT_max();
                post.run(config,freqT_ext.getOutputLargestPatterns(),grammar,xmlCharacters);
                nbOutputMaximalPatterns = post.getNbMaximalPattern();
                end3 = System.currentTimeMillis( );
                diff3 = end3 - end2;
                System.out.println("FREQT_MAX: maximal patterns "+post.getNbMaximalPattern()+", time "+ diff3);
            }else{
                if(config.postProcess()){
                    end2 = System.currentTimeMillis();
                    nbOutputFrequentPatterns = outputFrequentPatternsMap.size();
                    FreqT_max post = new FreqT_max();
                    post.run(config,outputFrequentPatternsMap,grammar,xmlCharacters);
                    nbOutputMaximalPatterns = post.getNbMaximalPattern();
                    end3 = System.currentTimeMillis( );
                    diff3 = end3 - end2;
                    System.out.println("FREQT_MAX: maximal patterns "+post.getNbMaximalPattern()+", time "+ diff3);
                }else
                {
                    nbOutputFrequentPatterns = output.getNbPattern();
                    output.close();
                }

            }

            //System.out.println(maxSize+" "+maxPattern);
        }
        catch (Exception e) {
            System.out.println("Error: running freqt");
        }
    }


    private void expandFreq1(Map < String , Projected > freq1){
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
    private Map<String, Projected> buildFreq1Set(Vector < Vector<NodeFreqT> > trans) {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.elementAt(i).size(); ++j) {
                String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                //find a list of location then add to freq1[node_label].locations
                if(node_label != null){
                    //System.out.println("Node "+ node_label+" "+lineNr);
                    Projected projected = new Projected();
                    //if node_label already exists
                    if(freq1.containsKey(node_label)) {
                        freq1.get(node_label).setProjectLocation(i,j);
                        freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        freq1.get(node_label).setProjectRootLocation(i,j);
                    }
                    else {
                        projected.setProjectLocation(i,j);
                        projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        projected.setProjectRootLocation(i,j);
                        freq1.put(node_label, projected);
                    }
                }
            }
        }
        return freq1;
    }

}