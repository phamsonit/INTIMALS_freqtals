package be.intimals.freqt.output;

import be.intimals.freqt.Config;
import be.intimals.freqt.core.Location;
import be.intimals.freqt.core.Pattern;
import be.intimals.freqt.core.Projected;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class LineOutput extends AOutputFormatter {

    public LineOutput(Config newConfig, Map<String, Vector<String>> newGrammar) throws IOException {
        super(newConfig, newGrammar);
    }

    /**
     * Output subtrees as string format.
     *
     * @param pat
     * @param projected
     */
    @Override
    public void report(Vector<String> pat, Projected projected) {
        try {
            final int size = Pattern.getPatternSize(pat);
            final int sup = projected.getProjectedSupport();
            final int wsup = projected.getProjectLocationSize();

            assert (size == projected.getProjectLocation(0).getLocationList().size());

            List<Integer> allOccurrences = projected.getUnionAllOccurences();

            assert (Pattern.checkConstraints(config, pat));
            assert (size == projected.getProjectLocation(0).getLocationList().size());

            out.write("occurrences:" + allOccurrences.size()
                    + " supp:" + sup + " wsupp:" + wsup + " size:" + size + "\t");

            int n = 0;
            for (int i = 0; i < pat.size(); ++i) {
                if (pat.elementAt(i).equals(")")) {
                    out.write(pat.elementAt(i));
                    --n;
                } else {
                    ++n;
                    out.write("(" + pat.elementAt(i));
                }
            }
            for (int i = 0; i < n; ++i) {
                out.write(")");
            }
            out.write("\n");

            //output locations
            if (config.addLocations()) {
                out.write("Locations : ");
                out.write(projected.getProjectLocation(0).getLocationId() + " ");
                for (int i = 0; i < projected.getProjectLocationSize() - 1; ++i) {
                    Location tmp1 = projected.getProjectLocation(i);
                    Location tmp2 = projected.getProjectLocation(i + 1);
                    if (tmp1.getLocationId() != tmp2.getLocationId()) {
                        out.write(tmp2.getLocationId() + " ");
                    }

                }
                out.write("\n");
            }

        } catch (IOException e) {
            System.out.println("report error");
        }
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }
}
