package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;


import be.intimals.freqt.output.*;

public class FreqT_ext extends FreqT {

    //private AOutputFormatter outputMaximalPatterns;

    //private Vector <String> _largestPattern;

    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();

    private Map<String,String> outputMaximalPatternsMap = new HashMap<>();


    private int nbOutputMaximalPatterns;

    //private int largestMinSup; //How to use for parallel computing

    long start;
    long timePerGroup;
    boolean finished;
    long timeStartRound;

    //int roundCount = 1;

    //store root occurrences for the second round
    Map<String,String> interruptedRootID = new LinkedHashMap<>();


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


    //the difference of this function is chooseOutput
    private void grammarExpand(Vector<String> largestPattern, Map.Entry<String, Projected> entry){
        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project(largestPattern, entry.getValue());
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(largestPattern, entry.getKey());
            String parentLabel = largestPattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(largestPattern, entry.getValue());
                        break;
                    case "1..*"://node-list
                        project(largestPattern, entry.getValue());

                        break;

                    default: //AST node has fixed N children
                        //project(entry.getValue());
                        //find all children of parentPos in the grammar
                        Vector<String> listOfChildrenGrammar = new Vector<>();
                        listOfChildrenGrammar.addAll(grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));
                        //find all children of parentPos in the pattern
                        Vector<String> listOfChildrenPattern = Pattern.findChildrenLabels(largestPattern, parentPos);
                        //find white labels and black labels
                        Set<String> blackLabelChildren = new LinkedHashSet<>();
                        Set<String> whiteLabelChildren = new LinkedHashSet<>();
                        if(whiteLabels.containsKey(parentLabel))
                            whiteLabelChildren.addAll(whiteLabels.get(parentLabel));
                        if(blackLabels.containsKey(parentLabel))
                            blackLabelChildren.addAll(blackLabels.get(parentLabel));
                        //expand this candidate if it doesn't miss the previous mandatory sibling
                        if (! Pattern.isMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(largestPattern, entry.getValue());
                        }else {
                            chooseOutput(largestPattern, entry.getValue(),outputMaximalPatternsMap,false);
                            return;
                        }

