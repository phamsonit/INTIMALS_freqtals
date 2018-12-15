package be.intimals.freqt.util;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class KBest<T> {
    public TreeMap<Double, List<T>> tree;
    private int k;
    private Double limitVal;
    private boolean keepDuplicates;

    private KBest(int k, boolean keepDuplicates) {
        this.k = k;
        this.tree = new TreeMap<>();
        this.limitVal = Double.MIN_VALUE;
        this.keepDuplicates = keepDuplicates;
    }

    public static <T> KBest<T> create(int k) {
        return new KBest<>(k, false);
    }

    public void add(Double val, T elem) {
        assert (tree.size() <= k);
        if (tree.size() < k) { // No k elements yet, simply add
            List<T> elemList = tree.getOrDefault(val, new ArrayList<>());
            elemList.add(elem);
            tree.put(val, elemList);
            if (val > limitVal) {
                limitVal = val;
            }
        } else if (val <= limitVal) {
            assert (tree.lastKey().equals(limitVal));
            List<T> existingList = tree.get(val);
            if (existingList == null) {
                existingList = new ArrayList<>();
                existingList.add(elem);
                tree.put(val, existingList);
                Double highest = tree.lastKey();
                tree.remove(highest);
                limitVal = tree.lastKey();
            } else {
                existingList.add(elem);
                tree.replace(val, existingList);
                // limitVal doesn't change here
            }
        }
        assert (tree.size() <= k);
    }


    public List<Pair<Double, T>> getKBest() {
        List<Pair<Double, T>> res = new ArrayList<>();
        for (Map.Entry<Double, List<T>> entry : tree.entrySet()) {
            if (res.size() < k) {
                for (T current : entry.getValue()) {
                    if (keepDuplicates || res.size() < k) {
                        res.add(new Pair<>(entry.getKey(), current));
                    }
                }
            }
        }
        return res;
    }

}
