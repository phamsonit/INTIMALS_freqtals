package be.intimals.freqt.constraints;

import be.intimals.freqt.core.Extension;
import be.intimals.freqt.core.Projected;
import java.util.*;
import javafx.util.Pair;

public interface IClosed {
    /**
     * Build the set of subtrees of a tree t such that they have one more vertex than t
     * (cfr. 3.2 The CMTreeMiner Algorithm (Chi et al., 2004))
     * @param rootWhitelist
     * @param childrenBlacklist
     * @param projected
     * @param right
     * @return
     */
    Set<Extension> buildBlanket(Set<String> rootWhitelist,
                                Map<String, Set<String>> childrenBlacklist,
                                Projected projected, Set<Pair<Integer, String>> right);

    /**
     * Prune 1-length frequent subtrees based on the fact that if a node k has as parent a node p in every occurrence in
     * every transaction, node k can't be closed and can be pruned. The parent node must be in the whitelist.
     * (cfr 4.2.1 Search space reduction using the backward scan (Kutty et al., 2007))
     * @param rootWhitelist
     * @param candidate
     */
    void pruneClosedFreq1(Set<String> rootWhitelist, Map<String, Projected> candidate);

    /**
     * Returns whether the set of support-matched extensions is empty. If it's empty, the pattern from which the blanket
     * was built is closed.
     * (cfr. 3.2 B^SM_t The CMTreeMiner Algorithm (Chi et al., 2004))
     * @param projected
     * @param blanket
     * @return
     */
    boolean compareSupportMatch(Projected projected, Set<Extension> blanket);

    /**
     * Returns whether the set of occurrence matching subtrees from the given blanket is empty or not.
     * We do not consider rightmost expansions in this set. If the set isn't empty,
     * the pattern from which the blanket was built can be pruned.
     * @param projected
     * @param blanket
     * @param right
     * @return
     */
    boolean pruneOccurrenceMatchSet(Projected projected, Set<Extension> blanket, Set<Pair<Integer, String>> right);
}
