package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    extended FREQT + without using max size constraints
 */

public class FreqT_Int_ext_serial extends FreqT_Int {

    //private Map<ArrayList<Integer>,String> MFP = new HashMap<>();
    //private Map<String,ArrayList<Integer>> interruptedRootID = new LinkedHashMap<>();

    private Map<ArrayList<Integer>,String> MFP = new ConcurrentHashMap<>();
    private Map<String,ArrayList<Integer>> interruptedRootID = new ConcurrentHashMap<>();

    private long timeout;
    private long timeStart2nd;
    private long timeSpent;
    private long timePerGroup;
    private long timeStartGroup;
    private boolean finished;

    //store root occurrences for the second round

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_Int_ext_serial(Config _config,
                                Map<String,ArrayList<String>> _grammar,
                                Map<Integer,ArrayList<String>> _grammarInt,
                                Map<Integer,ArrayList<Integer>> _blackLabelsInt,
                                Map<Integer,ArrayList<Integer>> _whiteLabelsInt,
                                Map<String,String> _xmlCharacters,
                                Map<Integer,String> _labelIndex,
                                ArrayList <ArrayList<NodeFreqT> >  _transaction) {
        super(_config);
        this.grammar = _grammar;
        this.grammarInt = _grammarInt;
        this.blackLabelsInt = _blackLabelsInt;
        this.whiteLabelsInt = _whiteLabelsInt;
        this.xmlCharacters = _xmlCharacters;
        this.labelIndex = _labelIndex;
        this.transaction = _transaction;
    }

    private void addPattern(ArrayList<Integer> _largestPattern, Projected projected, Map<ArrayList<Integer>,String> _outputPatterns){

        //remove the part of the pattern missing leaf
        ArrayList<Integer> patTemp = Pattern_Int.getPatternString1(_largestPattern);
        //check right mandatory children before adding pattern
        if(checkRightObligatoryChildren(patTemp, grammarInt, blackLabelsInt)) return;

        if(config.getFilter())
            addMFP(patTemp, projected, _outputPatterns);
        else
            addFP(patTemp, projected, _outputPatterns);
    }

    private void project(ArrayList<Integer> largestPattern, Projected projected) {
        try{

            timeSpent = System.currentTimeMillis() - timeStart2nd;
            if(timeSpent > timeout){ //long timeRemaining = timeFor2nd - timeSpent;
                finished = false;
                return;
            }
            //check timeout for the current group
            long timeGroupSpent = System.currentTimeMillis( ) - timeStartGroup;
            if( timeGroupSpent  > timePerGroup) {
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
                interruptedRootID.put(rootOccurrences,largestPattern);
                return;
            }
            //System.out.println(largestPattern);
            //find candidates
            Map<ArrayList<Integer>, Projected> candidates = generateCandidates(projected,transaction);
            /*
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(largestPattern,candidates,blackLabelsInt);
            //System.out.println("after blacklist pruning " + candidates.keySet());
            */
            //prune on minimum support and list of black labels
            pruneSupportAndBlacklist(candidates,config.getMinSupport(),largestPattern,blackLabelsInt);
            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                addPattern(largestPattern,projected,MFP);
                return;
            }
            /*
            //keep k candidates
            if (candidates.size() > config.getBeamSize()) {
                candidates = beam(candidates, config.getBeamSize());
                //System.out.println("candidates in beam " + candidates.keySet());
            }
            */
            //randomly select 1 candidate
//            if(candidates.size() > 1) {
//                candidates = randomBeam(candidates);
//                //System.out.println("random candidate in beam " + candidates.keySet());
//            }

            //expand the current pattern with each candidate
            Iterator < Map.Entry<ArrayList<Integer>,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();
                Map.Entry<ArrayList<Integer>, Projected> entry = iter.next();
                largestPattern.addAll(entry.getKey());
                //check continuous paragraphs
                //if potential candidate = SectionStatementBlock then check if candidate belongs to black-section or not
                String candidateLabel = labelIndex.get(entry.getKey().get(entry.getKey().size()-1));
                if(candidateLabel.equals("SectionStatementBlock"))
                    checkBlackSection(entry,transaction);
                //expand the pattern if all paragraphs are continuous
                if(candidateLabel.equals("ParagraphStatementBlock")) {
                    checkContinuousParagraph(largestPattern, entry, transaction);
                }
                //don't check maximal size constraint ....
                //constraint on real leaf node
                //constraint on obligatory children
                if(checkLeftObligatoryChild(largestPattern, entry.getKey(), grammarInt, blackLabelsInt)){
                    //do nothing = don't add pattern to MFP
                }else{
                    if( Pattern_Int.checkMissingLeaf(largestPattern) ){
                     //       || checkLeftObligatoryChild(largestPattern, entry.getKey(), grammarInt, blackLabelsInt)){
                        addPattern(largestPattern,entry.getValue(),MFP);
                    }else{
                        project(largestPattern, entry.getValue());
                    }
                }

                largestPattern = new ArrayList<>(largestPattern.subList(0,oldSize));
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }


