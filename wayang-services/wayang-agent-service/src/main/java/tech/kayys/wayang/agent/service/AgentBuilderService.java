package tech.kayys.wayang.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.*;
import tech.kayys.wayang.agent.entity.AgentEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Agent Builder Service - Core functionality for low-code agent building
 *
 * Features:
 * - Visual agent builder with drag-and-drop interface
 * - LLM configuration and management
 * - Tool selection and integration
 * - Workflow definition and management
 * - Template system for quick creation
 * - Agent execution and monitoring
 */
@ApplicationScoped
public class AgentBuilderService {

    @Inject
    AgentWorkflowBuilder workflowBuilder;

    @Inject
    AgentTemplateManager templateManager;

    @Inject
    AgentValidationService validationService;

    @Inject
    AgentVersionManager versionManager;

    @Inject
    AgentTestingService testingService;

    @Inject
    LLMProviderRegistry llmProviderRegistry;

    @Inject
    ToolMarketplace toolMarketplace;

    @Inject
    AgentMetricsCollector metricsCollector;

    @Inject
    AgentRepository agentRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Create new agent with full configuration (UI-focused)
     */
    public Uni<AgentBuilderResponse> createAgent(AgentBuilderRequest request) {
        Log.info("Creating agent via builder: " + request.name() + ", tenant: " + request.tenantId());

        // Validate the request
        ValidationResult validation = validateBuilderRequest(request);
        if (!validation.isValid()) {
            return Uni.createFrom().failure(new AgentValidationException(validation.errors()));
        }

        // Create agent entity from builder request
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName(request.name());
        agentEntity.setDescription(request.description());
        agentEntity.setTenantId(request.tenantId());
        agentEntity.setType(request.agentType());
        agentEntity.setLlmConfig(toJsonString(request.llmConfig()));
        agentEntity.setTools(toJsonString(request.tools()));
        agentEntity.setConfig(toJsonString(request.config()));

        return agentRepository.save(agentEntity)
                .onItem().transform(savedAgent -> mapToBuilderResponse(savedAgent, request))
                .invoke(response -> metricsCollector.recordAgentCreated(request.agentType()));
    }

    /**
     * Update existing agent configuration (UI-friendly)
     */
    public Uni<AgentBuilderResponse> updateAgent(String agentId, AgentBuilderUpdateRequest request) {
        Log.info("Updating agent via builder: " + agentId);

        return agentRepository.findById(agentId)
                .onItem().ifNull().failWith(() -> new AgentNotFoundException(agentId))
                .onItem().transformToUni(optionalAgent -> {
                    if (optionalAgent.isEmpty()) {
                        return Uni.createFrom().failure(new AgentNotFoundException(agentId));
                    }

                    AgentEntity existingAgent = optionalAgent.get();
                    // Update agent from builder request
                    if (request.name() != null) existingAgent.setName(request.name());
                    if (request.description() != null) existingAgent.setDescription(request.description());
                    if (request.llmConfig() != null) existingAgent.setLlmConfig(toJsonString(request.llmConfig()));
                    if (request.tools() != null) existingAgent.setTools(toJsonString(request.tools()));
                    if (request.config() != null) existingAgent.setConfig(toJsonString(request.config()));

                    return agentRepository.save(existingAgent)
                            .onItem().transform(updated -> mapToBuilderResponseFromEntity(updated));
                });
    }

    /**
     * Get agent configuration for UI editing
     */
    public Uni<AgentBuilderDetail> getAgentForBuilder(String agentId) {
        return agentRepository.findById(agentId)
                .onItem().ifNull().failWith(() -> new AgentNotFoundException(agentId))
                .onItem().transformToUni(optionalAgent -> {
                    if (optionalAgent.isEmpty()) {
                        return Uni.createFrom().failure(new AgentNotFoundException(agentId));
                    }

                    AgentEntity agent = optionalAgent.get();
                    return Uni.createFrom().item(new AgentBuilderDetail(
                            agent.getId().toString(),
                            agent.getName(),
                            agent.getDescription(),
                            agent.getTenantId(),
                            agent.getType(),
                            agent.getIsActive(),
                            fromJsonString(agent.getLlmConfig(), LLMConfig.class),
                            agent.getTools() != null ? fromJsonStringList(agent.getTools()) : List.of(),
                            agent.getConfig() != null ? fromJsonStringMap(agent.getConfig()) : Map.of(),
                            agent.getCreatedAt(),
                            agent.getUpdatedAt(),
                            "ACTIVE", // Status placeholder
                            List.of(), // Versions placeholder
                            new AgentMetrics(0L, 0, 0.0) // Metrics placeholder
                    ));
                });
    }

    /**
     * List agents for the builder UI
     */
    public Uni<AgentBuilderList> listAgentsForBuilder(
            String tenantId,
            AgentType agentType,
            String status,
            String search,
            int page,
            int size) {
        
        return agentRepository.findWithFilters(
                tenantId, agentType, null, null, page, size)
                .map(agents -> new AgentBuilderList(
                        agents.stream()
                                .map(agent -> new AgentBuilderSummary(
                                        agent.getId().toString(),
                                        agent.getName(),
                                        agent.getDescription(),
                                        agent.getType(),
                                        agent.getIsActive(),
                                        agent.getCreatedAt()
                                ))
                                .collect(Collectors.toList()),
                        page,
                        size,
                        agents.size(),
                        (int) Math.ceil((double) agents.size() / size)
                ));
    }

