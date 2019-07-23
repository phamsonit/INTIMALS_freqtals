package be.intimals.freqt.structure;

import javafx.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class NodeFreqT { //can extends from Node of Ekeko ???

    private String node_label;
    private String lineNr;

    private int parent;
    private int child;
    private int sibling;

    private String degree;
    private Boolean ordered;

    //additional information in new singleTree
    private int level;
    private int parentExt;
    private int childExt;
    private int siblingExt;






    private List<Pair<Integer,Integer>> nodeIDs = new LinkedList<>();



    public void setNodeLevel(int s) {
        this.level = s;
    }

    public void setNodeSiblingExt(int s) {
        this.siblingExt = s;
    }

    public void setNodeChildExt(int s)
    {
        this.childExt = s;
    }

    public void setNodeParentExt(int s)
    {
        this.parentExt = s;
    }


    public int getNodeLevel() {
        return this.level;
    }

    public int getNodeSiblingExt() {
        return this.siblingExt;
    }

    public int getNodeChildExt()
    {
        return this.childExt;
    }

    public int getNodeParentExt()
    {
        return this.parentExt;
    }




    ////////////////
    public void setIds(Integer i, Integer j){
        Pair<Integer,Integer> temp = new Pair<>(i,j);
        this.nodeIDs.add(temp);
    }
    public Pair<Integer,Integer> getNodeIDs(int i){
        return this.nodeIDs.get(i);
    }
    public int getSizeNodeIDs(){
        return this.nodeIDs.size();
    }



    public void setNodeLabel(String s)
    {
        this.node_label = s;
    }

    public void setLineNr(String s){
        this.lineNr = s;
    }

    public void setNodeSibling(int s) {
        this.sibling = s;
    }

    public void setNodeChild(int s)
    {
        this.child = s;
    }

    public void setNodeParent(int s)
    {
        this.parent = s;
    }

    public void setNodeDegree(String s) {this.degree = s;}

    public void setNodeOrdered (Boolean s) {this.ordered = s;}

    /////////////

    public String getNodeLabel()
    {
        return this.node_label;
    }

    public String getLineNr()
    {
        return this.lineNr;
    }

    public int getNodeSibling()
    {
        return this.sibling;
    }

    public int getNodeChild()
    {
        return this.child;
    }

    public int getNodeParent()
    {
        return this.parent;
    }

    public String getNodeDegree(){return this.degree;}

    public Boolean getNodeOrdered(){return  this.ordered;}

}
