package be.intimals.freqt.mdl.miner;

import java.util.HashMap;
import java.util.Map;

public class TabuMoves {
    private int tenure;
    private Map<Integer, Integer> tabu = new HashMap<>();

    private TabuMoves(int tenure) {
        this.tenure = tenure;
    }

    public static TabuMoves create(int tenure) {
        return new TabuMoves(tenure);
    }

    public void add(int id, int iteration) {
        tabu.put(id, iteration);
    }

    public boolean isTabu(int id, int currentIteration) {
        return tabu.containsKey(id) && tabu.get(id) + tenure > currentIteration;
    }
}