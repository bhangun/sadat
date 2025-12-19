package tech.kayys.wayang.schema.execution;

import java.util.Arrays;
import java.util.List;

public class WaitFor {
    private String type;
    private Task task;
    private String correlationKey;
    private Integer timeoutMs;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("event", "callback", "human", "signal");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid wait for type: " + type);
        }
        this.type = type;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
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
}