    public void run(Map <String, ArrayList<Integer> > _rootIDs, long start1st, FileWriter _report){
        try{
            //calculate times for incremental maximal pattern mining
            int roundCount = 1;
            finished = true;
            timeStart2nd = System.currentTimeMillis();
            timeout = (config.getTimeout())*(60*1000);
            timeSpent = 0;

            //System.out.println("number of root occurrences "+_rootIDs.size());
            //incremental mining
            while(! _rootIDs.isEmpty() && finished){
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
                timePerGroup = (timeout - timeSpent) / _rootIDs.size() ;
                //log(_report,"- timePerGroup = "+ timePerGroup +" ms");
                //log(_report,"===================");
                //for each group of root occurrences find patterns without max size constraints
                //int groupCount = 1;

                Iterator < Map.Entry<String,ArrayList<Integer>> > rootId = _rootIDs.entrySet().iterator();
                while(rootId.hasNext()){
                    //start expanding a group
                    timeStartGroup = System.currentTimeMillis( );
                    //log(_report,"- Group: "+groupCount);
                    Map.Entry<String,ArrayList<Integer> > entry = rootId.next();
                    //Vector<String> largestPattern = new Vector<>();
                    ArrayList<Integer> largestPattern = new ArrayList<>();
                    Projected projected = new Projected();

                    if(roundCount == 1) {
                        //String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                        //String rootLabel = tmp[0];
                        largestPattern.addAll(entry.getValue());
                        projected.setProjectedDepth(0);
                        //find the root positions
                        String[] temp = entry.getKey().split(";");
                        for(int i=0; i<temp.length; ++i){
                            String[] pos = temp[i].split("-");
                            projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                            projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                        }
                    }else{
                        //from the second round, expanding from the patterns which interrupted in the previous round
                        //String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                        //largestPattern = Pattern.formatPattern(tmp);
                        largestPattern.addAll(entry.getValue());
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
                    //largestPattern.setSize(largestPattern.size() - 1);
                    //largestPattern = new ArrayList<>();
                    //
                    //groupCount++;
                }
                //update running time
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }
            //print maximal patterns
            int nbMFP;
            String outFile = config.getOutputFile();
            if(config.getFilter()) {
                nbMFP = MFP.size();
                outputPatterns(MFP, outFile);
            }else {
                System.out.println("filter FP: "+MFP.size());
                Map<ArrayList<Integer>,String> mfpTemp = filterFP_multi(MFP);
                nbMFP = mfpTemp.size();
                outputPatterns(mfpTemp,outFile);
            }

            if(finished)
                log(_report,"\t + search finished");
            else
                log(_report,"\t + timeout in the second step");

            //report result
            log(_report,"\t + maximal patterns: "+ nbMFP);
            long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
            log(_report, "\t + running time: ..."+currentTimeSpent/1000+"s");
            log(_report,"- total running time "+(System.currentTimeMillis( )-start1st)/1000+"s");
            _report.flush();
            _report.close();

        }catch (Exception e){}
    }


    public void beamRun(Map <String, ArrayList<Integer> > _rootID,long start1st,FileWriter _report){
        try{

            List<Integer> numberIterations = Stream.iterate(1, n -> n + 1)
                    .limit(config.getIterations())
                    .collect(Collectors.toList());

            numberIterations.parallelStream().forEach((iteration) -> {
                //System.out.println("Iteration "+iteration+" ......................");

                Map <String, ArrayList<Integer> > _rootIDs = new ConcurrentHashMap<>(_rootID);
                //calculate times for incremental maximal pattern mining
                int roundCount = 1;
                finished = true;
                timeStart2nd = System.currentTimeMillis();
                timeout = (config.getTimeout())*(60*1000);
                timeSpent = 0;

                //System.out.println("number of root occurrences "+_rootIDs.size());
                //incremental mining
                while(! _rootIDs.isEmpty() && finished){
                    //store interrupted groups which run over timePerTask
                    interruptedRootID = new ConcurrentHashMap<>();
                    //calculate time for each group in the current round
                    //log(_report,"===================");
                    //log(_report,"ROUND: "+ roundCount);
                    //output to check each round
                    //System.out.println("ROUND: "+ roundCount);
                    //System.out.println("groups: "+ _rootIDs.size());
                    //System.out.println("#patterns: "+ outputMaximalPatternsMap.size());

                    //log(_report,"- nbGroups = "+ _rootIDs.size());
                    timePerGroup = (timeout - timeSpent) / _rootIDs.size() ;
                    //log(_report,"- timePerGroup = "+ timePerGroup +" ms");
                    //log(_report,"===================");
                    //for each group of root occurrences find patterns without max size constraints
                    //int groupCount = 1;

                    Iterator < Map.Entry<String,ArrayList<Integer>> > rootId = _rootIDs.entrySet().iterator();
                    while(rootId.hasNext()){
                        //start expanding a group
                        timeStartGroup = System.currentTimeMillis( );
                        //log(_report,"- Group: "+groupCount);
                        Map.Entry<String,ArrayList<Integer> > entry = rootId.next();
                        //Vector<String> largestPattern = new Vector<>();
                        ArrayList<Integer> largestPattern = new ArrayList<>();
                        Projected projected = new Projected();

                        if(roundCount == 1) {
                            //String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                            //String rootLabel = tmp[0];
                            largestPattern.addAll(entry.getValue());
                            projected.setProjectedDepth(0);
                            //find the root positions
                            String[] temp = entry.getKey().split(";");
                            for(int i=0; i<temp.length; ++i){
                                String[] pos = temp[i].split("-");
                                projected.setProjectLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                                projected.setProjectRootLocation(Integer.valueOf(pos[0]),Integer.valueOf(pos[1]));
                            }
                        }else{
                            //from the second round, expanding from the patterns which interrupted in the previous round
                            //String[]tmp = entry.getValue().substring(1,entry.getValue().length()-1).split(String.valueOf(","));
                            //largestPattern = Pattern.formatPattern(tmp);
                            largestPattern.addAll(entry.getValue());
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

                        project(largestPattern, projected);
                    }
                    //update running time
                    timeSpent = (System.currentTimeMillis() - timeStart2nd);
                    //update lists of root occurrences for next round
                    _rootIDs = interruptedRootID;
                    roundCount++;
                }
            });

            //print maximal patterns
            int nbMFP;
            String outFile = config.getOutputFile();
            if(config.getFilter()) {
                nbMFP = MFP.size();
                outputPatterns(MFP, outFile);
            }else {
                System.out.println("filter FP: "+MFP.size());
                Map<ArrayList<Integer>,String> mfpTemp = filterFP_multi(MFP);
                nbMFP = mfpTemp.size();
                outputPatterns(mfpTemp,outFile);
            }

            if(finished)
                log(_report,"\t + search finished");
            else
                log(_report,"\t + timeout in the second step");

            //report result
            log(_report,"\t + maximal patterns: "+ nbMFP);
            long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
            log(_report, "\t + running time: ..."+currentTimeSpent/1000+"s");
            log(_report,"- total running time "+(System.currentTimeMillis( )-start1st)/1000+"s");
            _report.flush();
            _report.close();

        }catch (Exception e){}
    }

}