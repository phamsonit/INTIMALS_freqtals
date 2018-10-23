package be.intimals.freqt.util;

public class DoubleUtil {
    private static final double LN_2 = Math.log(2.0D);

    public static double PRECISION = 0.000001;
    public static final double ZERO_LOG2P = 7.8886090522101180541172856528279e-31; // log_2(x) = -100
    public static double log2(double x) {
        if (x == 0.0) return ZERO_LOG2P;
        return Math.log(x) / LN_2;
    }

    public static double exp2(double x) {
        return Math.pow(2.0, x);
    }
}
