package be.intimals.freqt.core;

public class NodeFreqT { //can extends from Node of Ekeko ???

    private String nodeLabel;
    private String edgeLabel;

    private int parent;
    private int child;
    private int sibling;

    private String degree;
    private Boolean ordered;


    public void setNodeLabel(String s) {
        this.nodeLabel = s;
    }

    public void setNodeEdge(String s) {
        this.edgeLabel = s;
    }

    public void setNodeSibling(int s) {
        this.sibling = s;
    }

    public void setNodeChild(int s) {
        this.child = s;
    }

    public void setNodeParent(int s) {
        this.parent = s;
    }

    public void setNodeDegree(String s) {
        this.degree = s;
    }

    public void setNodeOrdered(Boolean s) {
        this.ordered = s;
    }

    /////////////

    public String getNodeLabel() {
        return this.nodeLabel;
    }

    public String getNodeEdge() {
        return this.edgeLabel;
    }

    public int getNodeSibling() {
        return this.sibling;
    }

    public int getNodeChild() {
        return this.child;
    }

    public int getNodeParent() {
        return this.parent;
    }

    public String getNodeDegree() {
        return this.degree;
    }

    public Boolean getNodeOrdered() {
        return this.ordered;
    }

}
