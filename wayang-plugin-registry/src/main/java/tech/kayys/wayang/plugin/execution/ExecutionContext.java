package tech.kayys.wayang.plugin.execution;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution Context - Variables, secrets, tracing
 */
public class ExecutionContext {
    public Map<String, Object> variables = new HashMap<>();
    public String secretsRef; // "vault://workflow/123"
    public Map<String, String> headers = new HashMap<>();
    public Map<String, Object> metadata = new HashMap<>();
}