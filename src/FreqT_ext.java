/*
find subtrees:
 */

import java.io.*;
import java.util.*;


public class FreqT_ext extends FreqT {
    private  static  char uniChar = '\u00a5';// Japanese Yen symbol
    private static Config config;
    private AOutputFormatter output;
    private Vector <String> largestPattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    private Map <String,Vector<String> > grammar     = new LinkedHashMap<>();
    private Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    private Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    public Set<String>                  outputFrequentPatterns = new LinkedHashSet<>(); //store patterns for post-processing

    private int largestMinSup;

    public Set<String> getOutputFrequentPatterns(){ return this.outputFrequentPatterns; }

    /**
     * add a pattern into outPatterns which is used for post-process
     * @param pat
     * @param projected
     * @param _outputPatterns
     */
    private void addPattern(Vector<String> pat, Projected projected,
                            Set<String> _outputPatterns){


        if(Pattern.countLeafNode(pat) < 2) return;

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
                String.valueOf(size)+"\t"+
                        patternString;

        _outputPatterns.add(patStr);
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
            Set<String> blackListChildren = Pattern.getChildrenLabels(blackLabels,largestPattern,entry.getKey());
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
            int wsup = rootSupport(entry.getValue());
            if(sup < largestMinSup){
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
                // keep root location and right-most location
                List<Integer> occurrences = projected.getProjectLocation(i).getLocationList().subList(0,1);

                //List<Integer> lines = projected.getProjectLineNr(i);

                String prefix = "";
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? transaction.elementAt(id).elementAt(pos).getNodeChild() :
                            transaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = transaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + transaction.elementAt(id).elementAt(l).getNodeLabel();
                        //String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();

                        Projected tmp;
                        if (candidate.containsKey(item)) {
                            //candidate.get(item).setProjectLocation(id, l); //keep right most position
                            candidate.get(item).addProjectLocation(id, l, occurrences);//keeping all locations
                            //candidate.get(item).addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            //rootId = id, rootPos = ?
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
                            //if (id, rootPos) exists in root ???
                            candidate.get(item).setProjectRootLocation(id, rootPos);//keeping root locations


                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            //tmp.setProjectLocation(id, l); //keep right most position
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            //tmp.addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
                            tmp.setProjectRootLocation(id, rootPos); //keeping root locations

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


    //expand candidate based on grammar
    private void grammarExpand(Map.Entry<String, Projected> entry){
        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project( entry.getValue() );
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(largestPattern, entry.getKey());
            String parentLabel = largestPattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

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
                            if (Pattern.checkRepeatedLabel(largestPattern, entry.getKey(), 5))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                    project(entry.getValue());
                                else{//output the current pattern
                                    addPattern(largestPattern,entry.getValue(),outputFrequentPatterns);
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
                        Vector<String> listOfChildrenPattern = Pattern.findChildren(largestPattern, parentPos);
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
    public void expandCandidate(Map.Entry<String, Projected> entry) {
        try{

            // add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    largestPattern.addElement(p[i]);
            }

            if(!Pattern.checkMissedLeafNode(largestPattern))
                grammarExpand(entry);

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
            Map<String, Projected> candidates = generateCandidates(projected);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates);
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(candidates);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                addPattern(largestPattern, projected, outputFrequentPatterns);
                //System.out.println(largestPattern);
                return;
            }

            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();

                Map.Entry<String, Projected> entry = iter.next();

                expandCandidate(entry);

                largestPattern.setSize(oldSize);
            }
        }catch (Exception e){System.out.println("Error: projected " + e);}
    }

    public void run(Map <String, String > _rootIDs,
                              Vector <Vector<NodeFreqT> > _transaction,
                              Map <String,Vector<String> > _grammar,
                              Map <String,Vector<String> > _blackLabels,
                              Map <String,Vector<String> > _whiteLabels){

        transaction = _transaction;
        grammar = _grammar;
        blackLabels = _blackLabels;
        whiteLabels = _whiteLabels;

        System.out.println("the largest patterns according to rootIDs");

        largestPattern = new Vector<>();
        Iterator < Map.Entry<String,String> > rootId = _rootIDs.entrySet().iterator();
        while(rootId.hasNext()){
            Map.Entry<String,String> entry = rootId.next();

            System.out.println(entry.getKey()+"\t"+entry.getValue());

            String rootLabel = entry.getValue().split(String.valueOf("\\("))[1];
            //System.out.println(root);
            largestPattern.add(rootLabel);

            Projected projected = new Projected();
            projected.setProjectedDepth(0);
            //set locations for projected
            String[] temp = entry.getKey().split(";");
            for(int i=0; i<temp.length; ++i){
                String[] pos = temp[i].split(",");
                projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
            }

            largestMinSup = support(projected);
            project(projected);
            largestPattern.setSize(largestPattern.size() - 1);

        }

    }





}