package be.intimals.freqt.structure;

import java.util.*;

public class Projected {
    private int depth = -1;
    private int support = -1;
    private int rootSupport = -1;
    private List<int[]> locations = new  ArrayList<>();
    //private List<int[]> rootLocations = new ArrayList<>();
    private List<List<Integer>> lineNr = new ArrayList<>();

    //////////////////////////////////////////////////////////
    public void Projected(){}

    public void setProjectedDepth(int d)
    {
        this.depth = d;
    }
    public int getProjectedDepth() {
        return this.depth;
    }

    public void setProjectedSupport(int s) {
        this.support = s;
    }
    public int getProjectedSupport(){
        return this.support;
    }

    public void setProjectedRootSupport(int s) {
        this.rootSupport = s;
    }
    public int getProjectedRootSupport(){
        return this.rootSupport;
    }

    //add right most position
    public void setProjectLocation(int i, int j) {
        int[] l = Location.init();
        Location.setLocationId(l, i);
        l = Location.addLocationPos(l, j);
        boolean dup = false;
        for(int k=0; k<locations.size(); ++k)
            if(Location.getLocationId(locations.get(k)) == i &&
                    Location.getLocationPos(locations.get(k)) == j)
                dup = true;

        if(!dup) this.locations.add(l);
    }

    public int[] getProjectLocation(int i){
        return this.locations.get(i);
    }

    public void deleteProjectLocation(int i){
        this.locations.remove(i);
    }

    public int getProjectLocationSize(){
        return this.locations.size();
    }

    //add positions of all occurrences
    public void addProjectLocation(int i, int j, int[] occurrences) {
        int[] l = Location.init(occurrences);
        Location.setLocationId(l, i);
        l = Location.addLocationPos(l, j);
        this.locations.add(l);
    }

    public void removeProjectLocation(int[] location){
        this.locations.remove(location);
    }

    /*
    //add root locations
    public void setProjectRootLocation(int i, int j) {
        int[] l = Location.init();
        Location.setLocationId(l, i);
        l = Location.addLocationPos(l, j);
        //check if l exists in rootLocations ????
        boolean dup=false;
        for(int k=0; k<rootLocations.size(); ++k)
            if(Location.getLocationId(rootLocations.get(k)) == i &&
                    Location.getLocationPos(rootLocations.get(k)) == j)
                dup=true;

        if(!dup) this.rootLocations.add(l);
    }

    public int[] getProjectRootLocation(int i){
        return this.rootLocations.get(i);
    }

    public int getProjectRootLocationSize(){
        return this.rootLocations.size();
    }
    */

}
