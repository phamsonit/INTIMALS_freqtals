package be.intimals.freqt.util;

import java.util.*;

public class Util {
    /**
     * Union of two lists.
     *
     * @param list1
     * @param list2
     * @param <T>
     * @return
     */
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<>();
        set.addAll(list1);
        set.addAll(list2);
        return new ArrayList<>(set);
    }

}
