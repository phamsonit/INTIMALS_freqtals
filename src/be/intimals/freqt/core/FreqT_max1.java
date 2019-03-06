package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.output.*;
import be.intimals.freqt.input.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class FreqT_max1 extends FreqT {

    private Vector < String > maximalPattern;
    private Vector < Vector<NodeFreqT> > newTransaction = new Vector<>();

    String inputPattern;
    String outputPattern;

    boolean found = false;

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_max1(Config config){
        super(config);
    }


    public String getOutputPattern(){return  this.outputPattern;}
    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            //if( Pattern.isMissedLeafNode(maximalPattern) ) return;

            //find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            Map <String , Projected > candidate = new LinkedHashMap<>();
            for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
                int id  = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                //add to keep all occurrences --> problem: memory consumption
                //List<Integer> occurrences = projected.getProjectLocation(i).getLocationList();

                String prefix = "";
                for(int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ?
                            newTransaction.elementAt(id).elementAt(pos).getNodeChild() :
                            newTransaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newdepth = depth - d;
                    for (int l = start; l != -1;
                         l = newTransaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + newTransaction.elementAt(id).elementAt(l).getNodeLabel();

                        Projected tmp;// = new Projected();
                        if(candidate.containsKey(item)) {
                            candidate.get(item).setProjectLocation(id,l); //store right most positions
                            //add to keep all occurrences
                            //candidate.get(item).addProjectLocation(id, l, occurrences);
                        }
                        else {
                            //add to keep all occurrences
                            tmp = new Projected();
                            tmp.setProjectedDepth(newdepth);
                            //tmp.addProjectLocation(id,l,occurrences);
                            tmp.setProjectLocation(id,l); //store right most positions
                            candidate.put(item, tmp);
                        }
                        //////////

                    }
                    if (d != -1) pos = newTransaction.elementAt(id).elementAt(pos).getNodeParent();
                    prefix += uniChar+")";
                }
            }
            //System.out.println(maximalPattern);
            //System.out.println("before support pruning " + candidate.keySet());
            prune(candidate,2);
            //System.out.println("after support pruning " + candidate.keySet());

            if(candidate.isEmpty()){
                if(Pattern.getPatternString1(maximalPattern).equals(inputPattern)) {
                    //System.out.println("output pattern: " + maximalPattern);
                    outputPattern = Pattern.getPatternString1(maximalPattern);
                    //return;
                }
                found = true;

            }

            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidate.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = maximalPattern.size();
                Map.Entry<String, Projected> entry = iter.next();
                // add new candidate to current pattern
                String[] p = entry.getKey().split(String.valueOf(uniChar));
                for (int i = 0; i < p.length; ++i) {
                    if (!p[i].isEmpty())
                        maximalPattern.addElement(p[i]);
                }
                if(!found)
                    project(entry.getValue());

                maximalPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("ERROR: post-processing expanding " + e);
        }
    }

    /**
     * check subtree of pat1 and pat2
     *
     */
    public void run(String pat1, String pat2) {
        try{
            //create input data
            Map<String,String> inputPatterns = new LinkedHashMap<>();

            if(pat1.length() <= pat2.length()){
                inputPattern = pat1;
                inputPatterns.put(pat1,"supports");
                inputPatterns.put(pat2,"supports");
            }else{
                inputPattern = pat2;
                inputPatterns.put(pat2,"supports");
                inputPatterns.put(pat1,"supports");
            }

            initDatabase(inputPatterns);

            //System.out.println("root label candidates " + freq1.keySet());

            //init maximalPattern
            maximalPattern = new Vector<>();
            String rootLabel1 = newTransaction.elementAt(0).elementAt(0).getNodeLabel();
            //String rootLabel2 = newTransaction.elementAt(1).elementAt(0).getNodeLabel();

            //if(rootLabel1.equals(rootLabel2)){
                maximalPattern.add(rootLabel1);
                //System.out.println("check maximality ========================== "+rootLabel1);
                //init locations
                Projected projected = new Projected();
                projected.setProjectedDepth(0);
                //projected.setProjectLocation(0,0);
                //projected.setProjectRootLocation(0,0);
                //projected.setProjectLocation(1,0);
                //projected.setProjectRootLocation(1,0);

                for(int i = 0; i < newTransaction.size(); ++i) {
                    for (int j = 0; j < newTransaction.elementAt(i).size(); ++j) {
                        String node_label = newTransaction.elementAt(i).elementAt(j).getNodeLabel();
                        if(node_label.equals(rootLabel1)){
                            projected.setProjectLocation(i,j);
                            projected.setProjectRootLocation(i,j);
                        }
                    }
                }
                //expand the pattern
                project(projected);
            //}
        }
        catch (Exception e) {}

    }

    /**
     * create transaction from variable outputLargestPatternMap
     * @param patternMap
     */
    private void initDatabase(Map<String,String> patternMap) {
        //System.out.println("reading input subtrees ...");
        ReadFile readFile = new ReadFile();
        readFile.createTransactionFromMap(patternMap,newTransaction);
    }

}