package tech.kayys.wayang.schema.governance;

public class RateLimit {
    private Double requestsPerSecond;
    private Integer burst;

    public Double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Double requestsPerSecond) {
        if (requestsPerSecond != null && requestsPerSecond < 0) {
            throw new IllegalArgumentException("Requests per second cannot be negative");
        }
        this.requestsPerSecond = requestsPerSecond;
    }

    public Integer getBurst() {
        return burst;
    }

    public void setBurst(Integer burst) {
        if (burst != null && burst < 0) {
            throw new IllegalArgumentException("Burst cannot be negative");
        }
        this.burst = burst;
    }
}
