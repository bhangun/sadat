package tech.kayys.wayang.plugin;

/**
 * Performance Profile
 */
public class PerformanceProfile {
    
    private double averageLatencyMs;
    private double throughputRps;
    private double p95LatencyMs;
    private double p99LatencyMs;
    
    private String cpuIntensity; // low, medium, high
    private String memoryIntensity; // low, medium, high
    private String ioIntensity; // low, medium, high


    public PerformanceProfile() {}

    public double getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(double averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public double getThroughputRps() {
        return throughputRps;
    }

    public void setThroughputRps(double throughputRps) {
        this.throughputRps = throughputRps;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public String getCpuIntensity() {
        return cpuIntensity;
    }

    public void setCpuIntensity(String cpuIntensity) {
        this.cpuIntensity = cpuIntensity;
    }

    public String getMemoryIntensity() {
        return memoryIntensity;
    }

    public void setMemoryIntensity(String memoryIntensity) {
        this.memoryIntensity = memoryIntensity;
    }

    public String getIoIntensity() {
        return ioIntensity;
    }

    public void setIoIntensity(String ioIntensity) {
        this.ioIntensity = ioIntensity;
    }

    private PerformanceProfile(Builder builder) {
        this.averageLatencyMs = builder.averageLatencyMs;
        this.throughputRps = builder.throughputRps;
        this.p95LatencyMs = builder.p95LatencyMs;
        this.p99LatencyMs = builder.p99LatencyMs;
        this.cpuIntensity = builder.cpuIntensity;
        this.memoryIntensity = builder.memoryIntensity;
        this.ioIntensity = builder.ioIntensity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double averageLatencyMs;
        private double throughputRps;
        private double p95LatencyMs;
        private double p99LatencyMs;
        private String cpuIntensity;
        private String memoryIntensity;
        private String ioIntensity;

        public Builder averageLatencyMs(double averageLatencyMs) {
            this.averageLatencyMs = averageLatencyMs;
            return this;
        }

        public Builder throughputRps(double throughputRps) {
            this.throughputRps = throughputRps;
            return this;
        }

        public Builder p95LatencyMs(double p95LatencyMs) {
            this.p95LatencyMs = p95LatencyMs;
            return this;
        }

        public Builder p99LatencyMs(double p99LatencyMs) {
            this.p99LatencyMs = p99LatencyMs;
            return this;
        }

        public Builder cpuIntensity(String cpuIntensity) {
            this.cpuIntensity = cpuIntensity;
            return this;
        }

        public Builder memoryIntensity(String memoryIntensity) {
            this.memoryIntensity = memoryIntensity;
            return this;
        }

        public Builder ioIntensity(String ioIntensity) {
            this.ioIntensity = ioIntensity;
            return this;
        }

        public PerformanceProfile build() {
            return new PerformanceProfile(this);
        }
    }
}
