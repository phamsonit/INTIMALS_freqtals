package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Triple;
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

        public GrammarEntry() {
        }

        public Map<TSGRule<T>, TSGRule<T>> getRules() {
            return rule;
        }

        public void setRules(Map<TSGRule<T>, TSGRule<T>> rule) {
            this.rule = rule;
        }

        public void addRule(TSGRule<T> newRule) {
            this.rule.put(newRule, newRule);
        }

        public TSGRule<T> getRule(TSGRule<T> val) {
            return this.rule.get(val);
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

        @Override
        public String toString() {
            return "[" + getCount() + "]"
                    + "(" + String.join(" | ", getRules().values().stream()
                    .map(r -> r.toString() + "[" + r.getCount() + "]").collect(Collectors.toList())) + ")";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ATSG.class.getName());
    static {
        // TODO DEBUG
        LOGGER.setUseParentHandlers(false);
    }

    protected Database<T> db = null;
    protected Map<T, GrammarEntry> grammar = new HashMap<>();
    // Count how many times child appears. First key is parent, second is child
    protected Map<T, MapCounter<T>> childrenCount = new HashMap<>();

    // First map keys are TIDs, second maps node ID to its TreeOccurrence
    protected Map<Integer, Map<Integer, TreeOccurrence<T>>> pointers = new HashMap<>();

    protected int productionsCount = 0;
    protected int grammarSize = 0;

    private static int patternId = 0;

    @Override
    public void addRule(TSGRule<T> ruleFormat) {
        for (Iterator<TreeOccurrence<T>> iterOccurrences = ruleFormat.getOccurrences().values()
                .stream().flatMap(e -> e.stream()).iterator(); iterOccurrences.hasNext(); ) {
            TreeOccurrence<T> newOccurrenceToAdd = iterOccurrences.next();
            //List<Integer> m = newOccurrenceToAdd.getMask();
            //for (int i = 0; i < m.size(); i++) {
            //    m.set(i, patternId);
            //}

            // The nodes in the new TreeOccurrence point to the old TreeOccurrence
            // Collect those old ones as some nodes may require new rules to be added
            Set<Pair<Integer, TreeOccurrence<T>>> visited = collectOccurrencesToVisit(newOccurrenceToAdd);

            // Note: ruleFormat gives the tree structure of the new rule to add but the parent can be different
            // depending on the current TSG (some nodes are parsed by newly added productions). We clear the added roots
            // each time as the parents can be different
            ruleFormat.setAddedRoots(new ArrayList<>());

            // Assert: Integer keys are unique as they correspond to a position in a preorder traversal
            assert(visited.stream().map(Pair::getKey).sequential().allMatch(new HashSet<>()::add));

            // If we add a new rule, some nodes may be not explained by any rule
            // Add new rules for those nodes
            for (Pair<Integer,TreeOccurrence<T>> e : visited) {
                Integer posOfParent = e.getKey();
                TreeOccurrence<T> visitedOccurrence = e.getValue();
                List<List<Integer>> remains = computeRemaining(visitedOccurrence);

                //ITSGNode<T> root = visitedOccurrence.getOwner().getRoot();

                // "remains" is a list of list containing non-explained nodes in between the newly added occurrence
                // i.e. if a pattern matches A B in the children A X B Y, the remains would be [[], [X], [Y]]
                //boolean allEmpty = remains.stream().allMatch(List::isEmpty); // Can't prune like that
                for (int i = 0; i < remains.size(); i++) {
                    List<Integer> indexes = remains.get(i);
                    // Note: can't prune at this point even if empty there might be an occurrence
                    // which isn't empty and you need to know how many times it was empty

                    if (!indexes.isEmpty()) {
                        int debug = 0;
                    }
                    // Keep reference of newly created rule in main pattern
                    TSGRule<T> createdInBetweenRule = addInBetweenRuleToGrammar(posOfParent, i, visitedOccurrence, indexes);
                    List<ITSGNode<T>> nodesInRule = ruleFormat.toPreOrderFilteredNodeList();

                    assert (ruleFormat.getAllOccurrences().size() == 0
                            || nodesInRule.size() == ruleFormat.getAllOccurrences().get(0).getSize());
                    ruleFormat.addCreatedRoot(nodesInRule.get(posOfParent), createdInBetweenRule.getRoot());

                    //ITSGNode<T> newRoot = createdInBetweenRule.getRoot();
                    //updateChildrenOfInBetweenRule(indexes, visitedOccurrence, root, newRoot);

                } // End remains

                // Remove the occurrence from the production
                visitedOccurrence.getOwner().removeOccurrence(visitedOccurrence.getTID(), visitedOccurrence);
                assert (visitedOccurrence.getOwner().getCount() == visitedOccurrence.getOwner().getOccurrences()
                        .values().stream().mapToInt(List::size).sum());

                // This is the occurrence that belongs to the rule which explains the root of the new occurrence
                if (posOfParent == 0) {
                    // The new rule also has a root and a parent specified but we fix the root depending on the position
                    // and the current grammar (e.g. a pattern (FD(A)(M)) can appear many times in the data but may
                    // have a different parent in the grammar (like a rule in between): the occurrence
                    // matches but not the root&parent
                    ITSGNode<T> rootToAdd = ruleFormat.getRoot();
                    ITSGNode<T> clonedRootToAdd = TSGNode.clone(rootToAdd);
                    ITSGNode<T> existingRoot = TSGNode.clone(visitedOccurrence.getOwner().getRoot());
                    existingRoot.setChildren(clonedRootToAdd.getChildren());

                    // Find grammar entry
                    T grammarKey = getGrammarKey(existingRoot);
                    GrammarEntry entryMain = grammar.getOrDefault(grammarKey, new GrammarEntry());
                    Map<TSGRule<T>, TSGRule<T>> rules = entryMain.getRules();

                    // Rule may already exist, get it or use default new one
                    TSGRule<T> ruleAsKey = TSGRule.create(getDelimiter());
                    ruleAsKey.setRoot(existingRoot);
                    TSGRule<T> ruleMain = rules.getOrDefault(ruleAsKey, ruleAsKey);
                    // TODO only add those roots that have same parent
                    ruleMain.setAddedRoots(ruleFormat.getAddedRoots());
                    ruleMain.addOccurrence(newOccurrenceToAdd);
                    ruleMain.setPatternId(patternId);

                    // Update pointers for the nodes in this occurrence
                    for (int i = 1; i < newOccurrenceToAdd.getSize(); i++) {
                        Integer id = newOccurrenceToAdd.getOccurrence().get(i);
                        Map<Integer, TreeOccurrence<T>> map = this.pointers.get(newOccurrenceToAdd.getTID());
                        map.put(id, newOccurrenceToAdd);
                    }

                    entryMain.addRule(ruleMain);
                    grammar.put(grammarKey, entryMain);
                }
            }
        }
        updateAllCounters();
        ++patternId;
    }
    /*
    private void updateChildrenOfInBetweenRule(List<Integer> indexes, TreeOccurrence<T> visitedOccurrence, ITSGNode<T> root, ITSGNode<T> newRoot) {
        // Fix children of the new rule in between as they have a new parent now
        IntStream.range(0, indexes.size()).forEach(j -> {
            int k = indexes.get(j);

            ITSGNode<T> childNode = root.getChildAt(k);
            int pos = visitedOccurrence.getOccurrence().get(k + 1); // +1 to skip root
            changeParentSingleRule(newRoot, childNode, pos, true);
            int debug = 0;
        });
    }

    private void changeParentSingleRule(ITSGNode<T> newRoot, ITSGNode<T> childNode, int pos, boolean updateChildren) {
        GrammarEntry oldEntry = grammar.get(getGrammarKey(childNode));
        assert (oldEntry != null);
        // Collect all occurrences that have the current child as root
        // Put the occurrence in a new rule which reflects the new parent
        List<TreeOccurrence<T>> collected = oldEntry.getRules().values().stream()
                .flatMap(r -> r.getOccurrences().values().stream()
                        .flatMap(o -> o.stream()))
                .filter(occur -> occur.getOccurrence().get(0) == pos)
                .collect(Collectors.toList());

        for (TreeOccurrence<T> childOccur : collected) {
            // Remove occurrence from existing
            TSGRule<T> parentRule = childOccur.getOwner();
            parentRule.removeOccurrence(childOccur.getTID(), childOccur);

            // Build new rule with this occurrence and with appropriate parent
            TSGRule<T> fixedRule = TSGRule.create(getDelimiter());
            ITSGNode<T> cloned = TSGNode.clone(parentRule.getRoot());
            cloned.setParent(null);
            ITSGNode<T> clonedParent = TSGNode.create(newRoot.getLabel());
            clonedParent.setChildren(new ArrayList<>());
            clonedParent.addChild(cloned);
            fixedRule.setRoot(cloned);
            fixedRule.addOccurrence(childOccur);

            // TODO
            //if (updateChildren) {
            //    // Also update children of this parent, their key based on the parent won't change
            //    // but their grandparent is different
            //    List<Integer> parentPos = Util.getParentPosFromPreorder(parentRule.toPreOrderList(), getDelimiter());
            //    int childrenCount = 0;
            //    for (int i = 0; i < parentPos.size(); i++) {
            //        if (parentPos.get(i) == 0) {
            //            changeParentSingleRule(cloned, cloned.getChildAt(childrenCount), childOccur.getOccurrence().get(i), false);
            //            ++childrenCount;
            //        }
            //    }
            //}

            // Add to grammar
            T newKey = getGrammarKey(fixedRule.getRoot());
            GrammarEntry newEntry = grammar.getOrDefault(newKey, new GrammarEntry());
            //newEntry.incCountBy(fixedRule.getCount());
            newEntry.addRule(fixedRule);
            //oldEntry.incCountBy(-1);
            grammar.put(newKey, newEntry);
        }
    }
    */


    private TSGRule<T> addInBetweenRuleToGrammar(Integer posOfParent, Integer i, TreeOccurrence<T> currentOccurrence,
                                                 List<Integer> indexes) {
        // If nodes where not matched by pattern, make a new rule for them
        TSGRule<T> newInBetweenRule = buildNewRuleInBetween(currentOccurrence.getOwner().getRoot(),
                posOfParent, i, indexes);
        ITSGNode<T> newRoot = newInBetweenRule.getRoot();

        T grammarKey = getGrammarKey(newRoot);
        GrammarEntry newGrammarEntry = grammar.getOrDefault(grammarKey, new GrammarEntry());
        Map<TSGRule<T>, TSGRule<T>> rules = newGrammarEntry.getRules();

        // The new rule that we're adding may already be in the grammar, merge them.
        if (rules.containsKey(newInBetweenRule)) {
            LOGGER.info("Already in rules" + newInBetweenRule.toString());
            newInBetweenRule = rules.get(newInBetweenRule);
        }

        // Build occurrence
        List<Integer> nodeIDs = new ArrayList<>();
        nodeIDs.add(currentOccurrence.getOccurrence().get(0));
        nodeIDs.addAll(indexes.stream()
                .map(k -> currentOccurrence.getOccurrence().get(k + 1))
                .collect(Collectors.toList()));
        TreeOccurrence<T> newTsgOccurrence = newInBetweenRule.addOccurrence(currentOccurrence.getTID(), nodeIDs);

        // Update grammar
        newGrammarEntry.addRule(newInBetweenRule);
        //newGrammarEntry.incCountBy(1);
        grammar.put(grammarKey, newGrammarEntry);

        // Remaining nodes should point to new TreeOccurrence
        indexes.forEach(k -> {
            Map<Integer, TreeOccurrence<T>> map = this.pointers.get(currentOccurrence.getTID());
            map.put(currentOccurrence.getOccurrence().get(k + 1), newTsgOccurrence);
            this.pointers.put(currentOccurrence.getTID(), map);
        });

        return newInBetweenRule;
    }

    private TSGRule<T> buildNewRuleInBetween(ITSGNode<T> root, Integer posOfParent, Integer i, List<Integer> indexes) {
        TSGRule<T> newInBetweenRule = TSGRule.create(getDelimiter());
        TSGNode<T> tempRoot = TSGNode.create(getPatternRuleKey(patternId, posOfParent, i));
        tempRoot.setChildren(IntStream.range(0, root.getChildrenCount())
                .filter(k -> indexes.contains(k)) // Note: here's why we remains are 0 indexed
                .mapToObj(k -> root.getChildAt(k))
                .collect(Collectors.toList()));
        newInBetweenRule.setRoot(tempRoot);
        newInBetweenRule.setInBetween(true);
        return newInBetweenRule;
    }

    private void updateAllCounters() {
        pruneEmptyRules();
        refreshChildrenCount();
    }

    private void pruneEmptyRules() {
        productionsCount = 0;
        grammarSize = 0;
        for (Iterator<Map.Entry<T, GrammarEntry>> iter = grammar.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<T, GrammarEntry> entry = iter.next();
            int sumEntry = 0;
            for (Iterator<Map.Entry<TSGRule<T>, TSGRule<T>>> iterRules = entry.getValue().getRules().entrySet().iterator(); iterRules.hasNext(); ) {
                Map.Entry<TSGRule<T>, TSGRule<T>> entryRule = iterRules.next();
                assert (entryRule.getKey() == entryRule.getValue());
                assert (entryRule.getKey().getCount() == entryRule.getKey().getCount());
                assert (entryRule.getKey().getAllOccurrences().size() == entryRule.getValue().getAllOccurrences().size());
                TSGRule<T> rule = entryRule.getKey();
                rule.setCount(rule.getAllOccurrences().size());
                //sumEntry += rule.getCount();
                if (entry.getValue().getRules().size() == 1 && rule.isInBetween()
                        && rule.toPreOrderFilteredNodeList().size() == 1) {
                    // This is an added in-between rule that doesn't have children & always goes to epsilon, remove
                    iter.remove();
                    LOGGER.info("Pruned empty added rule" + entry.getKey());
                } else if (rule.getCount() == 0) {
                    // No occurrences for this rule, remove
                    iterRules.remove();
                    LOGGER.info("Pruned single rule " + rule.toString() + " of " + entry.getKey());
                } else {
                    sumEntry += rule.getCount();
                }
            }
            entry.getValue().setCount(sumEntry);
            productionsCount += sumEntry;
            if (sumEntry > 0) grammarSize++;
        }

        // Remove in-between added roots that always go to epsilon (in second pass, as first pass removes those entries)
        for (Map.Entry<T, GrammarEntry> entry : grammar.entrySet()) {
            for (Map.Entry<TSGRule<T>, TSGRule<T>> entryRule : entry.getValue().getRules().entrySet()) {
                TSGRule<T> rule = entryRule.getKey();
                if (rule.hasAddedRoots()) {
                    for (Iterator<Pair<ITSGNode<T>, ITSGNode<T>>> iterAdded = rule.getAddedRoots().iterator(); iterAdded.hasNext(); ) {
                        Pair<ITSGNode<T>, ITSGNode<T>> added = iterAdded.next();
                        ITSGNode<T> addedNode = added.getValue();
                        if (!grammar.containsKey(getGrammarKey(addedNode))) {
                            LOGGER.info("Pruned no key " + rule.toString() + " of " + entry.getKey());
                            iterAdded.remove();
                        }
                    }
                }
            }
        }
    }

    private void refreshChildrenCount() {
        childrenCount.clear();
        for (Iterator<Map.Entry<T, GrammarEntry>> iter = grammar.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<T, GrammarEntry> entry = iter.next();
            for (TSGRule<T> rule : entry.getValue().getRules().keySet()) {
                assert (rule.getCount() > 0); // Should be pruned already
                computeIndependentProbas(rule.getRoot());

                // Also take into account added roots in-between which are not directly present in pattern structure
                if (rule.hasAddedRoots()) {
                    for (Pair<ITSGNode<T>, ITSGNode<T>> added : rule.getAddedRoots()) {
                        ITSGNode<T> parentNode = added.getKey();
                        ITSGNode<T> addedNode = added.getValue();
                        assert (grammar.containsKey(getGrammarKey(addedNode)));
                        T parentKey = getGrammarKey(parentNode);
                        MapCounter<T> counter = childrenCount.getOrDefault(parentKey, MapCounter.create());
                        // Note: +1 for geometric param estimation, added roots are not in preorder
                        counter.incNbAttemptsBy(1);
                        counter.incCountBy(getGrammarKey(addedNode), 1);
                        childrenCount.put(parentKey, counter);
                    }
                }
            }
        }
    }

    private void computeIndependentProbas(ITSGNode<T> current) {
        if (!current.isLeaf()) {
            T unannotatedKey = getGrammarKey(current);
            MapCounter<T> counter = childrenCount.getOrDefault(unannotatedKey, MapCounter.create());
            childrenCount.put(unannotatedKey, counter);
            counter.incNbSuccessBy(1); // i.e. Adding new sample
            for (ITSGNode<T> child : current.getChildren()) {
                counter.incCountBy(getGrammarKey(child), 1);
                counter.incNbAttemptsBy(1);
                computeIndependentProbas(child);
            }
        }
    }


    private List<List<Integer>> computeRemaining(TreeOccurrence<T> current) {
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
        LOGGER.info(remains.toString());
        return remains;
    }

    private Set<Pair<Integer, TreeOccurrence<T>>> collectOccurrencesToVisit(TreeOccurrence<T> newOccurrenceToAdd) {
        Integer tid = newOccurrenceToAdd.getTID();
        Map<Integer, TreeOccurrence<T>> currentPointers = pointers.get(tid);
        assert (currentPointers != null); // If this is null, pointers aren't built correctly

        List<T> preorderLabels = newOccurrenceToAdd.getOwner().toPreOrderList();
        List<Integer> parentPos = Util.getParentPosFromPreorder(preorderLabels, getDelimiter());
        // Assert: delimiters shouldn't be present
        assert (parentPos.size() == newOccurrenceToAdd.getSize());
        // Assert: parent positions must be within the range
        assert (parentPos.stream().allMatch(val -> val >= -1 && val < parentPos.size()));

        Set<Pair<Integer,TreeOccurrence<T>>> visited = new HashSet<>();
        List<Integer> nodeIds = newOccurrenceToAdd.getOccurrence();
        for (int i = 1; i < nodeIds.size(); i++) { // Skip root
            TreeOccurrence<T> occurrenceInTsg = currentPointers.get(nodeIds.get(i));
            int position = occurrenceInTsg.getOccurrence().indexOf(nodeIds.get(i));
            // Mark as "dirty"
            visited.add(new Pair<>(parentPos.get(i), occurrenceInTsg));
            // Mark this child as belonging to a pattern
            occurrenceInTsg.getMask().set(position, patternId);
            int debug = 0;
        }
        return visited;
    }

    @Override
    public void removeRule(TSGRule<T> rule) {
        ITSGNode<T> ruleRoot = rule.getRoot();
        T grammarKey = getGrammarKey(ruleRoot);
        GrammarEntry entry = grammar.get(grammarKey);
        if (entry == null) throw new IllegalArgumentException("Missing rule in grammar");

        Map<TSGRule<T>, TSGRule<T>> rules = entry.getRules();
        TSGRule<T> existingRule = rules.get(rule);
        if (existingRule == null) throw new IllegalArgumentException("This rule doesn't exist");

        // Replace the tree rule by the initial rule based on the database
        // We're only interested in indexes of non-leafs in the occurrence
        List<Boolean> isNodeLeaf = existingRule.toPreOrderFilteredNodeList().stream()
                .map(ITreeNode::isLeaf).collect(Collectors.toList());
        for (TreeOccurrence<T> occurrence : existingRule.getAllOccurrences()) {
            assert (occurrence.getSize() == isNodeLeaf.size());
            for (int i = 0; i < isNodeLeaf.size(); i++) {
                Boolean isLeaf = isNodeLeaf.get(i);
                if (isLeaf) continue;

                int tid = occurrence.getTID();
                List<Integer> ids = occurrence.getOccurrence();

                IDatabaseNode<T> node = db.findById(tid, ids.get(i));
                assert (node != null);
                // Note: It is possible there's already a rule with such format in the grammar, the occurrence will
                // be appended once again in this case e.g removing rule (A(B)(C)) but in DB it's still (A(B)(C))
                createRuleFromDatabaseNode(tid, node);

                existingRule.removeOccurrence(tid, occurrence);
                //entry.incCountBy(-1);
            }
        }


        // Remove added in-between rules
        if (existingRule.hasAddedRoots()) {
            for (Pair<ITSGNode<T>, ITSGNode<T>> parentChild : existingRule.getAddedRoots()) {
                ITSGNode<T> addedRoot = parentChild.getValue();
                T addedRootKey = getGrammarKey(addedRoot);
                GrammarEntry toRemove = grammar.get(addedRootKey);
                if (toRemove == null) continue; // Might've been pruned if no occurrences
                grammar.remove(addedRootKey);
            }
        }

        // Note: We can't remove cuz might be an existing rule. We remove the occurrence instead & will be replaced
        // by a new, identical one in that case

        // Arguably, you can make the updates as you go but it's hard to get them right
        buildPointers();
        updateAllCounters();

        //assert (debugAssertCheckRemoval(existingRule.getPatternId()));
    }

    @Override
    public void loadDatabase(Database<T> db) {
        this.db = db;
        int tid = 0;
        for (IDatabaseNode<T> transactionRoot : db.getTransactions()) {
            PeekableIterator<IDatabaseNode<T>> dfsIterator = Util.asPreOrderIterator(
                    Util.asSingleIterator(transactionRoot), (IDatabaseNode<T> node) -> node.getChildren().iterator());
            dfsIterator.next();
            while (dfsIterator.hasNext()) {
                IDatabaseNode<T> currentTreeNode = dfsIterator.peek();
                if (!currentTreeNode.isLeaf()) {
                    createRuleFromDatabaseNode(tid, currentTreeNode);
                }
                dfsIterator.next();
            }
            ++tid;
        }

        buildPointers();
        updateAllCounters();
    }

    //public Map<T, List<List<T>>> getElementaryTreesH1() {
    //    Map<T, List<List<T>>> res = new HashMap<>();
    //    for (Map.Entry<T, GrammarEntry> entry : grammar.entrySet()) {
    //        T key = entry.getKey();
    //        GrammarEntry grammarEntry = entry.getValue();
    //        for (TSGRule<T> tsgRule : grammarEntry.getRules().values()) {
    //            List<T> rule = tsgRule.toPreOrderList();
    //            List<List<T>> allRules = res.getOrDefault(key, new ArrayList<>());
    //            allRules.add(rule);
    //            res.putIfAbsent(key, allRules);
    //        }
    //    }
    //    return res;
    //}

    private TSGRule<T> createRuleFromDatabaseNode(int tid, IDatabaseNode<T> currentTreeNode) {
        T grammarKey = getGrammarKey(currentTreeNode);

        GrammarEntry entry = grammar.getOrDefault(grammarKey, new GrammarEntry());
        Map<TSGRule<T>, TSGRule<T>> rules = entry.getRules();

        // Build 1-depth trees
        TSGRule<T> newRule = TSGRule.create(getDelimiter());

        // Build root
        TSGNode<T> tsgRoot = TSGNode.createFromWithChildren(currentTreeNode, currentTreeNode.getParent());
        newRule.setRoot(tsgRoot);

        // Already found a rule of this form, use it instead
        if (rules.containsKey(newRule)) {
            newRule = rules.get(newRule);
        }

        List<IDatabaseNode<T>> children = currentTreeNode.getChildren();
        // Add database nodes ids to occurrence of the rule
        List<Integer> occurrence = new ArrayList<>();
        occurrence.add(currentTreeNode.getID());
        occurrence.addAll(children.stream()
                .map(IDatabaseNode::getID).collect(Collectors.toList()));
        newRule.addOccurrence(tid, occurrence);

        entry.addRule(newRule);
        grammar.put(grammarKey, entry);
        return newRule;
    }

    private void buildPointers() {
        for (Map.Entry<T, GrammarEntry> grammarEntries : this.grammar.entrySet()) {
            GrammarEntry entry = grammarEntries.getValue();
            Map<TSGRule<T>, TSGRule<T>> val = entry.getRules();
            for (TSGRule<T> rule : val.values()) {
                // Map each node in the database to its TSGOccurrence, group the maps by TIDs
                for (Map.Entry<Integer, List<TreeOccurrence<T>>> occurrencePerTID : rule.getOccurrences().entrySet()) {
                    Integer tid = occurrencePerTID.getKey();
                    for (TreeOccurrence<T> occurrence : occurrencePerTID.getValue()) {
                        // Skip root, may belong to multiple production (e.g. newly added productions)
                        for (int i = 1; i < occurrence.getSize(); i++) {
                            Map<Integer, TreeOccurrence<T>> currentMap = this.pointers.getOrDefault(tid,new HashMap<>());
                            currentMap.put(occurrence.getOccurrence().get(i), occurrence);
                            this.pointers.put(tid, currentMap);
                        }
                    }
                }
            }
        }
        int debug = 0;
    }

    private boolean debugAssertCheckRemoval(int pid) {
        for (Map.Entry<T, GrammarEntry> grammarEntries : this.grammar.entrySet()) {
            GrammarEntry entry = grammarEntries.getValue();
            Map<TSGRule<T>, TSGRule<T>> val = entry.getRules();
            for (TSGRule<T> rule : val.values()) {
                for (Map.Entry<Integer, List<TreeOccurrence<T>>> occurrencePerTID : rule.getOccurrences().entrySet()) {
                    for (TreeOccurrence<T> occurrence : occurrencePerTID.getValue()) {
                        int existingPid = -1;
                        for (Integer m : occurrence.getMask()) {
                            if (m == pid) {
                                assert (false);
                                return false; // Removed, should not be set
                            }
                            if (m != -1) {
                                if (existingPid == -1) {
                                    existingPid = m;
                                } else if (existingPid != m) {
                                    assert (false);
                                    return false; // Children belong to max one pattern
                                }
                            }
                        }

                    }
                }
            }
        }
        return true;
    }

    /**
     * Get the key that will be used for this node in the grammar. In other words, in a production A -> X, it's the
     * representation of the lhs, A.
     * @param child
     * @return
     */
    protected abstract T getGrammarKey(ITreeNode<T, ? extends ITreeNode<T, ?>> child);

    protected abstract T getPatternRuleKey(int patternId, int posInTree, int i);

    protected abstract Triple<Integer, Integer, Integer> getPatternRuleKey(T key);

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

    public double getCodingLength() {
        return getModelCodingLength() + getDataCodingLength();
    }
}
