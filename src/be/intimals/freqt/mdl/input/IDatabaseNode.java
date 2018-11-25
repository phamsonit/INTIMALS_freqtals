package be.intimals.freqt.mdl.input;

import be.intimals.freqt.mdl.common.ITreeNode;

public interface IDatabaseNode<T> extends ITreeNode<T, IDatabaseNode<T>> {
    int getTID();

    void setTID(int tid);

    long getUID();

    int getID();

    void resetUID();

    void resetID();
}
