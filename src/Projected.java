import java.util.*;

public class Projected {
    private int depth = -1;
    private int support = -1;
    //private Vector<Location> locations = new Vector<Location>();
    //private Vector<Location> rootLocations = new Vector<Location>();
    private List<Location> locations = new  ArrayList<>();
    private List<Location> rootLocations = new ArrayList<>();

    private List<List<Integer>> lineNr = new ArrayList<>();

    //add 1 line number
    public void setProjectLineNr(int a){
        List<Integer> l = new ArrayList<>();
        l.add(a);
        this.lineNr.add(l);
    }
    //add set of line numbers
    public void addProjectLineNr (Integer a, List<Integer> lines){
        List<Integer> l = new ArrayList<>(lines);
        l.add(a);
        this.lineNr.add(l);
    }



    //get a list of line numbers
    public List<Integer> getProjectLineNr(int a){
        return this.lineNr.get(a);
    }


    public void Projected(){}


    public void setProjectedDepth(int d)
    {
        this.depth = d;
    }

    public void setProjectedSupport(int s) {
        this.support = s;
    }

    //add a location to locations
    public void setProjectLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }

    //add a location to locations: for right-most extension
    public void setProjectRootLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.rootLocations.add(l);
    }

    public int getProjectedDepth() {
        return this.depth;
    }

    public int getProjectedSupport(){
        return this.support;
    }

    public Location getProjectLocation(int i){
        return this.locations.get(i);
    }

    public Location getProjectRootLocation(int i){
        return this.rootLocations.get(i);
    }

    public int getProjectLocationSize(){
        return this.locations.size();
    }

    //keep all occurences for all position extension
    public void addProjectLocation(int i, int j, List<Integer> occurrences) {
        Location l = new Location(occurrences);
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }

    //keep all occurences for all position extension
    public void addProjectRootLocation(int i, int j, List<Integer> occurrences) {
        Location l = new Location(occurrences);
        l.setLocationId(i);
        l.addLocationPos(j);
        this.rootLocations.add(l);
    }


    public void removeProjectLocation(int i){
        this.locations.remove(i);
    }

    public void removeProjectLocation(Location location){
        this.locations.remove(location);
    }

    public void addProjectLocation(Location location){
        this.locations.add(location);
    }
    /*
    public int getProjectedSupport() {
        if(this.support == -1) computeSupport();
        return this.support;
    }

    private void computeSupport(){
        int old = 0xffffffff;
        int size = 0;
        for(int i=0; i<getProjectLocationSize(); ++i) {
            if (getProjectLocation(i).getLocationId() != old) ++size;
            old = getProjectLocation(i).getLocationId();
        }
        this.support = size;
    }
    */



}
