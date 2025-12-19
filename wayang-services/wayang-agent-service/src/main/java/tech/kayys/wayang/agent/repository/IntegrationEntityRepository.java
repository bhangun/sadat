package tech.kayys.wayang.agent.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.IntegrationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IntegrationEntityRepository implements PanacheRepository<IntegrationEntity> {

    public Uni<List<IntegrationEntity>> findByTenantId(String tenantId) {
        return find("tenantId", tenantId).list();
    }

    public Uni<List<IntegrationEntity>> findByProvider(String provider) {
        return find("provider", provider).list();
    }

    public Uni<List<IntegrationEntity>> findByTenantIdAndIsActive(String tenantId, Boolean isActive) {
        return find("tenantId = ?1 AND isActive = ?2", tenantId, isActive).list();
    }

    public Uni<Optional<IntegrationEntity>> findById(UUID id) {
        return findByIdOptional(id);
    }
}