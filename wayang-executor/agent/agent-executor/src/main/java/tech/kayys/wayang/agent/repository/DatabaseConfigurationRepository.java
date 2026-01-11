package tech.kayys.wayang.agent.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.domain.AgentConfigurationEntity;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.service.JsonMapper;

/**
 * Production ConfigurationRepository using database
 */
@ApplicationScoped
public class DatabaseConfigurationRepository implements ConfigurationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfigurationRepository.class);

    @jakarta.inject.Inject
    AgentConfigurationRepository repository;

    @jakarta.inject.Inject
    JsonMapper jsonMapper;

    @Override
    public Uni<AgentConfiguration> findByAgentId(String agentId, String tenantId) {
        LOG.info("Finding agent configuration for agentId={}, tenantId={}", agentId, tenantId);
        return repository.findByAgentAndTenant(agentId, tenantId)
                .map(entity -> entity != null ? toConfiguration(entity) : null);
    }

    @Override
    public Uni<Void> save(AgentConfiguration config) {
        return repository.findByAgentAndTenant(config.agentId(), config.tenantId())
                .flatMap(existing -> {
                    AgentConfigurationEntity entity = existing != null ? existing : new AgentConfigurationEntity();

                    updateEntity(entity, config);

                    return repository.persistOrUpdate(entity)
                            .replaceWithVoid();
                });
    }

    @Override
    public Uni<Void> delete(String agentId, String tenantId) {
        return repository.deleteByAgentAndTenant(agentId, tenantId)
                .replaceWithVoid();
    }

    @Override
    public Uni<List<AgentConfiguration>> findByTenant(String tenantId) {
        return repository.findByTenant(tenantId)
                .map(entities -> entities.stream()
                        .map(this::toConfiguration)
                        .collect(Collectors.toList()));
    }

    private AgentConfiguration toConfiguration(AgentConfigurationEntity entity) {
        return AgentConfiguration.builder()
                .agentId(entity.getAgentId())
                .tenantId(entity.getTenantId())
                .llmProvider(entity.getLlmProvider())
                .llmModel(entity.getLlmModel())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .memoryEnabled(entity.getMemoryEnabled())
                .memoryType(entity.getMemoryType())
                .memoryWindowSize(entity.getMemoryWindowSize())
                .enabledTools(jsonMapper.fromJsonArray(entity.getEnabledTools()))
                .allowToolCalls(entity.getAllowToolCalls())
                .systemPrompt(entity.getSystemPrompt())
                .streaming(entity.getStreaming())
                .maxIterations(entity.getMaxIterations())
                .additionalConfig(jsonMapper.fromJsonObject(entity.getAdditionalConfig()))
                .build();
    }

    private void updateEntity(AgentConfigurationEntity entity, AgentConfiguration config) {
        entity.setAgentId(config.agentId());
        entity.setTenantId(config.tenantId());
        entity.setLlmProvider(config.llmProvider());
        entity.setLlmModel(config.llmModel());
        entity.setTemperature(config.temperature());
        entity.setMaxTokens(config.maxTokens());
        entity.setMemoryEnabled(config.memoryEnabled());
        entity.setMemoryType(config.memoryType());
        entity.setMemoryWindowSize(config.memoryWindowSize());
        entity.setEnabledTools(jsonMapper.toJsonArray(config.enabledTools()));
        entity.setAllowToolCalls(config.allowToolCalls());
        entity.setSystemPrompt(config.systemPrompt());
        entity.setStreaming(config.streaming());
        entity.setMaxIterations(config.maxIterations());
        entity.setAdditionalConfig(jsonMapper.toJsonObject(config.additionalConfig()));
    }
}
