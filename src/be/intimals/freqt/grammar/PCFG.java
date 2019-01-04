package be.intimals.freqt.grammar;

import be.intimals.freqt.util.DoubleUtil;
import be.intimals.freqt.util.MarkovChain;
import be.intimals.freqt.util.XMLUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PCFG {
    private static final Logger LOGGER = Logger.getLogger(PCFG.class.getName());
    private static String PARENT_ANNOTATION = "^"; // Unique delimiter not appearing in grammar

    private Map<String, Symbol> cfg = new HashMap<>();
    private Map<String, Set<String>> abstractNodes;
    private Map<String, String> images;
    private List<String> possibleRoots;

    public void loadGrammar(String path) throws IllegalArgumentException {
        LOGGER.setLevel(Level.OFF);
        try {
            Node root = XMLUtil.getXMLRoot(path);
            abstractNodes = loadAbstractNodes(root);
            images = loadImageNodes(root);
            cfg.put("", Symbol.EPSILON);

            NodeList childrenNodes = root.getChildNodes();
            for (int t = 0; t < childrenNodes.getLength(); ++t) {
                Node currentNode = childrenNodes.item(t);
                if ((!currentNode.hasAttributes() || currentNode.getAttributes().getNamedItem("abstract") == null)
                        && (currentNode.getNodeType() == Node.ELEMENT_NODE)) {
                    NodeList childrenList = currentNode.getChildNodes();
                    Symbol lhsSymbol = cfg.getOrDefault(currentNode.getNodeName().toLowerCase(),
                            Symbol.newSymbol(currentNode.getNodeName().toLowerCase()));
                    // Important to put it here already for recursive rules (e.g. ArrayAccess -> array -> ArrayAccess)
                    cfg.put(lhsSymbol.getName().toLowerCase(), lhsSymbol);
                    SymbolsRHS rhsSymbols = SymbolsRHS.newRHS();

                    for (int i = 0; i < childrenList.getLength(); ++i) {
                        if ((childrenList.item(i).getNodeType() == Node.ELEMENT_NODE)
                                && (childrenList.item(i).hasAttributes())) {

                            Node currentChild = childrenList.item(i);
                            NamedNodeMap nodeMap = currentChild.getAttributes();
                            boolean isMandatory = nodeMap.getNamedItem("optional") == null
                                    || !Boolean.valueOf(nodeMap.getNamedItem("optional").getNodeValue());
                            for (int j = 0; j < nodeMap.getLength(); ++j) { // For each attribute
                                Node currentChildAttr = nodeMap.item(j);
                                Symbol rhsSymbol = null;
                                switch (currentChildAttr.getNodeName()) {
                                    case "node":
                                        rhsSymbol = buildRHS(currentChild, currentChildAttr, lhsSymbol, isMandatory);
                                        rhsSymbol.setOrder(Symbol.Order.NONE);
                                        rhsSymbol.setChildren(Symbol.Children.N);
                                        break;

                                    case "ordered-nodelist":
                                        rhsSymbol = buildRHSList(currentChild, currentChildAttr, lhsSymbol,
                                                isMandatory, Symbol.Order.ORDERED);
                                        rhsSymbol.setOrder(Symbol.Order.ORDERED);
                                        rhsSymbol.setChildren(Symbol.Children.ONE_OR_MORE);
                                        break;

                                    case "unordered-nodelist":
                                        rhsSymbol = buildRHSList(currentChild, currentChildAttr,
                                                lhsSymbol, isMandatory, Symbol.Order.UNORDERED);
                                        rhsSymbol.setOrder(Symbol.Order.UNORDERED);
                                        rhsSymbol.setChildren(Symbol.Children.ONE_OR_MORE);
                                        break;

                                    case "simplevalue":
                                        rhsSymbol = buildRHS(currentChild, currentChildAttr, lhsSymbol, isMandatory);
                                        rhsSymbol.setOrder(Symbol.Order.NONE);
                                        rhsSymbol.setChildren(Symbol.Children.N);

                                        break;

                                    case "optional":
                                        // Not needed here
                                        break;

                                    default:
                                        LOGGER.info("Unknown attribute found " + currentChildAttr.getNodeName());
                                        break;
                                }
                                if (rhsSymbol != null) {
                                    cfg.put(rhsSymbol.getName().toLowerCase(), rhsSymbol);
                                    rhsSymbols.addSymbol(rhsSymbol);
                                }
                            }
                        }
                    }
                    lhsSymbol.addRule(rhsSymbols);
                    lhsSymbol.setChildren(Symbol.Children.N);
                }
            }

            // Symbols like A^X & A^Y currently point to the same SymbolsRHS in their lists.
            // Once all the rules are built, make copies as each should have a separate counter.
            this.cfg.entrySet().stream()
                    .filter(e -> e.getKey().contains(PARENT_ANNOTATION))
                    .forEach(e -> e.getValue().setRules(e.getValue().getRules().stream()
                            .map(SymbolsRHS::newRHS)
                            .collect(Collectors.toList())));

            possibleRoots = findRoots();

            LOGGER.info("CFG loaded");
            LOGGER.info(toPrettyString(false));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load the given grammar : " + e.getMessage());
        }
    }

    public void computeProbabilities() {
        computeMLLogProb();
        fitGeometric();
        computeTransitionMatrix();

        // For all X, sum of probabilities X->A should be 1
        assert (this.cfg.values().stream()
                // Filter no occurrences of X & terminals
                .filter(s -> s.getCount() != 0 && !s.getRules().isEmpty())
                .mapToDouble(s -> s.getRules().stream().mapToDouble(d -> DoubleUtil.exp2(d.getProb())).sum())
                .allMatch(d -> Double.compare(d, 1.0) == 0));
    }

    private void computeTransitionMatrix() {
        this.cfg.values().stream()
                .filter(s -> s.getName().endsWith(PCFG.getListAnnotatedName("")))
                .forEach(s -> {
                    String parentRuleName = PCFG.removeListAnnotation(s.getName());
                    Symbol parent = cfg.get(parentRuleName);

                    Set<String> keys = new HashSet<>(s.getRulesMap().keySet());
                    keys.add(Symbol.EPSILON.getName());
                    List<String> states = new ArrayList<>(s.getRulesMap().keySet());
                    // Sort states by alphabetical order
                    Collections.sort(states);
                    Map<String, Integer> statesIdx = new HashMap<>();
                    for (int i = 0; i < states.size(); i++) {
                        statesIdx.put(states.get(i), i);
                    }

                    // Init variables
                    double[] initState = new double[states.size()];
                    Arrays.fill(initState, 0.0);
                    double[][] transMatrix = new double[states.size()][];
                    for (int i = 0; i < transMatrix.length; i++) {
                        transMatrix[i] = new double[states.size()];
                        Arrays.fill(transMatrix[i], 0.0);
                    }
                    // Make epsilon absorbing
                    int epsilonIdx = statesIdx.get(Symbol.EPSILON.getName());
                    transMatrix[epsilonIdx][epsilonIdx] = 1.0;

                    for (List<String> sample : s.getListChildren()) {
                        if (sample.isEmpty()) {
                            // Starts at epsilon directly
                            ++initState[statesIdx.get(Symbol.EPSILON.getName())];
                        } else {
                            String prevState = sample.get(0);
                            int prevStateIdx = statesIdx.get(prevState);
                            ++initState[statesIdx.get(prevState)];
                            for (int i = 1; i < sample.size(); i++) {
                                String currentState = sample.get(i);
                                int currentStateIdx = statesIdx.get(currentState);

                                ++transMatrix[prevStateIdx][currentStateIdx];
                                prevStateIdx = currentStateIdx;
                            }
                            String lastState = sample.get(sample.size() - 1);
                            int lastStateIdx = statesIdx.get(lastState);
                            ++transMatrix[lastStateIdx][statesIdx.get(Symbol.EPSILON.getName())];
                        }
                    }
                    // Normalize
                    for (int i = 0; i < transMatrix.length; i++) {
                        double sumRow = Arrays.stream(transMatrix[i]).sum();
                        if (sumRow == 0) {
                            // State never occurred, make it absorbing TODO smooth?
                            transMatrix[i][i] = 1.0;
                            continue;
                        }
                        for (int j = 0; j < transMatrix[i].length; j++) {
                            //transMatrix[i][j] = DoubleUtil.log2(transMatrix[i][j]) - DoubleUtil.log2(sumRow);
                            transMatrix[i][j] = transMatrix[i][j] / sumRow;
                        }
                    }
                    double sumInit = Arrays.stream(initState).sum();
                    if (sumInit == 0) {
                        initState[epsilonIdx] = 1.0;
                    } else {
                        for (int i = 0; i < initState.length; i++) {
                            //initState[i] = DoubleUtil.log2(initState[i]) - DoubleUtil.log2(sumInit);
                            initState[i] = initState[i] / sumInit;
                        }
                    }
                    // All rows should sum to 1
                    assert (Math.abs(1.0 - Arrays.stream(initState).sum()) < DoubleUtil.PRECISION);
                    assert (Arrays.stream(transMatrix)
                            .allMatch(row -> Math.abs(1.0 - Arrays.stream(row).sum()) < DoubleUtil.PRECISION));

                    MarkovChain<String> mc = new MarkovChain<>(transMatrix, initState, states,
                            statesIdx, Symbol.EPSILON.getName());
                    parent.setMC(mc);
                    s.setMC(mc);
                });
    }

    private void fitGeometric() {
        // For list type rules, we're want to fit a geometric distribution
        this.cfg.values().stream()
                .filter(s -> s.getName().endsWith(PCFG.getListAnnotatedName("")))
                .forEach(s -> {
                    // Empty children list won't appear in occurrences lists, fix this based on parent count
                    String parentRuleName = PCFG.removeListAnnotation(s.getName());
                    Symbol parent = cfg.get(parentRuleName);
                    int emptyChildCount = 0;
                    if (parent.findEpsilonRule() != null) {
                        emptyChildCount = parent.getCount() - s.getListChildren().size();
                    }
                    // Add empty children to occurrences
                    for (int i = 0; i < emptyChildCount; i++) {
                        s.addStack();
                        s.removeStack();
                    }

                    int n = s.getListChildren().size();
                    // We model the geometric distribution for lists
                    // as k trials (k = 1, 2, ...) where the success is epsilon (epsilon "is" at list.size() + 1)
                    int sumSamples = s.getListChildren().stream().mapToInt(sample -> sample.size() + 1).sum();
                    double geomPHat = n / (double) sumSamples;
                    parent.setGeomParam(geomPHat);
                    s.setGeomParam(geomPHat);
                });
    }

    private void computeMLLogProb() {
        // Compute the log prob for each rule
        this.cfg.values().forEach(s -> {
            s.getRules().forEach(r -> {
                double prob = DoubleUtil.ZERO_LOG2P;
                if (r.getCount() != 0 && s.getCount() != 0) {
                    prob = r.getCount() / (double) s.getCount();
                }
                assert (!Double.isNaN(DoubleUtil.log2(prob)) && !Double.isInfinite(DoubleUtil.log2(prob)));
                r.setProb(DoubleUtil.log2(prob));
            });
        });
    }

    public double getDataCodingLength() {
        double length = 0.0;
        for (Symbol symbol : cfg.values()) {
            if (!symbol.getRules().isEmpty() && symbol.getCount() != 0) {
                if (!symbol.getName().endsWith(PCFG.getListAnnotatedName(""))) {
                    for (SymbolsRHS rule : symbol.getRules()) {
                        length += -(rule.getProb() * rule.getCount());
                    }
                } else {
                    for (List<String> sample : symbol.getListChildren()) {
                        // Encode prob of encountering sample of this size
                        //int k = sample.size() + 1;
                        //double p = symbol.getGeomParam();
                        //double q = 1 - p;
                        //double probSuccess = Math.pow(q, k - 1) * p;
                        //length += -(DoubleUtil.log2(probSuccess));

                        // Encode prob of encountering this sequence
                        MarkovChain<String> mc = symbol.getMC();
                        double prob = mc.run(sample);
                        length += -(DoubleUtil.log2(prob));
                    }
                }
            }
        }
        return length;
    }

    private List<String> findRoots() {
        // Find all the tags appearing in the grammar
        List<String> unannotated = this.cfg.entrySet().stream()
                .filter(e -> !e.getKey().contains(PARENT_ANNOTATION) && e.getValue() != Symbol.EPSILON)
                .map(Map.Entry::getKey).collect(Collectors.toList());
        // If a tag is used as a child, remove it from candidates
        unannotated.removeIf(tag -> cfg.keySet().stream().anyMatch(k -> k.startsWith(tag + PARENT_ANNOTATION)));
        if (unannotated.isEmpty()) throw new IllegalArgumentException("Unable to find root node");
        return unannotated;
    }

    private Symbol buildRHSList(Node currentChild, Node currentChildAttr, Symbol lhsSymbol,
                                boolean isMandatory, Symbol.Order orderType) {
        Symbol rhsSymbol = buildRHS(currentChild, currentChildAttr, lhsSymbol, true);

        // Replace rhs by a new production which can build a list of n symbols
        Symbol listProduction = Symbol.newSymbol(
                getListAnnotatedName(rhsSymbol.getName()));
        // Copy the rules
        listProduction.setRules(rhsSymbol.getRules());
        // Add epsilon
        listProduction.addRule(SymbolsRHS.newRHS().addSymbol(Symbol.EPSILON));
        // List rule is one or more children
        listProduction.setChildren(Symbol.Children.ONE_OR_MORE);
        // Set appropriate order type of children
        listProduction.setOrder(orderType);

        // Point rhs to this new production
        List<SymbolsRHS> newSymbols = new ArrayList<>();
        newSymbols.add(SymbolsRHS.newRHS().addSymbol(listProduction));
        rhsSymbol.setRules(newSymbols);

        listProduction.getRules().stream()
                .filter(r -> !r.isEpsilon())
                .forEach(r -> r.addSymbol(listProduction));
        // At this point listProduction is : L -> a L | epsilon
        if (!isMandatory) {
            rhsSymbol.addRule(SymbolsRHS.newRHS().addSymbol(Symbol.EPSILON));
            rhsSymbol.setMandatory(false);
        }
        // List symbol is part of production rules, add it
        this.cfg.put(listProduction.getName().toLowerCase(), listProduction);
        return rhsSymbol;
    }

    private Symbol buildRHS(Node currentChild, Node currentChildAttr, Symbol lhsSymbol, boolean isMandatory) {
        Symbol rhsSymbol;
        String targetNode = currentChildAttr.getNodeValue();
        if (abstractNodes.containsKey(targetNode)) {
            // Expand abstract node with all concrete nodes
            rhsSymbol = Symbol.newSymbol(getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName()));

            for (String concrete : abstractNodes.get(targetNode)) {
                String annotatedName = getParentAnnotatedName(concrete, currentChild.getNodeName());
                Symbol annotatedSymbol = cfg.getOrDefault(annotatedName,
                        Symbol.newSymbol(annotatedName));

                // Copy rules from concrete type
                Symbol concreteSymbol = cfg.getOrDefault(concrete.toLowerCase(),
                        Symbol.newSymbol(concrete.toLowerCase()));
                annotatedSymbol.setRules(concreteSymbol.getRules());
                cfg.put(concrete.toLowerCase(), concreteSymbol);

                rhsSymbol.addRule(SymbolsRHS.newRHS().addSymbol(annotatedSymbol));
                cfg.put(annotatedName, annotatedSymbol);
            }
        } else {
            rhsSymbol = cfg.getOrDefault(
                    getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName()),
                    Symbol.newSymbol(getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName())));
            // Add the unannotated target symbol to rhs rules
            Symbol targetRhsSymbolUnannotated = cfg.getOrDefault(targetNode.toLowerCase(),
                    Symbol.newSymbol(targetNode.toLowerCase()));

            // Also add target symbol, annotated with its parent
            Symbol targetRhsSymbolAnnotated = cfg.getOrDefault(
                    getParentAnnotatedName(targetNode, currentChild.getNodeName()),
                    Symbol.newSymbol(getParentAnnotatedName(targetNode, currentChild.getNodeName())));

            // The symbol should point to the annotated symbol
            rhsSymbol.addRule(SymbolsRHS.newRHS().addSymbol(targetRhsSymbolAnnotated));
            // The rules are the same & are based on the unannotated node
            targetRhsSymbolAnnotated.setRules(targetRhsSymbolUnannotated.getRules());

            // Put both in cfg: when reading the xml grammar, a node may match the unannotated symbol
            // and populate its rules. Those rules will be then used by all the annotated symbol
            // regardless of how many there is.
            cfg.put(targetRhsSymbolUnannotated.getName(), targetRhsSymbolUnannotated);
            cfg.put(targetRhsSymbolAnnotated.getName(), targetRhsSymbolAnnotated);
        }
        if (!isMandatory) {
            rhsSymbol.addRule(SymbolsRHS.newRHS().addSymbol(Symbol.EPSILON));
            rhsSymbol.setMandatory(false);
        }
        return rhsSymbol;
    }

    /**
     * Get the key in the cfg for a production of type A -> B where B is the child and A the parent.
     * @param child
     * @param parent
     * @return
     */
    public static String getParentAnnotatedName(String child, String parent) {
        return (child + PARENT_ANNOTATION + parent).toLowerCase();
    }

    /**
     * Get the key in the cfg for a production of type L -> a L | epsilon where a is a terminal.
     * @param name
     * @return
     */
    public static String getListAnnotatedName(String name) {
        return (name + PARENT_ANNOTATION + "list").toLowerCase();
    }

    public static String removeListAnnotation(String name) {
        String annotation = PCFG.getListAnnotatedName("");
        return (name.endsWith(annotation) ? name.substring(0, name.length() - annotation.length()) : name);
    }

    /**
     * Sets the delimiter between the child and parent. Should be unique, fixed and not appear in the grammar.
     * @param parentAnnotation
     */
    public static void setParentAnnotation(String parentAnnotation) {
        PARENT_ANNOTATION = parentAnnotation;
    }

    /**
     * Build a map of abstract nodes and their concrete nodes. The set contains only concrete nodes regardless
     * of the level of abstractions (ie nested abstract nodes).
     * @param root
     * @return
     */
    private static Map<String, Set<String>> loadAbstractNodes(Node root) {
        Map<String, Set<String>> res = new HashMap<>();
        NodeList childrenNodes = root.getChildNodes();
        for (int t = 0; t < childrenNodes.getLength(); ++t) {
            if (childrenNodes.item(t).hasAttributes()
                    && (childrenNodes.item(t).getNodeType() == Node.ELEMENT_NODE)) {
                Node currentChild = childrenNodes.item(t);
                Node abs = currentChild.getAttributes().getNamedItem("abstract");
                if (abs != null && Boolean.valueOf(abs.getNodeValue())) {
                    Set<String> abstractNodes = res.getOrDefault(currentChild.getNodeName(), new HashSet<>());
                    abstractNodes.add(currentChild.getFirstChild().getNodeName());
                    res.put(currentChild.getNodeName(), abstractNodes);
                }
            }
        }

        // An abstract node can have abstract children, fixpoint until everything is expanded
        boolean fix = false;
        while (!fix) {
            fix = true;
            for (Map.Entry<String, Set<String>> e : res.entrySet()) {
                Set<String> remove = new HashSet<>();
                Set<String> add = new HashSet<>();
                for (String concrete : e.getValue()) {
                    if (res.containsKey(concrete)) {
                        fix = false;
                        remove.add(concrete);
                        add.addAll(res.get(concrete));
                    }
                }
                e.getValue().removeAll(remove);
                e.getValue().addAll(add);
            }
        }
        return res;
    }

    /**
     * Returns a map <String, String> where the key is the value of the image attribute
     * and the value the name of the node (e.g. != : NOT_EQUALS)
     * @param root
     * @return
     */
    private static Map<String, String> loadImageNodes(Node root) {
        Map<String, String> res = new HashMap<>();
        NodeList childrenNodes = root.getChildNodes();
        for (int t = 0; t < childrenNodes.getLength(); ++t) {
            if (childrenNodes.item(t).hasAttributes()
                    && (childrenNodes.item(t).getNodeType() == Node.ELEMENT_NODE)) {
                Node currentChild = childrenNodes.item(t);
                Node abs = currentChild.getAttributes().getNamedItem("abstract");
                if (abs != null && Boolean.valueOf(abs.getNodeValue())) {
                    Node image = currentChild.getFirstChild().getAttributes().getNamedItem("image");
                    if (image != null) {
                        res.put(image.getNodeValue(), currentChild.getFirstChild().getNodeName());
                    }
                }
            }
        }

        return res;
    }

    public Map<String, Symbol> getCfg() {
        return cfg;
    }

    /**
     * Returns the terminals in the grammar (i.e. the symbols with no rhs rules)
     * @return
     */
    public Map<String, Symbol> getTerminals() {
        return cfg.entrySet().stream()
                .filter(e -> e.getValue().getRules().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a map <String, String> where the key is the value of the image attribute
     * and the value the name of the node (e.g. != : NOT_EQUALS).
     * @return
     */
    public Map<String, String> getImages() {
        return this.images;
    }

    public List<String> getRoots() {
        return this.possibleRoots;
    }

    public String toPrettyString(boolean showCounts) {
        BiFunction<StringBuilder, String, Void> appendIf = (StringBuilder sb, String val) -> {
            if (showCounts) {
                sb.append(val);
            }
            return null;
        };
        StringBuilder prod = new StringBuilder();
        prod.append("Productions: \n");
        StringBuilder terminals = new StringBuilder();
        terminals.append("Terminals: \n");
        for (Map.Entry<String, Symbol> e : this.cfg.entrySet()) {
            Symbol symbol = e.getValue();
            if (symbol.getRules().isEmpty() || symbol.getName().equals(Symbol.EPSILON.getName())) {
                terminals.append(symbol.toPrettyString());
                appendIf.apply(terminals, "(Count:" + symbol.getCount() + ")");
                terminals.append(" ");
            } else {
                prod.append(symbol.toPrettyString());
                appendIf.apply(prod, "(Count:" + symbol.getCount() + ")");
                prod.append(" -> \n\t\t");
                for (SymbolsRHS rhs : symbol.getRules()) {
                    prod.append("[").append(rhs.toString()).append("]");
                    appendIf.apply(prod, "(Count:" + rhs.getCount() + ", Log2P:" + rhs.getProb() + ")");
                    prod.append(" | ");
                }
                prod.delete(prod.length() - 3, prod.length() - 1);
                prod.append("\n");
            }
        }
        return prod.toString() + "\n" + terminals.toString();
    }

    public String toPrettyStringDebug() {
        Map<String, String> names = new HashMap<>();
        names.put(Symbol.EPSILON.getName(), "EPSILON");
        int i = 1;
        StringBuilder prod = new StringBuilder();
        prod.append("Productions: \n");
        StringBuilder terminals = new StringBuilder();
        terminals.append("Terminals: \n");
        for (Map.Entry<String, Symbol> e : this.cfg.entrySet()) {
            Symbol symbol = e.getValue();
            if (symbol.getRules().isEmpty() || symbol.getName().equals(Symbol.EPSILON.getName())) {
                terminals.append(symbol.toPrettyString());
                terminals.append(" ");
            } else if (symbol.getRules().size() >= 1 && !symbol.getRules().get(0).getRhs().isEmpty()) {

                String prepend;
                if (symbol.getName().equals(Symbol.EPSILON.getName())) {
                    prepend = "";
                } else {
                    prepend = symbol.getName().split("\\^")[0] + " ";
                }

                String symbolName = symbol.getName();

                //String symbolName;
                //if (names.containsKey(symbol.getName())) {
                //    symbolName = names.get(symbol.getName());
                //} else {
                //    symbolName = "P" + String.valueOf(i);
                //    names.put(symbol.getName(), symbolName);
                //    ++i;
                //}
                prod.append(prepend).append(symbolName);
                prod.append(" -> ");
                for (SymbolsRHS rhs : symbol.getRules()) {
                    StringBuilder current = new StringBuilder();
                    Symbol last = null;
                    for (Symbol x : rhs.getRhs()) {
                        String newName = x.getName();
                        //String newName = "";
                        //if (names.containsKey(x.getName())) {
                        //    newName += names.get(x.getName());
                        //} else {
                        //    newName += "P" + String.valueOf(i);
                        //    names.put(x.getName(), newName);
                        //    ++i;
                        //}
                        current.append(newName.toUpperCase()).append(" ");
                        last = x;
                    }
                    //if (last.getName().endsWith(PCFG.getListAnnotatedName(""))
                    //        && !symbol.getName().endsWith(PCFG.getListAnnotatedName(""))) {
                    //    // Production X -> L where L is a recursive list production
                    //    int debug = 0;
                    //    current.append("END ");
                    //}
                    current.deleteCharAt(current.length() - 1);
                    prod.append(current);
                    prod.append(" | ");
                }
                prod.delete(prod.length() - 3, prod.length() - 1);
                prod.append("\n");
            }
        }
        return prod.toString() + "\n" + terminals.toString() + "\n" + names.toString();
    }
}
