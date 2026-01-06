package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

/**
 * Describes a workflow definition without execution semantics.
 * Kernel treats this as opaque - semantics belong to plugins.
 */
@Data
@Builder(toBuilder = true)
public class WorkflowDescriptor {

    private final WorkflowId id;
    private final String version;
    private final String name;
    private final String description;
    private final String tenantId;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Workflow structure (opaque to kernel)
    private final List<NodeDescriptor> nodes;
    private final List<EdgeDescriptor> edges;

    // Metadata and configuration
    private final Map<String, Object> metadata;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;

    // Execution hints (optional)
    private final ExecutionHints executionHints;
    private final RetryPolicy retryPolicy;
    private final TimeoutPolicy timeoutPolicy;

    @JsonCreator
    public WorkflowDescriptor(
            @JsonProperty("id") WorkflowId id,
            @JsonProperty("version") String version,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("createdBy") String createdBy,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("nodes") List<NodeDescriptor> nodes,
            @JsonProperty("edges") List<EdgeDescriptor> edges,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("inputSchema") Map<String, Object> inputSchema,
            @JsonProperty("outputSchema") Map<String, Object> outputSchema,
            @JsonProperty("executionHints") ExecutionHints executionHints,
            @JsonProperty("retryPolicy") RetryPolicy retryPolicy,
            @JsonProperty("timeoutPolicy") TimeoutPolicy timeoutPolicy) {

        this.id = id;
        this.version = version;
        this.name = name;
        this.description = description;
        this.tenantId = tenantId;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.nodes = nodes != null ? Collections.unmodifiableList(nodes) : List.of();
        this.edges = edges != null ? Collections.unmodifiableList(edges) : List.of();
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Map.of();
        this.inputSchema = inputSchema != null ? Collections.unmodifiableMap(inputSchema) : Map.of();
        this.outputSchema = outputSchema != null ? Collections.unmodifiableMap(outputSchema) : Map.of();
        this.executionHints = executionHints;
        this.retryPolicy = retryPolicy;
        this.timeoutPolicy = timeoutPolicy;
    }

    public Optional<NodeDescriptor> findNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst();
    }

    public List<String> getStartNodes() {
        return nodes.stream()
                .filter(node -> "start".equals(node.getType()))
                .map(NodeDescriptor::getNodeId)
                .toList();
    }

    public List<String> getEndNodes() {
        return nodes.stream()
                .filter(node -> "end".equals(node.getType()))
                .map(NodeDescriptor::getNodeId)
                .toList();
    }
}
