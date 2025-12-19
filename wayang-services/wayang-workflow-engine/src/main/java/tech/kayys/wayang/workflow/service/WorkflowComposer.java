package tech.kayys.wayang.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.node.EdgeDefinition;

/**
 * Workflow Composition - Build complex workflows from simple ones
 */
@ApplicationScoped
public class WorkflowComposer {

    @Inject
    WorkflowRegistry workflowRegistry;

    /**
     * Compose workflows (sub-workflows)
     */
    public Uni<WorkflowDefinition> compose(
            String parentWorkflowId,
            List<String> childWorkflowIds) {

        // First, get the parent workflow
        return workflowRegistry.getWorkflow(parentWorkflowId)
                .onItem().transformToUni(parent -> {
                    if (parent == null) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Parent workflow not found: " + parentWorkflowId));
                    }

                    if (childWorkflowIds == null || childWorkflowIds.isEmpty()) {
                        // If no children, return parent as-is
                        return Uni.createFrom().item(parent);
                    }

                    // Collect all child workflow Unis
                    List<Uni<WorkflowDefinition>> childUnis = childWorkflowIds.stream()
                            .map(childId -> workflowRegistry.getWorkflow(childId))
                            .collect(Collectors.toList());

                    // Combine all child workflow Unis into a single Uni<List<WorkflowDefinition>>
                    return Uni.combine().all().unis(childUnis).discardItems()
                            .onItem().transformToUni(combined -> {
                                // Now fetch all child workflows again to get the actual values
                                List<Uni<WorkflowDefinition>> refetchedChildUnis = childWorkflowIds.stream()
                                        .map(childId -> workflowRegistry.getWorkflow(childId))
                                        .collect(Collectors.toList());

                                return Uni.combine().all().unis(refetchedChildUnis).collectFailures()
                                        .onItem().transform(childResults -> {
                                            // Verify all children were loaded
                                            for (int i = 0; i < childResults.size(); i++) {
                                                if (childResults.get(i) == null) {
                                                    throw new IllegalArgumentException("Child workflow not found: " + childWorkflowIds.get(i));
                                                }
                                            }

                                            // Create a new composite workflow by combining parent and children
                                            List<NodeDefinition> allNodes = new ArrayList<>(parent.getNodes());
                                            List<EdgeDefinition> allEdges = new ArrayList<>(parent.getEdges());

                                            // Add child workflows' nodes and edges with unique IDs
                                            for (WorkflowDefinition child : childResults) {
                                                if (child != null) {
                                                    for (NodeDefinition node : child.getNodes()) {
                                                        NodeDefinition newNode = node.toBuilder()
                                                                .id(child.getId() + "--" + node.getId()) // Use double dash to avoid conflicts
                                                                .build();
                                                        allNodes.add(newNode);
                                                    }

                                                    // Add child edges with updated node references
                                                    for (EdgeDefinition edge : child.getEdges()) {
                                                        EdgeDefinition newEdge = edge.toBuilder()
                                                                .from(child.getId() + "--" + edge.getFrom())
                                                                .to(child.getId() + "--" + edge.getTo())
                                                                .build();
                                                        allEdges.add(newEdge);
                                                    }
                                                }
                                            }

                                            // Build the composite workflow with all collected nodes and edges
                                            return WorkflowDefinition.builder()
                                                    .id(parent.getId() + "-composite")
                                                    .name(parent.getName() + " Composite")
                                                    .version(parent.getVersion())
                                                    .description("Composite workflow composed of " + parent.getName() +
                                                            " and " + childResults.size() + " child workflows")
                                                    .nodes(allNodes)
                                                    .edges(allEdges)
                                                    .parameters(parent.getParameters() != null ? parent.getParameters() : Map.of())
                                                    .build();
                                        });
                            });
                });
    }

    /**
     * Fork-join pattern
     */
    public WorkflowDefinition forkJoin(
            String workflowId,
            List<String> parallelBranches) {

        // Create a simple fork-join workflow structure
        WorkflowDefinition.Builder builder = WorkflowDefinition.builder()
                .id(workflowId + "-forkjoin")
                .name("Fork-Join: " + workflowId)
                .version("1.0")
                .description("Fork-Join pattern with " + parallelBranches.size() + " parallel branches");

        // Add fork node
        String forkNodeId = "fork-" + workflowId;
        builder.addNode(NodeDefinition.builder()
                .id(forkNodeId)
                .type("FORWARDING_NODE") // Type for fan-out nodes
                .name("Fork Node")
                .build());

        // Add join node
        String joinNodeId = "join-" + workflowId;
        builder.addNode(NodeDefinition.builder()
                .id(joinNodeId)
                .type("JOIN_NODE") // Type for synchronization nodes
                .name("Join Node")
                .build());

        // Connect fork to all branch heads (parallel execution)
        for (int i = 0; i < parallelBranches.size(); i++) {
            String branchId = parallelBranches.get(i);
            String branchHead = "branch-" + i + "-head";
            String branchTail = "branch-" + i + "-tail";

            // Add branch head and tail nodes
            builder.addNode(NodeDefinition.builder()
                    .id(branchHead)
                    .type("BRANCH_HEAD")
                    .name("Branch " + i + " Head for " + branchId)
                    .build());

            builder.addNode(NodeDefinition.builder()
                    .id(branchTail)
                    .type("BRANCH_TAIL")
                    .name("Branch " + i + " Tail for " + branchId)
                    .build());

            // Add edges: fork->branch-head and branch-tail->join
            builder.addEdge(EdgeDefinition.builder()
                    .from(forkNodeId)
                    .to(branchHead)
                    .condition("always")
                    .build());

            builder.addEdge(EdgeDefinition.builder()
                    .from(branchTail)
                    .to(joinNodeId)
                    .condition("always")
                    .build());
        }

        return builder.build();
    }

    /**
     * Simple sequential composition
     */
    public Uni<WorkflowDefinition> sequential(String workflowId, List<String> workflowIds) {
        return workflowRegistry.getWorkflow(workflowId)
            .onItem().transformToUni(mainWorkflow -> {
                if (workflowIds == null || workflowIds.isEmpty()) {
                    return Uni.createFrom().item(mainWorkflow);
                }

                List<Uni<WorkflowDefinition>> otherUnis = workflowIds.stream()
                    .map(id -> workflowRegistry.getWorkflow(id))
                    .collect(Collectors.toList());

                return Uni.combine().all().unis(otherUnis).collectFailures()
                    .onItem().transform(others -> {
                        // Create a sequential composition
                        List<NodeDefinition> allNodes = new ArrayList<>(mainWorkflow.getNodes());
                        List<EdgeDefinition> allEdges = new ArrayList<>(mainWorkflow.getEdges());

                        // Add other workflows' nodes and edges
                        for (WorkflowDefinition other : others) {
                            if (other != null) {
                                for (NodeDefinition node : other.getNodes()) {
                                    NodeDefinition newNode = node.toBuilder()
                                        .id(other.getId() + "--" + node.getId())
                                        .build();
                                    allNodes.add(newNode);
                                }

                                for (EdgeDefinition edge : other.getEdges()) {
                                    EdgeDefinition newEdge = edge.toBuilder()
                                        .from(other.getId() + "--" + edge.getFrom())
                                        .to(other.getId() + "--" + edge.getTo())
                                        .build();
                                    allEdges.add(newEdge);
                                }
                            }
                        }

                        return WorkflowDefinition.builder()
                            .id(workflowId + "-sequential")
                            .name("Sequential: " + workflowId)
                            .version(mainWorkflow.getVersion())
                            .description("Sequential composition of " + workflowIds.size() + " workflows")
                            .nodes(allNodes)
                            .edges(allEdges)
                            .build();
                    });
            });
    }

    /**
     * Conditional composition
     */
    public WorkflowDefinition conditional(String workflowId, List<String> conditionNodes) {
        return WorkflowDefinition.builder()
                .id(workflowId + "-conditional")
                .name("Conditional: " + workflowId)
                .version("1.0")
                .description("Conditional workflow composition with " + conditionNodes.size() + " conditions")
                .build();
    }
}