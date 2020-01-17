package be.intimals.freqt.structure;

import java.util.*;

public class Location {
    /**
     * Set of static functions to represent and modify a location
     * A location is an int[], where the first element is an identifier of this location, followed by a list of positions.
     *
     * !! Note: this definitely is very un-OOP-like code, but this is done to significantly reduce memory consumption.
     * We are creating **many** of these locations, so it takes less space to directly work with an int[]
     * instead of wrapping it in a class instance.
     *
     * @return
     */

    public static int[] init() {
        return new int[1]; // Reserve the first element to store location id
    }

    public static int[] init(int[] start) {
        int[] result = new int[start.length+1];
        result[0] = 0;
        System.arraycopy(start, 0, result, 1, start.length);
        return result;
    }

    public static void setLocationId(int[]location , int a) {
        location[0] = a;
    }

    public static int getLocationId(int[] location) {
        return location[0];
    }

    public static int getRoot(int[] location){
        return location[1];
    }

    public static int[] addLocationPos(int[] location, int a) {
        //should use Arrays.copy
        return appendArray(location, a);
    }

    public static int getLocationPos(int[] location) {
        return location[location.length-1];
    }

    public static List<Integer> getLocationList(int[] location) {
        List<Integer> list = toArrayList(location);
        list.remove(0); // Remove the id
        return list;
    }

    public static int[] getLocationArr(int[] location){
        return Arrays.copyOfRange(location,1,location.length);
    }



    // Convert a List<Integer> to an int[]
    private static List<Integer> toArrayList(int[] arr) {
        List<Integer> converted = new ArrayList<>();
        for(int i = 0; i < arr.length; i++) {
            converted.add(arr[i]);
        }
        return converted;
    }

    // Append an integer to an array
    private static int[] appendArray(int[] array, int x){
        int[] result = new int[array.length + 1];
        for(int i = 0; i < array.length; i++)
            result[i] = array[i];

        result[result.length - 1] = x;
        return result;
    }
}
