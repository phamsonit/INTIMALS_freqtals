package be.intimals.freqt.grammar;

import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.XMLUtil;
import javafx.util.Pair;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class Parser {
    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

    private PCFG pcfg;
    private Deque<Pair<Symbol, Symbol>> stack;
    private PeekableIterator<Node> dfsIterator;
    private String rootName;
    private Map<String, Symbol> cfg;
    private Map<String,Symbol> terminals;
    private Map<String, String> images;
    private int currentTid;

    public Parser(PCFG pcfg, String rootName) throws IOException {
        this.pcfg = pcfg;
        this.rootName = rootName.toLowerCase();
        this.cfg = pcfg.getCfg();
        if (!this.cfg.containsKey(this.rootName)) throw new IllegalArgumentException("Root not found");
        // TODO hacky, SourceFile not in grammar issue
        Symbol root = this.cfg.get(this.rootName);
        root.setName(PCFG.getParentAnnotatedName(root.getName(), "SourceFile"));
        this.cfg.put(root.getName(), root);

        this.terminals  = pcfg.getTerminals();
        this.images = pcfg.getImages();
        this.currentTid = 0;

        FileHandler handler = new FileHandler("out/parser_log.txt");
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "%3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        LOGGER.addHandler(handler);
    }

    public void parseDirectory(String path) {
        try {
            List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
            currentTid = 0;
            for (File f : files) {
                LOGGER.info("Parsing : " + f.getName());
                Node root = XMLUtil.getXMLRoot(f);
                parseSingleXML(root);
                ++currentTid;
            }
            pcfg.computeProbabilities();
            LOGGER.info(pcfg.toPrettyString(true));
        } catch (Exception e) {
            LOGGER.severe("Unable to parse directory " + e.getMessage());
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
            currentSymbol.incCount();

            Node currentNode = dfsIterator.peek();
            Node parentNode = currentNode.getParentNode();

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                LOGGER.info(showParseState(stack, currentNode));

                if (PCFG.getParentAnnotatedName(currentNode.getNodeName(), parentNode.getNodeName())
                        .equals(currentSymbol.getName())) {
                    // Symbol & input matching, find next production rule
                    handleMatching(currentSymbol, parentSymbol, currentNode, parentNode);
                } else {
                    // Symbol & input not matching, find appropriate production rule
                    handleNotMatching(currentSymbol, parentSymbol, currentNode, parentNode);
                }
            } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
                handleTerminal(currentSymbol);
            } else {
                LOGGER.severe("Should not have empty nodes in xml");
            }
        }

        // No more input, all other rules should resolve to epsilon or throw an error
        while (!stack.isEmpty()) {
            Pair<Symbol, Symbol> item = stack.peek();
            Symbol currentSymbol = item.getKey();
            Symbol parentSymbol = item.getValue();

            currentSymbol.incCount();
            tryEpsilon(currentSymbol, parentSymbol);
        }
        LOGGER.info("Done");
    }

    private void handleTerminal(Symbol currentSymbol) {
        // Terminal in XML
        // Should also match a terminal production rule
        if (!terminals.containsKey(currentSymbol.getName())) {
            throw new IllegalArgumentException("Terminal found when not expected :" + currentSymbol.getName());
        } // else continue expanding the stack
        dfsIterator.next();
        stack.poll();
    }

    private void handleMatching(Symbol currentSymbol, Symbol parentSymbol, Node currentNode, Node parentNode) {
        LOGGER.info("[OK] " + currentSymbol.getName());
        if (parentSymbol != null && parentSymbol.getOrder() != Symbol.Order.NONE) {
            parentSymbol.addChild(currentSymbol.getName());
        }

        String ruleName = PCFG.getParentAnnotatedName(currentNode.getNodeName(), parentNode.getNodeName());
        Symbol found = cfg.get(ruleName.toLowerCase());

        // Parent is now currentNode
        Node childNode = currentNode.getFirstChild();
        if (childNode != null) {
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

            LOGGER.info("[M] " + showUsedRule(currentSymbol, rule, currentNode, parentNode));

            // Increment rule usage counter
            rule.incCount();
            //rule.addOccurrence(currentTid, getNodeID(currentNode));

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
        } else {
            // XML tag with no children, symbol shouldn't have rules neither
            if (!found.getRules().isEmpty()) throw new IllegalArgumentException("Empty node");
            stack.poll();
        }
        // Advance in input
        dfsIterator.next();
    }

    private void handleNotMatching(Symbol currentSymbol, Symbol parentSymbol, Node currentNode, Node parentNode) {
        SymbolsRHS rule = currentSymbol.getRulesByName(
                PCFG.getParentAnnotatedName(currentNode.getNodeName(),
                        parentNode.getNodeName()));
        if (rule != null) {
            LOGGER.info("[D] " + showUsedRule(currentSymbol, rule, currentNode, parentNode));
            rule.incCount();
            //rule.addOccurrence(currentTid, getNodeID(currentNode));
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
            LOGGER.info("[E] " + showUsedRule(currentSymbol, epsilon, currentNode, parentNode));
            //epsilon.addOccurrence(currentTid, getNodeID(currentNode));

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


    private static String showUsedRule(Symbol currentSymbol, SymbolsRHS rule, Node currentNode, Node parentNode) {
        return "RULE: "
                + "(" + getNodeID(currentNode) + ", " + getNodeID(parentNode) + ") "
                + currentSymbol.getName()
                + " -> "
                + rule.toString();
    }

    private static String showParseState(Deque<Pair<Symbol, Symbol>> stack, Node currentNode) {
        String nodeID = currentNode.getAttributes().getNamedItem("ID").getNodeValue();
        return "\tInput: " + currentNode.getNodeName()
                + "(" + nodeID + ") "
                + "Stack: " + stack.stream().map(x -> x.getKey().getName()).collect(Collectors.toList());
    }

    private static String getNodeID(Node node) {
        return Optional.ofNullable(node)
                .map(Node::getAttributes).map(x -> x.getNamedItem("ID")).map(Node::getNodeValue)
                .orElse("");
    }
}
