package tech.kayys.wayang.workflow.saga.model;

import java.time.Instant;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain object - SagaDefinition
 */
/**
 * SagaDefinition domain object (simplified version - you can expand as needed)
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SagaDefinition {
    private String id;
    private String workflowId;
    private String name;
    private String description;
    private String tenantId;
    private String pivotNode;
    private Map<String, CompensationActionDefinition> compensations;
    private Set<String> retriableNodes;
    private Map<String, String> parameters;
    private Integer maxRetries;
    private Long retryDelayMs;
    private CompensationStrategy compensationStrategy;
    private Map<String, Object> metadata;
    private Long version;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    public boolean isRetriableNode(String nodeId) {
        return retriableNodes != null && retriableNodes.contains(nodeId);
    }

    public CompensationActionDefinition getCompensationForNode(String nodeId) {
        return compensations != null ? compensations.get(nodeId) : null;
    }

    public boolean hasCompensationForNode(String nodeId) {
        return compensations != null && compensations.containsKey(nodeId);
    }
}