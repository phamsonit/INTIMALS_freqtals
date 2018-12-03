package be.intimals.freqt.mdl.tsg;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapCounter<T> {
    private class Counter {
        private int count;

        public Counter(int val) {
            this.count = val;
        }

        public int getCount() {
            return count;
        }

        public Counter incCountBy(int count) {
            this.count += count;
            return this;
        }

        @Override
        public String toString() {
            return String.valueOf(count);
        }
    }

    private Map<T, Counter> counts = new HashMap<>();
    private Counter sum = new Counter(0);

    private MapCounter() {
    }

    public static <T> MapCounter<T> create() {
        return new MapCounter<>();
    }

    public void incCountBy(T key, Integer val) {
        if (!counts.containsKey(key) && val < 0) throw new IllegalArgumentException();
        counts.compute(key, (k, v) -> {
            Counter res = (v != null) ? v.incCountBy(val) : new Counter(val);
            return res.getCount() > 0 ? res : null;
        });
        sum.incCountBy(val);
        assert (checkConsistency());
    }

    public int getTotal() {
        return this.sum.getCount();
    }

    public Integer getCountFor(T key) {
        return (counts.get(key) != null) ? counts.get(key).getCount() : null;
    }

    public void remove(T key) {
        if (counts.containsKey(key)) {
            Counter counter = counts.get(key);
            sum.incCountBy(-counter.getCount());
            counts.remove(key);
        }
        assert (checkConsistency());
    }

    private boolean checkConsistency() {
        int sum = 0;
        for (Counter c : this.counts.values()) {
            assert (c.getCount() >= 0);
            sum += c.getCount();
        }
        assert (sum == this.sum.getCount());
        return true;
    }

    @Override
    public String toString() {
        String map = counts.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue().getCount()).collect(Collectors.joining(", "));
        return "T: " + sum + " | " + map;
    }
}
