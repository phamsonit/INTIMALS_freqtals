package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;

import java.util.*;
import java.util.stream.Collectors;
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
    //private int initialCount = 0;
    private Map<Integer, List<TreeOccurrence<T>>> occurrencesPerTID = new HashMap<>();
    private List<ITSGNode<T>> addedRoots = null;

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

    private void incCountBy(int by) {
        this.count += by;
        assert (this.count >= 0);
    }

    public List<ITSGNode<T>> getAddedRoots() {
        return addedRoots;
    }

    public void setAddedRoots(List<ITSGNode<T>> addedRoots) {
        this.addedRoots = addedRoots;
    }

    public Map<Integer, List<TreeOccurrence<T>>> getOccurrences() {
        return occurrencesPerTID;
    }

    public List<TreeOccurrence<T>> getAllOccurrences() {
        return occurrencesPerTID.values().stream().flatMap(e -> e.stream()).collect(Collectors.toList());
    }

    public List<TreeOccurrence<T>> getOccurrencesPerTID(int tid) {
        return occurrencesPerTID.getOrDefault(tid, new ArrayList<>());
    }

    public TreeOccurrence<T> addOccurrence(int tid, List<Integer> occurrence) {
        List<TreeOccurrence<T>> occurrences = this.occurrencesPerTID.getOrDefault(tid, new ArrayList<>());
        TreeOccurrence<T> toAdd = TreeOccurrence.create(tid, occurrence, this);
        occurrences.add(toAdd);
        incCountBy(1);
        this.occurrencesPerTID.put(tid, occurrences);
        return toAdd;
    }

    public TreeOccurrence<T> addOccurrence(TreeOccurrence<T> occurrence) {
        List<TreeOccurrence<T>> occurrences = this.occurrencesPerTID.getOrDefault(occurrence.getTID(),new ArrayList<>());
        occurrences.add(occurrence);
        occurrence.setOwner(this);
        incCountBy(1);
        this.occurrencesPerTID.put(occurrence.getTID(), occurrences);
        return occurrence;
    }

    public boolean removeOccurrence(int tid, TreeOccurrence<T> occurrence) {
        List<TreeOccurrence<T>> occurrences = this.occurrencesPerTID.get(tid);
        boolean removed = occurrences.remove(occurrence);
        if (removed) incCountBy(-1);
        this.occurrencesPerTID.put(tid, occurrences);
        return removed;
    }

    public T getDelimiter() {
        return delimiter;
    }

    public void addCreatedRoot(ITSGNode<T> root) {
        if (this.addedRoots == null) this.addedRoots = new ArrayList<>();
        this.addedRoots.add(root);
    }

    public List<ITSGNode<T>> getCreatedRoots() {
        return this.addedRoots;
    }

    public List<T> toPreOrderList() {
        if (root == null) return new ArrayList<>();

        return toPreOrderNodeList().stream().map(ITreeNode::getLabel).collect(Collectors.toList());
    }

    public List<ITSGNode<T>> toPreOrderNodeList() {
        if (root == null) return new ArrayList<>();
        ITSGNode<T> backtrack = TSGNode.create(delimiter);

        PeekableIterator<ITSGNode<T>> dfsIterator = Util.asPreOrderIteratorWithBacktrack(
                Util.asSingleIterator(root), (ITSGNode<T> node) -> node.getChildren().iterator(), backtrack);
        dfsIterator.next();
        List<ITSGNode<T>> res = new ArrayList<>();
        while (dfsIterator.hasNext()) {
            ITSGNode<T> next = dfsIterator.peek();
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
