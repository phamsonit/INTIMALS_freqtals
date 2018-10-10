package be.intimals.freqt.grammar;

import java.util.ArrayList;
import java.util.List;

public class SymbolsRHS {
    private List<Symbol> rhs;
    private double prob;
    private int count;

    public SymbolsRHS() {
        this.rhs = new ArrayList<>();
    }

    public static SymbolsRHS newRHS() {
        return new SymbolsRHS();
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
}
