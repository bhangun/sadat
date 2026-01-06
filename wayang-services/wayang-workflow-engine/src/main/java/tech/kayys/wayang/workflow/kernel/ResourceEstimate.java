package tech.kayys.wayang.workflow.kernel;

import java.util.Map;
import java.util.Objects;

/**
 * Estimated resource requirements for node execution
 */
public class ResourceEstimate {

    private final CpuRequirement cpu;
    private final MemoryRequirement memory;
    private final StorageRequirement storage;
    private final NetworkRequirement network;
    private final Map<String, Object> customRequirements;
    private final ConfidenceLevel confidence;

    public ResourceEstimate(CpuRequirement cpu, MemoryRequirement memory,
            StorageRequirement storage, NetworkRequirement network,
            Map<String, Object> customRequirements, ConfidenceLevel confidence) {
        this.cpu = Objects.requireNonNull(cpu, "cpu cannot be null");
        this.memory = Objects.requireNonNull(memory, "memory cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.network = Objects.requireNonNull(network, "network cannot be null");
        this.customRequirements = Map.copyOf(customRequirements);
        this.confidence = Objects.requireNonNull(confidence, "confidence cannot be null");
    }

    // Getters...
    public CpuRequirement getCpu() {
        return cpu;
    }

    public MemoryRequirement getMemory() {
        return memory;
    }

    public StorageRequirement getStorage() {
        return storage;
    }

    public NetworkRequirement getNetwork() {
        return network;
    }

    public Map<String, Object> getCustomRequirements() {
        return customRequirements;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public boolean canFitIn(ResourceEstimate available) {
        return this.cpu.compareTo(available.cpu) <= 0 &&
                this.memory.compareTo(available.memory) <= 0 &&
                this.storage.compareTo(available.storage) <= 0 &&
                this.network.compareTo(available.network) <= 0;
    }

    public ResourceEstimate add(ResourceEstimate other) {
        return new ResourceEstimate(
                this.cpu.add(other.cpu),
                this.memory.add(other.memory),
                this.storage.add(other.storage),
                this.network.add(other.network),
                mergeCustomRequirements(this.customRequirements, other.customRequirements),
                ConfidenceLevel.min(this.confidence, other.confidence));
    }

    public ResourceEstimate multiply(double factor) {
        return new ResourceEstimate(
                this.cpu.multiply(factor),
                this.memory.multiply(factor),
                this.storage.multiply(factor),
                this.network.multiply(factor),
                this.customRequirements,
                this.confidence);
    }

    private Map<String, Object> mergeCustomRequirements(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> merged = new java.util.HashMap<>(a);
        b.forEach((key, value) -> {
            if (merged.containsKey(key)) {
                // Simple merge strategy - for complex cases, domain-specific logic needed
                if (value instanceof Number && merged.get(key) instanceof Number) {
                    Number n1 = (Number) merged.get(key);
                    Number n2 = (Number) value;
                    merged.put(key, n1.doubleValue() + n2.doubleValue());
                } else {
                    merged.put(key, value); // Overwrite
                }
            } else {
                merged.put(key, value);
            }
        });
        return Map.copyOf(merged);
    }

    public enum ConfidenceLevel {
        LOW(0.3), MEDIUM(0.7), HIGH(0.9), EXACT(1.0);

        private final double value;

        ConfidenceLevel(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public static ConfidenceLevel min(ConfidenceLevel a, ConfidenceLevel b) {
            return a.value <= b.value ? a : b;
        }
    }

    public static class CpuRequirement implements Comparable<CpuRequirement> {
        private final double cores;
        private final Unit unit;

        public CpuRequirement(double cores, Unit unit) {
            this.cores = cores;
            this.unit = unit;
        }

        public double getCores() {
            return cores;
        }

        public Unit getUnit() {
            return unit;
        }

        public CpuRequirement add(CpuRequirement other) {
            return new CpuRequirement(this.cores + other.cores, this.unit);
        }

        public CpuRequirement multiply(double factor) {
            return new CpuRequirement(this.cores * factor, this.unit);
        }

        @Override
        public int compareTo(CpuRequirement other) {
            return Double.compare(this.cores, other.cores);
        }
    }

    public static class MemoryRequirement implements Comparable<MemoryRequirement> {
        private final long bytes;

        public MemoryRequirement(long bytes) {
            this.bytes = bytes;
        }

        public long getBytes() {
            return bytes;
        }

        public MemoryRequirement add(MemoryRequirement other) {
            return new MemoryRequirement(this.bytes + other.bytes);
        }

        public MemoryRequirement multiply(double factor) {
            return new MemoryRequirement((long) (this.bytes * factor));
        }

        @Override
        public int compareTo(MemoryRequirement other) {
            return Long.compare(this.bytes, other.bytes);
        }
    }

    public static class StorageRequirement implements Comparable<StorageRequirement> {
        private final long bytes;
        private final StorageType type;

        public StorageRequirement(long bytes, StorageType type) {
            this.bytes = bytes;
            this.type = type;
        }

        public long getBytes() {
            return bytes;
        }

        public StorageType getType() {
            return type;
        }

        public StorageRequirement add(StorageRequirement other) {
            if (this.type != other.type) {
                throw new IllegalArgumentException("Cannot add storage requirements of different types");
            }
            return new StorageRequirement(this.bytes + other.bytes, this.type);
        }

        public StorageRequirement multiply(double factor) {
            return new StorageRequirement((long) (this.bytes * factor), this.type);
        }

        @Override
        public int compareTo(StorageRequirement other) {
            return Long.compare(this.bytes, other.bytes);
        }
    }

    public static class NetworkRequirement implements Comparable<NetworkRequirement> {
        private final long bandwidthBps; // bits per second
        private final LatencyRequirement latency;

        public NetworkRequirement(long bandwidthBps, LatencyRequirement latency) {
            this.bandwidthBps = bandwidthBps;
            this.latency = latency;
        }

        public long getBandwidthBps() {
            return bandwidthBps;
        }

        public LatencyRequirement getLatency() {
            return latency;
        }

        public NetworkRequirement add(NetworkRequirement other) {
            return new NetworkRequirement(
                    Math.max(this.bandwidthBps, other.bandwidthBps),
                    this.latency.max(other.latency));
        }

        public NetworkRequirement multiply(double factor) {
            return new NetworkRequirement((long) (this.bandwidthBps * factor), this.latency);
        }

        @Override
        public int compareTo(NetworkRequirement other) {
            int bandwidthCompare = Long.compare(this.bandwidthBps, other.bandwidthBps);
            if (bandwidthCompare != 0)
                return bandwidthCompare;
            return this.latency.compareTo(other.latency);
        }
    }

    public enum Unit {
        MILLICORES, CORES
    }

    public enum StorageType {
        SSD, HDD, NVME, NETWORK
    }

    public static class LatencyRequirement implements Comparable<LatencyRequirement> {
        private final int maxLatencyMs;
        private final int minLatencyMs;

        public LatencyRequirement(int maxLatencyMs, int minLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
            this.minLatencyMs = minLatencyMs;
        }

        public int getMaxLatencyMs() {
            return maxLatencyMs;
        }

        public int getMinLatencyMs() {
            return minLatencyMs;
        }

        public LatencyRequirement max(LatencyRequirement other) {
            return new LatencyRequirement(
                    Math.max(this.maxLatencyMs, other.maxLatencyMs),
                    Math.max(this.minLatencyMs, other.minLatencyMs));
        }

        @Override
        public int compareTo(LatencyRequirement other) {
            return Integer.compare(this.maxLatencyMs, other.maxLatencyMs);
        }
    }
}