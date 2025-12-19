package tech.kayys.wayang.workflow.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

@RegisterForReflection
public class ExecutionContext {
    private String executionId;

    public String getExecutionId() {
        return executionId;
    }

    public List<WorkflowRuntimeEngine.ExecutionTrace> getExecutionTrace() {
        return List.of();
    }

    public long getExecutionDuration() {
        return 0;
    }
}
