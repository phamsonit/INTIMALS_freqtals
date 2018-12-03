package be.intimals.freqt.constraints;

import be.intimals.freqt.core.Extension;
import be.intimals.freqt.core.NodeFreqT;
import be.intimals.freqt.core.Projected;
import java.util.*;
import javafx.util.Pair;

/**
 * Methods used for mining closed patterns.
 */
public class Closed implements IClosed {
    private Vector<Vector<NodeFreqT>> transactions;

    public Closed(Vector<Vector<NodeFreqT>> newTransactions) {
        transactions = newTransactions;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Extension> buildBlanket(Set<String> rootWhitelist,
                                              Map<String, Set<String>> childrenBlacklist,
                                              Projected projected, Set<Pair<Integer, String>> right) {
        // Optimization: in theory, first checking root expansions (-1) is easier
        // but sorting the blanket appears more expensive in practice
        // Set<Extension> blanket = new TreeSet<>(Comparator.comparingInt(Extension::getPos)
        // .thenComparing(Extension::getLabel));
        Set<Extension> blanket = new HashSet<>(); // (-1 is root extension)

        for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
            Set<Integer> rightmost = new HashSet<>();

            int tid = projected.getProjectLocation(i).getLocationId();
            Vector<NodeFreqT> currentT = transactions.elementAt(tid);
            int rightmostNode = projected.getProjectLocation(i).getLocationPos();
            List<Integer> occurrence = projected.getProjectLocation(i).getLocationList();
            assert (occurrence.size() >= 1);

            // Try root extension
            int rootPos = occurrence.get(0);
            int rootExtension = currentT.elementAt(rootPos).getNodeParent();
            if (rootExtension != -1 && rootWhitelist.contains(currentT.elementAt(rootExtension).getNodeLabel())) {
                // Note: root extension must be allowed in root whitelist which isn't exact closed definition,
                //       children blacklist doesn't intervene here
                blanket.add(new Extension(-1, currentT.elementAt(rootExtension).getNodeLabel(), false));
            }

            // TODO refactor transactions into actual class
            // Try extensions of nodes resulting in a new rightmost path
            // Note: order of adding to set is important,
            //       first add the rightmost extensions (in case the same extension is possible)
            int currentPos = rightmostNode;
            int start = currentT.elementAt(rightmostNode).getNodeChild();
            int previousOrderedSibling = -1;
            while (currentPos != -1
                    && currentPos != rootExtension) { // Don't expand when no parent or past parent of pattern root
                rightmost.add(currentPos);

                // TODO refactor ordering as String
                int extensionPrevSibling = occurrence.indexOf(previousOrderedSibling);

                int idx = occurrence.indexOf(currentPos);
                Set<String> bannedChildren = getBannedTokens(childrenBlacklist, currentT.elementAt(currentPos));
                for (int next = start; next != -1; next = currentT.elementAt(next).getNodeSibling()) {
                    if (!bannedChildren.contains(currentT.elementAt(next).getNodeLabel())) {
                        blanket.add(new Extension(idx,
                                currentT.elementAt(next).getNodeLabel(), true, extensionPrevSibling));
                        right.add(new Pair<>(idx, currentT.elementAt(next).getNodeLabel()));
                    }
                }
                start = currentT.elementAt(currentPos).getNodeSibling();
                previousOrderedSibling = currentPos;
                currentPos = currentT.elementAt(currentPos).getNodeParent();
            }

            // Try extensions of nodes NOT resulting in a new rightmost path
            for (Integer n : occurrence) {
                if (n != rightmostNode) {
                    previousOrderedSibling = -1;
                    int nextChild = currentT.elementAt(n).getNodeChild();
                    int idx = occurrence.indexOf(n);
                    Set<String> bannedChildren = getBannedTokens(childrenBlacklist, currentT.elementAt(n));
                    while (nextChild != -1
                            // If expanding a node on rightmost path, don't expand past his child on the rightmost path
                            && !(rightmost.contains(n) && rightmost.contains(nextChild))) {
                        if (!occurrence.contains(nextChild)
                                && (!bannedChildren.contains(currentT.elementAt(nextChild).getNodeLabel()))) {
                            // If part of the occurrence or in children blacklist, skip this extension
                            blanket.add(new Extension(idx,
                                    currentT.elementAt(nextChild).getNodeLabel(), false,
                                    previousOrderedSibling));
                        }
                        int posInOccurrence = occurrence.indexOf(nextChild);
                        previousOrderedSibling = (posInOccurrence == -1 ? previousOrderedSibling : posInOccurrence);
                        nextChild = currentT.elementAt(nextChild).getNodeSibling();
                    }
                }
            }
        }
        return blanket;
    }

    private Set<String> getBannedTokens(Map<String, Set<String>> childrenBlacklist, NodeFreqT node) {
        Set<String> bannedChildren = childrenBlacklist.getOrDefault(node.getNodeLabel(), new HashSet<>());
        bannedChildren.add("token");
        return bannedChildren;
    }

