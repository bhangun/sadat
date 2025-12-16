package tech.kayys.execution;


import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class Event {
    private final String type;
    private final Instant timestamp;
    private final Map<String, Object> data;

    public Event(String type, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type);
        this.timestamp = Instant.now();
        this.data = Map.copyOf(data);
    }

    // Getters
    public String getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}