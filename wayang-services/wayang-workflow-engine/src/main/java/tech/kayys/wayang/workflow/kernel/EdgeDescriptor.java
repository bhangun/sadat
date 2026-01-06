package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Describes an edge/transition between nodes in a workflow.
 */
@Data
@Builder
public class EdgeDescriptor {
    private final String sourceNodeId;
    private final String targetNodeId;
    private final String condition; // Optional condition expression
    private final Map<String, Object> metadata;

    @JsonCreator
    public EdgeDescriptor(
            @JsonProperty("sourceNodeId") String sourceNodeId,
            @JsonProperty("targetNodeId") String targetNodeId,
            @JsonProperty("condition") String condition,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.condition = condition;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}
