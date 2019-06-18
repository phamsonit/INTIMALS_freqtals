package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.*;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FreqT_ext_multi extends FreqT {

    private AOutputFormatter outputMaximalPatterns;

    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    //store frequent patterns in the second step
    private Map<String,String> outputMaximalPatternsMap = null;
    private Map<String,String> frequentPatterns = null;

    private int nbOutputMaximalPatterns;
    //private int largestMinSup; //How to use for parallel computing

    //long start;
    //boolean finished;
    //long timePerGroup;

    int roundCount = 1;

    //store root occurrences for the second round
    //Map<String,String> interruptedRootID = new LinkedHashMap<>();
    Map<String,String> interruptedRootID = new ConcurrentHashMap<>();


    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_ext_multi(Config config,
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

        if (!checkOutput(pat)) {
            //nbOutputMaximalPatterns++;
            int support = projected.getProjectedSupport();
            int wsupport = projected.getProjectedRootSupport(); //=> root location
            int size = Pattern.getPatternSize(pat);

            //replace "," in the leafs by uniChar
            String patString = pat.elementAt(0);
            for (int i = 1; i < pat.size(); ++i)
                patString = patString + "," + pat.elementAt(i);
            //patString = patString + uniChar + pat.elementAt(i);

            String patternSupport =
                    "rootOccurrences" + "," +
                            String.valueOf(support) + "," +
                            String.valueOf(wsupport) + "," +
                            String.valueOf(size) + "\t" +
                            patString;//pat.toString(); //keeping for XML output

            String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes
            frequentPatterns.put(patternString, patternSupport);
        }
    }

    //TODO: keep top-k largest patterns

    //TODO: filter maximal patterns for frequentPatterns
    private Map<String,String> maximalFilter(Map<String,String> FP){
        //outputMaximalPatternsMap
        Map<String,String> MFP = new HashMap<>();
        try{
            Iterator < Map.Entry<String,String> > fp = FP.entrySet().iterator();
            //for each pattern
            while(fp.hasNext()){
                boolean found = false;
                Map.Entry<String, String> fpEntry = fp.next();

                if(MFP.isEmpty()){
                    MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
                else {
                    //check the pattern existing in MFP list ?
                    Iterator<Map.Entry<String, String>> mfp = MFP.entrySet().iterator();
                    while (mfp.hasNext()) {
                        Map.Entry<String, String> mfpEntry = mfp.next();
                        switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                            case 1:
                                found = true;
                                break;
                            case 2:
                                mfp.remove();
                                break;
                        }
                    }
                    if (!found) {
                        MFP.put(fpEntry.getKey(), fpEntry.getValue());
                    }
                }
            }
        }
        catch (Exception e){System.out.println("Error: Filter maximal pattern");}

        return MFP;

    }

    //the difference of this function is chooseOutput
    private void grammarExpand(Vector<String> largestPattern, Map.Entry<String, Projected> entry,
                               long timePerGroup, long start){
        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project(largestPattern, entry.getValue(),timePerGroup,start);
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(largestPattern, entry.getKey());
            String parentLabel = largestPattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(largestPattern, entry.getValue(),timePerGroup,start);
                        break;
                    case "1..*"://node-list
                        project(largestPattern, entry.getValue(),timePerGroup,start);

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
                            project(largestPattern, entry.getValue(),timePerGroup,start);
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
    private void expandCandidate(Vector<String> largestPattern, Map.Entry<String, Projected> entry,
                                 long timePerGroup, long start) {
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
                grammarExpand(largestPattern, entry,timePerGroup,start);

        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }

    /**
     * expand a subtree
     * @param projected
     */



    //private void project(Vector<String> largestPattern, Projected projected) {
    private void project(Vector<String> largestPattern, Projected projected,
                         long timePerGroup, long start) {
        try{
            //check timeout for the current group
            long end = System.currentTimeMillis( );
            long diff = end-start;
            if( diff  > timePerGroup) {
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
                chooseOutput(largestPattern,projected);
                return;
            }
            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();
                Map.Entry<String, Projected> entry = iter.next();

                //delete candidate that belongs to black-section
                String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
                if(candidateLabel.equals("SectionStatementBlock"))
                    deleteSection(entry,transaction);

                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock")) {
                    checkContinuousParagraph(largestPattern, entry, transaction);
                }
                expandCandidate(largestPattern, entry,timePerGroup,start);

                largestPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    private void groupExpandParallel(Map <String, String > _rootIDs,
                                     long timePerGroup, long timeStartRound, long timeFor2nd){

        _rootIDs.entrySet().parallelStream().forEach(entry -> {

            //calculate time to stop group expansion
            long timeStartGroup = System.currentTimeMillis( );
            final long currentTimeSpent = (timeStartGroup - timeStartRound);
            //TODO: how to completely exit recursion ?
            if(timeFor2nd < currentTimeSpent) {
                //System.out.println("2nd step timeout");
                return;
            }

            //boolean finished = true;
            //System.out.println("Group "+entry.getKey());
            //System.out.println(frequentPatterns.size()+" - "+ nbOutputMaximalPatterns);
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
            //int largestMinSup = projected.getProjectedSupport();
            project(largestPattern,projected,timePerGroup,timeStartGroup);
            //update size of the pattern for next expansion
            largestPattern.setSize(largestPattern.size() - 1);

        });
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
            frequentPatterns = new ConcurrentHashMap<>();
            //frequentPatterns = Collections.synchronizedMap(new HashMap<>());

            //calculate times for incremental maximal pattern mining
            long timeStartRound = System.currentTimeMillis();
            long timeEndRound  = timeStartRound;
            //time for pattern printing = 10% timeOut and at least 5 minutes !!!
            long time = Math.round(config.getTimeout()*0.1);
            long timeForPrint = time > 5 ? time : 5;//??? minute(s)
            long timeFor2nd = (config.getTimeout())*(60*1000)- timeFor1st - timeForPrint*60*1000;
            long timeSpent = 0;

            while(! _rootIDs.isEmpty()) {
                //store interrupted groups which run over timePerTask
                interruptedRootID = new LinkedHashMap<>();
                //check necessary time for next round
                if(timeFor2nd < timeSpent) break;

                //recalculate time for each group
                long timePerGroup = (timeFor2nd - timeSpent) / _rootIDs.size();

                //PARALLEL RUNNING: for each group of root occurrences find patterns without max size constraints
                groupExpandParallel(_rootIDs,timePerGroup,timeStartRound,timeFor2nd);

                //update running time of this round
                timeEndRound = System.currentTimeMillis();
                timeSpent = (timeEndRound - timeStartRound);

                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }
            //filter and print maximal patterns
            outputMaximalPatterns();

        }catch (Exception e){}
    }

    private  void outputMaximalPatterns(){

        try{
            //filter maximal patterns
            Map<String,String> maximalPatterns = maximalFilter(frequentPatterns);
            //output maximal patterns
            nbOutputMaximalPatterns = maximalPatterns.size();
            outputMaximalPatterns =  new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters);
            Iterator < Map.Entry<String,String> > iter1 = maximalPatterns.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                outputMaximalPatterns.printPattern(entry.getValue());
            }
            outputMaximalPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }


}