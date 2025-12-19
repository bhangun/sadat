package tech.kayys.wayang.agent.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.TemplateEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TemplateEntityRepository implements PanacheRepository<TemplateEntity> {

    public Uni<List<TemplateEntity>> findAllActiveTemplates() {
        return find("isActive", true).list();
    }

    public Uni<List<TemplateEntity>> findByTemplateType(String templateType) {
        return find("templateType", templateType).list();
    }

    public Uni<List<TemplateEntity>> findByTenantId(String tenantId) {
        // For templates that might be tenant-specific
        return find("1=1").list(); // Global templates by default
    }

    public Uni<Optional<TemplateEntity>> findById(UUID id) {
        return findByIdOptional(id);
    }

    public Uni<List<TemplateEntity>> findByIsActive(Boolean isActive) {
        return find("isActive", isActive).list();
    }
}