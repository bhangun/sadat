package tech.kayys.wayang.schema.governance;

import java.util.List;

public class TelemetryConfig {
    private Boolean enabled = true;
    private List<String> events;
    private Double sampleRate = 0.05;
    private String traceContextHeader = "x-trace-id";
    private String metricsPrefix;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public Double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Double sampleRate) {
        if (sampleRate != null && (sampleRate < 0 || sampleRate > 1)) {
            throw new IllegalArgumentException("Sample rate must be between 0 and 1");
        }
        this.sampleRate = sampleRate;
    }

    public String getTraceContextHeader() {
        return traceContextHeader;
    }

    public void setTraceContextHeader(String traceContextHeader) {
        this.traceContextHeader = traceContextHeader;
    }

    public String getMetricsPrefix() {
        return metricsPrefix;
    }

    public void setMetricsPrefix(String metricsPrefix) {
        this.metricsPrefix = metricsPrefix;
    }
}
