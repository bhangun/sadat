


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for recording provenance and lineage information.
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class ProvenanceService {
    
    /**
     * Record provenance event
     */
    public String record(NodeContext context, String eventType, Object data) {
        String provenanceId = UUID.randomUUID().toString();
        
        ProvenanceRecord record = ProvenanceRecord.builder()
            .id(provenanceId)
            .runId(context.getRunId())
            .nodeId(context.getNodeId())
            .eventType(eventType)
            .timestamp(Instant.now())
            .tenantId(context.getTenantId())
            .metadata(Map.of(
                "workflowId", context.getWorkflowId(),
                "dataType", data != null ? data.getClass().getSimpleName() : "null"
            ))
            .build();
        
        persist(record);
        
        return provenanceId;
    }
    
    private void persist(ProvenanceRecord record) {
        // Log for now, implement actual persistence
        log.info("PROVENANCE: {} - {} - {}", record.getEventType(), record.getNodeId(), record.getId());
    }
}