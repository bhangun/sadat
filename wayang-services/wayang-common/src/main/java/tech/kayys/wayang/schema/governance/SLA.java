package tech.kayys.wayang.schema.governance;

public class SLA {
    private Integer maxLatencyMs;
    private Double availability;

    public Integer getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(Integer maxLatencyMs) {
        if (maxLatencyMs != null && maxLatencyMs < 0) {
            throw new IllegalArgumentException("Max latency cannot be negative");
        }
        this.maxLatencyMs = maxLatencyMs;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        if (availability != null && (availability < 0 || availability > 1)) {
            throw new IllegalArgumentException("Availability must be between 0 and 1");
        }
        this.availability = availability;
    }
}
