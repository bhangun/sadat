package tech.kayys.wayang.schema.execution;

import java.util.Arrays;
import java.util.List;

public class ExecutionConfig {
    private String mode = "sync";
    private ErrorHandlingConfig retryPolicy;
    private Integer timeoutMs;
    private List<String> emitEvents;
    private List<String> sideEffects;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        List<String> validModes = Arrays.asList("sync", "async", "stream");
        if (!validModes.contains(mode)) {
            throw new IllegalArgumentException("Invalid execution mode: " + mode);
        }
        this.mode = mode;
    }

    public ErrorHandlingConfig getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(ErrorHandlingConfig retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        this.timeoutMs = timeoutMs;
    }

    public List<String> getEmitEvents() {
        return emitEvents;
    }

    public void setEmitEvents(List<String> emitEvents) {
        this.emitEvents = emitEvents;
    }

    public List<String> getSideEffects() {
        return sideEffects;
    }

    public void setSideEffects(List<String> sideEffects) {
        this.sideEffects = sideEffects;
    }
}
