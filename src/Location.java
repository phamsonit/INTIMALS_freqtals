import java.util.*;

public class Location {
    private int id;
    //private int pos; //right most position
    private List<Integer> pos = new ArrayList<>(); //positions of all labels in pattern


    //private Set<Integer> rootPos = new LinkedHashSet<>();
    ////////////////////////////////////////////////////////////

    public Location(){}
    public Location(List<Integer> start) {
        pos = new ArrayList<>(start);
    }

    public void setLocationId(int a) {
        this.id = a;
    }
    public int getLocationId() {
        return this.id;
    }

    public void addLocationPos(int a){
        this.pos.add(a);
    }
    public int getLocationPos() {
        return this.pos.get(this.pos.size()-1);
    }
    public List<Integer> getLocationList() {
        return this.pos;
    }


}
