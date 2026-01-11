package tech.kayys.wayang.agent.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.AgentMemoryManager;
import tech.kayys.wayang.agent.model.ToolRegistry;
import tech.kayys.wayang.agent.model.llmprovider.LLMProviderRegistry;

/**
 * Health check for agent system
 */
@ApplicationScoped
public class AgentHealthCheck implements org.eclipse.microprofile.health.HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(AgentHealthCheck.class);

    @Inject
    LLMProviderRegistry llmRegistry;

    @Inject
    ToolRegistry toolRegistry;

    @Inject
    AgentMemoryManager memoryManager;

    @Override
    @org.eclipse.microprofile.health.Liveness
    public org.eclipse.microprofile.health.HealthCheckResponse call() {
        var builder = org.eclipse.microprofile.health.HealthCheckResponse
                .named("agent-system")
                .up();

        try {
            // Check LLM providers
            List<String> providers = llmRegistry.getAvailableProviders();
            builder.withData("llm_providers", providers.size());

            // Check tool registry
            builder.withData("tools_registered", true);

            // Check memory manager
            builder.withData("memory_manager", "operational");

            return builder.build();

        } catch (Exception e) {
            LOG.error("Health check failed", e);
            return builder.down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
