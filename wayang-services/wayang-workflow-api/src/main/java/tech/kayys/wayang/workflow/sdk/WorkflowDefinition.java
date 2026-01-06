
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

@Data
@Builder(toBuilder = true)
public class WorkflowDefinition {
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String tenantId;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Workflow structure
    private final List<NodeDefinition> nodes;
    private final List<EdgeDefinition> edges;
    private final Map<String, Object> metadata;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;

    // Execution settings
    private final ExecutionSettings executionSettings;
    private final RetryPolicy retryPolicy;
    private final TimeoutSettings timeoutSettings;

    @JsonCreator
    public WorkflowDefinition(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("version") String version,
            @JsonProperty("description") String description,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("createdBy") String createdBy,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("nodes") List<NodeDefinition> nodes,
            @JsonProperty("edges") List<EdgeDefinition> edges,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("inputSchema") Map<String, Object> inputSchema,
            @JsonProperty("outputSchema") Map<String, Object> outputSchema,
            @JsonProperty("executionSettings") ExecutionSettings executionSettings,
            @JsonProperty("retryPolicy") RetryPolicy retryPolicy,
            @JsonProperty("timeoutSettings") TimeoutSettings timeoutSettings) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.tenantId = tenantId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.inputSchema = inputSchema != null ? inputSchema : new HashMap<>();
        this.outputSchema = outputSchema != null ? outputSchema : new HashMap<>();
        this.executionSettings = executionSettings;
        this.retryPolicy = retryPolicy;
        this.timeoutSettings = timeoutSettings;
    }
}