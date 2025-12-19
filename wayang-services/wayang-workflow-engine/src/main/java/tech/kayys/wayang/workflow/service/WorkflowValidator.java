package tech.kayys.wayang.workflow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;

/**
 * Workflow validator.
 */
@ApplicationScoped
public class WorkflowValidator {

    public Uni<ValidationResult> validate(WorkflowDefinition workflow) {
        List<String> errors = new ArrayList<>();

        // Check basic structure
        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            errors.add("Workflow must have at least one node");
        }

        if (workflow.getEdges() == null) {
            errors.add("Workflow must have edges defined");
        }

        // Validate node references in edges
        if (workflow.getNodes() != null && workflow.getEdges() != null) {
            Set<String> nodeIds = workflow.getNodes().stream()
                    .map(NodeDefinition::getId)
                    .collect(Collectors.toSet());

            for (EdgeDefinition edge : workflow.getEdges()) {
                if (!nodeIds.contains(edge.getFrom())) {
                    errors.add("Edge references unknown source node: " + edge.getFrom());
                }
                if (!nodeIds.contains(edge.getTo())) {
                    errors.add("Edge references unknown target node: " + edge.getTo());
                }
            }
        }

        // Check for cycles (optional - some workflows may allow cycles)
        if (workflow.getNodes() != null && workflow.getEdges() != null) {
            if (hasCycles(workflow)) {
                errors.add("Workflow contains cycles (not currently supported)");
            }
        }

        if (!errors.isEmpty()) {
            return Uni.createFrom().item(
                    ValidationResult.failure("Workflow validation failed", errors));
        }

        return Uni.createFrom().item(ValidationResult.success());
    }

    private boolean hasCycles(WorkflowDefinition workflow) {
        // Simple cycle detection using DFS
        Map<String, Set<String>> adjacency = new HashMap<>();

        for (EdgeDefinition edge : workflow.getEdges()) {
            adjacency.computeIfAbsent(edge.getFrom(), k -> new HashSet<>())
                    .add(edge.getTo());
        }

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (NodeDefinition node : workflow.getNodes()) {
            if (hasCycleDFS(node.getId(), adjacency, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCycleDFS(
            String nodeId,
            Map<String, Set<String>> adjacency,
            Set<String> visited,
            Set<String> recursionStack) {

        if (recursionStack.contains(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        Set<String> neighbors = adjacency.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (hasCycleDFS(neighbor, adjacency, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }
}