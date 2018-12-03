package be.intimals.freqt.mdl.input;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DatabaseNode<T> implements IDatabaseNode<T> {
    private static final Logger LOGGER = Logger.getLogger(DatabaseNode.class.getName());
    private static long uidCounter = 0; // TODO very basic
    private static int idCounter = 0;

    private long uid;
    private int tid;
    private int id;
    private IDatabaseNode<T> parent = null;
    private T label = null;
    private List<IDatabaseNode<T>> children = new ArrayList<>();

    // TODO Add notions of degree & order
    //private ITreeNode.Degree degree = Degree.NONE;
    //private ITreeNode.Order order = Order.NONE;

    private DatabaseNode() {
        setUID();
        setID();
    }

    private DatabaseNode(int tid, T label, IDatabaseNode<T> parent) {
        this.tid = tid;
        this.label = label;
        this.parent = parent;
        setUID();
        setID();
    }

    public static <T> DatabaseNode<T> create() {
        return new DatabaseNode<>();
    }

    public static <T> DatabaseNode<T> create(int tid, T label, IDatabaseNode<T> parent) {
        return new DatabaseNode<>(tid, label, parent);
    }


    private void setUID() {
        this.uid = uidCounter;
        ++uidCounter;
    }

    private void setID() {
        this.id = idCounter;
        ++idCounter;
    }

    @Override
    public T getLabel() {
        return label;
    }

    @Override
    public void setLabel(T label) {
        this.label = label;
    }

    @Override
    public int getTID() {
        return tid;
    }

    @Override
    public void setTID(int tid) {
        this.tid = tid;
    }

    @Override
    public long getUID() {
        return uid;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void resetUID() {
        uidCounter = 0;
    }

    @Override
    public void resetID() {
        idCounter = 0;
    }

    @Override
    public List<IDatabaseNode<T>> getChildren() {
        return this.children;
    }

    @Override
    public void setChildren(List<IDatabaseNode<T>> children) {
        this.children = children;
        for (IDatabaseNode<T> child : this.children) {
            child.setParent(this);
        }
    }

    @Override
    public void addChild(IDatabaseNode<T> child) {
        this.children.add(child);
        child.setParent(this);
    }

    @Override
    public IDatabaseNode<T> getChildAt(int idx) {
        return this.children.get(idx);
    }

    @Override
    public IDatabaseNode<T> getParent() {
        return this.parent;
    }

    @Override
    public void setParent(IDatabaseNode<T> parent) {
        this.parent = parent;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public int getChildrenCount() {
        return children.size();
    }

    @Override
    public String toString() {
        return "TreeNode{label=" + label + "}";
    }
}
