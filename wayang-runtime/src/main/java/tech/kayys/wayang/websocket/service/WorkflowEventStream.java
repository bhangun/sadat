package tech.kayys.wayang.websocket.service;

import java.util.UUID;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Workflow event streaming service
 */
@ApplicationScoped
public class WorkflowEventStream {

    public Multi<String> streamWorkflowEvents(String runId) {
        // In production, this would connect to Kafka or event store
        return Multi.createFrom().ticks()
                .every(java.time.Duration.ofSeconds(1))
                .map(tick -> String.format(
                        "{\"runId\": \"%s\", \"status\": \"RUNNING\", \"timestamp\": \"%s\"}",
                        runId,
                        java.time.Instant.now()));
    }

    public Multi<String> streamAgentEvents(UUID agentId) {
        return Multi.createFrom().ticks()
                .every(java.time.Duration.ofSeconds(2))
                .map(tick -> String.format(
                        "{\"agentId\": \"%s\", \"status\": \"THINKING\", \"timestamp\": \"%s\"}",
                        agentId,
                        java.time.Instant.now()));
    }
}
