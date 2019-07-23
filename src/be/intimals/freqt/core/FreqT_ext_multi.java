package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import java.lang.Exception;

/*
    extended FREQT + parallel at the second step
 */

public class FreqT_ext_multi extends FreqT {
    //private AOutputFormatter outputMaximalPatterns;

    //private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();
    //private Map<String,String> outputMaximalPatternsMap = null;
    //store frequent patterns in the second step
    private Map<String,String> frequentPatterns = null;
    //store root occurrences for the second round
    //Map<String,String> interruptedRootID = new LinkedHashMap<>();
    Map<String,String> interruptedRootID = null;


    private int roundCount;
    private long timeStart2nd;
    private long timeFor2nd;
    private long timePerGroup;
    private boolean timeout;

    private int max;



    Vector<Vector<String>> matrixLabels = new Vector<>();


    //////////////////
    public FreqT_ext_multi(Config config,
                           Map<String,Vector<String>> grammar,
                           Map<String,Vector<String>> blackLabels,
                           Map<String,Vector<String>> whiteLabels,
                           Map<String,String> xmlCharacters,
                           Vector <Vector<NodeFreqT> >  transaction) {
        super(config);
        this.grammar = grammar;
        this.blackLabels = blackLabels;
        this.whiteLabels = whiteLabels;
        this.xmlCharacters = xmlCharacters;
        this.transaction = transaction;
    }


    private void addFP(Vector<String> pat, Projected projected){
        if (!checkOutput(pat) && Pattern.getPatternSize(pat) > config.getMinNode()) {
            //System.out.println(pat);
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

    private void createLabelsTransaction(){
        //input pattern p1 = {A,B,C,D}
        //number of columns: map<int,String> index= {[0,A], [1,B], [2,C], [3,D]}

        //create a vector<Bitset> VB = [1 1 1 1]

        //add a new pattern p2 = {A,C,E,F}
        //update index: {[0,A], [1,B], [2,C], [3,D], [4,E], [5,F]}
        //update VB =  [1 1 1 1 0 0]
        //             [1 0 1 0 1 1]

        //add a new pattern p3 = {A,C,C,G}
        //update index: {[0,A], [1,B], [2,C], [3,D], [4,E], [5,F], [6,G]}
        //update VB =  [1 1 1 1 0 0 0]
        //             [1 0 1 0 1 1 0]
        //             [1 0 1 0 0 0 1]

        //problem 1: how to index duplicated labels
    }



    //serial filter maximal patterns by using original FREQT
    private Map<String,String> serialFilterMaximalFP(Map<String,String> FP){
        Map<String,String> MFP = new ConcurrentHashMap<>();
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
                        //check the labels of two subtrees before check maximal subtree
                        if(checkSubsetLabels(fpEntry.getKey(), mfpEntry.getKey())) {
                            switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                                case 1:
                                    found = true;
                                    break;
                                case 2:
                                    mfp.remove();
                                    break;
                            }
                        }
                    }
                    if (!found) {
                        MFP.put(fpEntry.getKey(), fpEntry.getValue());
                        //if(max < MFP.size()) max=MFP.size();
                    }
                }
            }
        }catch (Exception e){System.out.println("Error: Filter maximal pattern");}

        //System.out.println(max);
        return MFP;
    }


    //parallel filter maximal patterns by using pair-wise checking
    private Map<String,String> filterMaximalFP(Map<String,String> FP){

        Map<String,String> MFP = new ConcurrentHashMap<>();
        //parallel filter
        //final int parallelism = config.getNbCores();
        //ForkJoinPool forkJoinPool = null;
        //try{
            //forkJoinPool = new ForkJoinPool(parallelism);
            //forkJoinPool.submit(()->
               FP.entrySet().parallelStream().forEach(fpEntry-> {
                   boolean found = false;
                   max=MFP.size();
                   if (MFP.isEmpty()) {
                       MFP.put(fpEntry.getKey(), fpEntry.getValue());
                   } else {
                       //check the maximality in MFP list ?
                       Iterator<Map.Entry<String, String>> mfp = MFP.entrySet().iterator();
                       while (mfp.hasNext()) {
                           Map.Entry<String, String> mfpEntry = mfp.next();
                           //check labels of pat is subset of labels of mfpEntry
                           if(checkSubsetLabels(fpEntry.getKey(), mfpEntry.getKey())) {
                               switch (checkSubTree(fpEntry.getKey(), mfpEntry.getKey())) {
                                   case 1:
                                       found = true;
                                       break;
                                   case 2:
                                       mfp.remove();
                                       break;
                               }
                           }
                       }
                       if (!found) {
                           MFP.put(fpEntry.getKey(), fpEntry.getValue());
                           //if(max < MFP.size()) max=MFP.size();
                       }
                   }

               });
    //        ).get();
    //    }catch(Exception e){
    //        e.printStackTrace();
    //    }finally {
    //        if (forkJoinPool != null) {
    //            forkJoinPool.shutdown(); //always remember to shutdown the pool
    //        }
    //    }
        //System.out.println(max);
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
                            addFP(largestPattern, entry.getValue());
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
                addFP(largestPattern,entry.getValue());
                return;
            }
            else
                grammarExpand(largestPattern, entry, timeStartGroup);
        }catch (Exception e){System.out.println("Error: Freqt_ext expand candidate " + e);}
    }

    //main step to find candidates and expand a pattern
    private void project(Vector<String> largestPattern, Projected projected, long timeStartGroup) {
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
                addFP(largestPattern,projected);
                return;
            }
            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();
                Map.Entry<String, Projected> entry = iter.next();
                //TODO: need update for parallel version
                //delete candidate that belongs to black-section
