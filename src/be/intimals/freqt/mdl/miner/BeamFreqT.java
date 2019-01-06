package be.intimals.freqt.mdl.miner;

import be.intimals.freqt.Config;
import be.intimals.freqt.constraints.Closed;
import be.intimals.freqt.constraints.ClosedNoop;
import be.intimals.freqt.constraints.IClosed;
import be.intimals.freqt.core.*;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.mdl.tsg.*;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BeamFreqT {
    private static final Logger LOGGER = Logger.getLogger(BeamFreqT.class.getName());
    private static final Random RANDOM = new Random(41556254);
    public static char uniChar = '\u00a5';// Japanese Yen symbol

    private static Config config;
    private AOutputFormatter output;
    private Vector<Vector<NodeFreqT>> transactions = new Vector<>();
    private Set<String> listRootLabel = new HashSet<>(); // Possible roots of trees
    //private Set<String> listNodeList = new HashSet<>(); // Which nodes should be modeled as lists (not fixed N children)
    private Map<String, Projected> freq1;

    private ATSG<String> tsg;
    private Map<Integer, Set<Integer>> coveredByTid = new HashMap<>(); // Which nodes are already explained by a tree rule
    private double lengthToBeat;
    private double bestLengthFound = Double.MAX_VALUE;
    private List<CandidateRule> bestRulesFound = new ArrayList<>();
    private List<CandidateRule> addedToTsg = new ArrayList<>();
    private int noImprovementCount = 0;
    private int iteration = 0;
    private TabuMoves tabu;
    private Set<TSGRule<String>> uniqueRules;

    private Function<SearchStatistics, Boolean> stopCondition;
    public SearchStatistics stats;

    private BeamFreqT(Database<String> db, ATSG<String> tsg, Function<SearchStatistics, Boolean> stopCondition) {
        loadDatabase(db);
        assertDBConsistent(db);
        this.tsg = tsg;
        this.lengthToBeat = tsg.getCodingLength();
        this.stopCondition = stopCondition;
    }

    public static BeamFreqT create(Database<String> db, ATSG<String> tsg, Function<SearchStatistics, Boolean> stopCondition) {
        return new BeamFreqT(db, tsg, stopCondition);
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
                        //LOGGER.info("REMOVED location, already covered: "
                        //        + transactions.get(loc.getLocationId()).get(val).getNodeLabel() + " - " + val);
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

    //private void report(Vector<String> pat, Projected projected, Set<Extension> blanket) {
    //    boolean isClosed = blanket == null || closed.compareSupportMatch(projected, blanket);
    //    // closed if null blanket, closedness disabled or actually closed when enabled
    //    if (isClosed) {
    //        output.report(pat, projected);
    //        stats.incClosed();
    //    } else {
    //        stats.incNotClosed();
    //    }
    //}

    private Pair<Double, TSGRule<String>> tryPatternInTSG(Vector<String> pat, Projected projected) {
        TSGRule<String> rule = buildRuleFromPattern(pat, projected);
        if (rule.getCount() == 0) return null;
        tsg.addRule(rule);
        double res = tsg.getCodingLength();
        tsg.removeRule(rule);
        return new Pair<>(res, rule);
    }

    private TSGRule<String> buildRuleFromPattern(Vector<String> pat, Projected projected) {
        ITSGNode<String> patternRoot = TSGNode.buildFromList(pat.toArray(new String[]{}), ")");
        TSGRule<String> rule = TSGRule.create(tsg.getDelimiter());
        rule.setRoot(patternRoot);
        Map<Integer, Set<Integer>> tempCovered = cloneCover();
        for (int i = 0; i < projected.getProjectLocationSize(); i++) {
            Location loc = projected.getProjectLocation(i);
            List<Integer> occurrence = loc.getLocationList();
            // Keep a temp copy as we add occurrences, we need to add them to the new temp cover
            Set<Integer> covered = tempCovered.getOrDefault(loc.getLocationId(), new HashSet<>());
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
                tempCovered.putIfAbsent(loc.getLocationId(), covered);
            }
        }
        return rule;
    }

    private Map<Integer, Set<Integer>> cloneCover() {
        Map<Integer, Set<Integer>> res = new HashMap<>();
        coveredByTid.forEach((key, value) -> res.put(key, new HashSet<>(value)));
        return res;
    }


    private void project(Vector<String> pattern, Projected projected, CandidateRule currentBest) {
        try {
            if (stopCondition.apply(stats)) return;
            stats.incProject();
            ++iteration;
            ++noImprovementCount;
            debugPrintStats(pattern, projected, currentBest);

            if (noImprovementCount > config.getNoImproveMaxIter()) {
                //System.out.println("NO IMPROVEMENT");
                return;
            }
            Map<String, Projected> candidates = generateCandidates(projected);

            // Prune support & already covered
            prune(candidates);

            List<Pair<Double, CandidateRule>> kBestList;
            // Only try patterns with height > 1 because tsg is built on patterns of height 1
            //if (Pattern.getPatternHeight(pattern) > 1 || projected.getProjectLocationSize() > 5000) {
            if (Pattern.getPatternHeight(pattern) > 1) {
                // Out of all candidates, keep only a best limited subset of them
                KBest<CandidateRule> kbest = KBest.create(config.getBeamSize() == -1 ? Integer.MAX_VALUE
                        : config.getBeamSize());
                for (Map.Entry<String, Projected> entry : candidates.entrySet()) {
                    Vector<String> testPattern = expandPattern(pattern, entry.getKey());

                    Pair<Double, TSGRule<String>> lengthRule = tryPatternInTSG(testPattern, entry.getValue());
                    if (lengthRule != null) {
                        Double candidateLength = lengthRule.getKey();
                        // If found an improvement to coding length,
                        // only consider next candidate if improves even further
                        if (candidateLength <= currentBest.getLength()) { // improvedLength is MAX_VALUE by default
                            kbest.add(candidateLength,
                                    new CandidateRule(candidateLength, entry, lengthRule.getValue()));
                        }
                    }
                }
                kBestList = kbest.getKBest();

                if (kBestList.isEmpty() && currentBest.getLength() != Double.MAX_VALUE) {
                    // Found compressing pattern but no candidates improve it further
                    // Add to tsg & restart from freq1
                    double oldLength = lengthToBeat;
                    tsg.addRule(currentBest.getRule());
                    double newLength = tsg.getCodingLength();
                    currentBest.setLength(newLength);
                    lengthToBeat = currentBest.getLength();
                    addedToTsg.add(currentBest);
                    currentBest.pattern = new Vector<>(pattern);

                    LOGGER.info("ADDING :"
                            + currentBest.getRule().toPreOrderList() + " Old: " + oldLength + " New:" + lengthToBeat);
                    LOGGER.info(stats + " " + projected.getProjectLocation(0).getLocationList().size() + " improve: " + noImprovementCount);

                    // Local search heuristics update
                    this.noImprovementCount = 0;
                    tabu.add(currentBest.getRule().getRoot().getLabel().hashCode(), iteration);
                    uniqueRules.add(currentBest.getRule());

                    if (currentBest.getLength() < bestLengthFound) {
                        bestLengthFound = currentBest.getLength();
                        bestRulesFound = new ArrayList<>(addedToTsg);
                    }

                    // Clone cover
                    Map<Integer, Set<Integer>> oldCover = coveredByTid;
                    Map<Integer, Set<Integer>> tempCovered = cloneCover();
                    for (TreeOccurrence<String> ruleOccurrence : currentBest.getRule().getAllOccurrences()) {
                        Set<Integer> covered = tempCovered.getOrDefault(ruleOccurrence.getTID(), new HashSet<>());
                        covered.addAll(ruleOccurrence.getOccurrence());
                        tempCovered.putIfAbsent(ruleOccurrence.getTID(), covered);
                    }
                    coveredByTid = tempCovered;

                    expandFreq1Set();

                    // Restore values
                    addedToTsg.remove(addedToTsg.size() - 1);
                    tsg.removeRule(currentBest.getRule());
                    coveredByTid = oldCover;
                    lengthToBeat = oldLength;
                }
            } else {
                // Too small of a pattern to be interesting, to try to add to TSG
                kBestList = new ArrayList<>();
                for (Map.Entry<String, Projected> projectedEntry : candidates.entrySet()) {
                    kBestList.add(new Pair<>(Double.MAX_VALUE,
                            new CandidateRule(Double.MAX_VALUE, projectedEntry,null)));
                }
                int debug = 0;
            }

            if (stats.getProject() % 1000 == 0) {
                LOGGER.info("To beat: " + lengthToBeat + " candidates: " + String.join(", ",
                        kBestList.stream().map(e -> e.getValue().toString()).collect(Collectors.toList())));
            }

            // Expand the candidates remaining if any
            for (Pair<Double, CandidateRule> entryPair : kBestList) {
                CandidateRule candidateRule = entryPair.getValue();
                Map.Entry<String, Projected> currentCandidate = candidateRule.getPatternProject();
                double length = candidateRule.getLength();

                CandidateRule nextCandidateRule = currentBest;
                if (length < lengthToBeat) {
                    nextCandidateRule = candidateRule;
                }

                Vector<String> nextPattern = expandPattern(pattern, currentCandidate.getKey());

                if (Util.noDuplicateChildren(nextPattern, ")")) {
                    project(nextPattern, currentCandidate.getValue(), nextCandidateRule);
                } else {
                    System.out.println("Repeat ");
                }
                project(nextPattern, currentCandidate.getValue(), nextCandidateRule);
            }
        } catch (Exception e) {
            System.out.println("expanding error " + e);
        }
    }

    private void expandFreq1Set() {
        Vector<String> pattern = new Vector<>();
        freq1.entrySet().stream().filter(e -> !tabu.isTabu(e.getKey().hashCode(), iteration)).sorted(Comparator.comparingInt(e -> RANDOM.nextInt())).forEach(entry -> {
            entry.getValue().setProjectedDepth(0);
            pattern.addElement(entry.getKey());

            project(pattern, entry.getValue(), new CandidateRule());
            this.noImprovementCount = 0;

            pattern.setSize(pattern.size() - 1);
        });
    }

    private Vector<String> expandPattern(Vector<String> currentPattern, String added) {
        // Add new candidate to current pattern
        Vector<String> res = new Vector<>(currentPattern);
        String[] p = added.split(String.valueOf(uniChar));
        for (int i = 0; i < p.length; ++i) {
            if (!p[i].isEmpty()) {
                res.addElement(p[i]);
            }
        }
        return res;
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

            String prefix = "";
            for (int d = -1; d < depth && pos != -1; ++d) {
                int start = (d == -1) ? transactions.get(id).get(pos).getNodeChild() :
                        transactions.get(id).get(pos).getNodeSibling();
                int newDepth = depth - d;
                for (int l = start; l != -1;
                     l = transactions.get(id).get(l).getNodeSibling()) {
                    // Limit root of pattern to have 1 child (size must be >2 to allow the first child candidate)
                    String item = prefix + uniChar + transactions.get(id).get(l).getNodeLabel();

                    // TODO don't add if covered already?
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

            readRootLabel();  // Read root labels (AST Nodes)
            //readNodeList();

            tabu = TabuMoves.create(config.getTabuTenure());
            uniqueRules = new HashSet<>();

            // TODO useless grammar in output formats?
            output = config.outputAsXML() ? new XMLOutput(config, new HashMap<>()) : new LineOutput(config, new HashMap<>());

            System.out.println("find subtrees ... ");
            Thread.sleep(1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date start = new Date();
            String startDate = sdf.format(start);
            System.out.println(startDate);
            long startRun = System.currentTimeMillis();

            // Find 1-subtree
            freq1 = buildFreq1Set();

            // Prune 1-subtree
            prune(freq1);
            System.out.println("all candidates after first pruning " + freq1.keySet());
            System.out.println("all candidates after closed pruning " + freq1.keySet());
            // Grammar constraint: root pattern in listRootLabel
            if (!listRootLabel.isEmpty()) {
                freq1.entrySet().removeIf(e -> !listRootLabel.contains(e.getKey()));
            }
            System.out.println("all candidates after root config pruning " + freq1.keySet());

            // Expansion every 1-subtree to find larger subtrees
            // Using root whitelist so expand everything regardless of beam size initially
            expandFreq1Set();

            if (this.bestRulesFound.stream().allMatch(e -> e.pattern != null)) {
                this.bestRulesFound.forEach(e -> output.report(e.pattern, e.getPatternProject().getValue()));
            }
            output.close();

            long endRun = System.currentTimeMillis();
            long diff = endRun - startRun;
            System.out.println("MINING running time : " + diff + " ms");

            Date end = new Date();
            System.out.println("Start: " + startDate + "End: " + sdf.format(end));
            System.gc();
            Thread.sleep(1000);
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
        // Sort by nb of occurrences
        //Map<String, Projected> res = new LinkedHashMap<>();
        //freq1.entrySet().stream()
        //        .sorted(Comparator.comparingInt(e -> -e.getValue().getProjectLocationSize()))
        //        .forEach(e -> res.put(e.getKey(), e.getValue()));
        //return res;
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

    // TODO
    ///**
    // * Read list of node labels that should be considered as lists.
    // */
    //private void readNodeList() {
    //    try (Stream<String> stream = Files.lines(Paths.get(config.getNodeListFile()))) {
    //        stream.filter(line -> (!line.isEmpty() && line.charAt(0) != '#')).forEach(line -> listNodeList.add(line));
    //    } catch (IOException e) {
    //        System.out.println("Reading node list file error ");
    //    }
    //}

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

    public double getBestLength() {
        return this.bestLengthFound;
    }

    public List<CandidateRule> getBestRules() {
        return this.bestRulesFound;
    }

    public Set<TSGRule<String>> getUniqueRules() {
        return uniqueRules;
    }

    private void debugPrintStats(Vector<String> pattern, Projected projected, CandidateRule currentBest) throws IOException {
        if (stats.getProject() % 50 == 0) {
            System.out.println(stats + " " + projected.getProjectLocation(0).getLocationList().size() + " improve: " + noImprovementCount);
            System.out.println(this.addedToTsg.stream().map(e -> e.getRule()).collect(Collectors.toList()));
            System.out.println(pattern);
            output.flush();
        }
    }

}