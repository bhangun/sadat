package tech.kayys.wayang.project.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.canvas.schema.CanvasNode;
import tech.kayys.wayang.domain.CanvasDefinition;
import tech.kayys.silat.model.WorkflowDefinition;
import tech.kayys.silat.model.WorkflowDefinitionId;
import tech.kayys.silat.model.TenantId;
import tech.kayys.silat.model.WorkflowMetadata;
import tech.kayys.silat.model.NodeDefinition;
import tech.kayys.silat.model.NodeType;
import tech.kayys.silat.model.NodeId;
import tech.kayys.silat.model.Transition;
import tech.kayys.silat.model.InputDefinition;
import tech.kayys.silat.model.OutputDefinition;
import tech.kayys.silat.model.RetryPolicy;
import tech.kayys.silat.saga.CompensationPolicy;

/**
 * Converts visual canvas definition to Silat workflow definition
 */
@ApplicationScoped
public class CanvasToWorkflowConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CanvasToWorkflowConverter.class);

    /**
     * Convert canvas to workflow definition
     */
    public Uni<WorkflowDefinition> convert(
            CanvasDefinition canvas,
            String name,
            String version) {

        LOG.info("Converting canvas to workflow: {} v{}", name, version);

        return Uni.createFrom().item(() -> {
            // Convert canvas nodes to workflow nodes
            List<NodeDefinition> nodes = canvas.canvasData.nodes.stream()
                    .map(this::convertNode)
                    .collect(Collectors.toList());

            // Extract inputs from canvas
            Map<String, InputDefinition> inputs = extractInputs(canvas);

            // Extract outputs from canvas
            Map<String, OutputDefinition> outputs = extractOutputs(canvas);

            // Create metadata
            WorkflowMetadata metadata = new WorkflowMetadata(
                    canvas.metadata != null ? canvas.metadata.labels : Map.of(),
                    Map.of(), // annotations
                    Instant.now(),
                    canvas.createdBy != null ? canvas.createdBy : "control-plane");

            // Create workflow definition
            return new WorkflowDefinition(
                    WorkflowDefinitionId.of(name + "-" + version.replace(".", "-")),
                    TenantId.of(canvas.tenantId),
                    name,
                    version,
                    canvas.description != null ? canvas.description : "",
                    nodes,
                    inputs,
                    outputs,
                    metadata,
                    RetryPolicy.DEFAULT,
                    CompensationPolicy.disabled());
        });
    }

    /**
     * Convert canvas node to workflow node definition
     */
    private NodeDefinition convertNode(CanvasNode canvasNode) {
        // Find dependencies from edges
        List<NodeId> dependsOn = new ArrayList<>();

        // Determine executor type based on node type
        String executorType = mapNodeTypeToExecutor(canvasNode.type);

        // Convert transitions
        List<Transition> transitions = convertTransitions(canvasNode);

        // Extract timeout
        long timeoutSeconds = 300;
        if (canvasNode.config != null && canvasNode.config.containsKey("timeout")) {
            timeoutSeconds = ((Number) canvasNode.config.get("timeout")).longValue();
        }
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        // Check if critical
        boolean critical = false;
        if (canvasNode.config != null && canvasNode.config.containsKey("critical")) {
            critical = (boolean) canvasNode.config.get("critical");
        }

        return new NodeDefinition(
                NodeId.of(canvasNode.id),
                canvasNode.label,
                mapNodeType(canvasNode.type),
                executorType,
                canvasNode.config,
                dependsOn,
                transitions,
                extractRetryPolicy(canvasNode),
                timeout,
                critical);
    }

    /**
     * Map canvas node type to workflow node type
     */
    private NodeType mapNodeType(String canvasType) {
        return switch (canvasType.toUpperCase()) {
            case "TASK", "ACTION" -> NodeType.TASK;
            case "DECISION", "CONDITION" -> NodeType.DECISION;
            case "PARALLEL", "FORK" -> NodeType.PARALLEL;
            case "AGGREGATE", "JOIN" -> NodeType.AGGREGATE;
            case "HUMAN_TASK", "APPROVAL" -> NodeType.HUMAN_TASK;
            case "SUB_WORKFLOW", "NESTED" -> NodeType.SUB_WORKFLOW;
            case "EVENT_WAIT", "WAIT" -> NodeType.EVENT_WAIT;
            case "TIMER", "DELAY" -> NodeType.TIMER;
            case "AI_AGENT" -> NodeType.TASK; // AI agent as task
            case "INTEGRATION" -> NodeType.TASK; // Integration as task
            default -> NodeType.TASK;
        };
    }

    /**
     * Map node type to executor type
     */
    private String mapNodeTypeToExecutor(String nodeType) {
        return switch (nodeType.toUpperCase()) {
            case "AI_AGENT" -> "ai-agent-executor";
            case "INTEGRATION" -> "integration-executor";
            case "HTTP_CALL" -> "http-executor";
            case "DATABASE" -> "database-executor";
            case "SCRIPT" -> "script-executor";
            case "EMAIL" -> "email-executor";
            default -> "generic-executor";
        };
    }

    /**
     * Convert canvas edges to transitions
     */
    private List<Transition> convertTransitions(CanvasNode node) {
        // This would normally look at canvas.edges
        // For now, return empty list (transitions added by edge processing)
        return new ArrayList<>();
    }

    /**
     * Extract retry policy from node config
     */
    private RetryPolicy extractRetryPolicy(CanvasNode node) {
        @SuppressWarnings("unchecked")
        Map<String, Object> retryConfig = (Map<String, Object>) node.config.get("retry");

        if (retryConfig == null) {
            return RetryPolicy.DEFAULT;
        }

        return new RetryPolicy(
                ((Number) retryConfig.getOrDefault("maxAttempts", 3)).intValue(),
                Duration.ofSeconds(((Number) retryConfig.getOrDefault("initialDelay", 1)).longValue()),
                Duration.ofSeconds(((Number) retryConfig.getOrDefault("maxDelay", 300)).longValue()),
                ((Number) retryConfig.getOrDefault("backoffMultiplier", 2.0)).doubleValue(),
                (List<String>) retryConfig.getOrDefault("retryableExceptions", List.of()));
    }

    /**
     * Extract inputs from canvas
     */
    private Map<String, InputDefinition> extractInputs(CanvasDefinition canvas) {
        if (canvas.metadata == null || canvas.metadata.customFields == null) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> inputsConfig = (Map<String, Map<String, Object>>) canvas.metadata.customFields
                .getOrDefault("inputs", Map.of());

        Map<String, InputDefinition> inputs = new HashMap<>();

        inputsConfig.forEach((name, config) -> {
            inputs.put(name, new InputDefinition(
                    name,
                    (String) config.getOrDefault("type", "string"),
                    (boolean) config.getOrDefault("required", false),
                    config.get("default"),
                    (String) config.getOrDefault("description", "")));
        });

        return inputs;
    }

    /**
     * Extract outputs from canvas
     */
    private Map<String, OutputDefinition> extractOutputs(CanvasDefinition canvas) {
        if (canvas.metadata == null || canvas.metadata.customFields == null) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> outputsConfig = (Map<String, Map<String, Object>>) canvas.metadata.customFields
                .getOrDefault("outputs", Map.of());

        Map<String, OutputDefinition> outputs = new HashMap<>();

        outputsConfig.forEach((name, config) -> {
            outputs.put(name, new OutputDefinition(
                    name,
                    (String) config.getOrDefault("type", "string"),
                    (String) config.getOrDefault("description", "")));
        });

        return outputs;
    }
}
