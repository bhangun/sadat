package tech.kayys.wayang.plugin.node;

import tech.kayys.execution.Event;
import tech.kayys.wayang.plugin.ExecutionResult;

import java.util.Map;

public interface ProvenanceService {
    
    /**
     * Create a provenance event for node execution.
     */
    Event createExecutionEvent(String nodeId, NodeContext context, ExecutionResult result);
    
    /**
     * Create a custom event.
     */
    Event createEvent(String type, Map<String, Object> data);
    
    // Singleton access (safe default)
    static ProvenanceService getInstance() {
        return ProvenanceServiceHolder.INSTANCE;
    }

    class ProvenanceServiceHolder {
        static ProvenanceService INSTANCE = new DefaultProvenanceService();
    }

    class DefaultProvenanceService implements ProvenanceService {
        @Override
        public Event createExecutionEvent(String nodeId, NodeContext context, ExecutionResult result) {
            return new Event("node.execution", Map.of(
                "nodeId", nodeId,
                "runId", context.getRunId(),
                "tenantId", context.getTenantId(),
                "status", result.getStatus().name(),
                "outputs", result.getOutputs(),
                "timestamp", System.currentTimeMillis()
            ));
        }

        @Override
        public Event createEvent(String type, Map<String, Object> data) {
            return new Event(type, data);
        }
    }
}