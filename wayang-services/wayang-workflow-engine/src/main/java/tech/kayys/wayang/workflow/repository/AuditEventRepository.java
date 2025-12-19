package tech.kayys.wayang.workflow.repository;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.AuditEvent;
import tech.kayys.wayang.workflow.model.EventType;

/**
 * Audit Event Repository.
 */
@ApplicationScoped
public class AuditEventRepository implements PanacheRepositoryBase<AuditEvent, String> {

    public Uni<List<AuditEvent>> findByRunId(String runId) {
        return find("runId = ?1 order by timestamp", runId).list();
    }

    public Uni<List<AuditEvent>> findByRunIdAndNodeId(String runId, String nodeId) {
        return find("runId = ?1 and nodeId = ?2 order by timestamp", runId, nodeId).list();
    }

    public Uni<List<AuditEvent>> findByRunIdAndType(String runId, EventType type) {
        return find("runId = ?1 and eventType = ?2 order by timestamp", runId, type).list();
    }

    public Uni<Long> deleteOldEvents(Instant before) {
        return delete("timestamp < ?1", before);
    }
}
