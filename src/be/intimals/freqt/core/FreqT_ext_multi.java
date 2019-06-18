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
    //private AOutputFormatter outputMaximalPatterns;

    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    //private Map<String,String> outputMaximalPatternsMap = null;
    //store frequent patterns in the second step
    private Map<String,String> frequentPatterns = null;

    private int nbOutputMaximalPatterns;
    private int roundCount;
    private long timeStart2nd;
    private long timeFor2nd;
    private long timePerGroup;


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

    //filter maximal patterns
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

    //expand a pattern based on grammar
    private void grammarExpand(Vector<String> largestPattern, Map.Entry<String, Projected> entry,
                               long timeStartGroup){

        //get the current candidate label
        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project(largestPattern, entry.getValue(), timeStartGroup);
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(largestPattern, entry.getKey());
            String parentLabel = largestPattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(largestPattern, entry.getValue(),timeStartGroup);
                        break;
                    case "1..*"://node-list
                        project(largestPattern, entry.getValue(),timeStartGroup);
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
                            project(largestPattern, entry.getValue(),timeStartGroup);
                        }else {
                            chooseOutput(largestPattern, entry.getValue());
                            return;
                        }
                        break;
                }
            }
        }
    }

    //expand a pattern WITHOUT max size constraint
    private void expandCandidate(Vector<String> largestPattern, Map.Entry<String, Projected> entry, long timeStartGroup) {
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
                grammarExpand(largestPattern, entry, timeStartGroup);
        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }

    //main step to find candidates and expand a pattern
    private void project(Vector<String> largestPattern, Projected projected,long timeStartGroup) {
        try{
            //check timeout for the current group
            long diff = System.currentTimeMillis( ) - timeStartGroup;
            if( diff  > timePerGroup) { //keep the current pattern for next round
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
                expandCandidate(largestPattern, entry, timeStartGroup);
                largestPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    //parallel expand groups of root occurrences
    private void groupExpandParallel(Map <String, String > _rootIDs){
        _rootIDs.entrySet().parallelStream().forEach(entry -> {
            //check total running time of the second step
            if(checkTimeOut("groupExpandParallel: 2nd step timeout")) return;
            //start expanding for this group
            long timeStartGroup = System.currentTimeMillis( );
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
            //largestMinSup = projected.getProjectedSupport();
            project(largestPattern,projected,timeStartGroup);
            //update size of the pattern for next expansion
            largestPattern.setSize(largestPattern.size() - 1);

        });
    }

    //main function to find maximal patterns in the second step
    public void run(Map <String, String > _rootIDs, Vector <Vector<NodeFreqT>  > _transaction,
                    long timeFor1st, FileWriter _report){

        try{
            transaction = _transaction;
            frequentPatterns = new ConcurrentHashMap<>();
            roundCount = 1;
            //calculate times for incremental maximal pattern mining
            timeStart2nd = System.currentTimeMillis();
            //time for pattern printing = 10% timeOut and at least 5 minutes !!!
            long time = Math.round(config.getTimeout()*0.1);
            long timeForPrint = time > 20 ? time : 20;//??? minute(s)
            //time for the second step
            timeFor2nd = (config.getTimeout())*(60*1000)- timeFor1st - timeForPrint*60*1000;
            //time spent for each round
            long timeSpent = 0;
            //loop _rootIDs be empty or running time is larger than timeFor2nd.
            while(! _rootIDs.isEmpty()) {
                //store interrupted groups which run over timePerTask
                interruptedRootID = new LinkedHashMap<>();
                //check total running time of the second step
                if(timeFor2nd < timeSpent){
                    _report.write("timeout in the second step");
                    break;
                }
                //recalculate time for each group
                timePerGroup = (timeFor2nd - timeSpent) / _rootIDs.size();
                //PARALLEL RUNNING: for each group of root occurrences find patterns without max size constraints
                groupExpandParallel(_rootIDs);
                //update running time of this round
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }
            //filter and print maximal patterns
            outputMaximalPatterns();

        }catch (Exception e){}
    }

    //filter and print maximal patterns
    private  void outputMaximalPatterns(){
        try{
            //filter maximal patterns
            Map<String,String> maximalPatterns = maximalFilter(frequentPatterns);
            //output maximal patterns
            nbOutputMaximalPatterns = maximalPatterns.size();
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters);
            Iterator < Map.Entry<String,String> > iter1 = maximalPatterns.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<String,String> entry = iter1.next();
                outputMaximalPatterns.printPattern(entry.getValue());
            }
            outputMaximalPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }

    //check running time for the second step
    private boolean checkTimeOut(String message){

        //long currentTime = System.currentTimeMillis( );
        long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
        if(timeFor2nd < currentTimeSpent) {
            //System.out.println(message);
            return true;
        }else
            return false;
    }

}