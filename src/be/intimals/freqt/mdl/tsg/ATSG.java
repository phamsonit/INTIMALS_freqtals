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

    protected Database<T> db = null;
    protected Map<T, GrammarEntry> grammar = new HashMap<>();
    // Count how many times child appears. First key is parent, second is child
    protected Map<T, MapCounter<T>> childrenCount = new HashMap<>();

    // First map keys are TIDs, second maps node ID to its TreeOccurrence
    protected Map<Integer, Map<Integer, TreeOccurrence<T>>> pointers = new HashMap<>();

    protected long productionsCount = 0;

    private static int patternId = 0;

    @Override
    public void addRule(TSGRule<T> ruleFormat) {
        for (Iterator<TreeOccurrence<T>> iterOccurrences = ruleFormat.getOccurrences().values()
                .stream().flatMap(e -> e.stream()).iterator(); iterOccurrences.hasNext(); ) {
            TreeOccurrence<T> newOccurrenceToAdd = iterOccurrences.next();
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

                ITSGNode<T> root = visitedOccurrence.getOwner().getRoot();

                // "remains" is a list of list containing non-explained nodes in between the newly added occurrence
                // i.e. if a pattern matches A B in the children A X B Y, the remains would be [[], [X], [Y]]
                //boolean allEmpty = remains.stream().allMatch(List::isEmpty); // Can't prune like that
                for (int i = 0; i < remains.size(); i++) {
                    List<Integer> indexes = remains.get(i);
                    // Note: can't prune at this point even if empty there might be an occurrence
                    // which isn't empty and you need to know how many times it was empty

                    // If nodes where not matched by pattern, make a new rule for them
                    //TSGRule<T> newInBetweenRule = TSGRule.create(getDelimiter());
                    //ITSGNode<T> newParent = TSGNode.createFromWithChildren(root, root.getParent());
                    //TSGNode<T> newRoot = TSGNode.create(getPatternRuleKey(newParent, patternId, posOfParent, i));
                    //newRoot.setChildren(IntStream.range(0, newParent.getChildrenCount())
                    //        .filter(k -> indexes.contains(k)) // Note: here's why we remains are 0 indexed
                    //        .mapToObj(k -> newParent.getChildAt(k))
                    //        .collect(Collectors.toList()));
                    //newRoot.setParent(newParent);
                    //newParent.setChildren(Arrays.asList(newRoot));
                    //newInBetweenRule.setRoot(newRoot);

                    if (!indexes.isEmpty()) {
                        int debug = 0;
                    }
                    // Keep reference of newly created rule in main pattern
                    TSGRule<T> createdInBetweenRule = addInBetweenRuleToGrammar(posOfParent, i, visitedOccurrence, indexes);
                    ruleFormat.addCreatedRoot(createdInBetweenRule.getRoot());
                    ITSGNode<T> newRoot = createdInBetweenRule.getRoot();
                    updateChildrenOfInBetweenRule(indexes, visitedOccurrence, root, newRoot);

                } // End remains

                // Decrease the count for the production not used anymore
                // and the entry which contains this production. Also, remove the occurrence from the production
                //currentOccurrence.getOwner().incCountBy(-1);
                T ruleKey = getGrammarKey(visitedOccurrence.getOwner().getRoot());
                GrammarEntry toUpdate = grammar.get(ruleKey);
                //toUpdate.incCountBy(-1);
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

                    // Update pointers for the nodes in this occurrence
                    for (int i = 1; i < newOccurrenceToAdd.getSize(); i++) {
                        Integer id = newOccurrenceToAdd.getOccurrence().get(i);
                        Map<Integer, TreeOccurrence<T>> map = this.pointers.get(newOccurrenceToAdd.getTID());
                        map.put(id, newOccurrenceToAdd);
                    }

                    entryMain.addRule(ruleMain);
                    //entryMain.incCountBy(1);
                    grammar.put(grammarKey, entryMain);
                }
            }
        }
        refreshChildrenCount();
        pruneEmptyRules();
        ++patternId;
    }

    private void updateChildrenOfInBetweenRule(List<Integer> indexes, TreeOccurrence<T> visitedOccurrence, ITSGNode<T> root, ITSGNode<T> newRoot) {
        // Fix children of the new rule in between as they have a new parent now
        IntStream.range(0, indexes.size()).forEach(j -> {
            int k = indexes.get(j);

            ITSGNode<T> childNode = root.getChildAt(k);
            int pos = visitedOccurrence.getOccurrence().get(k + 1); // +1 to skip root
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
                // Remove occurrence from existing rule
                childOccur.getOwner().removeOccurrence(childOccur.getTID(), childOccur);

                // Build new rule with this occurrence and with appropriate parent
                TSGRule<T> fixedRule = TSGRule.create(getDelimiter());
                ITSGNode<T> cloned = TSGNode.clone(childOccur.getOwner().getRoot());
                ITSGNode<T> clonedParent = TSGNode.createFromWithParent(newRoot, newRoot.getParent());
                clonedParent.setChildren(new ArrayList<>());
                clonedParent.addChild(cloned);
                fixedRule.setRoot(cloned);
                fixedRule.addOccurrence(childOccur);

                // Add to grammar
                T newKey = getGrammarKey(fixedRule.getRoot());
                GrammarEntry newEntry = grammar.getOrDefault(newKey, new GrammarEntry());
                //newEntry.incCountBy(fixedRule.getCount());
                newEntry.addRule(fixedRule);
                //oldEntry.incCountBy(-1);
                grammar.put(newKey, newEntry);
            }
            int debug = 0;
        });
    }

    private TSGRule<T> addInBetweenRuleToGrammar(Integer posOfParent, Integer i, TreeOccurrence<T> currentOccurrence,
                                                 List<Integer> indexes) {
        // If nodes where not matched by pattern, make a new rule for them
        TSGRule<T> newInBetweenRule = buildNewRule(currentOccurrence.getOwner().getRoot(), posOfParent, i, indexes);
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


        // TODO if need to add the in-between rules to possible rules of their parents but need to prune empty later
        //T grammarParentKey = getGrammarKey(newRoot.getParent());
        //GrammarEntry entry = grammar.get(grammarParentKey);
        //assert (entry != null); // By construction
        //entry.addRule(newInBetweenRule);

        // TODO
        //// Update children counts
        //// Decrement old
        //T rootKey = getGrammarKey(root);
        //MapCounter<T> rootCounter = childrenCount.get(rootKey);
        //assert (rootCounter != null);
        //indexes.stream().map(k -> getGrammarKey(root.getChildAt(k)))
        //        .forEach(key -> {
        //            rootCounter.incCountBy(key, -1);
        //        });
        //rootCounter.incCountBy(grammarKey, 1);
        //// Add new
        //MapCounter<T> newCounter = childrenCount.getOrDefault(grammarKey, MapCounter.create());
        //newRoot.getChildren().stream().map(n -> getGrammarKey(n))
        //        .forEach(key -> {
        //            newCounter.incCountBy(key, 1);
        //        });
        //childrenCount.put(grammarKey, newCounter);

        return newInBetweenRule;
    }

    private TSGRule<T> buildNewRule(ITSGNode<T> root, Integer posOfParent, Integer i, List<Integer> indexes) {
        TSGRule<T> newInBetweenRule = TSGRule.create(getDelimiter());
        ITSGNode<T> newParent = TSGNode.createFromWithChildren(root, root.getParent());
        TSGNode<T> tempRoot = TSGNode.create(getPatternRuleKey(newParent, patternId, posOfParent, i));
        tempRoot.setChildren(IntStream.range(0, newParent.getChildrenCount())
                .filter(k -> indexes.contains(k)) // Note: here's why we remains are 0 indexed
                .mapToObj(k -> newParent.getChildAt(k))
                .collect(Collectors.toList()));
        tempRoot.setParent(newParent);
        newParent.setChildren(Arrays.asList(tempRoot));
        newInBetweenRule.setRoot(tempRoot);
        return newInBetweenRule;
    }

    private void pruneEmptyRules() {
        for (Iterator<Map.Entry<T, GrammarEntry>> iter = grammar.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<T, GrammarEntry> entry = iter.next();
            if (entry.getValue().getCount() == 0) {
                iter.remove();
                LOGGER.info("Pruned empty entry " + entry.getKey());
            } else {
                // TODO identify empty added rules
                for (Iterator<TSGRule<T>> iterRules = entry.getValue().getRules().keySet().iterator(); iterRules.hasNext(); ) {
                    TSGRule<T> rule = iterRules.next();
                    if (entry.getValue().getRules().size() == 1 && rule.toPreOrderList().size() == 2) {
                        iter.remove();
                        LOGGER.info("Pruned empty added rule" + entry.getKey());
                    } else if (rule.getCount() == 0) {
                        iterRules.remove();
                        LOGGER.info("Pruned single rule " + rule.toString() + " of " + entry.getKey());
                    }
                }
            }
        }
    }

    private void refreshChildrenCount() {
        childrenCount.clear();
        for (Iterator<Map.Entry<T, GrammarEntry>> iter = grammar.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<T, GrammarEntry> entry = iter.next();
            int sumEntry = 0;
            for (TSGRule<T> rule : entry.getValue().getRules().keySet()) {
                if (rule.getCount() > 0) {
                    rule.setCount(rule.getAllOccurrences().size());
                    sumEntry += rule.getCount();
                    for (ITSGNode<T> node : rule.toPreOrderNodeList()) {
                        if (!node.isLeaf()) {
                            MapCounter<T> counter = childrenCount.getOrDefault(getGrammarKey(node), MapCounter.create());
                            for (ITSGNode<T> child : node.getChildren()) {
                                counter.incCountBy(getGrammarKey(child), 1);
                            }
                            childrenCount.put(getGrammarKey(node), counter);
                        }
                    }
                    if (rule.getCreatedRoots() != null) {
                        for (Iterator<ITSGNode<T>> iterRules = rule.getCreatedRoots().iterator(); iterRules.hasNext(); ) {
                            ITSGNode<T> added = iterRules.next();
                            if (grammar.containsKey(getGrammarKey(added))) {
                                // TODO depending on rule, added.getParent may not be equal to rule.getRoot
                                MapCounter<T> counter = childrenCount.getOrDefault(getGrammarKey(added.getParent()), MapCounter.create());
                                counter.incCountBy(getGrammarKey(added), 1);
                                childrenCount.put(getGrammarKey(added.getParent()), counter);
                            } else {
                                iterRules.remove();
                            }
                        }
                    }
                }
            }
            entry.getValue().setCount(sumEntry);
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

        List<ITSGNode<T>> preorderNodes = newOccurrenceToAdd.getOwner().toPreOrderNodeList();
        List<T> preorderLabels = preorderNodes.stream().map(ITreeNode::getLabel).collect(Collectors.toList());
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
        List<Boolean> isNodeLeaf = existingRule.toPreOrderNodeList().stream()
                .map(ITreeNode::isLeaf).collect(Collectors.toList());
        for (TreeOccurrence<T> occurrence : existingRule.getAllOccurrences()) {
            for (int i = 0; i < isNodeLeaf.size(); i++) {
                Boolean isLeaf = isNodeLeaf.get(i);
                if (isLeaf) continue;

                int tid = occurrence.getTID();
                List<Integer> ids = occurrence.getOccurrence();

                IDatabaseNode<T> node = db.findById(tid, ids.get(i));
                // Note: It is possible there's already a rule with such format in the grammar, the occurrence will
                // be appended once again in this case e.g removing rule (A(B)(C)) but in DB it's still (A(B)(C))
                createRuleFromDatabaseNode(tid, node);

                existingRule.removeOccurrence(tid, occurrence);
                //entry.incCountBy(-1);
            }
        }

        // Remove added in-between rules
        for (ITSGNode<T> addedRoot : existingRule.getAddedRoots()) {
            T entryKey = getGrammarKey(addedRoot);
            GrammarEntry toRemove = grammar.get(entryKey);
            if (toRemove == null) continue; // Might've been pruned if no occurrences

            for (TSGRule<T> removeRule : toRemove.getRules().values()) {
                List<ITSGNode<T>> removeChildren = removeRule.toPreOrderNodeList().stream()
                        .filter(e -> e.getParent() != null && e.getParent().getLabel().equals(addedRoot.getLabel()))
                        .collect(Collectors.toList());
                for (ITSGNode<T> node : removeChildren) {
                    T removeKey = getGrammarKey(node);
                    GrammarEntry childEntry = grammar.get(removeKey);

                    // Going through full node list, some entries won't be present
                    // TODO only go through immediate children of the added root
                    if (childEntry != null) {
                        ITSGNode<T> newParent = node.getParent().getParent(); // Will have parent cuz filtered in stream
                        for (TSGRule<T> childRule : childEntry.getRules().values()) {
                            ITSGNode<T> newParentCloned = TSGNode.clone(newParent);
                            newParentCloned.setChildren(new ArrayList<>());
                            newParentCloned.addChild(childRule.getRoot());
                            T childKey = getGrammarKey(childRule.getRoot());
                            GrammarEntry newEntry = grammar.getOrDefault(childKey, new GrammarEntry());
                            TSGRule<T> newRule = newEntry.getRules().get(childRule);
                            if (newRule == null) {
                                newRule = childRule;
                            } else {
                                for (TreeOccurrence<T> occur : childRule.getAllOccurrences()) {
                                    newRule.addOccurrence(occur);
                                }
                            }
                            newEntry.addRule(newRule);
                            //newEntry.incCountBy(newRule.getCount());
                            grammar.put(childKey, newEntry);

                        }
                        //productionsCount -= childEntry.getCount();
                        grammar.remove(removeKey);
                    }
                }
            }

            productionsCount -= toRemove.getCount();
            grammar.remove(entryKey);
        }

        // Note: We can't remove cuz might be an existing rule. We remove the occurrence instead & will be replaced
        // by a new, identical one in that case

        /// Arguably, you can make the updates as you go but it's hard to get them right
        buildPointers();
        refreshChildrenCount();
        pruneEmptyRules();
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

        refreshChildrenCount();
    }

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

        // TODO incremental
        //// Increment counters of children
        //MapCounter<T> counts = childrenCount.getOrDefault(grammarKey, MapCounter.create());
        //children.forEach(c -> counts.incCountBy(getGrammarKey(c), 1));
        //childrenCount.put(grammarKey, counts);

        // Update grammar
        //entry.incCountBy(newRule.getCount());
        productionsCount += newRule.getCount();
        entry.addRule(newRule);
        grammar.put(grammarKey, entry);
        return newRule;
    }

    private void buildPointers() {
        for (Map.Entry<T, GrammarEntry> grammarEntries : this.grammar.entrySet()) {
            GrammarEntry entry = grammarEntries.getValue();
            Map<TSGRule<T>, TSGRule<T>> val = entry.getRules();
            for (TSGRule<T> rule : val.values()) {
                //// Increase count of productions used
                //productionsCount += rule.getCount();

                //// Increase the count of the specific production that is used
                //entry.incCountBy(rule.getCount());

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

    /**
     * Get the key that will be used for this node in the grammar. In other words, in a production A -> X, it's the
     * representation of the lhs, A.
     * @param child
     * @return
     */
    protected abstract T getGrammarKey(ITreeNode<T, ? extends ITreeNode<T, ?>> child);

    protected abstract T getPatternRuleKey(ITSGNode<T> newRoot, int patternId, Integer posInTree, int i);

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
