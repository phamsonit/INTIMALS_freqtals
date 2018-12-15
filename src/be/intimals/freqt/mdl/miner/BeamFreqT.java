package be.intimals.freqt.mdl.miner;

import be.intimals.freqt.Config;
import be.intimals.freqt.constraints.Closed;
import be.intimals.freqt.constraints.ClosedNoop;
import be.intimals.freqt.constraints.IClosed;
import be.intimals.freqt.core.*;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.mdl.tsg.ATSG;
import be.intimals.freqt.mdl.tsg.ITSGNode;
import be.intimals.freqt.mdl.tsg.TSGNode;
import be.intimals.freqt.mdl.tsg.TSGRule;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.LineOutput;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.util.KBest;
import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.SearchStatistics;
import be.intimals.freqt.util.Util;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class BeamFreqT {
    private static final Logger LOGGER = Logger.getLogger(BeamFreqT.class.getName());
    public static char uniChar = '\u00a5';// Japanese Yen symbol

    private static Config config;
    private AOutputFormatter output;
    private Vector<String> pattern;
    private Vector<Vector<NodeFreqT>> transactions = new Vector<>();
    private ATSG<String> tsg;
    private Set<String> listRootLabel = new HashSet<>(); // Possible roots of trees
    private Set<String> listNodeList = new HashSet<>(); // Which nodes should be modeled as lists (not fixed N children)
    private IClosed closed;
    private Map<Integer, Set<Integer>> coveredByTid = new HashMap<>(); // Which nodes are already explained by a tree rule
    private int BEAM_SIZE;
    private double bestLength;

    public SearchStatistics stats;

    private BeamFreqT(Database<String> db, ATSG<String> tsg, int beamSize) {
        loadDatabase(db);
        assertDBConsistent(db);
        this.tsg = tsg;
        this.BEAM_SIZE = beamSize;
        this.bestLength = tsg.getCodingLength();
    }

    public static BeamFreqT create(Database<String> db, ATSG<String> tsg, int beamSize) {
        return new BeamFreqT(db, tsg, beamSize);
    }

    /**
     * Prune candidates based on support. Also prunes if all occurrences of candidates are already covered.
     *
     * @param candidate
     */
    private void prune(Map<String, Projected> candidate) {
        Iterator<Map.Entry<String, Projected>> iter = candidate.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, Projected> entry = iter.next();
            int sup = entry.getValue().getProjectedSupport();
            if (sup < config.getMinSupport()) {
                iter.remove();
            } else {
                pruneAlreadyCovered(iter, entry.getValue());
            }
        }
    }

    private boolean pruneAlreadyCovered(Iterator<Map.Entry<String, Projected>> iter, Projected projected) {
        for (int i = 0; i < projected.getProjectLocationSize(); i++) {
            Location loc = projected.getProjectLocation(i);
            if (coveredByTid.containsKey(loc.getLocationId())) {
                Set<Integer> covered = coveredByTid.get(loc.getLocationId());
                for (Integer val : loc.getLocationList()) {
                    if (covered.contains(val)) {
                        // This node is already covered by another rule, we don't allow overlap, remove
                        projected.removeLocation(i);
                        LOGGER.info("REMOVED location, already covered: "
                                + transactions.get(loc.getLocationId()).get(val).getNodeLabel() + " - " + val);
                        // No longer enough support, remove from candidates
                        if (projected.getProjectLocationSize() < config.getMinSupport()) {
                            iter.remove();
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }

    private void report(Vector<String> pat, Projected projected, Set<Extension> blanket) {
        boolean isClosed = blanket == null || closed.compareSupportMatch(projected, blanket);
        // closed if null blanket, closedness disabled or actually closed when enabled
        if (isClosed) {
            output.report(pat, projected);
            //addPatternToTSG(pat, projected);
            //double modelLength = tsg.getModelCodingLength();
            //double dataLength = tsg.getDataCodingLength();
            //System.out.println("Model: " + tsg.getModelCodingLength());
            //System.out.println("Data : " + tsg.getDataCodingLength());
            //System.out.println("Sum : " + (modelLength + dataLength));
            stats.incClosed();
        } else {
            stats.incNotClosed();
        }
    }

    private Pair<Double, TSGRule<String>> addPatternToTSG(boolean tryAndRemove, Vector<String> pat, Projected projected) {
        TSGRule<String> rule = buildRuleFromPattern(pat, projected, !tryAndRemove);
        if (rule.getCount() == 0) return null;
        tsg.addRule(rule);
        double res = tsg.getCodingLength();
        if (tryAndRemove) {
            tsg.removeRule(rule);
        }
        return new Pair<>(res, rule);
    }

    private TSGRule<String> buildRuleFromPattern(Vector<String> pat, Projected projected, boolean updateCovered) {
        assert (pat.size() >= 3);
        ITSGNode<String> patternParentRoot = TSGNode.buildFromList(pat.toArray(new String[]{}), ")");
        ITSGNode<String> patternRoot = patternParentRoot.getChildAt(0);
        TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
        rule.setRoot(patternRoot);
        Map<Integer, Set<Integer>> tempCovered = new HashMap<>();
        for (int i = 0; i < projected.getProjectLocationSize(); i++) {
            Location loc = projected.getProjectLocation(i);
            // First element in occurrence is parent position that we don't need
            List<Integer> occurrence = new ArrayList<>(loc.getLocationList().subList(1, loc.getLocationList().size()));
            // If we add a rule definitively, we should update the covered nodes. Otherwise, we keep a temp copy and
            // update it on-demand
            Set<Integer> covered = updateCovered ? coveredByTid.getOrDefault(loc.getLocationId(), new HashSet<>()) :
                    tempCovered.getOrDefault(loc.getLocationId(),
                            new HashSet<>(coveredByTid.getOrDefault(loc.getLocationId(), new HashSet<>())));
            // NO overlap, remove overlapping locations
            // (Note: IF relaxing this constraint, need to change how rules are added as after handling one occurrence,
            // some overlapping nodes in the next occurrence will already point to the new rule)
            boolean removed = false;
            for (Integer val : occurrence) {
                if (covered.contains(val)) {
                    // This node is already covered by another rule, we don't allow overlap, remove
                    //LOGGER.info("SKIP location, would cover node twice: "
                    //        + transactions.get(loc.getLocationId()).get(val).getNodeLabel() + " - " + val);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                rule.addOccurrence(loc.getLocationId(), occurrence);
                covered.addAll(occurrence);
                if (updateCovered) {
                    coveredByTid.putIfAbsent(loc.getLocationId(), covered);
                } else {
                    tempCovered.putIfAbsent(loc.getLocationId(), covered);
                }
            }
        }
        return rule;
    }


    private void project(Projected projected, boolean shouldReport) {
        try {
            stats.incProject();
            debugPrintStats(projected);

            // Closed: check if can prune & keep blanket for closed check
            Set<Pair<Integer, String>> rightExtensions = new HashSet<>();
            Set<Extension> blanket = closed.buildBlanket(listRootLabel, new HashMap<>(),
                    projected, rightExtensions);
            // Build B^SM_t and B^OM_t which define if t is a closed subtree and if we can prune the whole branch
            boolean canPrune = closed.pruneOccurrenceMatchSet(projected, blanket, rightExtensions);
            // Closed pruning condition (see buildBlanket for explanations)
            if (canPrune) {
                stats.incPruned();
                return;
            }

            Map<String, Projected> candidates = generateCandidates(projected);

            prune(candidates);

            if (pattern.size() < 3) {
                for (Map.Entry<String, Projected> entry : candidates.entrySet()) {
                    int oldSize = pattern.size();
                    expandCandidate(entry, false);
                    pattern.setSize(oldSize);
                }
            } else {
                if (shouldReport) {
                    report(pattern, projected, blanket);
                }

                // Out of all candidates, keep only a best limited subset of them
                KBest<Map.Entry<String, Projected>> kbest = KBest.create(BEAM_SIZE);
                for (Map.Entry<String, Projected> entry : candidates.entrySet()) {
                    Vector<String> testPattern = expandPattern(entry.getKey());

                    Pair<Double, TSGRule<String>> lengthRule = addPatternToTSG(true, testPattern, entry.getValue());
                    if (lengthRule != null) {
                        kbest.add(lengthRule.getKey(), entry);
                    }
                }

                // Expand the candidates remaining
                for (Pair<Double, Map.Entry<String, Projected>> entry : kbest.getKBest()) {
                    int oldSize = pattern.size();
                    double oldLength = bestLength;

                    Map.Entry<String, Projected> currentCandidate = entry.getValue();
                    double length = entry.getKey();

                    Pair<Double, TSGRule<String>> lengthRule = null;
                    if (length < bestLength) {
                        Vector<String> addPattern = expandPattern(currentCandidate.getKey());
                        lengthRule = addPatternToTSG(false, addPattern,
                                currentCandidate.getValue());
                        if (lengthRule != null) {
                            LOGGER.info("Better than initial " + addPattern.toString()
                                    + " Before: " + bestLength + " After: " + length);
                            bestLength = length;
                        } else {
                            LOGGER.info("Better than initial but pruned " + addPattern.toString()
                                    + " Before: " + bestLength + " After: " + length);
                        }
                    }

                    expandCandidate(currentCandidate, lengthRule != null);

                    pattern.setSize(oldSize);
                    bestLength = oldLength;
                    if (lengthRule != null) {
                        tsg.removeRule(lengthRule.getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("expanding error " + e);
        }
    }

    private Vector<String> expandPattern(String added) {
        // Add new candidate to current pattern
        Vector<String> res = new Vector<>(pattern);
        String[] p = added.split(String.valueOf(uniChar));
        for (int i = 0; i < p.length; ++i) {
            if (!p[i].isEmpty()) {
                res.addElement(p[i]);
            }
        }
        return res;
    }

    private void expandCandidate(Map.Entry<String, Projected> entry, boolean shouldReport) {
        // Add new candidate to current pattern
        // TODO refactor with expandPattern
        String[] p = entry.getKey().split(String.valueOf(uniChar));
        for (int i = 0; i < p.length; ++i) {
            if (!p[i].isEmpty()) {
                pattern.addElement(p[i]);
            }
        }
        project(entry.getValue(), shouldReport);
    }

    private Map<String, Projected> generateCandidates(Projected projected) {
        // Find all candidates of the current subtree
        int depth = projected.getProjectedDepth();
        Map<String, Projected> candidate = new LinkedHashMap<>(); //keep the order of elements
        for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
            int id = projected.getProjectLocation(i).getLocationId();
            int pos = projected.getProjectLocation(i).getLocationPos();
            // Add to keep all occurrences --> problem: memory consumption
            List<Integer> occurrences = projected.getProjectLocation(i).getLocationList();
            int parentId = occurrences.get(0);

            String prefix = "";
            for (int d = -1; d < depth && pos != -1; ++d) {
                int start = (d == -1) ? transactions.get(id).get(pos).getNodeChild() :
                        transactions.get(id).get(pos).getNodeSibling();
                int newDepth = depth - d;
                for (int l = start; l != -1;
                     l = transactions.get(id).get(l).getNodeSibling()) {
                    // Limit root of pattern to have 1 child (size must be >2 to allow the first child candidate)
                    if (occurrences.size() > 2 && transactions.get(id).get(l).getNodeParent() == parentId) continue;
                    String item = prefix + uniChar + transactions.get(id).get(l).getNodeLabel();

                    // TODO don't add if covered
                    Projected tmp;
                    if (candidate.containsKey(item)) {
                        candidate.get(item).addProjectLocation(id, l, occurrences);
                    } else {
                        tmp = new Projected();
                        tmp.setProjectedDepth(newDepth);
                        tmp.addProjectLocation(id, l, occurrences);
                        candidate.put(item, tmp);
                    }
                }
                if (d != -1) {
                    pos = transactions.get(id).get(pos).getNodeParent();
                }
                prefix += uniChar + ")";
            }
        }
        assert (candidate.entrySet().stream()
                .allMatch(c -> c.getValue().getProjectLocationSize() != 0));
        return candidate;
    }

    /**
     * Run BeamFreqt with a properties file
     *
     * @param newConfig
     */
    public void run(Config newConfig) {
        try {
            config = newConfig;
            stats = new SearchStatistics();

            System.out.println("running BeamFreqT");
            System.out.println("=============");

            //loadDatabase();
            readRootLabel();  // Read root labels (AST Nodes)
            readNodeList();

            // TODO useless grammar in output formats?
            closed = config.closedOnly() ? new Closed(transactions) : new ClosedNoop();
            output = config.outputAsXML() ? new XMLOutput(config, new HashMap<>()) : new LineOutput(config, new HashMap<>());

            System.out.println("find subtrees ... ");

            // Find 1-subtree
            Map<String, Projected> freq1 = buildFreq1Set();

            // Prune 1-subtree
            prune(freq1);
            System.out.println("all candidates after first pruning " + freq1.keySet());
            closed.pruneClosedFreq1(listRootLabel, freq1);
            System.out.println("all candidates after closed pruning " + freq1.keySet());
            // Grammar constraint: root pattern in listRootLabel
            //freq1.entrySet().removeIf(e -> !listRootLabel.contains(e.getKey()));
            System.out.println("all candidates after root config pruning " + freq1.keySet());

            // Expansion every 1-subtree to find larger subtrees
            // Using root whitelist so expand everything regardless of beam size initially
            pattern = new Vector<>();
            for (Map.Entry<String, Projected> entry : freq1.entrySet()) {
                if (entry.getKey() != null && entry.getKey().charAt(0) != '*') {
                    entry.getValue().setProjectedDepth(0);
                    pattern.addElement(entry.getKey());

                    project(entry.getValue(), false);

                    pattern.setSize(pattern.size() - 1);
                }

            }
            output.close();
        } catch (Exception e) {
            System.out.println("running error");
        }

    }

    /**
     * Return all frequent subtrees of size 1.
     * @return
     */
    private Map<String, Projected> buildFreq1Set() {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for (int i = 0; i < transactions.size(); ++i) {
            for (int j = 0; j < transactions.get(i).size(); ++j) {
                String nodeLabel = transactions.get(i).get(j).getNodeLabel();
                // Find a list of location then add to freq1[node_label].locations

                if (nodeLabel != null && transactions.get(i).get(j).getNodeChild() == -1) continue;

                Projected projected = freq1.getOrDefault(nodeLabel, new Projected());
                // Will add another occurrence if label exists already
                projected.setProjectLocation(i, j);
                projected.setProjectRootLocation(i, j);
                // LinkedHashMap:  Note that insertion order is not affected if a key is re-inserted into the map
                freq1.put(nodeLabel, projected);
            }
        }
        return freq1;
    }

    /**
     * Read list of root labels into listRootLabel.
     */
    private void readRootLabel() {
        try (Stream<String> stream = Files.lines(Paths.get(config.getRootLabelFile()))) {
            stream.filter(line -> (!line.isEmpty() && line.charAt(0) != '#')).forEach(line -> listRootLabel.add(line));
        } catch (IOException e) {
            System.out.println("Reading root label list file error ");
        }
    }

    /**
     * Read list of node labels that should be considered as lists.
     */
    private void readNodeList() {
        try (Stream<String> stream = Files.lines(Paths.get(config.getNodeListFile()))) {
            stream.filter(line -> (!line.isEmpty() && line.charAt(0) != '#')).forEach(line -> listNodeList.add(line));
        } catch (IOException e) {
            System.out.println("Reading node list file error ");
        }
    }

    private void loadDatabase(Database<String> db) {
        for (int tid = 0; tid < db.getSize(); tid++) {
            IDatabaseNode<String> transactionRoot = db.getTid(tid);
            transactions.add(new Vector<>());
            NodeFreqT nodeParent = new NodeFreqT();
            nodeParent.setNodeParent(-1);
            nodeParent.setNodeSibling(-1);
            nodeParent.setNodeOrdered(true);
            nodeParent.setNodeLabel(transactionRoot.getLabel());
            transactions.get(tid).add(nodeParent);
            transformNode(tid, transactionRoot, 0);
        }
        assertDBConsistent(db);
    }

    private void assertDBConsistent(Database<String> db) {
        assert (db.getSize() == transactions.size());

        for (int tid = 0; tid < transactions.size(); tid++) {
            List<String> compareFirst = new ArrayList<>();
            for (int j = 0; j < transactions.get(tid).size(); j++) {
                compareFirst.add(transactions.get(tid).get(j).getNodeLabel());
            }
            List<String> compareSecond = new ArrayList<>(Collections.nCopies(compareFirst.size(), ""));

            IDatabaseNode<String> root = db.getTid(tid);
            PeekableIterator<IDatabaseNode<String>> iterator = Util.asPreOrderIterator(
                    Util.asSingleIterator(root),
                    (IDatabaseNode<String> node) -> node.getChildren().iterator());
            iterator.next();
            while (iterator.hasNext()) {
                IDatabaseNode<String> next = iterator.peek();
                compareSecond.set(next.getID(), next.getLabel());
                iterator.next();
            }
            assert (compareFirst.equals(compareSecond));
        }


    }

    private void transformNode(int tid, IDatabaseNode<String> currentTreeNode, int parentPos) {
        int startPos = transactions.get(tid).size();
        int nbChildren = currentTreeNode.getChildrenCount() - 1;
        for (int i = 0; i <= nbChildren; i++) {
            if (i == 0) transactions.get(tid).get(parentPos).setNodeChild(startPos);

            IDatabaseNode<String> child = currentTreeNode.getChildAt(i);
            NodeFreqT nodeChild = new NodeFreqT();
            nodeChild.setNodeParent(parentPos);
            nodeChild.setNodeLabel(child.getLabel());
            nodeChild.setNodeOrdered(true);
            nodeChild.setNodeChild(-1);
            nodeChild.setNodeSibling((i != nbChildren) ? transactions.get(tid).size() + 1 : -1);
            transactions.get(tid).add(nodeChild);
        }

        for (int i = 0; i < currentTreeNode.getChildren().size(); i++) {
            IDatabaseNode<String> child = currentTreeNode.getChildAt(i);
            transformNode(tid, child, startPos + i);
        }
    }

    private void debugPrintStats(Projected projected) throws IOException {
        if (stats.getProject() % 1000 == 0) {
            System.out.println(stats + " " + projected.getProjectLocation(0).getLocationList().size());
            System.out.println(pattern);
            output.flush();
        }
    }

}