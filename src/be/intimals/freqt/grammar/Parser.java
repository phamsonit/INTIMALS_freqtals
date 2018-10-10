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

    public Parser(PCFG pcfg, String rootName) {
        this.rootName = rootName;
        this.cfg = pcfg.getCfg();
        this.terminals  = pcfg.getTerminals();
        this.images = pcfg.getImages();
    }

    public void parseDirectory(String path) {
        try {
            List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
            for (File f : files) {
                System.out.println("Parsing : " + f.getName());
                Node root = XMLUtil.getXMLRoot(f);
                parseSingleXML(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseSingleXML(Node root) {
        // TODO assuming root is the parent of the actual root(ie root is SourceFile but "true" root is CompilationUnit)
        // TODO variable root
        this.stack = new ArrayDeque<>();
        this.stack.push(new Pair<>(cfg.get(PCFG.getParentAnnotatedName(rootName, "sourcefile")
                .toLowerCase()), null));
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
                        parentNode.getNodeName()).toLowerCase()
                        .equals(currentSymbol.getName())) {
                    currentSymbol.incCount();
                    System.out.println("EQUALS " + currentSymbol.getName());
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
                            PCFG.getParentAnnotatedName(childNodeName, currentNode.getNodeName()).toLowerCase());
                    if (rule == null && (
                            (found.getRules().size() == 1 && found.findEpsilonRule() == null)
                            || found.getRules().size() == 2 && found.findEpsilonRule() != null)) {
                        rule = found.getRules().get(0);
                    } else if (rule == null) {
                        throw new IllegalArgumentException("Ambiguous "
                                + PCFG.getParentAnnotatedName(childNodeName, currentNode.getNodeName()).toLowerCase());
                    }
                    // Increment rule usage counter
                    rule.incCount();
                    // Replace lhs non-terminal by rhs symbols
                    stack.poll();
                    List<Symbol> rhs = rule.getRhs();
                    for (int j = rhs.size() - 1; j >= 0; j--) {
                        Symbol next = rhs.get(j);
                        if (next.getName().endsWith("^list")) {
                            next.addStack();
                        }
                        this.stack.push(new Pair<>(next, currentSymbol));
                    }
                    // Advance in input
                    dfsIterator.next();
                } else {
                    // Symbol & input not matching, find appropriate production rule
                    SymbolsRHS rule = currentSymbol.getRulesByName(
                            PCFG.getParentAnnotatedName(currentNode.getNodeName(),
                                    parentNode.getNodeName()).toLowerCase());
                    if (rule != null) {
                        rule.incCount();
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
            } else if (currentNode.getNodeType() == Node.TEXT_NODE) { // Terminal in XML
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

    private void tryEpsilon(Symbol currentSymbol, Symbol parentSymbol) {
        SymbolsRHS epsilon = currentSymbol.findEpsilonRule();
        if (epsilon != null) {
            epsilon.incCount();
            System.out.println("Epsilon for: " + currentSymbol.getName());
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
}
