package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.util.DoubleUtil;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;

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
    private static final String PARENT_ANNOTATION = "^";

    protected String getGrammarKey(ITreeNode<String, ? extends ITreeNode<String, ?>> child) {
        ITreeNode<String, ?> parent = child.getParent();
        String parentLabel = parent != null ? parent.getLabel() : "";
        return (child.getLabel() + PARENT_ANNOTATION + parentLabel).toLowerCase();
    }

    @Override
    protected String getPatternRuleKey(ITSGNode<String> newRoot, int patternId, Integer posInTree, int i) {
        return "(" + patternId + " " + posInTree + " " + i + ")";
    }

    @Override
    public String getDelimiter() {
        return "$";
    }

    @Override
    public double getModelCodingLength() {
        double res = 0.0;
        for (Map.Entry<String, GrammarEntry> grammarEntries : grammar.entrySet()) {
            // For each production X -> Y, add the coding length of X being the root
            GrammarEntry entry = grammarEntries.getValue();
            // If this entry is not used anymore, skip
            if (entry.getCount() == 0) continue;

            double entryCodingLength = getModelEntryCodingLength(entry);
            res += entryCodingLength;
            System.out.println(entryCodingLength + " bits for " + grammarEntries.getKey() + " -> " + entry);
        }
        return res;
    }

    //private double getModelEntryCodingLength(GrammarEntry entry) {
    //    double res = 0.0;
    //    double productionsCountLog = DoubleUtil.log2(this.productionsCount);
    //    // Compute the coding length of this tree rule
//
    //    // Add the probability of having this symbol as root
    //    // Note: You add it only once as every rule derived from this root will have the same root
    //    double productionCount = entry.getCount();
    //    double rootP = -(DoubleUtil.log2(productionCount) - productionsCountLog);
    //    res += rootP;
//
    //    for (TSGRule<String> rule : entry.getRules().values()) {
    //        // If this entry is not used anymore, skip
    //        if (rule.getCount() == 0) continue;
    //        // Productions are in a tree format i.e. X -> (X(A_1(...))...(A_n(...)))
    //        // Traverse the tree in pre order and, at each node, compute the probability
    //        // of applying this specific production at this node
    //        PeekableIterator<ITSGNode<String>> dfsIterator = Util.asPreOrderIterator(
    //                Util.asIterator(rule.getRoot()), (ITSGNode<String> node) -> node.getChildren().iterator());
    //        dfsIterator.next();
    //        while (dfsIterator.hasNext()) {
    //            ITSGNode<String> currentNode = dfsIterator.peek();
    //            if (!currentNode.isLeaf()) { // Only consider productions
    //                // Build the key that was used to put this production in grammar
    //                String key = getGrammarKey(currentNode);
    //                GrammarEntry currentEntry = grammar.get(key);
//
    //                // Create a new rule with only children of the given node and use it as a key in our grammar
    //                TSGRule<String> ruleAsKey = TSGRule.create(getDelimiter());
    //                ruleAsKey.setRoot(TSGNode.createFromWithChildren(currentNode));
    //                TSGRule<String> ruleInGrammar = currentEntry.getRules().get(ruleAsKey);
    //                // Should not be null because of the assumptions made (i.e. there should be such production)
    //                // TODO Otherwise, need to go through the rules in the GrammarEntry and find where it matches
    //                assert (ruleInGrammar != null);
//
    //                // Compute probability of this production based on _initial_ counts (i.e. at start, without any
    //                // new rules added)
    //                double productionP = -(DoubleUtil.log2(ruleInGrammar.getInitialCount())
    //                        - DoubleUtil.log2(entry.getInitialCount()));
    //                System.out.println(productionP + " bits for " + rule);
    //                res += productionP;
    //                assert (!Double.isNaN(res));
    //            }
    //            dfsIterator.next();
    //        }
    //    }
    //    return res;
    //}

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
            PeekableIterator<ITSGNode<String>> dfsIterator = Util.asPreOrderIterator(
                    Util.asSingleIterator(rule.getRoot()), (ITSGNode<String> node) -> node.getChildren().iterator());
            dfsIterator.next();
            while (dfsIterator.hasNext()) {
                ITSGNode<String> currentNode = dfsIterator.peek();
                if (!currentNode.isLeaf() && !currentNode.isRoot()) { // Only consider productions
                    // For each node A_i, get count(X->A_i) and count(X) based on the distribution in the data
                    //TODO FIX CLONE
                    String key = getGrammarKey(currentNode);
                    String parentKey = getGrammarKey(currentNode.getParent());
                    assert (parentKey != null);
                    MapCounter<String> parentCounter = childrenCount.get(parentKey);
                    assert (parentCounter != null);
                    Integer prodCount = parentCounter.getCountFor(key);
                    assert (prodCount != 0);
                    Integer parentCount = parentCounter.getTotal();

                    double productionP = -(DoubleUtil.log2(prodCount)
                            - DoubleUtil.log2(parentCount));
                    //System.out.println(productionP + " bits for " + rule);
                    res += productionP;
                    assert (!Double.isNaN(res));
                }
                dfsIterator.next();
            }
        }
        return res;
    }

    @Override
    public double getDataCodingLength() {
        double res = 0.0;
        for (Map.Entry<String, GrammarEntry> grammarEntry : grammar.entrySet()) {
            double productionCount = grammarEntry.getValue().getCount();
            for (TSGRule<String> rule : grammarEntry.getValue().getRules().values()) {
                double log2P = -(DoubleUtil.log2(rule.getCount()) - DoubleUtil.log2(productionCount));
                System.out.println(log2P + " bits for " + rule);
                res += log2P;
                assert (!Double.isNaN(res));
            }

        }
        return res;
    }
}
