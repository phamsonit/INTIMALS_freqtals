package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.util.DoubleUtil;
import be.intimals.freqt.util.Triple;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple probabilistic model where each symbol is parent annotated
 * and every production is modeled as a sequence of N children.
 * The model is encoded using the distribution over the productions in
 * the original grammar.
 * The data uses additional production rules in between pattern rules.
 */
public class BasicTSG extends ATSG<String> {
    private static final Logger LOGGER = Logger.getLogger(BasicTSG.class.getName());
    static {
        // TODO debug, uncomment to disable
        LOGGER.setUseParentHandlers(false);
    }

    private static final String PARENT_ANNOTATION = "^";

    // Accumulator of coding length for entry in grammar
    private double modelEntryAcc = 0.0;
    // Counts number of nodes in an elementary tree
    private int modelEntryNodesCount = 0;

    protected String getGrammarKey(ITreeNode<String, ? extends ITreeNode<String, ?>> child) {
        //return child.getLabel().toLowerCase(); // moved to database loading because big overhead here
        return child.getLabel();
    }

    @Override
    protected String getPatternRuleKey(int patternId, int posInTree, int i) {
        return "(" + patternId + " " + posInTree + " " + i + ")";
    }

    @Override
    protected Triple<Integer, Integer, Integer> getPatternRuleKey(String patternKey) {
        // Reverse operation of getPatternRuleKey(int, int, int)
        assert (patternKey.contains("("));
        assert (patternKey.contains(")"));
        String trimmed = patternKey.substring(1, patternKey.length() - 1);
        String[] values = trimmed.split(" ");
        Integer patternId = Integer.valueOf(values[0]);
        Integer posInTree = Integer.valueOf(values[1]);
        Integer posInSiblings = Integer.valueOf(values[2]);
        return new Triple<>(patternId, posInTree, posInSiblings);
    }

    @Override
    public String getDelimiter() {
        return "$";
    }

    @Override
    public double getModelCodingLength() {
        double res = 0.0;
        // Uniform encoding for roots
        double rootP = -(DoubleUtil.log2(1) - DoubleUtil.log2(this.grammarSize));

        for (Map.Entry<String, GrammarEntry> grammarEntries : grammar.entrySet()) {
            // For each production X -> Y, add the coding length of X being the root
            GrammarEntry entry = grammarEntries.getValue();
            // If this entry is not used anymore, skip
            if (entry.getCount() == 0) continue;

            //double entryCodingLength = getModelEntryCodingLength(entry);
            double entryCodingLength = getModelEntryCodingLengthImproved(entry, rootP);
            res += entryCodingLength;
            LOGGER.info(entryCodingLength + " bits for " + grammarEntries.getKey() + " -> " + entry);
        }
        LOGGER.info("TOTAL MODEL: " + " bits for " + res);
        return res;
    }

    /*
    private double getModelEntryCodingLength(GrammarEntry entry) {
        double res = 0.0;
        double productionsCountLog = DoubleUtil.log2(this.productionsCount);
        // Compute the coding length of this tree rule

        // Add the probability of having this symbol as root
        // Note: You add it only once as every rule derived from this root will have the same root
        double currentProdCount = entry.getCount();
        double rootP = -(DoubleUtil.log2(currentProdCount) - productionsCountLog);

        for (TSGRule<String> rule : entry.getRules().values()) {
            // If this entry is not used anymore, skip
            if (rule.getCount() == 0) continue;
            res += rootP;
            // Productions are in a tree format i.e. X -> (X(A_1(...))...(A_n(...)))
            // Traverse the tree in pre order and, at each node, compute the probability
            // of applying this specific production at this node
            double mainLength = 0.0;
            List<ITSGNode<String>> nodes = rule.toPreOrderFilteredNodeList();
            for (ITSGNode<String> currentNode : nodes) {
                if (!currentNode.isLeaf()) { // Only consider productions
                    // For each node A_i, get count(X->A_i) and count(X) based on the distribution in the data
                    mainLength += getCodingLengthForChildren(currentNode);
                }
            }
            // Added roots have they own grammar entry but they do not appear in the rule tree
            // They do participate in the model length so add their coding length too
            double inBetweenLength = 0.0;
            if (rule.hasAddedRoots()) {
                for (Pair<ITSGNode<String>, ITSGNode<String>> parentChild : rule.getAddedRoots()) {
                    inBetweenLength += getCodingLengthForSingleChild(parentChild.getKey(), parentChild.getValue());
                }
            }
            res += mainLength;
            res += inBetweenLength;
            LOGGER.info(rule.toString() + " ROOT: " + rootP + " MAIN: " + mainLength + "ADDED: " + inBetweenLength);
        }
        return res;
    }
    */

