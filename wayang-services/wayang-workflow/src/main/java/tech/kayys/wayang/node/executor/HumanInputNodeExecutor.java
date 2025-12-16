package tech.kayys.wayang.node.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * HUMAN_INPUT node executor
 */
@ApplicationScoped
public class HumanInputNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(HumanInputNodeExecutor.class);

    // Store pending human input requests
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingInputs = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.HumanInputConfig config = node.getConfig().getHumanInputConfig();

        if (config == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Human input config is required"));
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingInputs.put(requestId, future);

        // Store request info in context for retrieval
        context.setVariable("humanInputRequestId", requestId);
        context.setVariable("humanInputFormSchema", config.getFormSchema());

        LOG.infof("Waiting for human input for node: %s (requestId: %s)", node.getName(), requestId);

        // Send notifications if configured
        if (config.getNotificationChannels() != null) {
            sendNotifications(requestId, config.getNotificationChannels(), node);
        }

        // Wait for input with timeout
        int timeout = config.getTimeout() != null ? config.getTimeout() : 3600000; // 1 hour default

        return Uni.createFrom().completionStage(future)
                .ifNoItem().after(java.time.Duration.ofMillis(timeout))
                .recoverWithItem(() -> {
                    pendingInputs.remove(requestId);
                    LOG.warnf("Human input timeout for node: %s", node.getName());
                    return Map.of("timeout", true);
                })
                .map(input -> {
                    pendingInputs.remove(requestId);
                    Map<String, Object> output = new HashMap<>();
                    output.put("humanInput", input);
                    output.put("requestId", requestId);
                    return new NodeExecutionResult(node.getId(), true, output, null);
                });
    }

    /**
     * Submit human input (called by external API)
     */
    public boolean submitInput(String requestId, Map<String, Object> input) {
        CompletableFuture<Map<String, Object>> future = pendingInputs.get(requestId);
        if (future != null) {
            future.complete(input);
            return true;
        }
        return false;
    }

    /**
     * Get pending requests
     */
    public Set<String> getPendingRequests() {
        return new HashSet<>(pendingInputs.keySet());
    }

    private void sendNotifications(String requestId, List<String> channels, Workflow.Node node) {
        // Implement notification logic (email, SMS, push, webhook)
        LOG.infof("Sending notifications via: %s for request: %s", channels, requestId);
        // In production: integrate with notification services
    }

    @Override
    public Workflow.Node.NodeType getSupportedType() {
        return Workflow.Node.NodeType.HUMAN_INPUT;
    }
}
