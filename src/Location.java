import java.util.*;

public class Location {
    private int id;
    private List<Integer> pos = new ArrayList<>();

    public Location(){}

    public int getLocationId()
    {
        return this.id;
    }

    public int getLocationPos() {
        return this.pos.get(this.pos.size()-1);
    }

    public void setLocationId(int a) {
        this.id = a;
    }

    public Location(List<Integer> start) {
        pos = new ArrayList<>(start);
    }

    public void addLocationPos(int a){
        this.pos.add(a);
    }

    public List<Integer> getLocationList() {
        return this.pos;
    }

}
