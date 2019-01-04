package be.intimals.freqt.core;

import be.intimals.freqt.Config;
import be.intimals.freqt.constraints.Closed;
import be.intimals.freqt.constraints.ClosedNoop;
import be.intimals.freqt.constraints.IClosed;
import be.intimals.freqt.input.CreateGrammar;
import be.intimals.freqt.input.ReadGrammar;
import be.intimals.freqt.input.ReadXML;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.LineOutput;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.util.SearchStatistics;
import java.io.*;
import java.util.*;
import javafx.util.Pair;


public class FreqT {
    public static char uniChar = '\u00a5';// Japanese Yen symbol

    private static Config config;
    private AOutputFormatter output;
    private Vector<String> pattern;
    private Vector<Vector<NodeFreqT>> transactions = new Vector<>();
    private Map<String, Vector<String>> grammar;
    private Set<String> listRootLabel = new HashSet<>();
    private Map<String, Set<String>> listBlackLabel = new LinkedHashMap<>();
    private IClosed closed;

    public SearchStatistics stats;

    /**
     * Get a blacklist children of a node.
     *
     * @param blackListNode
     * @param pat
     * @param candidate
     * @return
     */
    private Set<String> getBlackListChildren(Map<String, Set<String>> blackListNode,
                                                     Vector<String> pat, String candidate) {
        String[] candidateTemp = candidate.split(String.valueOf(uniChar));

        Vector<String> patternTemp = new Vector<>(pat);

        for (String aCandidate : candidateTemp) {
            if (!aCandidate.isEmpty()) {
                patternTemp.addElement(aCandidate);
            }
        }

        // Find parent's position of potentialCandidate in patternTemp
        int parentPos = Pattern.findParent(patternTemp, uniChar, candidate);
        String parentLabel = patternTemp.elementAt(parentPos).split(String.valueOf(uniChar))[0];

        return blackListNode.getOrDefault(parentLabel, new HashSet<>());
    }

    /**
     * Get potential candidate.
     *
     * @param candidate
     * @return
     */
    private String getPotentialCandidateLabel(String candidate) {
        String[] p = candidate.split(String.valueOf(uniChar));
        return p[p.length - 1];
    }

