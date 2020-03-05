package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.constraint.Constraint;
import be.intimals.freqt.input.ReadFile_Int;
import be.intimals.freqt.structure.*;
import be.intimals.freqt.structure.FTArray;

import java.util.*;

/*
    check subtree relationship of 2 input patterns
 */

public class FreqT_Int_subtree extends FreqT_Int {

    private FTArray maximalPattern;
    private Vector<Vector<NodeFreqT> > newTransaction = new Vector<>();

    private FTArray inputPattern = new FTArray();
    private String outputPattern;

    private boolean found;

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT_Int_subtree(Config config){
        super(config);
    }


    public String getOutputPattern(){return  this.outputPattern;}
    /**
     * expand a subtree
     * @param projected
     */
    private void expandPattern(Projected projected) {
        try{
            if(found) return;

            //System.out.println(maximalPattern);
            //find all candidates of the current subtree
            Map<FTArray, Projected> candidate = generateCandidates(projected);

            Constraint.prune(candidate,2);

            if(candidate.isEmpty()){
                //System.out.println("in find subtree "+maximalPattern+" - "+inputPattern);
                if(maximalPattern.equals(inputPattern)) {
                    //System.out.println("found subtree");
                    outputPattern = "found subtree";
                }
                //not found and stop
                found = true;
                return;

            }else {
                //expand the current pattern with each candidate
                Iterator<Map.Entry<FTArray, Projected>> iter = candidate.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<FTArray, Projected> entry = iter.next();
                    // add new candidate to current pattern
                    maximalPattern.addAll(entry.getKey());
                    expandPattern(entry.getValue());
                }
            }
        }catch (Exception e){
            System.out.println("ERROR: post-processing expanding " + e);
        }
    }

    private Map<FTArray, Projected> generateCandidates(Projected projected) {
        int depth = projected.getProjectedDepth();
        Map <FTArray, Projected > candidate = new LinkedHashMap<>();
        for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
            int id  = projected.getProjectLocation(i).getLocationId();
            int pos = projected.getProjectLocation(i).getLocationPos();
            //String prefix = "";
            FTArray prefixInt = new FTArray();
            for(int d = -1; d < depth && pos != -1; ++d) {
                int start = (d == -1) ?
                        newTransaction.elementAt(id).elementAt(pos).getNodeChild() :
                        newTransaction.elementAt(id).elementAt(pos).getNodeSibling();
                int newdepth = depth - d;
                for (int l = start; l != -1;
                     l = newTransaction.elementAt(id).elementAt(l).getNodeSibling()) {
                    //String item = prefix + uniChar + newTransaction.elementAt(id).elementAt(l).getNodeLabel();
                    FTArray itemInt = new FTArray();
                    itemInt.addAll(prefixInt);
                    itemInt.add(newTransaction.elementAt(id).elementAt(l).getNode_label_int());
                    Projected value = candidate.get(itemInt);
                    if(value != null) {
                        value.setProjectLocation(id,l); //store right most positions
                    }
                    else {
                        Projected tmp = new Projected();
                        tmp.setProjectedDepth(newdepth);
                        tmp.setProjectLocation(id,l); //store right most positions
                        candidate.put(itemInt, tmp);
                    }
                    //////////
                }
                if (d != -1) pos = newTransaction.elementAt(id).elementAt(pos).getNodeParent();
                //prefix += uniChar+")";
                prefixInt.add(-1);
            }
        }
        return candidate;
    }

    /**
     * check subtrees
     * @param pat1
     * @param pat2
     */
    public void checkSubtrees(FTArray pat1, FTArray pat2) {
        try{
            //create input data
            found = false;
            inputPattern = new FTArray(pat1);

            Vector<FTArray> inputPatterns = new Vector<>();
            inputPatterns.add(pat1);
            inputPatterns.add(pat2);
            initDatabase(inputPatterns);

            maximalPattern = new FTArray();
            int rootLabel_int = pat1.get(0);

            maximalPattern.add(rootLabel_int);
            //init locations of pattern
            Projected projected = new Projected();
            projected.setProjectedDepth(0);
            for(int i = 0; i < newTransaction.size(); ++i) {
                for (int j = 0; j < newTransaction.elementAt(i).size(); ++j) {
                    int node_label = newTransaction.elementAt(i).elementAt(j).getNode_label_int();
                    if(node_label == rootLabel_int){
                        projected.setProjectLocation(i,j);
                        //projected.setProjectRootLocation(i,j);
                    }
                }
            }
            //expand the pattern
            expandPattern(projected);
        }
        catch (Exception e) {System.out.println("Error: check sub-trees");}
    }

    /**
     * create transaction from list of patterns
     * @param patterns
     */
    private void initDatabase(Vector<FTArray> patterns) {
        //System.out.println("reading input subtrees ...");
        ReadFile_Int readFile = new ReadFile_Int();
        readFile.createTransactionFromMap(patterns,newTransaction);
    }

}