package be.intimals.freqt.grammar;

import be.intimals.freqt.util.XMLUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.stream.Collectors;

public class PCFG {
    private Map<String, Symbol> cfg = new HashMap<>();
    private Map<String, Set<String>> abstractNodes;
    private Map<String, String> images;
    private static final String PARENT_ANNOTATION = "^";

    public void loadGrammar(String path) {
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
                            boolean isMandatory = nodeMap.getNamedItem("mandatory") == null ? false :
                                    Boolean.valueOf(nodeMap.getNamedItem("mandatory").getNodeValue());
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

                                    case "mandatory":
                                        // Not needed here
                                        break;

                                    default:
                                        System.err.println("Unknown attribute found " + currentChildAttr.getNodeName());
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
            // TODO shouldn't be hardcoded
            Symbol start = cfg.get("compilationunit");
            start.setName(start.getName() + "^sourcefile");
            cfg.put(start.getName(), start);
            System.out.println("CFG loaded");
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
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
        return rhsSymbol;
    }

    private Symbol buildRHS(Node currentChild, Node currentChildAttr, Symbol lhsSymbol, boolean isMandatory) {
        Symbol rhsSymbol;
        String targetNode = currentChildAttr.getNodeValue();
        if (abstractNodes.containsKey(targetNode)) {
            // Expand abstract node with all concrete nodes
            rhsSymbol = Symbol.newSymbol(getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName()));

            for (String concrete : abstractNodes.get(targetNode)) {
                String annotatedName = getParentAnnotatedName(concrete, currentChild.getNodeName()).toLowerCase();
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
                    getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName()).toLowerCase(),
                    Symbol.newSymbol(
                            getParentAnnotatedName(currentChild.getNodeName(), lhsSymbol.getName()).toLowerCase()));
            // Add the unannotated target symbol to rhs rules
            Symbol targetRhsSymbolUnannotated = cfg.getOrDefault(targetNode.toLowerCase(),
                    Symbol.newSymbol(targetNode.toLowerCase()));

            // Also add target symbol, annotated with its parent
            Symbol targetRhsSymbolAnnotated = cfg.getOrDefault(
                    getParentAnnotatedName(targetNode.toLowerCase(), currentChild.getNodeName()).toLowerCase(),
                    Symbol.newSymbol(getParentAnnotatedName(targetNode.toLowerCase(),
                            currentChild.getNodeName()).toLowerCase()));

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
        return child + PARENT_ANNOTATION + parent;
    }

    /**
     * Get the key in the cfg for a production of type L -> a L | epsilon where a is a terminal.
     * @param name
     * @return
     */
    public static String getListAnnotatedName(String name) {
        return name + PARENT_ANNOTATION + "list";
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

    public void setCfg(Map<String, Symbol> cfg) {
        this.cfg = cfg;
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
     * and the value the name of the node (e.g. != : NOT_EQUALS)
     * @return
     */
    public Map<String, String> getImages() {
        return this.images;
    }
}
