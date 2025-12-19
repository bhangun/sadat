package tech.kayys.wayang.workflow.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.workflow.domain.SagaDefinitionEntity;
import tech.kayys.wayang.workflow.model.saga.CompensationStrategy;
import tech.kayys.wayang.workflow.model.saga.SagaDefinition;

/**
 * SagaDefinitionRegistry - Repository with enhanced functionality for saga
 * definitions
 */
@ApplicationScoped
@Transactional
@Slf4j
public class SagaDefinitionRegistry {

    /**
     * Get saga definition by workflow ID (without tenant isolation - for backward
     * compatibility)
     */
    public Uni<SagaDefinition> getSagaDefinition(String workflowId) {
        return SagaDefinitionEntity.<SagaDefinitionEntity>find("workflowId = ?1 and active = true", workflowId)
                .firstResult()
                .map(this::toDomain)
                .onFailure()
                .invoke(throwable -> log.error("Failed to fetch saga definition for workflow: {}", workflowId,
                        throwable));
    }

    /**
     * Get saga definition by workflow ID with tenant isolation
     */
    public Uni<SagaDefinition> getSagaDefinition(String workflowId, String tenantId) {
        return SagaDefinitionEntity
                .<SagaDefinitionEntity>find("workflowId = ?1 and tenantId = ?2 and active = true", workflowId, tenantId)
                .firstResult()
                .map(this::toDomain)
                .onFailure()
                .invoke(throwable -> log.error("Failed to fetch saga definition for workflow: {}, tenant: {}",
                        workflowId, tenantId, throwable));
    }

    private static final int BATCH_SIZE = 100;

    /**
     * Get saga definition by ID with tenant isolation
     */
    public Uni<SagaDefinition> getByIdAndTenant(String id, String tenantId) {
        return SagaDefinitionEntity
                .<SagaDefinitionEntity>find("id = ?1 and tenantId = ?2 and active = true", id, tenantId)
                .firstResult()
                .map(this::toDomain);
    }