    private double getModelEntryCodingLengthImproved(GrammarEntry entry, double rootP) {
        double res = 0.0;
        for (TSGRule<String> rule : entry.getRules().values()) {
            // If this entry is not used anymore, skip
            if (rule.getCount() == 0) continue;

            this.modelEntryAcc = 0.0;
            this.modelEntryNodesCount = 0;
            ITSGNode<String> fullTreeRoot = buildRootWithAddedRoots(rule);
            traverseModelChildren(fullTreeRoot);

            // TODO could also encode rootP once and number of rules for entry
            res += rootP; // Length for root encoding
            res += this.modelEntryAcc; // Sum of list encodings of all children
            res += this.modelEntryNodesCount; // 1 bit per node, internal/leaf
            LOGGER.info(rule.toString()
                    + " ROOT: " + rootP + " ACCU: " + this.modelEntryAcc + " NODES: " + this.modelEntryNodesCount);
        }
        return res;
    }

    private void traverseModelChildren(ITSGNode<String> currentNode) {
        this.modelEntryNodesCount++; // Count visited nodes for current tree
        if (currentNode.isLeaf()) return;

        // Those counts give the independent probability of each symbol
        String key = getGrammarKey(currentNode);
        MapCounter<String> counter = childrenCount.get(key);
        assert (counter != null);
        Integer parentCount = counter.getTotal();
        // Assert: if counts are properly done, those vars shouldn't be null

        // Code for lists: encode child symbol + either stop or not for each child
        for (int i = 0; i < currentNode.getChildrenCount(); i++) {
            ITSGNode<String> child = currentNode.getChildAt(i);

            String childKey = getGrammarKey(child);
            Integer prodCount = counter.getCountFor(childKey);
            assert (prodCount != 0);

            // Child coding length, indep proba of child symbol appearing
            double productionP = -(DoubleUtil.log2(prodCount) - DoubleUtil.log2(parentCount));

            // Stop/continue:
            double stopContinueP;
            if (i == currentNode.getChildrenCount() - 1) {
                // Last child, encode stop
                stopContinueP = -(DoubleUtil.log2(counter.getGeometricEstimator()));
            } else {
                // Encode continue
                stopContinueP = -(DoubleUtil.log2(1 - counter.getGeometricEstimator()));
            }
            this.modelEntryAcc += productionP;
            this.modelEntryAcc += stopContinueP;

            traverseModelChildren(child);
        }

    }

    /*
    private double getCodingLengthForChildren(ITSGNode<String> currentNode) {
        double res = 0.0;
        String key = getGrammarKey(currentNode);
        MapCounter<String> counter = childrenCount.get(key);
        assert (counter != null);
        // Assert: if counts are properly done, those vars shouldn't be null
        Integer parentCount = counter.getTotal();

        for (ITSGNode<String> child : currentNode.getChildren()) {
            String childKey = getGrammarKey(child);
            Integer prodCount = counter.getCountFor(childKey);
            assert (prodCount != 0);

            double productionP = -(DoubleUtil.log2(prodCount)
                    - DoubleUtil.log2(parentCount));
            res += productionP;
            assert (!Double.isNaN(res));
        }
        return res;
    }

    private double getCodingLengthForSingleChild(ITSGNode<String> parent, ITSGNode<String> child) {
        // TODO refactor with .ForChildren
        double res = 0.0;
        String key = getGrammarKey(parent);
        MapCounter<String> counter = childrenCount.get(key);
        assert (counter != null);
        Integer parentCount = counter.getTotal();

        String childKey = getGrammarKey(child);
        Integer prodCount = counter.getCountFor(childKey);
        assert (prodCount != 0);

        double productionP = -(DoubleUtil.log2(prodCount)
                - DoubleUtil.log2(parentCount));
        res += productionP;
        assert (!Double.isNaN(res));

        return res;
    }
    */

