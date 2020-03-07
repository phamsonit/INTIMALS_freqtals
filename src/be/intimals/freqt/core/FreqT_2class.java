package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.input.ReadXML_Int;
import be.intimals.freqt.structure.FTArray;
import be.intimals.freqt.structure.Pattern_Int;
import be.intimals.freqt.structure.Projected;
import be.intimals.freqt.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

public class FreqT_2class extends FreqT {



    public FreqT_2class(Config _config){
        super(_config);
    }

    @Override
    public void readASTData(){
        try{
            ReadXML_Int readXML_int = new ReadXML_Int();
            readXML_int.readDatabase(transaction,1, config.getAbstractLeafs(),
                    new File(config.getInputFiles1()), labelIndex, transactionClassID);
            readXML_int.readDatabase(transaction,0, config.getAbstractLeafs(),
                    new File(config.getInputFiles2()), labelIndex, transactionClassID);
            sizeClass1 = transactionClassID.stream().mapToInt(Integer::intValue).sum();
            sizeClass2 = transactionClassID.size() - sizeClass1;
        }catch (Exception e){
            System.out.println("read 2-class AST data error "+e);
        }
    }


    @Override
    public void runSecondStep(Map<String, FTArray> _rootIDs, FileWriter report) {
        FreqT_2class_ext freqT_ext = new FreqT_2class_ext(config, this.grammar, this.grammarInt,
                                                            this.blackLabelsInt, this.whiteLabelsInt,
                                                            this.xmlCharacters,this.labelIndex,this.transaction,
                                                            this.sizeClass1, this.sizeClass2);
        freqT_ext.run(_rootIDs, report);
    }


    //check and add pattern to maximal pattern list or keep N patterns which have highest score
    @Override
    public void addMFP(FTArray pattern, Projected projected, Map<FTArray,String> _MFP, Config _config){
        if(_MFP.containsKey(pattern)) return;

        double score = Constraint.chiSquare(projected, sizeClass1, sizeClass2);
        Util.printFTArray(pattern);
        System.out.println(score);
        if(score >= _config.getDSScore()){
            if(_config.keepHighestScore()){
                addHighScorePattern(pattern, projected, _MFP, score);
            }else{
                addMaximalPattern(pattern, projected, _MFP, _config, score);
            }
        }
    }

    //make a string of support, score, size for a pattern
    private String getPatternSupportString(FTArray pattern, Projected projected, double score){
        int[] ac = Constraint.get2ClassSupport(projected);
        String support = String.valueOf(ac[0]) +"-"+String.valueOf(ac[1]);
        int size = Pattern_Int.countNode(pattern);

        return support + "," + score + "," + String.valueOf(size);
    }

    //keep maximal patterns
    private void addMaximalPattern(FTArray pattern, Projected projected, Map<FTArray, String> _MFP, Config _config, double score) {
        //filter and keep maximal pattern
        Iterator< Map.Entry<FTArray,String> > p = _MFP.entrySet().iterator();
        while(p.hasNext()){
            Map.Entry<FTArray, String> entry = p.next();
            switch (CheckSubtree.checkSubTree(pattern, entry.getKey(), _config)){
                case 1: //pat is a subtree of entry.getKey
                    return;
                case 2: //entry.getKey is a subtree of pat
                    p.remove();
                    break;
            }
        }
        String patternSupport = getPatternSupportString(pattern, projected, score);
        //add new maximal pattern to the list
        _MFP.put(pattern, patternSupport);
    }

    //keep only numPattern (for example 100) patterns which have highest score
    private void addHighScorePattern(FTArray pattern, Projected projected, Map<FTArray,String> _MFP, double score){
        if(_MFP.containsKey(pattern)) return;

        if(_MFP.size() >= config.getNumPatterns()) {
            double minScore = getMinScore(_MFP);
            if (score > minScore) {
                //System.out.println("replace pattern");
                FTArray minPattern = getMinScorePattern(_MFP);
                String patternSupport = getPatternSupportString(pattern, projected, score);
                //remove smallest score pattern
                _MFP.remove(minPattern);
                //add new pattern
                _MFP.put(pattern, patternSupport);
            }
        }else{
            //add new pattern
            String patternSupport = getPatternSupportString(pattern, projected, score);
            _MFP.put(pattern, patternSupport);
        }
    }

    //return minimum score in the list of patterns
    private double getMinScore(Map<FTArray,String> _MFP){
        double score = 1000.0;
        for(Map.Entry<FTArray, String> entry : _MFP.entrySet()){
            String[] tmp = entry.getValue().split(",");
            double scoreTmp = Double.valueOf(tmp[1]);
            if(score > scoreTmp)
                score = scoreTmp;
        }
        return score;
    }

    // return a pattern which has minimum score in the list of patterns
    private FTArray getMinScorePattern(Map<FTArray,String> _MFP){
        double score = 1000.0;
        FTArray smallestPattern = new FTArray();

        for(Map.Entry<FTArray, String> entry : _MFP.entrySet()){
            String[] tmp = entry.getValue().split(",");
            double scoreTmp = Double.valueOf(tmp[1]);
            if(score > scoreTmp) {
                score = scoreTmp;
                smallestPattern = new FTArray(entry.getKey());
            }
        }
        return smallestPattern;
    }

}
