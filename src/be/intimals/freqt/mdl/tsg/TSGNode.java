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

    //public static <T> TSGNode<T> createFrom(IDatabaseNode<T> dbNode) {
    //    return new TSGNode<>(dbNode.getLabel());
    //}

    private static <T> TSGNode<T> createFrom(ITreeNode<T, ?> node) {
        return new TSGNode<>(node.getLabel());
    }

    //public static <T> TSGNode<T> createFromWithChildren(IDatabaseNode<T> dbNode) {
    //    TSGNode<T> res = new TSGNode<>(dbNode.getLabel());
    //    res.setChildren(dbNode.getChildren().stream()
    //            .map(n -> {
    //                TSGNode<T> r = TSGNode.createFrom(n);
    //                r.setParent(res);
    //                return r;
    //            }).collect(Collectors.toList()));
    //    return res;
    //}

    public static <T> TSGNode<T> createFromWithChildren(ITreeNode<T, ? extends ITreeNode<T, ?>> dbNode) {
        TSGNode<T> res = createFrom(dbNode);
        TSGNode<T> parent = dbNode.getParent() != null ? createFrom(dbNode.getParent()) : null;
        res.setParent(parent);
        res.setChildren(dbNode.getChildren().stream()
                .map(n -> {
                    TSGNode<T> r = TSGNode.createFrom(n);
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

    public static <T> ITSGNode<T> debugFromString(T[] tokens) {
        if (tokens.length == 0) return null;
        if (Arrays.stream(tokens).allMatch(e -> e.equals("|"))) return null;

        Deque<TSGNode<T>> stack = new ArrayDeque<>();
        for (int i = 0; i < tokens.length; i++) {
            T token = tokens[i];
            if (token.equals("|")) {
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
