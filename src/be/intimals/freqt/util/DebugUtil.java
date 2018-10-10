package be.intimals.freqt.util;

import be.intimals.freqt.core.NodeFreqT;
import be.intimals.freqt.core.Projected;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class DebugUtil {
    /**
     * Debug.
     * @param projected
     * @param pattern
     * @param transaction
     * @throws IOException
     */
    public static void writeHighWSupportPattern(Projected projected, Vector<String> pattern,
                                         Vector<Vector<NodeFreqT>> transaction) throws IOException {
        if (projected.getProjectLocationSize() > 10000) {
            int sup = projected.getProjectedSupport();
            int wsup = projected.getProjectLocationSize();
            int size = projected.getProjectLocation(0).getLocationList().size();

            //long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            //System.out.println(memUsed);
            FileWriter temp = new FileWriter("./out/" + pattern.hashCode() + ".txt");
            temp.write(sup + "\t" + wsup + "\t" + size + "\t\n");
            temp.write(pattern.toString() + "\n");
            for (int i = 0; i < projected.getProjectLocationSize(); i++) {
                temp.write(projected.getProjectLocation(i).getLocationId() + " [");

                for (Integer e : projected.getProjectLocation(i).getLocationList()) {
                    temp.write(transaction.get(projected.getProjectLocation(i).getLocationId())
                            .get(e).getNodeLabel() + "(" + e + "), ");
                }
                temp.write("]\n");
            }
            temp.flush();
            temp.close();
        }
    }
}
