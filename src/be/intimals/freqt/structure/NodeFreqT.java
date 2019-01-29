package be.intimals.freqt.structure;

public class NodeFreqT { //can extends from Node of Ekeko ???

    private String node_label;
    private String lineNr;

    private int parent;
    private int child;
    private int sibling;

    private String degree;
    private Boolean ordered;


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
