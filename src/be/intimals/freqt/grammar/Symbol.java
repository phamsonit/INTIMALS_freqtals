package be.intimals.freqt.grammar;

import java.util.*;

public class Symbol {
    public enum Order {
        ORDERED, UNORDERED, NONE
    }

    public enum Children {
        N, ONE_OR_MORE, NONE
    }

    public static final Symbol EPSILON = newSymbol("");

    private String name;
    private Order order = Order.NONE;
    private Children children = Children.N;
    private boolean mandatory = true;
    private List<SymbolsRHS> rules;
    private Map<String, SymbolsRHS> rulesMapped;
    private int count;
    private List<List<String>> listChildren = new ArrayList<>();
    private Deque<List<String>> stackChildren = new ArrayDeque<>();

    public Symbol(String name) {
        this.name = name;
        this.rules = new ArrayList<>();
        this.rulesMapped = null;
    }

    public static Symbol newSymbol(String name) {
        return new Symbol(name.toLowerCase()).setMandatory(true).setOrder(Order.NONE).setChildren(Children.NONE);
    }

    public static Symbol newSymbol(Symbol other) {
        return new Symbol(other.getName())
                .setMandatory(other.mandatory).setOrder(other.order).setChildren(other.children);
    }

    /**
     * Once the rule is built, transform to map (first symbol in rule) -> rule. Useful for parser.
     */
    public void lockAsMap() {
        if (rules.isEmpty()) throw new IllegalArgumentException("No rules");
        if (rulesMapped == null) {
            rulesMapped = new HashMap<>();
            for (SymbolsRHS rhs : rules) {
                assert (!rulesMapped.containsKey(rhs.getRhs().get(0).getName()));
                rulesMapped.put(rhs.getRhs().get(0).getName(), rhs);
            }
        }
    }

    public SymbolsRHS findEpsilonRule() {
        // TODO cache index of epsilon rule instead of searching
        for (SymbolsRHS rhs : rules) {
            if (rhs.isEpsilon()) return rhs;
        }
        return null;
    }

    public Symbol addRule(SymbolsRHS rule) {
        this.rules.add(rule);
        return this;
    }


    public String getName() {
        return name;
    }

    public Symbol setName(String name) {
        this.name = name;
        return this;
    }

    public List<SymbolsRHS> getRules() {
        return rules;
    }

    public Symbol setRules(List<SymbolsRHS> rules) {
        this.rules = rules;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public Symbol setOrder(Order order) {
        this.order = order;
        return this;
    }

    public Children getChildren() {
        return children;
    }

    public Symbol setChildren(Children children) {
        this.children = children;
        return this;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public Symbol setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public Map<String, SymbolsRHS> getRulesMap() {
        return this.rulesMapped;
    }

    public SymbolsRHS getRulesByName(String name) {
        lockAsMap();
        if (name == null) {
            return findEpsilonRule();
        } else {
            return rulesMapped.get(name);
        }
    }

    public void incCount() {
        this.count++;
    }

    public int getCount() {
        return this.count;
    }

    public List<List<String>> getListChildren() {
        return listChildren;
    }

    public void addChild(String child) {
        this.stackChildren.peek().add(child);
    }

    public void removeStack() {
        this.listChildren.add(this.stackChildren.poll());
    }

    public void addStack() {
        this.stackChildren.push(new ArrayList<>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Symbol{"
                + "name='" + toPrettyString() + '\''
                + '}';
    }

    public String toPrettyString() {
        return (name.equals(Symbol.EPSILON.getName()) ? "EPSILON" : name);
    }
}