    /**
     * {@inheritDoc}
     */
    public void pruneClosedFreq1(Set<String> rootWhitelist, Map<String, Projected> candidate) {
        Iterator<Map.Entry<String, Projected>> iter = candidate.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Projected> entry = iter.next();
            Projected projected = entry.getValue();
            String parentLabel = null;
            boolean uniqueParent = true;

            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int tid = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();

                // Freq1 should have single nodes
                assert (projected.getProjectLocation(i).getLocationList().size() == 1);

                int parentPos = transactions.elementAt(tid).elementAt(pos).getNodeParent();
                if (parentPos == -1) {
                    uniqueParent = false;
                    break; // Can't prune, no parent
                }

                String newParentLabel = transactions.elementAt(tid).elementAt(parentPos).getNodeLabel();
                if (parentLabel == null) {
                    parentLabel = newParentLabel; // First parent spotted
                } else if (!parentLabel.equals(newParentLabel)) {
                    uniqueParent = false;
                    break; // Can't prune, different parent exists
                }
            }

            if (uniqueParent && rootWhitelist.contains(parentLabel)) { // If not in root whitelist, can't prune
                iter.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pruneOccurrenceMatchSet(Projected projected,
                                                  Set<Extension> blanket, Set<Pair<Integer, String>> right) {
        // Note: we don't build the whole set, just checking if empty
        boolean res = false;
        Set<Extension> debug = new HashSet<>(); // TODO debug
        for (Extension vertex : blanket) {
            if (vertex.isRightmost()) {
                continue; // occurrence match set doesn't consider new rightmost extensions
            }
            if (right.contains(new Pair<>(vertex.getPos(), vertex.getLabel()))) {
                continue; // extension possible as both rightmost & not rightmost, can't prune
            }

            String newVertexLabel = vertex.getLabel();
            boolean allFound = true; // New extension must be present in every occurrence
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int tid = projected.getProjectLocation(i).getLocationId();
                Vector<NodeFreqT> currentT = transactions.elementAt(tid);
                List<Integer> occurrence = projected.getProjectLocation(i).getLocationList();
                boolean found = false; // Checking current occurrence

                if (vertex.getPos() == -1) { // Root extension
                    int parentPos = currentT.elementAt(occurrence.get(0)).getNodeParent();
                    if (parentPos != -1 && currentT.elementAt(parentPos).getNodeLabel().equals(newVertexLabel)) {
                        found = true;
                    }
                } else {
                    found = findExtensionMatch(vertex, currentT, occurrence);
                }
                if (!found) { // Not a valid extension for current occurrence -> not in occurrence match set -> skip
                    allFound = false;
                    break;
                }
            }

            if (allFound) { // Extension is possible in every occurrence -> in occurrence match set -> can prune
                res = true;
                debug.add(vertex); // TODO debug, remove break if debugging set
                //System.out.print("WITNESS: "+debug+ " ");
                break;
            }
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareSupportMatch(Projected projected, Set<Extension> blanket) {
        Set<Integer> projectedCover = new HashSet<>();
        boolean initProjectedCover = false;
        boolean res = true; // assume closed

        for (Extension vertex : blanket) {
            String newVertexLabel = vertex.getLabel();
            Set<Integer> extensionCover = new HashSet<>();

            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int tid = projected.getProjectLocation(i).getLocationId();
                Vector<NodeFreqT> currentT = transactions.elementAt(tid);
                if (!initProjectedCover) {
                    projectedCover.add(tid); // Cover of pattern
                }
                List<Integer> occurrence = projected.getProjectLocation(i).getLocationList();

                if (vertex.getPos() == -1) { // Root extension
                    int parentPos = currentT.elementAt(occurrence.get(0)).getNodeParent();
                    if (parentPos != -1 && currentT.elementAt(parentPos).getNodeLabel().equals(newVertexLabel)) {
                        extensionCover.add(tid);
                    }
                } else {
                    if (findExtensionMatch(vertex, currentT, occurrence)) {
                        extensionCover.add(tid);
                    }
                }
            }
            initProjectedCover = true; // Went through every occurrence, we know the cover of the pattern
            if (projectedCover.size() == extensionCover.size()) {
                res = false; // Same cover -> B^SM_t not empty -> t is not closed
                break;
            }
        }
        // if B_t is empty, by definition B^SM_t is empty
        return blanket.isEmpty() || res;
    }

    private static boolean findExtensionMatch(Extension vertex, Vector<NodeFreqT> currentT, List<Integer> occurrence) {
        boolean found = false;
        // Depending on TID, the id of the extended node is different but it's position in
        // the occurrence list is always the same. The extended node id is thus occurrence[extension.getPos]
        NodeFreqT extendedNode = currentT.elementAt(occurrence.get(vertex.getPos()));
        String newVertexLabel = vertex.getLabel();
        int childPos = extendedNode.getNodeChild();
        int prevSiblingInOccurrence = -1;
        while (childPos != -1) {
            if (occurrence.contains(childPos)) {
                prevSiblingInOccurrence = childPos;
            } else if (currentT.elementAt(childPos).getNodeLabel().equals(newVertexLabel)
                    && (vertex.getPreviousSibling() == -1 || (prevSiblingInOccurrence != -1
                    && occurrence.get(vertex.getPreviousSibling()) == prevSiblingInOccurrence))) {
                // Not already in occurrence, both labels match
                // and if ordered, the previous sibling in occurrence exists and matches labels
                found = true;
                break;
            }
            childPos = currentT.elementAt(childPos).getNodeSibling();
        }
        return found;
    }
}
