package tech.kayys.wayang.plugin.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor Metadata - Additional executor information
 */
public class ExecutorMetadata {
    public String version;
    public String language; // "java", "python", "go"
    public Map<String, String> labels = new HashMap<>();
    public Map<String, Object> config = new HashMap<>();
    public int maxConcurrency;
    public long timeoutMs;
}
