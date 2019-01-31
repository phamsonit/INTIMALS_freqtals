package be.intimals.freqt.core;

import be.intimals.freqt.structure.*;

import java.io.*;
import java.util.*;

import be.intimals.freqt.output.*;

public class FreqT_ext extends FreqT {

    private AOutputFormatter output;
    private Vector <String> largestPattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();



    private Map<String,String> outputLargestPatternsMap = new LinkedHashMap<>();

    private int nbOutputLargestPatterns;
    private int largestMinSup;
    ////////////////////////////////////////////////////////////////////////////////

    public int getNbOutputLargestPatterns(){
        return this.nbOutputLargestPatterns;
    }

    public Map<String,String> getOutputLargestPatterns(){ return this.outputLargestPatternsMap; }

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
                            if (Pattern.checkRepeatedLabel(largestPattern, entry.getKey(), config.getMaxRepeatLabel()))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                    project(entry.getValue());
                                else{//output the current pattern
                                    if(config.postProcess())
                                        addPattern(largestPattern,entry.getValue(),outputLargestPatternsMap);
                                    else
                                        output.report(largestPattern,entry.getValue());
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
    private void expandCandidate(Map.Entry<String, Projected> entry) {
        try{

            // add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    largestPattern.addElement(p[i]);
            }
            //TODO: don't check full leaf constraint ==> result: time consuming!!!
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

            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,largestMinSup);
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(largestPattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                if(config.postProcess())
                    addPattern(largestPattern,projected,outputLargestPatternsMap);
                else
                    output.report(largestPattern,projected);
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
                              //Config _config,
                              Vector <Vector<NodeFreqT>  > _transaction
                              //Map <String,Vector<String> > _grammar,
                              //Map <String,Vector<String> > _blackLabels,
                              //Map <String,Vector<String> > _whiteLabels,
                              //Map <String,String>          _xmlCharacters
                    ){

        //config = _config;
        transaction = _transaction;
        //grammar = _grammar;
        //blackLabels = _blackLabels;
        //whiteLabels = _whiteLabels;
        //xmlCharacters = _xmlCharacters;

        try{
            if(!config.postProcess()) output = new XMLOutput(config, grammar, xmlCharacters);

            largestPattern = new Vector<>();
            Iterator < Map.Entry<String,String> > rootId = _rootIDs.entrySet().iterator();
            while(rootId.hasNext()){
                Map.Entry<String,String> entry = rootId.next();
                //System.out.println(entry.getKey()+"\t"+entry.getValue());
                //String rootLabel = entry.getValue().split(String.valueOf("\\("))[1];
                String rootLabel = entry.getValue();
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

            if(! config.postProcess()) {
                nbOutputLargestPatterns = output.getNbPattern();
                output.close();
            }else nbOutputLargestPatterns = outputLargestPatternsMap.size();

        }catch (Exception e){}


    }

}