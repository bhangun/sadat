package tech.kayys.wayang.agent.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.dto.AuditLogEntry;

public interface AuditLogRepository {
    Uni<Void> save(AuditLogEntry entry);
}
