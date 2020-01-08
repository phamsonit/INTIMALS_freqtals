package be.intimals.freqt.input;

import be.intimals.freqt.FTArray;
import be.intimals.freqt.structure.NodeFreqT;
import be.intimals.freqt.structure.Pattern_Int;

import java.util.*;

public class ReadFile_Int {

    //create transaction from list of patterns
    public void createTransactionFromMap(Vector<FTArray> inPatterns,
                                         Vector<Vector<NodeFreqT>> trans){

        for(int i=0; i< inPatterns.size(); ++i){
            Vector <NodeFreqT> tran_tmp = new Vector<>();
            str2node(inPatterns.elementAt(i),tran_tmp);
            trans.add(tran_tmp);
        }
    }


    private void str2node(FTArray pat, Vector<NodeFreqT> trans){
        try{
            int size_int = Pattern_Int.countNode(pat);
            //init a list of node
            trans.setSize(size_int);
            Vector<Integer> sibling = new Vector<>(size_int);
            sibling.setSize(size_int);
            for(int i = 0; i < size_int; ++i){
                NodeFreqT nodeTemp = new NodeFreqT();
                nodeTemp.setNodeSibling(-1);
                nodeTemp.setNodeParent(-1);
                nodeTemp.setNodeChild(-1);
                trans.setElementAt(nodeTemp,i);
                //node.elementAt(i).setNodeChild(-1);
                //node.elementAt(i).setNodeParent(-1);
                //node.elementAt(i).setNodeSibling(-1);
                sibling.setElementAt(-1,i);
            }

            //create tree
            Vector<Integer> sr = new Vector<>();
            int id = 0;
            int top = 0;

            for(int i = 0; i< pat.size(); ++i){
                if(pat.get(i) == -1){
                    top = sr.size() - 1;
                    if (top < 1) continue;
                    int child = sr.elementAt(top);
                    int parent = sr.elementAt(top-1);
                    trans.elementAt(child).setNodeParent(parent);
                    if(trans.elementAt(parent).getNodeChild() == -1)
                        trans.elementAt(parent).setNodeChild(child);
                    if(sibling.elementAt(parent) != -1)
                        trans.elementAt(sibling.elementAt(parent)).setNodeSibling(child);
                    sibling.setElementAt(child,parent);
                    sr.setSize(top);
                }
                else {
                    //trans.elementAt(id).setNodeLabel(String.valueOf(pat.get(i)));
                    trans.elementAt(id).setNode_label_int(pat.get(i));
                    sr.addElement(id);
                    id ++;
                }
            }


            top = sr.size();
            while(top > 1){
                top = sr.size() - 1;
                //System.out.println("top "+top);
                //if (top < 1) continue;
                int child = sr.elementAt(top);
                int parent = sr.elementAt(top-1);
                trans.elementAt(child).setNodeParent(parent);
                if(trans.elementAt(parent).getNodeChild() == -1)
                    trans.elementAt(parent).setNodeChild(child);
                if(sibling.elementAt(parent) != -1)
                    trans.elementAt(sibling.elementAt(parent)).setNodeSibling(child);
                sibling.setElementAt(child,parent);
                sr.setSize(top);
                //System.out.println("sr.size "+sr.size());

            }


        }catch (Exception e) {
            System.out.println("Fatal: parse error << ["+ e +"]\n");
        }
    }

}
