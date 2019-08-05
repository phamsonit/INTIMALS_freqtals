package be.intimals.freqt.core;

import be.intimals.freqt.config.Config;
import be.intimals.freqt.input.ReadFile;
import be.intimals.freqt.input.ReadFile_Int;
import be.intimals.freqt.structure.*;

import java.util.*;

/*
    check subtree relationship of 2 input patterns
 */

public class FreqT_Int_subtree extends FreqT_Int {

    private ArrayList < Integer > maximalPattern;
    private Vector < Vector<NodeFreqT> > newTransaction = new Vector<>();

    private ArrayList<Integer> inputPattern = new ArrayList<>();
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
    private void project(Projected projected) {
        try{
            if(found) return;

            //System.out.println(maximalPattern);
            //find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            Map <ArrayList<Integer>, Projected > candidate = new LinkedHashMap<>();
            for(int i = 0; i < projected.getProjectLocationSize(); ++i ){
                int id  = Location.getLocationId(projected.getProjectLocation(i));
                int pos = Location.getLocationPos(projected.getProjectLocation(i));

                //String prefix = "";
                ArrayList<Integer> prefixInt = new ArrayList<>();
                for(int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ?
                            newTransaction.elementAt(id).elementAt(pos).getNodeChild() :
                            newTransaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newdepth = depth - d;
                    for (int l = start; l != -1;
                         l = newTransaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        //String item = prefix + uniChar + newTransaction.elementAt(id).elementAt(l).getNodeLabel();
                        ArrayList<Integer> itemInt = new ArrayList<>();
                        itemInt.addAll(prefixInt);
                        itemInt.add(newTransaction.elementAt(id).elementAt(l).getNode_label_int());

                        Projected tmp;// = new Projected();
                        if(candidate.containsKey(itemInt)) {
                            candidate.get(itemInt).setProjectLocation(id,l); //store right most positions

                        }
                        else {
                            tmp = new Projected();
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

            prune(candidate,2);

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
                Iterator<Map.Entry<ArrayList<Integer>, Projected>> iter = candidate.entrySet().iterator();
                while (iter.hasNext()) {
                    int oldSize = maximalPattern.size();
                    Map.Entry<ArrayList<Integer>, Projected> entry = iter.next();
                    // add new candidate to current pattern
                    maximalPattern.addAll(entry.getKey());
                    project(entry.getValue());
                    //maximalPattern = new ArrayList<>(maximalPattern.subList(0,oldSize));
                    //maximalPattern.setSize(oldSize);
                }
            }
        }catch (Exception e){
            System.out.println("ERROR: post-processing expanding " + e);
        }
    }

    /**
     * check subtrees
     * @param pat1
     * @param pat2
     */
    public void checkSubtrees(ArrayList<Integer> pat1, ArrayList<Integer> pat2) {
        try{
            //create input data
            found = false;
            inputPattern = new ArrayList<>(pat1);

            Vector<ArrayList<Integer>> inputPatterns = new Vector<>();
            inputPatterns.add(pat1);
            inputPatterns.add(pat2);
            initDatabase(inputPatterns);

//            System.out.println(pat1+" - "+ pat2);
//            for (int i=0; i<newTransaction.size();++i) {
//                for (int j = 0; j < newTransaction.elementAt(i).size(); ++j)
//                    System.out.print(newTransaction.elementAt(i).elementAt(j).getNode_label_int()+",");
//                System.out.println();
//            }



            maximalPattern = new ArrayList<>();
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
                        projected.setProjectRootLocation(i,j);
                    }
                }
            }
            //expand the pattern
            project(projected);
        }
        catch (Exception e) {System.out.println("Error: check sub-trees");}
    }

    /**
     * create transaction from list of patterns
     * @param patterns
     */
    private void initDatabase(Vector<ArrayList<Integer>> patterns) {
        //System.out.println("reading input subtrees ...");
        ReadFile_Int readFile = new ReadFile_Int();
        readFile.createTransactionFromMap(patterns,newTransaction);
    }

}