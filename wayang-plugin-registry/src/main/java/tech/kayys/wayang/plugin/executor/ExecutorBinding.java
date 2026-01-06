package tech.kayys.wayang.plugin.executor;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.execution.ExecutionMode;

/**
 * Executor Binding - Indirect reference to executor
 * This allows remote, multi-language, and hot-swappable executors
 */
public class ExecutorBinding {
    public String executorType; // "agent", "integration", "business", "custom"
    public String executorId; // "agent.default", "http.rest", "vector.pgvector"
    public ExecutionMode mode; // SYNC | ASYNC | STREAM
    public CommunicationProtocol protocol; // GRPC | KAFKA | REST | INPROC
    public Map<String, Object> config = new HashMap<>();

    public ExecutorBinding() {
    }

    public ExecutorBinding(String executorId, ExecutionMode mode, CommunicationProtocol protocol) {
        this.executorId = executorId;
        this.mode = mode;
        this.protocol = protocol;
    }
}
