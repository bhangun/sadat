package tech.kayys.wayang.workflow.kernel;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Marker for performance measurement points
 */
public class PerformanceMarker {

    private final String name;
    private final MarkerType type;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private final String parentMarkerId;

    public PerformanceMarker(String name, MarkerType type, Instant timestamp,
            Map<String, Object> metadata, String parentMarkerId) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.metadata = Map.copyOf(metadata);
        this.parentMarkerId = parentMarkerId;
    }

    // Simplified constructor
    public PerformanceMarker(String name, long timestampNanos) {
        this(name, MarkerType.INFO,
                Instant.ofEpochSecond(0, timestampNanos),
                Map.of(), null);
    }

    // Getters...
    public String getName() {
        return name;
    }

    public MarkerType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getParentMarkerId() {
        return parentMarkerId;
    }

    public enum MarkerType {
        START, END, INFO, WARNING, ERROR, CHECKPOINT
    }

    public static class Builder {
        private String name;
        private MarkerType type = MarkerType.INFO;
        private Instant timestamp = Instant.now();
        private Map<String, Object> metadata = Map.of();
        private String parentMarkerId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(MarkerType type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder parentMarkerId(String parentMarkerId) {
            this.parentMarkerId = parentMarkerId;
            return this;
        }

        public PerformanceMarker build() {
            return new PerformanceMarker(name, type, timestamp, metadata, parentMarkerId);
        }
    }
}