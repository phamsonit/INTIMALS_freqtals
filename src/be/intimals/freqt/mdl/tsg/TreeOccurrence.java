package be.intimals.freqt.mdl.tsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TSGOccurrence<T> {
    TSGRule<T> owner;
    private int tid;
    private List<Integer> pos = new ArrayList<>();
    private List<Integer> mask = new ArrayList<>();

    private TSGOccurrence() {
    }

    private TSGOccurrence(int tid, List<Integer> pos, TSGRule<T> owner) {
        this.tid = tid;
        this.pos = pos;
        this.mask = new ArrayList<>(Collections.nCopies(pos.size(), -1));
        this.owner = owner;
    }

    //public static TSGOccurrence create() {
    //    return new TSGOccurrence();
    //}

    public static <T> TSGOccurrence<T> create(int tid, List<Integer> occurrence, TSGRule<T> owner) {
        return new TSGOccurrence<>(tid, occurrence, owner);
    }

    public TSGOccurrence(List<Integer> start) {
        pos = new ArrayList<>(start);
    }

    public int getTID() {
        return this.tid;
    }

    public void setTID(int tid) {
        this.tid = tid;
    }

    public void setOccurrence(List<Integer> pos) {
        this.pos = pos;
    }

    public List<Integer> getOccurrence() {
        return this.pos;
    }

    public List<Integer> getMask() {
        return mask;
    }

    public void setMask(List<Integer> mask) {
        this.mask = mask;
    }

    public int getSize() {
        return this.pos.size();
    }

    public TSGRule<T> getOwner() {
        return owner;
    }

    public void setOwner(TSGRule<T> owner) {
        this.owner = owner;
    }
}