    /**
     * Execute agent through builder interface
     */
    public Uni<AgentExecutionResponse> executeAgent(
            String agentId,
            AgentExecutionRequest request) {
        Log.info("Executing agent via builder: " + agentId);

        return agentRepository.findById(agentId)
                .onItem().ifNull().failWith(() -> new AgentNotFoundException(agentId))
                .onItem().transformToUni(optionalAgent -> {
                    if (optionalAgent.isEmpty()) {
                        return Uni.createFrom().failure(new AgentNotFoundException(agentId));
                    }

                    AgentEntity agent = optionalAgent.get();
                    // Record execution start
                    metricsCollector.recordExecution(agentId, request.executionMode());

                    // In a real implementation, this would execute the actual agent
                    // For now, returning a mock response
                    AgentExecutionResponse response = new AgentExecutionResponse(
                            java.util.UUID.randomUUID().toString(),
                            Map.of("result", "Agent executed successfully from Agent Builder"),
                            new AgentMetrics(1L, 1, 0.0),
                            List.of("basic-tool")
                    );

                    return Uni.createFrom().item(response);
                });
    }

    /**
     * Get available tools for the builder UI
     */
    public Uni<List<ToolDefinition>> getAvailableTools() {
        return toolMarketplace.listAvailableTools()
                .map(tools -> tools.stream()
                        .map(this::mapToToolDefinition)
                        .collect(Collectors.toList()));
    }

    /**
     * Get available LLM providers for the builder UI
     */
    public Uni<List<LLMProviderDefinition>> getAvailableProviders() {
        // Return mock providers - in real implementation, this would fetch from registry
        return Uni.createFrom().item(List.of(
                new LLMProviderDefinition("openai", "OpenAI", "OpenAI API"),
                new LLMProviderDefinition("anthropic", "Anthropic", "Anthropic Claude"),
                new LLMProviderDefinition("ollama", "Ollama", "Local Ollama models")
        ));
    }

    // Helper methods
    private ValidationResult validateBuilderRequest(AgentBuilderRequest request) {
        List<String> errors = new java.util.ArrayList<>();
        
        if (request.name() == null || request.name().trim().isEmpty()) {
            errors.add("Agent name is required");
        }
        
        if (request.tenantId() == null || request.tenantId().trim().isEmpty()) {
            errors.add("Tenant ID is required");
        }
        
        if (request.agentType() == null) {
            errors.add("Agent type is required");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private AgentBuilderResponse mapToBuilderResponse(AgentEntity entity, AgentBuilderRequest request) {
        return new AgentBuilderResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.getTenantId(),
                entity.getType(),
                entity.getIsActive(),
                java.time.LocalDateTime.now(), // created at
                java.time.LocalDateTime.now(), // updated at
                "ACTIVE" // status
        );
    }

    private AgentBuilderResponse mapToBuilderResponseFromEntity(AgentEntity entity) {
        return new AgentBuilderResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.getTenantId(),
                entity.getType(),
                entity.getIsActive(),
                entity.getCreatedAt(), // created at
                entity.getUpdatedAt(), // updated at
                "ACTIVE" // status
        );
    }

    private ToolDefinition mapToToolDefinition(Map<String, Object> toolMap) {
        return new ToolDefinition(
                (String) toolMap.get("id"),
                (String) toolMap.get("name"), 
                (String) toolMap.get("description"),
                (String) toolMap.get("category"),
                Map.class.cast(toolMap.getOrDefault("parameters", Map.of()))
        );
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Log.errorf("Failed to serialize object to JSON: %s", e.getMessage());
            return "{}";
        }
    }

    private <T> T fromJsonString(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || json.equals("null")) {
            // Return appropriate default instances based on class type
            if (clazz == LLMConfig.class) {
                return (T) new LLMConfig("", "", 0.0, 0);
            } else if (clazz == AgentMetrics.class) {
                return (T) new AgentMetrics(0L, 0, 0.0); // Fixed: use correct constructor
            } else {
                try {
                    // Fallback - try to create an instance if possible
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    Log.warnf("Could not create instance of %s: %s", clazz.getSimpleName(), e.getMessage());
                    return null;
                }
            }
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            Log.errorf("Failed to deserialize JSON to %s: %s", clazz.getSimpleName(), e.getMessage());
            // Return appropriate default instances based on class type
            if (clazz == LLMConfig.class) {
                return (T) new LLMConfig("", "", 0.0, 0);
            } else if (clazz == AgentMetrics.class) {
                return (T) new AgentMetrics(0L, 0, 0.0); // Fixed: use correct constructor
            } else {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    Log.warnf("Could not create instance of %s: %s", clazz.getSimpleName(), ex.getMessage());
                    return null;
                }
            }
        }
    }

    private List<String> fromJsonStringList(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            // Parse JSON array to List<String>
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            Log.errorf("Failed to deserialize JSON list: %s", e.getMessage());
            return List.of(); // Return empty list on error
        }
    }

    private Map<String, Object> fromJsonStringMap(String json) {
        if (json == null || json.isEmpty()) return Map.of();
        try {
            // Parse JSON to Map<String, Object>
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException e) {
            Log.errorf("Failed to deserialize JSON map: %s", e.getMessage());
            return Map.of(); // Return empty map on error
        }
    }

    // Exception class
    public static class AgentNotFoundException extends RuntimeException {
        public AgentNotFoundException(String agentId) {
            super("Agent not found: " + agentId);
        }
    }
}