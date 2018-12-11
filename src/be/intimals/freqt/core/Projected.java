package be.intimals.freqt.core;

import be.intimals.freqt.util.Util;
import java.util.*;

public class Projected {
    private int depth = -1;
    private int support = -1;
    private List<Location> locations = new ArrayList<>();
    private List<Location> rootLocations = new ArrayList<>();

    public Projected() {
    }

    public void setProjectedDepth(int d) {
        this.depth = d;
    }

    /**
     * Add 1 location into locations.
     *
     * @param i
     * @param j
     */
    public void setProjectLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
        this.support = -1;
    }

    /**
     * Add location as root location TODO.
     *
     * @param i
     * @param j
     */
    public void setProjectRootLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.rootLocations.add(l);
    }

    public int getProjectedDepth() {
        return this.depth;
    }

    public int getProjectedSupport() {
        if (this.support == -1) computeSupport();
        return this.support;
    }

    public Location getProjectLocation(int i) {
        return this.locations.get(i);
    }

    // TODO not used?
    public Location getProjectRootLocation(int i) {
        return this.rootLocations.get(i);
    }

    public int getProjectLocationSize() {
        return this.locations.size();
    }

    /**
     * Copy the given occurrence and add a new location.
     * @param i
     * @param j
     * @param occurrences
     */
    public void addProjectLocation(int i, int j, List<Integer> occurrences) {
        Location l = new Location(occurrences);
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
        this.support = -1;
    }

    /**
     * Calculate the support of this subtree by identifying unique tids in the projected location list.
     */
    private void computeSupport() {
        int old = 0xffffffff;
        int size = 0;
        for (int i = 0; i < getProjectLocationSize(); ++i) {
            if (getProjectLocation(i).getLocationId() != old) ++size;
            old = getProjectLocation(i).getLocationId();
        }
        this.support = size;
    }

    /**
     * Return the union of all occurrences.
     * @return
     */
    public List<Integer> getUnionAllOccurences() {
        List<Integer> tmp = new ArrayList<>(getProjectLocation(0).getLocationList());
        for (int i = 1; i < getProjectLocationSize(); ++i) {
            tmp = Util.union(tmp, getProjectLocation(i).getLocationList());
        }
        //Collections.sort(tmp);
        return tmp;

    }

    public void removeLocation(int i) {
        this.locations.remove(i);
        this.support = -1;
    }
}
