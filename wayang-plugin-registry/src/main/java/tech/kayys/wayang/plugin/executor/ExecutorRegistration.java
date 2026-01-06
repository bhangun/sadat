package tech.kayys.wayang.plugin.executor;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import tech.kayys.wayang.plugin.CommunicationProtocol;

/**
 * Executor Registration - What Control Plane knows about an executor
 */
public class ExecutorRegistration {
    public String executorId;
    public String executorType; // "agent", "integration", "business"
    public URI endpoint;
    public CommunicationProtocol protocol;
    public Set<String> capabilities = new HashSet<>();
    public Set<String> supportedNodes = new HashSet<>();
    public ExecutorStatus status = ExecutorStatus.PENDING;
    public ExecutorMetadata metadata = new ExecutorMetadata();
    public Instant registeredAt;
    public Instant lastHeartbeat;

    // In-process plugin executor support
    public boolean inProcess = false;
    public Object executorInstance;
}