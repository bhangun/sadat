package tech.kayys.wayang.agent.orchestrator.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;
import tech.kayys.wayang.agent.dto.AgentMessage;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.ExecutionMetrics;
import tech.kayys.wayang.agent.dto.ExecutionStatus;
import tech.kayys.wayang.agent.orchestrator.client.GrpcAgentClient;
import tech.kayys.wayang.agent.orchestrator.client.KafkaAgentProducer;
import tech.kayys.wayang.agent.orchestrator.client.RestAgentClient;
import tech.kayys.wayang.agent.orchestrator.exception.TimeoutException;

/**
 * ============================================================================
 * AGENT COMMUNICATION BUS
 * ============================================================================
 * 
 * Handles inter-agent communication with support for:
 * - Multiple transport protocols (gRPC, REST, Kafka)
 * - Request-response pattern
 * - Publish-subscribe for events
 * - Message routing and load balancing
 */
@ApplicationScoped
public class AgentCommunicationBus {
    
    private static final Logger LOG = LoggerFactory.getLogger(AgentCommunicationBus.class);
    
    @Inject
    GrpcAgentClient grpcClient;
    
    @Inject
    RestAgentClient restClient;
    
    @Inject
    KafkaAgentProducer kafkaProducer;
    
    // Pending requests (for async responses)
    private final ConcurrentMap<String, CompletableFuture<AgentExecutionResult>> pendingRequests =
        new ConcurrentHashMap<>();
    
    /**
     * Send request to agent and wait for response
     */
    public Uni<AgentExecutionResult> sendRequest(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        
        LOG.debug("Sending request to agent: {} via {}", 
            agent.agentId(), agent.endpoint().type());
        
        return switch (agent.endpoint().type()) {
            case GRPC -> grpcClient.execute(agent, request);
            case REST -> restClient.execute(agent, request);
            case KAFKA -> sendViaKafka(agent, request);
            case INTERNAL -> executeInternal(agent, request);
        };
    }
    
    /**
     * Send request via Kafka (async)
     */
    private Uni<AgentExecutionResult> sendViaKafka(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        
        CompletableFuture<AgentExecutionResult> future = new CompletableFuture<>();
        pendingRequests.put(request.requestId(), future);
        
        // Send to Kafka
        kafkaProducer.sendRequest(agent, request)
            .subscribe().with(
                v -> LOG.debug("Request sent to Kafka"),
                error -> {
                    LOG.error("Failed to send to Kafka", error);
                    future.completeExceptionally(error);
                    pendingRequests.remove(request.requestId());
                }
            );
        
        // Wait for response with timeout
        return Uni.createFrom().completionStage(future)
            .ifNoItem().after(Duration.ofMillis(request.constraints().maxExecutionTimeMs()))
            .failWith(() -> {
                pendingRequests.remove(request.requestId());
                return new TimeoutException("No response from agent");
            });
    }
    
    /**
     * Handle response from Kafka
     */
    @Incoming("agent-responses")
    public void handleKafkaResponse(AgentExecutionResult result) {
        LOG.debug("Received response for request: {}", result.requestId());
        
        CompletableFuture<AgentExecutionResult> future = 
            pendingRequests.remove(result.requestId());
        
        if (future != null) {
            future.complete(result);
        }
    }
    
    /**
     * Execute with internal agent (in-process)
     */
    private Uni<AgentExecutionResult> executeInternal(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        
        // This would delegate to actual agent implementation
        LOG.debug("Executing with internal agent");
        
        return Uni.createFrom().item(new AgentExecutionResult(
            request.requestId(),
            agent.agentId(),
            ExecutionStatus.SUCCESS,
            Map.of("result", "Internal execution completed"),
            List.of("internal_execute"),
            new ExecutionMetrics(
                1000L, 100, 0, 0L, 1.0, Map.of()
            ),
            List.of(),
            Map.of(),
            Instant.now()
        ));
    }
    
    /**
     * Broadcast event to multiple agents
     */
    public Uni<Void> broadcastEvent(
            AgentMessage message,
            List<String> recipientIds) {
        
        LOG.debug("Broadcasting event to {} agents", recipientIds.size());
        
        return Multi.createFrom().iterable(recipientIds)
            .onItem().transformToUniAndMerge(recipientId ->
                sendMessage(recipientId, message)
            )
            .collect().asList()
            .replaceWithVoid();
    }
    
    private Uni<Void> sendMessage(String recipientId, AgentMessage message) {
        // Implementation would route to appropriate agent
        return Uni.createFrom().voidItem();
    }
}

