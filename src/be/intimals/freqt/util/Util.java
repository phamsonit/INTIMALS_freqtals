package be.intimals.freqt.util;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.core.CheckSubtree;
import be.intimals.freqt.structure.FTArray;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.structure.Projected;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Util {

    //filter maximal patterns from list of frequent patterns
    public static Map<FTArray,String> filterFP(Map<FTArray, String> _FP, Config _config){
        Map<FTArray,String> _MFP = new HashMap<>();
        try{
            Iterator < Map.Entry<FTArray,String> > fp = _FP.entrySet().iterator();
            //for each pattern
            while(fp.hasNext()){
                boolean found = false;
                Map.Entry<FTArray, String> fpEntry = fp.next();

                if(_MFP.isEmpty()){
                    _MFP.put(fpEntry.getKey(), fpEntry.getValue());
                }
                else {
                    //check the pattern existing in MFP list ?
                    Iterator<Map.Entry<FTArray, String>> mfp = _MFP.entrySet().iterator();
                    while (mfp.hasNext()) {
                        Map.Entry<FTArray, String> mfpEntry = mfp.next();
                        //check the labels of two subtrees before check maximal subtree
//                        if(checkSubsetLabel(fpEntry.getKey(), mfpEntry.getKey())) {
                        switch (CheckSubtree.checkSubTree(fpEntry.getKey(), mfpEntry.getKey(), _config)) {
                            case 1:
                                found = true;
                                break;
                            case 2:
                                mfp.remove();
                                break;
                        }
//                        }
                    }
                    if (!found) {
                        _MFP.put(fpEntry.getKey(), fpEntry.getValue());
                    }
                }
            }
        }catch (Exception e){System.out.println("Error: Filter maximal pattern");}
        return _MFP;
    }

    //print input trees
    public static void printTransaction(ArrayList <ArrayList<NodeFreqT>> trans){
        for(int i=0; i<trans.size(); ++i){
            for(int j=0; j<trans.get(i).size(); ++j)
                System.out.print((trans.get(i).get(j).getNodeLabel())+"-"+trans.get(i).get(j).getNode_label_int()+" , ");
            System.out.println();
        }
    }

    //print list of candidates: need for debugging
    public static void printCandidates(Map<FTArray, Projected> fp, Map<Integer, String> labelIndex){

        for(Map.Entry<FTArray, Projected> entry : fp.entrySet()){

            FTArray pat = entry.getKey();
            Projected projected = entry.getValue();


            System.out.print("candidate: ");
            for(int i=0; i<pat.size(); ++i){
                String label = labelIndex.get(pat.get(i));
                if(label == null){
                    System.out.print(pat.get(i)+" ");
                }else
                    System.out.print(label);
            }
            System.out.println("\ndepth:" + projected.getProjectedDepth());
            System.out.println("locations: ");
            printProjected(entry.getValue());
        }
    }

    //print a pattern in FTArray format
    public static void printFTArray(FTArray ft){
        for(int i=0; i< ft.size(); ++i)
            System.out.print(ft.get(i)+",");
        System.out.println();
    }

    //print a pattern in FTArray format
    public static void printFTArray(FTArray ft, Map<Integer, String> labelIndex){
        for(int i=0; i< ft.size(); ++i)
            if(ft.get(i)==-1)
                System.out.print("),");
            else
                System.out.print(labelIndex.get(ft.get(i))+",");
        System.out.println();
    }

    //print details of a projected
    public static void printProjected(Projected projected){
        for(int i = 0; i< projected.getProjectLocationSize(); ++i) {
            int classID = projected.getProjectLocation(i).getClassID();
            int locationID = projected.getProjectLocation(i).getLocationId();
            int rootID = projected.getProjectLocation(i).getRoot();
            int locationPos = projected.getProjectLocation(i).getLocationPos();
            System.out.print(classID+"-"+locationID+"-"+rootID+"-"+locationPos);
            if(i<projected.getProjectLocationSize()-1)
                System.out.print(";");
        }

    }

    //get root occurrences of a pattern
    public static String getStringRootOccurrence(Projected projected) {
        String rootOccurrences = "";
        for(int i=0; i<projected.getProjectLocationSize(); ++i){
            rootOccurrences = rootOccurrences +
                    projected.getProjectLocation(i).getClassID() + ("-") +
                    projected.getProjectLocation(i).getLocationId() + ("-") +
                    projected.getProjectLocation(i).getRoot();
            if(i < projected.getProjectLocationSize()-1)
                rootOccurrences = rootOccurrences + ";";
        }
        return rootOccurrences;
    }

}