    /**
     * Prune candidates based on support.
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
            }
        }
    }


    /**
     * Prune candidate based on blacklist children.
     *
     * @param candidate
     */
    private void pruneBlackList(Map<String, Projected> candidate) {
        Iterator<Map.Entry<String, Projected>> iterTemp = candidate.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, Projected> entry = iterTemp.next();
            Set<String> blackListChildren = getBlackListChildren(listBlackLabel, pattern, entry.getKey());
            String candidateLabel = getPotentialCandidateLabel(entry.getKey());
            if (blackListChildren.contains(candidateLabel)
                    || candidateLabel.equals("*get")
                    || candidateLabel.equals("*set")) {
                iterTemp.remove();
            }
        }
    }

    private void report(Vector<String> pat, Projected projected, Set<Extension> blanket) {
        if (Pattern.checkConstraints(config, pat)) {
            boolean isClosed = blanket == null || closed.compareSupportMatch(projected, blanket);
            // closed if null blanket, closedness disabled or actually closed when enabled
            if (isClosed) {
                output.report(pat, projected);
                stats.incClosed();
            } else {
                stats.incNotClosed();
            }
        } else {
            stats.incConstraint();
        }
    }

    /**
     * Expand a subtree.
     *
     * @param projected
     */
    private void project(Projected projected) {
        try {
            stats.incProject();
            debugPrintStats(pattern, projected);

            Set<Pair<Integer, String>> rightExtensions = new HashSet<>();
            Set<Extension> blanket = closed.buildBlanket(listRootLabel, listBlackLabel,
                    projected, rightExtensions);
            // Build B^SM_t and B^OM_t which define if t is a closed subtree and if we can prune the whole branch
            boolean canPrune = closed.pruneOccurrenceMatchSet(projected, blanket, rightExtensions);
            // Closed pruning condition (see buildBlanket for explanations)
            if (canPrune) {
                stats.incPruned();
                return;
            }

            // Size constraint: if pattern's size = maximal size then report pattern and stop expanding
            if (Pattern.getPatternSize(pattern) >= config.getMaxPatternSize()) {
                report(pattern,projected, null); // TODO Note: closed & size constraint issues when together
                return;
            }

            // Grammar constraint: left part of subtree must have full leaf nodes
            // If the left part of this subtree misses leaf node --> stop
            if (Pattern.checkMissedLeafNode(pattern)) {
                stats.incConstraint();
                return;
            }
            //TODO:how to delete right part of this pattern then print it

            // TODO Note: moved this outside of expanding candidates method
            // Size constraint: don't allow a pattern having a label repeated more than N times
            if (Pattern.checkNumberLabel(pattern,config.getMaxTimeLabel())) {
                report(pattern, projected, blanket);
                return;
            }

            // TODO debug
            //DebugUtil.writeHighWSupportPattern(projected, pattern, transactions);

            Map<String, Projected> candidates = generateCandidates(projected);

            //pruning relies on support: for each candidate if its support < minsup --> remove
            prune(candidates);

            //pruning relies on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(candidates);

            if (candidates.isEmpty()) {
                report(pattern, projected, blanket);
                return;
            }

            //expand the current pattern with each candidate
            Iterator<Map.Entry<String, Projected>> iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = pattern.size();

                Map.Entry<String, Projected> entry = iter.next();

                expandCandidate(entry);
                pattern.setSize(oldSize);
            }
        } catch (Exception e) {
            System.out.println("expanding error " + e);
        }
    }

    private void expandCandidate(Map.Entry<String, Projected> entry) {
        Set<String> blackListChildren = getBlackListChildren(listBlackLabel, pattern, entry.getKey());
        // add new candidate to current pattern
        String[] p = entry.getKey().split(String.valueOf(uniChar));
        for (int i = 0; i < p.length; ++i) {
            if (!p[i].isEmpty()) {
                pattern.addElement(p[i]);
            }
        }
        //////////expand subtree based on grammar///////////////////
        // Find parent of potentialCandidate
        String potentialCandidate = getPotentialCandidateLabel(entry.getKey());
        if (potentialCandidate.charAt(0) == '*') { // potentialCandidate is a leaf node
            project(entry.getValue());
        } else { // Internal node
            // Find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParent(pattern, uniChar, entry.getKey());
            String parentLabel = pattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if (grammar.containsKey(parentLabel)) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree = grammar.get(parentLabel).elementAt(1);
                switch (parentOrdered) {
                    case "unordered":
                        switch (parentDegree) {
                            case "1":
                                project(entry.getValue());
                                break;

                            case "1..*":
                                // Grammar constraint:
                                // don't allow N children of an unordered node to have the same label
                                if (Pattern.checkDuplicateChildren(pattern, uniChar, entry.getKey(),
                                        config.getMaxRepeatLabel())) {
                                    project(entry.getValue());
                                } else {
                                    stats.incConstraint();
                                }
                                break;

                            default: // Node has N children in the order of labels
                                // Grammar constraint:
                                // don't allow to directly expand a child without its previous mandatory sibling
                                // Find all children of parentPos in grammar
                                Vector<String> listOfChildrenGrammar = new Vector<>(
                                        grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));

                                // Find all children of parentPos in pattern
                                Vector<String> listOfChildrenPattern = Pattern.findChildren(pattern, parentPos);

                                String firstChildInGrammar = listOfChildrenGrammar.elementAt(0)
                                        .split(String.valueOf(uniChar))[0];

                                // Find the first mandatory child in grammar
                                String firstMandatoryChildGrammar = "";
                                for (int i = 0; i < listOfChildrenGrammar.size(); ++i) {
                                    String[] tmpChild = listOfChildrenGrammar.elementAt(i)
                                            .split(String.valueOf(uniChar));
                                    if (tmpChild[1].equals("true")) {
                                        firstMandatoryChildGrammar = tmpChild[0];
                                        break;
                                    }
                                }

                                if (potentialCandidate.equals(firstChildInGrammar)
                                        || potentialCandidate.equals(firstMandatoryChildGrammar)) {
                                    project(entry.getValue());
                                } else {
                                    // Check if a pattern has all previous mandatory children and not in blacklist
                                    int i = 0;
                                    int j = 0;
                                    boolean missMandatoryChild = false;
                                    while (i < listOfChildrenPattern.size() && j < listOfChildrenGrammar.size()) {
                                        String[] tmpChild = listOfChildrenGrammar.elementAt(j)
                                                .split(String.valueOf(uniChar));
                                        if (listOfChildrenPattern.elementAt(i).equals(tmpChild[0])) {
                                            ++i;
                                            ++j;
                                        } else {
                                            //
                                            if (tmpChild[1].equals("true")
                                                    && !blackListChildren.contains(tmpChild[0])) {
                                                missMandatoryChild = true;
                                                break;
                                            } else {
                                                j++;
                                            }
                                        }
                                    }
                                    // If name has full children
                                    if (!missMandatoryChild) {
                                        project(entry.getValue());
                                    } else {
                                        stats.incConstraint();
                                    }
                                }
                                break;
                        }
                        break;

                    case "ordered":
                        // Ordered nodes always have 1..* degree
                        project(entry.getValue());
                        break;

                    default:
                        System.err.println("Impossible ordering");
                        break;
                }
            }
        }
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
                int start = (d == -1) ? transactions.elementAt(id).elementAt(pos).getNodeChild() :
                        transactions.elementAt(id).elementAt(pos).getNodeSibling();
                int newDepth = depth - d;
                for (int l = start; l != -1;
                     l = transactions.elementAt(id).elementAt(l).getNodeSibling()) {
                    String item = prefix + uniChar + transactions.elementAt(id).elementAt(l).getNodeLabel();

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
                    pos = transactions.elementAt(id).elementAt(pos).getNodeParent();
                }
                prefix += uniChar + ")";
            }
        }
        return candidate;
    }

    /**
     * Run Freqt with file config.properties
     *
     * @param newConfig
     */
    public void run(Config newConfig) {
        try {
            config = newConfig;
            stats = new SearchStatistics();

            System.out.println("running FreqT");
            System.out.println("=============");

            initGrammar();
            initDatabase();
            readBlackLabel(); // Read data to blackListNode
            readRootLabel();  // Read root labels (AST Nodes)

            closed = config.closedOnly() ? new Closed(transactions) : new ClosedNoop();
            output = config.outputAsXML() ? new XMLOutput(config, grammar) : new LineOutput(config, grammar);

            System.out.println("find subtrees ... ");

            // Find 1-subtree
            Map<String, Projected> freq1 = buildFreq1Set();

            // Prune 1-subtree
            prune(freq1);
            System.out.println("all candidates after first pruning " + freq1.keySet());
            // Prune if closed or not (parent relation)
            closed.pruneClosedFreq1(listRootLabel, freq1);
            System.out.println("all candidates after closed pruning " + freq1.keySet());
            // Grammar constraint: root pattern in listRootLabel
            freq1.entrySet().removeIf(e -> !listRootLabel.isEmpty() && !listRootLabel.contains(e.getKey()));
            System.out.println("all candidates after root config pruning " + freq1.keySet());

            // Expansion every 1-subtree to find larger subtrees
            pattern = new Vector<>();
            for (Map.Entry<String, Projected> entry : freq1.entrySet()) {
                if (entry.getKey() != null && entry.getKey().charAt(0) != '*') {
                    if (grammar.containsKey(entry.getKey())) {
                        entry.getValue().setProjectedDepth(0);
                        pattern.addElement(entry.getKey());

                        project(entry.getValue());

                        pattern.setSize(pattern.size() - 1);
                    } else {
                        System.out.println(entry.getKey() + " doesn't exist in grammar ");
                    }
                }

            }
            output.close();
        } catch (Exception e) {
            System.out.println("running error");
        }

    }

    /**
     * Return all frequent subtrees of size 1.
     *
     * @return
     */
    private Map<String, Projected> buildFreq1Set() {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for (int i = 0; i < transactions.size(); ++i) {
            for (int j = 0; j < transactions.elementAt(i).size(); ++j) {
                String node_label = transactions.elementAt(i).elementAt(j).getNodeLabel();
                // Find a list of location then add to freq1[node_label].locations

                if (node_label != null && node_label.startsWith("*")) continue;

                Projected projected = freq1.getOrDefault(node_label, new Projected());
                // Will add another occurrence if label exists already
                projected.setProjectLocation(i, j);
                projected.setProjectRootLocation(i, j);
                // LinkedHashMap:  Note that insertion order is not affected if a key is re-inserted into the map
                freq1.put(node_label, projected);
            }
        }
        return freq1;
    }

    /**
     * Loads data from folders.
     */
    private void initDatabase() {
        System.out.println("create tree dataset ...");
        ReadXML readXML = new ReadXML();
        readXML.createTransaction(new File(config.getInputFiles()), transactions, grammar);
        System.out.println("total files: " + transactions.size());
    }

    /**
     * Loads the grammar from a file or builds it from a set of files.
     */
    private void initGrammar() throws Exception {
        // Read grammar from grammarFile
        System.out.println("read grammar ... ");
        grammar = new LinkedHashMap<>();

        if (config.buildGrammar()) {
            CreateGrammar createGrammar = new CreateGrammar();
            createGrammar.createGrammar(config.getInputFiles(), grammar);
        } else {
            ReadGrammar read = new ReadGrammar();
            read.readGrammar(config.getGrammarFile(), grammar);
        }

        // Output grammar
        System.out.println("########--INPUT GRAMMAR--###########");
        Iterator<Map.Entry<String, Vector<String>>> iter1 = grammar.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry<String, Vector<String>> entry = iter1.next();
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        System.out.println("###############################");
    }

    /**
     * Read list of root labels into listRootLabel.
     */
    private void readRootLabel() {
        try (BufferedReader br = new BufferedReader(new FileReader(config.getRootLabelFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && line.charAt(0) != '#') {
                    String[] str_tmp = line.split(" ");
                    listRootLabel.add(str_tmp[0]);
                }
            }
        } catch (IOException e) {
            System.out.println("Reading listRootLabel file error ");
        }

    }

    /**
     * Read list of blacklist labels into listBlackLabel.
     */
    private void readBlackLabel() {
        try (BufferedReader br = new BufferedReader(new FileReader(config.getBlackLabelFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && line.charAt(0) != '#') {
                    String[] str_tmp = line.split(" ");
                    String ASTNode = str_tmp[0];
                    Set<String> children = new HashSet<>();
                    for (int i = 1; i < str_tmp.length; ++i) {
                        children.add(str_tmp[i]);
                    }

                    listBlackLabel.put(ASTNode, children);
                }
            }
        } catch (IOException e) {
            System.out.println("Reading listBlackLabel file error ");
        }

    }

    private void debugPrintStats(Vector<String> pattern, Projected projected) throws IOException {
        if (stats.getProject() % 1000 == 0) {
            System.out.println(stats + " " + projected.getProjectLocation(0).getLocationList().size());
            System.out.println(pattern);
            output.flush();
        }
    }


}