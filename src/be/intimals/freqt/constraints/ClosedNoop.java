package be.intimals.freqt.constraints;

import be.intimals.freqt.core.Extension;
import be.intimals.freqt.core.NodeFreqT;
import be.intimals.freqt.core.Projected;
import java.util.*;
import javafx.util.Pair;

/**
 * Methods used for mining closed patterns when the closedness property is disabled.
 */
public class ClosedNoop implements IClosed {
    public ClosedNoop() {}

    /**
     * {@inheritDoc}
     */
    public Set<Extension> buildBlanket(Map<String, Vector<String>> grammar,
                                              Set<String> rootWhitelist,
                                              Map<String, Set<String>> childrenBlacklist,
                                              Projected projected, Set<Pair<Integer, String>> right) {
        return new HashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    public void pruneClosedFreq1(Set<String> rootWhitelist, Map<String, Projected> candidate) {}

    /**
     * {@inheritDoc}
     */
    public boolean pruneOccurrenceMatchSet(Projected projected,
                                                  Set<Extension> blanket, Set<Pair<Integer, String>> right) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareSupportMatch(Projected projected, Set<Extension> blanket) {
        return true;
    }
}
