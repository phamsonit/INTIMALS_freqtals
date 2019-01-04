package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents the RHS of a rule in our TSG. It contains the root of the tree rule along with it's occurrences
 * in the data.
 * Important: the hash is based on a preorder traversal with backtrack indicators which make it unique. Some of the
 * traversals (and hashcode) are cached and invalidated only on root change (NOT if the tree is modified).
 * @param <T>
 */
public class TSGRule<T> {
    private T delimiter;
    private ITSGNode<T> root = null;
    private int count = 0;
    private Map<Integer, List<TreeOccurrence<T>>> occurrencesPerTID = new HashMap<>();
    private List<Pair<ITSGNode<T>, ITSGNode<T>>> addedRoots = null;
    private boolean isInBetween = false;
    private List<ITSGNode<T>> cachedWithBacktrack = null;
    private List<ITSGNode<T>> cachedNoBacktrack = null;
    private int patternId = -1;
    private Integer cachedHashCode = null;

    private TSGRule(T delimiter) {
        this.delimiter = delimiter;
    }

    public static <T> TSGRule<T> create(T delimiter) {
        return new TSGRule<>(delimiter);
    }

    public ITSGNode<T> getRoot() {
        return root;
    }


    public int getCount() {
        return count;
    }
    public void setRoot(ITSGNode<T> root) {
        this.cachedWithBacktrack = null;
        this.cachedHashCode = null;
        this.cachedNoBacktrack = null;
        this.root = root;
    }

    public void setCount(int count) {
        this.count = count;
    }

    private void incCountBy(int by) {
        this.count += by;
        assert (this.count >= 0);
    }

    public List<Pair<ITSGNode<T>, ITSGNode<T>>> getAddedRoots() {
        return addedRoots;
    }

    public void setAddedRoots(List<Pair<ITSGNode<T>, ITSGNode<T>>> addedRoots) {
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

    public void addCreatedRoot(ITSGNode<T> parent, ITSGNode<T> root) {
        if (this.addedRoots == null) this.addedRoots = new ArrayList<>();
        this.addedRoots.add(new Pair<>(parent, root));
    }

    public boolean isInBetween() {
        return isInBetween;
    }

    public void setInBetween(boolean inBetween) {
        isInBetween = inBetween;
    }

    public List<T> toPreOrderList() {
        if (root == null) return new ArrayList<>();

        return toPreOrderNodeListBuilder(true).stream().map(ITreeNode::getLabel).collect(Collectors.toList());
    }

    /**
     * Build the preorder traversal of the rule tree. This traversal can be cached & is invalidated on .set.
     * The parameter controls if indicate going up one level after all children are expanded. If set to true, this
     * traversal can be used as unique key.
     * @param showBacktrack
     * @return
     */
    private List<ITSGNode<T>> toPreOrderNodeListBuilder(boolean showBacktrack) {
        if (root == null) return new ArrayList<>();
        if (cachedNoBacktrack != null && !showBacktrack) return cachedNoBacktrack;
        if (cachedWithBacktrack != null && showBacktrack) return cachedWithBacktrack;
        ITSGNode<T> backtrack = TSGNode.create(delimiter);

        PeekableIterator<ITSGNode<T>> dfsIterator = showBacktrack ? Util.asPreOrderIteratorWithBacktrack(
                Util.asSingleIterator(root), (ITSGNode<T> node) -> node.getChildren().iterator(), backtrack) :
                Util.asPreOrderIterator(
                        Util.asSingleIterator(root), (ITSGNode<T> node) -> node.getChildren().iterator());
        dfsIterator.next();
        List<ITSGNode<T>> res = new ArrayList<>();
        while (dfsIterator.hasNext()) {
            ITSGNode<T> next = dfsIterator.peek();
            res.add(next);
            dfsIterator.next();
        }
        if (cachedNoBacktrack == null && !showBacktrack) {
            cachedNoBacktrack = res;
        } else if (cachedWithBacktrack == null && showBacktrack) {
            cachedWithBacktrack = res;
        }

        return res;
    }

    public boolean hasAddedRoots() {
        return addedRoots != null && !addedRoots.isEmpty();
    }

    public int getPatternId() {
        return patternId;
    }

    public void setPatternId(int patternId) {
        this.patternId = patternId;
    }

    /**
     * Build the preorder traversal without showing backtracking nodes.
     * @return
     */
    public List<ITSGNode<T>> toPreOrderFilteredNodeList() {
        return toPreOrderNodeListBuilder(false);
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
        if (cachedHashCode == null) {
            cachedHashCode = Objects.hash(toPreOrderList().toArray());
        }
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return toPreOrderList().toString();
    }
}
