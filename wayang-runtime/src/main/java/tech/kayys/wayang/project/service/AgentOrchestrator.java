package tech.kayys.wayang.project.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.dto.AgentExecutionResult;
import tech.kayys.wayang.project.dto.AgentStatus;
import tech.kayys.wayang.project.dto.AgentTask;
import tech.kayys.wayang.project.dto.ActiveAgent;

/**
 * Orchestrates AI agent execution
 */
@ApplicationScoped
public class AgentOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AgentOrchestrator.class);

    @Inject
    LLMProviderFactory llmFactory;

    @Inject
    AgentMemoryManager memoryManager;

    @Inject
    AgentGuardrailEngine guardrailEngine;

    // Active agents cache
    private final Map<UUID, ActiveAgent> activeAgents = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Activate an AI agent
     */
    public Uni<AIAgent> activateAgent(UUID agentId) {
        LOG.info("Activating agent: {}", agentId);

        return AIAgent.<AIAgent>findById(agentId)
                .flatMap(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Agent not found"));
                    }

                    // Initialize LLM provider
                    return llmFactory.createProvider(agent.llmConfig)
                            .flatMap(llmProvider -> {
                                // Initialize memory
                                return memoryManager.initializeMemory(agent)
                                        .map(memory -> {
                                            // Create active agent
                                            ActiveAgent activeAgent = new ActiveAgent(
                                                    agent,
                                                    llmProvider,
                                                    memory);

                                            activeAgents.put(agentId, activeAgent);

                                            // Update status
                                            agent.status = AgentStatus.ACTIVE;
                                            return agent.persistAndFlush();
                                        });
                            })
                            .replaceWith(agent);
                });
    }

    /**
     * Execute agent task
     */
    public Uni<AgentExecutionResult> executeTask(UUID agentId, AgentTask task) {
        LOG.info("Executing task {} with agent {}", task.taskId(), agentId);

        ActiveAgent activeAgent = activeAgents.get(agentId);
        if (activeAgent == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Agent not active"));
        }

        return Uni.createFrom().item(() -> {
            // Check guardrails on input
            guardrailEngine.checkInput(activeAgent.agent, task.instruction());

            // Retrieve relevant memories
            List<String> memories = memoryManager.retrieveRelevant(
                    activeAgent.memory,
                    task.instruction());

            // Build prompt with context
            String prompt = buildPrompt(
                    activeAgent.agent,
                    task,
                    memories);

            return prompt;
        })
                .flatMap(prompt ->
                // Call LLM
                activeAgent.llmProvider.complete(prompt, activeAgent.agent.llmConfig))
                .map(llmResponse -> {
                    // Check guardrails on output
                    String sanitized = guardrailEngine.checkOutput(
                            activeAgent.agent,
                            llmResponse);

                    // Store interaction in memory
                    memoryManager.storeInteraction(
                            activeAgent.memory,
                            task.instruction(),
                            sanitized);

                    // Parse response
                    return parseAgentResponse(task.taskId(), sanitized);
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("Agent execution failed", error);
                    return new AgentExecutionResult(
                            task.taskId(),
                            false,
                            null,
                            List.of(),
                            Map.of(),
                            List.of(error.getMessage()));
                });
    }

    /**
     * Build prompt with context
     */
    private String buildPrompt(
            AIAgent agent,
            AgentTask task,
            List<String> memories) {

        StringBuilder prompt = new StringBuilder();

        // System prompt
        if (agent.llmConfig.systemPrompt != null) {
            prompt.append(agent.llmConfig.systemPrompt).append("\n\n");
        }

        // Relevant memories
        if (!memories.isEmpty()) {
            prompt.append("Relevant context:\n");
            memories.forEach(memory -> prompt.append("- ").append(memory).append("\n"));
            prompt.append("\n");
        }

        // Task context
        if (task.context() != null && !task.context().isEmpty()) {
            prompt.append("Current context:\n");
            task.context().forEach((key, value) -> prompt.append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        // Available tools
        if (agent.tools != null && !agent.tools.isEmpty()) {
            prompt.append("Available tools:\n");
            agent.tools.forEach(tool -> prompt.append("- ").append(tool.name)
                    .append(": ").append(tool.description).append("\n"));
            prompt.append("\n");
        }

        // Task instruction
        prompt.append("Task: ").append(task.instruction());

        return prompt.toString();
    }

    /**
     * Parse agent response
     */
    private AgentExecutionResult parseAgentResponse(
            String taskId,
            String response) {

        // Simple parsing - in production, this would be more sophisticated
        return new AgentExecutionResult(
                taskId,
                true,
                response,
                List.of("generate_response"),
                Map.of("response", response),
                List.of());
    }
}
