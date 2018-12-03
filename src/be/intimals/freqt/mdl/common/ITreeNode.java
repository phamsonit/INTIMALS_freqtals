package be.intimals.freqt.mdl.common;

import java.util.List;

public interface ITreeNode<T, L extends ITreeNode> {
    enum Order {
        ORDERED, UNORDERED, NONE
    }

    enum Degree {
        N, LIST, NONE
    }

    T getLabel();

    void setLabel(T label);

    List<L> getChildren();

    void setChildren(List<L> children);

    void addChild(L child);

    L getChildAt(int idx);

    L getParent();

    void setParent(L parent);

    boolean isLeaf();

    boolean isRoot();

    int getChildrenCount();
}
