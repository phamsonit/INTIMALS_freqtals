package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.util.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
    extended FREQT + without using max size constraints
 */

public class FreqT_ext extends FreqT {

    private Map<Projected, FTArray> interruptedRootIDs;// = new HashMap<>();

    private long timeStart2nd;
    private long timeSpent;
    private long timePerGroup;
    private long timeStartGroup;

    private boolean finishedGroup;
    private FTArray interrupted_pattern;
    private Projected interrupted_projected;

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
                //System.out.println("\nRound: "+roundCount);
                //System.out.println("#groups: "+_rootIDs.size());
                //each group of rootID has a running time budget "timePerGroup"
                //if a group cannot finish search in the given time
                //this group will be stored in the "interruptedRootID"
                //after passing over all groups in rootIDs, if still having time budget
                //the algorithm will continue exploring patterns from groups stored in interruptedRootID
                interruptedRootIDs = new HashMap<>();
                //calculate running time for each group in the current round
                timePerGroup = (timeout - timeSpent) / _rootIDs.size() ;
                for(Map.Entry<Projected, FTArray> group : _rootIDs.entrySet()){
                    //start expanding a group of rootID
                    timeStartGroup = System.currentTimeMillis( );
                    finishedGroup = true;

                    Projected projected = getProjected(group.getKey());

                    //keep current pattern and location if this group cannot finish
                    interrupted_pattern = group.getValue().subList(0,1);
                    interrupted_projected = group.getKey();
                    //expand the current root occurrences to find maximal patterns
                    expandLargestPattern(group.getValue(), projected);
                }
                //update running time
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootIDs;
                //increase number of round
                roundCount++;
            }
            //print the largest patterns
            if(!MFP.isEmpty()) {
                //printLargestPattern();
                //Map<FTArray, String> MFPFilter = filterLeafPattern(MFP, MFPLeaf);
                outputPatterns(MFP, config, grammar, labelIndex, xmlCharacters);
            }

            //report result
            reportResult(_report);

        }catch (Exception e){
            System.out.println("expand maximal pattern error "+e);
        }
    }

    //expand pattern to find maximal patterns
    private void expandLargestPattern(FTArray largestPattern, Projected projected) {
        try{
            if(!finishedGroup || !finished) return;
            //check running time of the 2nd step
            if (is2ndStepTimeout()) {
                //System.out.println("2nd step timeout");
                finished = false;
                return;
            }
            //check running for the current group
            if( isGroupTimeout() ) {
                //System.out.println("group timeout");
                //storeInterruptedRootID(largestPattern, projected);
                interruptedRootIDs.put(interrupted_projected, interrupted_pattern);
                finishedGroup = false;
                return;
            }
            //find candidates for the current pattern
            Map<FTArray, Projected> candidates = generateCandidates(projected, transaction);
            //prune on minimum support and list of black labels
            //Constraint.pruneSupportAndBlacklist(candidates, config.getMinSupport(), largestPattern, blackLabelsInt);
            Constraint.prune(candidates, config.getMinSupport(), config.getWeighted());
            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                if(leafPattern.size() > 0) {
                    //store pattern
                    //addPattern(largestPattern, projected);
                    addPattern(leafPattern, leafProjected);
                }
                return;
            }
            //expand the current pattern with each candidate
            for(Map.Entry<FTArray, Projected> entry : candidates.entrySet()){
                int oldSize = largestPattern.size();
                largestPattern.addAll(entry.getKey());
                if(largestPattern.getLast() < -1 )
                    keepLeafPattern(largestPattern, entry.getValue());
                FTArray oldLeafPattern = leafPattern;
                Projected oldLeafProjected = leafProjected;
                //check section and paragraphs in COBOL
                Constraint.checkCobolConstraints(largestPattern, entry, entry.getKey(), labelIndex, transaction);
                //check constraints
                if(Constraint.missingLeftObligatoryChild(largestPattern, entry.getKey(), grammarInt)){
                    //do nothing = don't store pattern to MFP
                }else{
                    if( Constraint.isNotFullLeaf(largestPattern) ){
                        if(leafPattern.size() > 0) {
                            //store the pattern
                            addPattern(leafPattern, leafProjected);
                        }
                    }else{
                        //continue expanding pattern
                        expandLargestPattern(largestPattern, entry.getValue());
                    }
                }
                largestPattern = largestPattern.subList(0, oldSize); //keep elements 0 to oldSize
                keepLeafPattern(oldLeafPattern, oldLeafProjected);
                //largestPattern.shrink(oldSize);
            }
        }catch (Exception e){
            System.out.println("Error: Freqt_ext projected " + e);
            e.printStackTrace();
        }
    }

    //add pattern to maximal pattern list
    private void addPattern(FTArray pat, Projected projected){
        //check output constraints and right mandatory children before storing pattern
        if(Constraint.checkOutput(pat, config.getMinLeaf(), config.getMinNode())
                && ! Constraint.missingRightObligatoryChild(pat, grammarInt)){
            if(config.get2Class()) {
                //check chi-square score
                if (Constraint.satisfyChiSquare(projected, sizeClass1, sizeClass2, config.getDSScore(), config.getWeighted()))
                    addMaximalPattern(pat, projected, MFP);
            }else{
                addMaximalPattern(pat, projected, MFP);
            }
        }
    }

    //get initial locations of a projected
    private Projected getProjected(Projected projected) {
        //create location for the current pattern
        Projected ouputProjected = new Projected();
        ouputProjected.setProjectedDepth(0);
        for(int i=0; i<projected.getProjectLocationSize(); i++){
            int classID = projected.getProjectLocation(i).getClassID();
            int locationID = projected.getProjectLocation(i).getLocationId();
            int rootID = projected.getProjectLocation(i).getRoot();
            //System.out.println("\n"+classID+" "+locationID+" "+rootID);
            Location temp = new Location();
            ouputProjected.addProjectLocation(classID,locationID,rootID, temp);
        }
        return ouputProjected;
    }

    private void reportResult(FileWriter _report) throws IOException {
        if(finished)
            log(_report,"\t + search finished");
        else
            log(_report,"\t + timeout in the second step");

        log(_report,"\t + maximal patterns: "+ MFP.size());
        long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
        log(_report, "\t + running time: ..."+currentTimeSpent/1000+"s");
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
        return ( (System.currentTimeMillis() - timeStart2nd) > timeout) ;
    }

    private boolean isGroupTimeout() {
        return ((System.currentTimeMillis( ) - timeStartGroup) > timePerGroup );
    }

}