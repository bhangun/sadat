package tech.kayys.wayang.mcp.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginExecutionContext {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, Object> configuration = new HashMap<>();
    private final long startTime = System.currentTimeMillis();
    private String executionId = java.util.UUID.randomUUID().toString();

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setConfiguration(Map<String, Object> config) {
        this.configuration.clear();
        this.configuration.putAll(config);
    }

    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    public String getExecutionId() {
        return executionId;
    }

    public long getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }

    public void log(String level, String message, Object... args) {
        System.out.printf("[%s] [%s] [%s] %s%n",
                level, executionId, Thread.currentThread().getName(),
                String.format(message, args));
    }
}
