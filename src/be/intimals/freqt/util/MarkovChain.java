package be.intimals.freqt.util;

import java.util.List;
import java.util.Map;

public class MarkovChain<T> {
    private double[][] transMatrix;
    private double[] initState;
    private List<T> states;
    private Map<T, Integer> statesIdx;
    private T empty;

    public MarkovChain(double[][] transMatrix, double[] initState, List<T> states, Map<T, Integer> statesIdx,
                       T empty) {
        this.transMatrix = transMatrix;
        this.initState = initState;
        this.states = states;
        this.statesIdx = statesIdx;
        this.empty = empty;
        if (!statesIdx.containsKey(empty)) throw new IllegalArgumentException("empty state not in states list");
    }

    public double runLog(List<T> steps) {
        double logProb = 0.0;
        int emptyIdx = getIdx(empty);

        if (steps.isEmpty()) {
            logProb += initState[emptyIdx];
        } else {
            // TODO error handling ie not in map
            T prev = steps.get(0);
            int prevIdx = getIdx(prev);
            logProb += initState[prevIdx];
            int currentIdx;
            for (int i = 1; i < steps.size(); i++) {
                T current = steps.get(i);
                currentIdx = getIdx(current);

                logProb += transMatrix[prevIdx][currentIdx];

                prevIdx = currentIdx;
            }
            int lastStateIdx = getIdx(steps.get(steps.size() -1));
            logProb += transMatrix[lastStateIdx][emptyIdx];
        }

        return logProb;
    }

    public double run(List<T> steps) {
        double prob;
        int emptyIdx = getIdx(empty);

        if (steps.isEmpty()) {
            prob = initState[emptyIdx];
        } else {
            // TODO error handling ie not in map
            T prev = steps.get(0);
            int prevIdx = getIdx(prev);
            prob = initState[prevIdx];
            int currentIdx;
            for (int i = 1; i < steps.size(); i++) {
                T current = steps.get(i);
                currentIdx = getIdx(current);

                prob *= transMatrix[prevIdx][currentIdx];

                prevIdx = currentIdx;
            }
            int lastStateIdx = getIdx(steps.get(steps.size() -1));
            prob *= transMatrix[lastStateIdx][emptyIdx];
        }

        return prob;
    }

    private int getIdx(T name) {
        if (!statesIdx.containsKey(name)) throw new IllegalArgumentException("State not in map");
        return statesIdx.get(name);
    }

    public double[][] getTransMatrix() {
        return transMatrix;
    }

    public void setTransMatrix(double[][] transMatrix) {
        this.transMatrix = transMatrix;
    }

    public double[] getInitState() {
        return initState;
    }

    public void setInitState(double[] initState) {
        this.initState = initState;
    }

    public List<T> getStates() {
        return states;
    }

    public void setStates(List<T> states) {
        this.states = states;
    }

    public Map<T, Integer> getStatesIdx() {
        return statesIdx;
    }

    public void setStatesIdx(Map<T, Integer> statesIdx) {
        this.statesIdx = statesIdx;
    }
}
