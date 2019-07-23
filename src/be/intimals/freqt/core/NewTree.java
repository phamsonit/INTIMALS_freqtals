package be.intimals.freqt.core;

import be.intimals.freqt.structure.NodeFreqT;

import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import javafx.util.Pair;

public class NewTree {

    private Vector<NodeFreqT> mainTree;
    private Map<Integer,String> index;

    public NewTree(){
        this.mainTree = new Vector<>();
        this.index = new HashMap<>();
    }

    private void setIndex(int id, String pat){
        this.index.put(id,pat);
    }

    //create or add pattern into tree
    //pat occurrences,support,rootSupport,size \t patternString
    public void updateTree(String pat, int patternID){

        String[]temp = pat.split("\t");
        String patternSupport = temp[0];
        String patternString = temp[1];

        System.out.println(patternID+" "+patternSupport+" "+patternString);

        setIndex(patternID,patternSupport);

        if(mainTree.size() == 0) {
            mainTree = createTree(patternString, patternID);
            //addTree(mainTree);
        }
        else{//add subtree into tree
            Vector<NodeFreqT> subtree = createTree(patternString,patternID);
            addTree(subtree);
            //addTree(mainTree);
        }
    }


    public void printTree(){
        int subTreePos = 0;
        while(subTreePos < mainTree.size()){
            System.out.print(mainTree.elementAt(subTreePos).getNodeLabel()+" ");
            for(int i=0; i<mainTree.elementAt(subTreePos).getSizeNodeIDs(); ++i)
                System.out.print(mainTree.elementAt(subTreePos).getNodeIDs(i)+" ");
            //add the current node to mainTree
            ++subTreePos;
            System.out.println();
        }
    }

    //add a pattern into an exist tree
    private void addTree(Vector<NodeFreqT> subTree){

        String subTreeRootLabel = subTree.elementAt(0).getNodeLabel();
        //add nodeIDs of root label of subtree into label of mainTree
        int subTreePos = 0;
        Pair<Integer,Integer> temp = subTree.elementAt(0).getNodeIDs(0);
        mainTree.elementAt(0).setIds(temp.getKey(),temp.getValue());

        //recur adding children of subTree into mainTree
        while(subTreePos < subTree.size()){

            ++subTreePos;
        }

    }

    //create new tree from input String, i.e., (A(B(D)(C(F))
    private Vector<NodeFreqT> createTree(String str, int patternID){
        Vector<NodeFreqT> subtree = new Vector<>();
        try{
            int len = str.length();
            int size = 0;
            String buff = ""; //individual node
            Vector<String> tmp = new Vector<>(); //a list of node

            int ii=0;
            while (ii<len){
                if(str.charAt(ii) == '(' || str.charAt(ii) == ')'){
                    if(! buff.isEmpty()){
                        if(buff.charAt(0)=='*'){
                            tmp.addElement(buff);
                        }
                        else{
                            String[] label = buff.split("_");
                            tmp.addElement(label[0]);
                        }
                        buff = "";
                        ++size;
                    }
                    if(str.charAt(ii) == ')') tmp.addElement(")");
                }
                else
                if(str.charAt(ii) == '\t' || str.charAt(ii) == ' ') {buff += "_";}
                else {
                    //adding to find leaf node i.e. *X(120)
                    if (str.charAt(ii)=='*'){
                        int bracket=0;
                        while(bracket >= 0){

                            if(str.charAt(ii)=='(')
                                bracket++;
                            else
                            if(str.charAt(ii)==')')
                                bracket--;

                            if(bracket==-1)
                                break;
                            else {
                                buff += str.charAt(ii);
                                ++ii;
                            }
                        }
                        //System.out.println(buff);
                        --ii;
                    }else buff += str.charAt(ii);
                }
                ++ii;
            }

            if (! buff.isEmpty()) throw new ArithmeticException("error !");

            //init a list of node
            subtree.setSize(size);
            Vector<Integer> sibling = new Vector<>(size);
            sibling.setSize(size);
            for(int i = 0; i < size; ++i){
                NodeFreqT node = new NodeFreqT();
                node.setNodeSibling(-1);
                node.setNodeParent(-1);
                node.setNodeChild(-1);
                node.setIds(patternID,i); //set patternID and node ID for the current node
                subtree.setElementAt(node,i);
                //node.elementAt(i).setNodeChild(-1);
                //node.elementAt(i).setNodeParent(-1);
                //node.elementAt(i).setNodeSibling(-1);
                sibling.setElementAt(-1,i);
            }
            //create tree
            Vector<Integer> sr = new Vector<>();
            int id = 0;
            int top = 0;

            for(int i = 0; i< tmp.size(); ++i){
                if( tmp.elementAt(i).equals(")") ){
                    top = sr.size() - 1;
                    if (top < 1) continue;
                    int child = sr.elementAt(top);
                    int parent = sr.elementAt(top-1);
                    subtree.elementAt(child).setNodeParent(parent);
                    if(subtree.elementAt(parent).getNodeChild() == -1)
                        subtree.elementAt(parent).setNodeChild(child);
                    if(sibling.elementAt(parent) != -1)
                        subtree.elementAt(sibling.elementAt(parent)).setNodeSibling(child);
                    sibling.setElementAt(child,parent);
                    sr.setSize(top);
                }
                else {
                    subtree.elementAt(id).setNodeLabel(tmp.elementAt(i));
                    sr.addElement(id);
                    id ++;
                }
            }

        }catch (Exception e) {
            System.out.println("Fatal: parse error << ["+ str +"]\n"+e);
            System.exit(-1);
        }
        return subtree;

    }

    //extract maximal trees from compressed tree
    public void extractMFP(){
        //find MFP from nodelist

    }







}
