package be.intimals.freqt.structure;


/**
 * A location is an FTArray plus an identifier of this location
 * first element of FTArray is root location
 */
public class Location extends FTArray {
    //new variable for 2-class data
    int classID = -1;
    int locationId = 0;

    public int getLocationId() { return locationId; }

    public void setLocationId(int a) { locationId = a; }

    public void addLocationPos(int x) { this.add(x); }

    public int getLocationPos() { return this.getLast(); }

    public int getRoot() { return this.get(0); }

    public String getIdPos(){
        return locationId + "-" + this.getLast() +";";
    }

    public Location(){ }

    public Location(Location other, int id, int pos){
        super(other);
        locationId = id;
        add(pos);
    }

    //new procedure for 2-class data
    public Location(Location other, int classId, int id, int pos){
        super(other);
        classID = classId;
        locationId = id;
        add(pos);
    }

    public int getClassID() {return  classID; }
    public void setClassID(int a) {classID = a;}
}
