package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.structure.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
    extended FREQT + without using max size constraints
 */

public class FreqT_ext extends FreqT {

    private Map<String, FTArray> interruptedRootID = new HashMap<>();

    private Map<Projected, FTArray> interruptedRootIDNew = new HashMap<>();

    private long timeStart2nd;
    private long timeSpent;
    private long timePerGroup;
    private long timeStartGroup;


    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_ext(Config _config,
                            Map<String,ArrayList<String>> _grammar,
                            Map<Integer,ArrayList<String>> _grammarInt,
                            Map<Integer,ArrayList<Integer>> _blackLabelsInt,
                            Map<Integer,ArrayList<Integer>> _whiteLabelsInt,
                            Map<String,String> _xmlCharacters,
                            Map<Integer,String> _labelIndex,
                            ArrayList <ArrayList<NodeFreqT> >  _transaction,
                            int _sizeClass1,
                            int _sizeClass2) {
        super(_config);
        this.grammar = _grammar;
        this.grammarInt = _grammarInt;
        this.blackLabelsInt = _blackLabelsInt;
        this.whiteLabelsInt = _whiteLabelsInt;
        this.xmlCharacters = _xmlCharacters;
        this.labelIndex = _labelIndex;
        this.transaction = _transaction;
        this.sizeClass1 = _sizeClass1;
        this.sizeClass2 = _sizeClass2;
    }

    public void run(Map <Projected, FTArray> _rootIDs, FileWriter _report){
        try{
            //set running time for the second steps
            setRunningTime();
            //set the number of round
            int roundCount = 1;
            while(! _rootIDs.isEmpty() && finished){
                System.out.println("Round: "+roundCount);
                //to store pattern of the group which run over timePerGroup
                interruptedRootIDNew = new LinkedHashMap<>();
                //calculate running time for each group in the current round
                timePerGroup = (timeout - timeSpent) / _rootIDs.size() ;
                //short explanation: each group of rootID has a running time budget "timePerGroup"
                //if a group of rootID cannot finish in the given time
                //patterns are not explored will be stored in the "interruptedRootID"
                //after pass over all rootIDs, if still having time budget the algorithm will
                //recalculate the running time and explore patterns stored in interruptedRootID
                for(Map.Entry<Projected, FTArray> entry : _rootIDs.entrySet()){
                    //start to expand a group of rootID
                    timeStartGroup = System.currentTimeMillis( );
                    if(roundCount==1) {
                        //build location for the pattern
                        Projected projected = new Projected();
                        projected.setProjectedDepth(0);
                        for(int i=0; i<entry.getKey().getProjectLocationSize(); i++){
                            //entry.getKey().getProjectLocation(i).shrink(3);
                            int classID = entry.getKey().getProjectLocation(i).getClassID();
                            int locationID = entry.getKey().getProjectLocation(i).getLocationId();
                            int rootID = entry.getKey().getProjectLocation(i).getRoot();
                            //System.out.println("\n"+classID+" "+locationID+" "+rootID);
                            Location temp = new Location();
                            projected.addProjectLocation(classID,locationID,rootID, temp);
                        }
                        expandLargestPattern(entry.getValue(), projected);
                    }else
                        expandLargestPattern(entry.getValue(), entry.getKey());

                }
                //update running time
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootIDNew;
                roundCount++;
            }
            //print the largest patterns
            if(!MFP.isEmpty())
                //printLargestPattern();
                outputPatterns(MFP, config, grammar, labelIndex, xmlCharacters);

            //report result
            reportResult(_report);

        }catch (Exception e){
            System.out.println("expand maximal pattern error "+e);
        }
    }

