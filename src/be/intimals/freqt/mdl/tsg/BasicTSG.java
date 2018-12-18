package be.intimals.freqt.mdl.tsg;

import be.intimals.freqt.mdl.common.ITreeNode;
import be.intimals.freqt.util.DoubleUtil;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;

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
        // TODO DEBUG
        LOGGER.setUseParentHandlers(false);
    }
    private static final String PARENT_ANNOTATION = "^";

    protected String getGrammarKey(ITreeNode<String, ? extends ITreeNode<String, ?>> child) {
        return child.getLabel().toLowerCase();
    }

    @Override
    protected String getPatternRuleKey(int patternId, Integer posInTree, int i) {
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
            LOGGER.info(entryCodingLength + " bits for " + grammarEntries.getKey() + " -> " + entry);
        }
        LOGGER.info("TOTAL MODEL: " + " bits for " + res);
        return res;
    }

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
            List<ITSGNode<String>> nodes = rule.toPreOrderFilteredNodeList();
            for (ITSGNode<String> currentNode : nodes) {
                if (!currentNode.isLeaf()) { // Only consider productions
                    // For each node A_i, get count(X->A_i) and count(X) based on the distribution in the data
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
                }
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
                LOGGER.info(log2P + " bits for " + rule);
                res += log2P;
                assert (!Double.isNaN(res));
            }

        }
        LOGGER.info("TOTAL DATA: " + res);
        return res;
    }
}
