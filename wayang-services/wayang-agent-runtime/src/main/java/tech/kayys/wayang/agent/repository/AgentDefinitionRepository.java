package tech.kayys.wayang.agent.repository;

import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.domain.AgentDefinitionEntity;
import tech.kayys.wayang.agent.model.AgentDefinition;

/**
 * Enhanced Repository with Database Persistence
 */
@ApplicationScoped
public class AgentDefinitionRepository implements PanacheRepository<AgentDefinitionEntity> {

    private static final Logger LOG = Logger.getLogger(AgentDefinitionRepository.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * Save agent definition
     */
    public Uni<AgentDefinition> saveAgent(
            AgentDefinition agent) {

        return Uni.createFrom().item(() -> {
            try {
                AgentDefinitionEntity entity = new AgentDefinitionEntity();
                entity.id = agent.getId();
                entity.name = agent.getName();
                entity.description = agent.getDescription();
                entity.type = agent.getType().name();
                entity.status = agent.getStatus().name();
                entity.definitionJson = objectMapper.writeValueAsString(agent);
                entity.createdBy = "system"; // TODO: Get from security context
                entity.version = "1.0";

                return entity;
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize agent definition", e);
            }
        })
                .chain(entity -> entity.persistAndFlush())
                .map(v -> agent)
                .onFailure().invoke(error -> LOG.errorf(error, "Failed to save agent: %s", agent.getId()));
    }

    /**
     * Find agent by ID
     */
    public Uni<AgentDefinition> findAgentById(String id) {
        return AgentDefinitionEntity.<AgentDefinitionEntity>findById(id)
                .map(entity -> {
                    if (entity == null)
                        return null;

                    try {
                        return objectMapper.readValue(
                                entity.definitionJson,
                                AgentDefinition.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize agent: " + id, e);
                    }
                });
    }

    /**
     * Find all agents with filters
     */
    public Uni<List<AgentDefinition>> findAllAgents(
            String status, String type, int page, int size) {

        StringBuilder query = new StringBuilder("SELECT e FROM AgentDefinitionEntity e WHERE 1=1");

        if (status != null) {
            query.append(" AND e.status = :status");
        }
        if (type != null) {
            query.append(" AND e.type = :type");
        }
        query.append(" ORDER BY e.updatedAt DESC");

        PanacheQuery<AgentDefinitionEntity> panacheQuery = AgentDefinitionEntity.find(query.toString());

        if (status != null) {
            panacheQuery = panacheQuery.withHint("status", status);
        }
        if (type != null) {
            panacheQuery = panacheQuery.withHint("type", type);
        }

        return panacheQuery.page(page, size).list()
                .map(entities -> entities.stream()
                        .map(entity -> {
                            try {
                                return objectMapper.readValue(
                                        entity.definitionJson,
                                        AgentDefinition.class);
                            } catch (Exception e) {
                                LOG.errorf(e, "Failed to deserialize agent: %s", entity.id);
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Update agent
     */
    public Uni<AgentDefinition> updateAgent(
            AgentDefinition agent) {

        return AgentDefinitionEntity.<AgentDefinitionEntity>findById(agent.getId())
                .chain(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Agent not found: " + agent.getId()));
                    }

                    try {
                        entity.name = agent.getName();
                        entity.description = agent.getDescription();
                        entity.type = agent.getType().name();
                        entity.status = agent.getStatus().name();
                        entity.definitionJson = objectMapper.writeValueAsString(agent);

                        return entity.persistAndFlush().map(v -> agent);
                    } catch (Exception e) {
                        return Uni.createFrom().failure(
                                new RuntimeException("Failed to update agent: " + agent.getId(), e));
                    }
                });
    }

    /**
     * Delete agent
     */
    public Uni<Boolean> deleteAgent(String id) {
        return AgentDefinitionEntity.deleteById(id)
                .map(deleted -> deleted != null && deleted);
    }

    /**
     * Search agents by name
     */
    public Uni<List<AgentDefinition>> searchAgents(
            String searchTerm, int limit) {

        return AgentDefinitionEntity.<AgentDefinitionEntity>find(
                "LOWER(name) LIKE ?1 OR LOWER(description) LIKE ?1",
                "%" + searchTerm.toLowerCase() + "%")
                .page(0, limit)
                .list()
                .map(entities -> entities.stream()
                        .map(entity -> {
                            try {
                                return objectMapper.readValue(
                                        entity.definitionJson,
                                        AgentDefinition.class);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList()));
    }
}
