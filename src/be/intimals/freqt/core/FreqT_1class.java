package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.input.ReadXML_Int;
import be.intimals.freqt.structure.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/*
    extended FREQT: replace string labels by int labels
 */
public class FreqT_1class extends FreqT {

    public int hitCount, missCount;

    ////////////////////////////////////////////////////////////////////////////////
    public FreqT_1class(Config _config){
        super(_config);
    }

    //read database and additional information
    @Override
    public void readASTData(){
        //transaction = new ReadXML_Int().readDatabase(config.getAbstractLeafs(), new File(config.getInputFiles()), labelIndex);
        ReadXML_Int readXML_int = new ReadXML_Int();
        readXML_int.readDatabase(transaction,1, config.getAbstractLeafs(),
                new File(config.getInputFiles()), labelIndex, transactionClassID);
    }


    @Override
    public void runSecondStep(Map<String, FTArray> _rootIDs, FileWriter report) {
        FreqT_1class_ext freqT_ext = new FreqT_1class_ext(config, this.grammar, this.grammarInt,
                                                            this.blackLabelsInt, this.whiteLabelsInt,
                                                            this.xmlCharacters,this.labelIndex,this.transaction);
        freqT_ext.run(_rootIDs, report);
    }



    @Override
    public void addMFP(FTArray pat, Projected projected, Map<FTArray,String> _MFP, Config _config){
        //if pat is already existed in the MFP then return
        if(_MFP.containsKey(pat)) return;
        //compare the input pattern to every pattern in _MFP
        Iterator < Map.Entry<FTArray,String> > p = _MFP.entrySet().iterator();
        while(p.hasNext()){
            Map.Entry<FTArray, String> entry = p.next();
            switch (CheckSubtree.checkSubTree(pat, entry.getKey(), _config)){
                case 1: //pat is a subtree of entry.getKey
                    return;
                case 2: //entry.getKey is a subtree of pat
                    p.remove();
                    break;
            }
        }
        String patternSupport = getPatternSupportString(pat, projected);
        //add new pattern to the list
        _MFP.put(pat, patternSupport);
    }


    private String getPatternSupportString(FTArray pattern, Projected projected){
        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport();
        int size = Pattern_Int.countNode(pattern);

        return String.valueOf(support) + "," + String.valueOf(wsupport) + "," + String.valueOf(size);
    }
}