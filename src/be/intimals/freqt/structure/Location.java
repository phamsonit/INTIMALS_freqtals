package be.intimals.freqt.structure;

import java.util.*;

public class Location {
    private int id;
    //private int pos; //right most position
//    private List<Integer> pos = new ArrayList<>();

    private int[] pos; //positions of all labels in pattern

    //private Set<Integer> rootPos = new LinkedHashSet<>();
    ////////////////////////////////////////////////////////////

    public Location(){
        pos = new int[0];
    }
    public Location(List<Integer> start) {
        pos = toPrimitiveIntArray(start);
//        pos = new ArrayList<>(start);
    }

    public void setLocationId(int a) {
        this.id = a;
    }
    public int getLocationId() {
        return this.id;
    }

    public void addLocationPos(int a) {
        pos = appendArray(pos, a);
//        this.pos.add(a);
    }

    public int getLocationPos() {
        return pos[pos.length-1];
//        return this.pos.get(this.pos.size()-1);
    }

    public List<Integer> getLocationList() {
        return toArrayList(pos);
    }

    // Convert an int[] to a List<Integer>
    private int[] toPrimitiveIntArray(List<Integer> al) {
        int[] converted = new int[al.size()];
        Iterator<Integer> iterator = al.iterator();
        for (int i = 0; i < converted.length; i++)
        {
            converted[i] = iterator.next().intValue();
        }
        return converted;
    }

    // Convert a List<Integer> to an int[]
    private List<Integer> toArrayList(int[] arr) {
        List<Integer> converted = new ArrayList<>();
        for(int i = 0; i < arr.length; i++) {
            converted.add(arr[i]);
        }
        return converted;
    }

    // Append an integer to an array
    private int[] appendArray(int[] array, int x){
        int[] result = new int[array.length + 1];
        for(int i = 0; i < array.length; i++)
            result[i] = array[i];

        result[result.length - 1] = x;
        return result;
    }
}
