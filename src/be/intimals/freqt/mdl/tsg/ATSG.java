package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;
import javafx.util.Pair;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ATSG<T> implements ITSG<T>, IMDL {
    public class GrammarEntry {
        // Rules map to itself for access (it can be considered as a sorted set) // TODO
        private Map<TSGRule<T>, TSGRule<T>> rule = new HashMap<>();
        private int count = 0;
        private int initialCount = 0;

        public GrammarEntry() {
        }

        public GrammarEntry(Map<TSGRule<T>, TSGRule<T>> rule, int count, int initialCount) {
            this.rule = rule;
            this.count = count;
            this.initialCount = initialCount;
        }

        public Map<TSGRule<T>, TSGRule<T>> getRules() {
            return rule;
        }

        public void setRules(Map<TSGRule<T>, TSGRule<T>> rule) {
            this.rule = rule;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void incCountBy(int add) {
            this.count = this.count + add;
        }

        public int getInitialCount() {
            return initialCount;
        }

        public void setInitialCount(int initialCount) {
            this.initialCount = initialCount;
        }

        public void incInitialCountBy(int add) {
            this.initialCount = this.initialCount + add;
        }

        @Override
        public String toString() {
            return "(" + String.join(" | ", getRules().values().stream()
                    .map(TSGRule::toString).collect(Collectors.toList())) + ")";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ATSG.class.getName());

    protected Database<T> db = null;
    protected Map<T, GrammarEntry> grammar = new HashMap<>();

    // First map keys are TIDs, second maps node ID to its TSGOccurrence
    protected Map<Integer, Map<Integer, TSGOccurrence<T>>> pointers = new HashMap<>();

    protected long productionsCount = 0;
    protected long initialProductionsCount = 0;

    private static int patternId = 0;

    @Override
    public void addRule(TSGRule<T> ruleToAdd) {
        for (Map.Entry<Integer, List<TSGOccurrence<T>>> entry : ruleToAdd.getOccurrences().entrySet()) {
            Integer tid = entry.getKey();

            for (TSGOccurrence<T> newOccurrenceToAdd : entry.getValue()) {
                Map<Integer, TSGOccurrence<T>> currentPointers = pointers.get(tid);
                assert (currentPointers != null); // If this is null, pointers aren't built correctly

                // The nodes in the new TSGOccurrence point to the old TSGOccurrences
                // Collect those old ones as some nodes may require new rules to be added
                Set<Pair<Integer,TSGOccurrence<T>>> visited = new HashSet<>();
                List<Integer> nodeIds = newOccurrenceToAdd.getOccurrence();
                for (int i = 1; i < nodeIds.size(); i++) { // Skip root
                    TSGOccurrence<T> occurrenceInTsg = currentPointers.get(nodeIds.get(i));
                    // Mark as "dirty"
                    visited.add(new Pair<>(i,occurrenceInTsg));
                    int position = occurrenceInTsg.getOccurrence().indexOf(nodeIds.get(i));
                    occurrenceInTsg.getMask().set(position, patternId);
                }

                // If we add a new rule, some nodes may be not explained by any rule
                // Add new rules for those nodes
                for (Pair<Integer,TSGOccurrence<T>> e : visited) {
                    TSGOccurrence<T> current = e.getValue();
                    List<List<Integer>> remains = new ArrayList<>();
                    List<Integer> accumulator = new ArrayList<>();
                    for (int i = 1; i < current.getMask().size(); i++) { // Skip root
                        Integer maskVal = current.getMask().get(i);
                        if (maskVal == patternId) {
                            remains.add(new ArrayList<>(accumulator));
                            accumulator.clear();
                        } else if (maskVal == -1) {
                            accumulator.add(i - 1); // Note: 0-based indexing for this
                        }
                    }
                    remains.add(new ArrayList<>(accumulator));
                    accumulator.clear();
                    System.out.println(remains);

                    // "remains" is a list of list containing non-explained nodes in between the newly added occurrence
                    // i.e. if a pattern matches A B in the children A X B Y, the remains would be [[], [X], [Y]]
                    for (List<Integer> indexes : remains) {
                        if (indexes.isEmpty()) continue;

                        // If nodes where not matched by pattern, make a new rule for them
                        TSGRule<T> newRule = TSGRule.create(getDelimiter());
                        ITSGNode<T> root = current.getOwner().getRoot();
                        ITSGNode<T> newRoot = TSGNode.createFromWithChildren(root);
                        newRoot.setChildren(IntStream.range(0, newRoot.getChildrenCount())
                                .filter(i -> indexes.contains(i)) // Note: here's why we remains are 0 indexed
                                .mapToObj(i -> newRoot.getChildAt(i))
                                .collect(Collectors.toList()));
                        newRule.setRoot(newRoot);

                        T grammarKey = getGrammarKey(newRoot);
                        GrammarEntry newGrammarEntry = grammar.getOrDefault(grammarKey, new GrammarEntry());
                        Map<TSGRule<T>, TSGRule<T>> rules = newGrammarEntry.getRules();

                        // The new rule that we're adding may already be in the grammar, merge them.
                        if (rules.containsKey(newRule)) {
                            System.out.println("ALREADY IN RULES");
                            newRule = rules.get(newRule);
                        }

                        List<Integer> newOccurrence = new ArrayList<>();
                        newOccurrence.add(current.getOccurrence().get(0));
                        newOccurrence.addAll(indexes.stream()
                                .map(i -> current.getOccurrence().get(i + 1))
                                .collect(Collectors.toList()));

                        TSGOccurrence<T> newTsgOccurrence = newRule.addOccurrence(current.getTID(), newOccurrence);
                        // TODO count
                        newRule.incInitialCount();
                        newRule.incCount();

                        // Update grammar
                        rules.put(newRule, newRule);
                        grammar.put(grammarKey, newGrammarEntry);
                        // Remaining nodes should point to new TSGOccurrence
                        indexes.forEach(i -> {
                            Map<Integer, TSGOccurrence<T>> map = this.pointers.get(tid);
                            map.put(current.getOccurrence().get(i + 1), newTsgOccurrence);
                            this.pointers.put(tid, map);
                        });
                        int debug = 0;
                    }

                    current.getOwner().incCountBy(-1);
                    T ruleKey = getGrammarKey(current.getOwner().getRoot());
                    GrammarEntry toUpdate = grammar.get(ruleKey);
                    toUpdate.incCountBy(-1);
                    current.getOwner().removeOccurrence(tid, current);
                    assert (current.getOwner().getCount() == current.getOwner().getOccurrences().values()
                            .stream().mapToInt(List::size).sum());
                    int debug = 0;
                }
            }
        }

        T grammarKey = getGrammarKey(ruleToAdd.getRoot());

        GrammarEntry entry = grammar.getOrDefault(grammarKey, new GrammarEntry());
        entry.incCountBy(ruleToAdd.getCount());
        Map<TSGRule<T>, TSGRule<T>> rules = entry.getRules();
        // This specific rule should not be in set of rules yet (key can be in grammar though)
        assert (!rules.containsKey(ruleToAdd));
        rules.put(ruleToAdd, ruleToAdd);
        grammar.put(grammarKey, entry);

        ++patternId;
    }

    @Override
    public void removeRule(TSGRule<T> rule) {
        // TODO ?
    }

    @Override
    public void loadDatabase(Database<T> db) {
        this.db = db;
        int tid = 0;
        for (IDatabaseNode<T> transactionRoot : db.getTransactions()) {
            PeekableIterator<IDatabaseNode<T>> dfsIterator = Util.asPreOrderIterator(
                    Util.asIterator(transactionRoot), (IDatabaseNode<T> node) -> node.getChildren().iterator());
            dfsIterator.next();
            while (dfsIterator.hasNext()) {
                IDatabaseNode<T> currentTreeNode = dfsIterator.peek();
                if (!currentTreeNode.isLeaf()) {
                    T grammarKey = getGrammarKey(currentTreeNode);

                    GrammarEntry entry = grammar.getOrDefault(grammarKey, new GrammarEntry());
                    Map<TSGRule<T>, TSGRule<T>> rules = entry.getRules();

                    // Build 1-depth trees
                    TSGRule<T> newRule = TSGRule.create(getDelimiter());

                    // Build root
                    TSGNode<T> tsgRoot = TSGNode.createFromWithChildren(currentTreeNode);
                    newRule.setRoot(tsgRoot);

                    // Already found a rule of this form, use it instead
                    if (rules.containsKey(newRule)) {
                        newRule = rules.get(newRule);
                    }

                    List<Integer> occurrence = new ArrayList<>();
                    occurrence.add(currentTreeNode.getID());
                    occurrence.addAll(currentTreeNode.getChildren().stream()
                            .map(IDatabaseNode::getID).collect(Collectors.toList()));
                    newRule.addOccurrence(tid, occurrence);
                    // TODO counters
                    newRule.incCount();
                    newRule.incInitialCount();

                    // Update grammar
                    rules.put(newRule, newRule);
                    grammar.put(grammarKey, entry);

                }
                dfsIterator.next();
            }
            ++tid;
        }

        buildPointers();
    }

    private void buildPointers() {
        for (Map.Entry<T, GrammarEntry> grammarEntries : this.grammar.entrySet()) {
            GrammarEntry entry = grammarEntries.getValue();
            Map<TSGRule<T>, TSGRule<T>> val = entry.getRules();
            for (TSGRule<T> rule : val.values()) {
                // Increase count of productions used
                productionsCount += rule.getCount();
                initialProductionsCount += rule.getCount();

                // Increase the count of the specific production that is used
                entry.incCountBy(rule.getCount());
                entry.incInitialCountBy(rule.getCount());

                // Map each node in the database to its TSGOccurrence, group the maps by TIDs
                for (Map.Entry<Integer, List<TSGOccurrence<T>>> occurrencePerTID : rule.getOccurrences().entrySet()) {
                    Integer tid = occurrencePerTID.getKey();
                    for (TSGOccurrence<T> occurrence : occurrencePerTID.getValue()) {
                        for (Integer id : occurrence.getOccurrence()) {
                            Map<Integer, TSGOccurrence<T>> currentMap = this.pointers.getOrDefault(tid, new HashMap<>());
                            currentMap.put(id, occurrence);
                            this.pointers.put(tid, currentMap);
                        }
                    }
                }
            }
        }
        int debug = 0;
    }

    /**
     * Get the key that will be used for this node in the grammar. In other words, in a production A -> X, it's the
     * representation of the lhs, A.
     * @param child
     * @return
     */
    protected abstract T getGrammarKey(ITreeNode<T, ? extends ITreeNode<T, ?>> child);

    /**
     * Get the value that will represent backtracking one level during a pre order traversal (e.g. a tree X(A)(B) would
     * produce [X, A, T, B, T, T] as traversal where T is the return value of this method.
     * The value should not appear anywhere in the tree.
     * @return
     */
    public abstract T getDelimiter();

    /**
     * Get the coding length of the model i.e. L(H).
     * @return number of bits (in R)
     */
    public abstract double getModelCodingLength();

    /**
     * Get the coding length of the data given the model i.e. L(D|H).
     * @return number of bits (in R)
     */
    public abstract double getDataCodingLength();
}
