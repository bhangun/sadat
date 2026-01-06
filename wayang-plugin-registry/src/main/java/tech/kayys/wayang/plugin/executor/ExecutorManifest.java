package tech.kayys.wayang.plugin.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.plugin.execution.ExecutionMode;

/**
 * Executor manifest
 */
public class ExecutorManifest {
    public String executorId;
    public String className;
    public List<String> nodeTypes = new ArrayList<>();
    public ExecutionMode mode;
    public List<String> protocols = new ArrayList<>();
    public boolean inProcess;
    public Map<String, Object> config = new HashMap<>();
}
