package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.output.*;
import be.intimals.freqt.input.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class FreqT_max extends FreqT {



    private AOutputFormatter outputMaximal;
    private Vector < String > maximalPattern;
    private Vector < Vector<NodeFreqT> > newTransaction = new Vector<>();

    //store mapping of old {sup, wsup, size} and pattern
    private Map<String,String> patSupMap = new LinkedHashMap<>();

    //store root labels of input patterns
    private Set<String> rootLabel = new LinkedHashSet<>();

    private int nbMaximalPatterns;
    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_max(Config config, Map<String,Vector<String>> grammar,
                     Map<String,Vector<String>> blackLabels, Map<String,Vector<String>> whiteLabels,
                     Map<String,String> xmlCharacters) {
        super(config);
        this.grammar = grammar;
        this.blackLabels = blackLabels;
        this.whiteLabels = whiteLabels;
        this.xmlCharacters = xmlCharacters;
    }

    public int getNbMaximalPattern(){
        return  this.nbMaximalPatterns;
    }

    private void prune (Map <String, Projected > candidate){
        Iterator < Map.Entry<String,Projected> > iter = candidate.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            int sup = support(entry.getValue());
            //int sup = entry.getValue().getProjectedSupport();
            if(sup < 1){//actually don't prune any candidate !
                iter.remove();
            }
            else entry.getValue().setProjectedSupport(sup);
        }
    }
    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            //System.out.println(getPatternString(maximalPattern));

            if( Pattern.checkMissedLeafNode(maximalPattern) ) return;

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

            prune(candidate);
            //System.out.println("after support pruning " + candidate.keySet());

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
                //check before reporting
                String patStr = Pattern.getPatternString(maximalPattern);
                if(patSupMap.containsKey(patStr)){
                    //System.out.println("max pattern "+ entry.getValue().getProjectedSupport()+" " + patStr);
                    if( entry.getValue().getProjectedSupport()==1)
                        //collect leaves of maximalPattern
                        //if leaves exists in
                        outputMaximal.report(maximalPattern, entry.getValue());
                }

                project(entry.getValue());
                maximalPattern.setSize(oldSize);
            }
        }catch (Exception e){
            System.out.println("ERROR: post-processing expanding " + e);
        }
    }

    /**
     * run Freqt with file config.properties
     *
     */
    public void run(Map<String,String> inPatterns) {
        try{

            //os = new FileWriter(config.getOutputFile());
            /*  ==============================  */
            //System.out.println("# patterns : "+ newTransaction.size());
            //System.out.println("==============================");
            //System.out.println("running FreqT post-processing");
            //System.out.println("==============================");

            initDatabase(inPatterns);

            outputMaximal = config.outputAsXML() ? new XMLOutput(config, grammar, xmlCharacters, patSupMap) :
                                            new LineOutput(config, grammar, xmlCharacters, patSupMap, uniChar);


            //find 1-subtree
            Map < String , Projected > freq1 = buildFreq1Set();

            //prune 1-subtree
            prune( freq1 );

            //System.out.println("root label candidates " + freq1.keySet());

            //expansion every 1-subtree to find larger subtrees
            maximalPattern = new Vector<>();
            Iterator < Map.Entry<String,Projected> > iter = freq1.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String,Projected> entry = iter.next();
                if(rootLabel.contains(entry.getKey())){
                    entry.getValue().setProjectedDepth(0);
                    maximalPattern.addElement(entry.getKey());

                    project(entry.getValue());

                    maximalPattern.setSize(maximalPattern.size()-1);
                }
            }
            nbMaximalPatterns = outputMaximal.getNbPattern();
            outputMaximal.close();
        }
        catch (Exception e) {System.out.println("running post-processing error "+e);}

    }

    private Map<String, Projected> buildFreq1Set() {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < newTransaction.size(); ++i) {
            for (int j = 0; j < newTransaction.elementAt(i).size(); ++j) {
                String node_label = newTransaction.elementAt(i).elementAt(j).getNodeLabel();
                //find a list of location then add to freq1[node_label].locations
                Projected projected = new Projected();
                //if node_label exists
                if(freq1.containsKey(node_label)) {
                    freq1.get(node_label).setProjectLocation(i,j);
                    freq1.get(node_label).setProjectRootLocation(i,j);
                }
                else {
                    projected.setProjectLocation(i,j);
                    projected.setProjectRootLocation(i,j);
                    freq1.put(node_label, projected);
                }
            }
        }
        return freq1;
    }

    private void initDatabase(Map<String,String> patternMap) {
        //System.out.println("reading input subtrees ...");
        ReadFile readFile = new ReadFile();
        readFile.createTransactionFromMap(patternMap,newTransaction,patSupMap,rootLabel);
    }

}