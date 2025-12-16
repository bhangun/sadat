
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.error.ErrorPayload;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for recording audit events.
 * Provides tamper-proof logging of all node executions.
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class AuditService {
    
    /**
     * Record node execution start
     */
    public void recordStart(NodeContext context) {
        AuditEntry entry = AuditEntry.builder()
            .id(UUID.randomUUID().toString())
            .event("NODE_START")
            .timestamp(Instant.now())
            .actor(Actor.system())
            .target(Target.node(context.getNodeId()))
            .metadata(Map.of(
                "runId", context.getRunId(),
                "workflowId", context.getWorkflowId(),
                "tenantId", context.getTenantId()
            ))
            .build();
        
        persist(entry);
    }
    
    /**
     * Record node execution completion
     */
    public void recordSuccess(NodeContext context, Object output) {
        AuditEntry entry = AuditEntry.builder()
            .id(UUID.randomUUID().toString())
            .event("NODE_SUCCESS")
            .timestamp(Instant.now())
            .actor(Actor.system())
            .target(Target.node(context.getNodeId()))
            .metadata(Map.of(
                "runId", context.getRunId(),
                "outputType", output != null ? output.getClass().getSimpleName() : "null"
            ))
            .build();
        
        persist(entry);
    }
    
    /**
     * Record error
     */
    public void recordError(NodeContext context, ErrorPayload error) {
        AuditEntry entry = AuditEntry.builder()
            .id(UUID.randomUUID().toString())
            .event("NODE_ERROR")
            .timestamp(Instant.now())
            .actor(Actor.system())
            .target(Target.node(context.getNodeId()))
            .metadata(Map.of(
                "runId", context.getRunId(),
                "errorType", error.getType(),
                "errorMessage", error.getMessage(),
                "retryable", error.isRetryable()
            ))
            .build();
        
        persist(entry);
    }
    
    /**
     * Persist audit entry
     * TODO: Implement actual persistence (database, audit log service, etc.)
     */
    private void persist(AuditEntry entry) {
        // For now, just log it
        log.info("AUDIT: {} - {} - {}", entry.getEvent(), entry.getTarget(), entry.getMetadata());
        
        // In production, persist to:
        // - PostgreSQL audit table
        // - Elasticsearch
        // - Kafka audit topic
        // - S3 append-only log
    }
}