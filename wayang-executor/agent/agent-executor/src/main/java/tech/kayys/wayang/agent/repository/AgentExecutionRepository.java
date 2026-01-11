package tech.kayys.wayang.agent.repository;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.domain.AgentExecutionEntity;

/**
 * Repository for Agent Executions
 */
@ApplicationScoped
public class AgentExecutionRepository
        implements PanacheRepositoryBase<AgentExecutionEntity, String> {

    public Uni<List<AgentExecutionEntity>> findByRun(String runId) {
        return list("runId", runId);
    }

    public Uni<List<AgentExecutionEntity>> findByTenant(
            String tenantId,
            int page,
            int size) {
        return find("tenantId = ?1 order by startedAt desc", tenantId)
                .page(page, size)
                .list();
    }

    public Uni<Long> countByStatus(String tenantId, String status) {
        return count("tenantId = ?1 and status = ?2", tenantId, status);
    }
}