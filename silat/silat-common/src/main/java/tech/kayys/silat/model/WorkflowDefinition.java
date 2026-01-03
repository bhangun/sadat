package tech.kayys.silat.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import tech.kayys.silat.saga.CompensationPolicy;

/**
 * Workflow Definition - Blueprint for workflow execution
 * Immutable after creation
 */
public record WorkflowDefinition(
        WorkflowDefinitionId id,
        String name,
        String version,
        String description,
        List<NodeDefinition> nodes,
        Map<String, InputDefinition> inputs,
        Map<String, OutputDefinition> outputs,
        WorkflowMetadata metadata,
        RetryPolicy defaultRetryPolicy,
        CompensationPolicy compensationPolicy) {

    public WorkflowDefinition {
        Objects.requireNonNull(id, "Workflow ID cannot be null");
        Objects.requireNonNull(name, "Workflow name cannot be null");
        Objects.requireNonNull(nodes, "Nodes cannot be null");

        nodes = List.copyOf(nodes);
        inputs = inputs != null ? Map.copyOf(inputs) : Map.of();
        outputs = outputs != null ? Map.copyOf(outputs) : Map.of();

        // defaults (do NOT remove user control)
        defaultRetryPolicy = defaultRetryPolicy != null ? defaultRetryPolicy : RetryPolicy.none();

        compensationPolicy = compensationPolicy != null ? compensationPolicy : CompensationPolicy.disabled();
    }

    // ==================== NODE ACCESS ====================

    public Optional<NodeDefinition> findNode(NodeId nodeId) {
        return nodes.stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst();
    }

    public List<NodeDefinition> getStartNodes() {
        return nodes.stream()
                .filter(NodeDefinition::isStartNode)
                .toList();
    }

    public List<NodeDefinition> getEndNodes() {
        return nodes.stream()
                .filter(NodeDefinition::isEndNode)
                .toList();
    }

    // ==================== VALIDATION ====================

    public boolean isValid() {
        return hasAtLeastOneStartNode()
                && hasNoCircularDependencies()
                && hasValidDependencies()
                && hasValidIO();
    }

    private boolean hasAtLeastOneStartNode() {
        return !getStartNodes().isEmpty();
    }

    private boolean hasNoCircularDependencies() {
        // Simple DFS cycle detection
        Map<NodeId, List<NodeId>> graph = buildDependencyGraph();
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> stack = new HashSet<>();

        for (NodeId nodeId : graph.keySet()) {
            if (detectCycle(nodeId, graph, visited, stack)) {
                return false;
            }
        }
        return true;
    }

    private boolean detectCycle(
            NodeId nodeId,
            Map<NodeId, List<NodeId>> graph,
            Set<NodeId> visited,
            Set<NodeId> stack) {

        if (stack.contains(nodeId))
            return true;
        if (visited.contains(nodeId))
            return false;

        visited.add(nodeId);
        stack.add(nodeId);

        for (NodeId dep : graph.getOrDefault(nodeId, List.of())) {
            if (detectCycle(dep, graph, visited, stack)) {
                return true;
            }
        }

        stack.remove(nodeId);
        return false;
    }

    private Map<NodeId, List<NodeId>> buildDependencyGraph() {
        Map<NodeId, List<NodeId>> graph = new HashMap<>();
        for (NodeDefinition node : nodes) {
            graph.put(node.id(), List.copyOf(node.dependsOn()));
        }
        return graph;
    }

    private boolean hasValidDependencies() {
        Set<NodeId> nodeIds = nodes.stream()
                .map(NodeDefinition::id)
                .collect(Collectors.toSet());

        return nodes.stream()
                .flatMap(n -> n.dependsOn().stream())
                .allMatch(nodeIds::contains);
    }

    private boolean hasValidIO() {
        // Inputs referenced must exist
        return inputs.keySet().stream().allMatch(Objects::nonNull)
                && outputs.keySet().stream().allMatch(Objects::nonNull);
    }

    // ==================== COMPENSATION ====================

    public boolean isCompensationEnabled() {
        return compensationPolicy != null && compensationPolicy.enabled();
    }

    // ==================== DEBUG / INTROSPECTION ====================

    public int nodeCount() {
        return nodes.size();
    }

    public Set<NodeId> allNodeIds() {
        return nodes.stream()
                .map(NodeDefinition::id)
                .collect(Collectors.toUnmodifiableSet());
    }
}