package tech.kayys.wayang.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;

/**
 * LogicDefinitionDTO - Workflow logic structure
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogicDefinitionDTO {

    @NotNull
    private List<NodeDTO> nodes = new ArrayList<>();

    @NotNull
    private List<ConnectionDTO> connections = new ArrayList<>();

    private WorkflowRulesDTO rules;

    // Getters and setters...
    public List<NodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<ConnectionDTO> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionDTO> connections) {
        this.connections = connections;
    }

    public WorkflowRulesDTO getRules() {
        return rules;
    }

    public void setRules(WorkflowRulesDTO rules) {
        this.rules = rules;
    }
}
