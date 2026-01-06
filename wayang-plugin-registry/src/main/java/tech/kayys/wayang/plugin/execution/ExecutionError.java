package tech.kayys.wayang.plugin.execution;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution Error - Structured error information
 */
public class ExecutionError {
    public String code;
    public String message;
    public String type;
    public Map<String, Object> details = new HashMap<>();
    public String stackTrace;
    public boolean retryable;
}