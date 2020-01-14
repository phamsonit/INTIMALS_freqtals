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

    public List<Integer> getLocationList(){
        ArrayList<Integer> list = new ArrayList<Integer>(firstFree);
        if(memory != null) {
            for(int i=0; i<firstFree; i++)
                list.add((int)memory[i]);
        } else {
            for(int i=0; i<firstFree; i++)
                list.add(intMemory[i]);
        }
        return list;
    }

    public Location(){
    }

    public Location(Location other, int id, int pos){
        super(other);
        locationId = id;
        add(pos);
    }

    public int[] getLocationArr(){
        if(memory != null ) {
            int siz = size();
            int[] result = new int[siz];
            for (int i=0; i<siz; i++){
                result[i] = memory[i];
            }
            return result;
        }
        return Arrays.copyOf(intMemory,size());
    }
}
