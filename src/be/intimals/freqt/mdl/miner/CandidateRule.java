package be.intimals.freqt.mdl.miner;

import be.intimals.freqt.core.Projected;
import be.intimals.freqt.mdl.tsg.TSGRule;

import java.util.Map;
import java.util.Vector;

public class CandidateRule {
    private Double length;
    private Map.Entry<String, Projected> patternProject;
    private TSGRule<String> rule;
    public Vector<String> pattern; // Temporary

    public CandidateRule() {
        this.length = Double.MAX_VALUE;
        this.patternProject = null;
        this.rule = null;
    }

    public CandidateRule(Double length, Map.Entry<String, Projected> patternProject, TSGRule<String> rule) {
        this.length = length;
        this.patternProject = patternProject;
        this.rule = rule;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Map.Entry<String, Projected> getPatternProject() {
        return patternProject;
    }

    public void setPatternProject(Map.Entry<String, Projected> patternProject) {
        this.patternProject = patternProject;
    }

    public TSGRule<String> getRule() {
        return rule;
    }

    public void setRule(TSGRule<String> rule) {
        this.rule = rule;
    }

    @Override
    public String toString() {
        return "{" + length + "; " + patternProject + "; " + rule + '}';
    }
}