                        break;
                }
            }
        }
    }


    private void expandCandidate(Vector<String> largestPattern, Map.Entry<String, Projected> entry) {
        try{

            // add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    largestPattern.addElement(p[i]);
            }
            //DON'T USE SIZE CONSTRAINT
            if(Pattern.isMissedLeafNode(largestPattern)) {
                chooseOutput(largestPattern, entry.getValue(), outputMaximalPatternsMap,false);
                return;
            }
            else
                grammarExpand(largestPattern, entry);

        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Vector<String> largestPattern, Projected projected) {
        try{

            //check timeout for the current group
            long end = System.currentTimeMillis( );
            long diff = end-start;
            if( diff  > timePerGroup) {
                //finished = false;
                //keep the depth of projector
                String rootOccurrences = String.valueOf(projected.getProjectedDepth())+"\t";
                //keep root occurrences and right-most occurrences
                for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                    rootOccurrences = rootOccurrences +
                            Location.getLocationId(projected.getProjectRootLocation(i)) + ("-") +
                            Location.getLocationPos(projected.getProjectRootLocation(i)) + ";";
                    //Location.getLocationPos(projected.getProjectLocation(i)) + ";";

                }

                String rightmostOccurrences="";
                for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                    rightmostOccurrences = rightmostOccurrences +
                            Location.getLocationId(projected.getProjectLocation(i)) + ("-") +
                            Location.getLocationPos(projected.getProjectLocation(i)) + ";";

                }
                rootOccurrences = rootOccurrences+"\t"+rightmostOccurrences;

                //store the current pattern for the next round
                //System.out.println((rootOccurrences+"--"+largestPattern.toString()));
                interruptedRootID.put(rootOccurrences,largestPattern.toString());

                return;
            }
            //System.out.println(largestPattern);
            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(largestPattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());
            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                chooseOutput(largestPattern,projected,outputMaximalPatternsMap,false);
                return;
            }
            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();
                Map.Entry<String, Projected> entry = iter.next();

                //constraints for mining COBOL
                //delete candidate that belongs to black-section
                String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
                if(candidateLabel.equals("SectionStatementBlock"))
                    deleteSection(entry,transaction);
                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock")) {
                    checkContinuousParagraph(largestPattern, entry, transaction);
                }

                expandCandidate(largestPattern, entry);

                largestPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    public void run(Map <String, String > _rootIDs,
                    Vector <Vector<NodeFreqT>  > _transaction,
                    long start1st,
                    FileWriter _report){

        try{
            transaction = _transaction;
            //calculate times for incremental maximal pattern mining
            int roundCount = 1;
            timeStartRound = System.currentTimeMillis();
            long timeEndRound  = System.currentTimeMillis();
            //time for pattern printing = 10%timeOut and at least 1 minute
            //long time = Math.round(config.getTimeout()*0.1);
            //long timeForPrint = time > 5 ? time : 5;//??? minute(s)
            //long timeFor2nd = (config.getTimeout())*(60*1000)- timeFor1st - timeForPrint*60*1000;
            long timeFor2nd = (config.getTimeout())*(60*1000);
            long timeSpent = 0;

            //System.out.println("number of root occurrences "+_rootIDs.size());
            //incremental mining
            while(! _rootIDs.isEmpty()){
                //store interrupted groups which run over timePerTask
                interruptedRootID = new LinkedHashMap<>();
                //calculate time for each group in the current round
                //log(_report,"===================");
                //log(_report,"ROUND: "+ roundCount);
                //output to check each round
                //System.out.println("ROUND: "+ roundCount);
                //System.out.println("groups: "+ _rootIDs.size());
                //System.out.println("#patterns: "+ outputMaximalPatternsMap.size());

                //log(_report,"- nbGroups = "+ _rootIDs.size());
                timePerGroup = (timeFor2nd - timeSpent) / _rootIDs.size() ;
                //log(_report,"- timePerGroup = "+ timePerGroup +" ms");
                //log(_report,"===================");
                //for each group of root occurrences find patterns without max size constraints
                //int groupCount = 1;

                Iterator < Map.Entry<String,String> > rootId = _rootIDs.entrySet().iterator();
                while(rootId.hasNext()){
                    //start expanding a group
                    start = System.currentTimeMillis( );
                    finished = true;
                    if(timeFor2nd < timeSpent){ //long timeRemaining = timeFor2nd - timeSpent;
                        //log(_report,"the second step timeout");
                        break;
                    }
                    //log(_report,"- Group: "+groupCount);
                    Map.Entry<String,String> entry = rootId.next();

                    Vector<String> largestPattern = new Vector<>();
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
                        //from the second round, expanding from the patterns which interrupted in the previous round
                        String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                        largestPattern = Pattern.formatPattern(tmp);
                        //print to test locations of interrupted pattern
                        //System.out.println(entry.getKey());
                        String[] projectTemp = entry.getKey().split("\t");
                        projected.setProjectedDepth(Integer.valueOf(projectTemp[0]));
                        //calculate root and right-most positions
                        String[] rootTemp = projectTemp[1].split(";");
                        for(int i=0; i<rootTemp.length; ++i) {
                            String[] pos = rootTemp[i].split("-");
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            ////location = (id,[root pos, rightmost pos])
                            //projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            //projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[2]));
                        }
                        String[] rightmostTemp = projectTemp[2].split(";");
                        for(int i=0; i<rightmostTemp.length; ++i) {
                            String[] pos = rightmostTemp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                        }
                    }
                    //largestMinSup = support(projected);
                    project(largestPattern, projected);
//                    //report for each group
//                    long end = System.currentTimeMillis();
//                    long diff = end - start;
//                    if(finished) {
//                        log(_report,"+ Finish : " + diff);
//                    }
//                    else {
//                        log(_report,"+ Stop : " + diff);
//                    }
//                    //report number of maximal patterns
//                    log(_report,"+ Maximal patterns: " + outputMaximalPatternsMap.size());

                    //update size of the pattern for next expansion
                    largestPattern.setSize(largestPattern.size() - 1);
                    //
                    //groupCount++;

                }
                //update running time
                timeEndRound = System.currentTimeMillis();
                timeSpent = (timeEndRound - timeStartRound);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }

            //print maximal patterns
            outputMaximalPatterns(outputMaximalPatternsMap);

            //report result
            log(_report,"\t + maximal patterns: "+ outputMaximalPatternsMap.size());
            long currentTimeSpent = (System.currentTimeMillis( ) - timeStartRound);
            log(_report, "\t + running time: ..."+Float.valueOf(currentTimeSpent)/1000+"s");
            log(_report,"- total running time "+Float.valueOf(System.currentTimeMillis( )-start1st)/1000+"s");
            _report.flush();
            _report.close();

        }catch (Exception e){}
    }

}