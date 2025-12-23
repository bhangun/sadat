package tech.kayys.wayang.workflow.service;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
                                                return Uni.createFrom().failure(
                                                                new IllegalArgumentException(
                                                                                "Parent workflow not found: "
                                                                                                + parentWorkflowId));
                                        }

                                        if (childWorkflowIds == null || childWorkflowIds.isEmpty()) {
                                                // If no children, return parent as-is
                                                return Uni.createFrom().item(parent);
                                        }

                                        // Collect all child workflow Unis
                                        List<Uni<WorkflowDefinition>> childUnis = childWorkflowIds.stream()
                                                        .map(childId -> workflowRegistry.getWorkflow(childId))
                                                        .collect(Collectors.toList());

                                        return Uni.join().all(childUnis).andCollectFailures()
                                                        .onItem().transform(childResults -> {
                                                                // Verify all children were loaded
                                                                for (int i = 0; i < childResults.size(); i++) {
                                                                        if (childResults.get(i) == null) {
                                                                                throw new IllegalArgumentException(
                                                                                                "Child workflow not found: "
                                                                                                                + childWorkflowIds
                                                                                                                                .get(i));
                                                                        }
                                                                }

                                                                // Create a new composite workflow by combining parent
                                                                // and children
                                                                List<NodeDefinition> allNodes = new ArrayList<>(
                                                                                parent.getNodes());
                                                                List<EdgeDefinition> allEdges = new ArrayList<>(
                                                                                parent.getEdges());

                                                                // Add child workflows' nodes and edges with unique IDs
                                                                for (WorkflowDefinition child : childResults) {
                                                                        if (child != null) {
                                                                                for (NodeDefinition node : child
                                                                                                .getNodes()) {
                                                                                        NodeDefinition newNode = NodeDefinition
                                                                                                        .builder()
                                                                                                        .id(child.getId()
                                                                                                                        + "--"
                                                                                                                        + node.getId())
                                                                                                        .displayName(node
                                                                                                                        .getDisplayName())
                                                                                                        .type(node.getType())
                                                                                                        .execution(node.getExecution())
                                                                                                        .build();
                                                                                        allNodes.add(newNode);
                                                                                }

                                                                                // Add child edges with updated node
                                                                                // references
                                                                                for (EdgeDefinition edge : child
                                                                                                .getEdges()) {
                                                                                        EdgeDefinition newEdge = EdgeDefinition
                                                                                                        .builder()
                                                                                                        .from(child.getId()
                                                                                                                        + "--"
                                                                                                                        + edge.getFrom())
                                                                                                        .to(child.getId()
                                                                                                                        + "--"
                                                                                                                        + edge.getTo())
                                                                                                        .condition(edge.getCondition())
                                                                                                        .build();
                                                                                        allEdges.add(newEdge);
                                                                                }
                                                                        }
                                                                }

                                                                // Build the composite workflow with all collected nodes
                                                                // and edges
                                                                return WorkflowDefinition.builder()
                                                                                .id(parent.getId() + "-composite")
                                                                                .name(parent.getName() + " Composite")
                                                                                .version(parent.getVersion())
                                                                                .description("Composite workflow composed of "
                                                                                                + parent.getName() +
                                                                                                " and "
                                                                                                + childResults.size()
                                                                                                + " child workflows")
                                                                                .nodes(allNodes)
                                                                                .edges(allEdges)
                                                                                .build();
                                                        });
                                });
        }

        /**
         * Fork-join pattern
         */
        public WorkflowDefinition forkJoin(
                        String workflowId,
                        List<String> parallelBranches) {

                List<NodeDefinition> nodes = new ArrayList<>();
                List<EdgeDefinition> edges = new ArrayList<>();

                // Add fork node
                String forkNodeId = "fork-" + workflowId;
                nodes.add(NodeDefinition.builder()
                                .id(forkNodeId)
                                .type("FORWARDING_NODE") // Type for fan-out nodes
                                .displayName("Fork Node")
                                .build());

                // Add join node
                String joinNodeId = "join-" + workflowId;
                nodes.add(NodeDefinition.builder()
                                .id(joinNodeId)
                                .type("JOIN_NODE") // Type for synchronization nodes
                                .displayName("Join Node")
                                .build());

                // Connect fork to all branch heads (parallel execution)
                for (int i = 0; i < parallelBranches.size(); i++) {
                        String branchId = parallelBranches.get(i);
                        String branchHead = "branch-" + i + "-head";
                        String branchTail = "branch-" + i + "-tail";

                        // Add branch head and tail nodes
                        nodes.add(NodeDefinition.builder()
                                        .id(branchHead)
                                        .type("BRANCH_HEAD")
                                        .displayName("Branch " + i + " Head for " + branchId)
                                        .build());

                        nodes.add(NodeDefinition.builder()
                                        .id(branchTail)
                                        .type("BRANCH_TAIL")
                                        .displayName("Branch " + i + " Tail for " + branchId)
                                        .build());

                        // Add edges: fork->branch-head and branch-tail->join
                        edges.add(EdgeDefinition.builder()
                                        .from(forkNodeId)
                                        .to(branchHead)
                                        .condition("always")
                                        .build());

                        edges.add(EdgeDefinition.builder()
                                        .from(branchTail)
                                        .to(joinNodeId)
                                        .condition("always")
                                        .build());
                }

                return WorkflowDefinition.builder()
                                .id(workflowId + "-forkjoin")
                                .name("Fork-Join: " + workflowId)
                                .version("1.0")
                                .description("Fork-Join pattern with " + parallelBranches.size()
                                                + " parallel branches")
                                .nodes(nodes)
                                .edges(edges)
                                .build();
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

                                        return Uni.join().all(otherUnis).andCollectFailures()
                                                        .onItem().transform(others -> {
                                                                // Create a sequential composition
                                                                List<NodeDefinition> allNodes = new ArrayList<>(
                                                                                mainWorkflow.getNodes());
                                                                List<EdgeDefinition> allEdges = new ArrayList<>(
                                                                                mainWorkflow.getEdges());

                                                                // Add other workflows' nodes and edges
                                                                for (WorkflowDefinition other : others) {
                                                                        if (other != null) {
                                                                                for (NodeDefinition node : other
                                                                                                .getNodes()) {
                                                                                        NodeDefinition newNode = NodeDefinition
                                                                                                        .builder()
                                                                                                        .id(other.getId()
                                                                                                                        + "--"
                                                                                                                        + node.getId())
                                                                                                        .displayName(node
                                                                                                                        .getDisplayName())
                                                                                                        .type(node.getType())
                                                                                                        .execution(node.getExecution())
                                                                                                        .build();
                                                                                        allNodes.add(newNode);
                                                                                }

                                                                                for (EdgeDefinition edge : other
                                                                                                .getEdges()) {
                                                                                        EdgeDefinition newEdge = EdgeDefinition
                                                                                                        .builder()
                                                                                                        .from(other.getId()
                                                                                                                        + "--"
                                                                                                                        + edge.getFrom())
                                                                                                        .to(other.getId()
                                                                                                                        + "--"
                                                                                                                        + edge.getTo())
                                                                                                        .condition(edge.getCondition())
                                                                                                        .build();
                                                                                        allEdges.add(newEdge);
                                                                                }
                                                                        }
                                                                }

                                                                return WorkflowDefinition.builder()
                                                                                .id(workflowId + "-sequential")
                                                                                .name("Sequential: " + workflowId)
                                                                                .version(mainWorkflow.getVersion())
                                                                                .description("Sequential composition of "
                                                                                                + workflowIds.size()
                                                                                                + " workflows")
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
                                .description("Conditional workflow composition with " + conditionNodes.size()
                                                + " conditions")
                                .build();
        }
}