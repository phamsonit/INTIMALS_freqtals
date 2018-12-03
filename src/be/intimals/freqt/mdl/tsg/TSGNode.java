package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TSGNode<T> implements ITSGNode<T> {
    private static final Logger LOGGER = Logger.getLogger(TSGNode.class.getName());

    private ITSGNode<T> parent = null;
    private T label = null;
    private List<ITSGNode<T>> children = new ArrayList<>();

    private TSGNode(){}

    private TSGNode(T label) {
        this.label = label;
    }

    public static <T> TSGNode<T> create(T label) {
        return new TSGNode<>(label);
    }

    public static <T> TSGNode<T> clone(ITSGNode<T> root) {
        if (root == null) return null;
        //TSGNode<T> res = new TSGNode<>(root.getLabel());
        //res.setParent(root.getParent() != null ? new TSGNode<>(root.getParent().getLabel()) : null);
        //res.setChildren(root.getChildren().stream().map(c -> {
        //    TSGNode<T> clonedChild = clone(c);
        //    clonedChild.setParent(res);
        //    return clonedChild;
        //}).collect(Collectors.toList()));
        //return res;

        ITSGNode<T> start = root.getParent();
        while (start.getParent() != null) {
            start = start.getParent();
        }
        List<TSGNode<T>> res = new ArrayList<>();
        cloneSingle(root, start, null, res);
        assert (res.size() == 1);
        return res.get(0);
    }

    private static <T> TSGNode<T> cloneSingle(ITSGNode<T> root, ITSGNode<T> current,
                                              ITSGNode<T> parent, List<TSGNode<T>> out) {
        TSGNode<T> res = new TSGNode<>(current.getLabel());
        if (current == root) {
            out.add(res);
        }
        res.setChildren(current.getChildren().stream().map(c -> {
            TSGNode<T> clonedChild = cloneSingle(root, c, res, out);
            return clonedChild;
        }).collect(Collectors.toList()));
        res.setParent(parent);
        return res;
    }

    public static <T> TSGNode<T> createFromWithParent(ITreeNode<T, ? extends ITreeNode<T, ?>> node,
                                                       ITreeNode<T, ? extends ITreeNode<T, ?>> parent) {
        TSGNode<T> res = new TSGNode<>(node.getLabel());
        if (parent != null) {
            TSGNode<T> parentNode = new TSGNode<>(parent.getLabel());
            if (parent.getParent() != null) {
                TSGNode<T> gpNode = new TSGNode<>(parent.getParent().getLabel());
                gpNode.setChildren(Arrays.asList(parentNode));
                parentNode.setParent(gpNode);
            }
            res.setParent(parentNode);
            parentNode.setChildren(Arrays.asList(res));
        }
        return res;
    }

    public static <T> TSGNode<T> createFromWithChildren(ITreeNode<T, ? extends ITreeNode<T, ?>> dbNode,
                                                        ITreeNode<T, ? extends ITreeNode<T, ?>> dbNodeParent) {
        TSGNode<T> res = createFromWithParent(dbNode, dbNodeParent);
        res.setChildren(dbNode.getChildren().stream()
                .map(n -> {
                    TSGNode<T> r = TSGNode.create(n.getLabel());
                    r.setParent(res);
                    return r;
                }).collect(Collectors.toList()));
        return res;
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
    public List<ITSGNode<T>> getChildren() {
        return this.children;
    }

    @Override
    public void setChildren(List<ITSGNode<T>> children) {
        this.children = children;
        for (ITSGNode<T> child : this.children) {
            child.setParent(this);
        }
    }

    @Override
    public void addChild(ITSGNode<T> child) {
        this.children.add(child);
        child.setParent(this);
    }

    @Override
    public ITSGNode<T> getChildAt(int idx) {
        return this.children.get(idx);
    }

    @Override
    public ITSGNode<T> getParent() {
        return this.parent;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public boolean isRoot() {
        return this.parent == null;
    }

    @Override
    public int getChildrenCount() {
        return this.children.size();
    }

    @Override
    public void setParent(ITSGNode<T> parent) {
        this.parent = parent;
    }

    public static <T> ITSGNode<T> debugFromString(T[] tokens, String delimiter) {
        if (tokens.length == 0) return null;
        if (Arrays.stream(tokens).allMatch(e -> e.equals(delimiter))) return null;

        Deque<TSGNode<T>> stack = new ArrayDeque<>();
        for (int i = 0; i < tokens.length; i++) {
            T token = tokens[i];
            if (token.equals(delimiter)) {
                stack.poll();
            } else {
                TSGNode<T> nodeParent = stack.isEmpty() ? null : stack.peek();
                TSGNode<T> node = TSGNode.create(token);
                node.setParent(nodeParent);
                if (nodeParent != null) nodeParent.getChildren().add(node);
                stack.push(node);
            }
        }

        while (stack.size() > 1) {
            stack.poll();
        }
        assert (stack.size() == 1);
        return stack.poll();
    }

}
