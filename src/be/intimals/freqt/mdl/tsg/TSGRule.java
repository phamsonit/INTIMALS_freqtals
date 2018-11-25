package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class represents the RHS of a rule in our TSG. It contains the root of the tree rule along with it's occurrences
 * in the data. It keeps two counters, the initial count which is initialized when the database is first loaded
 * (should not change afterwards) and the count which is the current number of occurrences of this rule
 * (should change when TSG changes).
 * @param <T>
 */
public class TSGRule<T> {
    private T delimiter;
    private ITSGNode<T> root = null;
    private int count = 0;
    private int initialCount = 0;
    private Map<Integer, List<TSGOccurrence<T>>> occurrencesPerTID = new HashMap<>();

    private TSGRule(T delimiter) {
        this.delimiter = delimiter;
    }

    public static <T> TSGRule<T> create(T delimiter) {
        return new TSGRule<>(delimiter);
    }

    public ITSGNode<T> getRoot() {
        return root;
    }

    public void setRoot(ITSGNode<T> root) {
        this.root = root;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incCount() {
        this.count++;
    }

    public void incCountBy(int by) {
        this.count += by;
        assert (this.count >= 0);
    }

    public int getInitialCount() {
        return initialCount;
    }

    public void setInitialCount(int initialCount) {
        this.initialCount = initialCount;
    }

    public void incInitialCount() {
        this.initialCount++;
    }

    public Map<Integer, List<TSGOccurrence<T>>> getOccurrences() {
        return occurrencesPerTID;
    }

    public List<TSGOccurrence<T>> getOccurrencesPerTID(int tid) {
        return occurrencesPerTID.getOrDefault(tid, new ArrayList<>());
    }

    public TSGOccurrence<T> addOccurrence(int tid, List<Integer> occurrence) {
        List<TSGOccurrence<T>> occurrences = this.occurrencesPerTID.getOrDefault(tid, new ArrayList<>());
        TSGOccurrence<T> toAdd = TSGOccurrence.create(tid, occurrence, this);
        occurrences.add(toAdd);
        this.occurrencesPerTID.put(tid, occurrences);
        return toAdd;
    }

    public void removeOccurrence(int tid, TSGOccurrence<T> occurrence) {
        List<TSGOccurrence<T>> occurrences = this.occurrencesPerTID.get(tid);
        occurrences.remove(occurrence);
        this.occurrencesPerTID.put(tid, occurrences);
    }

    public List<T> toPreOrderList() {
        if (root == null) return new ArrayList<>();
        ITSGNode<T> backtrack = TSGNode.create(delimiter);

        PeekableIterator<ITSGNode<T>> dfsIterator = Util.asPreOrderIteratorWithBacktrack(
                Util.asIterator(root), (ITSGNode<T> node) -> node.getChildren().iterator(), backtrack);
        dfsIterator.next();
        List<T> res = new ArrayList<>();
        while (dfsIterator.hasNext()) {
            T next = dfsIterator.peek() != backtrack ? dfsIterator.peek().getLabel() : delimiter;
            res.add(next);
            dfsIterator.next();
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TSGRule<?> tsgRule = (TSGRule<?>) o;
        if (!Objects.equals(root.getLabel(), tsgRule.root.getLabel())) return false;

        // Check if pre order traversal matches
        List<T> mine = this.toPreOrderList();
        List<?> theirs = tsgRule.toPreOrderList();
        if (theirs.size() != mine.size()) return false;
        return IntStream.range(0, mine.size()).allMatch(i -> Objects.equals(mine.get(i), theirs.get(i)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(toPreOrderList().toArray());
    }

    @Override
    public String toString() {
        return toPreOrderList().toString();
    }
}
