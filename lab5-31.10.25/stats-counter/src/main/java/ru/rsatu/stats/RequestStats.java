package ru.rsatu.stats;

import java.util.HashMap;
import java.util.Map;

public class RequestStats {
    private final Map<String, Integer> counters = new HashMap<>();

    public int increment(String key) {
        int value = counters.getOrDefault(key, 0) + 1;
        counters.put(key, value);
        return value;
    }

    public Map<String, Integer> snapshot() {
        return new HashMap<>(counters);
    }

    public void resetAll() {
        counters.clear();
    }
}