    @Override
    public double getDataCodingLength() {
        double res = 0.0;
        for (Map.Entry<String, GrammarEntry> grammarEntry : grammar.entrySet()) {
            double productionCount = grammarEntry.getValue().getCount();
            for (TSGRule<String> rule : grammarEntry.getValue().getRules().values()) {
                double log2P = -(DoubleUtil.log2(rule.getCount()) - DoubleUtil.log2(productionCount));
                LOGGER.info(log2P + " bits for " + rule);
                res += log2P;
                assert (!Double.isNaN(res));
            }

        }
        LOGGER.info("TOTAL DATA: " + res);
        return res;
    }

    /*
    public List<ITSGNode<String>> toPreOrderWithAddedRoots(TSGRule<String> rule, boolean showBacktrack) {
        if (!rule.hasAddedRoots()) return rule.toPreOrderFilteredNodeList();

        List<ITSGNode<String>> res = new ArrayList<>();
        // Clone the root of the rule because we will add the rules in-between as children
        ITSGNode<String> fullTreeRoot = TSGNode.clone(rule.getRoot());
        TSGRule<String> tempRule = TSGRule.create(getDelimiter());
        tempRule.setRoot(fullTreeRoot);
        List<ITSGNode<String>> preorderNodes = tempRule.toPreOrderFilteredNodeList();

        for (Pair<ITSGNode<String>, ITSGNode<String>> parentChild : rule.getAddedRoots()) {
            ITSGNode<String> child = parentChild.getValue();
            Triple<Integer, Integer, Integer> patternKey = getPatternRuleKey(child.getLabel());
            int posParent = patternKey.getSecond();
            int posInSiblings = patternKey.getThird();
            ITSGNode<String> parentNode = preorderNodes.get(posParent);
            parentNode.getChildren().add(posInSiblings, child);
        }

        // fullTreeRoot now has added roots inside its tree, do the traversal
        ITSGNode<String> backtrack = TSGNode.create(getDelimiter());
        PeekableIterator<ITSGNode<String>> dfsIterator = showBacktrack ? Util.asPreOrderIteratorWithBacktrack(
                Util.asSingleIterator(fullTreeRoot), (ITSGNode<String> node) -> node.getChildren().iterator(), backtrack) :
                Util.asPreOrderIterator(
                        Util.asSingleIterator(fullTreeRoot), (ITSGNode<String> node) -> node.getChildren().iterator());
        dfsIterator.next();
        while (dfsIterator.hasNext()) {
            ITSGNode<String> next = dfsIterator.peek();
            res.add(next);
            dfsIterator.next();
        }
        return res;
    }
    */

    private ITSGNode<String> buildRootWithAddedRoots(TSGRule<String> rule) {
        if (!rule.hasAddedRoots()) return rule.getRoot();

        List<ITSGNode<String>> res = new ArrayList<>();
        // Clone the root of the rule because we will add the rules in-between as children
        ITSGNode<String> fullTreeRoot = TSGNode.clone(rule.getRoot());
        TSGRule<String> tempRule = TSGRule.create(getDelimiter());
        tempRule.setRoot(fullTreeRoot);
        List<ITSGNode<String>> preorderNodes = tempRule.toPreOrderFilteredNodeList();

        for (Pair<ITSGNode<String>, ITSGNode<String>> parentChild : rule.getAddedRoots()) {
            ITSGNode<String> child = parentChild.getValue();
            Triple<Integer, Integer, Integer> patternKey = getPatternRuleKey(child.getLabel());
            int posParent = patternKey.getSecond();
            int posInSiblings = patternKey.getThird();
            ITSGNode<String> parentNode = preorderNodes.get(posParent);
            parentNode.getChildren().add(posInSiblings, child);
        }

        return fullTreeRoot;
    }
}