    /**
     * Get all active saga definitions for a tenant with pagination
     */
    public Uni<List<SagaDefinition>> findAllActive(String tenantId, Page page) {
        return SagaDefinitionEntity.<SagaDefinitionEntity>find("tenantId = ?1 and active = true", tenantId)
                .page(page)
                .list()
                .map(entities -> entities.stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()));
    }

    /**
     * Search saga definitions by criteria
     */
    public Uni<List<SagaDefinition>> search(String tenantId, String nameFilter, Boolean active) {
        StringBuilder queryBuilder = new StringBuilder("tenantId = ?1");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        int paramIndex = 2;

        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            queryBuilder.append(" and lower(name) like lower(?").append(paramIndex++).append(")");
            params.add("%" + nameFilter + "%");
        }

        if (active != null) {
            queryBuilder.append(" and active = ?").append(paramIndex++);
            params.add(active);
        }

        return SagaDefinitionEntity.<SagaDefinitionEntity>find(queryBuilder.toString(), params.toArray())
                .list()
                .map(entities -> entities.stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()));
    }

    /**
     * Create or update saga definition with validation and optimistic locking
     */
    public Uni<SagaDefinition> saveSagaDefinition(SagaDefinition sagaDefinition) {
        return Uni.createFrom().item(() -> {
            // Validate domain object
            validateSagaDefinition(sagaDefinition);
            return sagaDefinition;
        })
                .chain(validatedDefinition -> {
                    // Check for existing workflow ID conflict (excluding current entity)
                    return SagaDefinitionEntity
                            .count("workflowId = ?1 and tenantId = ?2 and id != ?3 and active = true",
                                    validatedDefinition.getWorkflowId(),
                                    validatedDefinition.getTenantId(),
                                    validatedDefinition.getId() != null ? validatedDefinition.getId() : "")
                            .map(existingCount -> {
                                if (existingCount > 0) {
                                    throw new IllegalStateException(
                                            String.format(
                                                    "Saga definition with workflow ID '%s' already exists for tenant '%s'",
                                                    validatedDefinition.getWorkflowId(),
                                                    validatedDefinition.getTenantId()));
                                }
                                return validatedDefinition;
                            });
                })
                .chain(validatedDefinition -> {
                    if (validatedDefinition.getId() != null) {
                        // Update existing
                        return SagaDefinitionEntity.<SagaDefinitionEntity>findById(validatedDefinition.getId())
                                .onItem().ifNotNull().transformToUni(existing -> {
                                    if (!existing.getTenantId().equals(validatedDefinition.getTenantId())) {
                                        throw new SecurityException(
                                                "Cannot update saga definition from different tenant");
                                    }
                                    updateEntity(existing, validatedDefinition);
                                    return existing.persistAndFlush().replaceWith(existing);
                                })
                                .onItem().ifNull().failWith(
                                        new NotFoundException(
                                                "Saga definition not found: " + validatedDefinition.getId()));
                    } else {
                        // Create new
                        SagaDefinitionEntity newEntity = toEntity(validatedDefinition);
                        return newEntity.persistAndFlush().replaceWith(newEntity);
                    }
                })
                .map(this::toDomain)
                .onFailure()
                .invoke(throwable -> log.error("Failed to save saga definition: {}", sagaDefinition, throwable));
    }

    /**
     * Batch save multiple saga definitions
     */
    public Uni<List<SagaDefinition>> saveAll(List<SagaDefinition> sagaDefinitions) {
        if (sagaDefinitions == null || sagaDefinitions.isEmpty()) {
            return Uni.createFrom().item(Collections.emptyList());
        }

        // Process sequentially since Quarkus Panache doesn't support bulk persist
        // directly
        Uni<List<SagaDefinition>> result = Uni.createFrom().item(new ArrayList<SagaDefinition>());

        for (SagaDefinition def : sagaDefinitions) {
            result = result.chain(list -> saveSagaDefinition(def).map(saved -> {
                list.add(saved);
                return list;
            }));
        }

        return result;
    }

    /**
     * Deactivate saga definition (soft delete)
     */
    public Uni<Boolean> deactivate(String id, String tenantId) {
        return SagaDefinitionEntity.update("active = false, updatedAt = ?1 where id = ?2 and tenantId = ?3",
                Instant.now(), id, tenantId)
                .map(count -> count > 0)
                .onFailure().invoke(throwable -> log.error("Failed to deactivate saga definition: {}, tenant: {}", id,
                        tenantId, throwable));
    }

    /**
     * Activate saga definition
     */
    public Uni<Boolean> activate(String id, String tenantId) {
        return SagaDefinitionEntity.update("active = true, updatedAt = ?1 where id = ?2 and tenantId = ?3",
                Instant.now(), id, tenantId)
                .map(count -> count > 0);
    }

    /**
     * Bulk load saga definitions by IDs
     */
    public Uni<List<SagaDefinition>> bulkLoad(Set<String> ids, String tenantId) {
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(Collections.emptyList());
        }

        // Convert to list and split into batches to avoid query size limits
        List<String> idList = new ArrayList<>(ids);
        List<List<String>> batches = splitIntoBatches(idList, BATCH_SIZE);

        // Process batches and collect results
        Uni<List<SagaDefinition>> result = Uni.createFrom().item(new ArrayList<>());

        for (List<String> batch : batches) {
            result = result.chain(acc -> SagaDefinitionEntity
                    .<SagaDefinitionEntity>find("id in ?1 and tenantId = ?2 and active = true", batch, tenantId)
                    .list()
                    .map(entities -> {
                        List<SagaDefinition> batchResults = entities.stream()
                                .map(this::toDomain)
                                .collect(Collectors.toList());
                        acc.addAll(batchResults);
                        return acc;
                    }));
        }

        return result;
    }

    /**
     * Check if saga definition exists
     */
    public Uni<Boolean> existsByWorkflowId(String workflowId, String tenantId) {
        return SagaDefinitionEntity.count("workflowId = ?1 and tenantId = ?2 and active = true",
                workflowId, tenantId)
                .map(count -> count > 0);
    }

    /**
     * Get count of active saga definitions for a tenant
     */
    public Uni<Long> countActiveByTenant(String tenantId) {
        return SagaDefinitionEntity.count("tenantId = ?1 and active = true", tenantId);
    }

    /**
     * Find saga definitions by compensation strategy
     */
    public Uni<List<SagaDefinition>> findByCompensationStrategy(String tenantId, CompensationStrategy strategy) {
        return SagaDefinitionEntity
                .<SagaDefinitionEntity>find("tenantId = ?1 and compensationStrategy = ?2 and active = true",
                        tenantId, strategy)
                .list()
                .map(entities -> entities.stream()
                        .map(this::toDomain)
                        .collect(Collectors.toList()));
    }

    // Private helper methods
    private void validateSagaDefinition(SagaDefinition sagaDefinition) {
        if (sagaDefinition == null) {
            throw new IllegalArgumentException("Saga definition cannot be null");
        }
        if (sagaDefinition.getWorkflowId() == null || sagaDefinition.getWorkflowId().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID cannot be null or empty");
        }
        if (sagaDefinition.getTenantId() == null || sagaDefinition.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (sagaDefinition.getPivotNode() == null || sagaDefinition.getPivotNode().trim().isEmpty()) {
            throw new IllegalArgumentException("Pivot node cannot be null or empty");
        }
    }

    private List<List<String>> splitIntoBatches(List<String> items, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
        }
        return batches;
    }

    private SagaDefinition toDomain(SagaDefinitionEntity entity) {
        if (entity == null)
            return null;

        return SagaDefinition.builder()
                .id(entity.getId())
                .workflowId(entity.getWorkflowId())
                .name(entity.getName())
                .description(entity.getDescription())
                .tenantId(entity.getTenantId())
                .pivotNode(entity.getPivotNode())
                .compensations(new HashMap<>(entity.getCompensations()))
                .retriableNodes(new HashSet<>(entity.getRetriableNodes()))
                .parameters(new HashMap<>(entity.getParameters()))
                .maxRetries(entity.getMaxRetries())
                .retryDelayMs(entity.getRetryDelayMs())
                .compensationStrategy(entity.getCompensationStrategy())
                .metadata(new HashMap<>(entity.getMetadata()))
                .version(entity.getVersion())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private SagaDefinitionEntity toEntity(SagaDefinition domain) {
        if (domain == null)
            return null;

        SagaDefinitionEntity entity = new SagaDefinitionEntity();
        entity.setId(domain.getId());
        entity.setWorkflowId(domain.getWorkflowId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setTenantId(domain.getTenantId());
        entity.setPivotNode(domain.getPivotNode());
        entity.setCompensations(domain.getCompensations());
        entity.setRetriableNodes(domain.getRetriableNodes());
        entity.setParameters(domain.getParameters());
        entity.setMaxRetries(domain.getMaxRetries());
        entity.setRetryDelayMs(domain.getRetryDelayMs());
        entity.setCompensationStrategy(domain.getCompensationStrategy());
        entity.setMetadata(domain.getMetadata());
        entity.setActive(domain.getActive() != null ? domain.getActive() : true);
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());

        return entity;
    }

    private void updateEntity(SagaDefinitionEntity entity, SagaDefinition domain) {
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setPivotNode(domain.getPivotNode());
        entity.setCompensations(domain.getCompensations());
        entity.setRetriableNodes(domain.getRetriableNodes());
        entity.setParameters(domain.getParameters());
        entity.setMaxRetries(domain.getMaxRetries());
        entity.setRetryDelayMs(domain.getRetryDelayMs());
        entity.setCompensationStrategy(domain.getCompensationStrategy());
        entity.setMetadata(domain.getMetadata());
        entity.setActive(domain.getActive() != null ? domain.getActive() : true);
        entity.setUpdatedBy(domain.getUpdatedBy());
        // createdAt and createdBy should not be updated
    }
}
