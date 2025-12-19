package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.model.ExecutionContext;

@ApplicationScoped
public class WorkflowRuntimeEngine {

    public Uni<ExecutionResult> executeWorkflow(WorkflowDefinition workflow, Map<String, Object> input,
            ExecutionContext context) {
        return Uni.createFrom().item(new ExecutionResult());
    }

    public static class ExecutionResult {
        public boolean isSuccess() {
            return true;
        }

        public Map<String, Object> getOutput() {
            return Map.of();
        }

        public List<ExecutionTrace> getTrace() {
            return List.of();
        }
    }

    public static class ExecutionTrace {
        // Add fields if needed
    }
}
