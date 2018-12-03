package be.intimals.freqt.mdl.miner;

import be.intimals.freqt.Config;
import be.intimals.freqt.constraints.Closed;
import be.intimals.freqt.constraints.ClosedNoop;
import be.intimals.freqt.constraints.IClosed;
import be.intimals.freqt.core.Extension;
import be.intimals.freqt.core.NodeFreqT;
import be.intimals.freqt.core.Pattern;
import be.intimals.freqt.core.Projected;
import be.intimals.freqt.mdl.input.Database;
import be.intimals.freqt.mdl.input.IDatabaseNode;
import be.intimals.freqt.mdl.tsg.ATSG;
import be.intimals.freqt.output.AOutputFormatter;
import be.intimals.freqt.output.LineOutput;
import be.intimals.freqt.output.XMLOutput;
import be.intimals.freqt.util.SearchStatistics;
import javafx.util.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


public class BeamFreqT {
    public static char uniChar = '\u00a5';// Japanese Yen symbol

    private static Config config;
    private AOutputFormatter output;
    private Vector<String> pattern;
    private Database<String> db;
    private Vector<Vector<NodeFreqT>> transactions = new Vector<>();
    private ATSG<String> tsg;
    private Set<String> listRootLabel = new HashSet<>();
    private Set<String> listNodeList = new HashSet<>();
    private IClosed closed;

    public SearchStatistics stats;

    private BeamFreqT(Database<String> db, ATSG<String> tsg) {
        this.db = db;
        this.tsg = tsg;
    }

    public static BeamFreqT create(Database<String> db, ATSG<String> tsg) {
        return new BeamFreqT(db, tsg);
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

    private void report(Vector<String> pat, Projected projected, Set<Extension> blanket) {
        boolean isClosed = blanket == null || closed.compareSupportMatch(projected, blanket);
        // closed if null blanket, closedness disabled or actually closed when enabled
        if (isClosed) {
            output.report(pat, projected);
            stats.incClosed();
        } else {
            stats.incNotClosed();
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
            if (stats.getProject() % 1000 == 0) {
                System.out.println(stats + " " + projected.getProjectLocation(0).getLocationList().size());
                System.out.println(pattern);
                output.flush();
            }

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

            // TODO debug
            //DebugUtil.writeHighWSupportPattern(projected, pattern, transactions);

            Map<String, Projected> candidates = generateCandidates(projected);

            //pruning relies on support: for each candidate if its support < minsup --> remove
            prune(candidates);

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

            project(entry.getValue());
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
                int start = (d == -1) ? transactions.get(id).get(pos).getNodeChild() :
                        transactions.get(id).get(pos).getNodeSibling();
                int newDepth = depth - d;
                for (int l = start; l != -1;
                     l = transactions.get(id).get(l).getNodeSibling()) {
                    String item = prefix + uniChar + transactions.get(id).get(l).getNodeLabel();

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

            loadDatabase();
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
            freq1.entrySet().removeIf(e -> !listRootLabel.contains(e.getKey()));
            System.out.println("all candidates after root config pruning " + freq1.keySet());

            // Expansion every 1-subtree to find larger subtrees
            pattern = new Vector<>();
            for (Map.Entry<String, Projected> entry : freq1.entrySet()) {
                if (entry.getKey() != null && entry.getKey().charAt(0) != '*') {
                    entry.getValue().setProjectedDepth(0);
                    pattern.addElement(entry.getKey());

                    project(entry.getValue());

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

    private void loadDatabase() {
        for (int tid = 0; tid < db.getSize(); tid++) {
            IDatabaseNode<String> transactionRoot = db.getTid(tid);
            transactions.add(new Vector<>());
            NodeFreqT nodeParent = new NodeFreqT();
            nodeParent.setNodeParent(-1);
            nodeParent.setNodeSibling(-1);
            nodeParent.setNodeOrdered(true);
            nodeParent.setNodeLabel(transactionRoot.getLabel());
            transactions.get(tid).add(nodeParent);
            transformNode(tid, transactionRoot, 0, -1);
        }
    }

    private void transformNode(int tid, IDatabaseNode<String> currentTreeNode, int parentPos, int sibilingPos) {
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
            transformNode(tid, child, startPos + i, 0);
        }
    }

}