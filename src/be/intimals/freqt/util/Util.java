package be.intimals.freqt.util;

import be.intimals.freqt.structure.FTArray;
import be.intimals.freqt.config.Config;
import be.intimals.freqt.core.CheckSubtree;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.structure.Pattern;
import be.intimals.freqt.structure.Pattern_Int;
import be.intimals.freqt.structure.Projected;

import java.io.FileWriter;
import java.io.IOException;
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
    /**
     * print patterns found in the first step
     * @param report
     * @param start
     * @throws IOException
     */
    public static void printPatternInTheFirstStep(Map<FTArray, String> MFP,
                                            Config config,
                                            Map<String, ArrayList<String>> grammar,
                                            Map<Integer, String> labelIndex,
                                            Map<String, String> xmlCharacters,
                                            FileWriter report,
                                            long start,
                                            boolean finished) throws IOException {
        log(report,"OUTPUT");
        log(report,"===================");
        if(finished)
            log(report,"finished search");
        else
            log(report,"timeout");

        String outFile = config.getOutputFile();
        int nbMFP;
        //if filtering maximal pattern in the mining process then print patterns
        nbMFP = MFP.size();

        outputPatterns(MFP, outFile, config, grammar, labelIndex, xmlCharacters);

        long end1 = System.currentTimeMillis( );
        long diff1 = end1 - start;
        log(report,"+ Maximal patterns = "+ nbMFP);
        log(report,"+ Running times = "+ diff1/1000 +" s");
        report.close();
    }

    //print maximal patterns to XML file
    private static void outputPatterns(Map<FTArray, String> MFP, String outFile,
                                Config config, Map<String, ArrayList <String> > grammar,
                                Map<Integer, String> labelIndex,
                                Map<String, String> xmlCharacters){
        try{
            //create output file to store patterns for mining common patterns
            FileWriter outputCommonPatterns = new FileWriter(outFile+".txt");
            //output maximal patterns
            AOutputFormatter outputMaximalPatterns =  new XMLOutput(outFile, config, grammar, xmlCharacters);
            Iterator< Map.Entry<FTArray,String> > iter1 = MFP.entrySet().iterator();
            while(iter1.hasNext()){
                Map.Entry<FTArray,String> entry = iter1.next();
                ArrayList <String> pat = Pattern_Int.getPatternStr(entry.getKey(),labelIndex);
                String supports = entry.getValue();
                ((XMLOutput) outputMaximalPatterns).report_Int(pat,supports);
                //System.out.println(pat);
                outputCommonPatterns.write(Pattern.getPatternString1(pat)+"\n");
            }
            outputMaximalPatterns.close();

            outputCommonPatterns.flush();
            outputCommonPatterns.close();

        }
        catch(Exception e){System.out.println("error print maximal patterns");}
    }

    //create a report
    public static FileWriter initReport(Config _config, int dataSize) throws IOException {
        String reportFile = _config.getOutputFile().replaceAll("\"","") +"_report.txt";
        FileWriter report = new FileWriter(reportFile);
        log(report,"INPUT");
        log(report,"===================");
        log(report,"- data sources : " + _config.getInputFiles());
        log(report,"- input files : " +  dataSize);
        log(report,"- minSupport : " + _config.getMinSupport());
        report.flush();
        return report;
    }

    //write a string to report
    public static void log(FileWriter report, String msg) throws IOException {
        //System.out.println(msg);
        report.write(msg + "\n");
        report.flush();
    }

    //print list of candidates: need for debugging
    public static void printCandidates(Map<FTArray, Projected> fp, Map<Integer, String> labelIndex){

        for(Map.Entry<FTArray, Projected> entry : fp.entrySet()){

            FTArray pat = entry.getKey();
            Projected projected = entry.getValue();

            System.out.print("\ndepth:" + projected.getProjectedDepth()+", ");

            for(int i=0; i<pat.size(); ++i){
                String label = labelIndex.get(pat.get(i));
                if(label == null){
                    System.out.print(pat.get(i)+" ");
                }else
                    System.out.print(label +" : ");
            }

            System.out.println();
            for(int i = 0 ; i<projected.getProjectLocationSize(); ++i){
                System.out.print(projected.getProjectLocation(i).getLocationId() +" ");
                printFTArray(projected.getProjectLocation(i));
            }

        }
    }

    //print a pattern in FTArray format
    public static void printFTArray(FTArray ft){
        for(int i=0; i< ft.size(); ++i)
            System.out.print(ft.get(i)+",");
        System.out.println();
    }
}
