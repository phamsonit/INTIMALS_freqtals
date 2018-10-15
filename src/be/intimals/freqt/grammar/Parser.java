package be.intimals.freqt.grammar;

import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.XMLUtil;
import javafx.util.Pair;
import org.w3c.dom.Node;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    private Deque<Pair<Symbol, Symbol>> stack;
    private PeekableIterator<Node> dfsIterator;
    private String rootName;
    private Map<String, Symbol> cfg;
    private Map<String,Symbol> terminals;
    private Map<String, String> images;
    private int currentTid;

    public Parser(PCFG pcfg, String rootName) {
        this.rootName = rootName.toLowerCase();
        this.cfg = pcfg.getCfg();
        this.terminals  = pcfg.getTerminals();
        this.images = pcfg.getImages();
        this.currentTid = 0;
    }

    public void parseDirectory(String path) {
        try {
            List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
            currentTid = 0;
            for (File f : files) {
                System.out.println("Parsing : " + f.getName());
                Node root = XMLUtil.getXMLRoot(f);
                parseSingleXML(root);
                ++currentTid;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseSingleXML(Node root) {
        this.stack = new ArrayDeque<>();
        this.stack.push(new Pair<>(cfg.get(rootName), null));
        this.dfsIterator = XMLUtil.asPreorderIterator(XMLUtil.asIterator(root.getChildNodes()), (Node e) ->
                XMLUtil.asIterator(e.getChildNodes()));
        this.dfsIterator.next();

        int debug = 0;
        while (dfsIterator.hasNext()) {
            ++debug;
            Pair<Symbol, Symbol> item = stack.peek();
            Symbol currentSymbol = item.getKey();
            Symbol parentSymbol = item.getValue();

            if (currentSymbol == null) throw new NullPointerException("Null symbol with remaining input");

            Node currentNode = dfsIterator.peek();
            Node parentNode = currentNode.getParentNode();

            String debugID = (currentNode.getAttributes() != null
                    && currentNode.getAttributes().getNamedItem("ID") != null
                    ? currentNode.getAttributes().getNamedItem("ID").getNodeValue() : "");
            System.out.println(" Input: " + currentNode.getNodeName()
                    + "(" + debugID + ") "
                    + "Stack: " + stack.stream().map(x -> x.getKey().getName()).collect(Collectors.toList()));

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                if (PCFG.getParentAnnotatedName(currentNode.getNodeName(),
                        parentNode.getNodeName())
                        .equals(currentSymbol.getName())) {
                    // Symbol & input matching, find next production rule
                    handleMatching(currentSymbol, parentSymbol, currentNode, parentNode);
                } else {
                    // Symbol & input not matching, find appropriate production rule
                    handleNotMatching(currentSymbol, parentSymbol, currentNode, parentNode);
                }
            } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
                // Terminal in XML
                currentSymbol.incCount();
                // Should also match a terminal production rule
                if (!terminals.containsKey(currentSymbol.getName())) {
                    throw new IllegalArgumentException("Terminal found when not expected :" + currentSymbol.getName());
                } // else continue expanding the stack
                dfsIterator.next();
                stack.poll();
            } else {
                System.err.println("Should not have empty nodes in xml");
            }
        }

        // No more input, all other rules should resolve to epsilon or throw an error
        while (!stack.isEmpty()) {
            Pair<Symbol, Symbol> item = stack.peek();
            Symbol currentSymbol = item.getKey();
            Symbol parentSymbol = item.getValue();

            tryEpsilon(currentSymbol, parentSymbol);
        }
        System.out.println("Done");
    }

    private void handleMatching(Symbol currentSymbol, Symbol parentSymbol, Node currentNode, Node parentNode) {
        currentSymbol.incCount();
        System.out.println("EQUALS: " + currentSymbol.getName());
        if (parentSymbol != null && parentSymbol.getOrder() != Symbol.Order.NONE) {
            //parentSymbol.addStack();
            parentSymbol.addChild(currentSymbol.getName());
        }

        String ruleName = PCFG.getParentAnnotatedName(currentNode.getNodeName(), parentNode.getNodeName());
        Symbol found = cfg.get(ruleName.toLowerCase());

        // Parent is now currentNode
        Node childNode = currentNode.getFirstChild();
        String childNodeName;

        // If string operator, find its node
        if (childNode.getNodeType() == Node.TEXT_NODE) {
            childNodeName = images.getOrDefault(childNode.getNodeValue(), childNode.getNodeValue());
        } else {
            childNodeName = childNode.getNodeName();
        }

        SymbolsRHS rule = found.getRulesByName(
                PCFG.getParentAnnotatedName(childNodeName, currentNode.getNodeName()));
        if (rule == null && (
                (found.getRules().size() == 1 && found.findEpsilonRule() == null)
                || found.getRules().size() == 2 && found.findEpsilonRule() != null)) {
            // rule of type X -> A or X -> A | epsilon possible, replace top of stack with A
            rule = found.getRules().get(0);
        } else if (rule == null) {
            throw new IllegalArgumentException("Ambiguous "
                    + PCFG.getParentAnnotatedName(childNodeName, currentNode.getNodeName()));
        }

        printUsedRule(currentSymbol, rule, currentNode, parentNode);

        // Increment rule usage counter
        rule.incCount();
        rule.addOccurrence(currentTid, getNodeID(currentNode));
        // Replace lhs non-terminal by rhs symbols
        stack.poll();
        List<Symbol> rhs = rule.getRhs();
        for (int j = rhs.size() - 1; j >= 0; j--) {
            Symbol next = rhs.get(j);
            // A list symbol should have order set
            assert (!next.getName().endsWith(PCFG.getListAnnotatedName(""))
                    || next.getOrder() != Symbol.Order.NONE);
            if (next.getOrder() != Symbol.Order.NONE) {
                // Add frame to collect children of this (un)ordered list symbol
                next.addStack();
            }
            this.stack.push(new Pair<>(next, currentSymbol));
        }
        // Advance in input
        dfsIterator.next();
    }

    private void handleNotMatching(Symbol currentSymbol, Symbol parentSymbol, Node currentNode, Node parentNode) {
        SymbolsRHS rule = currentSymbol.getRulesByName(
                PCFG.getParentAnnotatedName(currentNode.getNodeName(),
                        parentNode.getNodeName()));
        if (rule != null) {
            printUsedRule(currentSymbol, rule, currentNode, parentNode);
            rule.incCount();
            rule.addOccurrence(currentTid, getNodeID(parentNode));
            stack.poll();
            List<Symbol> rhs = rule.getRhs();
            for (int j = rhs.size() - 1; j >= 0; j--) {
                this.stack.push(new Pair<>(rhs.get(j), currentSymbol));
            }
        } else {
            tryEpsilon(currentSymbol, parentSymbol);
        }
        // Do not advance input iterator
        // Do not count current symbol as it is not a match
    }

    private void tryEpsilon(Symbol currentSymbol, Symbol parentSymbol) {
        SymbolsRHS epsilon = currentSymbol.findEpsilonRule();
        if (epsilon != null) {
            epsilon.incCount();

            Node currentNode = dfsIterator.peek();
            Node parentNode = (currentNode == null ? null : currentNode.getParentNode());
            printUsedRule(currentSymbol, epsilon, currentNode, parentNode);
            epsilon.addOccurrence(currentTid, getNodeID(parentNode));

            // Parent was a list symbol & epsilon encountered, no more children
            if (parentSymbol != null && parentSymbol.getOrder() != Symbol.Order.NONE) {
                parentSymbol.removeStack();
            }
            // Continue expanding the stack
            stack.poll();
        } else {
            throw new IllegalArgumentException("Epsilon rule not allowed here : "
                 + currentSymbol.getName());
        }
    }

    private static void printUsedRule(Symbol currentSymbol, SymbolsRHS rule, Node currentNode, Node parentNode) {
        System.out.println("RULE: "
                + currentSymbol.getName()
                + rule.toString()
                + "(" + getNodeID(currentNode) + ", " + getNodeID(parentNode) + ")");
    }

    private static String getNodeID(Node node) {
        return Optional.ofNullable(node)
                .map(Node::getAttributes).map(x -> x.getNamedItem("ID")).map(Node::getNodeValue)
                .orElse("");
    }
}
