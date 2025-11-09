package ru.rsatu.stats;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RequestStats {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public long increment(String key) {
        return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Long> snapshot() {
        Map<String, Long> snap = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> snap.put(k, v.get()));
        return snap;
    }

    public void resetAll() {
        counters.clear();
    }
}