    private void expandLargestPattern(FTArray largestPattern, Projected projected) {
        try{
            //check total running time
            if (is2ndStepTimeout()) return;

            //check running for the current group
            if( isGroupTimeout() ) {
                storeInterruptedRootID(largestPattern, projected, interruptedRootID);
                return;
            }

            //find candidates for the current pattern
            Map<FTArray, Projected> candidates = generateCandidates(projected, transaction);

            //prune on minimum support and list of black labels
            Constraint.pruneSupportAndBlacklist(candidates, config.getMinSupport(), largestPattern, blackLabelsInt);

            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                addPattern(largestPattern, projected, MFP);
                return;
            }

            //expand the current pattern with each candidate
            for(Map.Entry<FTArray, Projected> entry : candidates.entrySet()){
                int oldSize = largestPattern.size();
                largestPattern.addAll(entry.getKey());

                //check section and paragraphs in COBOL
                Constraint.checkCobolConstraints(largestPattern, entry, entry.getKey(), labelIndex, transaction);

                //check constraints
                if(Constraint.checkLeftObligatoryChild(largestPattern, entry.getKey(), grammarInt, blackLabelsInt)){
                    //do nothing = don't store pattern to MFP
                }else{
                    if( Constraint.satisfyFullLeaf(largestPattern) ){
                        //store the pattern
                        addPattern(largestPattern, entry.getValue(), MFP);
                    }else{
                        //continue expanding pattern
                        expandLargestPattern(largestPattern, entry.getValue());
                    }
                }
                largestPattern = largestPattern.subList(0, oldSize); //keep elements 0 to oldSize
                //largestPattern.shrink(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    private void addPattern(FTArray largestPattern, Projected projected, Map<FTArray,String> _MFP){
        //remove the part of the pattern that misses leaf
        FTArray patTemp = Pattern_Int.removeMissingLeaf(largestPattern);
        //check output constraints and right mandatory children before storing pattern
        if(Constraint.checkOutput(patTemp, config.getMinLeaf(), config.getMinNode())
                && ! Constraint.checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){

            if(config.get2Class()){
                add2ClassPattern(patTemp, projected, MFP);
            }else{
                //add1ClassPattern(patTemp,projected, MFP);
                addMaximalPattern(patTemp, projected, MFP, config, 0);
            }
        }
    }

    private void storeInterruptedRootID(FTArray largestPattern, Projected projected,
                                        Map<String, FTArray> _interruptedRootID) {
        try{
            //store depth and root locations
            String depth = String.valueOf(projected.getProjectedDepth());
            //store locations
            String locations="";
            //keep root occurrences and right-most occurrences
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                locations = locations +
                        projected.getProjectLocation(i).getClassID() + ("-") +
                        projected.getProjectLocation(i).getLocationId() + ("-") +
                        projected.getProjectLocation(i).getRoot() + ("-") +
                        projected.getProjectLocation(i).getLocationPos() + ";";
            }
            //store the pattern for the next round
            _interruptedRootID.put(depth+"\t"+locations, largestPattern);

            interruptedRootIDNew.put(projected, largestPattern);

        }catch (Exception e){
            System.out.println("store interrupted pattern error "+e);
        }

    }

    private void reportResult(FileWriter _report) throws IOException {
        if(finished)
            log(_report,"\t + search finished");
        else
            log(_report,"\t + timeout in the second step");

        log(_report,"\t + maximal patterns: "+ MFP.size());
        long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
        log(_report, "\t + running time: ..."+currentTimeSpent/1000+"s");
        //log(_report,"- total running time "+(System.currentTimeMillis( )-start1st)/1000+"s");
        _report.flush();
        _report.close();
    }

    private void setRunningTime() {
        finished = true;
        timeStart2nd = System.currentTimeMillis();
        timeout = (config.getTimeout())*(60*1000);
        timeSpent = 0;
    }

    private boolean is2ndStepTimeout() {
        if(System.currentTimeMillis() - timeStart2nd > timeout){ //long timeRemaining = timeFor2nd - timeSpent;
            finished = false;
            return true;
        }
        return false;
    }

    private boolean isGroupTimeout() {
        return (System.currentTimeMillis( ) - timeStartGroup) > timePerGroup;
    }

}