package be.intimals.freqt.grammar;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SymbolsRHS {
    private List<Symbol> rhs;
    private double prob;
    private int count;
    private List<Pair<Integer, Integer>> occurrences = new ArrayList<>();

    public SymbolsRHS() {
        this.rhs = new ArrayList<>();
    }

    public SymbolsRHS(SymbolsRHS other) {
        this.rhs = new ArrayList<>();
        rhs.addAll(other.getRhs());
        this.occurrences = new ArrayList<>();
    }

    public static SymbolsRHS newRHS() {
        return new SymbolsRHS();
    }

    public static SymbolsRHS newRHS(SymbolsRHS other) {
        return new SymbolsRHS(other);
    }

    public List<Symbol> getRhs() {
        return rhs;
    }

    public SymbolsRHS addSymbol(Symbol symbol) {
        this.rhs.add(symbol);
        return this;
    }

    public double getProb() {
        return prob;
    }

    public SymbolsRHS setProb(double prob) {
        this.prob = prob;
        return this;
    }

    public boolean isEpsilon() {
        return this.rhs.size() == 1 && this.rhs.get(0).equals(Symbol.EPSILON);
    }

    public void incCount() {
        this.count++;
    }

    public int getCount() {
        return this.count;
    }

    public void addOccurrence(Integer tid, String id) {
        try {
            this.occurrences.add(new Pair<>(tid, Integer.valueOf(id)));
        } catch (NumberFormatException e) {
            // Don't add
        }
    }

    public List<Pair<Integer, Integer>> getOccurrences() {
        return this.occurrences;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" -> ");
        for (Symbol s : rhs) {
            sb.append(s.toPrettyString()).append(" ");
        }

        return sb.toString();
    }
}
