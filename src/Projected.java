import java.util.*;

public class Projected {
    private int depth = -1;
    private int support = -1;
    //private Vector<Location> locations = new Vector<Location>();
    //private Vector<Location> rootLocations = new Vector<Location>();
    private List<Location> locations = new  ArrayList<>();
    private List<Location> rootLocations = new ArrayList<>();
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

    //////////////locations////////////////////
    //keep right most position
    public void setProjectLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }

    public Location getProjectLocation(int i){
        return this.locations.get(i);
    }

    public int getProjectLocationSize(){
        return this.locations.size();
    }

    //keep positions of all occurrences
    public void addProjectLocation(int i, int j, List<Integer> occurrences) {
        Location l = new Location(occurrences);
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }

    public void removeProjectLocation(Location location){
        this.locations.remove(location);
    }

    /////////////root locations ///////////////
    //add a position to root locations
    public void setProjectRootLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.rootLocations.add(l);
    }

    public Location getProjectRootLocation(int i){
        return this.rootLocations.get(i);
    }

    public int getProjectRootLocationSize(){
        return this.rootLocations.size();
    }

    /////////////lineNr//////////////////////
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

}
