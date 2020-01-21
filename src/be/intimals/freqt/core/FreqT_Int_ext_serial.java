package be.intimals.freqt.core;

import be.intimals.freqt.FTArray;
import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.structure.Location;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.structure.Pattern_Int;
import be.intimals.freqt.structure.Projected;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/*
    extended FREQT + without using max size constraints
 */

public class FreqT_Int_ext_serial extends FreqT_Int {

    private Map<FTArray, String> MFP = new HashMap<>();
    private Map<String, FTArray> interruptedRootID = new HashMap<>();

    private long timeout;
    private long timeStart2nd;
    private long timeSpent;
    private long timePerGroup;
    private long timeStartGroup;
    private boolean finished;

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

    public void run(Map <String, FTArray> _rootIDs, FileWriter _report){
        try{
            //set running time for the second steps
            setRunningTime();
            //set the number of round
            int roundCount = 1;
            while(! _rootIDs.isEmpty() && finished){
                //System.out.println("Round: "+roundCount);
                //to store pattern of the group which run over timePerGroup
                interruptedRootID = new LinkedHashMap<>();
                //calculate running time for each group in the current round
                timePerGroup = (timeout - timeSpent) / _rootIDs.size() ;
                //short explanation: each group of rootID has a running time budget "timePerGroup"
                //if a group of rootID cannot finish in the given time
                //patterns are not explored will be stored in the "interruptedRootID"
                //after pass over all rootIDs, if still having time budget the algorithm will
                //recalculate the running time and explore patterns stored in interruptedRootID
                Iterator < Map.Entry<String, FTArray> > rootId = _rootIDs.entrySet().iterator();
                while(rootId.hasNext()){
                    //start to expand a group of rootID
                    timeStartGroup = System.currentTimeMillis( );
                    //log(_report,"- Group: "+groupCount);
                    Map.Entry<String, FTArray> entry = rootId.next();
                    //Vector<String> largestPattern = new Vector<>();
                    FTArray largestPattern = new FTArray();
                    Projected projected = new Projected();

                    if(roundCount == 1) {
                        //build the largestPattern from the rootID found in the first step
                        buildPatternForFirstRound(entry, largestPattern, projected);
                    }else{
                        //build largestPattern from interruptedRootID found in the previous step
                        buildPatternForRoundN(entry, largestPattern, projected);
                    }
                    expandLargestPattern(largestPattern, projected);
                }
                //update running time
                timeSpent = (System.currentTimeMillis() - timeStart2nd);
                //update lists of root occurrences for next round
                _rootIDs = interruptedRootID;
                roundCount++;
            }
            //print the largest patterns
            int nbMFP = printLargestPattern();
            //report result
            reportResult(_report, nbMFP);

        }catch (Exception e){}
    }

    private void buildPatternForRoundN(Map.Entry<String, FTArray> entry, FTArray largestPattern, Projected projected) {
        //add the pattern
        largestPattern.addAll(entry.getValue());
        //set depth, root and right most locations for the pattern
        String[] projectTemp = entry.getKey().split("\t");
        projected.setProjectedDepth(Integer.parseInt(projectTemp[0]));
        //extract right most locations
        String[] rightmostTemp = projectTemp[1].split(";");
        for(int i=0; i<rightmostTemp.length; ++i) {
            String[] pos = rightmostTemp[i].split("-");
            Location initLocation = new Location();// {Integer.valueOf(pos[1])};
            projected.addProjectLocation(Integer.parseInt(pos[0]), Integer.parseInt(pos[2]), initLocation);
        }
    }

    private void buildPatternForFirstRound(Map.Entry<String, FTArray> entry, FTArray largestPattern, Projected projected) {
        largestPattern.addAll(entry.getValue());
        projected.setProjectedDepth(0);
        //extract the positions
        String[] temp = entry.getKey().split(";");
        for(int i=0; i<temp.length; ++i){
            String[] pos = temp[i].split("-");
            projected.setProjectLocation(Integer.parseInt(pos[0]),Integer.parseInt(pos[1]));
        }
    }

    private void expandLargestPattern(FTArray largestPattern, Projected projected) {
        try{
            //check total running time
            if (is2ndStepTimeout()) return;

            //check running for the current group
            if( isGroupTimeout() ) {
                storeInterruptedRootID(largestPattern, projected);
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
            Iterator < Map.Entry<FTArray,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = largestPattern.size();
                Map.Entry<FTArray, Projected> entry = iter.next();
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

    private void addPattern(FTArray largestPattern, Projected projected, Map<FTArray,String> _outputPatterns){
        //remove the part of the pattern that misses leaf
        FTArray patTemp = Pattern_Int.removeMissingLeaf(largestPattern);
        //check output constraints and right mandatory children before storing pattern
        if(checkOutput(patTemp) && ! Constraint.checkRightObligatoryChild(patTemp, grammarInt, blackLabelsInt)){
            if(config.getFilter())
                addMFP(patTemp, projected, _outputPatterns);
            else{
                addFP(patTemp, projected, _outputPatterns);
            }
        }
    }

    private void storeInterruptedRootID(FTArray largestPattern, Projected projected) {
        //store depth and root locations
        String depth = String.valueOf(projected.getProjectedDepth());
        //store locations
        String locations="";
        //keep root occurrences and right-most occurrences
        for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
            locations = locations +
                    projected.getProjectLocation(i).getLocationId() + ("-") +
                    projected.getProjectLocation(i).getRoot() + ("-") +
                    projected.getProjectLocation(i).getLocationPos() + ";";
        }
        //store the pattern for the next round
        interruptedRootID.put(depth+"\t"+locations, largestPattern);
    }

    private void reportResult(FileWriter _report, int nbMFP) throws IOException {
        if(finished)
            log(_report,"\t + search finished");
        else
            log(_report,"\t + timeout in the second step");
        log(_report,"\t + maximal patterns: "+ nbMFP);
        long currentTimeSpent = (System.currentTimeMillis( ) - timeStart2nd);
        log(_report, "\t + running time: ..."+currentTimeSpent/1000+"s");
        //log(_report,"- total running time "+(System.currentTimeMillis( )-start1st)/1000+"s");
        _report.flush();
        _report.close();
    }

    private int printLargestPattern() {
        //print maximal patterns
        int nbMFP;
        String outFile = config.getOutputFile();
        if(config.getFilter()) {
            nbMFP = MFP.size();
            outputPatterns(MFP, outFile);
        }else {
            System.out.println("filter FP: " + MFP.size());
            Map<FTArray,String> mfpTemp = filterFP(MFP);
            nbMFP = mfpTemp.size();
            outputPatterns(mfpTemp, outFile);
        }
        return nbMFP;
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