//                String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
//                if(candidateLabel.equals("SectionStatementBlock"))
//                    deleteSection(entry, transaction);
//                    //if(checkBlackSection(entry,transaction)) return;
                //expand the pattern if all paragraphs are continuous
//                if(candidateLabel.equals("ParagraphStatementBlock")) {
//                    checkContinuousParagraph(largestPattern, entry, transaction);
//                }

                expandCandidate(largestPattern, entry, timeStartGroup);
                largestPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext_multi projected " + e);
            e.printStackTrace();
        }
    }

    //parallel expand groups of root occurrences
    private void expandGroupParallel(Map <String, String > _rootIDs) {
        //final int parallelism = config.getNbCores();
        //ForkJoinPool forkJoinPool = null;
        //try{
            //forkJoinPool = new ForkJoinPool(parallelism);
            //forkJoinPool.submit(()->
                _rootIDs.entrySet().parallelStream().forEach(entry -> {
                    //check total running time of the second step
                    if (checkTimeOut()) {
                        //System.out.println("project: Timeout at the second step: "+System.currentTimeMillis());
                        timeout = true;
                        return;
                    }
                    //start expanding for this group
                    long timeStartGroup = System.currentTimeMillis();
                    //boolean finished = true;
                    //System.out.println("Group "+entry.getKey());
                    //System.out.println(frequentPatterns.size()+" - "+ nbOutputMaximalPatterns);
                    Vector<String> largestPattern = new Vector<>();
                    Projected projected = new Projected();
                    if (roundCount == 1) {
                        String[] tmp = entry.getValue().substring(1, entry.getValue().length() - 1).split(String.valueOf(","));
                        String rootLabel = tmp[0];
                        largestPattern.add(rootLabel);
                        projected.setProjectedDepth(0);
                        //calculate the root positions
                        String[] temp = entry.getKey().split(";");
                        for (int i = 0; i < temp.length; ++i) {
                            String[] pos = temp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                        }
                    } else {
                        //from the second round, expanding from the patterns which interrupted in the previous round
                        String[] tmp = entry.getValue().substring(1, entry.getValue().length() - 1).split(String.valueOf(","));
                        largestPattern = Pattern.formatPattern(tmp);
                        //print to test locations of interrupted pattern
                        //System.out.println(entry.getKey());
                        String[] projectTemp = entry.getKey().split("\t");
                        projected.setProjectedDepth(Integer.valueOf(projectTemp[0]));
                        //calculate root and right-most positions
                        String[] rootTemp = projectTemp[1].split(";");
                        for (int i = 0; i < rootTemp.length; ++i) {
                            String[] pos = rootTemp[i].split("-");
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            ////location = (id,[root pos, rightmost pos])
                            //projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            //projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[2]));
                        }
                        String[] rightmostTemp = projectTemp[2].split(";");
                        for (int i = 0; i < rightmostTemp.length; ++i) {
                            String[] pos = rightmostTemp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                        }
                    }
                    //largestMinSup = projected.getProjectedSupport();
                    project(largestPattern, projected, timeStartGroup);
                    //update size of the pattern for next expansion
                    largestPattern.setSize(largestPattern.size() - 1);
                });

    //            })
    //        ).get();
    //    }catch(Exception e){
    //        e.printStackTrace();
    //    }finally {
    //        if (forkJoinPool != null) {
    //            forkJoinPool.shutdown(); //always remember to shutdown the pool
    //        }
    //    }
    }

    //main function to find maximal patterns in the second step
    public void run(Map <String, String > _rootIDs, long start1st, FileWriter _report){
        try{
            frequentPatterns = new ConcurrentHashMap<>();
            timeout = false;
            roundCount = 1;
            //calculate times for incremental maximal pattern mining
            timeStart2nd = System.currentTimeMillis();
            timeFor2nd = (config.getTimeout())*(60*1000);
            //time spent for each round
            long timeSpent = 0;
            //loop _rootIDs be empty or running time is larger than timeFor2nd.
            while(! _rootIDs.isEmpty()) {
                //store interrupted groups which run over timePerTask
                //interruptedRootID = new LinkedHashMap<>();
                interruptedRootID = new ConcurrentHashMap<>();
                //recalculate time for each group
                timePerGroup = (timeFor2nd - timeSpent) / _rootIDs.size();
                //PARALLEL RUNNING: for each group of root occurrences find patterns without max size constraints
                expandGroupParallel(_rootIDs);
                //update running time of this round
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }

            System.out.println("Filtering maximal patterns ...");

            if(timeout)
                log(_report,"\t + timeout in the second step");
            else
                log(_report,"\t + search finished");
            log(_report,"\t + running time: "+ Float.valueOf(System.currentTimeMillis( ) - timeStart2nd)/1000+"s");

            long start = System.currentTimeMillis();
            log(_report,"\t + #FP: "+frequentPatterns.size());
            Map<String,String> maximalPatterns = filterMaximalFP(frequentPatterns);
            //Map<String,String> maximalPatterns = serialFilterMaximalFP(frequentPatterns);
            log(_report,"\t + filtering time: "+Float.valueOf(System.currentTimeMillis() - start)/1000+"s");

            System.out.println("Output maximal patterns ...");
            start = System.currentTimeMillis();

            String outFile = config.getOutputFile();
            outputMFP(maximalPatterns,outFile);

            log(_report,"\t + printing time: "+ Float.valueOf(System.currentTimeMillis()-start)/1000+"s");
            log(_report,"\t + maximal patterns: "+maximalPatterns.size());
            log(_report,"- total running time: "+ Float.valueOf(System.currentTimeMillis()-start1st)/1000+"s");


            _report.flush();
            _report.close();

        }catch (Exception e){}
    }

    //check running time for the second step
    private boolean checkTimeOut(){
        //long currentTime = System.currentTimeMillis( );
        long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
        if(timeFor2nd < currentTimeSpent) {
            return true;
        }else
            return false;
    }

}