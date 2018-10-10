package be.intimals.freqt.util;

public class SearchStatistics {
    private long nbClosed = 0; // Nb of closed patterns found
    private long nbNotClosed = 0; // Nb of times not closed pattern was encountered
    private long nbPruned = 0; // Nb of times a not closed pattern was pruned
    private long nbProject = 0; // Nb of times project was called
    private long nbConstraint = 0; // Nb of patterns rejected by constraints

    public long getClosed() {
        return nbClosed;
    }

    public void incClosed() {
        ++this.nbClosed;
    }

    public long getNotClosed() {
        return nbNotClosed;
    }

    public void incNotClosed() {
        ++this.nbNotClosed;
    }

    public long getPruned() {
        return nbPruned;
    }

    public void incPruned() {
        ++this.nbPruned;
    }

    public long getProject() {
        return nbProject;
    }

    public void incProject() {
        ++this.nbProject;
    }

    public void incConstraint() {
        ++this.nbConstraint;
    }

    @Override
    public String toString() {
        return "SearchStatistics{"
                + "projected: " + nbProject
                + ", closed: " + nbClosed
                + ", not closed: " + nbNotClosed
                + ", pruned:" + nbPruned
                + ", constrained:" + nbConstraint
                + '}';
    }
}
