package tech.kayys.wayang.workflow.model;

import java.util.List;
import java.util.Map;

public class ExecutionResult {
    private final boolean success;
    private final Map<String, Object> output;
    private final List<ExecutionTrace> trace;
    private final String error;

    public ExecutionResult(boolean success, Map<String, Object> output,
            List<ExecutionTrace> trace, String error) {
        this.success = success;
        this.output = output;
        this.trace = trace;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public List<ExecutionTrace> getTrace() {
        return trace;
    }

    public String getError() {
        return error;
    }
}
