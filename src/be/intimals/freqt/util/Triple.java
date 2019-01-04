package be.intimals.freqt.util;

public class Triple<F, S, T> {
    private F first;
    private S second;
    private T third;

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public T getThird() {
        return third;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public void setThird(T third) {
        this.third = third;
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode())
                ^ (third == null ? 0 : third.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triple)) {
            return false;
        }

        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
        return objectsEqual(other.first, first) && objectsEqual(other.second, second)
                && objectsEqual(other.third, third);
    }

    private static boolean objectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        return "Triple{" + String.valueOf(first) + " " + String.valueOf(second) + " " + String.valueOf(third) + "}";
    }
}