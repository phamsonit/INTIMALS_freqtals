/*
find subtrees:
 */

import java.io.*;
import java.util.*;


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
    private Set<String>                  outputPatterns = new LinkedHashSet<>(); //store patterns for post-processing

    ///////Implementations//////

    public Set<String> getOutputPatterns(){ return this.outputPatterns; }

    public Map <String,Vector<String> > getGrammar(){ return this.grammar;}

    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}


    //procedures relate to patterns//////////////
    /**
     * add a pattern into outPatterns
     * @param pat
     * @param projected
     */
    private void addPattern(Vector<String> pat, Projected projected){
        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectLocationSize();
        int size = Pattern.getPatternSize(pat);

        String patStr = String.valueOf(support)+" "+
                String.valueOf(wsupport)+" "+
                String.valueOf(size)+"\t"+
                Pattern.getPatternString1(pat);

        //System.out.println(patStr);

        outputPatterns.add(patStr);

    }

    /**
     * check combination of constraints
     * @param pat
     * @return
     */
    public boolean checkConstraints(Vector<String> pat) {
        boolean result = false;
        if( Pattern.getPatternSize(pat) >= config.getMaxPatternSize() ) result = true;
        if( Pattern.countLeafNode(pat)  >  config.getMaxLeaf() ) result = true;
        if( Pattern.checkMissedLeafNode(pat) ) result = true;
        if( Pattern.checkNumberLabel(pat,config.getMaxTimeLabel()) ) result = true;

        return result;
    }

    /**
     * prune candidates based on blacklist children
     * blacklist is created in the readWhiteLabel procedure
     * @param candidate
     */
    private void pruneBlackList(Map <String, Projected > candidate){
        Iterator < Map.Entry<String,Projected> > iterTemp = candidate.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, Projected> entry = iterTemp.next();
            Set<String> blackListChildren = Pattern.getChildrenLabels(blackLabels,pattern,entry.getKey());
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
     * prune candidates based on minimal support
     * @param candidate
     */
    private void prune (Map <String, Projected > candidate){

        Iterator < Map.Entry<String,Projected> > iter = candidate.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            int sup = support(entry.getValue());
            //int sup = entry.getValue().getProjectedSupport();
            if(sup < config.getMinSupport()){
                iter.remove();
            }
            else entry.getValue().setProjectedSupport(sup);
        }
    }


    ////Procedures to expand pattern ///////

    /**
     *right most path extension
     * @param projected
     * @return
     */
    public Map<String, Projected> generateCandidates(Projected projected) {
        Map<String, Projected> candidate = new LinkedHashMap<>(); //keep the order of elements
        try{
            // Find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                // Add to keep all occurrences --> problem: memory consumption
                List<Integer> occurrences = projected.getProjectLocation(i).getLocationList();
                List<Integer> lines = projected.getProjectLineNr(i);
                //List<Integer> rootOccurrences = projected.getProjectRootLocation(i).getLocationList();

                //System.out.println(rootOccurrences+" "+occurrences);

                String prefix = "";
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? transaction.elementAt(id).elementAt(pos).getNodeChild() :
                            transaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = transaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + transaction.elementAt(id).elementAt(l).getNodeLabel();
                        String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();

                        Projected tmp;
                        if (candidate.containsKey(item)) {
                            //candidate.get(item).setProjectLocation(id, l);
                            candidate.get(item).addProjectLocation(id, l, occurrences);//keeping all locations
                            candidate.get(item).addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            //find root locations
                            //rootId = id, rootPos = ?
                            //candidate.get(item).addProjectRootLocation(id, rootPos, rootOccurrences);//keeping all rootlocations
                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            //tmp.setProjectLocation(id, l);
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            tmp.addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            //find root locations
                            //tmp.addProjectRootLocation(id, rootPos, rootOccurrences); //keeping all rootlocations
                            candidate.put(item, tmp);
                        }
                    }
                    if (d != -1) {
                        pos = transaction.elementAt(id).elementAt(pos).getNodeParent();
                    }
                    prefix += uniChar + ")";
                }
            }

        }
        catch (Exception e){System.out.println("Error: generate candidates " + e);}
        return candidate;
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

    /**
     * expand a pattern
     * @param entry
     */
    public void expandCandidate(Map.Entry<String, Projected> entry) {
        try{
            // add new candidate to current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    pattern.addElement(p[i]);
            }
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
                                    //check line distance of repeated labels
                                    if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getLineDistance()))
                                        project(entry.getValue()); //new project for unordered nodes
                                else{//output the current pattern
                                    if(Pattern.getPatternSize(pattern)>config.getMinPatternSize())
                                        if( config.postProcess() )
                                            addPattern(pattern,entry.getValue());
                                        else output.report(pattern,entry.getValue());
                                }
                            }else
                            if(parentOrdered.equals("ordered")){
                                project(entry.getValue());
                            }
                            break;

                        default: //AST node has fixed N children
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
                            //expand this candidate if it doesn't miss the mandatory previous sibling
                            if (! Pattern.checkMissMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                                project(entry.getValue());
                            }

                            break;
                    }
                }
            }
        }
        catch (Exception e){System.out.println("Error: expand candidate " + e);}
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{
            System.out.println("pattern "+pattern);
            //AOutputFormatter output1 = new LineOutput(config, grammar, xmlCharacters, uniChar);
            //output1.printAllOccurrence(projected);

            if( checkConstraints(pattern) ){
                if(Pattern.getPatternSize(pattern) > config.getMinPatternSize()) {
                    if (config.postProcess())
                        addPattern(pattern, projected);
                    else output.report(pattern, projected);
                    //System.out.println("constraints " + getPatternString(pattern));
                }
                return;
            }

            Map<String, Projected> candidates = generateCandidates(projected);
            System.out.println("all candidates     " + candidates.keySet());
            //pruning relies on support: for each candidate if its support < minsup --> remove
            prune(candidates);
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning relies on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(candidates);
            //System.out.println("after blacklist pruning " + candidates.keySet());
            //pruneWhiteList(candidates);
            //System.out.println("after whitelist pruning " + candidates.keySet());
            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                if(Pattern.getPatternSize(pattern) > config.getMinPatternSize()) {
                    if (config.postProcess())
                        addPattern(pattern, projected);
                    else output.report(pattern, projected);

                    //System.out.println("no candidate " + getPatternString(pattern));
                }
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

    /**
     * run Freqt with file config.properties
     * @param _config
     */
    public void run(Config _config) {
        try{
            config = _config;
            //os = new FileWriter(config.getOutputFile());
            /*  ==============================  */
            System.out.println("==============================");
            System.out.println("running FreqT");
            System.out.println("==============================");

            if(config.buildGrammar()) Initial.initGrammar(config.getInputFiles(),grammar,config.buildGrammar());
            else Initial.initGrammar(config.getGrammarFile(),grammar,config.buildGrammar()) ;

            System.out.println("INPUT GRAMMAR");
            ReadGrammar.printGrammar(grammar);

            Initial.readWhiteLabel(config.getWhiteLabelFile(), grammar, whiteLabels, blackLabels); //read white labels and create black labels
            Initial.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters

            //initDatabase(config.getInputFiles(),grammar,transaction);
            Initial.initDatabase(config.getInputFiles(),grammar,transaction);

            if(!config.postProcess())
                output = config.outputAsXML() ? new XMLOutput(config, grammar, xmlCharacters) :
                                            new LineOutput(config, grammar, xmlCharacters, uniChar);

            //printTransaction(transaction);

            System.out.println("mining frequent subtrees ... ");

            //find 1-subtree
            Map < String , Projected > freq1 = buildFreq1Set(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune 1-subtree
            prune( freq1 );
            //System.out.println("all candidates after pruning " + freq1.keySet());

            //expansion every 1-subtree to find larger subtrees
            pattern = new Vector<>();
            Iterator < Map.Entry<String,Projected> > iter = freq1.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String,Projected> entry = iter.next();
                /*
                //test rootLocations
                for(int i=0; i< entry.getValue().getProjectLocationSize(); ++i) {
                    System.out.println(entry.getValue().getProjectRootLocation(i).getLocationId()+ " "+
                            entry.getValue().getProjectRootLocation(i).getLocationList());
                }
                */

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
            if(!config.postProcess())
                output.close();
            //os.close();
        }
        catch (Exception e) {
            System.out.println("Error: running freqt");
        }

    }

}