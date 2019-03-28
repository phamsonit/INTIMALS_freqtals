package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;


import be.intimals.freqt.output.*;

public class FreqT_ext extends FreqT {

    private AOutputFormatter outputMaximalPatterns;

    private Vector <String> largestPattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    private Map<String,String> outputMaximalPatternsMap = new HashMap<>();


    private int nbOutputMaximalPatterns;
    private int largestMinSup;

    long start;
    boolean finished;
    long timePerGroup;


    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_ext(Config config,
                     Map<String,Vector<String>> grammar,
                     Map<String,Vector<String>> blackLabels,
                     Map<String,Vector<String>> whiteLabels,
                     Map<String,String> xmlCharacters) {
        super(config);
        this.grammar = grammar;
        this.blackLabels = blackLabels;
        this.whiteLabels = whiteLabels;
        this.xmlCharacters = xmlCharacters;
    }

    public int getNbOutputMaximalPatterns(){
        return this.nbOutputMaximalPatterns;
    }



    private void chooseOutput(Vector<String> pat, Projected projected){

        if (checkOutput(pat))
            return;
        else
            addMaximality (pat,projected,outputMaximalPatternsMap);
    }



    //the difference of this function is chooseOutput
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
                        //project(entry.getValue());
                        if(parentOrdered.equals("unordered")) {
                            //don't allow N children of an unordered node to have the same label
                            if (Pattern.isRepeatedLabel(largestPattern, entry.getKey(), config.getMaxRepeatLabel()))
                                //check line distance of 2 nodes which have the same label
                                //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                project(entry.getValue());
                                else{//output the current pattern
                                    chooseOutput(largestPattern,entry.getValue());
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
                        if (! Pattern.isMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(entry.getValue());
                        }else {
                            chooseOutput(largestPattern, entry.getValue());
                            return;
                        }

                        break;
                }
            }
        }
    }

    /**
     * expand a pattern WITHOUT max size constraint
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
            //DON'T USE SIZE CONSTRAINT
            if(Pattern.isMissedLeafNode(largestPattern)) {
                chooseOutput(largestPattern,entry.getValue());
                return;
            }
            else
                grammarExpand(entry);

        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            //check timeout for the current group
            long end = System.currentTimeMillis( );
            long diff = end-start;
            if( diff  > timePerGroup) {
                finished = false;
                return;
            }
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
                chooseOutput(largestPattern,projected);
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
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    /**
     * expanding each group of root occurrences to find largest pattern
     * @param _rootIDs
     * @param _transaction
     * @param timeFor1st
     * @param _report
     */
    public void run(Map <String, String > _rootIDs,
                    Vector <Vector<NodeFreqT>  > _transaction,
                    long timeFor1st,
                    FileWriter _report){

        try{
            transaction = _transaction;
            //calculate times for incremental maximal pattern mining
            int roundCount = 1;
            long timeStartRound = System.currentTimeMillis();
            long timeEndRound  = System.currentTimeMillis();
            //time for pattern printing = 10%timeOut and at least 1 minute
            long time = Math.round(config.getTimeout()*0.1);
            long timeForPrint = time > 1 ? time : 1;//??? minute(s)
            long timeFor2nd = (config.getTimeout())*(60*1000)- timeFor1st - timeForPrint*60*1000;
            long timeSpent = 0;

            while(! _rootIDs.isEmpty()){
                //store groups which run over timePerTask
                Map<String,String> rootIDTemp = new LinkedHashMap<>();
                //calculate time for each group in the current round
                log(_report,"===================");
                log(_report,"ROUND: "+ roundCount);
                log(_report,"- nbGroups = "+ _rootIDs.size());
                timePerGroup = (timeFor2nd - timeSpent) / _rootIDs.size() ;
                log(_report,"- timePerGroup = "+ timePerGroup +" ms");
                log(_report,"===================");
                //for each group of root occurrences find patterns without size constraints
                int groupCount = 1;
                largestPattern = new Vector<>();
                Iterator < Map.Entry<String,String> > rootId = _rootIDs.entrySet().iterator();
                while(rootId.hasNext()){
                    //start expanding a group
                    start = System.currentTimeMillis( );
                    finished = true;
                    //long timeRemaining = timeFor2nd - timeSpent;
                    if(timeFor2nd - timeSpent <= 0){
                        log(_report,"the second step timeout");
                        break;
                    }
                    log(_report,"- Group: "+groupCount);

                    Map.Entry<String,String> entry = rootId.next();
                    Projected projected = new Projected();
                    if(roundCount == 1) {
                        String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                        String rootLabel = tmp[0];
                        largestPattern.add(rootLabel);
                        projected.setProjectedDepth(0);
                        //calculate the root positions
                        String[] temp = entry.getKey().split(";");
                        for(int i=0; i<temp.length; ++i){
                            String[] pos = temp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                        }
                    }else{
                        //from the second round, expanding from the pattern which stored in the previous round
                        String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                        largestPattern = Pattern.formatPattern(tmp);
                        String[] pro = entry.getKey().split("\t");
                        projected.setProjectedDepth(Integer.valueOf(pro[0])); //depth = ???
                        //calculate root and right-most positions
                        String[] temp = pro[1].split(";");
                        for(int i=0; i<temp.length; ++i){
                            String[] pos = temp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[2]));
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                        }
                    }

                    largestMinSup = support(projected);

                    project(projected);

                    long end = System.currentTimeMillis();
                    long diff = end - start;
                    if(finished) {
                        log(_report,"+ Finish : " + diff);
                        //rootId.remove();
                    }
                    else {
                        log(_report,"+ Stop : " + diff);
                        //keep the depth of projector
                        String rootOccurrences = String.valueOf(projected.getProjectedDepth())+"\t";
                        //keep root and right-most locations
                        for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                            rootOccurrences = rootOccurrences +
                                    Location.getLocationId(projected.getProjectRootLocation(i)) + ("-") +
                                    Location.getLocationPos(projected.getProjectRootLocation(i)) + "-" +
                                    Location.getLocationPos(projected.getProjectLocation(i)) + ";";
                        }
                        //store the current pattern for the next round
                        rootIDTemp.put(rootOccurrences,largestPattern.toString());
                    }
                    //update size of the pattern for next expansion
                    largestPattern.setSize(largestPattern.size() - 1);
                    groupCount++;
                    log(_report,"+ Maximal patterns: " + outputMaximalPatternsMap.size());
                    timeEndRound = System.currentTimeMillis();
                    timeSpent = (timeEndRound - timeStartRound);
                }
                //update this list of groups for the next round
                _rootIDs = rootIDTemp;
                roundCount++;
            }

            //
            nbOutputMaximalPatterns = outputMaximalPatternsMap.size();
            //output maximal patterns
            outputMaximalPatterns =  new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters);
            //System.out.println("outputLargestPatternsMap1 :" + outputLargestPatternsMap1.size()+" ==>");
            Iterator < Map.Entry<String,String> > iter1 = outputMaximalPatternsMap.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                //output XML
                outputMaximalPatterns.printPattern(entry.getValue());
            }
            outputMaximalPatterns.close();

        }catch (Exception e){}

    }


}