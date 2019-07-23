package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.output.*;
import be.intimals.freqt.input.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/*
    using original FREQT to find maximal FP from a list of input FP
    Note: slower than pair-wise subtree check
 */
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

    public FreqT_max(Config config) {
        super(config);
    }

    public int getNbMaximalPattern(){
        return  this.nbMaximalPatterns;
    }

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            //System.out.println(getPatternString(maximalPattern));

            if( Pattern.isMissedLeafNode(maximalPattern) ) return;

            //find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            Map <String , Projected > candidate = new LinkedHashMap<>();
            for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
                int id = Location.getLocationId(projected.getProjectLocation(i));
                int pos = Location.getLocationPos(projected.getProjectLocation(i));
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

            prune(candidate,1);
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
                //check pattern before reporting
                String patStr = Pattern.getPatternString(maximalPattern);
                if(patSupMap.containsKey(patStr)){
                    //System.out.println("max pattern "+ entry.getValue().getProjectedSupport()+" " + patStr);
                    if( entry.getValue().getProjectedSupport()==1) {
                        //outputMaximal.report(maximalPattern, entry.getValue());
                        System.out.println(maximalPattern);
                        nbMaximalPatterns++;
                        //System.out.println(Pattern.getPatternString1(maximalPattern));
                    }

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
            //System.out.println("==============================");
            //System.out.println("running FreqT post-processing");
            //System.out.println("==============================");

            initDatabase(inPatterns);

            outputMaximal = config.outputAsXML() ? new XMLOutput(config.getOutputFile(),config, grammar, xmlCharacters, patSupMap) :
                                            new LineOutput(config.getOutputFile(),config, grammar, xmlCharacters, patSupMap, uniChar);

            //find 1-subtree
            Map < String , Projected > freq1 = buildFP1Set(newTransaction);

            //prune 1-subtree
            prune(freq1 ,1 );

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
                    //garbage collector
                    //System.gc();
                }
            }
            //nbMaximalPatterns = outputMaximal.getNbPattern();
            outputMaximal.close();
        }
        catch (Exception e) {System.out.println("running post-processing error "+e);}

    }

    /**
     * create transaction from variable outputLargestPatternMap
     * @param patternMap
     */
    private void initDatabase(Map<String,String> patternMap) {
        //System.out.println("reading input subtrees ...");
        ReadFile readFile = new ReadFile();
        readFile.createTransactionFromMap(patternMap,newTransaction,patSupMap,rootLabel);
    }


}