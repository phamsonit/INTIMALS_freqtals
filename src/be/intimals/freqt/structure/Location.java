package be.intimals.freqt.structure;


import be.intimals.freqt.FTArray;

import java.util.*;

/**
 * A location is an FTArray plus an identifier of this location
 */
public class Location extends FTArray {
    int locationId = 0;
    public int getLocationId() { return locationId; }
    public void setLocationId(int a) { locationId = a; }

    public void addLocationPos(int x) { this.add(x); }
    public int getLocationPos() { return this.getLast(); }
    public int getRoot() { return this.get(0); }

    public String getIdPos(){
        return locationId + "-" + this.getLast() +";";
    }

    public Location(){
    }

    public Location(Location other, int id, int pos){
        super(other);
        locationId = id;
        add(pos);
    }
}
