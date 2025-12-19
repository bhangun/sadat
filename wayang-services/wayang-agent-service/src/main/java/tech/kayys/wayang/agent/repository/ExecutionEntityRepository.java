package tech.kayys.wayang.agent.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.ExecutionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExecutionEntityRepository implements PanacheRepository<ExecutionEntity> {

    public Uni<List<ExecutionEntity>> findByTenantId(String tenantId) {
        return find("tenantId", tenantId).list();
    }

    public Uni<List<ExecutionEntity>> findByAgentId(String agentId) {
        return find("agentId", agentId).list();
    }

    public Uni<List<ExecutionEntity>> findByWorkflowId(String workflowId) {
        return find("workflowId", workflowId).list();
    }

    public Uni<List<ExecutionEntity>> findByTenantIdAndStatus(String tenantId, String status) {
        return find("tenantId = ?1 AND status = ?2", tenantId, status).list();
    }

    public Uni<Optional<ExecutionEntity>> findById(UUID id) {
        return findByIdOptional(id);
    }
}